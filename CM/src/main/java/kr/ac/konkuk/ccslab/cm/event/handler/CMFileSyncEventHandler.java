package kr.ac.konkuk.ccslab.cm.event.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncChangeLogEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncClientEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncPullModifyState;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncPushModifyState;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncStateKey;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.entity.CMUserLoginKey;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.*;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.info.CMThreadInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileType;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncProgress;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncPullGenerator;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncPushGenerator;
import kr.ac.konkuk.ccslab.cm.util.CMUtil;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

public class CMFileSyncEventHandler extends CMEventHandler {

    public CMFileSyncEventHandler() {
        super();
        m_nType = CMInfo.CM_FILE_SYNC_EVENT_HANDLER;
    }

    @Override
    public boolean processEvent(CMEvent event) {
        boolean processResult;
        CMFileSyncEvent fse = (CMFileSyncEvent) event;
        int eventId = fse.getID();
        switch (eventId) {
            case CMFileSyncEvent.START_FILE_LIST -> processResult = processSTART_FILE_LIST(fse);
            case CMFileSyncEvent.START_FILE_LIST_ACK -> processResult = processSTART_FILE_LIST_ACK(fse);
            case CMFileSyncEvent.FILE_ENTRIES -> processResult = processFILE_ENTRIES(fse);
            case CMFileSyncEvent.FILE_ENTRIES_ACK -> processResult = processFILE_ENTRIES_ACK(fse);
            case CMFileSyncEvent.END_FILE_LIST -> processResult = processEND_FILE_LIST(fse);
            case CMFileSyncEvent.END_FILE_LIST_ACK -> processResult = processEND_FILE_LIST_ACK(fse);
            case CMFileSyncEvent.REQUEST_NEW_FILES -> processResult = processREQUEST_NEW_FILES(fse);
            case CMFileSyncEvent.COMPLETE_NEW_FILE -> processResult = processCOMPLETE_NEW_FILE(fse);
            case CMFileSyncEvent.COMPLETE_UPDATE_FILE -> processResult = processCOMPLETE_UPDATE_FILE(fse);
            case CMFileSyncEvent.COMPLETE_DELETE_FILES -> processResult = processCOMPLETE_DELETE_FILES(fse);
            case CMFileSyncEvent.SKIP_UPDATE_FILE -> processResult = processSKIP_UPDATE_FILE(fse);
            case CMFileSyncEvent.COMPLETE_FILE_SYNC -> processResult = processCOMPLETE_FILE_SYNC(fse);
            case CMFileSyncEvent.START_FILE_BLOCK_CHECKSUM -> processResult = processSTART_FILE_BLOCK_CHECKSUM(fse);
            case CMFileSyncEvent.START_FILE_BLOCK_CHECKSUM_ACK -> processResult =
                    processSTART_FILE_BLOCK_CHECKSUM_ACK(fse);
            case CMFileSyncEvent.FILE_BLOCK_CHECKSUM -> processResult = processFILE_BLOCK_CHECKSUM(fse);
            case CMFileSyncEvent.END_FILE_BLOCK_CHECKSUM -> processResult = processEND_FILE_BLOCK_CHECKSUM(fse);
            case CMFileSyncEvent.END_FILE_BLOCK_CHECKSUM_ACK -> processResult = processEND_FILE_BLOCK_CHECKSUM_ACK(fse);
            case CMFileSyncEvent.UPDATE_EXISTING_FILE -> processResult = processUPDATE_EXISTING_FILE(fse);
            case CMFileSyncEvent.ONLINE_MODE_LIST -> processResult = processONLINE_MODE_LIST(fse);
            case CMFileSyncEvent.ONLINE_MODE_LIST_ACK -> processResult = processONLINE_MODE_LIST_ACK(fse);
            case CMFileSyncEvent.END_ONLINE_MODE_LIST -> processResult = processEND_ONLINE_MODE_LIST(fse);
            case CMFileSyncEvent.END_ONLINE_MODE_LIST_ACK -> processResult = processEND_ONLINE_MODE_LIST_ACK(fse);
            case CMFileSyncEvent.LOCAL_MODE_LIST -> processResult = processLOCAL_MODE_LIST(fse);
            case CMFileSyncEvent.LOCAL_MODE_LIST_ACK -> processResult = processLOCAL_MODE_LIST_ACK(fse);
            case CMFileSyncEvent.END_LOCAL_MODE_LIST -> processResult = processEND_LOCAL_MODE_LIST(fse);
            case CMFileSyncEvent.END_LOCAL_MODE_LIST_ACK -> processResult = processEND_LOCAL_MODE_LIST_ACK(fse);
            case CMFileSyncEvent.START_PULL_SYNC -> processResult = processSTART_PULL_SYNC(fse);
            case CMFileSyncEvent.START_PULL_SYNC_ACK -> processResult = processSTART_PULL_SYNC_ACK(fse);
            case CMFileSyncEvent.START_SERVER_ENTRY_LIST -> processResult = processSTART_SERVER_ENTRY_LIST(fse);
            case CMFileSyncEvent.START_SERVER_ENTRY_LIST_ACK -> processResult = processSTART_SERVER_ENTRY_LIST_ACK(fse);
            case CMFileSyncEvent.SERVER_ENTRIES -> processResult = processSERVER_ENTRIES(fse);
            case CMFileSyncEvent.SERVER_ENTRIES_ACK -> processResult = processSERVER_ENTRIES_ACK(fse);
            case CMFileSyncEvent.END_SERVER_ENTRY_LIST -> processResult = processEND_SERVER_ENTRY_LIST(fse);
            case CMFileSyncEvent.END_SERVER_ENTRY_LIST_ACK -> processResult = processEND_SERVER_ENTRY_LIST_ACK(fse);
            case CMFileSyncEvent.COMPLETE_PULL_DELETE -> processResult = processCOMPLETE_PULL_DELETE(fse);
            case CMFileSyncEvent.COMPLETE_PULL_CREATE -> processResult = processCOMPLETE_PULL_CREATE(fse);
            case CMFileSyncEvent.COMPLETE_PULL_MODIFY -> processResult = processCOMPLETE_PULL_MODIFY(fse);
            case CMFileSyncEvent.COMPLETE_PULL_SYNC -> processResult = processCOMPLETE_PULL_SYNC(fse);
            case CMFileSyncEvent.COMPLETE_PULL_SYNC_ACK -> processResult = processCOMPLETE_PULL_SYNC_ACK(fse);
            case CMFileSyncEvent.REQUEST_PULL_CREATES -> processResult = processREQUEST_PULL_CREATES(fse);
            case CMFileSyncEvent.START_PUSH_ENTRY_LIST -> processResult = processSTART_PUSH_ENTRY_LIST(fse);
            case CMFileSyncEvent.START_PUSH_ENTRY_LIST_ACK -> processResult = processSTART_PUSH_ENTRY_LIST_ACK(fse);
            case CMFileSyncEvent.PUSH_ENTRIES -> processResult = processPUSH_ENTRIES(fse);
            case CMFileSyncEvent.PUSH_ENTRIES_ACK -> processResult = processPUSH_ENTRIES_ACK(fse);
            case CMFileSyncEvent.END_PUSH_ENTRY_LIST -> processResult = processEND_PUSH_ENTRY_LIST(fse);
            case CMFileSyncEvent.END_PUSH_ENTRY_LIST_ACK -> processResult = processEND_PUSH_ENTRY_LIST_ACK(fse);
            case CMFileSyncEvent.COMPLETE_PUSH_DELETE -> processResult = processCOMPLETE_PUSH_DELETE(fse);
            case CMFileSyncEvent.COMPLETE_PUSH_CREATE -> processResult = processCOMPLETE_PUSH_CREATE(fse);
            case CMFileSyncEvent.COMPLETE_PUSH_SYNC -> processResult = processCOMPLETE_PUSH_SYNC(fse);
            case CMFileSyncEvent.COMPLETE_PUSH_SYNC_ACK -> processResult = processCOMPLETE_PUSH_SYNC_ACK(fse);
            default -> {
                System.err.println("CMFileSyncEventHandler::processEvent(), invalid event id(" + eventId + ")!");
                return false;
            }
        }

        return processResult;
    }

    // called at the server
    private boolean processSTART_PULL_SYNC(CMFileSyncEvent fse) {
        CMFileSyncEventStartPullSync fse_sps = (CMFileSyncEventStartPullSync) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_PULL_SYNC() called..");
            System.out.println("fse_sps = " + fse_sps);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        String initiatorName = fse_sps.getInitiatorName();
        UUID initiatorUuid = fse_sps.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_sps.getInitiatorDeviceUuid();

        long clientCursor = fse_sps.getCursor();
        int returnCode;
        boolean isPullSync = false;

        // get the server-side cursor (last applied change id) for this client device
        long lastChangeId = syncInfo.getIndexRegistry().getOrLoad(initiatorName, initiatorDeviceUuid).lastChangeId();

        // decide the return code by comparing the client cursor with the server cursor
        if(lastChangeId == 0) {
            // no sync history on the server -> client should perform full push sync
            returnCode = 0;
        } else if(lastChangeId == clientCursor) {
            // client is already up to date
            returnCode = 1;
        } else if(lastChangeId > clientCursor) {
            // client is behind -> pull sync needed
            returnCode = 2;
            isPullSync = true;
        } else {
            // server cursor < client cursor -> error, full push sync needed
            returnCode = -1;
        }

        // create and send the START_PULL_SYNC_ACK event
        CMFileSyncEventStartPullSyncAck ackEvent = new CMFileSyncEventStartPullSyncAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(initiatorName);
        ackEvent.setInitiatorUuid(initiatorUuid);
        ackEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // 나머지 필드 설정
        ackEvent.setReturnCode(returnCode);
        ackEvent.setServerCursor(lastChangeId);

        boolean sendResult = CMEventManager.unicastEvent(ackEvent, initiatorName, initiatorUuid);
        if(!sendResult) {
            System.err.println("send error: " + ackEvent);
            return false;
        }

        // if pull sync is needed, start sending the server changelog entry list to the client
        if(isPullSync) {
            sendResult = startServerEntryList(clientCursor, lastChangeId, initiatorName, initiatorUuid,
                    initiatorDeviceUuid);
            if(!sendResult) {
                System.err.println("CMFileSyncEventHandler.processSTART_PULL_SYNC(), startServerEntryList error!");
                return false;
            }
        }

        return true;
    }

    // called at the server
    private boolean startServerEntryList(long clientCursor, long serverCursor, String initiatorName,
                                         UUID initiatorUuid, UUID initiatorDeviceUuid) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.startServerEntryList() called..");
            System.out.println("clientCursor = " + clientCursor + ", serverCursor = " + serverCursor);
            System.out.println("initiatorName = " + initiatorName);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);

        // read the changelog entries in the range (clientCursor, serverCursor], sorted by change id
        List<CMFileSyncChangeLogEntry> changeLogEntries =
                syncInfo.readChangeLogEntries(initiatorName, clientCursor, serverCursor);

        // keep only the latest changelog entry per path (entries are in change-id order)
        Map<String, CMFileSyncChangeLogEntry> latestEntryByPath = new LinkedHashMap<>();
        for(CMFileSyncChangeLogEntry entry : changeLogEntries) {
            latestEntryByPath.put(entry.getPath(), entry);
        }

        // build the server entry list to send to the client and store it in the serverEntryMap
        List<CMFileSyncChangeLogEntry> serverEntryList = new ArrayList<>(latestEntryByPath.values());
        syncInfo.getServerEntryMap().put(stateKey, serverEntryList);

        // build the pull-state map (relative path -> client entry) for tracking pull-sync completion
        Map<String, CMFileSyncClientEntry> pullStateMap = new LinkedHashMap<>();
        for(CMFileSyncChangeLogEntry entry : serverEntryList) {
            CMFileSyncClientEntry clientEntry = new CMFileSyncClientEntry()
                    .setPath(entry.getPath())
                    .setSize(entry.getSize())
                    .setOpHint(entry.getOp());
            pullStateMap.put(entry.getPath(), clientEntry);
        }
        syncInfo.getPullStateTable().put(stateKey, pullStateMap);

        if(CMInfo._CM_DEBUG) {
            System.out.println("serverEntryList size = " + serverEntryList.size());
            System.out.println("serverEntryList = " + serverEntryList);
        }

        // notify the client of the start of the server-entry-list transmission.
        // the actual SERVER_ENTRIES batches are sent when the START_SERVER_ENTRY_LIST_ACK is received.
        CMFileSyncEventStartServerEntryList fse_ssel = new CMFileSyncEventStartServerEntryList();
        // 공통 필드 설정
        fse_ssel.setInitiatorName(initiatorName);
        fse_ssel.setInitiatorUuid(initiatorUuid);
        fse_ssel.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // 나머지 필드 설정
        fse_ssel.setNumTotalFiles(serverEntryList.size());

        boolean sendResult = CMEventManager.unicastEvent(fse_ssel, initiatorName, initiatorUuid);
        if(!sendResult) {
            System.err.println("send error: " + fse_ssel);
            return false;
        }

        return true;
    }

    // called at the client
    private boolean processSTART_PULL_SYNC_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventStartPullSyncAck ackEvent = (CMFileSyncEventStartPullSyncAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_PULL_SYNC_ACK() called..");
            System.out.println("ackEvent = " + ackEvent);
        }

        int returnCode = ackEvent.getReturnCode();
        long serverCursor = ackEvent.getServerCursor();
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);

        boolean result = true;
        if(returnCode == -1) {
            // error: server cursor < client cursor -> recover with full push sync
            System.err.println("CMFileSyncEventHandler.processSTART_PULL_SYNC_ACK(), server cursor("
                    + serverCursor + ") < client cursor; starting full push sync.");
            // PULL -> FULL_SYNC 전이: startFullPushSync() 의 진행중 가드를 통과시키기 위해 NONE 으로 리셋
            syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
            result = syncManager.startFullPushSync();
        } else if(returnCode == 0) {
            // no sync history on the server -> full push sync
            if(CMInfo._CM_DEBUG) {
                System.out.println("no sync history on the server; starting full push sync.");
            }
            // PULL -> FULL_SYNC 전이: startFullPushSync() 의 진행중 가드를 통과시키기 위해 NONE 으로 리셋
            syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
            result = syncManager.startFullPushSync();
        } else if(returnCode == 1) {
            // client is already up to date -> end the sync session
            if(CMInfo._CM_DEBUG) {
                System.out.println("client is up to date with the server; nothing to sync.");
            }
            syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
        } else if(returnCode == 2) {
            // pull sync needed -> save the server cursor and wait for START_SERVER_ENTRY_LIST
            if(CMInfo._CM_DEBUG) {
                System.out.println("pull sync needed; serverCursor = " + serverCursor);
            }
            syncInfo.setServerCursor(serverCursor);
            // the server sends START_SERVER_ENTRY_LIST right after this ack.
        } else {
            System.err.println("CMFileSyncEventHandler.processSTART_PULL_SYNC_ACK(), invalid returnCode("
                    + returnCode + ")!");
            return false;
        }

        return result;
    }

    // called at the client
    private boolean processSTART_SERVER_ENTRY_LIST(CMFileSyncEvent fse) {
        CMFileSyncEventStartServerEntryList fse_ssel = (CMFileSyncEventStartServerEntryList) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_SERVER_ENTRY_LIST() called..");
            System.out.println("fse_ssel = " + fse_ssel);
        }

        String initiatorName = fse_ssel.getInitiatorName();
        UUID initiatorUuid = fse_ssel.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_ssel.getInitiatorDeviceUuid();

        // get the file-sync manager and the client sync home
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        Path clientSyncHome = syncManager.getClientSyncHome();
        // check and create the client sync home
        if(Files.notExists(clientSyncHome)) {
            try {
                Files.createDirectories(clientSyncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // create the ack event
        CMFileSyncEventStartServerEntryListAck ackFse = new CMFileSyncEventStartServerEntryListAck();
        // 공통 필드 설정
        ackFse.setInitiatorName(initiatorName);
        ackFse.setInitiatorUuid(initiatorUuid);
        ackFse.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // 나머지 필드 설정
        ackFse.setNumTotalFiles(fse_ssel.getNumTotalFiles());
        ackFse.setReturnCode(1);    // always success

        // send the ack event to the server (the sender of the start event)
        return CMEventManager.unicastEvent(ackFse, fse_ssel.getSender(), fse_ssel.getSenderUuid());
    }

    // called at the server
    private boolean processSTART_SERVER_ENTRY_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventStartServerEntryListAck fse_ack = (CMFileSyncEventStartServerEntryListAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_SERVER_ENTRY_LIST_ACK() called..");
            System.out.println("fse_ack = " + fse_ack);
        }

        String initiatorName = fse_ack.getInitiatorName();
        UUID initiatorUuid = fse_ack.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_ack.getInitiatorDeviceUuid();

        // create the first SERVER_ENTRIES event; the next batches follow upon receiving its ack
        CMFileSyncEventServerEntries fse_se = new CMFileSyncEventServerEntries();
        // 공통 필드 설정
        fse_se.setInitiatorName(initiatorName);
        fse_se.setInitiatorUuid(initiatorUuid);
        fse_se.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // 나머지 필드 설정
        fse_se.setNumFilesCompleted(0);
        setServerNumFilesAndEntryList(fse_se, 0);

        return CMEventManager.unicastEvent(fse_se, initiatorName, initiatorUuid);
    }

    // called at the server
    private CMFileSyncEvent setServerNumFilesAndEntryList(CMFileSyncEventServerEntries fse_se, int startListIndex) {
        // current number of bytes except the entry list
        int curByteNum = fse_se.getByteNum();
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.setServerNumFilesAndEntryList() called..");
            System.out.println("startListIndex = " + startListIndex);
            System.out.println("curByteNum before adding entries = " + curByteNum);
        }

        String initiatorName = fse_se.getInitiatorName();
        UUID initiatorDeviceUuid = fse_se.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        List<CMFileSyncChangeLogEntry> serverEntryList = syncInfo.getServerEntryMap().get(stateKey);
        if(serverEntryList == null) {
            System.err.println("CMFileSyncEventHandler.setServerNumFilesAndEntryList(), " +
                    "serverEntryList is null for stateKey = " + stateKey);
            fse_se.setNumFiles(0);
            return fse_se;
        }

        List<CMFileSyncChangeLogEntry> subList = new ArrayList<>();
        int index = startListIndex;
        int numFiles = 0;

        // add change-log entries to the sub-list while the event size stays under the limit.
        // each entry is marshalled as a JSON string (see CMFileSyncEventServerEntries), so the
        // size estimate uses that wire format rather than a field-by-field count.
        while(curByteNum < CMInfo.MAX_EVENT_SIZE && index < serverEntryList.size()) {
            CMFileSyncChangeLogEntry entry = serverEntryList.get(index);
            int entryBytes;
            try {
                entryBytes = CMInfo.STRING_LEN_BYTES_LEN + entry.toJsonString().getBytes().length;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return fse_se;
            }
            curByteNum += entryBytes;
            if(curByteNum < CMInfo.MAX_EVENT_SIZE) {
                subList.add(entry);
                numFiles++;
                index++;
            } else {
                break;
            }
        }

        // set numFiles and the entry sub-list
        fse_se.setNumFiles(numFiles);
        if(subList.isEmpty()) {
            System.err.println("CMFileSyncEventHandler.setServerNumFilesAndEntryList(), subList is empty!");
        } else {
            fse_se.setServerEntryList(subList);
        }

        return fse_se;
    }

    // called at the client
    private boolean processSERVER_ENTRIES(CMFileSyncEvent fse) {
        CMFileSyncEventServerEntries fse_se = (CMFileSyncEventServerEntries) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSERVER_ENTRIES() called..");
            System.out.println("fse_se = " + fse_se);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        List<CMFileSyncChangeLogEntry> serverEntryList = syncInfo.getServerEntryList();
        int returnCode = 1;
        int numFilesCompleted = 0;
        int numFiles = fse_se.getNumFiles();

        // append the server entry list in the event to the client-side serverEntryList
        if(numFiles > 0) {
            if(serverEntryList == null) {
                // set the new entry list
                syncInfo.setServerEntryList(fse_se.getServerEntryList());
                numFilesCompleted = numFiles;
            } else {
                // add the new entry list to the existing list
                boolean addResult = serverEntryList.addAll(fse_se.getServerEntryList());
                if(!addResult) {
                    System.err.println("CMFileSyncEventHandler.processSERVER_ENTRIES(), entry list add error!");
                    returnCode = 0;
                    numFilesCompleted = fse_se.getNumFilesCompleted();
                } else {
                    numFilesCompleted = fse_se.getNumFilesCompleted() + numFiles;
                }
            }
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("numFilesCompleted = " + numFilesCompleted);
            System.out.println("returnCode = " + returnCode);
        }

        // create and send the ack event to the server
        CMFileSyncEventServerEntriesAck ackEvent = new CMFileSyncEventServerEntriesAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(fse_se.getInitiatorName());
        ackEvent.setInitiatorUuid(fse_se.getInitiatorUuid());
        ackEvent.setInitiatorDeviceUuid(fse_se.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        ackEvent.setNumFilesCompleted(numFilesCompleted);
        ackEvent.setNumFiles(numFiles);
        ackEvent.setReturnCode(returnCode);

        return CMEventManager.unicastEvent(ackEvent, fse_se.getSender(), fse_se.getSenderUuid());
    }

    // called at the server
    private boolean processSERVER_ENTRIES_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventServerEntriesAck fse_sea = (CMFileSyncEventServerEntriesAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSERVER_ENTRIES_ACK() called..");
            System.out.println("fse_sea = " + fse_sea);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        String initiatorName = fse_sea.getInitiatorName();
        UUID initiatorDeviceUuid = fse_sea.getInitiatorDeviceUuid();

        // check the return code
        int returnCode = fse_sea.getReturnCode();
        if(returnCode == 0) {
            System.err.println("CMFileSyncEventHandler.processSERVER_ENTRIES_ACK(), return code = " + returnCode);
            return false;
        }

        // check if there are remaining server entries to send
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        int numFilesCompleted = fse_sea.getNumFilesCompleted();
        int serverEntryListSize = syncInfo.getServerEntryMap().get(stateKey).size();
        boolean sendResult;
        if(numFilesCompleted < serverEntryListSize) {
            // send the next batch
            sendResult = sendNextServerEntries(fse_sea);
        } else if(numFilesCompleted == serverEntryListSize) {
            // send the END_SERVER_ENTRY_LIST event
            sendResult = sendEND_SERVER_ENTRY_LIST(fse_sea);
        } else {
            System.err.println("CMFileSyncEventHandler.processSERVER_ENTRIES_ACK(), numFilesCompleted("
                    + numFilesCompleted + ") > serverEntryListSize(" + serverEntryListSize + ")!");
            return false;
        }

        return sendResult;
    }

    // called at the server
    private boolean sendNextServerEntries(CMFileSyncEventServerEntriesAck fse_sea) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.sendNextServerEntries() called..");
        }

        // create the next SERVER_ENTRIES event
        CMFileSyncEventServerEntries newfse = new CMFileSyncEventServerEntries();
        // 공통 필드 설정
        newfse.setInitiatorName(fse_sea.getInitiatorName());
        newfse.setInitiatorUuid(fse_sea.getInitiatorUuid());
        newfse.setInitiatorDeviceUuid(fse_sea.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        newfse.setNumFilesCompleted(fse_sea.getNumFilesCompleted());
        int startListIndex = fse_sea.getNumFilesCompleted();
        setServerNumFilesAndEntryList(newfse, startListIndex);

        // send the event to the client
        return CMEventManager.unicastEvent(newfse, fse_sea.getSender(), fse_sea.getSenderUuid());
    }

    // called at the server
    private boolean sendEND_SERVER_ENTRY_LIST(CMFileSyncEventServerEntriesAck fse_sea) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.sendEND_SERVER_ENTRY_LIST() called..");
        }

        // create the END_SERVER_ENTRY_LIST event
        CMFileSyncEventEndServerEntryList newfse = new CMFileSyncEventEndServerEntryList();
        // 공통 필드 설정
        newfse.setInitiatorName(fse_sea.getInitiatorName());
        newfse.setInitiatorUuid(fse_sea.getInitiatorUuid());
        newfse.setInitiatorDeviceUuid(fse_sea.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        newfse.setNumFilesCompleted(fse_sea.getNumFilesCompleted());

        // send the event to the client
        return CMEventManager.unicastEvent(newfse, fse_sea.getSender(), fse_sea.getSenderUuid());
    }

    // called at the client
    private boolean processEND_SERVER_ENTRY_LIST(CMFileSyncEvent fse) {
        CMFileSyncEventEndServerEntryList fse_esel = (CMFileSyncEventEndServerEntryList) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_SERVER_ENTRY_LIST() called..");
            System.out.println("fse_esel = " + fse_esel);
        }

        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        int numFilesCompleted = fse_esel.getNumFilesCompleted();
        List<CMFileSyncChangeLogEntry> serverEntryList = syncInfo.getServerEntryList();
        int returnCode = 1;

        // check the number of received server entries
        if(serverEntryList == null) {
            System.err.println("CMFileSyncEventHandler.processEND_SERVER_ENTRY_LIST(), serverEntryList is null!");
            returnCode = 0;
        } else if(numFilesCompleted != serverEntryList.size()) {
            System.err.println("CMFileSyncEventHandler.processEND_SERVER_ENTRY_LIST(), numFilesCompleted("
                    + numFilesCompleted + ") != serverEntryList size(" + serverEntryList.size() + ")!");
            returnCode = 0;
        }

        // create and send the ack event to the server
        CMFileSyncEventEndServerEntryListAck ackEvent = new CMFileSyncEventEndServerEntryListAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(fse_esel.getInitiatorName());
        ackEvent.setInitiatorUuid(fse_esel.getInitiatorUuid());
        ackEvent.setInitiatorDeviceUuid(fse_esel.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        ackEvent.setNumFilesCompleted(numFilesCompleted);
        ackEvent.setReturnCode(returnCode);

        boolean result = CMEventManager.unicastEvent(ackEvent, fse_esel.getSender(), fse_esel.getSenderUuid());
        if(!result) {
            System.err.println("CMFileSyncEventHandler.processEND_SERVER_ENTRY_LIST(), send ack error!");
            return false;
        }

        // stop if the entry-count check failed
        if(returnCode == 0) {
            return false;
        }

        // create the client path list and store it in CMFileSyncInfo
        Path syncHome = syncManager.getClientSyncHome();
        List<Path> clientPathList = syncManager.createPathList(syncHome);
        if(clientPathList == null) {
            System.err.println("CMFileSyncEventHandler.processEND_SERVER_ENTRY_LIST(), clientPathList is null!");
            return false;
        }
        syncInfo.setClientPathList(clientPathList);

        // compare each server entry with the client paths and classify into the pull maps
        result = syncManager.compareServerAndClientEntriesForPullSync();
        if(!result) {
            System.err.println("CMFileSyncEventHandler.processEND_SERVER_ENTRY_LIST(), " +
                    "compareServerAndClientEntriesForPullSync error!");
            return false;
        }

        // start the pull-map sync tasks (the pending push map runs after pull sync completes)
        result = syncManager.proceedPullMaps();
        if(!result) {
            System.err.println("CMFileSyncEventHandler.processEND_SERVER_ENTRY_LIST(), proceedPullMaps error!");
            return false;
        }

        return true;
    }

    // called at the server
    private boolean processEND_SERVER_ENTRY_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventEndServerEntryListAck fse_esels = (CMFileSyncEventEndServerEntryListAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_SERVER_ENTRY_LIST_ACK() called..");
            System.out.println("fse_esels = " + fse_esels);
        }

        return true;
    }

    // called at the server
    private boolean processCOMPLETE_PULL_DELETE(CMFileSyncEvent fse) {
        CMFileSyncEventCompletePullDelete fse_cpd = (CMFileSyncEventCompletePullDelete) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_PULL_DELETE() called..");
            System.out.println("fse_cpd = " + fse_cpd);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        String initiatorName = fse_cpd.getInitiatorName();
        UUID initiatorUuid = fse_cpd.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_cpd.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        List<String> deletedPathList = fse_cpd.getDeletedPathList();
        boolean result = true;

        // get the pull-state map for this client device
        Map<String, CMFileSyncClientEntry> pullStateMap = syncInfo.getPullStateTable().get(stateKey);
        if(pullStateMap == null) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_DELETE(), " +
                    "pullStateMap is null for stateKey = " + stateKey);
            return false;
        }

        // mark the deleted paths as completed in the pull-state map
        if(deletedPathList != null) {
            for(String deletedPath : deletedPathList) {
                CMFileSyncClientEntry entry = pullStateMap.get(deletedPath);
                if(entry != null) {
                    entry.setCompleted(true);
                } else {
                    System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_DELETE(), " +
                            "entry not found in pullStateMap for path: " + deletedPath);
                }
            }
        }

        // check whether the whole pull sync is complete
        if(syncManager.isCompletePullSync(pullStateMap)) {
            result = syncManager.completePullSync(initiatorName, initiatorUuid, initiatorDeviceUuid);
        }

        return result;
    }

    // called at the server
    private boolean processCOMPLETE_PULL_CREATE(CMFileSyncEvent fse) {
        CMFileSyncEventCompletePullCreate fse_cpc = (CMFileSyncEventCompletePullCreate) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_PULL_CREATE() called..");
            System.out.println("fse_cpc = " + fse_cpc);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        String initiatorName = fse_cpc.getInitiatorName();
        UUID initiatorUuid = fse_cpc.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_cpc.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        String createdPath = fse_cpc.getCreatedPath();
        boolean result = true;

        // get the pull-state map for this client device
        Map<String, CMFileSyncClientEntry> pullStateMap = syncInfo.getPullStateTable().get(stateKey);
        if(pullStateMap == null) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_CREATE(), " +
                    "pullStateMap is null for stateKey = " + stateKey);
            return false;
        }

        // mark the created path as completed in the pull-state map (single path per event)
        CMFileSyncClientEntry entry = pullStateMap.get(createdPath);
        if(entry != null) {
            entry.setCompleted(true);
        } else {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_CREATE(), " +
                    "entry not found in pullStateMap for path: " + createdPath);
            return false;
        }

        // check whether the whole pull sync is complete
        if(syncManager.isCompletePullSync(pullStateMap)) {
            result = syncManager.completePullSync(initiatorName, initiatorUuid, initiatorDeviceUuid);
        }

        return result;
    }

    // called at the server
    private boolean processCOMPLETE_PULL_MODIFY(CMFileSyncEvent fse) {
        CMFileSyncEventCompletePullModify fse_cpm = (CMFileSyncEventCompletePullModify) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_PULL_MODIFY() called..");
            System.out.println("fse_cpm = " + fse_cpm);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        String initiatorName = fse_cpm.getInitiatorName();
        UUID initiatorUuid = fse_cpm.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_cpm.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        String modifiedPath = fse_cpm.getModifiedPath();
        boolean result = true;

        // get the pull-state map for this client device
        Map<String, CMFileSyncClientEntry> pullStateMap = syncInfo.getPullStateTable().get(stateKey);
        if(pullStateMap == null) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_MODIFY(), " +
                    "pullStateMap is null for stateKey = " + stateKey);
            return false;
        }

        // mark the modified path as completed in the pull-state map (single path per event)
        CMFileSyncClientEntry entry = pullStateMap.get(modifiedPath);
        if(entry != null) {
            entry.setCompleted(true);
        } else {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_MODIFY(), " +
                    "entry not found in pullStateMap for path: " + modifiedPath);
            return false;
        }

        // check whether the whole pull sync is complete
        if(syncManager.isCompletePullSync(pullStateMap)) {
            result = syncManager.completePullSync(initiatorName, initiatorUuid, initiatorDeviceUuid);
        }

        return result;
    }

    // called at the client
    private boolean processCOMPLETE_PULL_SYNC(CMFileSyncEvent fse) {
        CMFileSyncEventCompletePullSync fse_cps = (CMFileSyncEventCompletePullSync) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC() called..");
            System.out.println("fse_cps = " + fse_cps);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        boolean isCompleted = true;
        int returnCode;

        // get the pull maps
        Map<String, CMFileSyncClientEntry> pullDeleteMap = syncInfo.getPullDeleteMap();
        Map<String, CMFileSyncClientEntry> pullCreateMap = syncInfo.getPullCreateMap();
        Map<String, CMFileSyncClientEntry> pullModifyMap = syncInfo.getPullModifyMap();

        // size check: total entries vs server-reported numFilesCompleted
        // (mismatch is logged but not fatal: conflict-rename failures can legitimately reduce the count)
        int sumMapSize = pullDeleteMap.size() + pullCreateMap.size() + pullModifyMap.size();
        if(sumMapSize != fse_cps.getNumFilesCompleted()) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC(), "
                    + "entry count mismatch: sumMapSize = " + sumMapSize
                    + ", numFilesCompleted = " + fse_cps.getNumFilesCompleted());
        }

        // verify every entry in each pull map is completed (continue scanning so all errors are logged)
        for(String key : pullDeleteMap.keySet()) {
            CMFileSyncClientEntry entry = pullDeleteMap.get(key);
            isCompleted &= entry.isCompleted();
            if(!entry.isCompleted()) {
                System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC(), "
                        + "pullDeleteMap entry not completed: " + key);
            }
        }
        for(String key : pullCreateMap.keySet()) {
            CMFileSyncClientEntry entry = pullCreateMap.get(key);
            isCompleted &= entry.isCompleted();
            if(!entry.isCompleted()) {
                System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC(), "
                        + "pullCreateMap entry not completed: " + key);
            }
        }
        for(String key : pullModifyMap.keySet()) {
            CMFileSyncClientEntry entry = pullModifyMap.get(key);
            isCompleted &= entry.isCompleted();
            if(!entry.isCompleted()) {
                System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC(), "
                        + "pullModifyMap entry not completed: " + key);
            }
        }

        returnCode = isCompleted ? 1 : 0;

        // send COMPLETE_PULL_SYNC_ACK to the server
        CMFileSyncEventCompletePullSyncAck ackEvent = new CMFileSyncEventCompletePullSyncAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(fse_cps.getInitiatorName());
        ackEvent.setInitiatorUuid(fse_cps.getInitiatorUuid());
        ackEvent.setInitiatorDeviceUuid(fse_cps.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        ackEvent.setNumFilesCompleted(fse_cps.getNumFilesCompleted());
        ackEvent.setReturnCode(returnCode);
        boolean sendResult = CMEventManager.unicastEvent(ackEvent, fse_cps.getSender(), fse_cps.getSenderUuid());
        if(!sendResult) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC(), "
                    + "failed to send COMPLETE_PULL_SYNC_ACK");
            return false;
        }

        // abort post-processing if not fully completed
        if(!isCompleted) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC(), "
                    + "not all pull entries completed; abort post-processing.");
            return false;
        }

        // persist in-memory online-mode list
        syncManager.saveOnlineModePathSizeMapToFile();

        // persist in-memory client-index and update cursor to serverCursor
        long serverCursor = syncInfo.getServerCursor();
        syncInfo.saveClientIndex(".", serverCursor);
        syncInfo.setCursor(serverCursor);
        syncInfo.saveClientCursor(".");

        // clear pull maps and reset per-session state
        pullDeleteMap.clear();
        pullCreateMap.clear();
        pullModifyMap.clear();
        syncInfo.setServerEntryList(null);
        syncInfo.setClientPathList(null);
        syncInfo.setServerCursor(-1L);

        // reset sync session state
        syncInfo.setSyncProgress(CMFileSyncProgress.NONE);

        // if pendingPushMap is non-empty, kick off push sync
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        if(!pendingPushMap.isEmpty()) {
            // caller sets syncProgress=PUSH before proceedPendingPushMap() (which does not set it)
            syncInfo.setSyncProgress(CMFileSyncProgress.PUSH);
            boolean pushStarted = syncManager.proceedPendingPushMap();
            if(!pushStarted) {
                // PULL itself completed successfully; a push-start failure is not a PULL failure.
                // proceedPendingPushMap() already rolled back its own snapshot, so reset the
                // session state to NONE to avoid getting stuck in PUSH.
                System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC(), "
                        + "failed to start push sync; resetting sync session state to NONE.");
                syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
            }
        }

        return true;
    }

    // called at the server
    private boolean processCOMPLETE_PULL_SYNC_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventCompletePullSyncAck fse_cpsa = (CMFileSyncEventCompletePullSyncAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC_ACK() called..");
            System.out.println("fse_cpsa = " + fse_cpsa);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        String initiatorName = fse_cpsa.getInitiatorName();
        UUID initiatorDeviceUuid = fse_cpsa.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        int returnCode = fse_cpsa.getReturnCode();
        int numFilesCompleted = fse_cpsa.getNumFilesCompleted();

        // log the returnCode but proceed with cleanup either way (avoid leaking server-side state)
        if(returnCode == 0) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC_ACK(), "
                    + "client reported pull-sync completion failure. stateKey = " + stateKey
                    + ", numFilesCompleted = " + numFilesCompleted);
        } else if(returnCode == 1) {
            if(CMInfo._CM_DEBUG) {
                System.out.println("client reported pull-sync completion success. stateKey = " + stateKey
                        + ", numFilesCompleted = " + numFilesCompleted);
            }
        } else {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC_ACK(), "
                    + "invalid returnCode: " + returnCode);
        }

        // cleanup pullModifyStateMap[stateKey] (holder lifecycle end)
        // remove from map first to drop external references, then close any remaining channels
        Map<CMFileSyncStateKey, CMFileSyncPullModifyState> pullModifyStateMap = syncInfo.getPullModifyStateMap();
        CMFileSyncPullModifyState pullModifyState = pullModifyStateMap.remove(stateKey);
        if(pullModifyState != null) {
            pullModifyState.cleanupAll();
            if(CMInfo._CM_DEBUG) {
                System.out.println("pullModifyState removed and cleaned up for stateKey = " + stateKey);
            }
        } else {
            // normal when this session had no MODIFY entries (PullModifyState is lazily created)
            if(CMInfo._CM_DEBUG) {
                System.out.println("pullModifyState is null (no MODIFY entries in this session). stateKey = "
                        + stateKey);
            }
        }

        // NOTE: server-side serverEntryListMap[stateKey] cleanup is mentioned in the design doc but
        // not implemented — change-log entries are streamed in startServerEntryList() without being
        // retained per-stateKey on the server.

        // defensive check: pullStateTable[stateKey] should already be removed by completePullSync()
        Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pullStateTable = syncInfo.getPullStateTable();
        if(pullStateTable.containsKey(stateKey)) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PULL_SYNC_ACK(), "
                    + "pullStateTable still has entry for stateKey = " + stateKey + ", removing as defensive cleanup.");
            pullStateTable.remove(stateKey);
        }

        return returnCode == 1;
    }

    // called at the client
    private boolean processEND_LOCAL_MODE_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventEndLocalModeListAck ackEvent = (CMFileSyncEventEndLocalModeListAck) fse;
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processEND_LOCAL_MODE_LIST_ACK() called..");
            System.out.println("ackEvent = " + ackEvent);
        }
        // check return code
        int returnCode = ackEvent.getReturnCode();
        if(returnCode != 1) {
            System.err.println("return code: "+returnCode);
            return false;
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        // print local mode files
        if(CMInfo._CM_DEBUG) {
            System.out.println("--- local mode files ---");
            //List<Path> onlineFiles = syncInfo.getOnlineModePathList();
            List<Path> onlineFiles = syncInfo.getOnlineModePathSizeMap().keySet().stream().toList();
            List<Path> pathList = syncInfo.getPathList();
            for(Path path : pathList) {
                if(!Files.isDirectory(path)) {
                    if(!onlineFiles.contains(path)) {
                        System.out.println(path);
                    }
                }
            }
            System.out.println("---");
        }

/*
        // save the online-mode-path list to file
        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        boolean ret = syncManager.saveOnlineModeListToFile();
        if(!ret) {
            System.err.println("error to save the online-mode-path list to file!");
        }
*/
        // save the online-mode-map to file
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        boolean ret = syncManager.saveOnlineModePathSizeMapToFile();
        if(!ret) {
            System.err.println("error to save the online-mode-map to file!");
        }

        // start the watch service
        ret = syncManager.startWatchService();
        if(!ret) {
            System.err.println("error to start WatchService!");
            return false;
        }

        // set the syncInProgress to false
        syncInfo.setSyncProgress(CMFileSyncProgress.NONE);

        // perform file-sync
        ret = syncManager.startPullSync();
        if(!ret) {
            System.err.println("error to start file-sync!");
            return false;
        }

        return true;
    }

    // called at the server
    private boolean processEND_LOCAL_MODE_LIST(CMFileSyncEvent fse) {
        CMFileSyncEventEndLocalModeList endEvent = (CMFileSyncEventEndLocalModeList) fse;
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processEND_LOCAL_MODE_LIST() called..");
            System.out.println("endEvent = " + endEvent);
        }
        // create an end-local-mode-list ack event
        CMFileSyncEventEndLocalModeListAck ackEvent = new CMFileSyncEventEndLocalModeListAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(endEvent.getInitiatorName());
        ackEvent.setInitiatorUuid(endEvent.getInitiatorUuid());
        ackEvent.setInitiatorDeviceUuid(endEvent.getInitiatorDeviceUuid());
        ackEvent.setNumLocalModeFiles(endEvent.getNumLocalModeFiles());
        ackEvent.setReturnCode(1);

        // send the ack event
        boolean ret = CMEventManager.unicastEvent(ackEvent, endEvent.getSender(), endEvent.getSenderUuid());
        if(!ret) {
            System.err.println("send error: "+ackEvent);
            return false;
        }

        return true;
    }

    // called at the client
    private boolean processLOCAL_MODE_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventLocalModeListAck ackEvent = (CMFileSyncEventLocalModeListAck) fse;
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncEventHandler.processLOCAL_MODE_LIST_ACK() called..");
            System.out.println("ackEvent = " + ackEvent);
        }

        int returnCode = ackEvent.getReturnCode();
        if(returnCode != 1) {
            System.err.println("return code = "+returnCode);
            return false;
        }

        return true;
    }

    // called at the server
    private boolean processLOCAL_MODE_LIST(CMFileSyncEvent fse) {
        CMFileSyncEventLocalModeList listEvent = (CMFileSyncEventLocalModeList)fse;
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processLOCAL_MODE_LIST() called..");
            System.out.println("listEvent = " + listEvent);
        }
        String initiatorName = listEvent.getInitiatorName();
        UUID initiatorUuid = listEvent.getInitiatorUuid();
        UUID initiatorDeviceUuid = listEvent.getInitiatorDeviceUuid();

        // get sync home of initiator
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = Objects.requireNonNull(cmInfo.getServiceManager(CMFileSyncManager.class));
        Path serverSyncHome = Objects.requireNonNull(syncManager.getServerSyncHome(initiatorName));

        // start push-file for all paths in the event
        boolean ret = true;
        for(String relativeStr : listEvent.getRelativePathList()) {
            // reconstruct a platform-appropriate Path from the forward-slash normalized string
            String[] parts = relativeStr.split("/");
            Path relativePath = Paths.get(parts[0], Arrays.copyOfRange(parts, 1, parts.length));
            // get the absolute path
            Path absPath = serverSyncHome.resolve(relativePath);
            // start push-file
            ret &= CMFileTransferManager.pushFile(absPath.toString(), initiatorName, initiatorUuid);
            if(!ret) {
                System.err.println("push error: "+absPath);
                return false;
            }
        }

/*
        // delete the list in the listEvent from the online-mode-list
        ret = onlineModeList.removeAll(listEvent.getRelativePathList());
        if(!ret) {
            System.err.println("remove error of path list from the online-mode-list!");
        }
*/

        // create and send ack event
        CMFileSyncEventLocalModeListAck ackEvent = new CMFileSyncEventLocalModeListAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(initiatorName);
        ackEvent.setInitiatorUuid(initiatorUuid);
        ackEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
        ackEvent.setRelativePathList(listEvent.getRelativePathList());
        if(ret) ackEvent.setReturnCode(1);
        else ackEvent.setReturnCode(0);

        ret = CMEventManager.unicastEvent(ackEvent, initiatorName, initiatorUuid);
        if(!ret) {
            System.err.println("send error: "+ackEvent);
            return false;
        }

        return true;
    }

    // called at the client
    private boolean processEND_ONLINE_MODE_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventEndOnlineModeListAck ackEvent = (CMFileSyncEventEndOnlineModeListAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_ONLINE_MODE_LIST_ACK() called..");
            System.out.println("ackEvent = " + ackEvent);
        }

        // check return code
        if(ackEvent.getReturnCode() != 1) {
            System.err.println("return code = "+ackEvent.getReturnCode());
            return false;
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        // print the online mode files
        if(CMInfo._CM_DEBUG) {
            System.out.println("--- online mode files ---");
            //syncInfo.getOnlineModePathList().stream().forEach(System.out::println);
            syncInfo.getOnlineModePathSizeMap().entrySet().stream().forEach(System.out::println);
            System.out.println("---");
        }

        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
/*
        // save the online-mode-path list to file
        boolean ret = syncManager.saveOnlineModeListToFile();
        if(!ret) {
            System.err.println("error to save the online-mode-path list to file!");
        }
*/
        // save the online-mode-map to file
        boolean ret = syncManager.saveOnlineModePathSizeMapToFile();
        if (!ret) {
            System.err.println("error to save the online-mode-map to file!");
        }

        // set syncInProgress to false
        syncInfo.setSyncProgress(CMFileSyncProgress.NONE);

        // restart watch service
        ret = syncManager.startWatchService();
        if(!ret) {
            System.err.println("error to start WatchService!");
            return false;
        }

        // perform file-sync
        ret = syncManager.startPullSync();
        if(!ret) {
            System.err.println("error to start file-sync!");
            return false;
        }

        return true;
    }

    // called at the server
    private boolean processEND_ONLINE_MODE_LIST(CMFileSyncEvent fse) {
        CMFileSyncEventEndOnlineModeList endEvent = (CMFileSyncEventEndOnlineModeList) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_ONLINE_MODE_LIST() called..");
            System.out.println("endEvent = " + endEvent);
        }

        // create and send ack event
        CMFileSyncEventEndOnlineModeListAck ackEvent = new CMFileSyncEventEndOnlineModeListAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(endEvent.getInitiatorName());
        ackEvent.setInitiatorUuid(endEvent.getInitiatorUuid());
        ackEvent.setInitiatorDeviceUuid(endEvent.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        ackEvent.setNumOnlineModeFiles(endEvent.getNumOnlineModeFiles());
        ackEvent.setReturnCode(1);

        boolean ret = CMEventManager.unicastEvent(ackEvent, endEvent.getSender(), endEvent.getSenderUuid());
        if(!ret) {
            System.err.println("send error: "+ackEvent);
            return false;
        }

        return true;
    }

    // called at the client
    private boolean processONLINE_MODE_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventOnlineModeListAck ackEvent = (CMFileSyncEventOnlineModeListAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processONLINE_MODE_LIST_ACK() called..");
            System.out.println("ackEvent = " + ackEvent);
        }

        // check return code
        int returnCode = ackEvent.getReturnCode();
        if(returnCode == 0) {
            System.err.println("return code is "+returnCode);
            return false;
        }

        // get online-mode-request queue
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        ConcurrentLinkedQueue<Path> requestQueue = Objects.requireNonNull(syncInfo.getOnlineModeRequestQueue());
        // get the list in event
        List<String> eventList = ackEvent.getRelativePathList();
        if(eventList == null || eventList.isEmpty()) {
            System.err.println("The list in event is null or empty!");
            return false;
        }
        // get online mode list
        // List<Path> onlineModeList = Objects.requireNonNull(syncInfo.getOnlineModePathList());
        // get online mode map
        Map<Path,Long> onlineModePathSizeMap = Objects.requireNonNull(syncInfo.getOnlineModePathSizeMap());
        List<Path> onlineModeList = onlineModePathSizeMap.keySet().stream().toList();
        // get info of sync home path
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        Path clientSyncHome = Objects.requireNonNull(syncManager.getClientSyncHome());
        int startPathIndex = clientSyncHome.getNameCount();

        boolean isInList = true;    // if the queue head is in the relative path list
        Path headPath = null;
        Path relativeHeadPath = null;
        boolean addResult = false;
        int numCompletePath = 0;

        while(isInList) {
            // check the queue head
            headPath = requestQueue.peek();
            if(headPath == null) {
                System.out.println("The online mode request queue is empty!");
                break;
            }
            // change the queue head to relative path
            relativeHeadPath = headPath.subpath(startPathIndex, headPath.getNameCount());
            // if the head is in the list of event (normalize to forward slashes for cross-OS comparison)
            String relativeHeadStr = relativeHeadPath.toString().replace('\\', '/');
            if(eventList.contains(relativeHeadStr)) {
                //// change the head path to the online mode
                long size;
                try {
                    // save the last-modified time of headPath
                    FileTime lastModifiedTime = Files.getLastModifiedTime(headPath);
                    // save the last-access time of headPath
                    BasicFileAttributes attrs = Files.readAttributes(headPath, BasicFileAttributes.class);
                    FileTime lastAccessTime = attrs.lastAccessTime();
                    // get the file size
                    size = Files.size(headPath);
                    // truncate the file
                    try(SeekableByteChannel channel = Files.newByteChannel(headPath, StandardOpenOption.WRITE)) {
                        channel.truncate(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                    // restore the last-modified time of headPath
                    Files.setLastModifiedTime(headPath, lastModifiedTime);
                    // restore the last-access time of headPath
                    Files.setAttribute(headPath, "lastAccessTime", lastAccessTime);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                // remove the head of queue and add it to the online mode map
                requestQueue.remove();
/*
                if(!onlineModeList.contains(headPath)) {
                    addResult = onlineModeList.add(headPath);
                    if(!addResult) {
                        System.err.println("error to add queue head to the online mode list!");
                        System.err.println(headPath);
                        return false;
                    }
                }
*/
                onlineModePathSizeMap.put(headPath, size);
                numCompletePath++;
            }
            else {
                isInList = false;
            }
        }

        // check if the number of paths that are moved to the online mode list is
        // the same as the list size of event
        if(numCompletePath != eventList.size()) {
            System.err.println("numCompletePath and list size of event are different!");
            System.err.println("numCompletePath = " + numCompletePath);
            System.err.println("list size of event = " + eventList.size());
            return false;
        }

        // check if the online-mode-request queue is empty
        if(requestQueue.isEmpty()) {
            // create and send the end-online-mode event
            CMFileSyncEventEndOnlineModeList endEvent = new CMFileSyncEventEndOnlineModeList();
            // 공통 필드 설정
            endEvent.setInitiatorName(ackEvent.getInitiatorName());
            endEvent.setInitiatorUuid(ackEvent.getInitiatorUuid());
            endEvent.setInitiatorDeviceUuid(ackEvent.getInitiatorDeviceUuid());
            // 나머지 필드 설정
            endEvent.setNumOnlineModeFiles(onlineModePathSizeMap.size());

            boolean ret = CMEventManager.unicastEvent(endEvent, ackEvent.getSender(), ackEvent.getSenderUuid());
            if(!ret) {
                System.err.println("send error: "+endEvent);
                return false;
            }
        }

        return true;
    }

    // called at the server
    private boolean processONLINE_MODE_LIST(CMFileSyncEvent fse) {
        CMFileSyncEventOnlineModeList listEvent = (CMFileSyncEventOnlineModeList) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processONLINE_MODE_LIST() called..");
            System.out.println("listEvent = " + listEvent);
        }
        // create and send an ack event
        CMFileSyncEventOnlineModeListAck ackEvent = new CMFileSyncEventOnlineModeListAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(listEvent.getInitiatorName());
        ackEvent.setInitiatorUuid(listEvent.getInitiatorUuid());
        ackEvent.setInitiatorDeviceUuid(listEvent.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        ackEvent.setRelativePathList(listEvent.getRelativePathList());
        ackEvent.setReturnCode(1);

        boolean ret = CMEventManager.unicastEvent(ackEvent, listEvent.getSender(), listEvent.getSenderUuid());
        if(!ret) {
            System.err.println("send error : "+ackEvent);
            return false;
        }

        return true;
    }

    // called at the server (full push sync) or the client (pull sync) depending on system type
    private boolean processEND_FILE_BLOCK_CHECKSUM_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventEndFileBlockChecksumAck ackEvent = (CMFileSyncEventEndFileBlockChecksumAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_ACK() called..");
            System.out.println("ackEvent = " + ackEvent);
        }

        // returnCode 가드 (양쪽 공통 선처리)
        final int returnCode = ackEvent.getReturnCode();
        if(returnCode != 1) {
            System.err.println("return code error: " + returnCode);
            return false;
        }

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if(confInfo.getSystemType().equals("SERVER")) {
            return processEND_FILE_BLOCK_CHECKSUM_ACK_AtServer(ackEvent);
        } else if(confInfo.getSystemType().equals("CLIENT")) {
            return processEND_FILE_BLOCK_CHECKSUM_ACK_AtClient(ackEvent);
        } else {
            System.err.println("CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_ACK(), "
                    + "unknown system type: " + confInfo.getSystemType());
            return false;
        }
    }

    // called at the server (full push sync): finalize the basis file using CMFileSyncGenerator state.
    private boolean processEND_FILE_BLOCK_CHECKSUM_ACK_AtServer(CMFileSyncEventEndFileBlockChecksumAck ackEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_ACK_AtServer() called..");

        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        // get the sync generator reference
        // initiator 정보 변수 선언
        String initiatorName = ackEvent.getInitiatorName();
        UUID initiatorUuid = ackEvent.getInitiatorUuid();
        UUID initiatorDeviceUuid = ackEvent.getInitiatorDeviceUuid();
        // get generator
        CMUserLoginKey loginKey = new CMUserLoginKey(initiatorName, initiatorUuid);
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        Objects.requireNonNull(syncGenerator);

        // get the target basis file channel
        int fileEntryIndex = ackEvent.getFileEntryIndex();
        Map<Integer, SeekableByteChannel> basisFileChannelMap = syncGenerator.getBasisFileChannelForReadMap();
        Objects.requireNonNull(basisFileChannelMap);
        SeekableByteChannel basisFileChannel = basisFileChannelMap.get(fileEntryIndex);
        if(basisFileChannel == null) {
            System.out.println("The basis file channel is null for the file entry index ("+fileEntryIndex+").");
        }
        // get the temp file channel
        Map<Integer, SeekableByteChannel> tempFileChannelMap = syncGenerator.getTempFileChannelForWriteMap();
        Objects.requireNonNull(tempFileChannelMap);
        SeekableByteChannel tempFileChannel = tempFileChannelMap.get(fileEntryIndex);
        if(tempFileChannel == null) {
            System.out.println("The temp file channel is null for the file entry index ("+fileEntryIndex+").");
        }

        // close the channels and remove them from the Maps
        if(basisFileChannel != null && basisFileChannel.isOpen()) {
            try {
                basisFileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            basisFileChannelMap.remove(fileEntryIndex);
        }
        if(tempFileChannel != null && tempFileChannel.isOpen()) {
            try {
                tempFileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            tempFileChannelMap.remove(fileEntryIndex);
        }

        // get the corresponding basis file path
        int basisFileIndex = Optional.ofNullable(syncGenerator.getBasisFileIndexMap().get(fileEntryIndex))
                .orElse(-1);
        if(basisFileIndex == -1) {
            System.err.println("Basis file index NOT FOUND for file entry index ("+fileEntryIndex+")!");
            return false;
        }

        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        List<Path> basisFileList = syncInfo.getBasisFileListMap().get(stateKey);
        Path basisFilePath = basisFileList.get(basisFileIndex);
        if(basisFilePath == null) {
            System.err.println("Basis file path NOT FOUND for basis file index ("+basisFileIndex+")!");
            return false;
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("basisFilePath = " + basisFilePath);
        }

        // get the temp file path
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        Path tempFilePath = syncManager.getTempPathOfBasisFile(basisFilePath);
        if(CMInfo._CM_DEBUG) {
            System.out.println("tempFilePath = " + tempFilePath);
        }

        // get the initiator file entry reference
        CMFileSyncEntry initiatorFileEntry = Optional.of(syncInfo)
                .map(CMFileSyncInfo::getInitiatorPathEntryListMap)
                .map(map -> map.get(stateKey))
                .map(list -> list.get(fileEntryIndex))
                .orElse(null);
        if(initiatorFileEntry == null) {
            System.err.println("initiator file entry is null! : initiatorName("+initiatorName+"), file entry index("
                    +fileEntryIndex+")");
            return false;
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("initiatorFileEntry = " + initiatorFileEntry);
        }

        if(basisFileChannel != null && tempFileChannel != null) {
            // compare file checksum of the temp file and the checksum of the ack event
            byte[] fileChecksum;
            try {
                fileChecksum = CMUtil.md5(tempFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if(!Arrays.equals(fileChecksum, ackEvent.getFileChecksum())) {
                System.err.println("File checksum error!");
                System.err.println("temp checksum = " + DatatypeConverter.printHexBinary(fileChecksum));
                System.err.println("event checksum = " + DatatypeConverter.printHexBinary(ackEvent.getFileChecksum()));
                return false;
            }

            // set the last modified time to that of the client file entry
            try {
                Files.setLastModifiedTime(tempFilePath, initiatorFileEntry.getLastModifiedTime());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            // move the temp file to the original sync home with the basis file name
            try {
                Files.move(tempFilePath, basisFilePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        else {
            // client file size = 0 (online mode)
            // client have never sent a UPDATE_EXISTING_FILE event
            try {
                Files.setLastModifiedTime(basisFilePath, initiatorFileEntry.getLastModifiedTime());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // complete the update-existing-file task
        boolean result = syncManager.completeUpdateFile(loginKey, basisFilePath);
        if(result) {
            // check if the file-sync is complete or not
            if( syncManager.isCompleteFileSync(loginKey) ) {
                // complete the file-sync task
                syncManager.completeFileSync(loginKey);
            }
        }

        return true;
    }

    // called at the client (pull sync): finalize the basis file using CMFileSyncPullGenerator state,
    // send COMPLETE_PULL_MODIFY to the server, and clean up the pull generator after the last entry.
    private boolean processEND_FILE_BLOCK_CHECKSUM_ACK_AtClient(CMFileSyncEventEndFileBlockChecksumAck ackEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_ACK_AtClient() called..");

        int fileEntryIndex = ackEvent.getFileEntryIndex();

        // get the pull generator
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncPullGenerator pullGenerator = syncInfo.getPullGenerator();
        if(pullGenerator == null) {
            System.err.println("CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_ACK_AtClient(), "
                    + "pullGenerator is null.");
            return false;
        }

        // close + remove the basis/temp channels for this fileEntryIndex
        Map<Integer, SeekableByteChannel> basisFileChannelMap = pullGenerator.getBasisFileChannelForReadMap();
        SeekableByteChannel basisFileChannel = basisFileChannelMap.get(fileEntryIndex);
        if(basisFileChannel == null) {
            System.out.println("The basis file channel is null for fileEntryIndex (" + fileEntryIndex + ").");
        }
        Map<Integer, SeekableByteChannel> tempFileChannelMap = pullGenerator.getTempFileChannelForWriteMap();
        SeekableByteChannel tempFileChannel = tempFileChannelMap.get(fileEntryIndex);
        if(tempFileChannel == null) {
            System.out.println("The temp file channel is null for fileEntryIndex (" + fileEntryIndex + ").");
        }
        if(basisFileChannel != null && basisFileChannel.isOpen()) {
            try { basisFileChannel.close(); } catch (IOException e) { e.printStackTrace(); }
            basisFileChannelMap.remove(fileEntryIndex);
        }
        if(tempFileChannel != null && tempFileChannel.isOpen()) {
            try { tempFileChannel.close(); } catch (IOException e) { e.printStackTrace(); }
            tempFileChannelMap.remove(fileEntryIndex);
        }

        // resolve basis path + lookup the entry (for serverMtime)
        List<CMFileSyncClientEntry> pullModifyEntryList = pullGenerator.getPullModifyEntryList();
        if(fileEntryIndex < 0 || fileEntryIndex >= pullModifyEntryList.size()) {
            System.err.println("CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_ACK_AtClient(), "
                    + "invalid fileEntryIndex: " + fileEntryIndex);
            return false;
        }
        CMFileSyncClientEntry entry = pullModifyEntryList.get(fileEntryIndex);
        String relativePath = entry.getPath();
        long serverMtime = entry.getServerMtime();
        if(serverMtime <= 0) {
            System.err.println("CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_ACK_AtClient(), "
                    + "invalid serverMtime for relativePath = " + relativePath + ", serverMtime = " + serverMtime);
            return false;
        }
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        Path clientSyncHome = syncManager.getClientSyncHome();
        Path basisFilePath = clientSyncHome.resolve(relativePath).normalize();
        Path tempFilePath = syncManager.getTempPathOfBasisFile(basisFilePath);
        if(CMInfo._CM_DEBUG) {
            System.out.println("basisFilePath = " + basisFilePath);
            System.out.println("tempFilePath = " + tempFilePath);
            System.out.println("serverMtime = " + serverMtime);
        }

        // verify MD5 → preserve server source mtime on temp → temp→basis move
        if(basisFileChannel != null && tempFileChannel != null) {
            byte[] fileChecksum;
            try {
                fileChecksum = CMUtil.md5(tempFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if(!Arrays.equals(fileChecksum, ackEvent.getFileChecksum())) {
                System.err.println("File checksum error!");
                System.err.println("temp checksum = " + DatatypeConverter.printHexBinary(fileChecksum));
                System.err.println("event checksum = " + DatatypeConverter.printHexBinary(ackEvent.getFileChecksum()));
                return false;
            }
            // preserve server source mtime on the temp file (mirror of full-push SERVER branch)
            try {
                Files.setLastModifiedTime(tempFilePath, FileTime.fromMillis(serverMtime * 1000L));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            try {
                Files.move(tempFilePath, basisFilePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            System.err.println("basis or temp channel is null in PULL --- unexpected for MODIFY entry: " + relativePath);
            return false;
        }

        // update lastSyncedMtimeMap with the preserved serverMtime so "file mtime == baseMtime" holds.
        // size 도 함께 저장: WatchService 의 self-event 필터가 mtime+size 양쪽을 비교한다.
        long curSize;
        try {
            curSize = syncInfo.currentSizeOrMinusOne(basisFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            curSize = -1L;
        }
        syncInfo.setLastSynced(relativePath, serverMtime, curSize);

        // send COMPLETE_PULL_MODIFY to the server
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        CMUser myself = interInfo.getMyself();
        String serverName = interInfo.getDefaultServerInfo().getServerName();
        CMFileSyncEventCompletePullModify fse_cpm = new CMFileSyncEventCompletePullModify();
        fse_cpm.setInitiatorName(myself.getName());
        fse_cpm.setInitiatorUuid(myself.getUuid());
        fse_cpm.setInitiatorDeviceUuid(syncInfo.getDeviceUuid());
        fse_cpm.setModifiedPath(relativePath);
        boolean sendResult = CMEventManager.unicastEvent(fse_cpm, serverName, null);
        if(!sendResult) {
            System.err.println("failed to send COMPLETE_PULL_MODIFY for " + relativePath);
            return false;
        }

        // mark this entry completed
        pullGenerator.getIsUpdateFileCompletedMap().put(relativePath, true);
        pullGenerator.setNumUpdateFilesCompleted(pullGenerator.getNumUpdateFilesCompleted() + 1);
        if(CMInfo._CM_DEBUG) {
            System.out.println("client-side completed entry: relativePath = " + relativePath
                    + ", numUpdateFilesCompleted = " + pullGenerator.getNumUpdateFilesCompleted());
        }

        // when every MODIFY entry is done, release the generator
        if(pullGenerator.getNumUpdateFilesCompleted() == pullModifyEntryList.size()) {
            if(CMInfo._CM_DEBUG) {
                System.out.println("all pull MODIFY entries completed. cleanup pullGenerator.");
            }
            pullGenerator.cleanupAll();
            syncInfo.setPullGenerator(null);
        }

        return true;
    }

    // called at the server (full push sync) or the client (pull sync) depending on system type
    private boolean processUPDATE_EXISTING_FILE(CMFileSyncEvent fse) {
        CMFileSyncEventUpdateExistingFile updateEvent = (CMFileSyncEventUpdateExistingFile) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processUPDATE_EXISTING_FILE() called..");
            System.out.println("updateEvent = " + updateEvent);
        }

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if(confInfo.getSystemType().equals("SERVER")) {
            return processUPDATE_EXISTING_FILE_AtServer(updateEvent);
        } else if(confInfo.getSystemType().equals("CLIENT")) {
            return processUPDATE_EXISTING_FILE_AtClient(updateEvent);
        } else {
            System.err.println("CMFileSyncEventHandler.processUPDATE_EXISTING_FILE(), "
                    + "unknown system type: " + confInfo.getSystemType());
            return false;
        }
    }

    // called at the server (full push sync): reconstruct the temp basis file using CMFileSyncGenerator state.
    private boolean processUPDATE_EXISTING_FILE_AtServer(CMFileSyncEventUpdateExistingFile updateEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processUPDATE_EXISTING_FILE_AtServer() called..");

        String initiatorName = updateEvent.getInitiatorName();
        UUID initiatorUuid = updateEvent.getInitiatorUuid();
        UUID initiatorDeviceUuid = updateEvent.getInitiatorDeviceUuid();

        // get the sync manager
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = Objects.requireNonNull(cmInfo.getServiceManager(CMFileSyncManager.class));

        // get the basis file path
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMUserLoginKey loginKey = new CMUserLoginKey(initiatorName, initiatorUuid);
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        Objects.requireNonNull(syncGenerator);
        int fileEntryIndex = updateEvent.getFileEntryIndex();
        int basisFileIndex = syncGenerator.getBasisFileIndexMap().get(fileEntryIndex);
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        List<Path> basisFileList = syncInfo.getBasisFileListMap().get(stateKey);
        Path basisFilePath = Objects.requireNonNull(basisFileList.get(basisFileIndex));
        if(CMInfo._CM_DEBUG) {
            System.out.println("basisFilePath = " + basisFilePath);
        }
        // create a temp basis file path
        Path tempBasisFilePath = syncManager.getTempPathOfBasisFile(basisFilePath);

        // get or create a file channel for write a temp file
        Map<Integer, SeekableByteChannel> writeChannelMap = syncGenerator.getTempFileChannelForWriteMap();
        Objects.requireNonNull(writeChannelMap);
        SeekableByteChannel writeChannel = writeChannelMap.get(fileEntryIndex);
        if(writeChannel == null) {
            // create a new channel and put to the channel map
            try {
                writeChannel = Files.newByteChannel(tempBasisFilePath, StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            writeChannelMap.put(fileEntryIndex, writeChannel);
        }

        // check non-matching bytes in the update event
        byte[] nonMatchBytes = updateEvent.getNonMatchBytes();
        if(nonMatchBytes != null) {
            ByteBuffer nonMatchBuffer = ByteBuffer.wrap(nonMatchBytes);
            try {
                int bytesWritten = writeChannel.write(nonMatchBuffer);
                if(bytesWritten != nonMatchBytes.length) {
                    System.err.println("bytes written to the channel = "+bytesWritten);
                    System.err.println("non-match bytes = "+nonMatchBytes.length);
                    return false;
                }
                if(CMInfo._CM_DEBUG) {
                    System.out.println("-------------------------- write non-matching bytes");
                    System.out.println("bytesWritten = " + bytesWritten);
                    System.out.println("writeChannel position = " + writeChannel.position());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // get the basis file channel map
        Map<Integer, SeekableByteChannel> readChannelMap = syncGenerator.getBasisFileChannelForReadMap();
        Objects.requireNonNull(readChannelMap);
        // search or open a target file channel
        SeekableByteChannel readChannel = readChannelMap.get(fileEntryIndex);
        if(readChannel == null) {
            try {
                readChannel = Files.newByteChannel(basisFilePath, StandardOpenOption.READ);
                readChannelMap.put(fileEntryIndex, readChannel);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // check matching block index in the update event
        int matchBlockIndex = updateEvent.getMatchBlockIndex();
        if(matchBlockIndex > -1) {

            // get the block size of this file (server: keyed by basisFileIndex)
            Map<Integer, Integer> blockSizeMap = Objects.requireNonNull(syncGenerator.getBlockSizeOfBasisFileMap());
            int blockSize = blockSizeMap.get(basisFileIndex);
            // prepare ByteBuffer
            ByteBuffer matchBuffer = ByteBuffer.allocate(blockSize);
            // read file channel to the ByteBuffer by the block size
            try {
                int readBytes = readChannel.position(blockSize * matchBlockIndex).read(matchBuffer);
                // write the ByteBuffer to the temp file channel
                matchBuffer.flip();
                int writeBytes = writeChannel.write(matchBuffer);
                if(CMInfo._CM_DEBUG) {
                    System.out.println("-------------------------- write a matching block");
                    System.out.println("blockSize = " + blockSize);
                    System.out.println("matchBlockIndex = " + matchBlockIndex);
                    System.out.println("readChannel position = " + blockSize*matchBlockIndex);
                    System.out.println("readBytes = " + readBytes);
                    System.out.println("--------------------------");
                    System.out.println("writeBytes = " + writeBytes);
                    System.out.println("writeChannel position = " + writeChannel.position());
                }

            } catch (IOException e) {
                e.printStackTrace();
                // close the channels and remove them from the map
                try {
                    readChannel.close();
                    writeChannel.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                readChannelMap.remove(fileEntryIndex);
                writeChannelMap.remove(fileEntryIndex);
                return false;
            }
        }

        // The file channels (readChannel and writeChannel) will be closed when END_FILE_BLOCK_CHECKSUM_ACK
        // event is received.
        return true;
    }

    // called at the client (pull sync): reconstruct the temp basis file using CMFileSyncPullGenerator state.
    private boolean processUPDATE_EXISTING_FILE_AtClient(CMFileSyncEventUpdateExistingFile updateEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processUPDATE_EXISTING_FILE_AtClient() called..");

        int fileEntryIndex = updateEvent.getFileEntryIndex();
        byte[] nonMatchBytes = updateEvent.getNonMatchBytes();
        int matchBlockIndex = updateEvent.getMatchBlockIndex();

        // get the pull generator (single instance on the client)
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncPullGenerator pullGenerator = syncInfo.getPullGenerator();
        if(pullGenerator == null) {
            System.err.println("CMFileSyncEventHandler.processUPDATE_EXISTING_FILE_AtClient(), "
                    + "pullGenerator is null.");
            return false;
        }

        // resolve the basis file path (client sync home + relativePath)
        List<CMFileSyncClientEntry> pullModifyEntryList = pullGenerator.getPullModifyEntryList();
        if(fileEntryIndex < 0 || fileEntryIndex >= pullModifyEntryList.size()) {
            System.err.println("CMFileSyncEventHandler.processUPDATE_EXISTING_FILE_AtClient(), "
                    + "invalid fileEntryIndex: " + fileEntryIndex);
            return false;
        }
        String relativePath = pullModifyEntryList.get(fileEntryIndex).getPath();
        CMFileSyncManager syncManager =
                Objects.requireNonNull(cmInfo.getServiceManager(CMFileSyncManager.class));
        Path clientSyncHome = syncManager.getClientSyncHome();
        Path basisFilePath = clientSyncHome.resolve(relativePath).normalize();
        if(CMInfo._CM_DEBUG) {
            System.out.println("basisFilePath = " + basisFilePath);
        }
        Path tempBasisFilePath = syncManager.getTempPathOfBasisFile(basisFilePath);

        // get or create a temp file write channel
        Map<Integer, SeekableByteChannel> writeChannelMap = pullGenerator.getTempFileChannelForWriteMap();
        Objects.requireNonNull(writeChannelMap);
        SeekableByteChannel writeChannel = writeChannelMap.get(fileEntryIndex);
        if(writeChannel == null) {
            try {
                writeChannel = Files.newByteChannel(tempBasisFilePath, StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            writeChannelMap.put(fileEntryIndex, writeChannel);
        }

        // write non-matching bytes
        if(nonMatchBytes != null) {
            ByteBuffer nonMatchBuffer = ByteBuffer.wrap(nonMatchBytes);
            try {
                int bytesWritten = writeChannel.write(nonMatchBuffer);
                if(bytesWritten != nonMatchBytes.length) {
                    System.err.println("bytes written to the channel = " + bytesWritten);
                    System.err.println("non-match bytes = " + nonMatchBytes.length);
                    return false;
                }
                if(CMInfo._CM_DEBUG) {
                    System.out.println("-------------------------- write non-matching bytes");
                    System.out.println("bytesWritten = " + bytesWritten);
                    System.out.println("writeChannel position = " + writeChannel.position());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // get or open the basis file read channel
        Map<Integer, SeekableByteChannel> readChannelMap = pullGenerator.getBasisFileChannelForReadMap();
        Objects.requireNonNull(readChannelMap);
        SeekableByteChannel readChannel = readChannelMap.get(fileEntryIndex);
        if(readChannel == null) {
            try {
                readChannel = Files.newByteChannel(basisFilePath, StandardOpenOption.READ);
                readChannelMap.put(fileEntryIndex, readChannel);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // process matching block (PULL: blockSize keyed by fileEntryIndex since basisFileIndex == fileEntryIndex)
        if(matchBlockIndex > -1) {
            Map<Integer, Integer> blockSizeMap = Objects.requireNonNull(pullGenerator.getBlockSizeOfBasisFileMap());
            Integer blockSizeBoxed = blockSizeMap.get(fileEntryIndex);
            if(blockSizeBoxed == null) {
                System.err.println("CMFileSyncEventHandler.processUPDATE_EXISTING_FILE_AtClient(), "
                        + "blockSize is null for fileEntryIndex = " + fileEntryIndex);
                return false;
            }
            int blockSize = blockSizeBoxed;
            ByteBuffer matchBuffer = ByteBuffer.allocate(blockSize);
            try {
                int readBytes = readChannel.position((long) blockSize * matchBlockIndex).read(matchBuffer);
                matchBuffer.flip();
                int writeBytes = writeChannel.write(matchBuffer);
                if(CMInfo._CM_DEBUG) {
                    System.out.println("-------------------------- write a matching block");
                    System.out.println("blockSize = " + blockSize);
                    System.out.println("matchBlockIndex = " + matchBlockIndex);
                    System.out.println("readChannel position = " + (long) blockSize * matchBlockIndex);
                    System.out.println("readBytes = " + readBytes);
                    System.out.println("--------------------------");
                    System.out.println("writeBytes = " + writeBytes);
                    System.out.println("writeChannel position = " + writeChannel.position());
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    readChannel.close();
                    writeChannel.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                readChannelMap.remove(fileEntryIndex);
                writeChannelMap.remove(fileEntryIndex);
                return false;
            }
        }

        // Channels are closed in processEND_FILE_BLOCK_CHECKSUM_ACK_AtClient.
        return true;
    }

    // called at the client (full push sync) or the server (pull sync) depending on system type
    private boolean processEND_FILE_BLOCK_CHECKSUM(CMFileSyncEvent fse) {
        CMFileSyncEventEndFileBlockChecksum endChecksumEvent = (CMFileSyncEventEndFileBlockChecksum) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM() called..");
            System.out.println("endChecksumEvent = " + endChecksumEvent);
        }

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if(confInfo.getSystemType().equals("SERVER")) {
            return processEND_FILE_BLOCK_CHECKSUM_AtServer(endChecksumEvent);
        } else if(confInfo.getSystemType().equals("CLIENT")) {
            return processEND_FILE_BLOCK_CHECKSUM_AtClient(endChecksumEvent);
        } else {
            System.err.println("CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM(), "
                    + "unknown system type: " + confInfo.getSystemType());
            return false;
        }
    }

    // called at the client (full push sync): compare blocks against the basis file and send the ACK to the server
    private boolean processEND_FILE_BLOCK_CHECKSUM_AtClient(CMFileSyncEventEndFileBlockChecksum endChecksumEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_AtClient() called..");

        int fileEntryIndex = endChecksumEvent.getFileEntryIndex();
        int blockSize = endChecksumEvent.getBlockSize();
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        // get the local path list
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        List<Path> pathList = syncInfo.getPathList();
        // get the target file path
        Path path = Objects.requireNonNull(pathList.get(fileEntryIndex));

        boolean ret = compareFileBlocks(endChecksumEvent);
        if(!ret) {
            System.err.println("error to compare file blocks!");
            return false;
        }

        // create and send an END_FILE_BLOCK_CHECKSUM_ACK event
        CMFileSyncEventEndFileBlockChecksumAck ackEvent = new CMFileSyncEventEndFileBlockChecksumAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(endChecksumEvent.getInitiatorName());
        ackEvent.setInitiatorUuid(endChecksumEvent.getInitiatorUuid());
        ackEvent.setInitiatorDeviceUuid(endChecksumEvent.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        ackEvent.setFileEntryIndex(fileEntryIndex);
        ackEvent.setTotalNumBlocks(endChecksumEvent.getTotalNumBlocks());
        ackEvent.setBlockSize(blockSize);
        ackEvent.setReturnCode(1);

        // calculate a file checksum and set it to the ack event
        byte[] fileChecksum;
        try {
            fileChecksum = CMUtil.md5(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        ackEvent.setFileChecksum(fileChecksum);
        // send the ack event
        ret = CMEventManager.unicastEvent(ackEvent, endChecksumEvent.getSender(), endChecksumEvent.getSenderUuid());
        if(!ret) return false;

        return true;
    }

    // called at the server (pull sync): compute delta from PullModifyState and send the ACK to the client
    private boolean processEND_FILE_BLOCK_CHECKSUM_AtServer(CMFileSyncEventEndFileBlockChecksum endChecksumEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_AtServer() called..");

        int fileEntryIndex = endChecksumEvent.getFileEntryIndex();
        int blockSize = endChecksumEvent.getBlockSize();
        String initiatorName = endChecksumEvent.getInitiatorName();
        UUID initiatorDeviceUuid = endChecksumEvent.getInitiatorDeviceUuid();

        // get the PullModifyState by stateKey
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        CMFileSyncPullModifyState pullModifyState = syncInfo.getPullModifyStateMap().get(stateKey);
        if(pullModifyState == null) {
            System.err.println("CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_AtServer(), "
                    + "pullModifyState is null for stateKey = " + stateKey);
            return false;
        }

        // resolve the source file path (server sync home + relativePath)
        String relativePath = pullModifyState.getFileEntryIndexToRelativePathMap().get(fileEntryIndex);
        if(relativePath == null) {
            System.err.println("CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_AtServer(), "
                    + "relativePath is null for fileEntryIndex = " + fileEntryIndex);
            return false;
        }
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager =
                Objects.requireNonNull(cmInfo.getServiceManager(CMFileSyncManager.class));
        Path serverSyncHome = syncManager.getServerSyncHome(initiatorName);
        Path sourcePath = serverSyncHome.resolve(relativePath).normalize();
        if(!Files.exists(sourcePath)) {
            System.err.println("CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_AtServer(), "
                    + "source file does not exist: " + sourcePath);
            return false;
        }

        // open the source file read channel and register it in PullModifyState
        // (compareFileBlocksAtServer opens its own try-with-resources channel; this one is a cleanup safety net)
        try {
            SeekableByteChannel sourceChannel = Files.newByteChannel(sourcePath, StandardOpenOption.READ);
            pullModifyState.getSourceFileChannelForReadMap().put(fileEntryIndex, sourceChannel);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        boolean ret = compareFileBlocksAtServer(endChecksumEvent, pullModifyState, sourcePath);
        if(!ret) {
            System.err.println("error to compare file blocks at server!");
            pullModifyState.cleanupForFileEntry(fileEntryIndex);
            return false;
        }

        // create and send an END_FILE_BLOCK_CHECKSUM_ACK event
        CMFileSyncEventEndFileBlockChecksumAck ackEvent = new CMFileSyncEventEndFileBlockChecksumAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(endChecksumEvent.getInitiatorName());
        ackEvent.setInitiatorUuid(endChecksumEvent.getInitiatorUuid());
        ackEvent.setInitiatorDeviceUuid(endChecksumEvent.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        ackEvent.setFileEntryIndex(fileEntryIndex);
        ackEvent.setTotalNumBlocks(endChecksumEvent.getTotalNumBlocks());
        ackEvent.setBlockSize(blockSize);
        ackEvent.setReturnCode(1);

        // calculate the source file MD5 and set it to the ack event
        byte[] fileChecksum;
        try {
            fileChecksum = CMUtil.md5(sourcePath);
        } catch (IOException e) {
            e.printStackTrace();
            pullModifyState.cleanupForFileEntry(fileEntryIndex);
            return false;
        }
        ackEvent.setFileChecksum(fileChecksum);

        // send the ack event to the client
        ret = CMEventManager.unicastEvent(ackEvent, endChecksumEvent.getSender(), endChecksumEvent.getSenderUuid());
        if(!ret) {
            pullModifyState.cleanupForFileEntry(fileEntryIndex);
            return false;
        }

        // cleanup per-fileEntry structures and mark this entry completed
        pullModifyState.cleanupForFileEntry(fileEntryIndex);
        pullModifyState.getIsUpdateFileCompletedMap().put(relativePath, true);
        pullModifyState.incrementNumUpdateFilesCompleted();

        if(CMInfo._CM_DEBUG) {
            System.out.println("server-side completed entry: relativePath = " + relativePath
                    + ", numUpdateFilesCompleted = " + pullModifyState.getNumUpdateFilesCompleted());
        }

        return true;
    }

    // called at the client
    private boolean compareFileBlocks(CMFileSyncEventEndFileBlockChecksum endChecksumEvent) {
        int fileEntryIndex = endChecksumEvent.getFileEntryIndex();
        int blockSize = endChecksumEvent.getBlockSize();
        // create hash-to-blockIndex Map
        Map<Short, Integer> hashToBlockIndexMap =
                makeHashToBlockIndexMap(fileEntryIndex);
        Objects.requireNonNull(hashToBlockIndexMap);
        // get block checksum Map
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<Integer, CMFileSyncBlockChecksum[]> checksumMap = syncInfo.getBlockChecksumMap();
        Objects.requireNonNull(checksumMap);
        // get block checksum array with the file entry index
        CMFileSyncBlockChecksum[] checksumArray = checksumMap.get(fileEntryIndex);
        Objects.requireNonNull(checksumArray);

        // get the local path list
        List<Path> pathList = syncInfo.getPathList();
        // get the target file path
        Path path = Objects.requireNonNull(pathList.get(fileEntryIndex));

        // get the file sync manager
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        // create a ByteBuffer to read a block from the file channel
        // ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        // create a ByteBuffer to read a block from the memory-mapped file (MappedByteBuffer)
        MappedByteBuffer mappedBuffer = null;
        ByteBuffer buffer = null;
        // create a ByteBuffer to store non-matching bytes
        ByteBuffer nonMatchBuffer = ByteBuffer.allocate(CMInfo.FILE_BLOCK_LEN);
        // initialize other local variables before the while loop
        boolean bBlockMatch = true;
        int oldA = 0;
        int oldB = 0;
        byte oldStartByte = 0;
        byte newEndByte = 0;
        int[] weakChecksumABS = new int[3]; // weak checksum array [3]
        short hash = 0;     // 16-bit hash
        int sortedBlockIndex = -1;
        int matchBlockIndex = -1;
        String receiver = endChecksumEvent.getSender();
        UUID receiverUuid = endChecksumEvent.getSenderUuid();
        String initiatorName = endChecksumEvent.getInitiatorName();
        UUID initiatorUuid = endChecksumEvent.getInitiatorUuid();
        UUID initiatorDeviceUuid = endChecksumEvent.getInitiatorDeviceUuid();

        // read (next) block, calculate (update) weak checksum, search a matching block
        //try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
        try (FileChannel channel = (FileChannel) Files.newByteChannel(path, StandardOpenOption.READ)) {

            long mapStartPosition = 0;
            long fileSize = channel.size();
            int mapCount = 0;
            while(mapStartPosition < fileSize) {

                if(CMInfo._CM_DEBUG) {
                    System.out.println("==========================================");
                    System.out.println("mapCount = " + mapCount);
                    System.out.println("fileSize = " + fileSize);
                    System.out.println("mapStartPosition = " + mapStartPosition);
                }

                mapCount++;

                if(fileSize - mapStartPosition > Integer.MAX_VALUE) {
                    mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, mapStartPosition, Integer.MAX_VALUE);
                    mapStartPosition += Integer.MAX_VALUE;
                }
                else {
                    mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, mapStartPosition,
                            fileSize - mapStartPosition);
                    mapStartPosition += fileSize - mapStartPosition;
                }

                bBlockMatch = true;

                if(mappedBuffer == null) {
                    System.err.println("MappedByteBuffer is null!");
                    return false;
                }

                if(CMInfo._CM_DEBUG) {
                    //System.out.println("channel position = "+channel.position()+", channel size = "+channel.size());
                    System.out.println("mappedBuffer: position = "+mappedBuffer.position()+", limit = "+
                            mappedBuffer.limit()+", capacity = "+mappedBuffer.capacity());
                }
                // for comparing rate of matching and non-matching cases
                long matchingCount = 0;
                long nonMatchingCount = 0;
                //while (channel.position() < channel.size()) {
                while(bBlockMatch && mappedBuffer.hasRemaining() || !bBlockMatch && mappedBuffer.remaining() >= blockSize) {

                    if(CMInfo._CM_DEBUG_2)
                        System.out.println("===============================");

                    if(bBlockMatch) {   // initial bBlockMatch is true
                        // read a new block to the buffer and calculate weak checksum
/*
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                    weakChecksumABS = syncManager.calculateWeakChecksumElements(buffer);
                    buffer.rewind();    // rewound the buffer position after the checksum calculation
*/
                        if(mappedBuffer.remaining() < blockSize) {
                            buffer = mappedBuffer.slice(mappedBuffer.position(), mappedBuffer.remaining());
                        }
                        else {
                            buffer = mappedBuffer.slice(mappedBuffer.position(), blockSize);
                        }
                        weakChecksumABS = syncManager.calculateWeakChecksumElements(buffer);
                        buffer.rewind();
                    }
                    else {
                        // read a new 1 byte to the buffer and update the weak checksum
/*
                    buffer.clear();
                    oldStartByte = buffer.get();    // read (remove) the (first) head byte of the buffer
                    buffer.compact();               // move the buffer by 1 byte to the left
                    channel.read(buffer);   // read a new 1byte from the channel and add it to the end of the buffer
                    buffer.clear(); // not sure
                    newEndByte = buffer.get(buffer.limit()-1);  // read the new end byte from the buffer
                    oldA = weakChecksumABS[0];
                    oldB = weakChecksumABS[1];
                    weakChecksumABS = syncManager.updateWeakChecksum(oldA, oldB, oldStartByte, newEndByte, blockSize);
*/
                        buffer.clear();
                        oldStartByte = buffer.get();
                        buffer = mappedBuffer.slice(mappedBuffer.position(), blockSize);
                        newEndByte = buffer.get(buffer.limit()-1);
                        oldA = weakChecksumABS[0];
                        oldB = weakChecksumABS[1];
                        weakChecksumABS = syncManager.updateWeakChecksum(oldA, oldB, oldStartByte, newEndByte, blockSize);
                    }

                    if(CMInfo._CM_DEBUG_2) {
                        System.out.println("-------rolling weak checksum = "+weakChecksumABS[2]);
                    }

                    // calculate 16-bit hash of the block weak checksum
                    hash = calculateHash(weakChecksumABS[2]);

                    // search a matching block
                    sortedBlockIndex = Optional.ofNullable(hashToBlockIndexMap.get(hash)).orElse(-1);
                    if(sortedBlockIndex >= 0) {
                        matchBlockIndex = searchMatchBlockIndex(sortedBlockIndex, weakChecksumABS[2], checksumArray,
                                hash, buffer);
                    }
                    else {
                        matchBlockIndex = -1;
                    }

                    if(CMInfo._CM_DEBUG_2) {
                        System.out.println("-------- hash = "+hash);
                        System.out.println("-------- sortedBlockIndex = "+sortedBlockIndex);
                        System.out.println("-------- matchBlockIndex = "+matchBlockIndex);
                    }

                    // if a matching block is found
                    if(matchBlockIndex >= 0) {
                        matchingCount++;
                        bBlockMatch = true;
                        // create and send an UPDATE_EXISTING_FILE event to the server
                        boolean ret = sendUpdateExistingFileEvent(receiver, receiverUuid, initiatorName,
                                initiatorUuid, initiatorDeviceUuid, fileEntryIndex,
                                nonMatchBuffer, matchBlockIndex);
                        if(!ret) return false;
                        // initialize buffer and non-matching block buffer
                        nonMatchBuffer.clear();

                        // update the position of mappedBuffer
                        if(mappedBuffer.remaining() < blockSize)
                            mappedBuffer.position(mappedBuffer.position()+mappedBuffer.remaining());
                        else
                            mappedBuffer.position(mappedBuffer.position()+blockSize);
                    }
                    else {
                        nonMatchingCount++;
                        bBlockMatch = false;
                        // write the head byte of the block buffer to the non-match buffer
                        nonMatchBuffer.put(buffer.get(0));
                        // if the non-match buffer is full,
                        if(!nonMatchBuffer.hasRemaining()) {
                            // create and send an UPDATE_EXISTING_FILE event to the server
                            boolean ret = sendUpdateExistingFileEvent(receiver, receiverUuid, initiatorName,
                                    initiatorUuid, initiatorDeviceUuid, fileEntryIndex,
                                    nonMatchBuffer, -1);
                            if(!ret) return false;
                            // initialize non-matching block buffer
                            nonMatchBuffer.clear();
                        }

                        // update the position of mappedBuffer
                        mappedBuffer.position(mappedBuffer.position()+1);
                    }

                    if(CMInfo._CM_DEBUG_2) {
                        System.out.println("===============================");
                    }
                }

                if(CMInfo._CM_DEBUG) {
                    System.out.println("------------------- end of while loop");
                }
                // check if the last non-matching bytes that remains in buffer and nonMatchBuffer, and send to the server.
                // At the last iteration of the above while loop,
                // non-matching bytes may remain in buffer and nonMatchBuffer if bBlockMatch is false.
                if(!bBlockMatch) {
                    buffer.position(1); // the first element has already been put to the nonMatchBuffer
                    nonMatchingCount += buffer.remaining(); // update the number of the last non-matching bytes
                    if(CMInfo._CM_DEBUG) {
                        System.out.println("================ send the last bytes in buffer and nonMatchBuffer");
                        System.out.println("buffer.remaining(): "+buffer.remaining());
                        System.out.println("nonMatchBuffer.remaining(): "+nonMatchBuffer.remaining());
                    }
                    while( buffer.hasRemaining() ) {
                        if(buffer.remaining() > nonMatchBuffer.remaining()) {
                            int oldLimit = buffer.limit();
                            buffer.limit(buffer.position() + nonMatchBuffer.remaining());
                            nonMatchBuffer.put(buffer);
                            boolean ret = sendUpdateExistingFileEvent(receiver, receiverUuid, initiatorName,
                                    initiatorUuid, initiatorDeviceUuid, fileEntryIndex,
                                    nonMatchBuffer, -1);
                            if(!ret) return false;
                            nonMatchBuffer.clear();
                            buffer.limit(oldLimit);
                        }
                        else {
                            nonMatchBuffer.put(buffer);
                            boolean ret = sendUpdateExistingFileEvent(receiver, receiverUuid, initiatorName,
                                    initiatorUuid, initiatorDeviceUuid, fileEntryIndex,
                                    nonMatchBuffer, -1);
                            if(!ret) return false;
                            nonMatchBuffer.clear();
                        }
                    }
                }

                if(CMInfo._CM_DEBUG) {
                    System.out.println("--------------- info after the last transmission of UPDATE_EXISTING_FILE event");
                    //System.out.println("channel position = "+channel.position()+", channel size = "+channel.size());
                    System.out.println("mappedBuffer: position = "+mappedBuffer.position()+", limit = "+
                            mappedBuffer.limit()+", capacity = "+mappedBuffer.capacity());
                    System.out.println("path = "+path);
                    System.out.println("matchingCount = " + matchingCount + ", total number of blocks = " +
                            endChecksumEvent.getTotalNumBlocks());
                    System.out.println("non-matching bytes = " + nonMatchingCount + ", total size = " +
                            Files.size(path) + " bytes");
                    System.out.printf("matching block rate = %5.3f\n",
                            matchingCount/(double)(endChecksumEvent.getTotalNumBlocks()));
                    System.out.printf("non-matching bytes rate = %5.3f\n",
                            nonMatchingCount/(double)(Files.size(path)));
                    System.out.println("-----------------");
                }

            }

        } catch(IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if(mappedBuffer != null)
                syncManager.closeDirectBuffer(mappedBuffer);
        }

        return true;
    }

    // called at the server (pull sync): mirrors compareFileBlocks() but pulls its checksum array and
    // hash map from the PullModifyState and reads the source file at the server-resolved sourcePath.
    private boolean compareFileBlocksAtServer(CMFileSyncEventEndFileBlockChecksum endChecksumEvent,
                                              CMFileSyncPullModifyState pullModifyState, Path sourcePath) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.compareFileBlocksAtServer() called..");

        int fileEntryIndex = endChecksumEvent.getFileEntryIndex();
        int blockSize = endChecksumEvent.getBlockSize();

        // create hash-to-blockIndex Map (server-side variant uses PullModifyState)
        Map<Short, Integer> hashToBlockIndexMap = makeHashToBlockIndexMapAtServer(pullModifyState, fileEntryIndex);
        Objects.requireNonNull(hashToBlockIndexMap);
        // get block checksum array with the file entry index
        CMFileSyncBlockChecksum[] checksumArray = pullModifyState.getBlockChecksumArrayMap().get(fileEntryIndex);
        Objects.requireNonNull(checksumArray);

        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);

        MappedByteBuffer mappedBuffer = null;
        ByteBuffer buffer = null;
        ByteBuffer nonMatchBuffer = ByteBuffer.allocate(CMInfo.FILE_BLOCK_LEN);
        boolean bBlockMatch = true;
        int oldA = 0;
        int oldB = 0;
        byte oldStartByte = 0;
        byte newEndByte = 0;
        int[] weakChecksumABS = new int[3];
        short hash = 0;
        int sortedBlockIndex = -1;
        int matchBlockIndex = -1;
        String receiver = endChecksumEvent.getSender();
        UUID receiverUuid = endChecksumEvent.getSenderUuid();
        String initiatorName = endChecksumEvent.getInitiatorName();
        UUID initiatorUuid = endChecksumEvent.getInitiatorUuid();
        UUID initiatorDeviceUuid = endChecksumEvent.getInitiatorDeviceUuid();

        try (FileChannel channel = (FileChannel) Files.newByteChannel(sourcePath, StandardOpenOption.READ)) {

            long mapStartPosition = 0;
            long fileSize = channel.size();
            int mapCount = 0;
            while(mapStartPosition < fileSize) {

                if(CMInfo._CM_DEBUG) {
                    System.out.println("==========================================");
                    System.out.println("mapCount = " + mapCount);
                    System.out.println("fileSize = " + fileSize);
                    System.out.println("mapStartPosition = " + mapStartPosition);
                }

                mapCount++;

                if(fileSize - mapStartPosition > Integer.MAX_VALUE) {
                    mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, mapStartPosition, Integer.MAX_VALUE);
                    mapStartPosition += Integer.MAX_VALUE;
                }
                else {
                    mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, mapStartPosition,
                            fileSize - mapStartPosition);
                    mapStartPosition += fileSize - mapStartPosition;
                }

                bBlockMatch = true;

                if(mappedBuffer == null) {
                    System.err.println("MappedByteBuffer is null!");
                    return false;
                }

                if(CMInfo._CM_DEBUG) {
                    System.out.println("mappedBuffer: position = "+mappedBuffer.position()+", limit = "+
                            mappedBuffer.limit()+", capacity = "+mappedBuffer.capacity());
                }
                long matchingCount = 0;
                long nonMatchingCount = 0;
                while(bBlockMatch && mappedBuffer.hasRemaining() || !bBlockMatch && mappedBuffer.remaining() >= blockSize) {

                    if(CMInfo._CM_DEBUG_2)
                        System.out.println("===============================");

                    if(bBlockMatch) {
                        if(mappedBuffer.remaining() < blockSize) {
                            buffer = mappedBuffer.slice(mappedBuffer.position(), mappedBuffer.remaining());
                        }
                        else {
                            buffer = mappedBuffer.slice(mappedBuffer.position(), blockSize);
                        }
                        weakChecksumABS = syncManager.calculateWeakChecksumElements(buffer);
                        buffer.rewind();
                    }
                    else {
                        buffer.clear();
                        oldStartByte = buffer.get();
                        buffer = mappedBuffer.slice(mappedBuffer.position(), blockSize);
                        newEndByte = buffer.get(buffer.limit()-1);
                        oldA = weakChecksumABS[0];
                        oldB = weakChecksumABS[1];
                        weakChecksumABS = syncManager.updateWeakChecksum(oldA, oldB, oldStartByte, newEndByte, blockSize);
                    }

                    if(CMInfo._CM_DEBUG_2) {
                        System.out.println("-------rolling weak checksum = "+weakChecksumABS[2]);
                    }

                    hash = calculateHash(weakChecksumABS[2]);

                    sortedBlockIndex = Optional.ofNullable(hashToBlockIndexMap.get(hash)).orElse(-1);
                    if(sortedBlockIndex >= 0) {
                        matchBlockIndex = searchMatchBlockIndex(sortedBlockIndex, weakChecksumABS[2], checksumArray,
                                hash, buffer);
                    }
                    else {
                        matchBlockIndex = -1;
                    }

                    if(CMInfo._CM_DEBUG_2) {
                        System.out.println("-------- hash = "+hash);
                        System.out.println("-------- sortedBlockIndex = "+sortedBlockIndex);
                        System.out.println("-------- matchBlockIndex = "+matchBlockIndex);
                    }

                    if(matchBlockIndex >= 0) {
                        matchingCount++;
                        bBlockMatch = true;
                        boolean ret = sendUpdateExistingFileEvent(receiver, receiverUuid, initiatorName,
                                initiatorUuid, initiatorDeviceUuid, fileEntryIndex,
                                nonMatchBuffer, matchBlockIndex);
                        if(!ret) return false;
                        nonMatchBuffer.clear();

                        if(mappedBuffer.remaining() < blockSize)
                            mappedBuffer.position(mappedBuffer.position()+mappedBuffer.remaining());
                        else
                            mappedBuffer.position(mappedBuffer.position()+blockSize);
                    }
                    else {
                        nonMatchingCount++;
                        bBlockMatch = false;
                        nonMatchBuffer.put(buffer.get(0));
                        if(!nonMatchBuffer.hasRemaining()) {
                            boolean ret = sendUpdateExistingFileEvent(receiver, receiverUuid, initiatorName,
                                    initiatorUuid, initiatorDeviceUuid, fileEntryIndex,
                                    nonMatchBuffer, -1);
                            if(!ret) return false;
                            nonMatchBuffer.clear();
                        }

                        mappedBuffer.position(mappedBuffer.position()+1);
                    }

                    if(CMInfo._CM_DEBUG_2) {
                        System.out.println("===============================");
                    }
                }

                if(CMInfo._CM_DEBUG) {
                    System.out.println("------------------- end of while loop");
                }
                // flush any non-matching bytes that remain in buffer/nonMatchBuffer at the end
                if(!bBlockMatch) {
                    buffer.position(1);
                    nonMatchingCount += buffer.remaining();
                    if(CMInfo._CM_DEBUG) {
                        System.out.println("================ send the last bytes in buffer and nonMatchBuffer");
                        System.out.println("buffer.remaining(): "+buffer.remaining());
                        System.out.println("nonMatchBuffer.remaining(): "+nonMatchBuffer.remaining());
                    }
                    while( buffer.hasRemaining() ) {
                        if(buffer.remaining() > nonMatchBuffer.remaining()) {
                            int oldLimit = buffer.limit();
                            buffer.limit(buffer.position() + nonMatchBuffer.remaining());
                            nonMatchBuffer.put(buffer);
                            boolean ret = sendUpdateExistingFileEvent(receiver, receiverUuid, initiatorName,
                                    initiatorUuid, initiatorDeviceUuid, fileEntryIndex,
                                    nonMatchBuffer, -1);
                            if(!ret) return false;
                            nonMatchBuffer.clear();
                            buffer.limit(oldLimit);
                        }
                        else {
                            nonMatchBuffer.put(buffer);
                            boolean ret = sendUpdateExistingFileEvent(receiver, receiverUuid, initiatorName,
                                    initiatorUuid, initiatorDeviceUuid, fileEntryIndex,
                                    nonMatchBuffer, -1);
                            if(!ret) return false;
                            nonMatchBuffer.clear();
                        }
                    }
                }

                if(CMInfo._CM_DEBUG) {
                    System.out.println("--------------- info after the last transmission of UPDATE_EXISTING_FILE event");
                    System.out.println("mappedBuffer: position = "+mappedBuffer.position()+", limit = "+
                            mappedBuffer.limit()+", capacity = "+mappedBuffer.capacity());
                    System.out.println("path = "+sourcePath);
                    System.out.println("matchingCount = " + matchingCount + ", total number of blocks = " +
                            endChecksumEvent.getTotalNumBlocks());
                    System.out.println("non-matching bytes = " + nonMatchingCount + ", total size = " +
                            Files.size(sourcePath) + " bytes");
                    System.out.printf("matching block rate = %5.3f\n",
                            matchingCount/(double)(endChecksumEvent.getTotalNumBlocks()));
                    System.out.printf("non-matching bytes rate = %5.3f\n",
                            nonMatchingCount/(double)(Files.size(sourcePath)));
                    System.out.println("-----------------");
                }

            }

        } catch(IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if(mappedBuffer != null)
                syncManager.closeDirectBuffer(mappedBuffer);
        }

        return true;
    }

    // called at the client or server
    private boolean sendUpdateExistingFileEvent(String receiver, UUID receiverUuid,
                                                String initiatorName, UUID initiatorUuid,
                                                UUID initiatorDeviceUuid, int fileEntryIndex,
                                                ByteBuffer nonMatchBuffer, int matchBlockIndex) {
        ////// create and send an UPDATE_EXISTING_FILE event to the sync receiver
        CMFileSyncEventUpdateExistingFile updateEvent = new CMFileSyncEventUpdateExistingFile();
        // 공통 필드 설정
        updateEvent.setInitiatorName(initiatorName);
        updateEvent.setInitiatorUuid(initiatorUuid);
        updateEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // 나머지 필드 설정
        updateEvent.setFileEntryIndex(fileEntryIndex);
        // set non-match buffer
        nonMatchBuffer.flip();
        updateEvent.setNumNonMatchBytes(nonMatchBuffer.remaining());
        byte[] nonMatchBytes = new byte[nonMatchBuffer.remaining()];
        nonMatchBuffer.get(nonMatchBytes);
        updateEvent.setNonMatchBytes(nonMatchBytes);
        // set matching block index
        updateEvent.setMatchBlockIndex(matchBlockIndex);
        // send the event
        boolean ret = CMEventManager.unicastEvent(updateEvent, receiver, receiverUuid);
        if(!ret) {
            System.err.println("send error, updateEvent = "+updateEvent);
            return false;
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("sent updateEvent = " + updateEvent);
        }
        return true;
    }

    // called at the client or server
    private int searchMatchBlockIndex(int sortedBlockIndex, int weakChecksum,
                                      CMFileSyncBlockChecksum[] checksumArray, short hash, ByteBuffer buffer) {
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("=== CMFileSyncEventHandler.searchMatchBlockIndex() called..");
            System.out.println("sortedBlockIndex = " + sortedBlockIndex);
            System.out.println("weakChecksum = " + weakChecksum);
        }

        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        byte[] strongChecksum;
        if(weakChecksum == checksumArray[sortedBlockIndex].getWeakChecksum()) {
            // check the strong checksum
            strongChecksum = syncManager.calculateStrongChecksum(buffer);
            if(Arrays.equals(strongChecksum, checksumArray[sortedBlockIndex].getStrongChecksum())) {
                int matchBlockIndex = checksumArray[sortedBlockIndex].getBlockIndex();
                if(CMInfo._CM_DEBUG_2)
                    System.out.println("matchBlockIndex = " + matchBlockIndex);
                return matchBlockIndex;
            }
        }
        else if(sortedBlockIndex+1 < checksumArray.length){
            // look at the next sorted block while the 16-bit hash is the same
            sortedBlockIndex++;
            int nextWeakChecksum = checksumArray[sortedBlockIndex].getWeakChecksum();
            while(hash == calculateHash(nextWeakChecksum)) {
                if(weakChecksum == nextWeakChecksum) {
                    // check the strong checksum
                    strongChecksum = syncManager.calculateStrongChecksum(buffer);
                    if(Arrays.equals(strongChecksum, checksumArray[sortedBlockIndex].getStrongChecksum())) {
                        int matchBlockIndex = checksumArray[sortedBlockIndex].getBlockIndex();
                        if(CMInfo._CM_DEBUG_2)
                            System.out.println("matchBlockIndex = " + matchBlockIndex);
                        return matchBlockIndex;
                    }
                }
                sortedBlockIndex++;
                nextWeakChecksum = checksumArray[sortedBlockIndex].getWeakChecksum();
            }
        }

        // not found a matching block
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("not found matching block for sortedBlockIndex("+sortedBlockIndex+")");
        }
        return -1;
    }

    // called at the client
    private Map<Short, Integer> makeHashToBlockIndexMap(int fileEntryIndex) {

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.makeHashToBlockIndexMap() called..");
            System.out.println("fileEntryIndex = " + fileEntryIndex);
        }

        // sort the block checksum array of the corresponding file
        // get the block checksum array
        Map<Integer, CMFileSyncBlockChecksum[]> blockChecksumMap = CMFileSyncInfo.getInstance()
                .getBlockChecksumMap();
        Objects.requireNonNull(blockChecksumMap);
        CMFileSyncBlockChecksum[] checksumArray = blockChecksumMap.get( fileEntryIndex );
        Objects.requireNonNull(checksumArray);
        // sort the checksum array by the weak checksum
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("checksumArray before sorting = " + Arrays.toString(checksumArray));
        }
        Arrays.sort(checksumArray, Comparator.comparingInt(CMFileSyncBlockChecksum::getWeakChecksum));
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("checksumArray after sorting = " + Arrays.toString(checksumArray));
        }

        // get the outer (fileIndex-to-hash-to-blockIndex) Map
        Map<Integer, Map<Short, Integer>> outerMap = CMFileSyncInfo.getInstance()
                .getFileIndexToHashToBlockIndexMap();
        Objects.requireNonNull(outerMap);
        // create an inner (hash-to-blockIndex) Map and set to the outer Map
        Map<Short, Integer> hashToBlockIndexMap = new Hashtable<>();
        outerMap.put(fileEntryIndex, hashToBlockIndexMap);

        // repeat the following task for each checksum array element
        for(int i = 0; i < checksumArray.length; i++) {
            CMFileSyncBlockChecksum blockChecksum = checksumArray[i];
            // calculate a 16-bit hash of the weak checksum
            int weakChecksum = blockChecksum.getWeakChecksum();
            short hash = calculateHash(weakChecksum);
            // set the pair (hash, block index) to the Map only if the hash key does not already exist in the Map.
            // 16bit hash indicates the first element of the block checksum array.
            // Other block checksum element that has the same 16-bit hash can be found by the linear search
            // from the first element because the checksum array is sorted by the (weak) checksum value.
            if(hashToBlockIndexMap.containsKey(hash)) continue;
            hashToBlockIndexMap.put(hash, i);
            if(CMInfo._CM_DEBUG_2) {
                System.out.println("key hash("+hash+"), value block index("+i+") added to the Map.");
            }
        }

        return hashToBlockIndexMap;
    }

    // called at the server (pull sync): mirrors makeHashToBlockIndexMap() but reads/writes via PullModifyState.
    private Map<Short, Integer> makeHashToBlockIndexMapAtServer(CMFileSyncPullModifyState pullModifyState,
                                                                int fileEntryIndex) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.makeHashToBlockIndexMapAtServer() called..");
            System.out.println("fileEntryIndex = " + fileEntryIndex);
        }

        // sort the block checksum array by the weak checksum
        CMFileSyncBlockChecksum[] checksumArray = pullModifyState.getBlockChecksumArrayMap().get(fileEntryIndex);
        Objects.requireNonNull(checksumArray);
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("checksumArray before sorting = " + Arrays.toString(checksumArray));
        }
        Arrays.sort(checksumArray, Comparator.comparingInt(CMFileSyncBlockChecksum::getWeakChecksum));
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("checksumArray after sorting = " + Arrays.toString(checksumArray));
        }

        // reuse the inner Map reserved at processSTART_FILE_BLOCK_CHECKSUM_AtServer if present; otherwise create one
        Map<Integer, Map<Short, Integer>> outerMap = pullModifyState.getFileIndexToHashToBlockIndexMap();
        Objects.requireNonNull(outerMap);
        Map<Short, Integer> hashToBlockIndexMap = outerMap.get(fileEntryIndex);
        if(hashToBlockIndexMap == null) {
            hashToBlockIndexMap = new Hashtable<>();
            outerMap.put(fileEntryIndex, hashToBlockIndexMap);
        }

        for(int i = 0; i < checksumArray.length; i++) {
            CMFileSyncBlockChecksum blockChecksum = checksumArray[i];
            int weakChecksum = blockChecksum.getWeakChecksum();
            short hash = calculateHash(weakChecksum);
            if(hashToBlockIndexMap.containsKey(hash)) continue;
            hashToBlockIndexMap.put(hash, i);
            if(CMInfo._CM_DEBUG_2) {
                System.out.println("key hash("+hash+"), value block index("+i+") added to the Map.");
            }
        }

        return hashToBlockIndexMap;
    }

    // called at the client or server
    private short calculateHash(int weakChecksum) {

        if(CMInfo._CM_DEBUG_2) {
            System.out.println("=== CMFileSyncEventHandler.calculateHash() called..");
            System.out.println("weakChecksum = " + weakChecksum);
            System.out.println("weakChecksum binary = "+Integer.toBinaryString(weakChecksum));
        }

        short hash = 0;
        for(int i = 0; i < 2; i++) {
            int checksum = weakChecksum >> (16*i);
            hash += (short)checksum;

            if(CMInfo._CM_DEBUG_2) {
                System.out.println("["+i+"] checksum = "+Integer.toBinaryString(checksum));
                System.out.println("hash = "+Integer.toBinaryString(hash));
            }
        }

        return hash;
    }

    // called at the client (full push sync) or the server (pull sync) depending on system type
    private boolean processFILE_BLOCK_CHECKSUM(CMFileSyncEvent fse) {
        CMFileSyncEventFileBlockChecksum fileBlockChecksumEvent = (CMFileSyncEventFileBlockChecksum) fse;
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processFILE_BLOCK_CHECKSUM() called..");
            System.out.println("fileBlockChecksumEvent = " + fileBlockChecksumEvent);
        }

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if(confInfo.getSystemType().equals("SERVER")) {
            return processFILE_BLOCK_CHECKSUM_AtServer(fileBlockChecksumEvent);
        } else if(confInfo.getSystemType().equals("CLIENT")) {
            return processFILE_BLOCK_CHECKSUM_AtClient(fileBlockChecksumEvent);
        } else {
            System.err.println("CMFileSyncEventHandler.processFILE_BLOCK_CHECKSUM(), "
                    + "unknown system type: " + confInfo.getSystemType());
            return false;
        }
    }

    // called at the client. Full-sync(initial push) + incremental PUSH MODIFY 양쪽 모두 진입.
    // 10-2 doc 15348~15428 F-3: syncProgress == PUSH 분기 우선 처리 후, 아니면 기존 full-sync 로직.
    private boolean processFILE_BLOCK_CHECKSUM_AtClient(CMFileSyncEventFileBlockChecksum fileBlockChecksumEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processFILE_BLOCK_CHECKSUM_AtClient() called..");

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        // [F-3 PUSH 분기]
        // 진입 조건: syncProgress == PUSH + 이벤트의 initiator 3종이 본 클라와 일치.
        // 책임: 수신한 sub-array를 pushModifyState.blockChecksumArrayMap[fileEntryIndex]의
        //      startBlockIndex 위치에 System.arraycopy로 누적. ACK 없음 (full-sync도 동일).
        if (syncInfo.getSyncProgress() == CMFileSyncProgress.PUSH) {
            CMUser myself = CMInteractionInfo.getInstance().getMyself();
            String initiatorName = fileBlockChecksumEvent.getInitiatorName();
            UUID initiatorUuid = fileBlockChecksumEvent.getInitiatorUuid();
            UUID initiatorDeviceUuid = fileBlockChecksumEvent.getInitiatorDeviceUuid();
            if (!myself.getName().equals(initiatorName)
                    || !myself.getUuid().equals(initiatorUuid)
                    || !syncInfo.getDeviceUuid().equals(initiatorDeviceUuid)) {
                System.err.println("processFILE_BLOCK_CHECKSUM PUSH branch: initiator mismatch: "
                        + "name=" + initiatorName + ", uuid=" + initiatorUuid
                        + ", deviceUuid=" + initiatorDeviceUuid);
                return false;
            }

            int fileEntryIndex = fileBlockChecksumEvent.getFileEntryIndex();
            int startBlockIndex = fileBlockChecksumEvent.getStartBlockIndex();
            int numCurrentBlocks = fileBlockChecksumEvent.getNumCurrentBlocks();
            CMFileSyncBlockChecksum[] partialChecksumArray = fileBlockChecksumEvent.getChecksumArray();

            CMFileSyncPushModifyState pushState = syncInfo.getPushModifyState();
            if (pushState == null) {
                System.err.println("processFILE_BLOCK_CHECKSUM PUSH branch: pushModifyState is null. ignoring.");
                return false;
            }
            CMFileSyncBlockChecksum[] arr = pushState.getBlockChecksumArrayMap().get(fileEntryIndex);
            if (arr == null) {
                System.err.println("processFILE_BLOCK_CHECKSUM PUSH branch: "
                        + "array slot missing for fileEntryIndex=" + fileEntryIndex);
                return false;
            }

            // PullModifyState 동명 자료구조 2510~2522와 정확히 대칭, full-sync 1300~1302와 동일 패턴
            System.arraycopy(partialChecksumArray, 0, arr, startBlockIndex, numCurrentBlocks);
            if (CMInfo._CM_DEBUG) {
                System.out.println("FILE_BLOCK_CHECKSUM accumulated: fileEntryIndex=" + fileEntryIndex
                        + ", startBlockIndex=" + startBlockIndex
                        + ", count=" + numCurrentBlocks);
            }
            return true;
        }

        // [기존 full-sync 분기]
        // get checksum array with the file entry index as a key
        Map<Integer, CMFileSyncBlockChecksum[]> checksumMap = syncInfo.getBlockChecksumMap();
        Objects.requireNonNull(checksumMap);
        CMFileSyncBlockChecksum[] checksumArray = checksumMap.get(fileBlockChecksumEvent.getFileEntryIndex());
        Objects.requireNonNull(checksumArray);

        // add sub array of the event to the checksum array
        CMFileSyncBlockChecksum[] subArray = fileBlockChecksumEvent.getChecksumArray();
        int startIndex = fileBlockChecksumEvent.getStartBlockIndex();
        for(int i = 0; i < subArray.length; i++) {
            checksumArray[startIndex+i] = subArray[i];
        }

        if(CMInfo._CM_DEBUG_2) {
            System.out.println("checksumArray is ");
            for(int i = 0; i < checksumArray.length; i++)
                System.out.println("["+i+"] = "+checksumArray[i]);
        }

        return true;
    }

    // called at the server (pull sync): fills the source file block checksum array in CMFileSyncPullModifyState
    private boolean processFILE_BLOCK_CHECKSUM_AtServer(CMFileSyncEventFileBlockChecksum fileBlockChecksumEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processFILE_BLOCK_CHECKSUM_AtServer() called..");

        String initiatorName = fileBlockChecksumEvent.getInitiatorName();
        UUID initiatorDeviceUuid = fileBlockChecksumEvent.getInitiatorDeviceUuid();
        int fileEntryIndex = fileBlockChecksumEvent.getFileEntryIndex();
        int startBlockIndex = fileBlockChecksumEvent.getStartBlockIndex();
        int numCurrentBlocks = fileBlockChecksumEvent.getNumCurrentBlocks();
        CMFileSyncBlockChecksum[] partialChecksumArray = fileBlockChecksumEvent.getChecksumArray();

        // get the PullModifyState by stateKey
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        CMFileSyncPullModifyState pullModifyState = syncInfo.getPullModifyStateMap().get(stateKey);
        if(pullModifyState == null) {
            System.err.println("CMFileSyncEventHandler.processFILE_BLOCK_CHECKSUM_AtServer(), "
                    + "pullModifyState is null for stateKey = " + stateKey);
            return false;
        }

        // get the full checksum array for fileEntryIndex
        CMFileSyncBlockChecksum[] checksumArray = pullModifyState.getBlockChecksumArrayMap().get(fileEntryIndex);
        if(checksumArray == null) {
            System.err.println("CMFileSyncEventHandler.processFILE_BLOCK_CHECKSUM_AtServer(), "
                    + "checksumArray is null for fileEntryIndex = " + fileEntryIndex);
            return false;
        }

        // copy the partial array into the full array at startBlockIndex
        System.arraycopy(partialChecksumArray, 0, checksumArray, startBlockIndex, numCurrentBlocks);

        return true;
    }

    // called at the server (full push sync) or the client (pull sync) depending on system type
    private boolean processSTART_FILE_BLOCK_CHECKSUM_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventStartFileBlockChecksumAck startAckEvent = (CMFileSyncEventStartFileBlockChecksumAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_ACK() called..");
            System.out.println("startAckEvent = " + startAckEvent);
        }

        // returnCode 가드 (양쪽 공통 선처리)
        if(startAckEvent.getReturnCode() == 0) {
            System.err.println("CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_ACK(), "
                    + "returnCode == 0, abort.");
            return false;
        }

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if(confInfo.getSystemType().equals("SERVER")) {
            return processSTART_FILE_BLOCK_CHECKSUM_ACK_AtServer(startAckEvent);
        } else if(confInfo.getSystemType().equals("CLIENT")) {
            return processSTART_FILE_BLOCK_CHECKSUM_ACK_AtClient(startAckEvent);
        } else {
            System.err.println("CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_ACK(), "
                    + "unknown system type: " + confInfo.getSystemType());
            return false;
        }
    }

    // called at the server. Full-sync(initial push) + incremental PUSH MODIFY 양쪽 모두 진입.
    // 10-2 doc 15891~16126 F-2: pushGeneratorMap.get(loginKey) != null 분기 우선 처리 후,
    // 아니면 기존 full-sync 분기로 fall-through.
    private boolean processSTART_FILE_BLOCK_CHECKSUM_ACK_AtServer(CMFileSyncEventStartFileBlockChecksumAck startAckEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_ACK_AtServer() called..");

        String initiatorName = startAckEvent.getInitiatorName();
        UUID initiatorUuid = startAckEvent.getInitiatorUuid();
        UUID initiatorDeviceUuid = startAckEvent.getInitiatorDeviceUuid();
        int fileEntryIndex = startAckEvent.getFileEntryIndex();
        CMUserLoginKey loginKey = new CMUserLoginKey(initiatorName, initiatorUuid);
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        // [F-2 PUSH 분기] pushGeneratorMap.get(loginKey) != null 이면 PUSH 컨텍스트
        CMFileSyncPushGenerator pushGen = syncInfo.getPushGeneratorMap().get(loginKey);
        if (pushGen != null) {
            // (i) returnCode 검증 — full-sync에는 없는 PUSH 전용 방어 (doc 15930~15939).
            //     dispatcher가 returnCode==0은 이미 abort했지만 !=1까지 엄격 체크.
            int returnCode = startAckEvent.getReturnCode();
            if (returnCode != 1) {
                System.err.println("processSTART_FILE_BLOCK_CHECKSUM_ACK PUSH branch: "
                        + "client reported START_ACK failure for fileEntryIndex=" + fileEntryIndex);
                return false;
            }

            CMFileSyncBlockChecksum[] pushChecksumArray =
                    pushGen.getBlockChecksumArrayMap().get(fileEntryIndex);
            if (pushChecksumArray == null) {
                System.err.println("processSTART_FILE_BLOCK_CHECKSUM_ACK PUSH branch: "
                        + "blockChecksumArrayMap missing for fileEntryIndex=" + fileEntryIndex);
                return false;
            }
            // server-owned source-of-truth — event echo가 아니라 generator state에서 직접 획득 (doc 15953~15960)
            int pushBlockSize = pushGen.getBlockSizeOfBasisFileMap().get(fileEntryIndex);
            int pushTotalNumBlocks = pushChecksumArray.length;

            // (ii) FILE_BLOCK_CHECKSUM batch 송신 — full-sync 분할 정책과 동일
            int pushCurIndex = 0;
            while (pushCurIndex < pushChecksumArray.length) {
                CMFileSyncEventFileBlockChecksum fse_fbc = new CMFileSyncEventFileBlockChecksum();
                fse_fbc.setInitiatorName(initiatorName);
                fse_fbc.setInitiatorUuid(initiatorUuid);
                fse_fbc.setInitiatorDeviceUuid(initiatorDeviceUuid);
                fse_fbc.setFileEntryIndex(fileEntryIndex);
                fse_fbc.setTotalNumBlocks(pushTotalNumBlocks);
                fse_fbc.setStartBlockIndex(pushCurIndex);

                int remainingEventBytes = CMInfo.MAX_EVENT_SIZE - fse_fbc.getByteNum();
                int checksumBytes = Integer.BYTES * 3 + CMInfo.STRONG_CHECKSUM_LEN;
                int numCurrentBlocks = remainingEventBytes / checksumBytes;
                if (pushCurIndex + numCurrentBlocks > pushChecksumArray.length) {
                    numCurrentBlocks = pushChecksumArray.length - pushCurIndex;
                }
                fse_fbc.setNumCurrentBlocks(numCurrentBlocks);
                CMFileSyncBlockChecksum[] partialChecksumArray =
                        Arrays.copyOfRange(pushChecksumArray, pushCurIndex, pushCurIndex + numCurrentBlocks);
                fse_fbc.setChecksumArray(partialChecksumArray);

                if (!CMEventManager.unicastEvent(fse_fbc, initiatorName, initiatorUuid)) {
                    System.err.println("processSTART_FILE_BLOCK_CHECKSUM_ACK PUSH branch: "
                            + "send FILE_BLOCK_CHECKSUM error.");
                    return false;
                }
                if (CMInfo._CM_DEBUG) {
                    System.out.println("sent FILE_BLOCK_CHECKSUM: fileEntryIndex=" + fileEntryIndex
                            + ", startBlockIndex=" + pushCurIndex
                            + ", numCurrentBlocks=" + numCurrentBlocks);
                }
                pushCurIndex += numCurrentBlocks;
            }

            // (iii) END_FILE_BLOCK_CHECKSUM 송신
            CMFileSyncEventEndFileBlockChecksum fse_efbc = new CMFileSyncEventEndFileBlockChecksum();
            fse_efbc.setInitiatorName(initiatorName);
            fse_efbc.setInitiatorUuid(initiatorUuid);
            fse_efbc.setInitiatorDeviceUuid(initiatorDeviceUuid);
            fse_efbc.setFileEntryIndex(fileEntryIndex);
            fse_efbc.setTotalNumBlocks(pushTotalNumBlocks);
            fse_efbc.setBlockSize(pushBlockSize);
            if (!CMEventManager.unicastEvent(fse_efbc, initiatorName, initiatorUuid)) {
                System.err.println("processSTART_FILE_BLOCK_CHECKSUM_ACK PUSH branch: "
                        + "send END_FILE_BLOCK_CHECKSUM error.");
                return false;
            }
            return true;
        }

        // [기존 full-sync 분기]
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        Objects.requireNonNull(syncGenerator);

        // get the block checksum array of the file
        CMFileSyncBlockChecksum[] checksumArray = syncGenerator.getBlockChecksumArrayMap()
                .get(fileEntryIndex);
        Objects.requireNonNull(checksumArray);

        // repeat to create and send FILE_BLOCK_CHECKSUM events
        int totalNumBlocks = startAckEvent.getTotalNumBlocks();
        int curIndex = 0;
        int remainingEventBytes = 0;
        int checksumBytes = 0;
        int numCurrentBlocks = 0;
        boolean ret = false;
        while(curIndex < checksumArray.length) {
            // create FILE_BLOCK_CHECKSUM event
            CMFileSyncEventFileBlockChecksum checksumEvent = new CMFileSyncEventFileBlockChecksum();
            // 공통 필드 설정
            checksumEvent.setInitiatorName(initiatorName);
            checksumEvent.setInitiatorUuid(initiatorUuid);
            checksumEvent.setInitiatorDeviceUuid(startAckEvent.getInitiatorDeviceUuid());
            // 나머지 필드 설정
            checksumEvent.setFileEntryIndex(fileEntryIndex);
            checksumEvent.setTotalNumBlocks(totalNumBlocks);
            checksumEvent.setStartBlockIndex(curIndex);

            // calculate the maximum number of checksum elements
            remainingEventBytes = CMInfo.MAX_EVENT_SIZE - checksumEvent.getByteNum();
            checksumBytes = Integer.BYTES * 3;  // block index, weak checksum, length of array
            checksumBytes += CMInfo.STRONG_CHECKSUM_LEN;    // length of strong checksum
            numCurrentBlocks = remainingEventBytes / checksumBytes;
            if(curIndex + numCurrentBlocks > checksumArray.length)
                numCurrentBlocks = checksumArray.length - curIndex;

            // set numCurrentBlocks and checksum array fields
            checksumEvent.setNumCurrentBlocks(numCurrentBlocks);
            CMFileSyncBlockChecksum[] partialChecksumArray =
                    Arrays.copyOfRange(checksumArray, curIndex, curIndex+numCurrentBlocks);
            checksumEvent.setChecksumArray(partialChecksumArray);

            // send the event
            ret = CMEventManager.unicastEvent(checksumEvent, initiatorName, initiatorUuid);
            if(!ret) return false;

            // update the curIndex
            curIndex += numCurrentBlocks;
        }

        // create and send END_FILE_BLOCK_CHECKSUM event
        CMFileSyncEventEndFileBlockChecksum endChecksumEvent = new CMFileSyncEventEndFileBlockChecksum();
        // 공통 필드 설정
        endChecksumEvent.setInitiatorName(initiatorName);
        endChecksumEvent.setInitiatorUuid(initiatorUuid);
        endChecksumEvent.setInitiatorDeviceUuid(syncGenerator.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        endChecksumEvent.setFileEntryIndex(fileEntryIndex);
        endChecksumEvent.setTotalNumBlocks(totalNumBlocks); // checksumArrays.length
        endChecksumEvent.setBlockSize(startAckEvent.getBlockSize());
        ret = CMEventManager.unicastEvent(endChecksumEvent, initiatorName, initiatorUuid);
        if(!ret) return false;

        return true;
    }

    // called at the client (pull sync): sends the pull generator's block checksums to the server
    private boolean processSTART_FILE_BLOCK_CHECKSUM_ACK_AtClient(CMFileSyncEventStartFileBlockChecksumAck startAckEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_ACK_AtClient() called..");

        String initiatorName = startAckEvent.getInitiatorName();
        UUID initiatorUuid = startAckEvent.getInitiatorUuid();
        UUID initiatorDeviceUuid = startAckEvent.getInitiatorDeviceUuid();
        int fileEntryIndex = startAckEvent.getFileEntryIndex();
        int totalNumBlocks = startAckEvent.getTotalNumBlocks();
        int blockSize = startAckEvent.getBlockSize();

        // get the pull generator (CMFileSyncInfo holds a single instance on the client)
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncPullGenerator pullGenerator = syncInfo.getPullGenerator();
        if(pullGenerator == null) {
            System.err.println("CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_ACK_AtClient(), "
                    + "pullGenerator is null.");
            return false;
        }

        // get the block checksum array of the file
        CMFileSyncBlockChecksum[] checksumArray = pullGenerator.getBlockChecksumArrayMap().get(fileEntryIndex);
        if(checksumArray == null) {
            System.err.println("CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_ACK_AtClient(), "
                    + "checksumArray is null for fileEntryIndex = " + fileEntryIndex);
            return false;
        }

        // determine the receiver (default server)
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        String serverName = interInfo.getDefaultServerInfo().getServerName();
        UUID serverUuid = null;

        // repeat to create and send FILE_BLOCK_CHECKSUM events to the server
        int curIndex = 0;
        int remainingEventBytes;
        int checksumBytes;
        int numCurrentBlocks;
        boolean ret;
        while(curIndex < checksumArray.length) {
            CMFileSyncEventFileBlockChecksum checksumEvent = new CMFileSyncEventFileBlockChecksum();
            // 공통 필드 설정
            checksumEvent.setInitiatorName(initiatorName);
            checksumEvent.setInitiatorUuid(initiatorUuid);
            checksumEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
            // 나머지 필드 설정
            checksumEvent.setFileEntryIndex(fileEntryIndex);
            checksumEvent.setTotalNumBlocks(totalNumBlocks);
            checksumEvent.setStartBlockIndex(curIndex);

            // calculate the maximum number of checksum elements
            remainingEventBytes = CMInfo.MAX_EVENT_SIZE - checksumEvent.getByteNum();
            checksumBytes = Integer.BYTES * 3;  // block index, weak checksum, length of array
            checksumBytes += CMInfo.STRONG_CHECKSUM_LEN;    // length of strong checksum
            numCurrentBlocks = remainingEventBytes / checksumBytes;
            if(curIndex + numCurrentBlocks > checksumArray.length)
                numCurrentBlocks = checksumArray.length - curIndex;

            // set numCurrentBlocks and checksum array fields
            checksumEvent.setNumCurrentBlocks(numCurrentBlocks);
            CMFileSyncBlockChecksum[] partialChecksumArray =
                    Arrays.copyOfRange(checksumArray, curIndex, curIndex+numCurrentBlocks);
            checksumEvent.setChecksumArray(partialChecksumArray);

            // send the event to the server
            ret = CMEventManager.unicastEvent(checksumEvent, serverName, serverUuid);
            if(!ret) return false;

            curIndex += numCurrentBlocks;
        }

        // create and send END_FILE_BLOCK_CHECKSUM event to the server
        CMFileSyncEventEndFileBlockChecksum endChecksumEvent = new CMFileSyncEventEndFileBlockChecksum();
        // 공통 필드 설정
        endChecksumEvent.setInitiatorName(initiatorName);
        endChecksumEvent.setInitiatorUuid(initiatorUuid);
        endChecksumEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // 나머지 필드 설정
        endChecksumEvent.setFileEntryIndex(fileEntryIndex);
        endChecksumEvent.setTotalNumBlocks(totalNumBlocks);
        endChecksumEvent.setBlockSize(blockSize);
        ret = CMEventManager.unicastEvent(endChecksumEvent, serverName, serverUuid);
        if(!ret) return false;

        return true;
    }

    // called at the client (full push) or the server (pull sync) depending on system type
    private boolean processSTART_FILE_BLOCK_CHECKSUM(CMFileSyncEvent fse) {
        CMFileSyncEventStartFileBlockChecksum startChecksumEvent = (CMFileSyncEventStartFileBlockChecksum) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM() called..");
            System.out.println("startChecksumEvent = " + startChecksumEvent);
        }

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if(confInfo.getSystemType().equals("SERVER")) {
            return processSTART_FILE_BLOCK_CHECKSUM_AtServer(startChecksumEvent);
        } else if(confInfo.getSystemType().equals("CLIENT")) {
            return processSTART_FILE_BLOCK_CHECKSUM_AtClient(startChecksumEvent);
        } else {
            System.err.println("CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM(), unknown system type: "
                    + confInfo.getSystemType());
            return false;
        }
    }

    // called at the client. Full-sync(initial push) + incremental PUSH MODIFY 양쪽 모두 진입.
    // 10-2 doc 15207~15344 F-1: syncProgress == PUSH 분기 우선 처리 후, 아니면 기존 full-sync 로직.
    private boolean processSTART_FILE_BLOCK_CHECKSUM_AtClient(CMFileSyncEventStartFileBlockChecksum startChecksumEvent) {
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        // [F-1 PUSH 분기]
        // 진입 조건: syncProgress == PUSH + 이벤트의 initiator 3종이 본 클라와 일치 (PUSH 세션 initiator는 클라 자기 자신).
        // 책임: (i) 첫 START 도착 시 PushModifyState lazy 생성·등록
        //      (ii) 이번 entry의 fileEntryIndex 자료 자리 마련 (blockChecksumArrayMap/blockSizeMap/
        //           fileEntryIndexToRelativePathMap/isUpdateFileCompletedMap)
        //      (iii) START_FILE_BLOCK_CHECKSUM_ACK 송신 (서버로).
        if (syncInfo.getSyncProgress() == CMFileSyncProgress.PUSH) {
            CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
            CMUser myself = interInfo.getMyself();
            String initiatorName = startChecksumEvent.getInitiatorName();
            UUID initiatorUuid = startChecksumEvent.getInitiatorUuid();
            UUID initiatorDeviceUuid = startChecksumEvent.getInitiatorDeviceUuid();
            if (!myself.getName().equals(initiatorName)
                    || !myself.getUuid().equals(initiatorUuid)
                    || !syncInfo.getDeviceUuid().equals(initiatorDeviceUuid)) {
                System.err.println("processSTART_FILE_BLOCK_CHECKSUM PUSH branch: initiator mismatch: "
                        + "name=" + initiatorName + ", uuid=" + initiatorUuid
                        + ", deviceUuid=" + initiatorDeviceUuid);
                return false;
            }

            int fileEntryIndex = startChecksumEvent.getFileEntryIndex();
            int totalNumBlocks = startChecksumEvent.getTotalNumBlocks();
            int blockSize = startChecksumEvent.getBlockSize();
            String relPath = startChecksumEvent.getRelativePath();
            if (relPath == null || relPath.isEmpty()) {
                System.err.println("processSTART_FILE_BLOCK_CHECKSUM PUSH branch: "
                        + "relativePath is empty for fileEntryIndex=" + fileEntryIndex);
                return false;
            }

            // (i) lazy 생성 (정상 흐름에서는 직전 PUSH 세션이 processCOMPLETE_PUSH_SYNC에서 null 처리)
            CMFileSyncPushModifyState pushState = syncInfo.getPushModifyState();
            if (pushState == null) {
                String serverName = interInfo.getDefaultServerInfo().getServerName();
                pushState = new CMFileSyncPushModifyState(myself.getName(), myself.getUuid(),
                        syncInfo.getDeviceUuid(), serverName);
                syncInfo.setPushModifyState(pushState);
                if (CMInfo._CM_DEBUG) {
                    System.out.println("CMFileSyncPushModifyState lazy-created.");
                }
            }

            // (ii) entry 자리 마련 — 빈 배열 자리만, 채움은 F-3에서
            pushState.getBlockChecksumArrayMap().put(fileEntryIndex,
                    new CMFileSyncBlockChecksum[totalNumBlocks]);
            pushState.getBlockSizeMap().put(fileEntryIndex, blockSize);
            pushState.getFileEntryIndexToRelativePathMap().put(fileEntryIndex, relPath);
            pushState.getIsUpdateFileCompletedMap().put(relPath, false);

            // (iii) START_FILE_BLOCK_CHECKSUM_ACK 송신 (서버로)
            CMFileSyncEventStartFileBlockChecksumAck fse_ack = new CMFileSyncEventStartFileBlockChecksumAck();
            fse_ack.setInitiatorName(pushState.getInitiatorName());
            fse_ack.setInitiatorUuid(pushState.getInitiatorUuid());
            fse_ack.setInitiatorDeviceUuid(pushState.getInitiatorDeviceUuid());
            fse_ack.setFileEntryIndex(fileEntryIndex);
            fse_ack.setTotalNumBlocks(totalNumBlocks);
            fse_ack.setBlockSize(blockSize);
            fse_ack.setReturnCode(1);
            boolean sendResult = CMEventManager.unicastEvent(fse_ack, pushState.getServerName(), null);
            if (!sendResult) {
                System.err.println("processSTART_FILE_BLOCK_CHECKSUM PUSH branch: "
                        + "send START_FILE_BLOCK_CHECKSUM_ACK error!");
                return false;
            }
            return true;
        }

        // [기존 full-sync 분기]
        int fileIndex = startChecksumEvent.getFileEntryIndex();
        int totalNumBlocks = startChecksumEvent.getTotalNumBlocks();
        int returnCode = 1;

        // get the file in the initiator file entry list
        CMFileSyncInfo fsInfo = CMFileSyncInfo.getInstance();
        Path path = fsInfo.getPathList().get(fileIndex);
        if(CMInfo._CM_DEBUG) {
            System.out.println("path = " + path);
        }
        if(path == null) returnCode = 0;

        // create an array of CMFileSyncBlockChecksum for the file and add to the Map
        CMFileSyncBlockChecksum[] checksumArray = new CMFileSyncBlockChecksum[totalNumBlocks];
        Map<Integer, CMFileSyncBlockChecksum[]> checksumArrayMap = fsInfo.getBlockChecksumMap();
        Objects.requireNonNull(checksumArrayMap);
        checksumArrayMap.put(fileIndex, checksumArray);

        // get the fileIndexToHashToBlockIndexMap
        Map<Integer, Map<Short, Integer>> fileToHashToBlockMap =
                fsInfo.getFileIndexToHashToBlockIndexMap();
        Objects.requireNonNull(fileToHashToBlockMap);
        // create a hashToBlockMap object and add it to the Map
        Map<Short, Integer> hashToBlockMap = new Hashtable<>();
        fileToHashToBlockMap.put(fileIndex, hashToBlockMap);

        // create an ack event
        CMFileSyncEventStartFileBlockChecksumAck ackEvent = new CMFileSyncEventStartFileBlockChecksumAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(startChecksumEvent.getInitiatorName());
        ackEvent.setInitiatorUuid(startChecksumEvent.getInitiatorUuid());
        ackEvent.setInitiatorDeviceUuid(startChecksumEvent.getInitiatorDeviceUuid());
        // set fields as they are in the received event
        ackEvent.setBlockSize(startChecksumEvent.getBlockSize());
        ackEvent.setFileEntryIndex(fileIndex);
        ackEvent.setTotalNumBlocks(totalNumBlocks);
        // set return code
        ackEvent.setReturnCode(returnCode);

        // send the ack event
        return CMEventManager.unicastEvent(ackEvent, startChecksumEvent.getSender(), startChecksumEvent.getSenderUuid());
    }

    // called at the server (pull sync): registers fileEntryIndex<->path mapping in the
    // CMFileSyncPullModifyState holder and reserves structures to receive source block checksums.
    private boolean processSTART_FILE_BLOCK_CHECKSUM_AtServer(CMFileSyncEventStartFileBlockChecksum startChecksumEvent) {
        if(CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_AtServer() called..");

        int fileEntryIndex = startChecksumEvent.getFileEntryIndex();
        int totalNumBlocks = startChecksumEvent.getTotalNumBlocks();
        int blockSize = startChecksumEvent.getBlockSize();
        String relativePath = startChecksumEvent.getRelativePath();
        String initiatorName = startChecksumEvent.getInitiatorName();
        UUID initiatorUuid = startChecksumEvent.getInitiatorUuid();
        UUID initiatorDeviceUuid = startChecksumEvent.getInitiatorDeviceUuid();
        int returnCode = 1;

        // relativePath must be non-empty in pull sync
        if(relativePath == null || relativePath.isEmpty()) {
            System.err.println("CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_AtServer(), "
                    + "relativePath is empty.");
            returnCode = 0;
        }

        // resolve the server-side source file path (server sync home + relativePath)
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        if(returnCode == 1) {
            Path serverSyncHome = syncManager.getServerSyncHome(initiatorName);
            Path sourceAbsPath = serverSyncHome.resolve(relativePath).normalize();
            if(!Files.exists(sourceAbsPath)) {
                System.err.println("CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_AtServer(), "
                        + "source file does not exist: " + sourceAbsPath);
                returnCode = 0;
            }
        }

        // register the holder structures only when the source is valid
        if(returnCode == 1) {
            CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
            CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
            Map<CMFileSyncStateKey, CMFileSyncPullModifyState> pullModifyStateMap = syncInfo.getPullModifyStateMap();
            CMFileSyncPullModifyState pullModifyState = pullModifyStateMap.get(stateKey);
            if(pullModifyState == null) {
                pullModifyState = new CMFileSyncPullModifyState(initiatorName, initiatorUuid, initiatorDeviceUuid);
                pullModifyStateMap.put(stateKey, pullModifyState);
                if(CMInfo._CM_DEBUG)
                    System.out.println("created and registered new PullModifyState for stateKey = " + stateKey);
            }

            // reserve structures for this fileEntryIndex
            CMFileSyncBlockChecksum[] checksumArray = new CMFileSyncBlockChecksum[totalNumBlocks];
            pullModifyState.getBlockChecksumArrayMap().put(fileEntryIndex, checksumArray);
            Map<Short, Integer> hashToBlockMap = new Hashtable<>();
            pullModifyState.getFileIndexToHashToBlockIndexMap().put(fileEntryIndex, hashToBlockMap);
            pullModifyState.getBlockSizeMap().put(fileEntryIndex, blockSize);
            pullModifyState.getFileEntryIndexToRelativePathMap().put(fileEntryIndex, relativePath);
            pullModifyState.getIsUpdateFileCompletedMap().put(relativePath, false);
        }

        // create and send the ack event to the client (CMFileSyncPullGenerator)
        CMFileSyncEventStartFileBlockChecksumAck ackEvent = new CMFileSyncEventStartFileBlockChecksumAck();
        // 공통 필드 설정
        ackEvent.setInitiatorName(initiatorName);
        ackEvent.setInitiatorUuid(initiatorUuid);
        ackEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // 나머지 필드 설정 (수신 이벤트 그대로)
        ackEvent.setBlockSize(blockSize);
        ackEvent.setFileEntryIndex(fileEntryIndex);
        ackEvent.setTotalNumBlocks(totalNumBlocks);
        ackEvent.setReturnCode(returnCode);

        return CMEventManager.unicastEvent(ackEvent, startChecksumEvent.getSender(), startChecksumEvent.getSenderUuid());
    }

    // called at the client
    private boolean processREQUEST_NEW_FILES(CMFileSyncEvent fse) {
        CMFileSyncEventRequestNewFiles fse_rnf = (CMFileSyncEventRequestNewFiles) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processREQUEST_NEW_FILES() called..");
            System.out.println("event = " + fse_rnf);
        }
        //// to use the CMFileTransferManager service to push new files to the server

        // get CMFileSyncManager
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        // get the sync home
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        Path syncHome;
        if (confInfo.getSystemType().equals("SERVER")) {
            syncHome = syncManager.getServerSyncHome(fse_rnf.getSender());
        } else {
            syncHome = syncManager.getClientSyncHome();
        }
        // get the requester name
        String requesterName = fse_rnf.getSender();
        UUID requesterUuid = fse_rnf.getSenderUuid();
        // check if the requested file list is null or empty
        List<Path> requestedFileList = fse_rnf.getRequestedFileList();
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
            Path syncPath = syncHome.resolve(path);   // adjust the path with the sync home
            if( !CMFileTransferManager.pushFile(syncPath.toString(), requesterName, requesterUuid) )
                sendResult = false;
        }

        // The next sync task will be conducted at the CMFileTransferManager,
        // when the transmission of each requested file completes
        // by moving the transferred file to the server sync home.

        return sendResult;
    }

    // called at the server: pull-sync 의 CREATE 대상 파일들을 클라이언트로 push 한다.
    private boolean processREQUEST_PULL_CREATES(CMFileSyncEvent fse) {
        CMFileSyncEventRequestPullCreates fse_rpc = (CMFileSyncEventRequestPullCreates) fse;

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processREQUEST_PULL_CREATES() called..");
            System.out.println("event = " + fse_rpc);
        }

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if (!confInfo.getSystemType().equals("SERVER")) {
            System.err.println("CMFileSyncEventHandler.processREQUEST_PULL_CREATES(), system type is not SERVER!");
            return false;
        }

        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        String initiatorName = fse_rpc.getInitiatorName();
        UUID initiatorUuid = fse_rpc.getInitiatorUuid();

        List<Path> requestedFileList = fse_rpc.getRequestedFileList();
        if (requestedFileList == null || requestedFileList.isEmpty()) {
            System.err.println("CMFileSyncEventHandler.processREQUEST_PULL_CREATES(), requestedFileList is empty.");
            return false;
        }

        // initiator 의 서버측 sync home 기준으로 경로를 조립한다.
        Path serverSyncHome = syncManager.getServerSyncHome(initiatorName).toAbsolutePath().normalize();

        boolean sendResult = true;
        for (Path relPath : requestedFileList) {
            Path absPath = serverSyncHome.resolve(relPath).toAbsolutePath().normalize();
            // path traversal 방어: 조립 결과가 sync home 하위가 아니면 거부
            if (!absPath.startsWith(serverSyncHome)) {
                System.err.println("CMFileSyncEventHandler.processREQUEST_PULL_CREATES(), path traversal rejected: "
                        + relPath);
                sendResult = false;
                continue;
            }
            if (!Files.exists(absPath)) {
                System.err.println("CMFileSyncEventHandler.processREQUEST_PULL_CREATES(), file not found: " + absPath);
                sendResult = false;
                continue;
            }
            if (!CMFileTransferManager.pushFile(absPath.toString(), initiatorName, initiatorUuid)) {
                System.err.println("CMFileSyncEventHandler.processREQUEST_PULL_CREATES(), pushFile failed: " + absPath);
                sendResult = false;
            }
        }

        return sendResult;
    }

    // called at the server
    private boolean processSTART_PUSH_ENTRY_LIST(CMFileSyncEvent fse) {
        CMFileSyncEventStartPushEntryList fse_spel = (CMFileSyncEventStartPushEntryList) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST() called..");
            System.out.println("fse_spel = " + fse_spel);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        String initiatorName = fse_spel.getInitiatorName();
        UUID initiatorUuid = fse_spel.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_spel.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        int numTotalFiles = fse_spel.getNumTotalFiles();
        int returnCode = 1;

        if (!confInfo.getSystemType().equals("SERVER")) {
            System.err.println("CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST(), not a SERVER.");
            return false;
        }

        Path serverSyncHome = syncManager.getServerSyncHome(initiatorName);
        if (Files.notExists(serverSyncHome)) {
            try {
                Files.createDirectories(serverSyncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pushStateTable = syncInfo.getPushStateTable();
        if (pushStateTable.containsKey(stateKey)) {
            System.err.println("CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST(), " +
                    "push session already in progress for stateKey = " + stateKey);
            returnCode = 0;
        }

        // loginKey는 ACK 송신 실패 cleanup에서도 참조 — try 블록 밖에서 선언
        CMUserLoginKey loginKey = new CMUserLoginKey(initiatorName, initiatorUuid);

        if (returnCode == 1) {
            Map<String, CMFileSyncClientEntry> pushStateMap = new Hashtable<>();
            pushStateTable.put(stateKey, pushStateMap);
            // 10-2 doc 11718~11723: pushOpRecordTable lifecycle = 세션 시작 시 빈 List 마련.
            // 각 op 완료 분기에서 record add → completePushSync 일괄 append → ACK 시점 remove.
            syncInfo.getPushOpRecordTable().put(stateKey, new ArrayList<>());
            // 10-2 doc 10901~10927: 역방향 인덱스 등록 (loginKey → stateKey).
            // checkCompletePushCreate가 fileSender의 loginKey만 알 때 O(1)로 stateKey 조회용.
            syncInfo.getPushLoginKeyToStateKeyMap().put(loginKey, stateKey);
            if (CMInfo._CM_DEBUG) {
                System.out.println("registered new pushStateMap for stateKey = " + stateKey
                        + ", numTotalFiles = " + numTotalFiles);
            }
        }

        CMFileSyncEventStartPushEntryListAck ackEvent = new CMFileSyncEventStartPushEntryListAck();
        ackEvent.setInitiatorName(initiatorName);
        ackEvent.setInitiatorUuid(initiatorUuid);
        ackEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
        ackEvent.setNumTotalFiles(numTotalFiles);
        ackEvent.setReturnCode(returnCode);

        boolean sendResult = CMEventManager.unicastEvent(ackEvent, initiatorName, initiatorUuid);
        if (!sendResult) {
            System.err.println("CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST(), failed to send ACK.");
            if (returnCode == 1) {
                pushStateTable.remove(stateKey);
                syncInfo.getPushOpRecordTable().remove(stateKey);
                syncInfo.getPushLoginKeyToStateKeyMap().remove(loginKey);
            }
            return false;
        }

        return true;
    }

    // called at the client
    private boolean processSTART_PUSH_ENTRY_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventStartPushEntryListAck fse_ack = (CMFileSyncEventStartPushEntryListAck) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST_ACK() called..");
            System.out.println("fse_ack = " + fse_ack);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        String initiatorName = fse_ack.getInitiatorName();
        UUID initiatorUuid = fse_ack.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_ack.getInitiatorDeviceUuid();
        int returnCode = fse_ack.getReturnCode();
        int numTotalFiles = fse_ack.getNumTotalFiles();

        if (!confInfo.getSystemType().equals("CLIENT")) {
            System.err.println("CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST_ACK(), not a CLIENT.");
            return false;
        }

        if (returnCode == 0) {
            System.err.println("CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST_ACK(), " +
                    "server rejected push session start. abort.");
            syncInfo.setPushEntryList(null);
            syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
            return false;
        }

        List<CMFileSyncClientEntry> pushEntryList = syncInfo.getPushEntryList();
        if (pushEntryList == null) {
            System.err.println("CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST_ACK(), " +
                    "pushEntryList is null. unexpected.");
            return false;
        }

        if (numTotalFiles != pushEntryList.size()) {
            System.err.println("CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST_ACK(), " +
                    "numTotalFiles mismatch: ack=" + numTotalFiles + ", local=" + pushEntryList.size());
            return false;
        }

        CMFileSyncEventPushEntries fse_pe = new CMFileSyncEventPushEntries();
        fse_pe.setInitiatorName(initiatorName);
        fse_pe.setInitiatorUuid(initiatorUuid);
        fse_pe.setInitiatorDeviceUuid(initiatorDeviceUuid);
        fse_pe.setNumFilesCompleted(0);
        setPushNumFilesAndEntryList(fse_pe, 0);

        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        String serverName = interInfo.getDefaultServerInfo().getServerName();
        boolean sendResult = CMEventManager.unicastEvent(fse_pe, serverName, null);
        if (!sendResult) {
            System.err.println("CMFileSyncEventHandler.processSTART_PUSH_ENTRY_LIST_ACK(), " +
                    "failed to send first PUSH_ENTRIES.");
            return false;
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("first PUSH_ENTRIES sent. numFiles = " + fse_pe.getNumFiles());
        }

        return true;
    }

    // called at the client
    private CMFileSyncEventPushEntries setPushNumFilesAndEntryList(CMFileSyncEventPushEntries fse_pe,
                                                                   int startListIndex) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.setPushNumFilesAndEntryList() called..");
            System.out.println("startListIndex = " + startListIndex);
        }
        int curByteNum = fse_pe.getByteNum();

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        List<CMFileSyncClientEntry> pushEntryList = syncInfo.getPushEntryList();
        List<CMFileSyncClientEntry> subList = new ArrayList<>();
        int index = startListIndex;
        int numFiles = 0;

        while (curByteNum < CMInfo.MAX_EVENT_SIZE && index < pushEntryList.size()) {
            CMFileSyncClientEntry entry = pushEntryList.get(index);
            // CMFileSyncClientEntry serialized size (serverMtime excluded — PULL-only)
            int entryByteNum = CMInfo.STRING_LEN_BYTES_LEN
                    + entry.getPath().getBytes().length
                    + Long.BYTES          // size
                    + Long.BYTES          // curMtime
                    + Long.BYTES          // baseMtime
                    + Integer.BYTES       // opHint (CMFileSyncOp ordinal)
                    + Byte.BYTES          // isCompleted
                    + Byte.BYTES;         // isDirectory
            if (curByteNum + entryByteNum < CMInfo.MAX_EVENT_SIZE) {
                subList.add(entry);
                curByteNum += entryByteNum;
                numFiles++;
                index++;
            } else {
                break;
            }
        }

        fse_pe.setNumFiles(numFiles);
        if (subList.isEmpty()) {
            System.err.println("CMFileSyncEventHandler.setPushNumFilesAndEntryList(), subList is empty.");
        } else {
            fse_pe.setPushEntryList(subList);
        }
        return fse_pe;
    }

    // called at the server
    private boolean processPUSH_ENTRIES(CMFileSyncEvent fse) {
        CMFileSyncEventPushEntries fse_pe = (CMFileSyncEventPushEntries) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processPUSH_ENTRIES() called..");
            System.out.println("fse_pe = " + fse_pe);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        String initiatorName = fse_pe.getInitiatorName();
        UUID initiatorUuid = fse_pe.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_pe.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        int numFiles = fse_pe.getNumFiles();
        int prevNumFilesCompleted = fse_pe.getNumFilesCompleted();
        List<CMFileSyncClientEntry> receivedEntryList = fse_pe.getPushEntryList();
        int returnCode = 1;
        int numFilesCompleted = prevNumFilesCompleted;

        if (!confInfo.getSystemType().equals("SERVER")) {
            System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES(), not a SERVER.");
            return false;
        }

        Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pushStateTable = syncInfo.getPushStateTable();
        Map<String, CMFileSyncClientEntry> pushStateMap = pushStateTable.get(stateKey);
        if (pushStateMap == null) {
            System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES(), " +
                    "pushStateMap is null for stateKey = " + stateKey);
            returnCode = 0;
        }

        if (returnCode == 1) {
            if (receivedEntryList == null || receivedEntryList.isEmpty()) {
                System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES(), " +
                        "receivedEntryList is null or empty.");
                returnCode = 0;
            } else if (receivedEntryList.size() != numFiles) {
                System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES(), numFiles mismatch: " +
                        "event.numFiles = " + numFiles + ", actualSize = " + receivedEntryList.size());
                returnCode = 0;
            } else {
                for (CMFileSyncClientEntry entry : receivedEntryList) {
                    String relPath = entry.getPath();
                    if (pushStateMap.containsKey(relPath)) {
                        // 정상 흐름에서는 도달 불가 (caller pushEntryList는 Map.values() 스냅샷이라 unique 보장).
                        // 마지막 값으로 overwrite하여 진행, 송신측 진단을 위한 로그만 남김.
                        System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES(), " +
                                "duplicate path in push session: " + relPath);
                    }
                    pushStateMap.put(relPath, entry);
                }
                numFilesCompleted = prevNumFilesCompleted + numFiles;
                if (CMInfo._CM_DEBUG) {
                    System.out.println("pushStateMap updated. size = " + pushStateMap.size()
                            + ", numFilesCompleted = " + numFilesCompleted);
                }
            }
        }

        CMFileSyncEventPushEntriesAck ackEvent = new CMFileSyncEventPushEntriesAck();
        ackEvent.setInitiatorName(initiatorName);
        ackEvent.setInitiatorUuid(initiatorUuid);
        ackEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
        ackEvent.setNumFilesCompleted(numFilesCompleted);
        ackEvent.setNumFiles(numFiles);
        ackEvent.setReturnCode(returnCode);

        boolean sendResult = CMEventManager.unicastEvent(ackEvent, fse_pe.getSender(), fse_pe.getSenderUuid());
        if (!sendResult) {
            System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES(), failed to send PUSH_ENTRIES_ACK.");
            return false;
        }

        return returnCode == 1;
    }

    // called at the client
    private boolean processPUSH_ENTRIES_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventPushEntriesAck fse_ack = (CMFileSyncEventPushEntriesAck) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processPUSH_ENTRIES_ACK() called..");
            System.out.println("fse_ack = " + fse_ack);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        int returnCode = fse_ack.getReturnCode();
        int numFilesCompleted = fse_ack.getNumFilesCompleted();

        if (!confInfo.getSystemType().equals("CLIENT")) {
            System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES_ACK(), not a CLIENT.");
            return false;
        }

        if (returnCode == 0) {
            System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES_ACK(), " +
                    "server reported failure. abort push session.");
            syncInfo.setPushEntryList(null);
            syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
            return false;
        }

        List<CMFileSyncClientEntry> pushEntryList = syncInfo.getPushEntryList();
        if (pushEntryList == null) {
            System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES_ACK(), " +
                    "pushEntryList is null. unexpected.");
            return false;
        }
        int pushEntryListSize = pushEntryList.size();

        boolean sendResult;
        if (numFilesCompleted < pushEntryListSize) {
            sendResult = sendNextPushEntries(fse_ack);
        } else if (numFilesCompleted == pushEntryListSize) {
            sendResult = sendEND_PUSH_ENTRY_LIST(fse_ack);
        } else {
            System.err.println("CMFileSyncEventHandler.processPUSH_ENTRIES_ACK(), " +
                    "numFilesCompleted (" + numFilesCompleted + ") > pushEntryList size ("
                    + pushEntryListSize + ").");
            return false;
        }

        return sendResult;
    }

    // called at the client
    private boolean sendNextPushEntries(CMFileSyncEventPushEntriesAck fse_ack) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.sendNextPushEntries() called..");
        }
        CMFileSyncEventPushEntries newfse = new CMFileSyncEventPushEntries();
        newfse.setInitiatorName(fse_ack.getInitiatorName());
        newfse.setInitiatorUuid(fse_ack.getInitiatorUuid());
        newfse.setInitiatorDeviceUuid(fse_ack.getInitiatorDeviceUuid());
        newfse.setNumFilesCompleted(fse_ack.getNumFilesCompleted());
        int startListIndex = fse_ack.getNumFilesCompleted();
        setPushNumFilesAndEntryList(newfse, startListIndex);

        return CMEventManager.unicastEvent(newfse, fse_ack.getSender(), fse_ack.getSenderUuid());
    }

    // called at the client
    private boolean sendEND_PUSH_ENTRY_LIST(CMFileSyncEventPushEntriesAck fse_ack) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.sendEND_PUSH_ENTRY_LIST() called..");
        }
        CMFileSyncEventEndPushEntryList newfse = new CMFileSyncEventEndPushEntryList();
        newfse.setInitiatorName(fse_ack.getInitiatorName());
        newfse.setInitiatorUuid(fse_ack.getInitiatorUuid());
        newfse.setInitiatorDeviceUuid(fse_ack.getInitiatorDeviceUuid());
        newfse.setNumFilesCompleted(fse_ack.getNumFilesCompleted());

        return CMEventManager.unicastEvent(newfse, fse_ack.getSender(), fse_ack.getSenderUuid());
    }

    // called at the server
    private boolean processEND_PUSH_ENTRY_LIST(CMFileSyncEvent fse) {
        CMFileSyncEventEndPushEntryList fse_epel = (CMFileSyncEventEndPushEntryList) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST() called..");
            System.out.println("fse_epel = " + fse_epel);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        String initiatorName = fse_epel.getInitiatorName();
        UUID initiatorUuid = fse_epel.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_epel.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        int numFilesCompleted = fse_epel.getNumFilesCompleted();
        int returnCode = 1;

        if (!confInfo.getSystemType().equals("SERVER")) {
            System.err.println("CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST(), not a SERVER.");
            return false;
        }

        Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pushStateTable = syncInfo.getPushStateTable();
        Map<String, CMFileSyncClientEntry> pushStateMap = pushStateTable.get(stateKey);
        if (pushStateMap == null) {
            System.err.println("CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST(), " +
                    "pushStateMap is null for stateKey = " + stateKey);
            returnCode = 0;
        } else if (pushStateMap.size() != numFilesCompleted) {
            System.err.println("CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST(), entry count mismatch: " +
                    "pushStateMap.size = " + pushStateMap.size()
                    + ", event.numFilesCompleted = " + numFilesCompleted);
            returnCode = 0;
        }

        // ACK 송신을 op별 처리 시작보다 먼저 수행 (PULL 패턴과 일관, op별 비동기 처리 지연 방지)
        CMFileSyncEventEndPushEntryListAck ackEvent = new CMFileSyncEventEndPushEntryListAck();
        ackEvent.setInitiatorName(initiatorName);
        ackEvent.setInitiatorUuid(initiatorUuid);
        ackEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
        ackEvent.setNumFilesCompleted(numFilesCompleted);
        ackEvent.setReturnCode(returnCode);

        boolean sendResult = CMEventManager.unicastEvent(ackEvent, fse_epel.getSender(), fse_epel.getSenderUuid());
        if (!sendResult) {
            System.err.println("CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST(), failed to send ACK.");
            return false;
        }

        if (returnCode == 0) {
            // 검증 실패: stale state 누적 방지를 위해 즉시 세션 폐기 (자동 회복성 우선)
            if (pushStateMap != null) {
                pushStateTable.remove(stateKey);
            }
            return false;
        }

        boolean result = syncManager.proceedPushStateMap(stateKey, initiatorUuid);
        if (!result) {
            System.err.println("CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST(), " +
                    "failed to proceed pushStateMap for stateKey = " + stateKey);
            return false;
        }

        return true;
    }

    // called at the client
    private boolean processEND_PUSH_ENTRY_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventEndPushEntryListAck fse_epela = (CMFileSyncEventEndPushEntryListAck) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST_ACK() called..");
            System.out.println("fse_epela = " + fse_epela);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        int returnCode = fse_epela.getReturnCode();
        int numFilesCompleted = fse_epela.getNumFilesCompleted();

        if (!confInfo.getSystemType().equals("CLIENT")) {
            System.err.println("CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST_ACK(), not a CLIENT.");
            return false;
        }

        if (returnCode == 0) {
            System.err.println("CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST_ACK(), " +
                    "server reported entry count mismatch. abort push session. " +
                    "numFilesCompleted = " + numFilesCompleted);
            // 서버측은 processEND_PUSH_ENTRY_LIST에서 pushStateTable.remove로 이미 정리됨.
            // 클라는 자기 스냅샷만 롤백.
            syncInfo.setPushEntryList(null);
            syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
            return false;
        } else if (returnCode != 1) {
            System.err.println("CMFileSyncEventHandler.processEND_PUSH_ENTRY_LIST_ACK(), " +
                    "invalid returnCode: " + returnCode);
            return false;
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("entry list phase completed. waiting for op events. "
                    + "numFilesCompleted = " + numFilesCompleted);
        }
        return true;
    }

    // called at the client
    private boolean processCOMPLETE_PUSH_DELETE(CMFileSyncEvent fse) {
        CMFileSyncEventCompletePushDelete fse_cpd = (CMFileSyncEventCompletePushDelete) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_PUSH_DELETE() called..");
            System.out.println("fse_cpd = " + fse_cpd);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        CMUser myself = interInfo.getMyself();
        String initiatorName = fse_cpd.getInitiatorName();
        UUID initiatorUuid = fse_cpd.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_cpd.getInitiatorDeviceUuid();
        List<String> deletedPathList = fse_cpd.getDeletedPathList();

        // initiator 검증 (본 클라가 보낸 push 세션의 통보인지)
        if (!myself.getName().equals(initiatorName)
                || !myself.getUuid().equals(initiatorUuid)
                || !syncInfo.getDeviceUuid().equals(initiatorDeviceUuid)) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_DELETE(), initiator mismatch: "
                    + "name=" + initiatorName + ", uuid=" + initiatorUuid
                    + ", deviceUuid=" + initiatorDeviceUuid);
            return false;
        }

        // syncProgress 가드 (PUSH 세션 진행 중이어야 정상 통보)
        if (syncInfo.getSyncProgress() != CMFileSyncProgress.PUSH) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_DELETE(), stale event: "
                    + "syncProgress = " + syncInfo.getSyncProgress());
            return false;
        }

        if (deletedPathList == null || deletedPathList.isEmpty()) {
            if (CMInfo._CM_DEBUG) {
                System.out.println("deletedPathList is empty. nothing to update.");
            }
            return true;
        }

        // 인메모리 client-index에서 path 메타 정보 제거.
        // CLAUDE.md divergence: lastSyncedMtimeMap 외에 lastSyncedSizeMap도 함께 정리해야
        // WatchService self-event 필터(mtime+size)가 일관 유지됨.
        Map<String, Long> lastSyncedMtimeMap = syncInfo.getLastSyncedMtimeMap();
        Map<String, Long> lastSyncedSizeMap = syncInfo.getLastSyncedSizeMap();
        for (String deletedPath : deletedPathList) {
            Long prevMtime = lastSyncedMtimeMap.remove(deletedPath);
            lastSyncedSizeMap.remove(deletedPath);
            if (CMInfo._CM_DEBUG) {
                if (prevMtime == null) {
                    System.out.println("path not in lastSyncedMtimeMap (already gone): " + deletedPath);
                } else {
                    System.out.println("removed from lastSynced maps: " + deletedPath
                            + " (prev mtime = " + prevMtime + ")");
                }
            }
        }

        // 본 핸들러는 client-index 갱신까지만 책임. 세션 완료/cursor 갱신/pushEntryList 정리는
        // processCOMPLETE_PUSH_SYNC에서 일괄 처리.
        return true;
    }

    // called at the client
    private boolean processCOMPLETE_PUSH_CREATE(CMFileSyncEvent fse) {
        CMFileSyncEventCompletePushCreate fse_cpc = (CMFileSyncEventCompletePushCreate) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_PUSH_CREATE() called..");
            System.out.println("fse_cpc = " + fse_cpc);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        CMUser myself = interInfo.getMyself();
        String initiatorName = fse_cpc.getInitiatorName();
        UUID initiatorUuid = fse_cpc.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_cpc.getInitiatorDeviceUuid();
        String createdPath = fse_cpc.getCreatedPath();

        // initiator 검증
        if (!myself.getName().equals(initiatorName)
                || !myself.getUuid().equals(initiatorUuid)
                || !syncInfo.getDeviceUuid().equals(initiatorDeviceUuid)) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_CREATE(), initiator mismatch: "
                    + "name=" + initiatorName + ", uuid=" + initiatorUuid
                    + ", deviceUuid=" + initiatorDeviceUuid);
            return false;
        }

        // syncProgress 가드
        if (syncInfo.getSyncProgress() != CMFileSyncProgress.PUSH) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_CREATE(), stale event: "
                    + "syncProgress = " + syncInfo.getSyncProgress());
            return false;
        }

        // pendingPushMap에 createdPath이 존재하는지 sanity check (정상 흐름 보장 — entry 값은 사용 안 함)
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        if (pendingPushMap == null || !pendingPushMap.containsKey(createdPath)) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_CREATE(), "
                    + "createdPath not found in pendingPushMap: " + createdPath);
            return false;
        }

        // 인메모리 client-index 갱신은 PULL의 checkCompletePullCreate 패턴과 일관하게
        // 현재 디스크 측정값을 truth로 사용 (self-event 필터가 디스크 현재 상태와 비교하므로
        // lastSyncedMap도 동일 출처여야 정확. 디렉토리 size는 OS dirent 값으로 일관 측정).
        CMFileSyncManager syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        Path clientSyncHome = syncManager.getClientSyncHome();
        Path absPath = clientSyncHome.resolve(createdPath).toAbsolutePath().normalize();
        long curMtime;
        long curSize;
        try {
            curMtime = syncInfo.currentMtimeSecOrMinusOne(absPath);
            curSize = syncInfo.currentSizeOrMinusOne(absPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Long prevMtime = syncInfo.getLastSyncedMtimeMap().get(createdPath);
        syncInfo.setLastSynced(createdPath, curMtime, curSize);
        if (CMInfo._CM_DEBUG) {
            if (prevMtime != null) {
                System.out.println("warning: createdPath already in lastSyncedMtimeMap: " + createdPath
                        + " (prev mtime = " + prevMtime + ", overwritten with " + curMtime + ")");
            } else {
                System.out.println("added to lastSynced maps: " + createdPath
                        + " (curMtime = " + curMtime + ", size = " + curSize + ")");
            }
        }

        // 세션 완료 판정/cursor 갱신/pendingPushMap·pushEntryList 정리는 processCOMPLETE_PUSH_SYNC에서.
        return true;
    }

    // called at the client
    // 10-2 doc 14539: PUSH 세션의 클라측 종단 핸들러.
    // 책임 순서: (i) initiator/syncProgress 검증 → (ii) 개수 정합으로 returnCode 결정 →
    //          (iii) COMPLETE_PUSH_SYNC_ACK 송신 → (iv) returnCode==0이면 cursor/cleanup skip →
    //          (v) saveClientIndex (lastSyncedMtimeMap 영속화는 본 메소드가 유일 flush 지점) →
    //          (vi) setCursor + saveClientCursor → (vii) pendingPushMap clear + pushEntryList null →
    //          (viii) syncProgress = NONE.
    private boolean processCOMPLETE_PUSH_SYNC(CMFileSyncEvent fse) {
        CMFileSyncEventCompletePushSync fse_cps = (CMFileSyncEventCompletePushSync) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC() called..");
            System.out.println("fse_cps = " + fse_cps);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        CMUser myself = interInfo.getMyself();
        String initiatorName = fse_cps.getInitiatorName();
        UUID initiatorUuid = fse_cps.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_cps.getInitiatorDeviceUuid();
        int numFilesCompleted = fse_cps.getNumFilesCompleted();
        long newServerCursor = fse_cps.getNewServerCursor();
        int returnCode = 0;

        // (i) initiator 검증
        if (!myself.getName().equals(initiatorName)
                || !myself.getUuid().equals(initiatorUuid)
                || !syncInfo.getDeviceUuid().equals(initiatorDeviceUuid)) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC(), initiator mismatch: "
                    + "name=" + initiatorName + ", uuid=" + initiatorUuid
                    + ", deviceUuid=" + initiatorDeviceUuid);
            return false;
        }

        // (i) syncProgress 가드 (stale 차단)
        if (syncInfo.getSyncProgress() != CMFileSyncProgress.PUSH) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC(), stale event: "
                    + "syncProgress = " + syncInfo.getSyncProgress());
            return false;
        }

        // (ii) 개수 정합 검증 — mismatch 시 returnCode=0 유지 + 계속 진행 (PULL 정책과 일관, ACK는 echo)
        List<CMFileSyncClientEntry> pushEntryList = syncInfo.getPushEntryList();
        int snapshotSize = (pushEntryList == null) ? 0 : pushEntryList.size();
        if (snapshotSize != numFilesCompleted) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC(), count mismatch: "
                    + "numFilesCompleted = " + numFilesCompleted
                    + ", pushEntryList.size = " + snapshotSize);
        } else {
            returnCode = 1;
        }

        // (iii) COMPLETE_PUSH_SYNC_ACK 송신 (returnCode 동행)
        CMFileSyncEventCompletePushSyncAck ackEvent = new CMFileSyncEventCompletePushSyncAck();
        ackEvent.setInitiatorName(initiatorName);
        ackEvent.setInitiatorUuid(initiatorUuid);
        ackEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);
        ackEvent.setNumFilesCompleted(numFilesCompleted);
        ackEvent.setReturnCode(returnCode);
        boolean sendResult = CMEventManager.unicastEvent(ackEvent, fse_cps.getSender(), fse_cps.getSenderUuid());
        if (!sendResult) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC(), "
                    + "failed to send COMPLETE_PUSH_SYNC_ACK.");
            // 서버는 이미 commit 완료 → cursor 적용은 idempotent, 송신 실패와 무관하게 진행 (PULL 6640-6642 패턴).
            // 단, returnCode==0이었던 경우는 진전 없음 → 즉시 false.
            if (returnCode == 0) return false;
        }

        // (iv) returnCode==0 시 cursor 적용·정리 skip (검증 실패 → 다음 세션 cursor 비교로 자동 따라잡기)
        if (returnCode == 0) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC(), "
                    + "verification failed; skip cursor apply and cleanup.");
            return false;
        }

        // (v) lastSyncedMtimeMap/lastSyncedSizeMap 영속화 (op별 핸들러 누적 결과를 본 메소드에서 1회 flush)
        syncInfo.saveClientIndex(".", newServerCursor);

        // (vi) cursor 적용 + 영속화
        syncInfo.setCursor(newServerCursor);
        syncInfo.saveClientCursor(".");

        // (vii) PUSH 세션 truth 일괄 정리
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        if (pendingPushMap != null) pendingPushMap.clear();
        syncInfo.setPushEntryList(null);

        // (viii) syncProgress = NONE (세션 종료, ACK/cursor/cleanup 모두 끝난 뒤)
        syncInfo.setSyncProgress(CMFileSyncProgress.NONE);

        if (CMInfo._CM_DEBUG) {
            System.out.println("PUSH session completed. newServerCursor = " + newServerCursor
                    + ", numFilesCompleted = " + numFilesCompleted);
        }
        return true;
    }

    // called at the server
    // 10-2 doc 14907: PUSH 세션의 server-side 마지막 핸들러.
    // 책임: returnCode 로깅 + pushStateTable/pushOpRecordTable 제거(completePushSync가 의도적으로 미정리한 부분)
    //      + pushGeneratorMap 잔여 방어 cleanup (정상 흐름이라면 F-6에서 이미 정리).
    // returnCode == 0이어도 정리는 진행(누수 방지 우선). 반환은 returnCode == 1 시에만 true.
    private boolean processCOMPLETE_PUSH_SYNC_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventCompletePushSyncAck fse_cpsa = (CMFileSyncEventCompletePushSyncAck) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC_ACK() called..");
            System.out.println("fse_cpsa = " + fse_cpsa);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        String initiatorName = fse_cpsa.getInitiatorName();
        UUID initiatorUuid = fse_cpsa.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_cpsa.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        int returnCode = fse_cpsa.getReturnCode();
        int numFilesCompleted = fse_cpsa.getNumFilesCompleted();

        // (1) returnCode 로깅 (PULL ACK 6749-6779 패턴)
        if (returnCode == 0) {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC_ACK(), "
                    + "client reported push-sync completion failure. stateKey = " + stateKey
                    + ", numFilesCompleted = " + numFilesCompleted);
        } else if (returnCode == 1) {
            if (CMInfo._CM_DEBUG) {
                System.out.println("client reported push-sync completion success. stateKey = " + stateKey
                        + ", numFilesCompleted = " + numFilesCompleted);
            }
        } else {
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC_ACK(), "
                    + "invalid returnCode: " + returnCode);
        }

        // (2) pushStateTable 정리 (completePushSync가 ACK 도달까지 의도적으로 보존한 자료)
        Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pushStateTable = syncInfo.getPushStateTable();
        Map<String, CMFileSyncClientEntry> removedStateMap = pushStateTable.remove(stateKey);
        if (CMInfo._CM_DEBUG) {
            System.out.println("pushStateTable entry removed for stateKey = " + stateKey
                    + ", removedStateMap size = "
                    + (removedStateMap == null ? "null" : removedStateMap.size()));
        }
        if (removedStateMap == null) {
            // 정상 흐름이라면 본 시점에 반드시 존재해야 함
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC_ACK(), "
                    + "pushStateTable has no entry for stateKey = " + stateKey + " (unexpected).");
        }

        // (3) pushOpRecordTable 정리 (10-2 doc 11999 lifecycle 종착점)
        Map<CMFileSyncStateKey, List<CMFileSyncChangeLogEntry>> pushOpRecordTable = syncInfo.getPushOpRecordTable();
        List<CMFileSyncChangeLogEntry> removedOpRecords = pushOpRecordTable.remove(stateKey);
        if (CMInfo._CM_DEBUG) {
            System.out.println("pushOpRecordTable entry removed for stateKey = " + stateKey
                    + ", removedOpRecords size = "
                    + (removedOpRecords == null ? "null" : removedOpRecords.size()));
        }

        // (4) 역방향 인덱스 정리 (10-2 doc 10951~10972, pushStateTable과 lifecycle 동기)
        CMUserLoginKey loginKey = new CMUserLoginKey(initiatorName, initiatorUuid);
        syncInfo.getPushLoginKeyToStateKeyMap().remove(loginKey);
        if (CMInfo._CM_DEBUG) {
            System.out.println("pushLoginKeyToStateKeyMap entry removed for loginKey = " + loginKey);
        }

        // (5) pushGeneratorMap 방어적 잔여 검사·제거 (PullModifyState 정리 6781-6814 대칭)
        // 정상 흐름이라면 F-6 핸들러가 마지막 entry 처리 직후 cleanupAll + remove를 수행했어야 함.
        Map<CMUserLoginKey, CMFileSyncPushGenerator> pushGeneratorMap = syncInfo.getPushGeneratorMap();
        CMFileSyncPushGenerator removedGen = pushGeneratorMap.remove(loginKey);
        if (removedGen != null) {
            removedGen.cleanupAll();
            if (CMInfo._CM_DEBUG) {
                System.out.println("pushGenerator removed and cleaned up for loginKey = " + loginKey);
            }
            System.err.println("CMFileSyncEventHandler.processCOMPLETE_PUSH_SYNC_ACK(), "
                    + "pushGenerator was still present for loginKey = " + loginKey
                    + " (defensive cleanup performed).");
        } else {
            // MODIFY 대상이 없었던 세션이거나 F-6에서 정상 정리됨
            if (CMInfo._CM_DEBUG) {
                System.out.println("pushGenerator is null (no MODIFY entries or already cleaned). loginKey = "
                        + loginKey);
            }
        }

        return returnCode == 1;
    }

    // called at the server
    private boolean processSTART_FILE_LIST(CMFileSyncEvent fse) {

        CMFileSyncEventStartFileList fse_sfl = (CMFileSyncEventStartFileList) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_LIST() called..");
            System.out.println("event = " + fse_sfl);
        }

        String initiatorName = fse_sfl.getInitiatorName();
        UUID initiatorUuid = fse_sfl.getInitiatorUuid();
        // get the file-sync manager
        CMFileSyncManager fsManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        // get server sync home for initiatorName
        // 추후 양방향 파일 동기화시에 server 명칭보다 receiver 명칭으로 수정 필요
        Path serverSyncHome = fsManager.getServerSyncHome(initiatorName);
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
        CMFileSyncEventStartFileListAck ackFse = new CMFileSyncEventStartFileListAck();
        // 공통 필드 설정
        ackFse.setInitiatorName(initiatorName);
        ackFse.setInitiatorUuid(initiatorUuid);
        ackFse.setInitiatorDeviceUuid(fse_sfl.getInitiatorDeviceUuid());
        // 나머지 필드 설정 (num total files, return code)
        ackFse.setNumTotalFiles(fse_sfl.getNumTotalFiles());
        ackFse.setReturnCode(1);    // always success

        // send the ack event to the client
        return CMEventManager.unicastEvent(ackFse, initiatorName, initiatorUuid);
    }

    // called at the client
    private boolean processSTART_FILE_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventStartFileListAck fse_sfla = (CMFileSyncEventStartFileListAck) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_LIST_ACK() called..");
            System.out.println("event = " + fse_sfla);
        }

        String receiver = fse_sfla.getSender();
        UUID receiverUuid = fse_sfla.getSenderUuid();

        // create a FILE_ENTRIES event
        CMFileSyncEventFileEntries newfse = new CMFileSyncEventFileEntries();
        // 공통 필드 설정
        newfse.setInitiatorName(fse_sfla.getInitiatorName());
        newfse.setInitiatorUuid(fse_sfla.getInitiatorUuid());
        newfse.setInitiatorDeviceUuid(fse_sfla.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        newfse.setNumFilesCompleted(0);
        // set numFiles and fileEntryList
        setNumFilesAndEntryList(newfse, 0);

        return CMEventManager.unicastEvent(newfse, receiver, receiverUuid);
    }

    // called at the client
    private CMFileSyncEvent setNumFilesAndEntryList(CMFileSyncEventFileEntries newfse, int startListIndex) {
        // get current number of bytes except the entry list
        int curByteNum = newfse.getByteNum();
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.setNumFilesAndEntryList() called..");
            System.out.println("startListIndex = " + startListIndex);
            System.out.println("curByteNum before adding entries = " + curByteNum);
        }
        // set variables before the while loop
        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        List<Path> pathList = syncInfo.getPathList();
        List<Path> subList = new ArrayList<>();
        int index = startListIndex;
        int numFiles = 0;
        CMFileSyncManager fsManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
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
                    + Long.BYTES
                    + Integer.BYTES;    // file type (CMFileType -> int)
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
/*
        List<CMFileSyncEntry> fileEntryList = subList.stream()
                .map(path -> {
                    CMFileSyncEntry fileEntry = new CMFileSyncEntry();
                    try {
                        fileEntry.setPathRelativeToHome(path.subpath(startPathIndex, path.getNameCount()))
                                .setSize(Files.size(path))
                                .setLastModifiedTime(Files.getLastModifiedTime(path))
                                .setType(CMFileType.getType(path));
                        if (CMInfo._CM_DEBUG)
                            System.out.println("fileEntry = " + fileEntry);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                    return fileEntry;
                }).collect(Collectors.toList());
*/
        Map<Path,Long> onlineModePathToSizeMap = Objects.requireNonNull(syncInfo.getOnlineModePathSizeMap());
        List<CMFileSyncEntry> fileEntryList = new ArrayList<>();
        for(Path path : subList) {
            CMFileSyncEntry fileEntry = new CMFileSyncEntry();
            try {
                fileEntry.setPathRelativeToHome(path.subpath(startPathIndex, path.getNameCount()))
                        .setLastModifiedTime(Files.getLastModifiedTime(path))
                        .setType(CMFileType.getType(path));
                // set size of the path according to file mode (online or local)
                if(fsManager.isOnlineMode(path))
                    fileEntry.setSize(onlineModePathToSizeMap.get(path));
                else fileEntry.setSize(Files.size(path));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            fileEntryList.add(fileEntry);
        }

        if (fileEntryList.isEmpty())
            System.err.println("fileEntryList is empty.");
        else
            newfse.setInitiatorPathEntryList(fileEntryList);

        return newfse;
    }

    // called at the server
    private boolean processFILE_ENTRIES(CMFileSyncEvent fse) {
        CMFileSyncEventFileEntries fse_fe = (CMFileSyncEventFileEntries) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processFILE_ENTRIES() called..");
            System.out.println("event = " + fse_fe);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        String initiatorName = fse_fe.getInitiatorName();
        UUID initiatorUuid = fse_fe.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_fe.getInitiatorDeviceUuid();
        int returnCode = 1;
        int numFilesCompleted = 0;
        int numFiles = fse_fe.getNumFiles();

        // if 0, the entry list is null in the event
        if(numFiles > 0) {
            // set or add the entry list of the event to the entry Map
            CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
            List<CMFileSyncEntry> entryList = syncInfo.getInitiatorPathEntryListMap().get(stateKey);
            if (entryList == null) {
                // set the new entry list to the Map
                syncInfo.getInitiatorPathEntryListMap().put(stateKey, fse_fe.getInitiatorPathEntryList());
                // set the number of completed files
                numFilesCompleted = numFiles;
            } else {
                // add the new entry list to the existing list
                boolean addResult = entryList.addAll(fse_fe.getInitiatorPathEntryList());
                if(!addResult) {
                    System.err.println("entry list add error!");
                    System.err.println("existing list = "+entryList);
                    System.err.println("new list = "+fse_fe.getInitiatorPathEntryList());
                    returnCode = 0;
                    numFilesCompleted = numFiles;
                }
                else {
                    // update the number of completed files
                    numFilesCompleted = fse_fe.getNumFilesCompleted() + numFiles;
                }
            }
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("numFilesCompleted = " + numFilesCompleted);
            System.out.println("returnCode = " + returnCode);
        }

        // create FILE_ENTRIES_ACK event
        CMFileSyncEventFileEntriesAck fseAck = new CMFileSyncEventFileEntriesAck();
        // 공통 필드 설정
        fseAck.setInitiatorName(initiatorName);
        fseAck.setInitiatorUuid(initiatorUuid);
        fseAck.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // 나머지 필드 설정
        fseAck.setNumFilesCompleted(numFilesCompleted);   // updated
        fseAck.setNumFiles(numFiles);
        fseAck.setReturnCode(returnCode);

        // send the ack event
        return CMEventManager.unicastEvent(fseAck, initiatorName, initiatorUuid);
    }

    // called at the client
    private boolean processFILE_ENTRIES_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventFileEntriesAck fse_fea = (CMFileSyncEventFileEntriesAck) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processFILE_ENTRIES_ACK() called..");
            System.out.println("event = " + fse_fea);
        }

        // check the return code
        int returnCode = fse_fea.getReturnCode();
        if (returnCode == 0) {
            System.err.println("return code = " + returnCode);
            return false;
        }

        // check if there are remaining file entry elements to be sent
        int numFilesCompleted = fse_fea.getNumFilesCompleted();
        int pathListSize = CMFileSyncInfo.getInstance().getPathList().size();
        boolean result;
        if (numFilesCompleted < pathListSize) {
            // send the next elements
            result = sendNextFileEntries(fse_fea);
        } else if (numFilesCompleted == pathListSize) {
            // send the END_FILE_LIST event
            result = sendEND_FILE_LIST(fse_fea);
        } else {
            System.err.println("numFilesCompleted = " + numFilesCompleted);
            System.err.println("pathListSize = " + pathListSize);
            return false;
        }

        return result;
    }

    // called at the client
    private boolean sendEND_FILE_LIST(CMFileSyncEventFileEntriesAck fse) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.sendEND_FILE_LIST() called..");
        }

        // create an END_FILE_LIST event
        CMFileSyncEventEndFileList newfse = new CMFileSyncEventEndFileList();
        // 공통 필드 설정
        newfse.setInitiatorName(fse.getInitiatorName());
        newfse.setInitiatorUuid(fse.getInitiatorUuid());
        newfse.setInitiatorDeviceUuid(fse.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        newfse.setNumFilesCompleted(fse.getNumFilesCompleted());

        // send the event to the sync receiver
        return CMEventManager.unicastEvent(newfse, fse.getSender(), fse.getSenderUuid());
    }

    // called at the client
    private boolean sendNextFileEntries(CMFileSyncEventFileEntriesAck fse) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.sendNextFileEntries() called..");
        }

        // create FILE_ENTRIES event
        CMFileSyncEventFileEntries newfse = new CMFileSyncEventFileEntries();
        // 공통 필드 설정
        newfse.setInitiatorName(fse.getInitiatorName());
        newfse.setInitiatorUuid(fse.getInitiatorUuid());
        newfse.setInitiatorDeviceUuid(fse.getInitiatorDeviceUuid());
        // 나머지 필드 설정
        newfse.setNumFilesCompleted(fse.getNumFilesCompleted());

        // set numFiles and fileEntryList
        int startListIndex = fse.getNumFilesCompleted();
        setNumFilesAndEntryList(newfse, startListIndex);

        // send FILE_ENTRIES event
        return CMEventManager.unicastEvent(newfse, fse.getSender(), fse.getSenderUuid());
    }

    // called at the server
    private boolean processEND_FILE_LIST(CMFileSyncEvent fse) {
        CMFileSyncEventEndFileList fse_efl = (CMFileSyncEventEndFileList) fse;

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_LIST() called..");
            System.out.println("fse = " + fse_efl);
        }

        int returnCode;

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        // check the elements of file entry list
        String initiatorName = fse_efl.getInitiatorName();
        UUID initiatorUuid = fse_efl.getInitiatorUuid();
        UUID initiatorDeviceUuid = fse_efl.getInitiatorDeviceUuid();
        int numFilesCompleted = fse_efl.getNumFilesCompleted();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        List<CMFileSyncEntry> fileEntryList = syncInfo.getInitiatorPathEntryListMap()
                .get(stateKey);
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
        CMFileSyncEventEndFileListAck fseAck = new CMFileSyncEventEndFileListAck();
        // 공통 필드 설정
        fseAck.setInitiatorName(initiatorName);
        fseAck.setInitiatorUuid(initiatorUuid);
        fseAck.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // 나머지 필드 설정
        fseAck.setNumFilesCompleted(numFilesCompleted);
        fseAck.setReturnCode(returnCode);

        // send the ack event
        boolean result = CMEventManager.unicastEvent(fseAck, initiatorName, initiatorUuid);
        if (!result) {
            System.err.println("send END_FILE_LIST_ACK error!");
            return false;
        }

        // start CMFileSyncGeneratorTask
        CMFileSyncGenerator fileSyncGenerator =
                new CMFileSyncGenerator(initiatorName, initiatorUuid, initiatorDeviceUuid);
        ExecutorService es = CMThreadInfo.getInstance().getExecutorService();
        es.submit(fileSyncGenerator);
        // set the generator in the CMFileSyncInfo
        syncInfo.getSyncGeneratorMap().put(new CMUserLoginKey(initiatorName, initiatorUuid), fileSyncGenerator);

        return true;
    }

    // called at the client
    private boolean processEND_FILE_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventEndFileListAck fse_efla = (CMFileSyncEventEndFileListAck) fse;

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_LIST_ACK() called..");
            System.out.println("fse = " + fse_efla);
        }

        return true;
    }

    // called by the client
    private boolean processCOMPLETE_NEW_FILE(CMFileSyncEvent fse) {
        CMFileSyncEventCompleteNewFile fse_cnf = (CMFileSyncEventCompleteNewFile) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_NEW_FILE() called..");
            System.out.println("fse = " + fse_cnf);
        }
        // update info for the new-file completion at the client
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<Path, Boolean> isFileSyncCompletedMap = syncInfo.getIsFileSyncCompletedMap();
        Objects.requireNonNull(isFileSyncCompletedMap);
        isFileSyncCompletedMap.put(Paths.get(fse_cnf.getCompletedPath()), true);

        // CMFileSyncManager 구하기
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);

        // syncHome 구하기
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        Path syncHome;
        if(confInfo.getSystemType().equals("SERVER")) {
            syncHome = syncManager.getServerSyncHome(fse_cnf.getSender());
        } else {
            syncHome = syncManager.getClientSyncHome();
        }

        // 완료된 path의 절대 경로 구하기
        Path relPath = Paths.get(fse_cnf.getCompletedPath());
        Path absPath = syncHome.resolve(relPath).toAbsolutePath().normalize();
        // 절대경로의 현재 mtime + size 구하기 (self-event 필터용)
        long curMtime;
        long curSize;
        try {
            curMtime = syncInfo.currentMtimeSecOrMinusOne(absPath);
            curSize = syncInfo.currentSizeOrMinusOne(absPath);
        } catch (IOException e) {
            e.printStackTrace();
            curMtime = -1;
            curSize = -1;
        }

        // 인메모리 client-index Map에 (path, mtime, size) 추가하기
        syncInfo.setLastSynced(relPath.toString(), curMtime, curSize);

        // 인메모리 cursor 값 업데이트
        long memCursor = syncInfo.getCursor();
        long newCursor = fse_cnf.getCursor();
        if(CMInfo._CM_DEBUG) {
            System.out.printf("[CM] processCOMPLETE_NEW_FILE: cursor before=%d, after=%d%n", memCursor, newCursor);
        }
        if(memCursor >= newCursor) {
            System.err.printf("memory cursor %d >= received cursor %d%n", memCursor, newCursor);
        }
        syncInfo.setCursor(newCursor);

        return true;
    }

    // called by the client
    private boolean processCOMPLETE_UPDATE_FILE(CMFileSyncEvent fse) {
        CMFileSyncEventCompleteUpdateFile fse_cuf = (CMFileSyncEventCompleteUpdateFile) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_UPDATE_FILE() called..");
            System.out.println("fse = " + fse_cuf);
        }
        // update info for the file-update completion at the sync sender
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<Path, Boolean> isFileSyncCompletedMap = syncInfo.getIsFileSyncCompletedMap();
        Objects.requireNonNull(isFileSyncCompletedMap);
        isFileSyncCompletedMap.put(Paths.get(fse_cuf.getCompletedPath()), true);

        // CMFileSyncManager 구하기
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);

        // syncHome 구하기
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        Path syncHome;
        if(confInfo.getSystemType().equals("SERVER")) {
            syncHome = syncManager.getServerSyncHome(fse_cuf.getSender());
        } else {
            syncHome = syncManager.getClientSyncHome();
        }

        // 완료된 path의 절대 경로 구하기
        Path relPath = Paths.get(fse_cuf.getCompletedPath());
        Path absPath = syncHome.resolve(relPath).toAbsolutePath().normalize();
        // 절대경로의 현재 mtime + size 구하기 (self-event 필터용)
        long curMtime;
        long curSize;
        try {
            curMtime = syncInfo.currentMtimeSecOrMinusOne(absPath);
            curSize = syncInfo.currentSizeOrMinusOne(absPath);
        } catch (IOException e) {
            e.printStackTrace();
            curMtime = -1;
            curSize = -1;
        }

        // 인메모리 client-index Map에 (path, mtime, size) 추가하기
        syncInfo.setLastSynced(relPath.toString(), curMtime, curSize);

        // 인메모리 cursor 값 업데이트
        long memCursor = syncInfo.getCursor();
        long newCursor = fse_cuf.getCursor();
        if(CMInfo._CM_DEBUG) {
            System.out.printf("[CM] processCOMPLETE_UPDATE_FILE: cursor before=%d, after=%d%n", memCursor, newCursor);
        }
        if(memCursor >= newCursor) {
            System.err.printf("memory cursor %d >= received cursor %d%n", memCursor, newCursor);
        }
        syncInfo.setCursor(newCursor);

        return true;
    }

    // called by the client
    private boolean processCOMPLETE_DELETE_FILES(CMFileSyncEvent fse) {
        CMFileSyncEventCompleteDeleteFiles fse_cdf = (CMFileSyncEventCompleteDeleteFiles) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_DELETE_FILES() called..");
            System.out.println("fse = " + fse_cdf);
        }

        // 인메모리 client-index Map에서 삭제된 path list의 각 원소에 대해 (path, lastSyncedMtime) 삭제
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        List<String> deletedPathList = fse_cdf.getDeletedPathList();
        if(deletedPathList != null) {
            for(String relPath : deletedPathList) {
                syncInfo.removeLastSyncedMtime(relPath);
            }
        }

        // 인메모리 cursor 값 업데이트
        long memCursor = syncInfo.getCursor();
        long newCursor = fse_cdf.getCursor();
        if(CMInfo._CM_DEBUG) {
            System.out.printf("[CM] processCOMPLETE_DELETE_FILES: cursor before=%d, after=%d%n", memCursor, newCursor);
        }
        if(memCursor >= newCursor) {
            System.err.printf("memory cursor %d >= received cursor %d%n", memCursor, newCursor);
        }
        syncInfo.setCursor(newCursor);

        return true;
    }

    // called by the client
    private boolean processSKIP_UPDATE_FILE(CMFileSyncEvent fse) {
        CMFileSyncEventSkipUpdateFile skipFileEvent = (CMFileSyncEventSkipUpdateFile) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSKIP_UPDATE_FILE() called..");
            System.out.println("skipFileEvent = " + skipFileEvent);
        }
        // update info for the file-update completion at the client
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<Path, Boolean> isFileSyncCompletedMap = syncInfo.getIsFileSyncCompletedMap();
        Objects.requireNonNull(isFileSyncCompletedMap);
        isFileSyncCompletedMap.put(Paths.get(skipFileEvent.getSkippedPath()), true);

        return true;
    }

    // called by the client
    private boolean processCOMPLETE_FILE_SYNC(CMFileSyncEvent fse) {
        CMFileSyncEventCompleteFileSync fse_cfs = (CMFileSyncEventCompleteFileSync) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_FILE_SYNC() called..");
            System.out.println("fse = " + fse_cfs);
        }

        // compare event field (number of completed files) to the size of local sync-completion Map
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<Path, Boolean> isFileSyncCompletedMap = syncInfo.getIsFileSyncCompletedMap();
        int numFilesCompleted = fse_cfs.getNumFilesCompleted();
        int mapSize = isFileSyncCompletedMap.size();
        if(numFilesCompleted != mapSize) {
            System.err.println("numFilesCompleted="+numFilesCompleted+", Map size="+mapSize);
            return false;
        }
        // check each element of file-sync completion Map
        int numNotCompletedFiles = 0;
        for(Map.Entry<Path, Boolean> entry : isFileSyncCompletedMap.entrySet()) {
            Path k = entry.getKey();
            Boolean v = entry.getValue();
            if(!v) {
                System.err.println("path not completed = "+k);
                numNotCompletedFiles++;
            }
        }
        if(numNotCompletedFiles > 0) {
            System.err.println("# of files not yet synchronized = "+numNotCompletedFiles);
            return false;
        }

        // delete(initialize) isFileSyncCompletedMap for client in CMFileSyncInfo
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        syncInfo.getIsFileSyncCompletedMap().clear();

        // 인메모리 cursor 업데이트
        long memCursor = syncInfo.getCursor();
        long newCursor = fse_cfs.getCursor();
        if(CMInfo._CM_DEBUG) {
            System.out.printf("[CM] processCOMPLETE_FILE_SYNC: cursor before=%d, after=%d%n", memCursor, newCursor);
        }
        if(memCursor >= newCursor) {
            System.err.printf("memory cursor %d >= received cursor %d%n", memCursor, newCursor);
        }
        syncInfo.setCursor(newCursor);

        // 파일 cursor 가져오기
        Path cursorFile = syncInfo.getCursorFile(".");

        // cursor 파일 값이 정상이면,
        if(cursorFile != null && Files.exists(cursorFile)) {
            memCursor = syncInfo.getCursor();
            try {
                long fileCursor = Long.parseLong(Files.readString(cursorFile).trim());
                // memory cursor가 더 작은 비정상 상태이면,
                if(memCursor < fileCursor) {
                    System.err.printf("cursor regression detected!: mem = %d, file = %d%n", memCursor, fileCursor);
                    syncInfo.setCursor(fileCursor);
                    // 파일로 메타 정보 저장 필요 없음
                } else if(memCursor == fileCursor) {
                    System.out.printf("cursor values are the same: mem = %d, file = %d%n", memCursor, fileCursor);
                    // 파일로 메타 정보 저장 필요 없음
                } else {
                    // 정상적인 memory cursor가 file cursor보다 큰 상태이면,
                    syncInfo.saveClientCursor(".");
                    syncInfo.saveClientIndex(".", syncInfo.getCursor());
                }
            } catch(IOException | NumberFormatException e) {
                e.printStackTrace();
                syncInfo.saveClientCursor(".");
                syncInfo.saveClientIndex(".", syncInfo.getCursor());
            }
        }
        // cursor 파일이 없으면,
        else {
            syncInfo.saveClientCursor(".");
            syncInfo.saveClientIndex(".", syncInfo.getCursor());
        }

        // change the file-sync state to stop
        syncInfo.setSyncProgress(CMFileSyncProgress.NONE);

        // check if the watch service has detected another change
        if(syncInfo.isFileChangeDetected() && !syncInfo.isWatchServiceTaskDone()) {
            syncInfo.setFileChangeDetected(false);
            // TODO: 양방향 push 동기화(startPushSync) 구현 후 교체 예정
            syncManager.startFullPushSync();
        }

        return true;
    }
}
