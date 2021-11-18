package kr.ac.konkuk.ccslab.cm.event.handler;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class CMFileSyncEventHandler extends CMEventHandler {

    public CMFileSyncEventHandler(CMInfo cmInfo) {
        super(cmInfo);
        m_nType = CMInfo.CM_FILE_SYNC_EVENT_HANDLER;
    }

    @Override
    public boolean processEvent(CMEvent event) {
        boolean processResult;
        CMFileSyncEvent fse = (CMFileSyncEvent) event;
        int eventId = fse.getID();
        switch (eventId) {
            case CMFileSyncEvent.START_FILE_LIST:
                processResult = processSTART_FILE_LIST(fse);
                break;
            case CMFileSyncEvent.START_FILE_LIST_ACK:
                processResult = processSTART_FILE_LIST_ACK(fse);
                break;
            case CMFileSyncEvent.FILE_ENTRIES:
                processResult = processFILE_ENTRIES(fse);
                break;
            case CMFileSyncEvent.FILE_ENTRIES_ACK:
                processResult = processFILE_ENTRIES_ACK(fse);
                break;
            case CMFileSyncEvent.END_FILE_LIST:
                processResult = processEND_FILE_LIST(fse);
                break;
            case CMFileSyncEvent.END_FILE_LIST_ACK:
                processResult = processEND_FILE_LIST_ACK(fse);
                break;
            case CMFileSyncEvent.REQUEST_NEW_FILES:
                processResult = processREQUEST_NEW_FILES(fse);
                break;
            case CMFileSyncEvent.COMPLETE_NEW_FILE:
                processResult = processCOMPLETE_NEW_FILE(fse);
                break;
            case CMFileSyncEvent.COMPLETE_UPDATE_FILE:
                processResult = processCOMPLETE_UPDATE_FILE(fse);
                break;
            case CMFileSyncEvent.COMPLETE_FILE_SYNC:
                processResult = processCOMPLETE_FILE_SYNC(fse);
                break;
            default:
                System.err.println("CMFileSyncEventHandler::processEvent(), invalid event id(" + eventId + ")!");
                return false;
        }

        return processResult;
    }

    // called at the client
    private boolean processREQUEST_NEW_FILES(CMFileSyncEvent fse) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processREQUEST_NEW_FILES() called..");
            System.out.println("event = " + fse);
        }
        //// to use the CMFileTransferManager service to push new files to the server

        // get CMFileSyncManager
        CMFileSyncManager syncManager = (CMFileSyncManager) m_cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        // get the client sync home
        Path clientSyncHome = syncManager.getClientSyncHome();
        // get the requester name
        String requesterName = fse.getRequesterName();  // server name
        // check if the requested file list is null or empty
        List<Path> requestedFileList = fse.getRequestedFileList();
        if(requestedFileList == null) {
            System.err.println("requestedFileList is null!");
            return false;
        }
        else if(requestedFileList.isEmpty()) {
            System.err.println("requestedFileList is empty!");
            return false;
        }
        // use file-push service of the CMFileTransferManager for each element of the requested file list
        boolean sendResult = true;
        for(Path path : requestedFileList) {
            Path syncPath = clientSyncHome.resolve(path);   // adjust the path with the sync home
            if( !CMFileTransferManager.pushFile(syncPath.toString(), requesterName, m_cmInfo) )
                sendResult = false;
        }

        // The next sync task will be conducted at the CMFileTransferManager,
        // when the transmission of each requested file completes
        // by moving the transferred file to the server sync home.

        return sendResult;
    }

    // called at the server
    private boolean processSTART_FILE_LIST(CMFileSyncEvent fse) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processSTART_FILE_LIST() called..");
            System.out.println("event = " + fse);
        }

        String userName = fse.getUserName();
        // get the file-sync manager
        CMFileSyncManager fsManager = (CMFileSyncManager) m_cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        // get server sync home for userName
        Path serverSyncHome = fsManager.getServerSyncHome(userName);
        // check and create the server sync home
        if (Files.notExists(serverSyncHome)) {
            try {
                Files.createDirectories(serverSyncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // create the ack event
        CMFileSyncEvent ackFse = new CMFileSyncEvent();
        ackFse.setID(CMFileSyncEvent.START_FILE_LIST_ACK);
        ackFse.setSender(fse.getReceiver());  // server name
        ackFse.setReceiver(userName);
        ackFse.setUserName(userName);
        ackFse.setNumTotalFiles(fse.getNumTotalFiles());
        ackFse.setReturnCode(1);    // always success

        // send the ack event to the client

        return CMEventManager.unicastEvent(ackFse, userName, m_cmInfo);
    }

    // called at the client
    private boolean processSTART_FILE_LIST_ACK(CMFileSyncEvent fse) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processSTART_FILE_LIST_ACK() called..");
            System.out.println("event = " + fse);
        }

        String server = fse.getSender();

        // create a FILE_ENTRIES event
        CMFileSyncEvent newfse = new CMFileSyncEvent();
        newfse.setID(CMFileSyncEvent.FILE_ENTRIES);
        newfse.setSender(fse.getReceiver());  // user name
        newfse.setReceiver(server);  // server name
        newfse.setUserName(fse.getUserName());    // user name
        newfse.setNumFilesCompleted(0); // initialized to 0
        // set numFiles and fileEntryList
        setNumFilesAndEntryList(newfse, 0);

        return CMEventManager.unicastEvent(newfse, server, m_cmInfo);
    }

    // called at the client
    private CMFileSyncEvent setNumFilesAndEntryList(CMFileSyncEvent newfse, int startListIndex) {
        // get current number of bytes except the entry list
        int curByteNum = newfse.getByteNum();
        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.setNumFilesAndEntryList() called..");
            System.out.println("startListIndex = " + startListIndex);
            System.out.println("curByteNum before adding entries = " + curByteNum);
        }
        // set variables before the while loop
        List<Path> pathList = m_cmInfo.getFileSyncInfo().getPathList();
        List<Path> subList = new ArrayList<>();
        int index = startListIndex;
        int numFiles = 0;
        CMFileSyncManager fsManager = (CMFileSyncManager) m_cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        Path clientSyncHome = fsManager.getClientSyncHome();
        int startPathIndex = clientSyncHome.getNameCount();
        // create sub-list that will be added as the file-entry-list to the event
        while (curByteNum < CMInfo.MAX_EVENT_SIZE && index < pathList.size()) {
            Path path = pathList.get(index);
            // change the absolute path to the relative path
            Path relativePath = path.subpath(startPathIndex, path.getNameCount());
            curByteNum += CMInfo.STRING_LEN_BYTES_LEN
                    + relativePath.toString().getBytes().length
                    + Long.BYTES
                    + Long.BYTES;
            if (curByteNum < CMInfo.MAX_EVENT_SIZE) {
                subList.add(path);  // add the absolute path because it will be used to get meta-data.
                numFiles++;
                index++;
            } else {
                break;
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("absolute path = " + path);
                System.out.println("relative path = " + relativePath);
            }
        }

        // set numFiles
        newfse.setNumFiles(numFiles);
        // make an entry list from the subList
        List<CMFileSyncEntry> fileEntryList = subList.stream()
                .map(path -> {
                    CMFileSyncEntry fileEntry = new CMFileSyncEntry();
                    try {
                        fileEntry.setPathRelativeToHome(path.subpath(startPathIndex, path.getNameCount()))
                                .setSize(Files.size(path))
                                .setLastModifiedTime(Files.getLastModifiedTime(path));
                        if (CMInfo._CM_DEBUG)
                            System.out.println("fileEntry = " + fileEntry);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                    return fileEntry;
                }).collect(Collectors.toList());

        if (fileEntryList.isEmpty())
            System.err.println("fileEntryList is empty.");
        else
            newfse.setFileEntryList(fileEntryList);

        return newfse;
    }

    // called at the server
    private boolean processFILE_ENTRIES(CMFileSyncEvent fse) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processFILE_ENTRIES() called..");
            System.out.println("event = " + fse);
        }

        String userName = fse.getUserName();
        int returnCode = 1;
        int numFilesCompleted = 0;
        int numFiles = fse.getNumFiles();

        // if 0, the entry list is null in the event
        if(numFiles > 0) {
            // set or add the entry list of the event to the entry hashtable
            List<CMFileSyncEntry> entryList = m_cmInfo.getFileSyncInfo().getFileEntryListHashtable().get(userName);
            if (entryList == null) {
                // set the new entry list to the hashtable
                m_cmInfo.getFileSyncInfo().getFileEntryListHashtable().put(userName, fse.getFileEntryList());
                // set the number of completed files
                numFilesCompleted = numFiles;
            } else {
                // add the new entry list to the existing list
                boolean addResult = entryList.addAll(fse.getFileEntryList());
                if(!addResult) {
                    System.err.println("entry list add error!");
                    System.err.println("existing list = "+entryList);
                    System.err.println("new list = "+fse.getFileEntryList());
                    returnCode = 0;
                    numFilesCompleted = numFiles;
                }
                else {
                    // update the number of completed files
                    numFilesCompleted = fse.getNumFilesCompleted() + numFiles;
                }
            }
        }
        System.out.println("numFilesCompleted = " + numFilesCompleted);
        System.out.println("returnCode = " + returnCode);

        // create FILE_ENTRIES_ACK event
        CMFileSyncEvent fseAck = new CMFileSyncEvent();
        fseAck.setID(CMFileSyncEvent.FILE_ENTRIES_ACK);
        fseAck.setSender(fse.getReceiver());  // server
        fseAck.setReceiver(fse.getSender());  // client
        fseAck.setUserName(fse.getUserName());
        fseAck.setNumFilesCompleted(numFilesCompleted);   // updated
        fseAck.setNumFiles(numFiles);

        // send the ack event
        return CMEventManager.unicastEvent(fseAck, userName, m_cmInfo);
    }

    // called at the client
    private boolean processFILE_ENTRIES_ACK(CMFileSyncEvent fse) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processFILE_ENTRIES_ACK() called..");
            System.out.println("event = " + fse);
        }

        // check the return code
        int returnCode = fse.getReturnCode();
        if (returnCode == 0) {
            System.err.println("return code = " + returnCode);
            return false;
        }

        // check if there are remaining file entry elements to be sent
        int numFilesCompleted = fse.getNumFilesCompleted();
        int pathListSize = m_cmInfo.getFileSyncInfo().getPathList().size();
        boolean result;
        if (numFilesCompleted < pathListSize) {
            // send the next elements
            result = sendNextFileEntries(fse);
        } else if (numFilesCompleted == pathListSize) {
            // send the END_FILE_LIST event
            result = sendEND_FILE_LIST(fse);
        } else {
            System.err.println("numFilesCompleted = " + numFilesCompleted);
            System.err.println("pathListSize = " + pathListSize);
            return false;
        }

        return result;
    }

    // called at the client
    private boolean sendEND_FILE_LIST(CMFileSyncEvent fse) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.sendEND_FILE_LIST() called..");
        }

        // crate an END_FILE_LIST event
        CMFileSyncEvent newfse = new CMFileSyncEvent();
        newfse.setID(CMFileSyncEvent.END_FILE_LIST);
        newfse.setSender(fse.getReceiver());  // client
        String server = fse.getSender();
        newfse.setReceiver(server);  // server
        newfse.setUserName(fse.getUserName());
        newfse.setNumFilesCompleted(fse.getNumFilesCompleted());

        // send the event to the server
        return CMEventManager.unicastEvent(newfse, server, m_cmInfo);
    }

    // called at the client
    private boolean sendNextFileEntries(CMFileSyncEvent fse) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.sendNextFileEntries() called..");
        }

        CMFileSyncInfo syncInfo = m_cmInfo.getFileSyncInfo();
        // create FILE_ENTRIES event
        CMFileSyncEvent newfse = new CMFileSyncEvent();
        newfse.setID(CMFileSyncEvent.FILE_ENTRIES);
        newfse.setSender(fse.getReceiver());  // client
        String server = fse.getSender();
        newfse.setReceiver(server);  // server
        newfse.setUserName(fse.getUserName());    // client
        newfse.setNumFilesCompleted(fse.getNumFilesCompleted());

        // set numFiles and fileEntryList
        int startListIndex = fse.getNumFilesCompleted();
        setNumFilesAndEntryList(newfse, startListIndex);

        // send FILE_ENTRIES event
        return CMEventManager.unicastEvent(newfse, server, m_cmInfo);
    }

    // called at the server
    private boolean processEND_FILE_LIST(CMFileSyncEvent fse) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processEND_FILE_LIST() called..");
            System.out.println("fse = " + fse);
        }

        int returnCode;

        // check the elements of file entry list
        String userName = fse.getUserName();
        int numFilesCompleted = fse.getNumFilesCompleted();
        List<CMFileSyncEntry> fileEntryList = m_cmInfo.getFileSyncInfo().getFileEntryListHashtable()
                .get(userName);
        int numFileEntries;
        // the fileEntryList can be null if the client has no file-entry.
        if(fileEntryList == null)
            numFileEntries = 0;
        else
            numFileEntries = fileEntryList.size();

        if (numFileEntries == numFilesCompleted) {
            returnCode = 1;
        } else {
            returnCode = 0;
        }

        // create an END_FILE_LIST_ACK event
        CMFileSyncEvent fseAck = new CMFileSyncEvent();
        fseAck.setID(CMFileSyncEvent.END_FILE_LIST_ACK);
        fseAck.setSender(fse.getReceiver());  // server
        fseAck.setReceiver(fse.getSender());  // client
        fseAck.setUserName(userName);
        fseAck.setNumFilesCompleted(numFilesCompleted);
        fseAck.setReturnCode(returnCode);

        // send the ack event
        boolean result = CMEventManager.unicastEvent(fseAck, userName, m_cmInfo);
        if (!result) {
            System.err.println("send END_FILE_LIST_ACK error!");
            return false;
        }

        // start CMFileSyncGeneratorTask
        CMFileSyncGenerator fileSyncGenerator = new CMFileSyncGenerator(userName, m_cmInfo);
        ExecutorService es = m_cmInfo.getThreadInfo().getExecutorService();
        es.submit(fileSyncGenerator);
        // set the generator in the CMFileSyncInfo
        m_cmInfo.getFileSyncInfo().getSyncGeneratorHashtable().put(userName, fileSyncGenerator);

        return true;
    }

    // called at the client
    private boolean processEND_FILE_LIST_ACK(CMFileSyncEvent fse) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processEND_FILE_LIST_ACK() called..");
            System.out.println("fse = " + fse);
        }

        return true;
    }

    // called by the client
    private boolean processCOMPLETE_NEW_FILE(CMFileSyncEvent fse) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processCOMPLETE_NEW_FILE() called..");
            System.out.println("fse = " + fse);
        }
        // update info for the new-file completion at the client
        CMFileSyncInfo syncInfo = m_cmInfo.getFileSyncInfo();
        Hashtable<Path, Boolean> isFileSyncCompletedHashtable = syncInfo.getIsFileSyncCompletedHashtable();
        isFileSyncCompletedHashtable.put(fse.getCompletedPath(), true);

        return true;
    }

    // called by the client
    private boolean processCOMPLETE_UPDATE_FILE(CMFileSyncEvent fse) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processCOMPLETE_UPDATE_FILE() called..");
            System.out.println("fse = " + fse);
        }
        // update info for the file-update completion at the client
        CMFileSyncInfo syncInfo = m_cmInfo.getFileSyncInfo();
        Hashtable<Path, Boolean> isFileSyncCompletedHashtable = syncInfo.getIsFileSyncCompletedHashtable();
        isFileSyncCompletedHashtable.put(fse.getCompletedPath(), true);

        return true;
    }

    // called by the client
    private boolean processCOMPLETE_FILE_SYNC(CMFileSyncEvent fse) {

        // TODO: from here
        return true;
    }
}
