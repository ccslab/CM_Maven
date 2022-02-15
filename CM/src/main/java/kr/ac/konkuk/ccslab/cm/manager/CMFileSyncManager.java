package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.*;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;
import kr.ac.konkuk.ccslab.cm.thread.CMWatchServiceTask;

import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
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

        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.startFileSync() called..");

        // client -> server
        // check if the client has logged in to the default server.
        CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
        if(confInfo.getSystemType().equals("SERVER"))
        {
            System.err.println("The system type is SERVER!");
            return false;
        }

        CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
        int nState = myself.getState();
        if(nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
        {
            System.err.println("You must log in to the default server!");
            return false;
        }

        CMFileSyncInfo fsInfo = m_cmInfo.getFileSyncInfo();

        if(fsInfo.isSyncInProgress()) {
            System.err.println("The file sync is in progress!");
            return false;
        }
        else {
            // set syncInProgress to true.
            fsInfo.setSyncInProgress(true);
        }

        // set file sync home.
        Path syncHome = getClientSyncHome();
        if(Files.notExists(syncHome)) {
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
        // send the file list to the server
        boolean sendResult = sendFileList();
        if(!sendResult) {
            System.err.println("CMFileSyncManager.startFileSync(), error to send the file list.");
            return false;
        }

        return true;
    }

    public List<Path> createPathList(Path syncHome) {

        if(CMInfo._CM_DEBUG)
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

        if( pathList.isEmpty() )
            System.err.println("CMFileSyncManager.createPathList(), The sync-home is empty.");

        if(CMInfo._CM_DEBUG) {
            for (Path p : pathList)
                System.out.println(p);
        }

        return pathList;
    }

    // currently called by client
    private boolean sendFileList() {
        if(CMInfo._CM_DEBUG)
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
        if(pathList == null)
            fse.setNumTotalFiles(0);
        else
            fse.setNumTotalFiles(pathList.size());

        // send the event
        boolean sendResult = CMEventManager.unicastEvent(fse, serverName, m_cmInfo);
        if(!sendResult) {
            System.err.println("CMFileSyncManager.sendFileList(), send error!");
            System.err.println(fse);
            return false;
        }
        return true;
    }

    // currently called by server
    public void checkNewTransferForSync(CMFileEvent fe) {

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.checkNewTransferForSync() called..");
            System.out.println("file event = " + fe);
        }
        // get the file name
        String fileName = fe.getFileName();
        // get the new file list
        String fileSender = fe.getFileSender();
        List<CMFileSyncEntry> newClientPathEntryList = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap()
                .get(fileSender).getNewClientPathEntryList();
        Objects.requireNonNull(newClientPathEntryList);

        // search for the entry in the newClientPathEntryList
        CMFileSyncEntry foundEntry = null;
        Path foundPath = null;
        for(CMFileSyncEntry entry : newClientPathEntryList) {
            if(entry.getPathRelativeToHome().endsWith(fileName)) {
                foundEntry = entry;
                foundPath = entry.getPathRelativeToHome();
                break;
            }
        }
        if(foundPath != null) {
            // get the file-transfer home
            Path transferFileHome = m_cmInfo.getConfigurationInfo().getTransferedFileHome().resolve(fileSender);
            // get the server sync home
            Path serverSyncHome = getServerSyncHome(fileSender);
            // move the transferred file to the sync home (including sub-directories)
            try {
                Files.move(transferFileHome.resolve(fileName), serverSyncHome.resolve(foundPath));
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
            if(result) {
                // remove the completed newClientPathEntry from the list
                final Path finalFoundPath = foundPath;
                boolean removeResult = newClientPathEntryList.removeIf(entry ->
                        entry.getPathRelativeToHome().equals(finalFoundPath));

                if(!removeResult) {
                    System.err.println("remove error from the new-client-path-entry-list: "+foundPath);
                    return;
                }
                // check if the file-sync is complete or not
                if(isCompleteFileSync(fileSender)) {
                    // complete the file-sync task
                    completeFileSync(fileSender);
                }
            }
        }
    }

    // called by the server
    public boolean completeNewFileTransfer(String userName, Path path) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completeNewFileTransfer() called..");
            System.out.println("userName = " + userName);
            System.out.println("path = " + path);
        }
        // get CMFileSyncGenerator
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(userName);
        if(syncGenerator == null) {
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
        if(!ret) {
            System.err.println("send error: "+fse);
            return false;
        }

        return true;
    }

    // called at the server
    public boolean skipUpdateFile(String userName, Path basisFile) {
        if(CMInfo._CM_DEBUG) {
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
        if(CMInfo._CM_DEBUG) {
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
        if(CMInfo._CM_DEBUG) {
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
        if(syncGenerator == null) {
            System.err.println("syncGenerator is null!");
            return false;
        }

        // compare the number of new files completed to the size of the new-file list
        newClientPathEntryList = syncGenerator.getNewClientPathEntryList();
        numNewFilesCompleted = syncGenerator.getNumNewFilesCompleted();
        if(!newClientPathEntryList.isEmpty()) {
            System.err.println("numNewFilesCompleted = "+numNewFilesCompleted);
            System.err.println("size of newClientPathEntryList = "+newClientPathEntryList.size());
            return false;
        }
        // get basis file list
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        basisFileList = Objects.requireNonNull(syncInfo.getBasisFileListMap()).get(userName);
        // compare the number of updated files to the size of the basis-file list
        numUpdateFilesCompleted = syncGenerator.getNumUpdateFilesCompleted();
        if(basisFileList != null && numUpdateFilesCompleted < basisFileList.size()) {
            System.err.println("numUpdateFilesCompleted = "+numUpdateFilesCompleted);
            System.err.println("size of basisFileList = "+basisFileList.size());
            return false;
        }
        // compare the number of files of which sync is completed to the size of client file-entry list
        fileEntryList = m_cmInfo.getFileSyncInfo().getClientPathEntryListMap().get(userName);
        numFilesCompleted = numNewFilesCompleted + numUpdateFilesCompleted;
        if(fileEntryList != null && numFilesCompleted < fileEntryList.size()) {
            System.err.println("numFilesCompleted = "+numFilesCompleted);
            System.err.println("size of client file-entry list = "+fileEntryList.size());
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
        if(numNewFilesNotCompleted > 0) {
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
        if(numUpdateFilesNotCompleted > 0) {
            System.err.println("numUpdateFilesNotCompleted = " + numUpdateFilesNotCompleted);
            return false;
        }

        if(CMInfo._CM_DEBUG) {
            System.out.println("The sync of all files is completed.");
        }

        return true;
    }

    // called by the server
    public boolean completeFileSync(String userName) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completeFileSync() called..");
            System.out.println("userName = " + userName);
        }
        // send the file-sync completion event
        boolean result = true;
        result = sendCompleteFileSync(userName);
        if(!result) return false;
        deleteFileSyncInfo(userName);
        return true;
    }

    // called by the server
    private boolean sendCompleteFileSync(String userName) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.sendCompleteFileSync() called..");
            System.out.println("userName = " + userName);
        }

        // get the CMFileSyncGenerator reference
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(userName);
        if(syncGenerator == null) {
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
        if(CMInfo._CM_DEBUG) {
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

    // called by the server
    public int calculateWeakChecksum(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateWeakChecksum() called..");
            System.out.println("ByteBuffer remaining size = "+buffer.remaining());
        }
        int[] abs = calculateWeakChecksumElements(buffer);

        if(CMInfo._CM_DEBUG) {
            System.out.println("weak checksum = " + abs[2]);
        }
        return abs[2];
    }

    // called by the client
    // reference: http://tutorials.jenkov.com/rsync/checksums.html
    public int[] calculateWeakChecksumElements(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("=== CMFileSyncManager.calculateWeakChecksumElements() called..");
            System.out.println("ByteBuffer remaining size = "+buffer.remaining());
        }

        int A = 0;
        int B = 0;
        int S = 0;
        int[] abs = new int[3]; // abs[0] = A, abs[1] = B, abs[2] = S
        int M = (int) Math.pow(2.0, 16.0);
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("initial A = " + A + ", B = " + B + ", S = " + S);
            System.out.println("M = " + M);
            System.out.print("initial abs = ");
            for(int e : abs) System.out.print(e+" ");
            System.out.println();
        }

        // repeat to update A and B for each block data
        while( buffer.hasRemaining() ) {
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
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("A = " + A + ", B = " + B + ", S = " + S);
            System.out.println("abs = "+Arrays.toString(abs));
        }

        return abs;
    }

    public byte[] calculateStrongChecksum(ByteBuffer buffer) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateStrongChecksum() called..");
            System.out.println("ByteBuffer remaining size = "+buffer.remaining());
        }
        // get MD5
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        md.update( buffer.array() );
        byte[] digest = md.digest();

        if(CMInfo._CM_DEBUG) {
            String checksum = DatatypeConverter.printHexBinary(digest).toUpperCase();
            System.out.println("checksum hex binary = " + checksum);
            System.out.println("checksum array string = "+ Arrays.toString(digest));
            System.out.println("length = "+ digest.length + "bytes.");
        }

        return digest;
    }

    public byte[] calculateFileChecksum(Path path) {
        if(CMInfo._CM_DEBUG) {
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
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;
            while((bytesCount = fis.read(byteArray)) != -1) {
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

        if(CMInfo._CM_DEBUG) {
            String checksum = DatatypeConverter.printHexBinary(bytes).toUpperCase();
            System.out.println("checksum = " + checksum);
        }

        return bytes;
    }

    // called by the client
    public int[] updateWeakChecksum(int oldA, int oldB, byte oldStartByte, byte newEndByte, int blockSize) {
        if(CMInfo._CM_DEBUG_2) {
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
        newABS[1] = B ;
        newABS[2] = S;

        if(CMInfo._CM_DEBUG_2) {
            System.out.println("A = " + A + ", B = " + B + ", S = " + S);
            System.out.println("newABS = "+Arrays.toString(newABS));
        }

        return newABS;
    }

    // calculate a checksum of a file (that is the sum of block weak checksum values)
    // The server will validate the newly created file with this file checksum.
    public int calculateWeakChecksum(Path path, int blockSize) {

        if(CMInfo._CM_DEBUG) {
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
            while( channel.position() < channel.size() ) {
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

        if(CMInfo._CM_DEBUG) {
            System.out.println("fileChecksum = " + fileChecksum);
        }

        return fileChecksum;
    }

    public Path getTempPathOfBasisFile(Path basisFilePath) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getTempPathOfBasisFile() called..");
            System.out.println("basisFilePath = " + basisFilePath);
        }

        String fileName = basisFilePath.getFileName().toString();
        String tempFileName = CMInfo.TEMP_FILE_PREFIX + fileName;
        Path tempBasisFilePath = basisFilePath.resolveSibling(tempFileName);

        if(CMInfo._CM_DEBUG) {
            System.out.println("tempBasisFilePath = " + tempBasisFilePath);
        }

        return tempBasisFilePath;
    }

    public boolean startWatchService() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startWatchService() called..");
        }
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        // check if the WatchService is already started
        if(!syncInfo.isWatchServiceTaskDone()) {
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

        // create a WatchServiceTask
        Path syncHome = Objects.requireNonNull(getClientSyncHome());
        CMWatchServiceTask watchTask = new CMWatchServiceTask(syncHome, watchService, this, syncInfo);
        // start the WatchServiceTask
        Future<?> future = es.submit(watchTask);
        if(future == null) {
            System.err.println("error submitting watch-service task to the ExecutorService!");
            return false;
        }
        // store the Future<?> to the syncInfo
        syncInfo.setWatchServiceFuture(future);

        return true;
    }

    public boolean stopWatchService() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.stopWatchService() called..");
        }

        // get syncInfo
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());

        // check if the watch service is already done
        if(syncInfo.isWatchServiceTaskDone()) {
            System.out.println("The watch service has already stopped..");
            return true;
        }

        // get WatchService reference
        WatchService watchService = syncInfo.getWatchService();
        if(watchService == null) {
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
        if(watchFuture == null) {
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
        if(!syncInfo.isWatchServiceTaskDone()) {
            System.err.println("The watch-service task is not done!");
            return false;
        }
        // initialize the WatchService and WatchService task references
        syncInfo.setWatchService(null);
        syncInfo.setWatchServiceFuture(null);

        return true;
    }

    public boolean requestOnlineMode(List<Path> pathList) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.requestOnlineMode() called..");
            System.out.println("pathList = " + pathList);
        }

        // check if current file-sync status
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        if(syncInfo.isSyncInProgress()) {
            System.err.println("Currently file-sync task is working! You should wait!");
            return false;
        }
        // check if the watch service is running
        if(syncInfo.isWatchServiceTaskDone()) {
            System.err.println("The file-sync monitoring stops! You should start the file-sync!");
            return false;
        }

        // check argument
        if(pathList == null) {
            System.err.println("The argument pathList is null!");
            return false;
        }

        // change the file-sync status
        syncInfo.setSyncInProgress(true);
        // stop the watch service
        boolean ret = stopWatchService();
        if(!ret) {
            System.err.println("error stopping WatchService!");
            return false;
        }

        //// extending all directory paths in the path list to file paths

        // extracting file-only list in the argument path list
        List<Path> fileOnlyList = pathList.stream()
                .filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                .collect(Collectors.toList());
        // extracting directory-only list in the argument path list
        List<Path> dirOnlyList = pathList.stream()
                .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                .collect(Collectors.toList());
        // extend each directory to a list of files and added to the file-only list
        ret = false;
        for(Path dir : dirOnlyList) {
            try {
                List<Path> fileList = Files.walk(dir)
                        .filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                        .collect(Collectors.toList());
                if(!fileList.isEmpty()) {
                    ret = fileOnlyList.addAll(fileList);
                    if(!ret) {
                        System.err.println("error to add files in "+dir);
                        continue;
                    }
                    if(CMInfo._CM_DEBUG) {
                        System.out.println("files in "+dir+" added to the file-only list");
                        for(Path path : fileList)
                            System.out.println("path = " + path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // take out path element that is already the online mode in the file-only list
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
        }

        //// create and send an online-mode-list event

        // get the user and server names
        String userName = m_cmInfo.getInteractionInfo().getMyself().getName();
        Objects.requireNonNull(userName);
        String serverName = m_cmInfo.getInteractionInfo().getDefaultServerInfo().getServerName();
        Objects.requireNonNull(serverName);
        // event transmission loop
        int listIndex = 0;
        boolean sendResult = false;
        while(listIndex < filteredFileOnlyList.size()) {
            // create an event
            CMFileSyncEventOnlineModeList listEvent = new CMFileSyncEventOnlineModeList();
            listEvent.setSender(userName);
            listEvent.setReceiver(serverName);
            listEvent.setRequester(userName);

            // get relative path list to be added to this event
            List<Path> subList = createSubOnlineModeListForEvent(listEvent, filteredFileOnlyList, listIndex);
            // update the listIndex
            listIndex += subList.size();
            // set the sublist to the event
            listEvent.setRelativePathList(subList);
            // send the event
            sendResult = CMEventManager.unicastEvent(listEvent, serverName, m_cmInfo);
            if(!sendResult) {
                System.err.println("send error: "+listEvent);
                return false;
            }
            if(CMInfo._CM_DEBUG) {
                System.out.println("sent listEvent = " + listEvent);
            }
        }

        // add filteredFileOnlyList to the online-mode-request queue
        ConcurrentLinkedQueue<Path> onlineModeRequestQueue = syncInfo.getOnlineModeRequestQueue();
        Objects.requireNonNull(onlineModeRequestQueue);
        ret = onlineModeRequestQueue.addAll(filteredFileOnlyList);
        if(!ret) {
            System.err.println("error to add filteredFileOnlyList to the online-mode-request queue!");
            return false;
        }

        return true;
    }

    // From the starting index (listIndex) of filteredFileOnlyList,
    // create a sublist that will be added to an online-mode-list event (listEvent)
    private List<Path> createSubOnlineModeListForEvent(CMFileSyncEventOnlineModeList listEvent,
                                                       List<Path> filteredFileOnlyList, int listIndex) {
        if(CMInfo._CM_DEBUG) {
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
        for(int i = listIndex; i < filteredFileOnlyList.size(); i++) {
            // get the relative path of the i-th element
            Path path = filteredFileOnlyList.get(i);
            Path relativePath = path.subpath(startPathIndex, path.getNameCount());
            // check the size of the relative path and add it to the event
            curByteNum += CMInfo.STRING_LEN_BYTES_LEN + relativePath.toString().getBytes().length;
            if(curByteNum < CMInfo.MAX_EVENT_SIZE) {
                ret = subList.add(relativePath);
                if(!ret) {
                    System.err.println("error to add "+relativePath);
                    return null;
                }
            }
            else
                break;
        }
        return subList;
    }

    public boolean requestLocalMode(List<Path> pathList) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.requestLocalMode() called..");
            System.out.println("pathList = " + pathList);
        }

        // check if current file-sync status
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        if(syncInfo.isSyncInProgress()) {
            System.err.println("Currently file-sync task is working! You should wait!");
            return false;
        }
        // check if the watch service is running
        if(syncInfo.isWatchServiceTaskDone()) {
            System.err.println("The file-sync monitoring stops! You should start the file-sync!");
            return false;
        }

        // check argument
        if(pathList == null) {
            System.err.println("The argument pathList is null!");
            return false;
        }

        // change the file-sync status
        syncInfo.setSyncInProgress(true);
        // stop the watch service
        boolean ret = stopWatchService();
        if(!ret) {
            System.err.println("error stopping WatchService!");
            return false;
        }

        //// extending all directory paths in the path list to file paths

        // extracting file-only list in the argument path list
        List<Path> fileOnlyList = pathList.stream()
                .filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                .collect(Collectors.toList());
        // extracting directory-only list in the argument path list
        List<Path> dirOnlyList = pathList.stream()
                .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                .collect(Collectors.toList());
        // extend each directory to a list of files and added to the file-only list
        ret = false;
        for(Path dir : dirOnlyList) {
            try {
                List<Path> fileList = Files.walk(dir)
                        .filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                        .collect(Collectors.toList());
                if(!fileList.isEmpty()) {
                    ret = fileOnlyList.addAll(fileList);
                    if(!ret) {
                        System.err.println("error to add files in "+dir);
                        continue;
                    }
                    if(CMInfo._CM_DEBUG) {
                        System.out.println("files in "+dir+" added to the file-only list");
                        for(Path path : fileList)
                            System.out.println("path = " + path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

        //// create and send a local-mode-list event

        // get the user and server names
        String userName = m_cmInfo.getInteractionInfo().getMyself().getName();
        Objects.requireNonNull(userName);
        String serverName = m_cmInfo.getInteractionInfo().getDefaultServerInfo().getServerName();
        Objects.requireNonNull(serverName);
        // event transmission loop
        int listIndex = 0;
        boolean sendResult = false;
        while(listIndex < filteredFileOnlyList.size()) {
            // create an event
            CMFileSyncEventLocalModeList listEvent = new CMFileSyncEventLocalModeList();
            listEvent.setSender(userName);
            listEvent.setReceiver(serverName);
            listEvent.setRequester(userName);

            // get relative path list to be added to this event
            List<Path> subList = createSubLocalModeListForEvent(listEvent, filteredFileOnlyList, listIndex);
            // update the listIndex
            listIndex += subList.size();
            // set the sublist to the event
            listEvent.setRelativePathList(subList);
            // send the event
            sendResult = CMEventManager.unicastEvent(listEvent, serverName, m_cmInfo);
            if(!sendResult) {
                System.err.println("send error: "+listEvent);
                return false;
            }
            if(CMInfo._CM_DEBUG) {
                System.out.println("sent listEvent = " + listEvent);
            }
        }

        // add filteredFileOnlyList to the local-mode-request queue
        ConcurrentLinkedQueue<Path> localModeRequestQueue = syncInfo.getLocalModeRequestQueue();
        Objects.requireNonNull(localModeRequestQueue);
        ret = localModeRequestQueue.addAll(filteredFileOnlyList);
        if(!ret) {
            System.err.println("error to add filteredFileOnlyList to the local-mode-request queue!");
            return false;
        }

        return true;
    }

    // From the starting index (listIndex) of filteredFileOnlyList,
    // create a sublist that will be added to a local-mode-list event (listEvent)
    private List<Path> createSubLocalModeListForEvent(CMFileSyncEventLocalModeList listEvent, List<Path> filteredFileOnlyList, int listIndex) {
        if(CMInfo._CM_DEBUG) {
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
        for(int i = listIndex; i < filteredFileOnlyList.size(); i++) {
            // get the relative path of the i-th element
            Path path = filteredFileOnlyList.get(i);
            Path relativePath = path.subpath(startPathIndex, path.getNameCount());
            // check the size of the relative path and add it to the event
            curByteNum += CMInfo.STRING_LEN_BYTES_LEN + relativePath.toString().getBytes().length;
            if(curByteNum < CMInfo.MAX_EVENT_SIZE) {
                ret = subList.add(relativePath);
                if(!ret) {
                    System.err.println("error to add "+relativePath);
                    return null;
                }
            }
            else
                break;
        }
        return subList;
    }

    // called at the client
    public void checkTransferForLocalMode(CMFileEvent fe) {
        if(CMInfo._CM_DEBUG) {
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
        if(headPath == null) {
            System.err.println("Local-mode-request queue is empty!");
            return;
        }
        if(!headPath.endsWith(fileName)) {
            System.err.println("Head of local-mode-request queue does not match the transferred file name!");
            System.out.println("headPath = " + headPath);
            System.out.println("fileName = " + fileName);
            return;
        }

        try {
            // save the last modified time of the queue head path
            FileTime lastModifiedTime = Files.getLastModifiedTime(headPath, LinkOption.NOFOLLOW_LINKS);
            // move the transferred file to the sync home
            Files.move(transferFileHome.resolve(fileName), headPath);
            // restore the last modified time
            Files.setLastModifiedTime(headPath, lastModifiedTime);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // delete head from the request queue
        localModeRequestQueue.remove();
        // delete path from the online-mode-list
        List<Path> onlineModeList = Objects.requireNonNull(syncInfo.getOnlineModePathList());
        boolean ret = onlineModeList.remove(headPath);
        if(!ret) {
            System.err.println("remove error from the online-mode-list: "+headPath);
            return;
        }

        // check if the queue is not empty
        if(!localModeRequestQueue.isEmpty()) {
            System.out.println("Local-mode-request queue is not empty.");
            return;
        }
        // if the queue is empty, create and send an end-local-mode-list event
        CMFileSyncEventEndLocalModeList endEvent = new CMFileSyncEventEndLocalModeList();
        endEvent.setSender(fe.getFileReceiver());
        endEvent.setReceiver(fileSender);
        endEvent.setRequester(fe.getFileReceiver());
        // numLocalModeFiles includes directories.
        int numLocalModeFiles = syncInfo.getPathList().size() - syncInfo.getOnlineModePathList().size();
        endEvent.setNumLocalModeFiles(numLocalModeFiles);

        ret = CMEventManager.unicastEvent(endEvent, fileSender, m_cmInfo);
        if(!ret) {
            System.err.println("send error: "+endEvent);
        }

        return;
    }

}
