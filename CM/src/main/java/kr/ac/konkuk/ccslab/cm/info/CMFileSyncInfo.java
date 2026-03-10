package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncStateKey;
import kr.ac.konkuk.ccslab.cm.entity.CMUserLoginKey;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncMode;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;

import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

public class CMFileSyncInfo {
    private static CMFileSyncInfo instance;

    public static final String SYNC_HOME = "FileSyncHome";
    public static final int BLOCK_SIZE = 700;   // Bytes, from rsync
    public static final int MAX_BLOCK_SIZE = 1 << 17;   // 2^17 = 131,072 (from rsync version > 30)
    //public static final String ONLINE_MODE_LIST_FILE_NAME = "online_mode_list.txt";
    public static final String ONLINE_MODE_MAP_FILE = "online_mode_map.txt";

    private CMFileSyncMode currentMode;
    private boolean syncInProgress;
    private List<Path> pathList;        // 4 client
    private Map<Path, Boolean> isFileSyncCompletedMap;  // 4 client
    private Map<Integer, CMFileSyncBlockChecksum[]> blockChecksumMap;   // 4 client
    private Map<Integer, Map<Short, Integer>> fileIndexToHashToBlockIndexMap; // 4 client

    private Map<CMFileSyncStateKey, List<CMFileSyncEntry>> initiatorPathEntryListMap;    // 4 server
    private Map<CMUserLoginKey, CMFileSyncGenerator> syncGeneratorMap;      // 4 server

    private boolean fileChangeDetected;
    private WatchService watchService;
    private Future<?> watchServiceFuture;

    private ConcurrentLinkedQueue<Path> onlineModeRequestQueue;     // 4 client
    //private List<Path> onlineModePathList;      // 4 client
    private Map<Path,Long> onlineModePathSizeMap;  // 4 client
    //private Map<String, List<Path>> onlineModePathListMap;      // 4 server

    private ConcurrentLinkedQueue<Path> localModeRequestQueue;      // 4 client
    private Map<CMFileSyncStateKey, List<Path>> basisFileListMap;           // 4 server

    private ScheduledFuture<?> proactiveModeTaskFuture;

    // [NEW] 4 client
    private UUID m_deviceUuid;

    private CMFileSyncInfo() {

        currentMode = CMFileSyncMode.OFF;
        syncInProgress = false;
        pathList = null;
        isFileSyncCompletedMap = new Hashtable<>();
        blockChecksumMap = new Hashtable<>();
        fileIndexToHashToBlockIndexMap = new Hashtable<>();

        initiatorPathEntryListMap = new Hashtable<>();
        syncGeneratorMap = new Hashtable<>();

        fileChangeDetected = false;
        watchService = null;
        watchServiceFuture = null;

        onlineModeRequestQueue = new ConcurrentLinkedQueue<>();
        //onlineModePathList = new ArrayList<>();
        onlineModePathSizeMap = new HashMap<>();
        //onlineModePathListMap = new HashMap<>();

        localModeRequestQueue = new ConcurrentLinkedQueue<>();
        basisFileListMap = new HashMap<>();

        proactiveModeTaskFuture = null;
    }

    // getInstance()
    public static synchronized CMFileSyncInfo getInstance() {
        if(instance == null) {
            instance = new CMFileSyncInfo();
        }
        return instance;
    }

    ///////////// getter/setter
    public CMFileSyncMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(CMFileSyncMode currentMode) {
        this.currentMode = currentMode;
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

    public Map<CMFileSyncStateKey, List<CMFileSyncEntry>> getInitiatorPathEntryListMap() {
        return initiatorPathEntryListMap;
    }

    public Map<CMUserLoginKey, CMFileSyncGenerator> getSyncGeneratorMap() {
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

/*
    public List<Path> getOnlineModePathList() {
        return onlineModePathList;
    }

    public void setOnlineModePathList(List<Path> onlineModePathList) {
        this.onlineModePathList = onlineModePathList;
    }
*/

    public Map<Path, Long> getOnlineModePathSizeMap() {
        return onlineModePathSizeMap;
    }

    public void setOnlineModePathSizeMap(Map<Path, Long> onlineModePathSizeMap) {
        this.onlineModePathSizeMap = onlineModePathSizeMap;
    }

/*
    public Map<String, List<Path>> getOnlineModePathListMap() {
        return onlineModePathListMap;
    }
*/

    public ConcurrentLinkedQueue<Path> getLocalModeRequestQueue() {
        return localModeRequestQueue;
    }

    public Map<CMFileSyncStateKey, List<Path>> getBasisFileListMap() {
        return basisFileListMap;
    }

    public ScheduledFuture<?> getProactiveModeTaskFuture() {
        return proactiveModeTaskFuture;
    }

    public void setProactiveModeTaskFuture(ScheduledFuture<?> proactiveModeTaskFuture) {
        this.proactiveModeTaskFuture = proactiveModeTaskFuture;
    }

    public boolean isProactiveModeTaskDone() {
        if (proactiveModeTaskFuture == null) {
            return true;
        }
        return proactiveModeTaskFuture.isDone() || proactiveModeTaskFuture.isCancelled();
    }

    // [NEW] 4 client
    public synchronized UUID getDeviceUuid() {
        return m_deviceUuid;
    }

    // [NEW] 4 client
    public synchronized void setDeviceUuid(UUID deviceUuid) {
        this.m_deviceUuid = deviceUuid;
    }
}
