package kr.ac.konkuk.ccslab.cm.event.handler;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.*;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileType;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;

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
            case CMFileSyncEvent.START_FILE_LIST -> processResult = processSTART_FILE_LIST(fse);
            case CMFileSyncEvent.START_FILE_LIST_ACK -> processResult = processSTART_FILE_LIST_ACK(fse);
            case CMFileSyncEvent.FILE_ENTRIES -> processResult = processFILE_ENTRIES(fse);
            case CMFileSyncEvent.FILE_ENTRIES_ACK -> processResult = processFILE_ENTRIES_ACK(fse);
            case CMFileSyncEvent.END_FILE_LIST -> processResult = processEND_FILE_LIST(fse);
            case CMFileSyncEvent.END_FILE_LIST_ACK -> processResult = processEND_FILE_LIST_ACK(fse);
            case CMFileSyncEvent.REQUEST_NEW_FILES -> processResult = processREQUEST_NEW_FILES(fse);
            case CMFileSyncEvent.COMPLETE_NEW_FILE -> processResult = processCOMPLETE_NEW_FILE(fse);
            case CMFileSyncEvent.COMPLETE_UPDATE_FILE -> processResult = processCOMPLETE_UPDATE_FILE(fse);
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
            default -> {
                System.err.println("CMFileSyncEventHandler::processEvent(), invalid event id(" + eventId + ")!");
                return false;
            }
        }

        return processResult;
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

        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());

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
        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
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
        syncInfo.setSyncInProgress(false);

        // perform file-sync
        ret = syncManager.sync();
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
        String requester = endEvent.getRequester();

/*
        //// get the number of local-mode files
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        List<Path> basisFileList = Objects.requireNonNull(syncInfo.getBasisFileListMap().get(requester));
        List<Path> onlineModePathList = syncInfo.getOnlineModePathListMap().get(requester);
        Objects.requireNonNull(onlineModePathList);
        // filter only file type in basisFileList
        List<Path> filteredBasisFileList = basisFileList.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());
        int numLocalModeFiles = filteredBasisFileList.size() - onlineModePathList.size();
*/

        // create an end-local-mode-list event
        CMFileSyncEventEndLocalModeListAck ackEvent = new CMFileSyncEventEndLocalModeListAck();
        ackEvent.setSender(endEvent.getReceiver());
        ackEvent.setReceiver(endEvent.getSender());
        ackEvent.setRequester(endEvent.getRequester());
        ackEvent.setNumLocalModeFiles(endEvent.getNumLocalModeFiles());
        ackEvent.setReturnCode(1);
/*
        ackEvent.setNumLocalModeFiles(numLocalModeFiles);   // number calculated at the server
        if(endEvent.getNumLocalModeFiles() == numLocalModeFiles)
            ackEvent.setReturnCode(1);
        else
            ackEvent.setReturnCode(0);
*/

        // send the ack event
        boolean ret = CMEventManager.unicastEvent(ackEvent, endEvent.getSender(), m_cmInfo);
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
        String requester = listEvent.getRequester();

/*
        // get the online-mode-list-map
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        Map<String, List<Path>> onlineModeListMap = Objects.requireNonNull(syncInfo.getOnlineModePathListMap());
        // get the online-mode list with requester
        List<Path> onlineModeList = Objects.requireNonNull(onlineModeListMap.get(requester));
*/
        // get sync home of requester
        CMFileSyncManager syncManager = Objects.requireNonNull(m_cmInfo.getServiceManager(CMFileSyncManager.class));
        Path serverSyncHome = Objects.requireNonNull(syncManager.getServerSyncHome(requester));

        // start push-file for all paths in the event
        boolean ret = true;
        for(Path relativePath : listEvent.getRelativePathList()) {
            // get the absolute path
            Path absPath = serverSyncHome.resolve(relativePath);
            // start push-file
            ret &= CMFileTransferManager.pushFile(absPath.toString(), requester, m_cmInfo);
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
        ackEvent.setSender(listEvent.getReceiver());
        ackEvent.setReceiver(listEvent.getSender());
        ackEvent.setRequester(requester);
        ackEvent.setRelativePathList(listEvent.getRelativePathList());
        if(ret) ackEvent.setReturnCode(1);
        else ackEvent.setReturnCode(0);

        ret = CMEventManager.unicastEvent(ackEvent, listEvent.getSender(), m_cmInfo);
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

        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());

        // print the online mode files
        if(CMInfo._CM_DEBUG) {
            System.out.println("--- online mode files ---");
            //syncInfo.getOnlineModePathList().stream().forEach(System.out::println);
            syncInfo.getOnlineModePathSizeMap().entrySet().stream().forEach(System.out::println);
            System.out.println("---");
        }

        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
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
        syncInfo.setSyncInProgress(false);

        // restart watch service
        ret = syncManager.startWatchService();
        if(!ret) {
            System.err.println("error to start WatchService!");
            return false;
        }

        // perform file-sync
        ret = syncManager.sync();
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

/*
        // get the online mode list of requester
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        Map<String, List<Path>> onlineModeListMap = syncInfo.getOnlineModePathListMap();
        Objects.requireNonNull(onlineModeListMap);
        List<Path> onlineModeList = onlineModeListMap.get(endEvent.getRequester());
        Objects.requireNonNull(onlineModeList);
*/

        // create and send ack event
        CMFileSyncEventEndOnlineModeListAck ackEvent = new CMFileSyncEventEndOnlineModeListAck();
        ackEvent.setSender(endEvent.getReceiver());
        ackEvent.setReceiver(endEvent.getSender());
        ackEvent.setRequester(endEvent.getRequester());
        ackEvent.setNumOnlineModeFiles(endEvent.getNumOnlineModeFiles());
        ackEvent.setReturnCode(1);
/*
        int numOnlineFiles = endEvent.getNumOnlineModeFiles();
        ackEvent.setNumOnlineModeFiles(numOnlineFiles);
        if(numOnlineFiles == onlineModeList.size())
            ackEvent.setReturnCode(1);
        else
            ackEvent.setReturnCode(0);
*/

        boolean ret = CMEventManager.unicastEvent(ackEvent, endEvent.getSender(), m_cmInfo);
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
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        ConcurrentLinkedQueue<Path> requestQueue = Objects.requireNonNull(syncInfo.getOnlineModeRequestQueue());
        // get the list in event
        List<Path> eventList = ackEvent.getRelativePathList();
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
        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
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
            // if the head is in the list of event
            if(eventList.contains(relativeHeadPath)) {
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
            endEvent.setSender(ackEvent.getReceiver());
            endEvent.setReceiver(ackEvent.getSender());
            endEvent.setRequester(ackEvent.getRequester());
            //endEvent.setNumOnlineModeFiles(onlineModeList.size());
            endEvent.setNumOnlineModeFiles(onlineModePathSizeMap.size());

            boolean ret = CMEventManager.unicastEvent(endEvent, ackEvent.getSender(), m_cmInfo);
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
        String requester = listEvent.getRequester();
/*
        // get online-mode-list Map
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        Map<String, List<Path>> onlineModeListMap = syncInfo.getOnlineModePathListMap();
        Objects.requireNonNull(onlineModeListMap);
        // get the online mode list with the requester
        List<Path> onlineModeList = onlineModeListMap.get(requester);
        if(onlineModeList == null) {
            onlineModeList = new ArrayList<>();
            onlineModeListMap.put(requester, onlineModeList);
        }
        // add the list in the event to the online mode list
        boolean ret = onlineModeList.addAll(listEvent.getRelativePathList());
        if(!ret) {
            System.err.println("error to add event list to the online mode list!");
        }
*/

        // create and send an ack event
        CMFileSyncEventOnlineModeListAck ackEvent = new CMFileSyncEventOnlineModeListAck();
        ackEvent.setSender(listEvent.getReceiver());
        ackEvent.setReceiver(listEvent.getSender());
        ackEvent.setRequester(requester);
        ackEvent.setRelativePathList(listEvent.getRelativePathList());
        ackEvent.setReturnCode(1);
/*
        if(ret) ackEvent.setReturnCode(1);
        else ackEvent.setReturnCode(0);
*/

        boolean ret = CMEventManager.unicastEvent(ackEvent, listEvent.getSender(), m_cmInfo);
        if(!ret) {
            System.err.println("send error : "+ackEvent);
            return false;
        }

        return true;
    }

    // called at the server
    private boolean processEND_FILE_BLOCK_CHECKSUM_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventEndFileBlockChecksumAck ackEvent = (CMFileSyncEventEndFileBlockChecksumAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM_ACK() called..");
            System.out.println("ackEvent = " + ackEvent);
        }

        // check the return code
        final int returnCode = ackEvent.getReturnCode();
        if(returnCode != 1) {
            System.err.println("return code error: " + returnCode);
            return false;
        }

        // get the sync generator reference
        String userName = ackEvent.getSender();
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(userName);
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

        List<Path> basisFileList = Objects.requireNonNull(m_cmInfo.getFileSyncInfo().getBasisFileListMap()
                .get(userName));
        Path basisFilePath = basisFileList.get(basisFileIndex);
        if(basisFilePath == null) {
            System.err.println("Basis file path NOT FOUND for basis file index ("+basisFileIndex+")!");
            return false;
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("basisFilePath = " + basisFilePath);
        }

        // get the temp file path
        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        Path tempFilePath = syncManager.getTempPathOfBasisFile(basisFilePath);
        if(CMInfo._CM_DEBUG) {
            System.out.println("tempFilePath = " + tempFilePath);
        }

        // get the client file entry reference
        CMFileSyncEntry clientFileEntry = Optional.of(m_cmInfo)
                .map(CMInfo::getFileSyncInfo)
                .map(CMFileSyncInfo::getClientPathEntryListMap)
                .map(t -> t.get(userName))
                .map(l -> l.get(fileEntryIndex))
                .orElse(null);
        if(clientFileEntry == null) {
            System.err.println("client file entry is null! : userName("+userName+"), file entry index("
                    +fileEntryIndex+")");
            return false;
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("clientFileEntry = " + clientFileEntry);
        }

        if(basisFileChannel != null && tempFileChannel != null) {
            // compare file checksum of the temp file and the checksum of the ack event
            byte[] fileChecksum = syncManager.calculateFileChecksum(tempFilePath);
            if(!Arrays.equals(fileChecksum, ackEvent.getFileChecksum())) {
                System.err.println("File checksum error!");
                System.err.println("temp checksum = " + DatatypeConverter.printHexBinary(fileChecksum));
                System.err.println("event checksum = " + DatatypeConverter.printHexBinary(ackEvent.getFileChecksum()));
                return false;
            }

            // set the last modified time to that of the client file entry
            try {
                Files.setLastModifiedTime(tempFilePath, clientFileEntry.getLastModifiedTime());
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
                Files.setLastModifiedTime(basisFilePath, clientFileEntry.getLastModifiedTime());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // complete the update-existing-file task
        boolean result = syncManager.completeUpdateFile(userName, basisFilePath);
        if(result) {
            // check if the file-sync is complete or not
            if( syncManager.isCompleteFileSync(userName) ) {
                // complete the file-sync task
                syncManager.completeFileSync(userName);
            }
        }

        return true;
    }

    // called at the server
    private boolean processUPDATE_EXISTING_FILE(CMFileSyncEvent fse) {
        CMFileSyncEventUpdateExistingFile updateEvent = (CMFileSyncEventUpdateExistingFile) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processUPDATE_EXISTING_FILE() called..");
            System.out.println("updateEvent = " + updateEvent);
        }

        // get the server sync home
        String userName = updateEvent.getSender();
        CMFileSyncManager syncManager = Objects.requireNonNull(m_cmInfo.getServiceManager(CMFileSyncManager.class));
        Path serverSyncHome = Objects.requireNonNull(syncManager.getServerSyncHome(userName));

        // get the basis file path
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().get(userName);
        Objects.requireNonNull(syncGenerator);
        int fileEntryIndex = updateEvent.getFileEntryIndex();
        int basisFileIndex = syncGenerator.getBasisFileIndexMap().get(fileEntryIndex);
        List<Path> basisFileList = Objects.requireNonNull(m_cmInfo.getFileSyncInfo().getBasisFileListMap()
                .get(userName));
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

            // get the block size of this file
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

    // called at the client
    private boolean processEND_FILE_BLOCK_CHECKSUM(CMFileSyncEvent fse) {
        CMFileSyncEventEndFileBlockChecksum endChecksumEvent = (CMFileSyncEventEndFileBlockChecksum) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_BLOCK_CHECKSUM() called..");
            System.out.println("endChecksumEvent = " + endChecksumEvent);
        }

        int fileEntryIndex = endChecksumEvent.getFileEntryIndex();
        int blockSize = endChecksumEvent.getBlockSize();
        String sender = endChecksumEvent.getReceiver();
        String receiver = endChecksumEvent.getSender();
        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
        Objects.requireNonNull(syncManager);
        // get the local path list
        List<Path> pathList = Objects.requireNonNull(m_cmInfo.getFileSyncInfo().getPathList());
        // get the target file path
        Path path = Objects.requireNonNull(pathList.get(fileEntryIndex));

        boolean ret = compareFileBlocks(endChecksumEvent);
        if(!ret) {
            System.err.println("error to compare file blocks!");
            return false;
        }

        // create and send an END_FILE_BLOCK_CHECKSUM_ACK event
        CMFileSyncEventEndFileBlockChecksumAck ackEvent = new CMFileSyncEventEndFileBlockChecksumAck();
        ackEvent.setSender(sender);
        ackEvent.setReceiver(receiver);
        ackEvent.setFileEntryIndex(fileEntryIndex);
        ackEvent.setTotalNumBlocks(endChecksumEvent.getTotalNumBlocks());
        ackEvent.setBlockSize(blockSize);
        // set a return code
/*
            if(channel.position() == channel.size())
                ackEvent.setReturnCode(1);
            else
                ackEvent.setReturnCode(0);
*/
        ackEvent.setReturnCode(1);

        // calculate a file checksum and set it to the ack event
        byte[] fileChecksum = syncManager.calculateFileChecksum(path);
        ackEvent.setFileChecksum(fileChecksum);
        // send the ack event
        ret = CMEventManager.unicastEvent(ackEvent, receiver, m_cmInfo);
        if(!ret) return false;

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
        Map<Integer, CMFileSyncBlockChecksum[]> checksumMap = m_cmInfo.getFileSyncInfo()
                .getBlockChecksumMap();
        Objects.requireNonNull(checksumMap);
        // get block checksum array with the file entry index
        CMFileSyncBlockChecksum[] checksumArray = checksumMap.get(fileEntryIndex);
        Objects.requireNonNull(checksumArray);

        // get the local path list
        List<Path> pathList = Objects.requireNonNull(m_cmInfo.getFileSyncInfo().getPathList());
        // get the target file path
        Path path = Objects.requireNonNull(pathList.get(fileEntryIndex));

        // get the file sync manager
        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
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
        String sender = endChecksumEvent.getReceiver();
        String receiver = endChecksumEvent.getSender();

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
                        boolean ret = sendUpdateExistingFileEvent(sender, receiver, fileEntryIndex,
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
                            boolean ret = sendUpdateExistingFileEvent(sender, receiver, fileEntryIndex,
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
                            boolean ret = sendUpdateExistingFileEvent(sender, receiver, fileEntryIndex,
                                    nonMatchBuffer, -1);
                            if(!ret) return false;
                            nonMatchBuffer.clear();
                            buffer.limit(oldLimit);
                        }
                        else {
                            nonMatchBuffer.put(buffer);
                            boolean ret = sendUpdateExistingFileEvent(sender, receiver, fileEntryIndex,
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

    // called at the client
    private boolean sendUpdateExistingFileEvent(String sender, String receiver, int fileEntryIndex,
                                                ByteBuffer nonMatchBuffer, int matchBlockIndex) {
        ////// create and send an UPDATE_EXISTING_FILE event to the server
        CMFileSyncEventUpdateExistingFile updateEvent = new CMFileSyncEventUpdateExistingFile();
        updateEvent.setSender(sender);
        updateEvent.setReceiver(receiver);
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
        boolean ret = CMEventManager.unicastEvent(updateEvent, receiver, m_cmInfo);
        if(!ret) {
            System.err.println("send error, updateEvent = "+updateEvent);
            return false;
        }
        if(CMInfo._CM_DEBUG) {
            System.out.println("sent updateEvent = " + updateEvent);
        }
        return true;
    }

    // called at the client
    private int searchMatchBlockIndex(int sortedBlockIndex, int weakChecksum,
                                      CMFileSyncBlockChecksum[] checksumArray, short hash, ByteBuffer buffer) {
        if(CMInfo._CM_DEBUG_2) {
            System.out.println("=== CMFileSyncEventHandler.searchMatchBlockIndex() called..");
            System.out.println("sortedBlockIndex = " + sortedBlockIndex);
            System.out.println("weakChecksum = " + weakChecksum);
        }

        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
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
        Map<Integer, CMFileSyncBlockChecksum[]> blockChecksumMap = m_cmInfo.getFileSyncInfo()
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
        Map<Integer, Map<Short, Integer>> outerMap = m_cmInfo.getFileSyncInfo()
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

    // called at the client
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

    // called at the client
    private boolean processFILE_BLOCK_CHECKSUM(CMFileSyncEvent fse) {
        CMFileSyncEventFileBlockChecksum checksumEvent = (CMFileSyncEventFileBlockChecksum) fse;
        // store checksum in the Map
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processFILE_BLOCK_CHECKSUM() called..");
            System.out.println("checksumEvent = " + checksumEvent);
        }

        // get checksum array with the file entry index as a key
        Map<Integer, CMFileSyncBlockChecksum[]> checksumMap =
                m_cmInfo.getFileSyncInfo().getBlockChecksumMap();
        Objects.requireNonNull(checksumMap);
        CMFileSyncBlockChecksum[] checksumArray = checksumMap.get(checksumEvent.getFileEntryIndex());
        Objects.requireNonNull(checksumArray);

        // add sub array of the event to the checksum array
        CMFileSyncBlockChecksum[] subArray = checksumEvent.getChecksumArray();
        int startIndex = checksumEvent.getStartBlockIndex();
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

    // called at the server
    private boolean processSTART_FILE_BLOCK_CHECKSUM_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventStartFileBlockChecksumAck startAckEvent = (CMFileSyncEventStartFileBlockChecksumAck) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM_ACK() called..");
            System.out.println("startAckEvent = " + startAckEvent);
        }
        // get CMFileSyncGenerator reference
        String userName = startAckEvent.getSender();
        CMFileSyncGenerator syncGenerator = m_cmInfo.getFileSyncInfo().getSyncGeneratorMap()
                .get(userName);
        Objects.requireNonNull(syncGenerator);

        // get the block checksum array of the file
        int fileEntryIndex = startAckEvent.getFileEntryIndex();
        CMFileSyncBlockChecksum[] checksumArray = syncGenerator.getBlockChecksumArrayMap()
                .get(fileEntryIndex);
        Objects.requireNonNull(checksumArray);

        // repeat to create and send FILE_BLOCK_CHECKSUM events
        int totalNumBlocks = startAckEvent.getTotalNumBlocks();
        int curIndex = 0;
        String myName = m_cmInfo.getInteractionInfo().getMyself().getName();
        int remainingEventBytes = 0;
        int checksumBytes = 0;
        int numCurrentBlocks = 0;
        boolean ret = false;
        while(curIndex < checksumArray.length) {
            // create FILE_BLOCK_CHECKSUM event
            CMFileSyncEventFileBlockChecksum checksumEvent = new CMFileSyncEventFileBlockChecksum();
            checksumEvent.setSender(myName);
            checksumEvent.setReceiver(userName);
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
            ret = CMEventManager.unicastEvent(checksumEvent, startAckEvent.getSender(), m_cmInfo);
            if(!ret) return false;

            // update the curIndex
            curIndex += numCurrentBlocks;
        }

        // create and send END_FILE_BLOCK_CHECKSUM event
        CMFileSyncEventEndFileBlockChecksum endChecksumEvent = new CMFileSyncEventEndFileBlockChecksum();
        endChecksumEvent.setSender(m_cmInfo.getInteractionInfo().getMyself().getName());
        endChecksumEvent.setReceiver(userName);
        endChecksumEvent.setFileEntryIndex(fileEntryIndex);
        endChecksumEvent.setTotalNumBlocks(totalNumBlocks); // checksumArrays.length
        endChecksumEvent.setBlockSize(startAckEvent.getBlockSize());
        ret = CMEventManager.unicastEvent(endChecksumEvent, userName, m_cmInfo);
        if(!ret) return false;

        return true;
    }

    // called at the client
    private boolean processSTART_FILE_BLOCK_CHECKSUM(CMFileSyncEvent fse) {
        CMFileSyncEventStartFileBlockChecksum startChecksumEvent = (CMFileSyncEventStartFileBlockChecksum) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_BLOCK_CHECKSUM() called..");
            System.out.println("startChecksumEvent = " + startChecksumEvent);
        }

        int fileIndex = startChecksumEvent.getFileEntryIndex();
        int totalNumBlocks = startChecksumEvent.getTotalNumBlocks();
        int returnCode = 1;

        // get the file in the client file entry list
        CMFileSyncInfo fsInfo = m_cmInfo.getFileSyncInfo();
        Objects.requireNonNull(fsInfo);
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
        ackEvent.setSender(m_cmInfo.getInteractionInfo().getMyself().getName());
        ackEvent.setReceiver(startChecksumEvent.getSender());
        // set fields as they are in the received event
        ackEvent.setBlockSize(startChecksumEvent.getBlockSize());
        ackEvent.setFileEntryIndex(fileIndex);
        ackEvent.setTotalNumBlocks(totalNumBlocks);
        // set return code
        ackEvent.setReturnCode(returnCode);

        // send the ack event
        return CMEventManager.unicastEvent(ackEvent, startChecksumEvent.getSender(), m_cmInfo);
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
        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
        // get the client sync home
        Path clientSyncHome = syncManager.getClientSyncHome();
        // get the requester name
        String requesterName = fse_rnf.getRequesterName();  // server name
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

        CMFileSyncEventStartFileList fse_sfl = (CMFileSyncEventStartFileList) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_LIST() called..");
            System.out.println("event = " + fse_sfl);
        }

        String userName = fse_sfl.getUserName();
        // get the file-sync manager
        CMFileSyncManager fsManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
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
        CMFileSyncEventStartFileListAck ackFse = new CMFileSyncEventStartFileListAck();
        ackFse.setSender(fse_sfl.getReceiver());  // server name
        ackFse.setReceiver(userName);
        ackFse.setUserName(userName);
        ackFse.setNumTotalFiles(fse_sfl.getNumTotalFiles());
        ackFse.setReturnCode(1);    // always success

        // send the ack event to the client

        return CMEventManager.unicastEvent(ackFse, userName, m_cmInfo);
    }

    // called at the client
    private boolean processSTART_FILE_LIST_ACK(CMFileSyncEvent fse) {
        CMFileSyncEventStartFileListAck fse_sfla = (CMFileSyncEventStartFileListAck) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processSTART_FILE_LIST_ACK() called..");
            System.out.println("event = " + fse_sfla);
        }

        String server = fse_sfla.getSender();

        // create a FILE_ENTRIES event
        CMFileSyncEventFileEntries newfse = new CMFileSyncEventFileEntries();
        newfse.setID(CMFileSyncEvent.FILE_ENTRIES);
        newfse.setSender(fse_sfla.getReceiver());  // user name
        newfse.setReceiver(server);  // server name
        newfse.setUserName(fse_sfla.getUserName());    // user name
        newfse.setNumFilesCompleted(0); // initialized to 0
        // set numFiles and fileEntryList
        setNumFilesAndEntryList(newfse, 0);

        return CMEventManager.unicastEvent(newfse, server, m_cmInfo);
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
        List<Path> pathList = m_cmInfo.getFileSyncInfo().getPathList();
        List<Path> subList = new ArrayList<>();
        int index = startListIndex;
        int numFiles = 0;
        CMFileSyncManager fsManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
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
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
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
            newfse.setClientPathEntryList(fileEntryList);

        return newfse;
    }

    // called at the server
    private boolean processFILE_ENTRIES(CMFileSyncEvent fse) {
        CMFileSyncEventFileEntries fse_fe = (CMFileSyncEventFileEntries) fse;
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processFILE_ENTRIES() called..");
            System.out.println("event = " + fse_fe);
        }

        String userName = fse_fe.getUserName();
        int returnCode = 1;
        int numFilesCompleted = 0;
        int numFiles = fse_fe.getNumFiles();

        // if 0, the entry list is null in the event
        if(numFiles > 0) {
            // set or add the entry list of the event to the entry Map
            List<CMFileSyncEntry> entryList = m_cmInfo.getFileSyncInfo().getClientPathEntryListMap().get(userName);
            if (entryList == null) {
                // set the new entry list to the Map
                m_cmInfo.getFileSyncInfo().getClientPathEntryListMap().put(userName, fse_fe.getClientPathEntryList());
                // set the number of completed files
                numFilesCompleted = numFiles;
            } else {
                // add the new entry list to the existing list
                boolean addResult = entryList.addAll(fse_fe.getClientPathEntryList());
                if(!addResult) {
                    System.err.println("entry list add error!");
                    System.err.println("existing list = "+entryList);
                    System.err.println("new list = "+fse_fe.getClientPathEntryList());
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
        fseAck.setSender(fse_fe.getReceiver());  // server
        fseAck.setReceiver(fse_fe.getSender());  // client
        fseAck.setUserName(fse_fe.getUserName());
        fseAck.setNumFilesCompleted(numFilesCompleted);   // updated
        fseAck.setNumFiles(numFiles);
        fseAck.setReturnCode(returnCode);

        // send the ack event
        return CMEventManager.unicastEvent(fseAck, userName, m_cmInfo);
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
        int pathListSize = m_cmInfo.getFileSyncInfo().getPathList().size();
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

        // crate an END_FILE_LIST event
        CMFileSyncEventEndFileList newfse = new CMFileSyncEventEndFileList();
        newfse.setSender(fse.getReceiver());  // client
        String server = fse.getSender();
        newfse.setReceiver(server);  // server
        newfse.setUserName(fse.getUserName());
        newfse.setNumFilesCompleted(fse.getNumFilesCompleted());

        // send the event to the server
        return CMEventManager.unicastEvent(newfse, server, m_cmInfo);
    }

    // called at the client
    private boolean sendNextFileEntries(CMFileSyncEventFileEntriesAck fse) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.sendNextFileEntries() called..");
        }

        CMFileSyncInfo syncInfo = m_cmInfo.getFileSyncInfo();
        // create FILE_ENTRIES event
        CMFileSyncEventFileEntries newfse = new CMFileSyncEventFileEntries();
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
        CMFileSyncEventEndFileList fse_efl = (CMFileSyncEventEndFileList) fse;

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processEND_FILE_LIST() called..");
            System.out.println("fse = " + fse_efl);
        }

        int returnCode;

        // check the elements of file entry list
        String userName = fse_efl.getUserName();
        int numFilesCompleted = fse_efl.getNumFilesCompleted();
        List<CMFileSyncEntry> fileEntryList = m_cmInfo.getFileSyncInfo().getClientPathEntryListMap()
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
        CMFileSyncEventEndFileListAck fseAck = new CMFileSyncEventEndFileListAck();
        fseAck.setSender(fse_efl.getReceiver());  // server
        fseAck.setReceiver(fse_efl.getSender());  // client
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
        m_cmInfo.getFileSyncInfo().getSyncGeneratorMap().put(userName, fileSyncGenerator);

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
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        Map<Path, Boolean> isFileSyncCompletedMap = syncInfo.getIsFileSyncCompletedMap();
        Objects.requireNonNull(isFileSyncCompletedMap);
        isFileSyncCompletedMap.put(fse_cnf.getCompletedPath(), true);

        return true;
    }

    // called by the client
    private boolean processCOMPLETE_UPDATE_FILE(CMFileSyncEvent fse) {
        CMFileSyncEventCompleteUpdateFile fse_cuf = (CMFileSyncEventCompleteUpdateFile) fse;

        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncEventHandler.processCOMPLETE_UPDATE_FILE() called..");
            System.out.println("fse = " + fse_cuf);
        }
        // update info for the file-update completion at the client
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        Map<Path, Boolean> isFileSyncCompletedMap = syncInfo.getIsFileSyncCompletedMap();
        Objects.requireNonNull(isFileSyncCompletedMap);
        isFileSyncCompletedMap.put(fse_cuf.getCompletedPath(), true);

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
        CMFileSyncInfo syncInfo = Objects.requireNonNull(m_cmInfo.getFileSyncInfo());
        Map<Path, Boolean> isFileSyncCompletedMap = syncInfo.getIsFileSyncCompletedMap();
        Objects.requireNonNull(isFileSyncCompletedMap);
        isFileSyncCompletedMap.put(skipFileEvent.getSkippedPath(), true);

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
        CMFileSyncInfo syncInfo = m_cmInfo.getFileSyncInfo();
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
        CMFileSyncManager syncManager = m_cmInfo.getServiceManager(CMFileSyncManager.class);
        syncInfo.getIsFileSyncCompletedMap().clear();

        // change the file-sync state to stop
        syncInfo.setSyncInProgress(false);

        // check if the watch service has detected another change
        if(syncInfo.isFileChangeDetected() && !syncInfo.isWatchServiceTaskDone()) {
            syncInfo.setFileChangeDetected(false);
            syncManager.sync();
        }

        return true;
    }
}
