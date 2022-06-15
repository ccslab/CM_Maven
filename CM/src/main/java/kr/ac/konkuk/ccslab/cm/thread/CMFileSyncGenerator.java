package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEventRequestNewFiles;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEventStartFileBlockChecksum;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncUpdateMode;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileType;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;

public class CMFileSyncGenerator implements Runnable {
    private String userName;
    private CMInfo cmInfo;
    private List<CMFileSyncEntry> newClientPathEntryList;
    private Map<Integer, CMFileSyncBlockChecksum[]> blockChecksumArrayMap;
    private Map<Integer, Integer> basisFileIndexMap;    // key: client entry index, value: basis index
    private Map<Integer, Integer> blockSizeOfBasisFileMap;
    private Map<Integer, SeekableByteChannel> basisFileChannelForReadMap;   // for reading basis file
    // for writing a temp file to update basis file
    private Map<Integer, SeekableByteChannel> tempFileChannelForWriteMap;

    private Map<Path, Boolean> isNewFileCompletedMap;
    private Map<Path, Boolean> isUpdateFileCompletedMap;
    private int numNewFilesCompleted;
    private int numUpdateFilesCompleted;

    public CMFileSyncGenerator(String userName, CMInfo cmInfo) {
        this.userName = userName;
        this.cmInfo = cmInfo;
        newClientPathEntryList = null;
        blockChecksumArrayMap = new Hashtable<>();
        basisFileIndexMap = new Hashtable<>();
        blockSizeOfBasisFileMap = new Hashtable<>();
        basisFileChannelForReadMap = new Hashtable<>();
        tempFileChannelForWriteMap = new Hashtable<>();

        isNewFileCompletedMap = new Hashtable<>();
        isUpdateFileCompletedMap = new Hashtable<>();
        numNewFilesCompleted = 0;
        numUpdateFilesCompleted = 0;
    }

    public List<CMFileSyncEntry> getNewClientPathEntryList() {
        return newClientPathEntryList;
    }

    public Map<Integer, CMFileSyncBlockChecksum[]> getBlockChecksumArrayMap() {
        return blockChecksumArrayMap;
    }

    public Map<Path, Boolean> getIsNewFileCompletedMap() {
        return isNewFileCompletedMap;
    }

    public Map<Path, Boolean> getIsUpdateFileCompletedMap() {
        return isUpdateFileCompletedMap;
    }

    public int getNumNewFilesCompleted() {
        return numNewFilesCompleted;
    }

    public void setNumNewFilesCompleted(int numNewFilesCompleted) {
        this.numNewFilesCompleted = numNewFilesCompleted;
    }

    public int getNumUpdateFilesCompleted() {
        return numUpdateFilesCompleted;
    }

    public void setNumUpdateFilesCompleted(int numUpdateFilesCompleted) {
        this.numUpdateFilesCompleted = numUpdateFilesCompleted;
    }

    public Map<Integer, Integer> getBasisFileIndexMap() {
        return basisFileIndexMap;
    }

    public Map<Integer, SeekableByteChannel> getBasisFileChannelForReadMap() {
        return basisFileChannelForReadMap;
    }

    public Map<Integer, SeekableByteChannel> getTempFileChannelForWriteMap() {
        return tempFileChannelForWriteMap;
    }

    public Map<Integer, Integer> getBlockSizeOfBasisFileMap() {
        return blockSizeOfBasisFileMap;
    }

    @Override
    public void run() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.run() called..");
        }

        // create a basis file-entry-list at the server
        List<Path> basisFileList = createBasisFileList();  // always return a list object
        if (CMInfo._CM_DEBUG) {
            System.out.println("basisFileList = " + basisFileList);
        }
        // put the basis file list to the basis-file-list-map
        CMFileSyncInfo syncInfo = Objects.requireNonNull(cmInfo.getFileSyncInfo());
        Map<String, List<Path>> basisFileListMap = Objects.requireNonNull(syncInfo.getBasisFileListMap());
        basisFileListMap.put(userName, basisFileList);

        //// compare the client file-entry-list and the basis file-entry-list

        // delete files that exist only at the server and update the basisFileList
        deleteFilesAndUpdateBasisFileList();
        if (CMInfo._CM_DEBUG) {
            System.out.println("basisFileList after the deletion = " + basisFileList);
        }

        // create a new file-entry-list that will be added to the server
        //newClientPathEntryList = createNewClientPathEntryList();
        CMConfigurationInfo confInfo = Objects.requireNonNull(cmInfo.getConfigurationInfo());
        CMFileSyncUpdateMode updateMode = confInfo.getFileSyncUpdateMode();
        newClientPathEntryList = createNewClientPathEntryList(updateMode);
        if (newClientPathEntryList == null) {
            System.err.println("CMFileSyncGenerator.run(), newFileList is null!");
            return;
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("newFileList = " + newClientPathEntryList);
        }

        // request the files in the new file-entry-list from the client
        boolean requestResult = requestTransferOfNewFiles();
        if (!requestResult) {
            System.err.println("request new files error!");
            return;
        }

        // update the files at the server by synchronizing with those at the client
        requestResult = compareBasisAndClientFileList();
        if (!requestResult) {
            System.err.println("compare-basis-and-client-file-list error!");
        }

        // check if the file-sync task is completed. (both client and server sync home are empty)
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        if (syncManager.isCompleteFileSync(userName)) {
            syncManager.completeFileSync(userName);
        }
    }

    private boolean compareBasisAndClientFileList() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.compareBasisAndClientFileList() called..");
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(cmInfo.getFileSyncInfo());
        List<Path> basisFileList = Objects.requireNonNull(syncInfo.getBasisFileListMap()).get(userName);

        if (basisFileList == null) {
            System.err.println("basisFileList is null!");
            return false;
        }
        if (basisFileList.isEmpty()) {
            if (CMInfo._CM_DEBUG) {
                System.out.println("basisFileList is empty.");
            }
            return true;
        }

        boolean sendResult;

        // get CMFileSyncManager object
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        // get the server sync home
        Path serverSyncHome = syncManager.getServerSyncHome(userName);

        for (int basisFileIndex = 0; basisFileIndex < basisFileList.size(); basisFileIndex++) {
            Path basisFile = basisFileList.get(basisFileIndex);
            if (CMInfo._CM_DEBUG) {
                System.out.println("-----------------------------------");
                System.out.println("basisFileIndex = " + basisFileIndex);
                System.out.println("basisFile = " + basisFile);
            }
            // get relative path
            Path relativeBasisFile = basisFile.subpath(serverSyncHome.getNameCount(),
                    basisFile.getNameCount());
            // search for the client file entry
            CMFileSyncEntry clientPathEntry = null;
            int clientPathEntryIndex = -1;
            List<CMFileSyncEntry> clientPathEntryList = cmInfo.getFileSyncInfo().getClientPathEntryListMap()
                    .get(userName);
            for (int i = 0; i < clientPathEntryList.size(); i++) {
                CMFileSyncEntry entry = clientPathEntryList.get(i);
                if (relativeBasisFile.equals(entry.getPathRelativeToHome())) {
                    clientPathEntry = entry;
                    clientPathEntryIndex = i;
                    break;
                }
            }
            if (clientPathEntry == null) {
                System.err.println("client file entry not found for basisFile(" + relativeBasisFile + ")!");
                continue;   // proceed to the next basis file
            }

            // compare clientFileEntry and basisFile
            long sizeOfBasisFile;
            FileTime lastModifiedTimeOfBasisFile;
            try {
                sizeOfBasisFile = Files.size(basisFile);
                lastModifiedTimeOfBasisFile = Files.getLastModifiedTime(basisFile);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            if (clientPathEntry.getSize() == sizeOfBasisFile &&
                    clientPathEntry.getLastModifiedTime().equals(lastModifiedTimeOfBasisFile)) {
                // already synchronized
                syncManager.skipUpdateFile(userName, basisFile);
                if (CMInfo._CM_DEBUG) {
                    System.out.println("basisFile(" + basisFile + ") skips synchronization.");
                    System.out.println("sizeOfBasisFile = " + sizeOfBasisFile);
                    System.out.println("lastModifiedTimeOfBasisFile = " + lastModifiedTimeOfBasisFile);
                }
                continue;
            }

            // check the directory case
            if (clientPathEntry.getType() == CMFileType.DIR) {
                if (!clientPathEntry.getLastModifiedTime().equals(lastModifiedTimeOfBasisFile)) {
                    try {
                        // update the last modified time of the sub-directory
                        Files.setLastModifiedTime(basisFile, clientPathEntry.getLastModifiedTime());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // complete the update-file task of the file-sync for this sub-directory
                boolean ret = syncManager.completeUpdateFile(userName, basisFile);
                if (!ret) {
                    System.err.println("error of completing update file(dir)!");
                    System.err.println("userName(" + userName + "), dir(" + basisFile + ")");
                }
                // continue with the next basis file
                continue;
            }

            // add the index pair to the table
            basisFileIndexMap.put(clientPathEntryIndex, basisFileIndex);

            // get block checksum
            CMFileSyncBlockChecksum[] checksumArray = createBlockChecksum(basisFileIndex, basisFile);
            if (checksumArray == null) {
                System.err.println("checksum array is null!");
                continue;
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("checksum array size = " + checksumArray.length);
            }

            // add block-checksum array to the table
            // key: client entry index, value: block-checksum array
            blockChecksumArrayMap.put(clientPathEntryIndex, checksumArray);

            // send the client entry index and block-checksum array to the client
            sendResult = sendBlockChecksum(clientPathEntryIndex, checksumArray);
            if (!sendResult) {
                System.err.println("send block-checksum error!");
                return false;
            }

            if (CMInfo._CM_DEBUG) {
                System.out.println("-----------------------------------");
            }
        }

        return true;
    }

    private boolean sendBlockChecksum(int clientFileEntryIndex, CMFileSyncBlockChecksum[] checksumArray) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.sendBlockChecksum() called..");
        }

        // create a START_FILE_BLOCK_CHECKSUM event
        CMFileSyncEventStartFileBlockChecksum fse = new CMFileSyncEventStartFileBlockChecksum();
        fse.setSender(cmInfo.getInteractionInfo().getMyself().getName());
        fse.setReceiver(userName);
        fse.setFileEntryIndex(clientFileEntryIndex);
        fse.setTotalNumBlocks(checksumArray.length);
        // get basis file index
        int basisFileIndex = basisFileIndexMap.get(clientFileEntryIndex);
        // get block size with the basis file index
        fse.setBlockSize(blockSizeOfBasisFileMap.get(basisFileIndex));
        // send the event
        boolean ret = CMEventManager.unicastEvent(fse, userName, cmInfo);
        if (!ret) {
            System.err.println("send error, fse: " + fse);
            return false;
        }

        return true;
    }

    private CMFileSyncBlockChecksum[] createBlockChecksum(int basisFileIndex, Path basisFile) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.createBlockChecksum() called..");
            System.out.println("basisFileIndex = " + basisFileIndex);
            System.out.println("basisFile = " + basisFile);
        }

        // calculate the block size based on the file size
        long fileSize;
        int blockSize;
        try {
            fileSize = Files.size(basisFile);
            blockSize = calculateBlockSize(fileSize);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // store the block size in the table
        blockSizeOfBasisFileMap.put(basisFileIndex, blockSize);

        // set the number of blocks
        int numBlocks = (int) (fileSize / blockSize);
        if (fileSize % blockSize > 0) numBlocks++;
        if (CMInfo._CM_DEBUG)
            System.out.println("numBlocks = " + numBlocks);

        // create a block-checksum array
        CMFileSyncBlockChecksum[] checksumArray = new CMFileSyncBlockChecksum[numBlocks];
        // get the file-sync manager
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);

        // get SeekableByteChannel from the basis file
        SeekableByteChannel channel;
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        try {
            channel = Files.newByteChannel(basisFile, StandardOpenOption.READ);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // create block-checksum object in the array, and set the weak and strong checksum
        int bytesRead = 0;
        int weakChecksum = 0;
        byte[] strongChecksum = null;
        for (int i = 0; i < checksumArray.length; i++) {
            // create a block-checksum object that is set to the i-th element
            checksumArray[i] = new CMFileSyncBlockChecksum();
            // initialize the ByteBuffer
            buffer.clear();
            // read the i-th block from the channel to the buffer
            try {
                bytesRead = channel.read(buffer);
                if (CMInfo._CM_DEBUG) {
                    System.out.println("--------------");
                    System.out.println("block(" + i + "), bytesRead = " + bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            // set current block index (i) in the array element
            checksumArray[i].setBlockIndex(i);
            // calculate and set weak checksum in the array element
            buffer.flip();
            weakChecksum = syncManager.calculateWeakChecksum(buffer);
            checksumArray[i].setWeakChecksum(weakChecksum);
            // calculate and set strong checksum in the array element
            buffer.rewind();    // buffer position needs to be rewound after the calculation of weak checksum
            strongChecksum = syncManager.calculateStrongChecksum(buffer);
            checksumArray[i].setStrongChecksum(strongChecksum);
        }

        // close the channel
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return checksumArray;
    }

    // refer to the rsync code to calculate block size according to file size
    private int calculateBlockSize(long fileSize) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.calculateBlockSize() called..");
            System.out.println("fileSize = " + fileSize);
        }
        int blength;
        if (fileSize < CMFileSyncInfo.BLOCK_SIZE * CMFileSyncInfo.BLOCK_SIZE) {
            blength = CMFileSyncInfo.BLOCK_SIZE;
        } else {
            int c, cnt;
            long l;
            for (c = 1, l = fileSize, cnt = 0; (l >>= 2) > 0; c <<= 1, cnt++) {
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("c=" + c + ", l=" + l + ", cnt=" + cnt);
            }

            if (c < 0 || c >= CMFileSyncInfo.MAX_BLOCK_SIZE)
                blength = CMFileSyncInfo.MAX_BLOCK_SIZE;
            else {
                blength = 0;
                do {
                    blength |= c;
                    if (fileSize < (long) blength * blength)
                        blength &= ~c;
                    c >>= 1;
                } while (c >= 8);    // round to multiple of 8
                blength = Math.max(blength, CMFileSyncInfo.BLOCK_SIZE);
            }
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("calculated block size = " + blength);
        }
        return blength;
    }

    private boolean requestTransferOfNewFiles() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.requestTransferOfNewFiles() called..");
        }

        if (newClientPathEntryList == null) {
            System.err.println("newClientPathEntryList is null!");
            return false;   // null is an error
        }
        if (newClientPathEntryList.isEmpty()) {
            if (CMInfo._CM_DEBUG) {
                System.out.println("newClientPathEntryList is empty.");
            }
            return true;
        }

        // get sync home
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        Path syncHome = syncManager.getServerSyncHome(userName);

        // create new sub-directories that do not have to be requested from the client
        Iterator<CMFileSyncEntry> iter = newClientPathEntryList.iterator();
        while (iter.hasNext()) {
            CMFileSyncEntry entry = iter.next();

            if (entry.getType() == CMFileType.DIR) {
                // get an absolute path of the new sub-directory
                Path absolutePath = syncHome.resolve(entry.getPathRelativeToHome());
                // create the sub-directory
                try {
                    Files.createDirectories(absolutePath);
                } catch (IOException e) {
                    System.err.println("error of creating a new sub-directory: " + absolutePath);
                    e.printStackTrace();
                    continue;
                }
                // set the last modified time of the directory
                try {
                    Files.setLastModifiedTime(absolutePath, entry.getLastModifiedTime());
                } catch (IOException e) {
                    System.err.println("error of setting last modified time of: " + absolutePath);
                    e.printStackTrace();
                    continue;
                }
                // set the completion of new-file-transfer
                boolean ret = syncManager.completeNewFileTransfer(userName, entry.getPathRelativeToHome());
                if (ret) {
                    // remove the current entry from this list
                    iter.remove();
                } else {
                    System.err.println("error of completeNewFileTransfer(), user(" + userName
                            + "), relative path(" + entry.getPathRelativeToHome() + ")");
                    continue;
                }
            }
        }

        // filter only file entry list out of newClientPathEntryList
        List<CMFileSyncEntry> newFileEntryList = newClientPathEntryList.stream()
                .filter(entry -> entry.getType() != CMFileType.DIR)
                .collect(Collectors.toList());

        int numRequestsCompleted = 0;
        boolean sendResult;
        while (numRequestsCompleted < newFileEntryList.size()) {
            // create a request event
            CMFileSyncEventRequestNewFiles fse = new CMFileSyncEventRequestNewFiles();
            String serverName = cmInfo.getInteractionInfo().getMyself().getName();
            fse.setSender(serverName);   // server
            fse.setReceiver(userName);
            fse.setRequesterName(serverName); // server
            //// set numRequestedFiles and requestedFileList
            // get the size of the remaining event fields
            int curByteNum = fse.getByteNum();
            List<Path> requestedFileList = new ArrayList<>();
            int numRequestedFiles = 0;
            while (numRequestsCompleted < newFileEntryList.size() && curByteNum < CMInfo.MAX_EVENT_SIZE) {
                Path path = newFileEntryList.get(numRequestsCompleted).getPathRelativeToHome();
                curByteNum += CMInfo.STRING_LEN_BYTES_LEN
                        + path.toString().getBytes().length;
                if (curByteNum < CMInfo.MAX_EVENT_SIZE) {
                    // increment the numRequestedFiles
                    numRequestedFiles++;
                    // add path to the requestedFileList
                    requestedFileList.add(path);
                    // increment the numRequestsCompleted
                    numRequestsCompleted++;
                } else
                    break;
            }
            // set numRequestedFiles and requestedFileList to the event
            fse.setNumRequestedFiles(numRequestedFiles);
            fse.setRequestedFileList(requestedFileList);
            // send the request event
            sendResult = CMEventManager.unicastEvent(fse, userName, cmInfo);
            if (!sendResult) {
                System.err.println("CMFileSyncGenerator.requestTransferOfNewFiles(), send error!");
                return false;
            }

            if (CMInfo._CM_DEBUG) {
                System.out.println("sent REQUEST_NEW_FILES event = " + fse);
            }
        }

        return true;
    }

/*
    private List<CMFileSyncEntry> createNewClientPathEntryList() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.createNewClientPathEntryList() called..");
        }
        // get clientPathEntryList
        CMFileSyncInfo syncInfo = Objects.requireNonNull(cmInfo.getFileSyncInfo());
        Map<String, List<CMFileSyncEntry>> clientPathEntryListMap = syncInfo.getClientPathEntryListMap();
        Objects.requireNonNull(clientPathEntryListMap);
        List<CMFileSyncEntry> clientPathEntryList = clientPathEntryListMap.get(userName);
        if (clientPathEntryList == null) {
            return new ArrayList<>();
        }
        // get the start path index
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Path serverSyncHome = syncManager.getServerSyncHome(userName);
        int startPathIndex = serverSyncHome.getNameCount();
        // get basis file list
        List<Path> basisFileList = Objects.requireNonNull(syncInfo.getBasisFileListMap().get(userName));
        // get the relative path list from the basis file list
        List<Path> relativeBasisFileList = basisFileList.stream()
                .map(path -> path.subpath(startPathIndex, path.getNameCount()))
                .collect(Collectors.toList());
        // create a new file list that will be added to the server
        List<CMFileSyncEntry> newClientPathEntryList = clientPathEntryList.stream()
                .filter(entry -> !relativeBasisFileList.contains(entry.getPathRelativeToHome()))
                .collect(Collectors.toList());

        // initialize isNewFileCompletedMap
        for (CMFileSyncEntry entry : newClientPathEntryList) {
            isNewFileCompletedMap.put(entry.getPathRelativeToHome(), false);
        }

        return newClientPathEntryList;
    }
*/

    /*
        private List<CMFileSyncEntry> createNewClientPathEntryListAndUpdateBasisFileList(CMFileSyncUpdateMode updateMode) {
            if (CMInfo._CM_DEBUG) {
                System.out.println("=== CMFileSyncGenerator.createNewClientPathEntryListAndUpdateBasisFileList called..");
                System.out.println("updateMode = " + updateMode);
            }
            // get the client-file-entry list
            CMFileSyncInfo syncInfo = Objects.requireNonNull(cmInfo.getFileSyncInfo());
            Map<String, List<CMFileSyncEntry>> clientPathEntryListMap = syncInfo.getClientPathEntryListMap();
            Objects.requireNonNull(clientPathEntryListMap);
            List<CMFileSyncEntry> clientPathEntryList = clientPathEntryListMap.get(userName);
            if (clientPathEntryList == null) {
                return new ArrayList<>();
            }

            // get the basis file list
            List<Path> basisFileList = syncInfo.getBasisFileListMap().get(userName);
            Objects.requireNonNull(basisFileList);

            // get the basis file list with relative path
            CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
            Objects.requireNonNull(syncManager);
            Path serverSyncHome = syncManager.getServerSyncHome(userName);
            int startPathIndex = serverSyncHome.getNameCount();
            List<Path> relativeBasisFileList = basisFileList.stream()
                    .map(path -> path.subpath(startPathIndex, path.getNameCount()))
                    .collect(Collectors.toList());

            // create a new empty entry list
            List<CMFileSyncEntry> newClientPathEntryList = new ArrayList<>();
            for (CMFileSyncEntry entry : clientPathEntryList) {
                // If the client entry that is not contained in the basis file list,
                // the entry is added to the new client entry list.
                if(!relativeBasisFileList.contains(entry.getPathRelativeToHome())) {
                    newClientPathEntryList.add(entry);
                }
                else {
                    // get basis file
                    Path basisFile = serverSyncHome.resolve(entry.getPathRelativeToHome());
                    long basisFileSize;
                    FileTime basisFileLastModifiedTime;
                    try {
                        basisFileSize = Files.size(basisFile);
                        basisFileLastModifiedTime = Files.getLastModifiedTime(basisFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    // if the client entry needs to be synchronized with the basis file,
                    if(basisFileSize != entry.getSize() ||
                            !basisFileLastModifiedTime.equals(entry.getLastModifiedTime())) {
                        // if the update mode is FILE
                        if(updateMode == CMFileSyncUpdateMode.FILE) {
                            // every client entry to be synced is added to the new list.
                            newClientPathEntryList.add(entry);
                        }
                        else if (updateMode == CMFileSyncUpdateMode.HYBRID) {
                            // get configurations
                            CMConfigurationInfo confInfo = Objects.requireNonNull(cmInfo.getConfigurationInfo());
                            long fileSizeThreshold = confInfo.getFileSizeThreshold();
                            double fileModRatioThreshold = confInfo.getFileModRatioThreshold();
                            // if the entry is not DIR type
                            if (entry.getType() != CMFileType.DIR) {
                                // if the entry file size is less than or equal to the size threshold,
                                // the entry is added to the new list.
                                if (entry.getSize() <= fileSizeThreshold) {
                                    newClientPathEntryList.add(entry);
                                } else {
                                    // if the difference of modified size is greater than the ratio threshold,
                                    // the entry is added to the new list.
                                    long entrySize = entry.getSize();
                                    long sizeDifference;
                                    if (basisFileSize >= entrySize) sizeDifference = basisFileSize - entrySize;
                                    else sizeDifference = entrySize - basisFileSize;
                                    if (sizeDifference / (double) basisFileSize > fileModRatioThreshold)
                                        newClientPathEntryList.add(entry);
                                }
                            }
                        }
                    }
                }
            }

            // added the new client-entry list to isNewFileCompletedMap
            // , deleted new entries from the basis file list and isUpdateFileCompletedMap
            for (CMFileSyncEntry entry : newClientPathEntryList) {
                Path entryPath = entry.getPathRelativeToHome();
                isNewFileCompletedMap.put(entryPath, false);
                if (relativeBasisFileList.contains(entryPath)) {
                    Path absolutePath = serverSyncHome.resolve(entryPath);
                    basisFileList.remove(absolutePath);
                    isUpdateFileCompletedMap.remove(absolutePath);
                }
            }

            return newClientPathEntryList;
        }
    */
    private List<CMFileSyncEntry> createNewClientPathEntryList(CMFileSyncUpdateMode updateMode) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.createNewClientPathEntryList() called..");
            System.out.println("updateMode = " + updateMode);
        }
        // get the client-file-entry list
        CMFileSyncInfo syncInfo = Objects.requireNonNull(cmInfo.getFileSyncInfo());
        Map<String, List<CMFileSyncEntry>> clientPathEntryListMap = syncInfo.getClientPathEntryListMap();
        Objects.requireNonNull(clientPathEntryListMap);
        List<CMFileSyncEntry> clientPathEntryList = clientPathEntryListMap.get(userName);
        if (clientPathEntryList == null) {
            return new ArrayList<>();
        }

        // get the basis file list
        List<Path> basisFileList = syncInfo.getBasisFileListMap().get(userName);
        Objects.requireNonNull(basisFileList);

        // get the basis file list with relative path
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        Path serverSyncHome = syncManager.getServerSyncHome(userName);
        int startPathIndex = serverSyncHome.getNameCount();
        List<Path> relativeBasisFileList = basisFileList.stream()
                .map(path -> path.subpath(startPathIndex, path.getNameCount()))
                .collect(Collectors.toList());

        // create a new empty entry list
        List<CMFileSyncEntry> newClientPathEntryList = new ArrayList<>();
        for (CMFileSyncEntry entry : clientPathEntryList) {
            // get entry (relative) path
            Path entryPath = entry.getPathRelativeToHome();
            // get basis (absolute) path
            Path basisFile = serverSyncHome.resolve(entryPath);
            // If the entry path is not in the basis file list,
            if (!relativeBasisFileList.contains(entryPath)) {
                if (updateMode == CMFileSyncUpdateMode.DELTA) {
                    // create an empty file in the sync home
                    try {
                        Files.createFile(basisFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    // add it to the basis file list and isUpdateFileCompletedMap
                    basisFileList.add(basisFile);
                    isUpdateFileCompletedMap.put(basisFile, false);
                } else {
                    // add entry to the new list
                    newClientPathEntryList.add(entry);
                    // add entry path to the isNewFileCompletedMap
                    isNewFileCompletedMap.put(entryPath, false);
                }
            } else {
                // get the size and last modified time of the basis file
                long basisFileSize;
                FileTime basisFileLastModifiedTime;
                try {
                    basisFileSize = Files.size(basisFile);
                    basisFileLastModifiedTime = Files.getLastModifiedTime(basisFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                // If the basis file needs to be synchronized,
                if(basisFileSize != entry.getSize() ||
                    !basisFileLastModifiedTime.equals(entry.getLastModifiedTime())) {
                    if(updateMode == CMFileSyncUpdateMode.FILE) {
                        // add the entry to the new list
                        newClientPathEntryList.add(entry);
                        // removed from the basis file list and isUpdateFileCompletedMap
                        basisFileList.remove(basisFile);
                        isUpdateFileCompletedMap.remove(basisFile);
                    }
                }
            }
        }

        return newClientPathEntryList;
    }

    private void deleteFilesAndUpdateBasisFileList() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.deleteFilesAndUpdateBasisFileList() called..");
        }
        // get the client file-entry-list
        CMFileSyncInfo syncInfo = Objects.requireNonNull(cmInfo.getFileSyncInfo());
        Map<String, List<CMFileSyncEntry>> clientPathEntryListMap = syncInfo.getClientPathEntryListMap();
        Objects.requireNonNull(clientPathEntryListMap);
        List<CMFileSyncEntry> fileEntryList = clientPathEntryListMap.get(userName);

        // get the client path list from the file-entry-list
        List<Path> entryPathList = null;
        if (fileEntryList != null) {
            entryPathList = fileEntryList.stream()
                    .map(CMFileSyncEntry::getPathRelativeToHome)
                    .collect(Collectors.toList());
        }
        // get the CMFileSyncManager object
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);

        //// create target file list that exists only at the server and that will be deleted
        // get the server sync home and the start index
        Path serverSyncHome = syncManager.getServerSyncHome(userName);
        int startPathIndex = serverSyncHome.getNameCount();
        // get basis file list
        List<Path> basisFileList = Objects.requireNonNull(syncInfo.getBasisFileListMap()).get(userName);
        // firstly, delete files (not directories) that exist only at the basis file list
        Iterator<Path> iter = basisFileList.iterator();
        while (iter.hasNext()) {
            Path path = iter.next();
            if (!Files.isDirectory(path)) {
                if (entryPathList == null ||
                        !entryPathList.contains(path.subpath(startPathIndex, path.getNameCount()))) {
                    try {
                        Files.delete(path);
                        iter.remove();
                        if (CMInfo._CM_DEBUG) {
                            System.out.println("deleted file = " + path);
                        }
                    } catch (IOException e) {
                        System.err.println("file delete error: " + path);
                        e.printStackTrace();
                        continue;
                    }
                }
            }
        }
        // delete directories that exist only at the basis file list
        iter = basisFileList.iterator();
        while (iter.hasNext()) {
            Path path = iter.next();
            if (entryPathList == null ||
                    !entryPathList.contains(path.subpath(startPathIndex, path.getNameCount()))) {
                try {
                    Files.delete(path);
                    iter.remove();
                    if (CMInfo._CM_DEBUG) {
                        System.out.println("deleted directory = " + path);
                    }
                } catch (IOException e) {
                    System.err.println("directory delete error: " + path);
                    e.printStackTrace();
                    continue;
                }
            }
        }

        if (entryPathList == null) {
            // check if the basis file list is empty
            if (!basisFileList.isEmpty()) {
                System.err.println("The basis file list is not empty after deletion!");
                for (Path path : basisFileList)
                    System.out.println("remaining path = " + path);
                basisFileList.clear();
            }
        }

        // initialize isUpdateFileCompletedMap
        for (Path path : basisFileList) {
            isUpdateFileCompletedMap.put(path, false);
        }
    }

    private List<Path> createBasisFileList() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.createBasisFileList() called..");
        }
        // get the file sync manager
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        if (syncManager == null) {
            System.err.println("CMFileSyncGenerator.createBasisFileList(), file-sync manager is null!");
            return null;
        }
        // get the server sync home
        Path serverSyncHome = syncManager.getServerSyncHome(userName);
        // check if the sync home exists or not
        if (Files.notExists(serverSyncHome)) {
            System.err.println("CMFileSyncGenerator.createBasisFileList(), the server sync-home does not exist!");
            return null;
        }
        // create a basis file list
        return syncManager.createPathList(serverSyncHome);
    }
}
