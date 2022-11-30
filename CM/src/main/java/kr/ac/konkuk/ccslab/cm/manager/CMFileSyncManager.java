package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.*;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncMode;
import kr.ac.konkuk.ccslab.cm.info.enums.CMTestFileModType;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncProactiveModeTask;
import kr.ac.konkuk.ccslab.cm.thread.CMWatchServiceTask;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CMFileSyncManager extends CMServiceManager {

    public CMFileSyncManager(CMInfo cmInfo) {
        super(cmInfo);
        m_nType = CMInfo.CM_FILE_SYNC_MANAGER;
    }

    public Path getClientSyncHome() {
        CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
        return confInfo.getTransferedFileHome().resolve(CMFileSyncInfo.SYNC_HOME)
                .toAbsolutePath().normalize();
    }

    public Path getServerSyncHome(String userName) {
        CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
        return confInfo.getTransferedFileHome().resolve(userName)
                .resolve(CMFileSyncInfo.SYNC_HOME).toAbsolutePath().normalize();
    }

    // currently called by client
    public synchronized boolean sync() {

        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.startFileSync() called..");

        CMFileSyncInfo fsInfo = m_cmInfo.getFileSyncInfo();

        if (fsInfo.isSyncInProgress()) {
            System.err.println("The file sync is in progress!");
            return false;
        } else {
            // set syncInProgress to true.
            fsInfo.setSyncInProgress(true);
        }

        // set file sync home.
        Path syncHome = getClientSyncHome();
        if (Files.notExists(syncHome)) {
            try {
                Files.createDirectories(syncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // create a path list in the sync-file-home.
        List<Path> pathList = createPathList(syncHome);
        // store the path list in the CMFileSyncInfo.
        fsInfo.setPathList(pathList);

        // update the online-mode-list
        //List<Path> onlineModeList = Objects.requireNonNull(fsInfo.getOnlineModePathList());
        List<Path> onlineModeList = fsInfo.getOnlineModePathSizeMap().keySet().stream().toList();
        Iterator<Path> iter = onlineModeList.iterator();
        while (iter.hasNext()) {
            Path onlinePath = iter.next();
            if (!pathList.contains(onlinePath)) {
                //iter.remove();
                fsInfo.getOnlineModePathSizeMap().remove(onlinePath);
            }
        }

        //boolean ret = saveOnlineModeListToFile();
        boolean ret = saveOnlineModePathSizeMapToFile();
        if (!ret) {
            System.err.println("error to save online-mode-list to file!");
        }

        // send the file list to the server
        boolean sendResult = sendFileList();
        if (!sendResult) {
            System.err.println("CMFileSyncManager.startFileSync(), error to send the file list.");
            return false;
        }

        return true;
    }

    public List<Path> createPathList(Path syncHome) {

        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.createPathList() called..");

        List<Path> pathList;
        try {
            // change to absolute path -> sorted -> change to a list
            pathList = Files.walk(syncHome)
                    .filter(path -> !path.equals(syncHome))
                    .map(path -> path.toAbsolutePath().normalize())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (pathList.isEmpty()) {
            if (CMInfo._CM_DEBUG) {
                System.err.println("CMFileSyncManager.createPathList(), The sync-home is empty.");
            }
        }

        if (CMInfo._CM_DEBUG) {
            for (Path p : pathList)
                System.out.println(p);
        }

        return pathList;
    }

    // currently called by client
    private boolean sendFileList() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.sendFileList() called..");

        String userName;
        String serverName;
        List<Path> pathList;

        // create START_FILE_LIST event.
        CMFileSyncEventStartFileList fse = new CMFileSyncEventStartFileList();
        // get my name
        userName = m_cmInfo.getInteractionInfo().getMyself().getName();
        fse.setSender(userName);
        // get default server name
        serverName = m_cmInfo.getInteractionInfo().getDefaultServerInfo().getServerName();
        fse.setReceiver(serverName);

        fse.setUserName(userName);
        // get path list
        pathList = m_cmInfo.getFileSyncInfo().getPathList();
        if (pathList == null)
            fse.setNumTotalFiles(0);
        else
            fse.setNumTotalFiles(pathList.size());

        // send the event
        boolean sendResult = CMEventManager.unicastEvent(fse, serverName, m_cmInfo);
        if (!sendResult) {
            System.err.println("CMFileSyncManager.sendFileList(), send error!");
            System.err.println(fse);
            return false;
        }
        return true;
    }

    // currently called by server
    public void checkNewTransferForSync(CMFileEvent fe) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.checkNewTransferForSync() called..");
            System.out.println("file event = " + fe);
        }
        // get the file name
        String fileName = fe.getFileName();
        // get the new file list
        String fileSender = fe.getFileSender();
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(fileSender);
        if (syncGenerator == null) {
            if(CMInfo._CM_DEBUG)
                System.err.println("The sync generator for (" + fileSender + ") is null!");
            return;
        }
        List<CMFileSyncEntry> newClientPathEntryList = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap()
                .get(fileSender).getNewClientPathEntryList();
        Objects.requireNonNull(newClientPathEntryList);

        // search for the entry in the newClientPathEntryList
        CMFileSyncEntry foundEntry = null;
        Path foundPath = null;
        for (CMFileSyncEntry entry : newClientPathEntryList) {
            if (entry.getPathRelativeToHome().endsWith(fileName)) {
                foundEntry = entry;
                foundPath = entry.getPathRelativeToHome();
                break;
            }
        }
        if (foundPath != null) {
            // get the file-transfer home
            Path transferFileHome = m_cmInfo.getConfigurationInfo().getTransferedFileHome().resolve(fileSender);
            // get the server sync home
            Path serverSyncHome = getServerSyncHome(fileSender);
            // move the transferred file to the sync home (including sub-directories)
            try {
                //Files.move(transferFileHome.resolve(fileName), serverSyncHome.resolve(foundPath));
                Files.move(transferFileHome.resolve(fileName), serverSyncHome.resolve(foundPath),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // set the last-modified-time of the corresponding client file entry
            try {
                Files.setLastModifiedTime(serverSyncHome.resolve(foundPath), foundEntry.getLastModifiedTime());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // complete the new-file-transfer
            boolean result = completeNewFileTransfer(fileSender, foundPath);
            if (result) {
                // remove the completed newClientPathEntry from the list
                final Path finalFoundPath = foundPath;
                boolean removeResult = newClientPathEntryList.removeIf(entry ->
                        entry.getPathRelativeToHome().equals(finalFoundPath));

                if (!removeResult) {
                    System.err.println("remove error from the new-client-path-entry-list: " + foundPath);
                    return;
                }
                // check if the file-sync is complete or not
                if (isCompleteFileSync(fileSender)) {
                    // complete the file-sync task
                    completeFileSync(fileSender);
                }
            }
        }
    }

    // called by the server
    public boolean completeNewFileTransfer(String userName, Path path) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completeNewFileTransfer() called..");
            System.out.println("userName = " + userName);
            System.out.println("path = " + path);
        }
        // get CMFileSyncGenerator
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(userName);
        if (syncGenerator == null) {
            System.err.println("syncGenerator is null!");
            return false;
        }
        // set the isNewFileCompletedHashMap element
        syncGenerator.getIsNewFileCompletedMap().put(path, true);
        // update numNewFilesCompleted
        int numNewFilesCompleted = syncGenerator.getNumNewFilesCompleted();
        numNewFilesCompleted++;
        syncGenerator.setNumNewFilesCompleted(numNewFilesCompleted);

        // create a COMPLETE_NEW_FILE event
        String serverName = m_cmInfo.getInteractionInfo().getMyself().getName();
        CMFileSyncEventCompleteNewFile fse = new CMFileSyncEventCompleteNewFile();
        fse.setSender(serverName);
        fse.setReceiver(userName);
        fse.setUserName(userName);
        fse.setCompletedPath(path);

        // send the event
        boolean ret = CMEventManager.unicastEvent(fse, userName, m_cmInfo);
        if (!ret) {
            System.err.println("send error: " + fse);
            return false;
        }

        return true;
    }

    // called at the server
    public boolean skipUpdateFile(String userName, Path basisFile) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.skipUpdateFile() called..");
            System.out.println("userName = " + userName);
            System.out.println("basisFile = " + basisFile);
        }
        // get CMFileSyncGenerator
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(userName);
        Objects.requireNonNull(syncGenerator);
        // set the isUpdateFileCompletedMap element
        syncGenerator.getIsUpdateFileCompletedMap().put(basisFile, true);
        // update numUpdateFilesCompleted
        int numUpdateFilesCompleted = syncGenerator.getNumUpdateFilesCompleted();
        numUpdateFilesCompleted++;
        syncGenerator.setNumUpdateFilesCompleted(numUpdateFilesCompleted);

        // create a SKIP_UPDATE_FILE event
        String serverName = m_cmInfo.getInteractionInfo().getMyself().getName();
        CMFileSyncEventSkipUpdateFile fse = new CMFileSyncEventSkipUpdateFile();
        fse.setSender(serverName);
        fse.setReceiver(userName);
        fse.setUserName(userName);

        // get the relative path of the basis file path
        Path syncHome = getServerSyncHome(userName);
        Path relativePath = basisFile.subpath(syncHome.getNameCount(), basisFile.getNameCount());
        // set the relative path to the event
        fse.setSkippedPath(relativePath);

        return CMEventManager.unicastEvent(fse, userName, m_cmInfo);
    }

    // called by the server
    public boolean completeUpdateFile(String userName, Path path) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completeUpdateFile() called..");
            System.out.println("userName = " + userName);
            System.out.println("path = " + path);
        }
        // get CMFileSyncGenerator
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(userName);
        Objects.requireNonNull(syncGenerator);
        // set the isUpdateFileCompletedMap element
        syncGenerator.getIsUpdateFileCompletedMap().put(path, true);
        // update numUpdateFilesCompleted
        int numUpdateFilesCompleted = syncGenerator.getNumUpdateFilesCompleted();
        numUpdateFilesCompleted++;
        syncGenerator.setNumUpdateFilesCompleted(numUpdateFilesCompleted);

        // create a COMPLETE_UPDATE_FILE event
        String serverName = m_cmInfo.getInteractionInfo().getMyself().getName();
        CMFileSyncEventCompleteUpdateFile fse = new CMFileSyncEventCompleteUpdateFile();
        fse.setSender(serverName);
        fse.setReceiver(userName);
        fse.setUserName(userName);

        // get the relative path of the basis file path
        Path syncHome = getServerSyncHome(userName);
        Path relativePath = path.subpath(syncHome.getNameCount(), path.getNameCount());
        // set the relative path to the event
        fse.setCompletedPath(relativePath);

        return CMEventManager.unicastEvent(fse, userName, m_cmInfo);
    }

    // called by the server
    public boolean isCompleteFileSync(String userName) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.isCompleteFileSync() called..");
            System.out.println("userName = " + userName);
        }

        List<CMFileSyncEntry> newClientPathEntryList = null;
        List<Path> basisFileList = null;
        List<CMFileSyncEntry> fileEntryList = null;
        int numNewFilesCompleted = 0;
        int numUpdateFilesCompleted = 0;
        int numFilesCompleted = 0;
        int numNewFilesNotCompleted = 0;
        int numUpdateFilesNotCompleted = 0;
        Map<Path, Boolean> isNewFileCompletedMap = null;
        Map<Path, Boolean> isUpdateFileCompletedMap = null;

        // get CMFileSyncGenerator object
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(userName);
        if (syncGenerator == null) {
            System.err.println("syncGenerator is null!");
            return false;
        }

        // compare the number of new files completed to the size of the new-file list
        newClientPathEntryList = syncGenerator.getNewClientPathEntryList();
        numNewFilesCompleted = syncGenerator.getNumNewFilesCompleted();
        if (!newClientPathEntryList.isEmpty()) {
            if (CMInfo._CM_DEBUG) {
                System.err.println("numNewFilesCompleted = " + numNewFilesCompleted);
                System.err.println("size of newClientPathEntryList = " + newClientPathEntryList.size());
            }
            return false;
        }
        // get basis file list
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        basisFileList = Objects.requireNonNull(syncInfo.getBasisFileListMap()).get(userName);
        // compare the number of updated files to the size of the basis-file list
        numUpdateFilesCompleted = syncGenerator.getNumUpdateFilesCompleted();
        if (basisFileList != null && numUpdateFilesCompleted < basisFileList.size()) {
            if (CMInfo._CM_DEBUG) {
                System.err.println("numUpdateFilesCompleted = " + numUpdateFilesCompleted);
                System.err.println("size of basisFileList = " + basisFileList.size());
            }
            return false;
        }
        // compare the number of files of which sync is completed to the size of client file-entry list
        fileEntryList = m_cmInfo.getFileSyncInfo().getClientPathEntryListMap().get(userName);
        numFilesCompleted = numNewFilesCompleted + numUpdateFilesCompleted;
        if (fileEntryList != null && numFilesCompleted < fileEntryList.size()) {
            System.err.println("numFilesCompleted = " + numFilesCompleted);
            System.err.println("size of client file-entry list = " + fileEntryList.size());
            return false;
        }
        // check each element of the isNewFileCompletedMap
        isNewFileCompletedMap = syncGenerator.getIsNewFileCompletedMap();
        numNewFilesNotCompleted = 0;
        for (Map.Entry<Path, Boolean> entry : isNewFileCompletedMap.entrySet()) {
            Path k = entry.getKey();
            Boolean v = entry.getValue();
            if (!v) {
                numNewFilesNotCompleted++;
                System.err.println("new file path='" + k + '\'' + ", value=" + v);
            }
        }
        if (numNewFilesNotCompleted > 0) {
            System.err.println("numNewFilesNotCompleted = " + numNewFilesNotCompleted);
            return false;
        }
        // check each element of the isUpdateFileCompletedMap
        isUpdateFileCompletedMap = syncGenerator.getIsUpdateFileCompletedMap();
        numUpdateFilesNotCompleted = 0;
        for (Map.Entry<Path, Boolean> entry : isUpdateFileCompletedMap.entrySet()) {
            Path k = entry.getKey();
            Boolean v = entry.getValue();
            if (!v) {
                numUpdateFilesNotCompleted++;
                System.err.println("update file path='" + k + '\'' + ", value=" + v);
            }
        }
        if (numUpdateFilesNotCompleted > 0) {
            System.err.println("numUpdateFilesNotCompleted = " + numUpdateFilesNotCompleted);
            return false;
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("The sync of all files is completed.");
        }

        return true;
    }

    // called by the server
    public boolean completeFileSync(String userName) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completeFileSync() called..");
            System.out.println("userName = " + userName);
        }
        // send the file-sync completion event
        boolean result = true;
        result = sendCompleteFileSync(userName);
        if (!result) return false;
        deleteFileSyncInfo(userName);
        return true;
    }

    // called by the server
    private boolean sendCompleteFileSync(String userName) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.sendCompleteFileSync() called..");
            System.out.println("userName = " + userName);
        }

        // get the CMFileSyncGenerator reference
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(userName);
        if (syncGenerator == null) {
            System.err.println("syncGenerator is null!");
            return false;
        }

        // create a COMPLETE_FILE_SYNC event
        String serverName = m_cmInfo.getInteractionInfo().getMyself().getName();
        int numFilesCompleted = syncGenerator.getNumNewFilesCompleted() + syncGenerator.getNumUpdateFilesCompleted();

        CMFileSyncEventCompleteFileSync fse = new CMFileSyncEventCompleteFileSync();
        fse.setSender(serverName);
        fse.setReceiver(userName);
        fse.setUserName(userName);
        fse.setNumFilesCompleted(numFilesCompleted);

        // send the event
        return CMEventManager.unicastEvent(fse, userName, m_cmInfo);
    }

    // called by the server
    private void deleteFileSyncInfo(String userName) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.deleteFileSyncInfo() called..");
            System.out.println("userName = " + userName);
        }
        // get CMFileSyncInfo reference
        CMFileSyncInfo syncInfo = m_cmInfo.getFileSyncInfo();
        // remove element in fileEntryListMap
        syncInfo.getClientPathEntryListMap().remove(userName);
        // remove element in syncGeneratorMap
        syncInfo.getSyncGeneratorMap().remove(userName);
    }

    // called by the client
/*
    public void deleteFileSyncInfo() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.deleteFileSyncInfo() called..");
        }
        // get CMFileSyncInfo reference
        CMFileSyncInfo syncInfo = m_cmInfo.getFileSyncInfo();
        // initialize the pathList
        syncInfo.setPathList(null);
        // clear the isFileSyncCompletedMap
        syncInfo.getIsFileSyncCompletedMap().clear();
    }
*/

    // called by the server
    public int calculateWeakChecksum(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateWeakChecksum() called..");
            System.out.println("ByteBuffer remaining size = " + buffer.remaining());
        }
        int[] abs = calculateWeakChecksumElements(buffer);

        if (CMInfo._CM_DEBUG) {
            System.out.println("weak checksum = " + abs[2]);
        }
        return abs[2];
    }

    // called by the client
    // reference: http://tutorials.jenkov.com/rsync/checksums.html
    public int[] calculateWeakChecksumElements(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (CMInfo._CM_DEBUG_2) {
            System.out.println("=== CMFileSyncManager.calculateWeakChecksumElements() called..");
            System.out.println("ByteBuffer remaining size = " + buffer.remaining());
        }

        int A = 0;
        int B = 0;
        int S = 0;
        int[] abs = new int[3]; // abs[0] = A, abs[1] = B, abs[2] = S
        int M = (int) Math.pow(2.0, 16.0);
        if (CMInfo._CM_DEBUG_2) {
            System.out.println("initial A = " + A + ", B = " + B + ", S = " + S);
            System.out.println("M = " + M);
            System.out.print("initial abs = ");
            for (int e : abs) System.out.print(e + " ");
            System.out.println();
        }

        // repeat to update A and B for each block data
        while (buffer.hasRemaining()) {
            A += buffer.get();
            B += A;
        }
        // get mod M value of A and B
        A = A % M;
        B = B % M;
        abs[0] = A;
        abs[1] = B;
        // get checksum (S) based on A and B
        S = A + M * B;
        abs[2] = S;
        if (CMInfo._CM_DEBUG_2) {
            System.out.println("A = " + A + ", B = " + B + ", S = " + S);
            System.out.println("abs = " + Arrays.toString(abs));
        }

        return abs;
    }

    public byte[] calculateStrongChecksum(ByteBuffer buffer) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateStrongChecksum() called..");
            System.out.println("ByteBuffer remaining size = " + buffer.remaining());
        }
        // get MD5
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        //md.update(buffer.array());
        md.update(buffer);
        byte[] digest = md.digest();

        if (CMInfo._CM_DEBUG) {
            String checksum = DatatypeConverter.printHexBinary(digest).toUpperCase();
            System.out.println("checksum hex binary = " + checksum);
            System.out.println("checksum array string = " + Arrays.toString(digest));
            System.out.println("length = " + digest.length + "bytes.");
        }

        return digest;
    }

    public byte[] calculateFileChecksum(Path path) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateFileChecksum() called..");
            System.out.println("path = " + path);
        }
        // get MD5
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path.toFile());
            byte[] byteArray = new byte[8192];
            int bytesCount = 0;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                md.update(byteArray, 0, bytesCount);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] bytes = md.digest();

        if (CMInfo._CM_DEBUG) {
            String checksum = DatatypeConverter.printHexBinary(bytes).toUpperCase();
            System.out.println("checksum = " + checksum);
        }

        return bytes;
    }

    // called by the client
    public int[] updateWeakChecksum(int oldA, int oldB, byte oldStartByte, byte newEndByte, int blockSize) {
        if (CMInfo._CM_DEBUG_2) {
            System.out.println("=== CMFileSyncManager.updateWeakChecksum() called..");
            System.out.println("oldA = " + oldA);
            System.out.println("oldB = " + oldB);
            System.out.println("oldStartByte = " + oldStartByte);
            System.out.println("newEndByte = " + newEndByte);
            System.out.println("blockSize = " + blockSize);
        }
        // calculate rolling checksum from the previous checksum value
        int A, B, S;
        int M = (int) Math.pow(2.0, 16.0);
        int[] newABS = new int[3];

        A = oldA;
        A -= oldStartByte;
        A += newEndByte;
        A %= M;

        B = oldB;
        B -= blockSize * oldStartByte;
        B += A;
        B %= M;

        S = A + M * B;

        newABS[0] = A;
        newABS[1] = B;
        newABS[2] = S;

        if (CMInfo._CM_DEBUG_2) {
            System.out.println("A = " + A + ", B = " + B + ", S = " + S);
            System.out.println("newABS = " + Arrays.toString(newABS));
        }

        return newABS;
    }

    // calculate a checksum of a file (that is the sum of block weak checksum values)
    // The server will validate the newly created file with this file checksum.
    public int calculateWeakChecksum(Path path, int blockSize) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateWeakChecksum(Path, int) called..");
            System.out.println("path = " + path);
            System.out.println("blockSize = " + blockSize);
        }
        // assign a ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        // assign related variables
        int[] weakChecksumABS;
        int fileChecksum = 0;
        int M = (int) Math.pow(2.0, 16.0);
        SeekableByteChannel channel = null;
        try {
            // open the file channel
            channel = Files.newByteChannel(path, StandardOpenOption.READ);
            // repeat to calculate a block checksum and add it to the file checksum value
            while (channel.position() < channel.size()) {
                // read the next block of the file and write to the buffer
                buffer.clear();
                channel.read(buffer);
                // calculate the weak checksum of the block
                buffer.flip();
                weakChecksumABS = calculateWeakChecksumElements(buffer);
                // add the block checksum to the current file checksum value
                fileChecksum += weakChecksumABS[2];
                fileChecksum %= M;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("fileChecksum = " + fileChecksum);
        }

        return fileChecksum;
    }

    public Path getTempPathOfBasisFile(Path basisFilePath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getTempPathOfBasisFile() called..");
            System.out.println("basisFilePath = " + basisFilePath);
        }

        String fileName = basisFilePath.getFileName().toString();
        String tempFileName = CMInfo.TEMP_FILE_PREFIX + fileName;
        Path tempBasisFilePath = basisFilePath.resolveSibling(tempFileName);

        if (CMInfo._CM_DEBUG) {
            System.out.println("tempBasisFilePath = " + tempBasisFilePath);
        }

        return tempBasisFilePath;
    }

    public boolean startWatchService() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startWatchService() called..");
        }
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        // check if the WatchService is already started
        if (!syncInfo.isWatchServiceTaskDone()) {
            System.out.println("The watch service is already running..");
            return true;
        }

        // get ExecutorService reference
        ExecutorService es = m_cmInfo.getThreadInfo().getExecutorService();
        Objects.requireNonNull(es);
        // create WatchService and store the reference
        final WatchService watchService;
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        syncInfo.setWatchService(watchService);

        // check sync home directory
        Path syncHome = Objects.requireNonNull(getClientSyncHome());
        if (Files.notExists(syncHome)) {
            try {
                Files.createDirectories(syncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        // create a WatchServiceTask
        CMWatchServiceTask watchTask = new CMWatchServiceTask(syncHome, watchService, this, syncInfo);
        // start the WatchServiceTask
        Future<?> future = es.submit(watchTask);
        if (future == null) {
            System.err.println("error submitting watch-service task to the ExecutorService!");
            return false;
        }
        // store the Future<?> to the syncInfo
        syncInfo.setWatchServiceFuture(future);

        return true;
    }

    public boolean stopWatchService() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.stopWatchService() called..");
        }

        // get syncInfo
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());

        // check if the watch service is already done
        if (syncInfo.isWatchServiceTaskDone()) {
            System.out.println("The watch service has already stopped..");
            return true;
        }

        // get WatchService reference
        WatchService watchService = syncInfo.getWatchService();
        if (watchService == null) {
            System.err.println("WatchService reference is null!");
            return false;
        }
        // stop the WatchService
        try {
            watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // get the Future<?> reference of the watch-service task
        Future<?> watchFuture = syncInfo.getWatchServiceFuture();
        if (watchFuture == null) {
            System.err.println("Future<?> of the watch-service task is null!");
            return false;
        }
        // wait until the task is done
        try {
            watchFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
            return false;
        }
        // check if the watch-service task is done in the ExecutorService
        if (!syncInfo.isWatchServiceTaskDone()) {
            System.err.println("The watch-service task is not done!");
            return false;
        }
        // initialize the WatchService and WatchService task references
        syncInfo.setWatchService(null);
        syncInfo.setWatchServiceFuture(null);

        return true;
    }

    public boolean requestOnlineMode(List<Path> pathList) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.requestOnlineMode() called..");
            System.out.println("pathList = " + pathList);
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        // check current file-sync mode
        if (syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.err.println("Current file-sync mode is OFF!");
            return false;
        }

        // check if current file-sync status
        if (syncInfo.isSyncInProgress()) {
            System.err.println("Currently file-sync task is working! You should wait!");
            return false;
        }
        // check if the watch service is running
        if (syncInfo.isWatchServiceTaskDone()) {
            System.err.println("The file-sync monitoring stops! You should start the file-sync!");
            return false;
        }

        // check argument
        if (pathList == null) {
            System.err.println("The argument pathList is null!");
            return false;
        }

        // change the file-sync status
        syncInfo.setSyncInProgress(true);
        // stop the watch service
        boolean ret = stopWatchService();
        if (!ret) {
            System.err.println("error stopping WatchService!");
            return false;
        }

        //// extending all directory paths in the path list to file paths

        // extracting file-only list in the argument path list
        List<Path> fileOnlyList = pathList.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());
        // extracting directory-only list in the argument path list
        List<Path> dirOnlyList = pathList.stream()
                .filter(path -> Files.isDirectory(path))
                .collect(Collectors.toList());
        // extend each directory to a list of files and added to the file-only list
        ret = false;
        for (Path dir : dirOnlyList) {
            try {
                List<Path> fileList = Files.walk(dir)
                        .filter(path -> !Files.isDirectory(path))
                        .collect(Collectors.toList());
                if (!fileList.isEmpty()) {
                    ret = fileOnlyList.addAll(fileList);
                    if (!ret) {
                        System.err.println("error to add files in " + dir);
                        continue;
                    }
                    if (CMInfo._CM_DEBUG) {
                        System.out.println("files in " + dir + " added to the file-only list");
                        for (Path path : fileList)
                            System.out.println("path = " + path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("file only list: ");
            for (Path path : fileOnlyList)
                System.out.println("path = " + path);
        }

        /*// take out path element that is already the online mode in the file-only list
        List<Path> onlineModePathList = Objects.requireNonNull(syncInfo.getOnlineModePathList());
        List<Path> filteredFileOnlyList = fileOnlyList.stream()
                .filter(path -> !onlineModePathList.contains(path))
                .collect(Collectors.toList());

        if(filteredFileOnlyList.isEmpty()) {
            System.err.println("Selected files are already online mode ones!");
            return false;
        }

        if(CMInfo._CM_DEBUG) {
            System.out.println("filtered file only list: ");
            for(Path path : filteredFileOnlyList)
                System.out.println("path = " + path);
        }*/

        //// create and send an online-mode-list event

        // get the user and server names
        String userName = m_cmInfo.getInteractionInfo().getMyself().getName();
        Objects.requireNonNull(userName);
        String serverName = m_cmInfo.getInteractionInfo().getDefaultServerInfo().getServerName();
        Objects.requireNonNull(serverName);
        // event transmission loop
        int listIndex = 0;
        boolean sendResult = false;
        while (listIndex < fileOnlyList.size()) {
            // create an event
            CMFileSyncEventOnlineModeList listEvent = new CMFileSyncEventOnlineModeList();
            listEvent.setSender(userName);
            listEvent.setReceiver(serverName);
            listEvent.setRequester(userName);

            // get relative path list to be added to this event
            List<Path> subList = createSubOnlineModeListForEvent(listEvent, fileOnlyList, listIndex);
            // update the listIndex
            listIndex += subList.size();
            // set the sublist to the event
            listEvent.setRelativePathList(subList);
            // send the event
            sendResult = CMEventManager.unicastEvent(listEvent, serverName, m_cmInfo);
            if (!sendResult) {
                System.err.println("send error: " + listEvent);
                return false;
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("sent listEvent = " + listEvent);
            }
        }

        // add filteredFileOnlyList to the online-mode-request queue
        ConcurrentLinkedQueue<Path> onlineModeRequestQueue = syncInfo.getOnlineModeRequestQueue();
        Objects.requireNonNull(onlineModeRequestQueue);
        ret = onlineModeRequestQueue.addAll(fileOnlyList);
        if (!ret) {
            System.err.println("error to add filteredFileOnlyList to the online-mode-request queue!");
            return false;
        }

        return true;
    }

    // From the starting index (listIndex) of filteredFileOnlyList,
    // create a sublist that will be added to an online-mode-list event (listEvent)
    private List<Path> createSubOnlineModeListForEvent(CMFileSyncEventOnlineModeList listEvent,
                                                       List<Path> filteredFileOnlyList, int listIndex) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.createSubOnlineModeListForEvent() called..");
            System.out.println("listEvent = " + listEvent);
            System.out.println("filteredFileOnlyList = " + filteredFileOnlyList);
            System.out.println("listIndex = " + listIndex);
        }
        // get the name count of the sync home path
        int startPathIndex = getClientSyncHome().getNameCount();
        // get the current size of the given event
        int curByteNum = listEvent.getByteNum();
        // create an empty sublist
        List<Path> subList = new ArrayList<>();

        boolean ret = false;
        for (int i = listIndex; i < filteredFileOnlyList.size(); i++) {
            // get the relative path of the i-th element
            Path path = filteredFileOnlyList.get(i);
            Path relativePath = path.subpath(startPathIndex, path.getNameCount());
            // check the size of the relative path and add it to the event
            curByteNum += CMInfo.STRING_LEN_BYTES_LEN + relativePath.toString().getBytes().length;
            if (curByteNum < CMInfo.MAX_EVENT_SIZE) {
                ret = subList.add(relativePath);
                if (!ret) {
                    System.err.println("error to add " + relativePath);
                    return null;
                }
            } else
                break;
        }
        return subList;
    }

    public boolean requestLocalMode(List<Path> pathList) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.requestLocalMode() called..");
            System.out.println("pathList = " + pathList);
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        // check current file-sync mode
        if (syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.err.println("Current file-sync mode is OFF!");
            return false;
        }

        // check if current file-sync status
        if (syncInfo.isSyncInProgress()) {
            System.err.println("Currently file-sync task is working! You should wait!");
            return false;
        }
        // check if the watch service is running
        if (syncInfo.isWatchServiceTaskDone()) {
            System.err.println("The file-sync monitoring stops! You should start the file-sync!");
            return false;
        }

        // check argument
        if (pathList == null) {
            System.err.println("The argument pathList is null!");
            return false;
        }

        // change the file-sync status
        syncInfo.setSyncInProgress(true);
        // stop the watch service
        boolean ret = stopWatchService();
        if (!ret) {
            System.err.println("error stopping WatchService!");
            return false;
        }

        //// extending all directory paths in the path list to file paths

        // extracting file-only list in the argument path list
        List<Path> fileOnlyList = pathList.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());
        // extracting directory-only list in the argument path list
        List<Path> dirOnlyList = pathList.stream()
                .filter(path -> Files.isDirectory(path))
                .collect(Collectors.toList());
        // extend each directory to a list of files and added to the file-only list
        ret = false;
        for (Path dir : dirOnlyList) {
            try {
                List<Path> fileList = Files.walk(dir)
                        .filter(path -> !Files.isDirectory(path))
                        .collect(Collectors.toList());
                if (!fileList.isEmpty()) {
                    ret = fileOnlyList.addAll(fileList);
                    if (!ret) {
                        System.err.println("error to add files in " + dir);
                        continue;
                    }
                    if (CMInfo._CM_DEBUG) {
                        System.out.println("files in " + dir + " added to the file-only list");
                        for (Path path : fileList)
                            System.out.println("path = " + path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("file only list: ");
            for (Path path : fileOnlyList)
                System.out.println("path = " + path);
        }
/*
        // take out path element that is already the local mode in the file-only list
        // The local mode files are not in the online mode list.
        List<Path> onlineModePathList = Objects.requireNonNull(syncInfo.getOnlineModePathList());
        List<Path> filteredFileOnlyList = fileOnlyList.stream()
                .filter(path -> onlineModePathList.contains(path))
                .collect(Collectors.toList());

        if(filteredFileOnlyList.isEmpty()) {
            System.err.println("Selected files are already local mode ones!");
            return false;
        }

        if(CMInfo._CM_DEBUG) {
            System.out.println("filtered file only list: ");
            for(Path path : filteredFileOnlyList)
                System.out.println("path = " + path);
        }
*/

        //// create and send a local-mode-list event

        // get the user and server names
        String userName = m_cmInfo.getInteractionInfo().getMyself().getName();
        Objects.requireNonNull(userName);
        String serverName = m_cmInfo.getInteractionInfo().getDefaultServerInfo().getServerName();
        Objects.requireNonNull(serverName);
        // event transmission loop
        int listIndex = 0;
        boolean sendResult = false;
        while (listIndex < fileOnlyList.size()) {
            // create an event
            CMFileSyncEventLocalModeList listEvent = new CMFileSyncEventLocalModeList();
            listEvent.setSender(userName);
            listEvent.setReceiver(serverName);
            listEvent.setRequester(userName);

            // get relative path list to be added to this event
            List<Path> subList = createSubLocalModeListForEvent(listEvent, fileOnlyList, listIndex);
            // update the listIndex
            listIndex += subList.size();
            // set the sublist to the event
            listEvent.setRelativePathList(subList);
            // send the event
            sendResult = CMEventManager.unicastEvent(listEvent, serverName, m_cmInfo);
            if (!sendResult) {
                System.err.println("send error: " + listEvent);
                return false;
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("sent listEvent = " + listEvent);
            }
        }

        // add filteredFileOnlyList to the local-mode-request queue
        ConcurrentLinkedQueue<Path> localModeRequestQueue = syncInfo.getLocalModeRequestQueue();
        Objects.requireNonNull(localModeRequestQueue);
        ret = localModeRequestQueue.addAll(fileOnlyList);
        if (!ret) {
            System.err.println("error to add filteredFileOnlyList to the local-mode-request queue!");
            return false;
        }

        return true;
    }

    // From the starting index (listIndex) of filteredFileOnlyList,
    // create a sublist that will be added to a local-mode-list event (listEvent)
    private List<Path> createSubLocalModeListForEvent(CMFileSyncEventLocalModeList listEvent, List<Path> filteredFileOnlyList, int listIndex) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.createSubLocalModeListForEvent() called..");
            System.out.println("listEvent = " + listEvent);
            System.out.println("filteredFileOnlyList = " + filteredFileOnlyList);
            System.out.println("listIndex = " + listIndex);
        }
        // get the name count of the sync home path
        int startPathIndex = getClientSyncHome().getNameCount();
        // get the current size of the given event
        int curByteNum = listEvent.getByteNum();
        // create an empty sublist
        List<Path> subList = new ArrayList<>();

        boolean ret = false;
        for (int i = listIndex; i < filteredFileOnlyList.size(); i++) {
            // get the relative path of the i-th element
            Path path = filteredFileOnlyList.get(i);
            Path relativePath = path.subpath(startPathIndex, path.getNameCount());
            // check the size of the relative path and add it to the event
            curByteNum += CMInfo.STRING_LEN_BYTES_LEN + relativePath.toString().getBytes().length;
            if (curByteNum < CMInfo.MAX_EVENT_SIZE) {
                ret = subList.add(relativePath);
                if (!ret) {
                    System.err.println("error to add " + relativePath);
                    return null;
                }
            } else
                break;
        }
        return subList;
    }

    // called at the client
    public void checkTransferForLocalMode(CMFileEvent fe) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.checkTransferForLocalMode() called..");
            System.out.println("fe = " + fe);
        }

        // get transferred file info
        String fileName = fe.getFileName();
        String fileSender = fe.getFileSender();
        Path transferFileHome = Objects.requireNonNull(m_cmInfo.getConfigurationInfo().getTransferedFileHome());

        // get local-mode-request queue
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        ConcurrentLinkedQueue<Path> localModeRequestQueue = syncInfo.getLocalModeRequestQueue();
        Objects.requireNonNull(localModeRequestQueue);

        // compare the queue head (absolute path) and the transferred file name
        Path headPath = localModeRequestQueue.peek();
        if (headPath == null) {
            if (CMInfo._CM_DEBUG) {
                System.err.println("Local-mode-request queue is empty!");
            }
            return;
        }
        if (!headPath.endsWith(fileName)) {
            System.err.println("Head of local-mode-request queue does not match the transferred file name!");
            System.out.println("headPath = " + headPath);
            System.out.println("fileName = " + fileName);
            return;
        }

        try {
            // save the last modified time of the queue head path
            FileTime lastModifiedTime = Files.getLastModifiedTime(headPath);
            // move the transferred file to the sync home
            Files.move(transferFileHome.resolve(fileName), headPath, StandardCopyOption.REPLACE_EXISTING);
            // restore the last modified time
            Files.setLastModifiedTime(headPath, lastModifiedTime);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // delete head from the request queue
        localModeRequestQueue.remove();
        // delete path from the online-mode-list
/*
        List<Path> onlineModeList = Objects.requireNonNull(syncInfo.getOnlineModePathList());
        boolean ret = onlineModeList.remove(headPath);
        if (!ret) {
            System.err.println("remove error from the online-mode-list: " + headPath);
        }
*/
        // delete path from the online-mode-map
        Map<Path, Long> onlineModePathSizeMap = syncInfo.getOnlineModePathSizeMap();
        Objects.requireNonNull(onlineModePathSizeMap);
        Long size = onlineModePathSizeMap.remove(headPath);
        if (size == null) {
            System.err.println("remove error from the online-mode-map: " + headPath);
        }

        // check if the queue is not empty
        if (!localModeRequestQueue.isEmpty()) {
            System.out.println("Local-mode-request queue is not empty.");
            return;
        }
        // if the queue is empty, create and send an end-local-mode-list event
        CMFileSyncEventEndLocalModeList endEvent = new CMFileSyncEventEndLocalModeList();
        endEvent.setSender(fe.getFileReceiver());
        endEvent.setReceiver(fileSender);
        endEvent.setRequester(fe.getFileReceiver());

        // filter only file type from the path list
        List<Path> pathList = Objects.requireNonNull(syncInfo.getPathList());
        List<Path> filteredPathList = pathList.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());
        // get the number of local-mode files
        //int numLocalModeFiles = filteredPathList.size() - syncInfo.getOnlineModePathList().size();
        int numLocalModeFiles = filteredPathList.size() - syncInfo.getOnlineModePathSizeMap().size();
        endEvent.setNumLocalModeFiles(numLocalModeFiles);

        boolean ret = CMEventManager.unicastEvent(endEvent, fileSender, m_cmInfo);
        if (!ret) {
            System.err.println("send error: " + endEvent);
        }

        return;
    }

    // called at the client
    public boolean startFileSync(CMFileSyncMode fileSyncMode) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startFileSync() called..");
            System.out.println("fileSyncMode = " + fileSyncMode);
        }

        // check the argument
        if (fileSyncMode == CMFileSyncMode.OFF) {
            System.err.println("The argument file-sync mode is OFF!");
            return false;
        }

        // get CMFileSyncInfo reference
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());

        // check the current file-sync mode
        if (syncInfo.getCurrentMode() != CMFileSyncMode.OFF) {
            System.err.println("Current file-sync mode (" + syncInfo.getCurrentMode() + ") has already started!");
            return false;
        }

        // get the online-mode-list-file path
        //Path storedListPath = Paths.get(CMInfo.SETTINGS_DIR, CMFileSyncInfo.ONLINE_MODE_LIST_FILE_NAME);
        Path storedListPath = Paths.get(CMInfo.SETTINGS_DIR, CMFileSyncInfo.ONLINE_MODE_MAP_FILE);
        // load the online-mode-list file
/*
        List<Path> onlineModePathList = loadPathListFromFile(storedListPath);
        if (onlineModePathList != null) {
            // set to CMFileSyncInfo
            syncInfo.setOnlineModePathList(onlineModePathList);
        } else {
            System.err.println("The loaded online-mode-path-list is null!");
        }
*/
        // load the online-mode-map file
        Map<Path, Long> onlineModePathSizeMap = loadPathSizeMapFromFile(storedListPath);
        if (onlineModePathSizeMap != null) {
            // set to CMFileSyncInfo
            syncInfo.setOnlineModePathSizeMap(onlineModePathSizeMap);
        } else {
            System.err.println("The loaded online-mode-map is null!");
        }

        // start the watch service
        boolean ret = startWatchService();
        if (!ret) {
            System.err.println("error starting watch service!");
            return false;
        }

        // check if the file-sync mode is AUTO
        if (fileSyncMode == CMFileSyncMode.AUTO) {
            // start the proactive mode task
            ret = startProactiveMode();
            if (!ret) {
                System.err.println("error to start proactive mode!");
                return false;
            }
            // update the current file-sync mode to AUTO
            syncInfo.setCurrentMode(CMFileSyncMode.AUTO);
        } else {
            // update the current file-sync mode to MANUAL
            syncInfo.setCurrentMode(CMFileSyncMode.MANUAL);
        }

        // conduct file-sync task once
        ret = sync();
        if (!ret) {
            System.err.println("error starting file-sync!");
            return false;
        }

        return true;
    }

    // called at the client
    private boolean startProactiveMode() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startProactiveMode() called..");
        }
        // check if a proactive mode task is already running
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        if (!syncInfo.isProactiveModeTaskDone()) {
            System.err.println("A proactive mode task is already running!");
            return false;
        }

        // get the scheduled-executor-service reference
        ScheduledExecutorService ses = m_cmInfo.getThreadInfo().getScheduledExecutorService();
        Objects.requireNonNull(ses);
        // get directory-activation-monitoring-period
        CMConfigurationInfo confInfo = Objects.requireNonNull(m_cmInfo.getConfigurationInfo());
        long period = confInfo.getDirActivationMonitoringPeriod();
        TimeUnit unit = confInfo.getDirActivationMonitoringPeriodUnit();
        // create a scheduled proactive mode task
        CMFileSyncProactiveModeTask proactiveModeTask = new CMFileSyncProactiveModeTask(this,
                syncInfo, confInfo);
        ScheduledFuture<?> scheduledFuture = ses.scheduleWithFixedDelay(proactiveModeTask, period, period, unit);
        if (scheduledFuture == null) {
            System.err.println("error to call scheduleWithFixedDelay()!");
            return false;
        }
        // set the task future reference
        syncInfo.setProactiveModeTaskFuture(scheduledFuture);

        return true;
    }

    // called at the client
    private boolean stopProactiveMode() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.stopProactiveMode() called..");
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        // check if the proactive-mode task has already done
        if (syncInfo.isProactiveModeTaskDone()) {
            System.out.println("The proactive-mode task has already done.");
            return true;
        }

        // get Future reference of the proactive-mode task
        ScheduledFuture<?> proactiveModeTaskFuture = syncInfo.getProactiveModeTaskFuture();
        if (proactiveModeTaskFuture == null) {
            System.err.println("proactiveModeTaskFuture is null!");
            return false;
        }
        // cancel the proactive-mode task
        proactiveModeTaskFuture.cancel(true);
        try {
            proactiveModeTaskFuture.get(5, TimeUnit.SECONDS);
        } catch (CancellationException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
            return false;
        }
        // check the state of the proactive-mode task
        if (!syncInfo.isProactiveModeTaskDone()) {
            System.err.println("The proactive-mode task is still running!");
            return false;
        }

        // initialize the future reference in CMFileSyncInfo
        syncInfo.setProactiveModeTaskFuture(null);

        return true;
    }

/*    private List<Path> loadPathListFromFile(Path listPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.loadPathListFromFile() called..");
            System.out.println("storedListPath = " + listPath);
        }

        if (listPath == null) {
            System.err.println("The argument list path is null!");
            return null;
        }
        if (!Files.exists(listPath)) {
            System.err.println("The argument (" + listPath + ") does not exists!");
            return null;
        }

        List<Path> list;
        try {
            list = Files.lines(listPath).map(Path::of).collect(Collectors.toList());

            if (CMInfo._CM_DEBUG) {
                System.out.println("--- loaded online-mode path list: ");
                for (Path path : list)
                    System.out.println("path = " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return list;
    }*/

    // called at the client
    private Map<Path, Long> loadPathSizeMapFromFile(Path storedPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.loadPathSizeMapFromFile() called..");
            System.out.println("storedPath = " + storedPath);
        }
        // check if the argument is null
        if (storedPath == null) {
            System.err.println("The argument path is null!");
            return null;
        }
        // check if the argument path exists
        if (!Files.exists(storedPath)) {
            System.err.println("The argument (" + storedPath + ") does not exists!");
            return null;
        }
        // read line-by-line from the file and add (key, value) to the online-mode-map
        Map<Path, Long> pathSizeMap;
        try {
            pathSizeMap = Files.lines(storedPath)
                    .map(line -> line.split(" "))
                    .collect(Collectors.toMap(tokens -> Paths.get(tokens[0]),
                            tokens -> Long.parseLong(tokens[1])));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return pathSizeMap;
    }

    // called at the client
    public boolean stopFileSync() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.stopFileSync() called..");
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());

        // check current file-sync mode
        if (syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.out.println("Current file-sync mode is already " + syncInfo.getCurrentMode());
            return true;
        }

        // save the online-mode-path list to the file
        //boolean ret = saveOnlineModeListToFile();
        // save the online-mode-map to the file
        boolean ret = saveOnlineModePathSizeMapToFile();
        if (!ret) {
            System.err.println("error to save the online-mode-path-list to file!");
        }

        // set syncInProgress to false
        syncInfo.setSyncInProgress(false);

        // check file-sync mode
        if (syncInfo.getCurrentMode() == CMFileSyncMode.AUTO) {
            ret = stopProactiveMode();
            if (!ret) {
                System.err.println("error to stop proactive-mode task!");
                return false;
            }
        }

        // stop the watch service
        ret = stopWatchService();
        if (!ret) {
            System.err.println("error to stop watch service!");
            return false;
        }

        // update the current file-sync mode
        syncInfo.setCurrentMode(CMFileSyncMode.OFF);

        return true;
    }

    // called at the client
/*    public boolean saveOnlineModeListToFile() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.saveOnlineModeListToFile() called..");
        }

        // get the online-mode-path list
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        List<Path> onlineModeList = Objects.requireNonNull(syncInfo.getOnlineModePathList());
        // get the online-mode-list file path
        Path storedPath = Paths.get(CMInfo.SETTINGS_DIR, CMFileSyncInfo.ONLINE_MODE_LIST_FILE_NAME);

        // save the list to the file
        boolean ret = savePathListToFile(onlineModeList, storedPath);
        if (!ret) {
            System.err.println("error to save the online-mode-path list to file!");
            return false;
        }

        return true;
    }

    // called at the client
    private boolean savePathListToFile(List<Path> pathList, Path storedPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.savePathListToFile() called..");
            System.out.println("pathList = " + pathList);
            System.out.println("storedPath = " + storedPath);
        }

        if (pathList == null || storedPath == null) {
            System.err.println("The path list or path is null!");
            return false;
        }

        if (!Files.exists(storedPath)) {
            try {
                Path parentPath = storedPath.getParent();
                if (parentPath != null)
                    Files.createDirectories(parentPath);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // create or open file
        try (BufferedWriter writer = Files.newBufferedWriter(storedPath)) {
            // write lists to the file
            for (Path path : pathList) {
                writer.write(path.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }*/

    // called at the client
    public boolean saveOnlineModePathSizeMapToFile() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.saveOnlineModePathSizeMapToFile() called..");
        }
        // get online-mode-map
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        Map<Path, Long> onlineModePathSizeMap = syncInfo.getOnlineModePathSizeMap();
        Objects.requireNonNull(onlineModePathSizeMap);
        // get online-mode-map file path
        Path storedPath = Paths.get(CMInfo.SETTINGS_DIR, CMFileSyncInfo.ONLINE_MODE_MAP_FILE);
        // save map to the file
        boolean ret = savePathSizeMapToFile(onlineModePathSizeMap, storedPath);
        if (!ret) {
            System.err.println("error to save the online-mode-map to file!");
            return false;
        }
        return true;
    }

    // called at the client
    private boolean savePathSizeMapToFile(Map<Path, Long> map, Path storedPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.savePathSizeMapToFile() called..");
            System.out.println("map = " + map);
            System.out.println("storedPath = " + storedPath);
        }
        // check arguments
        if (map == null || storedPath == null) {
            System.err.println("The argument map or path is null!");
            return false;
        }
        // check the stored file
        if (!Files.exists(storedPath)) {
            Path parentPath = storedPath.getParent();
            if (parentPath != null) {
                try {
                    Files.createDirectories(parentPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        // create or open file
        try (BufferedWriter writer = Files.newBufferedWriter(storedPath)) {
            for (Map.Entry<Path, Long> entry : map.entrySet()) {
                Path path = entry.getKey();
                long size = entry.getValue();
                writer.write(path.toString() + " " + Long.toString(size));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean createTestFile(Path path, long size) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.crateTestFile() called..");
            System.out.println("path = " + path);
            System.out.println("size = " + size);
        }

        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {

            // declare relevant variables
            final int arraySize = 1024;
            byte[] byteArray = new byte[arraySize];
            long remainingBytes = size;
            ByteBuffer byteBuffer = ByteBuffer.allocate(arraySize);
            int numBytesWritten = 0;

            // create a Random object
            Random random = new Random();
            while (remainingBytes > 0) {
                // get random bytes array
                random.nextBytes(byteArray);
                // init byteBuffer
                byteBuffer.clear();
                // write array to byteBuffer
                if (remainingBytes < arraySize) {
                    byteBuffer.put(byteArray, 0, (int) remainingBytes);
                } else {
                    byteBuffer.put(byteArray);
                }
                // write byteBuffer to the file channel
                byteBuffer.flip();
                numBytesWritten = channel.write(byteBuffer);
                // update remainingBytes
                remainingBytes -= numBytesWritten;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

/*
    public boolean createModifiedTestFile(Path path, Path modPath, int percentage) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.createModifiedTestFile() called..");
            System.out.println("path = " + path);
            System.out.println("modPath = " + modPath);
            System.out.println("percentage = " + percentage + " %");
        }

        // copy source file (path) to target file (modPath)
        try {
            Files.copy(path, modPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try (SeekableByteChannel channel = Files.newByteChannel(modPath, StandardOpenOption.WRITE)) {
            // get the file size
            long size = channel.size();
            // get the modification size
            long modifiedSize = size * percentage / 100;
            // declare relevant variables
            final int arraySize = 1024;
            byte[] byteArray = new byte[arraySize];
            long remainingBytes = modifiedSize;
            ByteBuffer byteBuffer = ByteBuffer.allocate(arraySize);
            int numBytesWritten = 0;

            // create a Random object
            Random random = new Random();
            while (remainingBytes > 0) {
                // get random bytes array
                random.nextBytes(byteArray);
                // init byteBuffer
                byteBuffer.clear();
                // write array to byteBuffer
                if (remainingBytes < arraySize) {
                    byteBuffer.put(byteArray, 0, (int) remainingBytes);
                } else {
                    byteBuffer.put(byteArray);
                }
                // write byteBuffer to the file channel
                byteBuffer.flip();
                numBytesWritten = channel.write(byteBuffer);
                // update remainingBytes
                remainingBytes -= numBytesWritten;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
*/

    public boolean createModifiedTestFile(Path path, Path modPath, CMTestFileModType modType, int percentage) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.createModifiedTestFile() called..");
            System.out.println("path = " + path);
            System.out.println("modPath = " + modPath);
            System.out.println("modType = " + modType);
            System.out.println("percentage = " + percentage);
        }

        // copy the source file (path) to the target file (modPath)
        try {
            Files.copy(path, modPath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        SeekableByteChannel channel = null;
        try {
            // open the target channel
            if(modType == CMTestFileModType.APPEND)
                channel = Files.newByteChannel(modPath, StandardOpenOption.APPEND);
            else channel = Files.newByteChannel(modPath, StandardOpenOption.WRITE);

            // calculate number of bytes to be modified
            long size = channel.size();
            long modifiedSize = size * percentage / 100;

            // if modType is TRUNC, truncate the target file
            if(modType == CMTestFileModType.TRUNC) {
                channel.truncate(size - modifiedSize);
            }
            else {
                // declare variables to write new bytes
                final int arraySize = 1024;
                byte[] byteArray = new byte[arraySize];
                long remainingBytes = modifiedSize;
                ByteBuffer byteBuffer = ByteBuffer.allocate(arraySize);
                int numBytesWritten = 0;
                // write new random bytes to the target channel until the remaining bytes becomes 0
                Random random = new Random();
                while(remainingBytes > 0) {
                    // get random bytes array
                    random.nextBytes(byteArray);
                    // clear byteBuffer
                    byteBuffer.clear();
                    // write byteArray to byteBuffer
                    if(remainingBytes < arraySize) byteBuffer.put(byteArray, 0, (int) remainingBytes);
                    else byteBuffer.put(byteArray);
                    // write byteBuffer to the file channel
                    byteBuffer.flip();
                    numBytesWritten = channel.write(byteBuffer);
                    // update remainingBytes
                    remainingBytes -= numBytesWritten;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    // URL: https://stackoverflow.com/questions/2972986/
    // how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java/19447758#19447758
    public void closeDirectBuffer(ByteBuffer cb) {
        if (cb == null || !cb.isDirect()) return;
        // we could use this type cast and call functions without reflection code,
        // but static import from sun.* package is risky for non-SUN virtual machine.
        //try { ((sun.nio.ch.DirectBuffer)cb).cleaner().clean(); } catch (Exception ex) { }

        // JavaSpecVer: 1.6, 1.7, 1.8, 9, 10
        boolean isOldJDK = System.getProperty("java.specification.version", "99").startsWith("1.");
        try {
            if (isOldJDK) {
                Method cleaner = cb.getClass().getMethod("cleaner");
                cleaner.setAccessible(true);
                Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
                clean.setAccessible(true);
                clean.invoke(cleaner.invoke(cb));
            } else {
                Class unsafeClass;
                try {
                    unsafeClass = Class.forName("sun.misc.Unsafe");
                } catch (Exception ex) {
                    // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
                    // but that method should be added if sun.misc.Unsafe is removed.
                    unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                }
                Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
                clean.setAccessible(true);
                Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                Object theUnsafe = theUnsafeField.get(null);
                clean.invoke(theUnsafe, cb);
            }
        } catch (Exception ex) {
        }
        cb = null;
    }

    // called at the client
    public synchronized List<Path> getSyncDirectoryList() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getSyncDirectoryList() called..");
        }

        // get file-sync home
        Path syncHome = getClientSyncHome();
        Objects.requireNonNull(syncHome);
        // get the list of directory paths
        List<Path> dirList;
        try {
            dirList = Files.walk(syncHome)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (CMInfo._CM_DEBUG) {
            dirList.forEach(System.out::println);
        }

        return dirList;
    }

    // called at the client
    public double calculateDirActivationRatio(Path dir) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateDirActivationRatio() called..");
            System.out.println("dir = " + dir);
        }
        // check the argument
        if (!Files.isDirectory(dir)) {
            System.err.println(dir + " is not a directory!");
        }
        // get list of normal files (not directory types)
        List<Path> fileList;
        try {
            fileList = Files.walk(dir, 1)
                    .filter(p -> !Files.isDirectory(p))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        if (fileList.isEmpty()) {
            System.out.println(dir + " does not have a normal file.");
            return 0;
        }

        // get duration-since-last-access threshold (DSLAT)
        CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
        Objects.requireNonNull(confInfo);
        long durationSinceLastAccessThreshold = confInfo.getDurationSinceLastAccessThreshold();
        // get the unit of duration since last access threshold (DSLATU)
        TimeUnit unit = confInfo.getDurationSinceLastAccessThresholdUnit();
        if (CMInfo._CM_DEBUG) {
            System.out.println("durationSinceLastAccessThreshold = " + durationSinceLastAccessThreshold);
            System.out.println("unit = " + unit);
        }

        // get the sum of file activation ratio
        double totalActivationRatio = 0;
        // get current time by DSLATU
        long currentTime = System.currentTimeMillis();
        long ct = unit.convert(currentTime, TimeUnit.MILLISECONDS);
        if (CMInfo._CM_DEBUG) {
            System.out.println("currentTime = " + currentTime);
            System.out.println("ct = " + ct + " " + unit);
        }
        for (Path path : fileList) {
            // get the last access time
            BasicFileAttributes attr = null;
            try {
                attr = Files.readAttributes(path, BasicFileAttributes.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileTime lastAccessTime = attr.lastAccessTime();
            long lat = lastAccessTime.to(unit);

            // calculate file activation ratio
            double fileActivationRatio = (double) durationSinceLastAccessThreshold / (ct - lat);
            if (fileActivationRatio > 1) fileActivationRatio = 1;
            // sum file activation ratio
            totalActivationRatio += fileActivationRatio;

            if (CMInfo._CM_DEBUG) {
                System.out.println("----- path = " + path);
                System.out.println("lastAccessTime = " + lastAccessTime);
                System.out.println("lat = " + lat + " " + unit);
                System.out.println("fileActivationRatio = " + fileActivationRatio);
                System.out.println("totalActivationRatio = " + totalActivationRatio);
            }
        }
        // get DAR (= average of FAR)
        double dirActivationRatio = totalActivationRatio / fileList.size();
        if (CMInfo._CM_DEBUG) {
            System.out.println("dirActivationRatio = " + dirActivationRatio);
        }

        return dirActivationRatio;
    }

    // called at the client
    public boolean startProactiveOnlineMode(Path dir) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startProactiveOnlineMode() called..");
            System.out.println("dir = " + dir);
        }
        // get the file-sync home dir
        Path syncHome = Objects.requireNonNull(getClientSyncHome());
        // get the root drive of the sync home
        Path root = Objects.requireNonNull(syncHome.getRoot());
        // get total space of the root drive
        long totalSpace = root.toFile().getTotalSpace();
        ///// get total space for file-sync
        // get file-sync storage ratio
        CMConfigurationInfo confInfo = Objects.requireNonNull(m_cmInfo.getConfigurationInfo());
        long fileSyncStorage = confInfo.getFileSyncStorage() * 1024 * 1024; // MB to Bytes
        if (fileSyncStorage == 0) {
            System.err.println("File-sync storage is 0! It must be greater than 0!");
            return false;
        }
        if (fileSyncStorage > totalSpace) {
            System.err.println("file-sync storage(" + fileSyncStorage + ") > total-space(" + totalSpace + ")!");
            return false;
        }

        /////
        ///// get used-sync-space ratio
        // get used space of the file-sync home dir
        long usedSyncSpace = getDirectorySize(syncHome);
        // calculate used sync-space ratio
        double usedStorageRatio = usedSyncSpace / (double) fileSyncStorage;
        /////
        // get used-sync-space-ratio threshold
        double usedStorageRatioThreshold = confInfo.getUsedStorageRatioThreshold();

        if (CMInfo._CM_DEBUG) {
            System.out.println("* syncHome = " + syncHome);
            System.out.println("* root = " + root);
            System.out.println("* totalSpace = " + totalSpace + " Bytes.");
            System.out.println("* fileSyncStorage = " + fileSyncStorage + " Bytes.");
            System.out.println("* usedSyncSpace = " + usedSyncSpace);
            System.out.println("* usedStorageRatio = " + usedStorageRatio);
            System.out.println("* usedStorageRatioThreshold = " + usedStorageRatioThreshold);
        }

        // check used-sync-space-ratio and threshold
        if (usedStorageRatio <= usedStorageRatioThreshold) {
            System.out.println("** No need to change any file to the online mode.");
            return true;
        }

        ///// get a list of local-mode files sorted by ascending order of last-access-time
        // get local-mode file list
        CMFileSyncInfo syncInfo = m_cmInfo.getFileSyncInfo();
        Objects.requireNonNull(syncInfo);
        List<Path> pathList = syncInfo.getPathList();
        List<Path> onlineModePathList = syncInfo.getOnlineModePathSizeMap().keySet().stream().toList();
        List<Path> localModePathList = pathList.stream()
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> !onlineModePathList.contains(p))
                .collect(Collectors.toList());
        if (CMInfo._CM_DEBUG) {
            System.out.println("** local-mode path list");
            localModePathList.forEach(System.out::println);
        }
        // check local-mode path list
        if (localModePathList.isEmpty()) {
            System.err.println("The local-mode path list is empty!");
            return false;
        }
        // sort the local-mode path list by ascending order of last access time
        localModePathList.sort((p1, p2) -> {
                    BasicFileAttributes attr1 = null;
                    BasicFileAttributes attr2 = null;
                    try {
                        attr1 = Files.readAttributes(p1, BasicFileAttributes.class);
                        attr2 = Files.readAttributes(p2, BasicFileAttributes.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FileTime accessTime1 = attr1.lastAccessTime();
                    FileTime accessTime2 = attr2.lastAccessTime();
                    return accessTime1.compareTo(accessTime2);
                }
        );
        if (CMInfo._CM_DEBUG) {
            System.out.println("** sorted local-mode path list");
            localModePathList.forEach(System.out::println);
        }
        /////

        ///// get the list of local-mode files to become online mode.
        long usedSyncSpaceToBeUpdated = usedSyncSpace;
        double usedStorageRatioToBeUpdated;
        List<Path> pathListToBeOnline = new ArrayList<>();
        // add files from the local-mode list to the list to be online mode.
        for (Path path : localModePathList) {
            try {
                usedSyncSpaceToBeUpdated -= Files.size(path);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            usedStorageRatioToBeUpdated = usedSyncSpaceToBeUpdated / (double) fileSyncStorage;
            pathListToBeOnline.add(path);

            if (CMInfo._CM_DEBUG) {
                System.out.println("** path = " + path + " : added to the list to be online.");
                System.out.println("usedSyncSpaceToBeUpdated = " + usedSyncSpaceToBeUpdated);
                System.out.println("usedStorageRatioToBeUpdated = " + usedStorageRatioToBeUpdated);
            }
            if (usedStorageRatioToBeUpdated <= usedStorageRatioThreshold) {
                System.out.println("** Now USR-updated(" + usedStorageRatioToBeUpdated + ") <= USRT("
                        + usedStorageRatioThreshold + ")");
                break;
            }
        }
        /////

        // request online-mode for the list
        boolean ret = requestOnlineMode(pathListToBeOnline);
        if (!ret) {
            System.err.println("error from requestOnlineMode()!");
            return false;
        }

        return true;
    }

    // called at the client
    private long getDirectorySize(Path path) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getDirectorySize() called..");
            System.out.println("path = " + path);
        }
        long size;
        try {
            size = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("size = " + size + " Bytes.");
        }

        return size;
    }

    // called at the client
    public boolean startProactiveLocalMode(Path dir) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startProactiveLocalMode() called..");
            System.out.println("dir = " + dir);
        }
        // get the file-sync home dir
        Path syncHome = Objects.requireNonNull(getClientSyncHome());
        // get the root drive of the sync home
        Path root = Objects.requireNonNull(syncHome.getRoot());
        // get total space of the root drive
        long totalSpace = root.toFile().getTotalSpace();
        ///// get total space for file-sync
        // get file-sync storage ratio
        CMConfigurationInfo confInfo = Objects.requireNonNull(m_cmInfo.getConfigurationInfo());
        long fileSyncStorage = confInfo.getFileSyncStorage() * 1024 * 1024; // MB to Bytes
        if (fileSyncStorage == 0) {
            System.err.println("File-sync storage is 0! It must be greater than 0!");
            return false;
        }
        if (fileSyncStorage > totalSpace) {
            System.err.println("file-sync storage(" + fileSyncStorage + ") > total-space(" + totalSpace + ")!");
            return false;
        }

        /////
        ///// get used-sync-space ratio
        // get used space of the file-sync home dir
        long usedSyncSpace = getDirectorySize(syncHome);
        // calculate used sync-space ratio
        double usedStorageRatio = usedSyncSpace / (double) fileSyncStorage;
        /////
        // get used-sync-space-ratio threshold
        double usedStorageRatioThreshold = confInfo.getUsedStorageRatioThreshold();

        if (CMInfo._CM_DEBUG) {
            System.out.println("* syncHome = " + syncHome);
            System.out.println("* root = " + root);
            System.out.println("* totalSpace = " + totalSpace + " Bytes.");
            System.out.println("* fileSyncStorage = " + fileSyncStorage + " Bytes.");
            System.out.println("* usedSyncSpace = " + usedSyncSpace);
            System.out.println("* usedStorageRatio = " + usedStorageRatio);
            System.out.println("* usedStorageRatioThreshold = " + usedStorageRatioThreshold);
        }

        // check used-sync-space-ratio and threshold
        if (usedStorageRatio > usedStorageRatioThreshold) {
            System.out.println("** Not enough sync space to change any online-mode file to the local mode.");
            return true;
        }
        // get max-delay-access threshold
        long maxAccessDelayThreshold = confInfo.getMaxAccessDelayThreshold();
        if (CMInfo._CM_DEBUG) {
            System.out.println("maxAccessDelayThreshold = " + maxAccessDelayThreshold + " ms.");
        }
        // get input throughput from the server
        String defaultServer = m_cmInfo.getInteractionInfo().getDefaultServerInfo().getServerName();
        // get input throughput (MBps)
        double inputThroughput = CMCommManager.measureInputThroughput(defaultServer, m_cmInfo);
        // calculate minimum size (Bytes) of a file to be local mode
        long minFileSizeForLocalMode = (long) (inputThroughput * 1000000 * maxAccessDelayThreshold / 1000);
        if (CMInfo._CM_DEBUG) {
            System.out.println(String.format("inputThroughput from [%s] = %.2f MBps%n",
                    defaultServer, inputThroughput));
            System.out.println("minFileSizeForLocalMode = " + minFileSizeForLocalMode + " Bytes.");
        }
        // get online-mode file list
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        List<Path> onlineModePathList = syncInfo.getOnlineModePathSizeMap().keySet().stream().toList();
        if (onlineModePathList.isEmpty()) {
            System.err.println("The online-mode-path list is empty!");
            return false;
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("** online-mode-path list");
            onlineModePathList.forEach(System.out::println);
        }

        // get list of online-mode files of size greater than minimum size
        // and sorted by descending order of last access time
        List<Path> bigSortedOnlineModePathList = onlineModePathList.stream()
                .filter(p -> syncInfo.getOnlineModePathSizeMap().get(p) >= minFileSizeForLocalMode)
                .sorted((p1, p2) -> {
                    BasicFileAttributes attr1 = null;
                    BasicFileAttributes attr2 = null;
                    try {
                        attr1 = Files.readAttributes(p1, BasicFileAttributes.class);
                        attr2 = Files.readAttributes(p2, BasicFileAttributes.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FileTime accessTime1 = attr1.lastAccessTime();
                    FileTime accessTime2 = attr2.lastAccessTime();
                    return accessTime2.compareTo(accessTime1);
                })
                .collect(Collectors.toList());
        if (bigSortedOnlineModePathList.isEmpty()) {
            System.err.println("sorted online-mode-path list greater than min size is empty!");
            return false;
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("** sorted online-mode-path list greater than min size");
            bigSortedOnlineModePathList.forEach(System.out::println);
        }

        ///// get the list of online-mode files to become local mode
        long usedSyncSpaceToBeUpdated = usedSyncSpace;
        double usedStorageRatioToBeUpdated = usedSyncSpace / (double) fileSyncStorage;
        List<Path> pathListToBeLocal = new ArrayList<>();
        // add files from the sorted BIG online-mode list to the list to be local mode.
        for (Path path : bigSortedOnlineModePathList) {
            usedSyncSpaceToBeUpdated += syncInfo.getOnlineModePathSizeMap().get(path);
            usedStorageRatioToBeUpdated = usedSyncSpaceToBeUpdated / (double) fileSyncStorage;
            pathListToBeLocal.add(path);

            if (CMInfo._CM_DEBUG) {
                System.out.println("** path = " + path + " : added to the list to be local.");
                System.out.println("usedSyncSpaceToBeUpdated = " + usedSyncSpaceToBeUpdated);
                System.out.println("usedStorageRatioToBeUpdated = " + usedStorageRatioToBeUpdated);
            }

            if (usedStorageRatioToBeUpdated > usedStorageRatioThreshold) {
                System.out.println("** Now USR-updated(" + usedStorageRatioToBeUpdated + ") > USRT("
                        + usedStorageRatioThreshold + ")");
                break;
            }
        }
        /////
        // check used-storage-ratio updated
        if (usedStorageRatioToBeUpdated > usedStorageRatioThreshold) {
            boolean ret = requestLocalMode(pathListToBeLocal);
            return ret;
        }

        ///// Here, USR <= USRT, that is, more online-mode files can be added to the list to become local
        if (CMInfo._CM_DEBUG) {
            System.out.println("** Still USR <= USRT");
        }
        // get list of the remaining online-mode files sorted by descending order of last access time
        List<Path> smallSortedOnlineModePathList = onlineModePathList.stream()
                .filter(p -> syncInfo.getOnlineModePathSizeMap().get(p) < minFileSizeForLocalMode)
                .sorted((p1, p2) -> {
                    BasicFileAttributes attr1 = null;
                    BasicFileAttributes attr2 = null;
                    try {
                        attr1 = Files.readAttributes(p1, BasicFileAttributes.class);
                        attr2 = Files.readAttributes(p2, BasicFileAttributes.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FileTime accessTime1 = attr1.lastAccessTime();
                    FileTime accessTime2 = attr2.lastAccessTime();
                    return accessTime2.compareTo(accessTime1);
                })
                .collect(Collectors.toList());

        // add files from the sorted SMALL online-mode list to the list to be local mode.
        for (Path path : smallSortedOnlineModePathList) {
            usedSyncSpaceToBeUpdated += syncInfo.getOnlineModePathSizeMap().get(path);
            usedStorageRatioToBeUpdated = usedSyncSpaceToBeUpdated / (double) fileSyncStorage;
            pathListToBeLocal.add(path);

            if (CMInfo._CM_DEBUG) {
                System.out.println("** path = " + path + " : added to the list to be local.");
                System.out.println("usedSyncSpaceToBeUpdated = " + usedSyncSpaceToBeUpdated);
                System.out.println("usedStorageRatioToBeUpdated = " + usedStorageRatioToBeUpdated);
            }

            if (usedStorageRatioToBeUpdated > usedStorageRatioThreshold) {
                System.out.println("** Now USR-updated(" + usedStorageRatioToBeUpdated + ") > USRT("
                        + usedStorageRatioThreshold + ")");
                break;
            }
        }
        /////
        // request local-mode
        boolean ret = requestLocalMode(pathListToBeLocal);
        return ret;
    }

    // called at the client
    public boolean isOnlineMode(Path path) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.isOnlineMode() called..");
            System.out.println("path = " + path);
        }

        // get online-mode path-size map
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        Map<Path, Long> onlineModePathSizeMap = syncInfo.getOnlineModePathSizeMap();
        Objects.requireNonNull(onlineModePathSizeMap);
        final Long size = onlineModePathSizeMap.get(path);
        if (CMInfo._CM_DEBUG) {
            System.out.println("size = " + size);
        }
        if (size == null) {
            return false;
        }
        return true;
    }

    // called at the client
    private void writeCommonTestInfoToFile(Path resultPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.writeCommonTestInfoToFile() called..");
            System.out.println("resultPath = " + resultPath);
        }

        final CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        final CMConfigurationInfo confInfo = Objects.requireNonNull(m_cmInfo.getConfigurationInfo());

        try (BufferedWriter bw = Files.newBufferedWriter(resultPath);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.println("Current file-sync mode: " + syncInfo.getCurrentMode());
            pw.println("DIR_ACTIVATION_MONITORING_PERIOD: " + confInfo.getDirActivationMonitoringPeriod());
            pw.println("DIR_ACTIVATION_MONITORING_PERIOD_UNIT: " + confInfo.getDirActivationMonitoringPeriodUnit());
            pw.println("DURATION_SINCE_LAST_ACCESS_THRESHOLD: " + confInfo.getDurationSinceLastAccessThreshold());
            pw.println("DURATION_SINCE_LAST_ACCESS_THRESHOLD_UNIT: "
                    + confInfo.getDurationSinceLastAccessThresholdUnit());
            pw.println("ONLINE_MODE_THRESHOLD: " + confInfo.getOnlineModeThreshold());
            pw.println("LOCAL_MODE_THRESHOLD: " + confInfo.getLocalModeThreshold());
            pw.println("FILE_SYNC_STORAGE: " + confInfo.getFileSyncStorage());
            pw.println("USED_STORAGE_RATIO_THRESHOLD: " + confInfo.getUsedStorageRatioThreshold());
            pw.println("MAX_ACCESS_DELAY_THRESHOLD: " + confInfo.getMaxAccessDelayThreshold());
            pw.println("----------------------------------------------");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("Current file-sync mode: " + syncInfo.getCurrentMode());
            System.out.println("DIR_ACTIVATION_MONITORING_PERIOD: " + confInfo.getDirActivationMonitoringPeriod());
            System.out.println("DIR_ACTIVATION_MONITORING_PERIOD_UNIT: "
                    + confInfo.getDirActivationMonitoringPeriodUnit());
            System.out.println("DURATION_SINCE_LAST_ACCESS_THRESHOLD: " + confInfo.getDurationSinceLastAccessThreshold());
            System.out.println("DURATION_SINCE_LAST_ACCESS_THRESHOLD: "
                    + confInfo.getDurationSinceLastAccessThresholdUnit());
            System.out.println("ONLINE_MODE_THRESHOLD: " + confInfo.getOnlineModeThreshold());
            System.out.println("LOCAL_MODE_THRESHOLD: " + confInfo.getLocalModeThreshold());
            System.out.println("FILE_SYNC_STORAGE: " + confInfo.getFileSyncStorage());
            System.out.println("USED_STORAGE_RATIO_THRESHOLD: " + confInfo.getUsedStorageRatioThreshold());
            System.out.println("MAX_ACCESS_DELAY_THRESHOLD: " + confInfo.getMaxAccessDelayThreshold());
        }

    }

    // called at the client
    private Path[] checkAndCreateTestFileNameArray(Path testFileDir, final int NUM_TEST_FILES) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.checkAndCreateTestFileNameArray() called..");
            System.out.println("testFileDir = " + testFileDir);
            System.out.println("NUM_TEST_FILES = " + NUM_TEST_FILES);
        }
        final Path[] testFileNameArray;
        try {
            testFileNameArray = Files.walk(testFileDir)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .toArray(Path[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (testFileNameArray.length != NUM_TEST_FILES) {
            System.err.println("Number of test files(" + testFileNameArray.length +
                    ") is different from NUM_TEST_FILES(" + NUM_TEST_FILES + ")!");
            return null;
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println(Arrays.toString(testFileNameArray));
        }

        return testFileNameArray;
    }

    // called at the client
    private boolean testAddFile(final long sleepTime, Path srcPath, Path targetPath, Path resultPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.testAddFile() called..");
            System.out.println("srcPath = " + srcPath);
            System.out.println("targetPath = " + targetPath);
            System.out.println("resultPath = " + resultPath);
        }

        // wait for file-addition period
        if (CMInfo._CM_DEBUG) {
            System.out.println("** waiting for file-addition period: " + sleepTime/1000 + " seconds.");
        }
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // write the file-add event to the result file
        Path syncHome = getClientSyncHome();
        try (BufferedWriter bw = Files.newBufferedWriter(resultPath, StandardOpenOption.APPEND);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.println(targetPath.getFileName() + ", add, local, " + getDirectorySize(syncHome) + " Bytes");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("**" + targetPath.getFileName() + ", add, local, "
                    + getDirectorySize(syncHome) + " Bytes");
        }

        return true;
    }

    // called at the client
    private boolean testAccessFile(final long sleepTime, Path srcPath, Path targetPath, Path resultPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.testAccessFile() called..");
            System.out.println("srcPath = " + srcPath);
            System.out.println("targetPath = " + targetPath);
            System.out.println("resultPath = " + resultPath);
        }
        // wait for file-access period
        if (CMInfo._CM_DEBUG) {
            System.out.println("** waiting for file-access period: " + sleepTime/1000 + " seconds.");
        }
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // access a file (file copy or request for local-mode)
        if (!Files.exists(targetPath)) {
            System.err.println("A file to be accessed does not exist!");
            System.err.println(targetPath);
            return false;
        }
        boolean isOnlineMode = isOnlineMode(targetPath);
        if (isOnlineMode) {
            List<Path> list = new ArrayList<>();
            list.add(targetPath);
            boolean ret = requestLocalMode(list);
            if (!ret) {
                System.err.println("Error to request local mode!");
                return false;
            }
        } else {
            try {
                Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // write the file-access event to the result file
        Path syncHome = getClientSyncHome();
        try (BufferedWriter bw = Files.newBufferedWriter(resultPath, StandardOpenOption.APPEND);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.print(targetPath.getFileName() + ", access, ");
            if (isOnlineMode) pw.print("online, ");
            else pw.print("local, ");
            pw.println(getDirectorySize(syncHome) + " Bytes");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("**" + targetPath.getFileName() + ", access, ");
            if (isOnlineMode) System.out.print("online, ");
            else System.out.print("local, ");
            System.out.println(getDirectorySize(syncHome) + " Bytes");
        }

        return true;
    }

    // called at the client
    private boolean testAccessNoFile(final long sleepTime, Path resultPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncManager.testAccessNoFile() called..");
            System.out.println("resultPath = " + resultPath);
        }

        // wait until the next recording to the result file
        if (CMInfo._CM_DEBUG) {
            System.out.println("** waiting for next no-access record: "+sleepTime/1000+" seconds.");
        }
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // write current access state to the result file
        Path syncHome = getClientSyncHome();
        try (BufferedWriter bw = Files.newBufferedWriter(resultPath, StandardOpenOption.APPEND);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.println("no file access, " + getDirectorySize(syncHome) + " Bytes");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("** no file access, " + getDirectorySize(syncHome) + " Bytes");
        }

        return true;
    }

    // called at the client
    public boolean simulateDeactivatingFileAccess(String fileName) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.simulateDeactivatingFileAccess() called..");
            System.out.println("fileName = " + fileName);
        }

        // declare constants for the file-access simulation
        final String TEST_FILE_DIR_NAME = "test-file-access";
        final int NUM_TEST_FILES = 10;
        final int FILE_ADD_PERIOD = 5;
        final int FILE_ACCESS_PERIOD = 2;
        final int ACTIVATION_PERIOD = 3 * 24;
        final int DEACTIVATION_PERIOD = 3 * 24;

        // check the existence of the test-file directory
        final CMConfigurationInfo confInfo = Objects.requireNonNull(m_cmInfo.getConfigurationInfo());
        final Path testFileDir = confInfo.getTransferedFileHome().resolve(TEST_FILE_DIR_NAME)
                .toAbsolutePath().normalize();
        if (!Files.exists(testFileDir)) {
            System.err.println("Test-file directory (" + testFileDir + ") not exists!");
            return false;
        }

        // check the test files in the test-file directory
        final Path[] testFileNameArray = checkAndCreateTestFileNameArray(testFileDir, NUM_TEST_FILES);
        if (testFileNameArray == null) {
            System.err.println("testFileNameArray is null!");
            return false;
        }

        // check if the sync-home directory is empty
        final CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        final Path syncHome = getClientSyncHome();
        if (!syncInfo.getPathList().isEmpty()) {
            System.err.println("The sync home must be empty to start the file-access test!");
            return false;
        }
        // print out the current working directory where the result file will be created.
        if (CMInfo._CM_DEBUG) {
            System.out.println("current working directory: " + System.getProperty("user.dir"));
        }
        // create the result file
        final Path resultPath;
        try {
            resultPath = Files.createFile(Path.of(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // write common info to the result file
        writeCommonTestInfoToFile(resultPath);

        ///// file-access activation test
        int totalElapsedSeconds = 0;
        int fileIndex = 0;
        boolean ret = false;
        while (totalElapsedSeconds < ACTIVATION_PERIOD) {
            // add a new file (copy a file from the test dir to the sync home)
            Path srcPath = testFileDir.resolve(testFileNameArray[fileIndex]);
            Path targetPath = syncHome.resolve(testFileNameArray[fileIndex]);
            ret = testAddFile(FILE_ADD_PERIOD*1000, srcPath, targetPath, resultPath);
            if (!ret) {
                System.err.println("testAddFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ADD_PERIOD;

            // access a file (file copy or request for local-mode)
            ret = testAccessFile(FILE_ACCESS_PERIOD*1000, srcPath, targetPath, resultPath);
            if (!ret) {
                System.err.println("testAccessFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ACCESS_PERIOD;

            // update file index to be tested
            fileIndex = (fileIndex + 1) % testFileNameArray.length;
        }
        /////
        ///// file-access deactivation test
        totalElapsedSeconds = 0;
        while (totalElapsedSeconds < DEACTIVATION_PERIOD) {

            ret = testAccessNoFile((FILE_ADD_PERIOD+FILE_ACCESS_PERIOD)*1000, resultPath);
            if(!ret) {
                System.err.println("testAccessNoFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ADD_PERIOD + FILE_ACCESS_PERIOD;
        }
        /////
        // print out the result file
        if (CMInfo._CM_DEBUG) {
            System.out.println("---------------- end of deactivating file-access test");
            try {
                Files.readAllLines(resultPath).forEach(System.out::println);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("----------------");
        }

        return true;
    }

    // called at the client
    public boolean simulateActivatingFileAccess(String fileName) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.simulateActivatingFileAccess() called..");
            System.out.println("fileName = " + fileName);
        }

        // declare constants for the file-access simulation
        final String TEST_FILE_DIR_NAME = "test-file-access";
        final int NUM_TEST_FILES = 10;
        final int FILE_ACCESS_PERIOD = 7;
        final int ACTIVATION_PERIOD = 3 * 24;
        final int DEACTIVATION_PERIOD = 3 * 24;

        // check the existence of the test-file directory
        final CMConfigurationInfo confInfo = Objects.requireNonNull(m_cmInfo.getConfigurationInfo());
        final Path testFileDir = confInfo.getTransferedFileHome().resolve(TEST_FILE_DIR_NAME)
                .toAbsolutePath().normalize();
        if (!Files.exists(testFileDir)) {
            System.err.println("Test-file directory (" + testFileDir + ") not exists!");
            return false;
        }

        // check the test files in the test-file directory
        final Path[] testFileNameArray = checkAndCreateTestFileNameArray(testFileDir, NUM_TEST_FILES);
        if (testFileNameArray == null) {
            System.err.println("testFileNameArray is null!");
            return false;
        }

        // check if the sync-home directory is empty
        final CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        final Path syncHome = getClientSyncHome();
        if (!syncInfo.getPathList().isEmpty()) {
            System.err.println("The sync home must be empty to start the file-access test!");
            return false;
        }

        // copy test directory files to the sync home
        for(Path name : testFileNameArray) {
            Path srcPath = testFileDir.resolve(name);
            Path targetPath = syncHome.resolve(name);
            try {
                Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // wait until the file-sync completes
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // request online-mode for all files in the sync home
        try {
            List<Path> pathList = Files.walk(syncHome)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            boolean ret = requestOnlineMode(pathList);
            if(!ret) {
                System.err.println("error to request online mode!");
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // print out the current working directory where the result file will be created.
        if (CMInfo._CM_DEBUG) {
            System.out.println("current working directory: " + System.getProperty("user.dir"));
        }
        // create the result file
        final Path resultPath;
        try {
            resultPath = Files.createFile(Path.of(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // write common info to the result file
        writeCommonTestInfoToFile(resultPath);

        ///// file-access deactivation test
        int totalElapsedSeconds = 0;
        boolean ret = false;
        while(totalElapsedSeconds < DEACTIVATION_PERIOD) {
            // after sleep time, record current state to the result file
            ret = testAccessNoFile(FILE_ACCESS_PERIOD*1000, resultPath);
            if(!ret) {
                System.err.println("testAccessNoFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ACCESS_PERIOD;
        }
        /////
        ///// file-access activation test
        totalElapsedSeconds = 0;
        int fileIndex = 0;
        while(totalElapsedSeconds < ACTIVATION_PERIOD) {
            // after sleep time, access a file
            Path srcPath = testFileDir.resolve(testFileNameArray[fileIndex]);
            Path targetPath = syncHome.resolve(testFileNameArray[fileIndex]);
            ret = testAccessFile(FILE_ACCESS_PERIOD*1000, srcPath, targetPath, resultPath);
            if(!ret) {
                System.err.println("testAccessFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ACCESS_PERIOD;
            // update file index to be accessed
            fileIndex = (fileIndex + 1) % testFileNameArray.length;
        }
        /////
        // print out the result file
        if (CMInfo._CM_DEBUG) {
            System.out.println("---------------- end of activating file-access test");
            try {
                Files.readAllLines(resultPath).forEach(System.out::println);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("----------------");
        }

        return true;
    }

    public void clearSyncHome() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.clearSyncHome() called..");
        }

        Path syncHome = getClientSyncHome();
        try {
            Files.walk(syncHome)
                    .filter(p -> !(p.equals(syncHome)))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // called at the client
    public List<Path> getOnlineModeFiles() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getOnlineModeFiles() called..");
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        if(syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.err.println("Current file sync mode is OFF!");
            System.err.println("You should start file sync to get the updated online mode file list.");
            return null;
        }

        List<Path> onlineModeFiles = syncInfo.getOnlineModePathSizeMap().keySet().stream().toList();

        if(CMInfo._CM_DEBUG) {
            System.out.println("-- online mode file list");
            for(Path path : onlineModeFiles)
                System.out.println(path);
        }
        return onlineModeFiles;
    }

    // called at the client
    public List<Path> getLocalModeFiles() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getLocalModeFiles() called..");
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        if(syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.err.println("Current file sync mode is OFF!");
            System.err.println("You should start file sync to get the updated local mode file list.");
            return null;
        }

        List<Path> onlineModeFiles = getOnlineModeFiles();
        List<Path> pathList = syncInfo.getPathList();

        List<Path> localModeFiles = pathList.stream()
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> !onlineModeFiles.contains(p)).toList();

        if(CMInfo._CM_DEBUG) {
            System.out.println("-- local mode file list");
            for(Path path : localModeFiles)
                System.out.println(path);
        }

        return localModeFiles;
    }
}
