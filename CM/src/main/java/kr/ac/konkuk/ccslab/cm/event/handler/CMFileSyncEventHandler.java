package kr.ac.konkuk.ccslab.cm.event.handler;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class CMFileSyncEventHandler extends CMEventHandler {

    public CMFileSyncEventHandler(CMInfo cmInfo) {
        super(cmInfo);
        m_nType = CMInfo.CM_FILE_SYNC_EVENT_HANDLER;
    }

    @Override
    public boolean processEvent(CMEvent event) {
        boolean processResult;
        CMFileSyncEvent fse = (CMFileSyncEvent)event;
        int eventId = fse.getID();
        switch(eventId) {
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
            default:
                System.err.println("CMFileSyncEventHandler::processEvent(), invalid event id(" + eventId + ")!");
                return false;
        }

        return processResult;
    }

    // called at the server
    private boolean processSTART_FILE_LIST(CMFileSyncEvent fse) {

        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processSTART_FILE_LIST() called..");
            System.out.println("event = "+fse);
        }

        String userName = fse.getUserName();
        // get the file-sync manager
        CMFileSyncManager fsManager = (CMFileSyncManager) m_cmInfo.getServiceManagerHashtable()
                .get(CMInfo.CM_FILE_SYNC_MANAGER);
        // get server sync home for userName
        Path serverSyncHome = fsManager.getServerSyncHome(userName);
        // check and create the server sync home
        if(Files.notExists(serverSyncHome)) {
            try {
                Files.createDirectories(serverSyncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // check the file-entry-list hashtable
        List<CMFileSyncEntry> entryList = m_cmInfo.getFileSyncInfo().getFileEntryListHashtable().get(userName);
        if( entryList == null ) {
            // create a new list
            entryList = new Vector<>();
            m_cmInfo.getFileSyncInfo().getFileEntryListHashtable().put(userName, entryList);
        }
        else {
            // emtpry the list
            entryList.clear();
        }

        // create the ack event
        CMFileSyncEvent ackFse = new CMFileSyncEvent();
        ackFse.setID(CMFileSyncEvent.START_FILE_LIST_ACK);
        ackFse.setSender( fse.getReceiver() );  // server name
        ackFse.setReceiver( userName );
        ackFse.setUserName( userName );
        ackFse.setNumTotalFiles( fse.getNumTotalFiles() );
        ackFse.setReturnCode(1);    // always success

        // send the ack event to the client

        return CMEventManager.unicastEvent(ackFse, userName, m_cmInfo);
    }

    // processed at the client
    private boolean processSTART_FILE_LIST_ACK(CMFileSyncEvent fse) {

        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processSTART_FILE_LIST_ACK() called..");
            System.out.println("event = "+fse);
        }

        // create a FILE_ENTRIES event
        CMFileSyncEvent newfse = new CMFileSyncEvent();
        newfse.setID(CMFileSyncEvent.FILE_ENTRIES);
        newfse.setSender( fse.getReceiver() );  // user name
        newfse.setReceiver( fse.getSender() );  // server name
        newfse.setNumFilesCompleted(0); // initialized to 0
        // get numFiles and fileEntryList
        newfse = setNumFilesAndEntryList(newfse, 0);
        // from here
        return false;
    }

    private CMFileSyncEvent setNumFilesAndEntryList(CMFileSyncEvent newfse, int startListIndex) {
        // get current number of bytes except the entry list
        int curByteNum = newfse.getByteNum();
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.setNumFilesAndEntryList() called..");
            System.out.println("curByteNum before adding entries = " + curByteNum);
        }
        // add file entry element until the maximum event size
        String userName = newfse.getSender();
        List<Path> pathList = m_cmInfo.getFileSyncInfo().getPathList();
        List<Path> subList = new ArrayList<>();
        int index = startListIndex;
        int numFiles = 0;
        while( curByteNum < CMInfo.MAX_EVENT_SIZE && index < pathList.size() ) {
            Path path = pathList.get(index);
            curByteNum += CMInfo.STRING_LEN_BYTES_LEN
                    + path.toString().getBytes().length
                    + Long.BYTES
                    + Long.BYTES;
            if( curByteNum < CMInfo.MAX_EVENT_SIZE ) {
                subList.add(path);
                numFiles++;
                index++;
            }
            else {
                break;
            }
        }

        // set numFiles
        newfse.setNumFiles(numFiles);
        // make an entry list from the subList

        // from here

        return newfse;
    }

    private boolean processFILE_ENTRIES(CMFileSyncEvent fse) {
        return false;
    }

    private boolean processFILE_ENTRIES_ACK(CMFileSyncEvent fse) {
        return false;
    }

    private boolean processEND_FILE_LIST(CMFileSyncEvent fse) {
        return false;
    }

    private boolean processEND_FILE_LIST_ACK(CMFileSyncEvent fse) {
        return false;
    }
}
