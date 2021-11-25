package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEventRequestNewFiles;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CMFileSyncGenerator implements Runnable {
    private String userName;
    private CMInfo cmInfo;
    private List<CMFileSyncEntry> fileEntryList;
    private List<Path> basisFileList;
    private List<Path> newFileList;
    private Hashtable<Integer, CMFileSyncBlockChecksum[]> blockChecksumArrayHashtable;
    private Hashtable<Integer, Integer> basisFileIndexHashtable;    // key: client entry index, value: basis index
    private Hashtable<Integer, Integer> blockSizeOfBasisFileTable;
    private Hashtable<Integer, SeekableByteChannel> basisFileChannelForReadTable;   // for read basis file
    private Hashtable<Integer, SeekableByteChannel> basisFileChannelForWriteTable;  // for write basis file

    private Hashtable<Path, Boolean> isNewFileCompletedHashtable;
    private Hashtable<Path, Boolean> isUpdateFileCompletedHashtable;
    private int numNewFilesCompleted;
    private int numUpdateFilesCompleted;

    public CMFileSyncGenerator(String userName, CMInfo cmInfo) {
        this.userName = userName;
        this.cmInfo = cmInfo;
        fileEntryList = null;
        basisFileList = null;
        newFileList = null;
        blockChecksumArrayHashtable = null;
        basisFileIndexHashtable = null;
        blockSizeOfBasisFileTable = null;
        basisFileChannelForReadTable = null;
        basisFileChannelForWriteTable = null;

        isNewFileCompletedHashtable = null;
        isUpdateFileCompletedHashtable = null;
        numNewFilesCompleted = 0;
        numUpdateFilesCompleted = 0;
    }

    public List<Path> getNewFileList() {
        return newFileList;
    }

    public List<CMFileSyncEntry> getFileEntryList() {
        return fileEntryList;
    }

    public List<Path> getBasisFileList() {
        return basisFileList;
    }

    public Hashtable<Integer, CMFileSyncBlockChecksum[]> getBlockChecksumArrayHashtable() {
        return blockChecksumArrayHashtable;
    }

    public Hashtable<Integer, Integer> getBasisFileIndexHashtable() {
        return basisFileIndexHashtable;
    }

    public Hashtable<Integer, Integer> getBlockSizeOfBasisFileTable() {
        return blockSizeOfBasisFileTable;
    }

    public Hashtable<Path, Boolean> getIsNewFileCompletedHashtable() {
        return isNewFileCompletedHashtable;
    }

    public void setIsNewFileCompletedHashtable(Hashtable<Path, Boolean> isNewFileCompletedHashtable) {
        this.isNewFileCompletedHashtable = isNewFileCompletedHashtable;
    }

    public Hashtable<Path, Boolean> getIsUpdateFileCompletedHashtable() {
        return isUpdateFileCompletedHashtable;
    }

    public void setIsUpdateFileCompletedHashtable(Hashtable<Path, Boolean> isUpdateFileCompletedHashtable) {
        this.isUpdateFileCompletedHashtable = isUpdateFileCompletedHashtable;
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

    @Override
    public void run() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.run() called..");
        }

        // get client file-entry-list
        fileEntryList = cmInfo.getFileSyncInfo().getFileEntryListHashtable().get(userName);
        if(CMInfo._CM_DEBUG) {
            System.out.println("fileEntryList = " + fileEntryList);
        }

        // create a basis file-entry-list at the server
        basisFileList = createBasisFileList();  // always return a list object
        if(CMInfo._CM_DEBUG) {
            System.out.println("basisFileList = " + basisFileList);
        }

        //// compare the client file-entry-list and the basis file-entry-list

        // delete files that exists only at the server and update the basisFileList
        deleteFilesAndUpdateBasisFileList();
        if(CMInfo._CM_DEBUG) {
            System.out.println("basisFileList after the deletion = " + basisFileList);
        }
        // create an isUpdateFileCompletedHashtable object
        if(!basisFileList.isEmpty()) isUpdateFileCompletedHashtable = new Hashtable<>();

        // create a new file-entry-list that will be added to the server
        newFileList = createNewFileList();
        if(newFileList == null) {
            System.err.println("CMFileSyncGenerator.run(), newFileList is null!");
            return;
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("newFileList = " + newFileList);
        }
        // create an isNewFileCompletedHashtable object
        if(!newFileList.isEmpty()) isNewFileCompletedHashtable = new Hashtable<>();

        // request the files in the new file-entry-list from the client
        boolean requestResult = requestTransferOfNewFiles();
        if(!requestResult) {
            System.err.println("request new files error!");
            return;
        }

        // update the files at the server by synchronizing with those at the client
        requestResult = compareBasisAndClientFileList();
        if(!requestResult) {
            System.err.println("compare-basis-and-client-file-list error!");
        }

        // check if the file-sync task is completed. (both client and server sync home are empty)
        CMFileSyncManager syncManager = (CMFileSyncManager) cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        if(syncManager.isCompleteFileSync(userName)) {
            syncManager.completeFileSync(userName);
        }
    }

    private boolean compareBasisAndClientFileList() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.compareBasisAndClientFileList() called..");
        }

        if(basisFileList == null) {
            System.err.println("basisFileList is null!");
            return false;
        }
        if(basisFileList.isEmpty()) {
            System.out.println("basisFileList is empty.");
            return true;
        }

        boolean sendResult;

        // get CMFileSyncManager object
        CMFileSyncManager syncManager = (CMFileSyncManager) cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        // get the server sync home
        Path serverSyncHome = syncManager.getServerSyncHome(userName);

        // check and create a basis-file-index hashtable
        if(basisFileIndexHashtable == null) {
            basisFileIndexHashtable = new Hashtable<>();
        }

        for(int basisFileIndex = 0; basisFileIndex < basisFileList.size(); basisFileIndex++) {
            Path basisFile = basisFileList.get(basisFileIndex);
            if(CMInfo._CM_DEBUG) {
                System.out.println("-----------------------------------");
                System.out.println("basisFileIndex = " + basisFileIndex);
                System.out.println("basisFile = " + basisFile);
            }
            // get relative path
            Path relativeBasisFile = basisFile.subpath(serverSyncHome.getNameCount(),
                    basisFile.getNameCount());
            // search for the client file entry
            CMFileSyncEntry clientFileEntry = null;
            int clientFileEntryIndex = -1;
            for(int i = 0; i < fileEntryList.size(); i++) {
                CMFileSyncEntry entry = fileEntryList.get(i);
                if(relativeBasisFile.equals(entry.getPathRelativeToHome())) {
                    clientFileEntry = entry;
                    clientFileEntryIndex = i;
                    break;
                }
            }
            if(clientFileEntry == null) {
                System.err.println("client file entry not found for basisFile("+relativeBasisFile+")!");
                continue;   // proceed to the next basis file
            }

            // add the index pair to the table
            basisFileIndexHashtable.put(clientFileEntryIndex, basisFileIndex);
            if(CMInfo._CM_DEBUG) {
                System.out.println("clientFileEntryIndex = " + clientFileEntryIndex);
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
            if(clientFileEntry.getSize() == sizeOfBasisFile &&
                    clientFileEntry.getLastModifiedTime().equals(lastModifiedTimeOfBasisFile)) {
                // already synchronized
                if(CMInfo._CM_DEBUG) {
                    System.out.println("basisFile("+basisFile+") skips synchronization.");
                    System.out.println("sizeOfBasisFile = " + sizeOfBasisFile);
                    System.out.println("lastModifiedTimeOfBasisFile = " + lastModifiedTimeOfBasisFile);
                }
                continue;
            }

            // get block checksum
            CMFileSyncBlockChecksum[] checksumArray = createBlockChecksum(basisFileIndex, basisFile);
            if(checksumArray == null) {
                System.err.println("checksum array is null!");
                continue;
            }
            if(CMInfo._CM_DEBUG) {
                System.out.println("checksum array size = "+checksumArray.length);
            }

            // add block-checksum array to the table
            if(blockChecksumArrayHashtable == null) {
                blockChecksumArrayHashtable = new Hashtable<>();
            }
            // key: client entry index, value: block-checksum array
            blockChecksumArrayHashtable.put(clientFileEntryIndex, checksumArray);

            // send the client entry index and block-checksum array to the client
            sendResult = sendBlockChecksum(clientFileEntryIndex, checksumArray);
            if(!sendResult) {
                System.err.println("send block-checksum error!");
                return false;
            }
        }

        return true;
    }

    private boolean sendBlockChecksum(int clientFileEntryIndex, CMFileSyncBlockChecksum[] checksumArray) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncGenerator.sendBlockChecksum() called..");
            System.err.println("TO be implemented..");
        }

        // TODO: from here
        return true;
    }

    private CMFileSyncBlockChecksum[] createBlockChecksum(int basisFileIndex, Path basisFile) {
        if(CMInfo._CM_DEBUG) {
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
        if(blockSizeOfBasisFileTable == null)
            blockSizeOfBasisFileTable = new Hashtable<>();
        blockSizeOfBasisFileTable.put(basisFileIndex, blockSize);

        // set the number of blocks
        int numBlocks = (int) (fileSize / blockSize);
        if(fileSize % blockSize > 0) numBlocks++;
        if(CMInfo._CM_DEBUG)
            System.out.println("numBlocks = " + numBlocks);

        // create a block-checksum array
        CMFileSyncBlockChecksum[] checksumArray = new CMFileSyncBlockChecksum[numBlocks];
        // get the file-sync manager
        CMFileSyncManager syncManager = (CMFileSyncManager) cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
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
        for(int i = 0; i < checksumArray.length; i++) {
            // create a block-checksum object that is set to the i-th element
            checksumArray[i] = new CMFileSyncBlockChecksum();
            // initialize the ByteBuffer
            buffer.clear();
            // read the i-th block from the channel to the buffer
            try {
                bytesRead = channel.read(buffer);
                if(CMInfo._CM_DEBUG) {
                    System.out.println("--------------");
                    System.out.println("block("+i+"), bytesRead = " + bytesRead);
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
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.calculateBlockSize() called..");
            System.out.println("fileSize = " + fileSize);
        }
        int blength;
        if(fileSize < CMFileSyncInfo.BLOCK_SIZE * CMFileSyncInfo.BLOCK_SIZE) {
            blength = CMFileSyncInfo.BLOCK_SIZE;
        }
        else {
            int c, cnt;
            long l;
            for(c = 1, l = fileSize, cnt = 0; (l >>= 2) > 0; c <<= 1, cnt++){}
            System.out.println("c="+c+", l="+l+", cnt="+cnt);
            if(c < 0 || c >= CMFileSyncInfo.MAX_BLOCK_SIZE)
                blength = CMFileSyncInfo.MAX_BLOCK_SIZE;
            else {
                blength = 0;
                do {
                    blength |= c;
                    if(fileSize < (long)blength*blength)
                        blength &= ~c;
                    c >>= 1;
                } while(c >= 8);    // round to multiple of 8
                blength = Math.max(blength, CMFileSyncInfo.BLOCK_SIZE);
            }
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("calculated block size = " + blength);
        }
        return blength;
    }

    private boolean requestTransferOfNewFiles() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.requestTransferOfNewFiles() called..");
        }

        if(newFileList == null) {
            System.err.println("newFileList is null!");
            return false;   // null is an error
        }
        if(newFileList.isEmpty()) {
            System.out.println("newFileList is empty.");
            return true;
        }

        int numRequestsCompleted = 0;
        boolean sendResult;
        while(numRequestsCompleted < newFileList.size()) {
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
            while(numRequestsCompleted < newFileList.size() && curByteNum < CMInfo.MAX_EVENT_SIZE) {
                Path path = newFileList.get(numRequestsCompleted);
                curByteNum += CMInfo.STRING_LEN_BYTES_LEN
                        + path.toString().getBytes().length;
                if(curByteNum < CMInfo.MAX_EVENT_SIZE) {
                    // increment the numRequestedFiles
                    numRequestedFiles++;
                    // add path to the requestedFileList
                    requestedFileList.add(path);
                    // increment the numRequestsCompleted
                    numRequestsCompleted++;
                }
                else
                    break;
            }
            // set numRequestedFiles and requestedFileList to the event
            fse.setNumRequestedFiles(numRequestedFiles);
            fse.setRequestedFileList(requestedFileList);
            // send the request event
            sendResult = CMEventManager.unicastEvent(fse, userName, cmInfo);
            if(!sendResult) {
                System.err.println("CMFileSyncGenerator.requestTransferOfNewFiles(), send error!");
                return false;
            }

            if(CMInfo._CM_DEBUG) {
                System.out.println("sent REQUEST_NEW_FILES event = " + fse);
            }
        }

        return true;
    }

    private List<Path> createNewFileList() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.createNewFileList() called..");
        }
        // check fileEntryList
        if(fileEntryList == null) {
            return new ArrayList<>();
        }
        // get the start path index
        CMFileSyncManager syncManager = (CMFileSyncManager) cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        Path serverSyncHome = syncManager.getServerSyncHome(userName);
        int startPathIndex = serverSyncHome.getNameCount();
        // get the relative path list from the basis file list
        List<Path> relativeBasisFileList = basisFileList.stream()
                .map(path -> path.subpath(startPathIndex, path.getNameCount()))
                .collect(Collectors.toList());
        // create a new file list that will be added to the server
        return fileEntryList.stream()
                .map(CMFileSyncEntry::getPathRelativeToHome)
                .filter(path -> !relativeBasisFileList.contains(path))
                .collect(Collectors.toList());
    }

    private void deleteFilesAndUpdateBasisFileList() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.deleteFilesOnlyAtServer() called..");
        }
        // get the client file-entry-list
        List<CMFileSyncEntry> fileEntryList = cmInfo.getFileSyncInfo().getFileEntryListHashtable().get(userName);
        if(fileEntryList == null) {
            System.out.println("fileEntryList of user("+userName+") is null!");
            // remove all files in the basisFileList
            basisFileList
                    .forEach(path -> {
                        try {
                            if(CMInfo._CM_DEBUG)
                                System.out.println("path = " + path);
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            // clear the basisFileList
            basisFileList.clear();
        }
        else {
            // get the client path list from the file-entry-list
            List<Path> entryPathList = fileEntryList.stream()
                    .map(CMFileSyncEntry::getPathRelativeToHome)
                    .collect(Collectors.toList());
            // get the CMFileSyncManager object
            CMFileSyncManager syncManager = (CMFileSyncManager) cmInfo.getServiceManagerHashtable()
                    .get(CMInfo.CM_FILE_SYNC_MANAGER);
            //// create target file list that exists only at the server and that will be deleted
            // get the server sync home and the start index
            Path serverSyncHome = syncManager.getServerSyncHome(userName);
            int startPathIndex = serverSyncHome.getNameCount();
            // create the deleted file list
            basisFileList.stream()
                    .filter(path -> !entryPathList.contains(path.subpath(startPathIndex, path.getNameCount())))
                    .forEach(path -> {
                        try {
                            if(CMInfo._CM_DEBUG)
                                System.out.println("path = " + path);
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            // update the basis file list
            basisFileList = basisFileList.stream()
                    .filter(path -> entryPathList.contains(path.subpath(startPathIndex, path.getNameCount())))
                    .collect(Collectors.toList());
        }
    }

    private List<Path> createBasisFileList() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncGenerator.createBasisFileList() called..");
        }
        // get the file sync manager
        CMFileSyncManager syncManager = (CMFileSyncManager) cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        if(syncManager == null) {
            System.err.println("CMFileSyncGenerator.createBasisFileList(), file-sync manager is null!");
            return null;
        }
        // get the server sync home
        Path serverSyncHome = syncManager.getServerSyncHome(userName);
        // check if the sync home exists or not
        if(Files.notExists(serverSyncHome)) {
            System.err.println("CMFileSyncGenerator.createBasisFileList(), the server sync-home does not exist!");
            return null;
        }
        // create a basis file list
        return syncManager.createPathList(serverSyncHome);
    }
}
