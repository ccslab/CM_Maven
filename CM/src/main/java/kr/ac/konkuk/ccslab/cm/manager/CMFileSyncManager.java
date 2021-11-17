package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEvent;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    public boolean startFileSync() {

        if(CMInfo._CM_DEBUG)
            System.out.println("CMFileSyncManager::startFileSync() called..");

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
            System.out.println("CMFileSyncManager::createPathList() called..");

        List<Path> pathList;
        try {
            // filter only regular files -> change to absolute path -> sorted -> change to a list
            pathList = Files.walk(syncHome)
                    .filter(Files::isRegularFile)
                    .map(path -> path.toAbsolutePath().normalize())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if( pathList.isEmpty() )
            System.err.println("CMFileSyncManager::createPathList(), The sync-home is empty.");

        if(CMInfo._CM_DEBUG) {
            for (Path p : pathList)
                System.out.println(p);
        }

        return pathList;
    }

    // currently called by client
    private boolean sendFileList() {
        if(CMInfo._CM_DEBUG)
            System.out.println("CMFileSyncManager::sendFileList() called..");

        String userName;
        String serverName;
        List<Path> pathList;

        // create START_FILE_LIST event.
        CMFileSyncEvent fse = new CMFileSyncEvent();
        fse.setID(CMFileSyncEvent.START_FILE_LIST);
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
            System.out.println("CMFileSyncManager.checkNewTransferForSync() called..");
            System.out.println("file event = " + fe);
        }
        // get the file name
        String fileName = fe.getFileName();
        // get the new file list
        String fileSender = fe.getFileSender();
        List<Path> newFileList = m_cmInfo.getFileSyncInfo().getSyncGeneratorHashtable()
                .get(fileSender).getNewFileList();
        if(newFileList == null) {
            System.err.println("newFileList is null!");
            return;
        }
        // search for the fileName in the newFileList
        Path foundPath = null;
        for(Path path : newFileList) {
            if(path.endsWith(fileName)) {
                foundPath = path;
                break;
            }
        }
        if(foundPath != null) {
            // get the file-transfer home
            Path transferFileHome = m_cmInfo.getConfigurationInfo().getTransferedFileHome().resolve(fileSender);
            // get the server sync home
            Path serverSyncHome = getServerSyncHome(fileSender);
            // move the transferred file to the sync home
            try {
                Files.move(transferFileHome.resolve(fileName), serverSyncHome.resolve(fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // get the client file entry
            List<CMFileSyncEntry> entryList = m_cmInfo.getFileSyncInfo().getFileEntryListHashtable()
                    .get(fileSender);
            if(entryList == null) {
                System.err.println("The entry list of user("+fileSender+") is null!");
                return;
            }
            // search for the corresponding client entry
            boolean searchResult = false;
            for(CMFileSyncEntry entry : entryList) {
                if(entry.getPathRelativeToHome().toString().equals(fileName)) {
                    // set the last-modified-time of the corresponding client file entry
                    try {
                        Files.setLastModifiedTime(serverSyncHome.resolve(fileName), entry.getLastModifiedTime());
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    searchResult = true;
                }
            }
            if(!searchResult) {
                System.err.println("No file entry found for ("+fileName+")!");
                return;
            }

            // complete the new-file-transfer
            completeNewFileTransfer(fileSender, foundPath);
        }
    }

    public boolean completeNewFileTransfer(String userName, Path path) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncManager.completeNewFileTransfer() called..");
            System.out.println("userName = " + userName);
            System.out.println("path = " + path);
        }
        // get CMFileSyncGenerator
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorHashtable().get(userName);
        if(syncGenerator == null) {
            System.err.println("syncGenerator is null!");
            return false;
        }
        // set the isNewFileCompletedHashtable element
        syncGenerator.getIsNewFileCompletedHashtable().put(path, true);
        // update numNewFilesCompleted
        int numNewFilesCompleted = syncGenerator.getNumNewFilesCompleted();
        numNewFilesCompleted++;
        syncGenerator.setNumNewFilesCompleted(numNewFilesCompleted);

        // create a COMPLETE_NEW_FILE event
        String serverName = m_cmInfo.getInteractionInfo().getMyself().getName();
        CMFileSyncEvent fse = new CMFileSyncEvent();
        fse.setID(CMFileSyncEvent.COMPLETE_NEW_FILE);
        fse.setSender(serverName);
        fse.setReceiver(userName);
        fse.setUserName(userName);
        fse.setCompletedPath(path);

        // send the event
        return CMEventManager.unicastEvent(fse, userName, m_cmInfo);
    }

}
