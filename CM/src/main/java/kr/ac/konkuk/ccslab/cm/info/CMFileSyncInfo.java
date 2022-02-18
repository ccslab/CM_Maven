package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;

import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

public class CMFileSyncInfo {

    public static final String SYNC_HOME = "FileSyncHome";
    public static final int BLOCK_SIZE = 700;   // Bytes, from rsync
    public static final int MAX_BLOCK_SIZE = 1 << 17;   // 2^17 = 131,072 (from rsync version > 30)
    public static final String ONLINE_MODE_LIST_FILE_NAME = "online_mode_list.txt";

    private boolean syncInProgress;
    private List<Path> pathList;        // 4 client
    private Map<Path, Boolean> isFileSyncCompletedMap;  // 4 client
    private Map<Integer, CMFileSyncBlockChecksum[]> blockChecksumMap;   // 4 client
    private Map<Integer, Map<Short, Integer>> fileIndexToHashToBlockIndexMap; // 4 client

    private Map<String, List<CMFileSyncEntry>> clientPathEntryListMap;    // 4 server
    private Map<String, CMFileSyncGenerator> syncGeneratorMap;      // 4 server

    private boolean fileChangeDetected;
    private WatchService watchService;
    private Future<?> watchServiceFuture;

    private ConcurrentLinkedQueue<Path> onlineModeRequestQueue;     // 4 client
    private List<Path> onlineModePathList;      // 4 client
    //private Map<String, List<Path>> onlineModePathListMap;      // 4 server

    private ConcurrentLinkedQueue<Path> localModeRequestQueue;      // 4 client
    private Map<String, List<Path>> basisFileListMap;           // 4 server

    public CMFileSyncInfo() {
        syncInProgress = false;
        pathList = null;
        isFileSyncCompletedMap = new Hashtable<>();
        blockChecksumMap = new Hashtable<>();
        fileIndexToHashToBlockIndexMap = new Hashtable<>();

        clientPathEntryListMap = new Hashtable<>();
        syncGeneratorMap = new Hashtable<>();

        fileChangeDetected = false;
        watchService = null;
        watchServiceFuture = null;

        onlineModeRequestQueue = new ConcurrentLinkedQueue<>();
        onlineModePathList = new ArrayList<>();
        //onlineModePathListMap = new HashMap<>();

        localModeRequestQueue = new ConcurrentLinkedQueue<>();
        basisFileListMap = new HashMap<>();
    }

    public boolean isSyncInProgress() {
        return syncInProgress;
    }

    public void setSyncInProgress(boolean syncInProgress) {
        this.syncInProgress = syncInProgress;
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncInfo.setSyncInProgress( "+syncInProgress+" ) called ..");
        }
    }

    public Map<Integer, CMFileSyncBlockChecksum[]> getBlockChecksumMap() {
        return blockChecksumMap;
    }

    public Map<Integer, Map<Short, Integer>> getFileIndexToHashToBlockIndexMap() {
        return fileIndexToHashToBlockIndexMap;
    }

    public List<Path> getPathList() {
        return pathList;
    }

    public void setPathList(List<Path> pathList) {
        this.pathList = pathList;
    }

    public Map<Path, Boolean> getIsFileSyncCompletedMap() {
        return isFileSyncCompletedMap;
    }

    public Map<String, List<CMFileSyncEntry>> getClientPathEntryListMap() {
        return clientPathEntryListMap;
    }

    public Map<String, CMFileSyncGenerator> getSyncGeneratorMap() {
        return syncGeneratorMap;
    }

    public boolean isFileChangeDetected() {
        return fileChangeDetected;
    }

    public void setFileChangeDetected(boolean fileChangeDetected) {
        this.fileChangeDetected = fileChangeDetected;
    }

    public WatchService getWatchService() {
        return watchService;
    }

    public void setWatchService(WatchService watchService) {
        this.watchService = watchService;
    }

    public Future<?> getWatchServiceFuture() {
        return watchServiceFuture;
    }

    public void setWatchServiceFuture(Future<?> watchServiceFuture) {
        this.watchServiceFuture = watchServiceFuture;
    }

    public boolean isWatchServiceTaskDone() {
        if(watchServiceFuture == null) return true;
        return watchServiceFuture.isDone() || watchServiceFuture.isCancelled();
    }

    public ConcurrentLinkedQueue<Path> getOnlineModeRequestQueue() {
        return onlineModeRequestQueue;
    }

    public List<Path> getOnlineModePathList() {
        return onlineModePathList;
    }

/*
    public Map<String, List<Path>> getOnlineModePathListMap() {
        return onlineModePathListMap;
    }
*/

    public ConcurrentLinkedQueue<Path> getLocalModeRequestQueue() {
        return localModeRequestQueue;
    }

    public Map<String, List<Path>> getBasisFileListMap() {
        return basisFileListMap;
    }
}
