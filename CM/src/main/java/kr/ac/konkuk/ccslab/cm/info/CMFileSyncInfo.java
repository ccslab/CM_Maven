package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncChangeLogEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncClientEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncIndexRegistry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncIndexRepository;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncIndexSnapshotStore;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncJacksonSnapshotStore;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncPullModifyState;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncPushLease;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncPushModifyState;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncStateKey;
import kr.ac.konkuk.ccslab.cm.entity.CMUserLoginKey;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncMode;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncProgress;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncPullGenerator;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncPushGenerator;
import kr.ac.konkuk.ccslab.cm.util.CMUUIDConverter;
import kr.ac.konkuk.ccslab.cm.util.CMUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private CMFileSyncProgress syncProgress;
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

    // [NEW] 4 server: in-memory index registry
    private CMFileSyncIndexRegistry indexRegistry;

    // [NEW 10-3] 4 server: per-user 전역 changelog head (2단계 cursor 모델의 전역 레벨).
    // 값 = 사용자 공유 changelog의 max changeId. (a) changeId allocator, (b) pull 비교 기준값 겸함.
    // key = initiatorName. 영속화 경로: .cm-settings/file-sync/server/<name>/changelogHead
    // (per-device cursor와 같은 계층, deviceUuid 없음). warm-up = changelog max(2.2).
    private final Map<String, Long> changelogHeadMap = new ConcurrentHashMap<>();

    // [NEW 10-3] 4 server: per-user push 세션 소유권(lease) 보관 맵(§2.6.1). key = initiatorName.
    // 한 사용자의 push 세션을 하나로 직렬화 — 다른 디바이스 push 는 tryAcquire 실패로 busy 거절된다.
    private final Map<String, CMFileSyncPushLease> userPushLeases = new ConcurrentHashMap<>();

    // [NEW] 4 client: 파일 동기화 커서 (서버 ChangeLog 상의 마지막 처리 위치, -1 = 미동기화)
    private long m_lCursor;
    // [NEW] 4 client: 클라이언트 베이스 스냅샷 path -> lastSyncedMtime (sec)
    private final Map<String, Long> m_lastSyncedMtimeMap = new ConcurrentHashMap<>();
    // [NEW] 4 client: 클라이언트 베이스 스냅샷 path -> lastSyncedSize (bytes).
    // pull 로 받은 파일이 WatchService 의 self-event 로 다시 push 되는 것을 막기 위한 필터에 사용.
    private final Map<String, Long> m_lastSyncedSizeMap = new ConcurrentHashMap<>();
    // [NEW] 4 client: pull-delete 직전 등록된 path -> 등록 시각(ms).
    // WatchService 의 self-DELETE 이벤트를 식별/drop 하는 데 사용 (영속화 불필요).
    private final Map<String, Long> m_pendingPullDeletePaths = new ConcurrentHashMap<>();

    // [NEW] 4 server: pull sync를 위해 서버가 클라이언트로 보내기로 결정한 server entry list
    private Map<CMFileSyncStateKey, List<CMFileSyncChangeLogEntry>> serverEntryMap;
    // [NEW] 4 server: pull sync의 완료 여부 상태를 확인하기 위한 map
    private Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pullStateTable;
    // [NEW] 4 server: stateKey별 pull sync MODIFY 진행 상태 holder
    private Map<CMFileSyncStateKey, CMFileSyncPullModifyState> pullModifyStateMap;

    // [NEW] 4 client: 서버로부터 받은 server entry list (수신 이벤트의 serverEntryList 참조)
    private List<CMFileSyncChangeLogEntry> serverEntryList;
    // [NEW] 4 client: server entry list와 비교할 클라이언트측 동기화 디렉토리 파일 리스트
    private List<Path> clientPathList;
    // [NEW] 4 client: pull sync 완료 후 클라이언트가 맞춰야 할 서버측 cursor 값
    private long serverCursor;
    // [NEW] 4 client: pull sync에서 삭제 대상인 client entry (relative path 기준)
    private Map<String, CMFileSyncClientEntry> pullDeleteMap;
    // [NEW] 4 client: pull sync에서 신규 추가 대상인 client entry (relative path 기준)
    private Map<String, CMFileSyncClientEntry> pullCreateMap;
    // [NEW] 4 client: pull sync에서 업데이트 대상인 client entry (relative path 기준)
    private Map<String, CMFileSyncClientEntry> pullModifyMap;
    // [NEW] 4 client: pull sync에서 server entry와 비교 결과 push 대상인 client entry (relative path 기준)
    private Map<String, CMFileSyncClientEntry> pendingPushMap;
    // [NEW] 4 client: pull sync MODIFY용 block-checksum generator 스레드
    private CMFileSyncPullGenerator pullGenerator;

    // [NEW] 4 client: incremental PUSH 송신용 entry 스냅샷 (pendingPushMap 또는 watch service 후보 맵에서 생성).
    // PULL의 serverEntryList(클라가 수신)와 대칭으로, 클라가 서버로 보낼 entry list.
    private List<CMFileSyncClientEntry> pushEntryList;
    // [NEW] 4 server: push sync의 완료 여부 상태를 확인하기 위한 map (PULL의 pullStateTable과 동일 구조).
    // 서버가 PUSH_ENTRIES 수신 시 stateKey별로 누적 저장, op별 처리 완료 시 isCompleted=true,
    // 모든 entry 완료 시 COMPLETE_PUSH_SYNC 송신 트리거.
    private Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pushStateTable;

    // [NEW] 4 server: incremental PUSH MODIFY 전용. 동시 진행되는 클라이언트별 PushGenerator 보관.
    // key: CMUserLoginKey(initiatorName, initiatorUuid) — full-sync syncGeneratorMap과 동일 키 정책.
    // pushStateTable(stateKey-keyed)이 op 완료 truth, 본 Map이 MODIFY 진행 자료(채널·체크섬 배열 등)를 담당.
    private Map<CMUserLoginKey, CMFileSyncPushGenerator> pushGeneratorMap;
    // [NEW] 4 server: PUSH 세션 동안 op record 누적용 (옵션 A, 10-2 doc 11716~11723).
    // lifecycle: processSTART_PUSH_ENTRY_LIST 빈 리스트 마련 → 각 op 완료 분기 record add
    //            → completePushSync 일괄 appendChangelogBatch → COMPLETE_PUSH_SYNC_ACK 시점 remove.
    private Map<CMFileSyncStateKey, List<CMFileSyncChangeLogEntry>> pushOpRecordTable;
    // [NEW] 4 server: pushStateTable의 역방향 인덱스 (10-2 doc 10853~10888).
    // (initiatorName, loginUuid) → CMFileSyncStateKey(initiatorName, initiatorDeviceUuid).
    // checkCompletePushCreate가 파일 송신자의 loginKey만 알고 있을 때 O(1)로 stateKey 조회용.
    // pushStateTable과 lifecycle 동기: START에서 put, COMPLETE_PUSH_SYNC_ACK에서 remove.
    private Map<CMUserLoginKey, CMFileSyncStateKey> pushLoginKeyToStateKeyMap;
    // [NEW] 4 client: incremental PUSH MODIFY용 source-side holder (CMFileSyncPullModifyState의 거울 짝).
    // 클라이언트는 동시 PUSH 세션 1개 전제이므로 단일 필드. 첫 START_FILE_BLOCK_CHECKSUM 수신 시 lazy 생성.
    private CMFileSyncPushModifyState pushModifyState;

    // [NEW] 동기화 제외 대상 glob 패턴 (OS 자동 생성 파일, 임시 파일, conflict 보존 파일 등).
    // 향후 cm-client.conf / cm-server.conf 에서 추가 패턴을 머지하도록 확장 예정.
    // 주의: "-conflict-*" 는 의도적으로 제외. conflict-rename 파일은 사용자 데이터 보존용
    // 백업이므로 다음 push sync 때 서버로 전송되어 다른 디바이스에서도 확인 가능해야 함.
    // .DS_Store 같은 OS 자동 재생성 파일은 ignore 로 충돌 자체를 차단하므로 무한 루프 위험 없음.
    private static final List<String> DEFAULT_IGNORED_GLOBS = List.of(
            ".DS_Store", "._*", ".Spotlight-V100", ".Trashes", ".fseventsd",
            "Thumbs.db", "desktop.ini",
            "*.tmp", "*.swp"
    );
    private final List<PathMatcher> ignoreMatchers = DEFAULT_IGNORED_GLOBS.stream()
            .map(g -> FileSystems.getDefault().getPathMatcher("glob:" + g))
            .toList();

    private CMFileSyncInfo() {

        currentMode = CMFileSyncMode.OFF;
        syncProgress = CMFileSyncProgress.NONE;
        m_lCursor = -1;
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

        // [NEW] pull sync 관련 필드 초기화
        serverEntryMap = new HashMap<>();   // 4 server
        pullStateTable = new HashMap<>();   // 4 server
        pullModifyStateMap = new Hashtable<>(); // 4 server
        serverEntryList = null;             // 4 client
        clientPathList = null;              // 4 client
        serverCursor = -1;                  // 4 client
        pullDeleteMap = new HashMap<>();    // 4 client
        pullCreateMap = new HashMap<>();    // 4 client
        pullModifyMap = new HashMap<>();    // 4 client
        pendingPushMap = new HashMap<>();   // 4 client
        pullGenerator = null;              // 4 client

        // [NEW] push sync 관련 필드 초기화
        pushEntryList = null;                 // 4 client (push 세션 시작 시 스냅샷 생성)
        pushStateTable = new HashMap<>();     // 4 server
        pushGeneratorMap = new Hashtable<>(); // 4 server
        pushOpRecordTable = new HashMap<>();  // 4 server
        pushLoginKeyToStateKeyMap = new Hashtable<>(); // 4 server
        pushModifyState = null;               // 4 client (lazy 생성)

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if (confInfo.getSystemType().equals("SERVER")) {
            CMFileSyncIndexSnapshotStore store = new CMFileSyncJacksonSnapshotStore();
            Path indexBaseDir = Paths.get(CMInfo.SETTINGS_DIR, "file-sync", "server");
            indexRegistry = new CMFileSyncIndexRegistry(store, indexBaseDir);
        } else {
            indexRegistry = null;
        }
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

    public CMFileSyncProgress getSyncProgress() {
        return syncProgress;
    }

    public void setSyncProgress(CMFileSyncProgress syncProgress) {
        this.syncProgress = syncProgress;
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncInfo.setSyncProgress( "+syncProgress+" ) called ..");
        }
    }

    // 하위 호환: 동기화 진행 여부 (NONE이 아니면 진행 중)
    public boolean isSyncInProgress() {
        return syncProgress != CMFileSyncProgress.NONE;
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

    // [NEW] 4 server
    public CMFileSyncIndexRegistry getIndexRegistry() {
        return indexRegistry;
    }

    public void setIndexRegistry(CMFileSyncIndexRegistry indexRegistry) {
        this.indexRegistry = indexRegistry;
    }

    // [NEW] 4 client: cursor getter/setter
    public long getCursor() {
        return m_lCursor;
    }

    public void setCursor(final long lCursor) {
        this.m_lCursor = lCursor;
    }

    // [NEW] 4 server: serverEntryMap getter
    public Map<CMFileSyncStateKey, List<CMFileSyncChangeLogEntry>> getServerEntryMap() {
        return serverEntryMap;
    }

    // [NEW] 4 server: pullModifyStateMap getter
    public Map<CMFileSyncStateKey, CMFileSyncPullModifyState> getPullModifyStateMap() {
        return pullModifyStateMap;
    }

    // [NEW] 4 server: pullStateTable getter
    public Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> getPullStateTable() {
        return pullStateTable;
    }

    // [NEW] 4 client: serverEntryList getter/setter
    public List<CMFileSyncChangeLogEntry> getServerEntryList() {
        return serverEntryList;
    }

    public void setServerEntryList(List<CMFileSyncChangeLogEntry> serverEntryList) {
        this.serverEntryList = serverEntryList;
    }

    // [NEW] 4 client: clientPathList getter/setter
    public List<Path> getClientPathList() {
        return clientPathList;
    }

    public void setClientPathList(List<Path> clientPathList) {
        this.clientPathList = clientPathList;
    }

    // [NEW] 4 client: serverCursor getter/setter
    public long getServerCursor() {
        return serverCursor;
    }

    public void setServerCursor(long serverCursor) {
        this.serverCursor = serverCursor;
    }

    // [NEW] 4 client: pullDeleteMap getter
    public Map<String, CMFileSyncClientEntry> getPullDeleteMap() {
        return pullDeleteMap;
    }

    // [NEW] 4 client: pullCreateMap getter
    public Map<String, CMFileSyncClientEntry> getPullCreateMap() {
        return pullCreateMap;
    }

    // [NEW] 4 client: pullModifyMap getter
    public Map<String, CMFileSyncClientEntry> getPullModifyMap() {
        return pullModifyMap;
    }

    // [NEW] 4 client: pendingPushMap getter
    public Map<String, CMFileSyncClientEntry> getPendingPushMap() {
        return pendingPushMap;
    }

    // [NEW] 4 client: pullGenerator getter/setter
    public CMFileSyncPullGenerator getPullGenerator() {
        return pullGenerator;
    }

    public void setPullGenerator(CMFileSyncPullGenerator pullGenerator) {
        this.pullGenerator = pullGenerator;
    }

    // [NEW] 4 client: pushEntryList getter/setter (push 세션 시작 시 스냅샷 설정)
    public List<CMFileSyncClientEntry> getPushEntryList() {
        return pushEntryList;
    }

    public void setPushEntryList(List<CMFileSyncClientEntry> pushEntryList) {
        this.pushEntryList = pushEntryList;
    }

    // [NEW] 4 server: pushStateTable getter (no setter — pullStateTable과 동일 정책)
    public Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> getPushStateTable() {
        return pushStateTable;
    }

    // [NEW] 4 server: pushGeneratorMap getter (no setter — Map 객체 자체 교체 없음)
    public Map<CMUserLoginKey, CMFileSyncPushGenerator> getPushGeneratorMap() {
        return pushGeneratorMap;
    }

    // [NEW] 4 server: pushOpRecordTable getter (no setter — Map 객체 자체 교체 없음)
    public Map<CMFileSyncStateKey, List<CMFileSyncChangeLogEntry>> getPushOpRecordTable() {
        return pushOpRecordTable;
    }

    // [NEW] 4 server: pushLoginKeyToStateKeyMap getter (no setter — Map 객체 자체 교체 없음)
    public Map<CMUserLoginKey, CMFileSyncStateKey> getPushLoginKeyToStateKeyMap() {
        return pushLoginKeyToStateKeyMap;
    }

    // [NEW] 4 client: pushModifyState getter/setter (정리 시점에 null 설정 필요하여 setter 보유)
    public CMFileSyncPushModifyState getPushModifyState() {
        return pushModifyState;
    }

    public void setPushModifyState(CMFileSyncPushModifyState pushModifyState) {
        this.pushModifyState = pushModifyState;
    }

    // [NEW] 4 client: lastSyncedMtimeMap getter / convenience methods
    public Map<String, Long> getLastSyncedMtimeMap() {
        return m_lastSyncedMtimeMap;
    }

    public long getLastSyncedMtime(String relPath) {
        Long mtime = m_lastSyncedMtimeMap.get(relPath);
        long lastSyncedMtime;
        if (mtime == null) lastSyncedMtime = -1;
        else lastSyncedMtime = mtime;
        return lastSyncedMtime;
    }

    public void setLastSyncedMtime(String relPath, long mtimeSec) {
        m_lastSyncedMtimeMap.put(relPath, mtimeSec);
    }

    public void removeLastSyncedMtime(String relPath) {
        m_lastSyncedMtimeMap.remove(relPath);
    }

    // [NEW] 4 client: lastSyncedSizeMap getter / convenience methods
    public Map<String, Long> getLastSyncedSizeMap() {
        return m_lastSyncedSizeMap;
    }

    public long getLastSyncedSize(String relPath) {
        Long size = m_lastSyncedSizeMap.get(relPath);
        return size == null ? -1L : size;
    }

    public void setLastSyncedSize(String relPath, long sizeBytes) {
        m_lastSyncedSizeMap.put(relPath, sizeBytes);
    }

    public void removeLastSyncedSize(String relPath) {
        m_lastSyncedSizeMap.remove(relPath);
    }

    /**
     * mtime + size 를 한 번에 저장 (self-event 필터용 짝).
     */
    public void setLastSynced(String relPath, long mtimeSec, long sizeBytes) {
        m_lastSyncedMtimeMap.put(relPath, mtimeSec);
        m_lastSyncedSizeMap.put(relPath, sizeBytes);
    }

    // ---- pendingPullDeletePaths: WatchService self-DELETE 필터 ----

    /**
     * pull-delete 로 곧 삭제될 path 를 등록한다 (반드시 Files.delete() 직전에 호출).
     */
    public void addPendingPullDelete(String relPath) {
        m_pendingPullDeletePaths.put(relPath, System.currentTimeMillis());
    }

    /**
     * WatchService 가 가져온 DELETE 이벤트의 path 가 pull-delete 등록 분이었는지 확인하고 제거.
     * @return true 면 self-delete (필터 drop 대상)
     */
    public boolean consumePendingPullDelete(String relPath) {
        return m_pendingPullDeletePaths.remove(relPath) != null;
    }

    /**
     * 등록은 됐지만 ttlMs 가 지나도록 이벤트가 안 들어온 항목 정리 (leak 방지).
     */
    public void sweepStalePendingPullDeletes(long ttlMs) {
        long cutoff = System.currentTimeMillis() - ttlMs;
        m_pendingPullDeletePaths.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    // ---- ignore patterns: 동기화 대상에서 제외할 경로 매처 ----

    /**
     * 상대경로(또는 단일 segment) 가 ignore 패턴에 매칭되는지 검사한다.
     * 경로의 모든 segment (각 디렉토리 컴포넌트 + 파일명) 에 대해 패턴 매칭을 시도하여,
     * 어느 하나라도 매칭되면 무시 대상으로 간주.
     */
    public boolean isIgnored(Path relPath) {
        if (relPath == null) return false;
        int n = relPath.getNameCount();
        if (n == 0) {
            // 단일 segment 로 들어온 경우 (Path.of("foo")) 자체를 매칭
            return matchesAnyIgnore(relPath);
        }
        for (int i = 0; i < n; i++) {
            if (matchesAnyIgnore(relPath.getName(i))) return true;
        }
        return false;
    }

    private boolean matchesAnyIgnore(Path segment) {
        for (PathMatcher m : ignoreMatchers) {
            if (m.matches(segment)) return true;
        }
        return false;
    }

    /**
     * 주어진 절대경로의 마지막 수정 시간을 초 단위로 반환합니다.
     * 파일이 없으면 -1을 리턴합니다.
     */
    public long currentMtimeSecOrMinusOne(Path abs) throws IOException {
        if (!Files.exists(abs)) return -1L;
        return Files.getLastModifiedTime(abs).toMillis() / 1000;
    }

    /**
     * 주어진 절대경로의 파일 크기(bytes)를 반환합니다. 파일이 없으면 -1.
     */
    public long currentSizeOrMinusOne(Path abs) throws IOException {
        if (!Files.exists(abs)) return -1L;
        return Files.size(abs);
    }

    // --------------------------------------------------------------------
    // [NEW] 4 client: 파일 경로 도우미
    //   <project_home>/.cm-settings/file-sync/client/{cursor,client-index.json}
    // --------------------------------------------------------------------
    private Path getClientSyncBaseDir(final String projectHome) {
        return Path.of(Objects.requireNonNull(projectHome, "projectHome must not be null"),
                ".cm-settings", "file-sync", "client");
    }

    public Path getCursorFile(final String projectHome) {
        return getClientSyncBaseDir(projectHome).resolve("cursor");
    }

    private Path getClientIndexFile(final String projectHome) {
        return getClientSyncBaseDir(projectHome).resolve("client-index.json");
    }

    // --------------------------------------------------------------------
    // [NEW] 4 client: 저장 / 로드 API
    // --------------------------------------------------------------------

    /**
     * 클라이언트 커서를 파일로 저장합니다.
     * 경로가 없으면 생성합니다.
     *
     * @param projectHome 프로젝트 홈 디렉토리 (예: 앱이 인식하는 루트)
     */
    public void saveClientCursor(final String projectHome) {
        final Path baseDir = getClientSyncBaseDir(projectHome);

        try {
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }

            // cursor 저장 (음수이면 빈 문자열)
            final Path cursorFile = getCursorFile(projectHome);
            final String cursorStr = String.valueOf(m_lCursor);
            Files.writeString(cursorFile, cursorStr);

        } catch (IOException e) {
            System.err.println("CMFileSyncInfo.saveClientCursor(), failed to save: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 클라이언트 커서를 파일에서 읽어옵니다.
     * 파일이 없으면 해당 필드는 변경하지 않습니다.
     *
     * @param projectHome 프로젝트 홈 디렉토리
     */
    public void loadClientCursor(final String projectHome) {
        try {
            // cursor 읽기
            final Path cursorFile = getCursorFile(projectHome);
            if (Files.exists(cursorFile)) {
                final String cursorStr = Files.readString(cursorFile).trim();
                m_lCursor = cursorStr.isEmpty() ? -1 : Long.parseLong(cursorStr);
            }
        } catch (IOException e) {
            System.err.println("CMFileSyncInfo.loadClientCursor(), failed to load: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // DTO는 내부 static class로 간단히
    static class ClientIndexDto {
        public long baseCursor;
        public String generatedAt;
        public Map<String, Long> files;       // relPath -> mtimeSec
        public Map<String, Long> fileSizes;   // relPath -> sizeBytes (nullable for backward compat)
    }

    /**
     * 클라이언트 index 파일에서 읽어옵니다.
     * 파일이 없으면 아무 것도 하지 않습니다.
     *
     * @param projectHome 프로젝트 홈 디렉토리
     */
    public void loadClientIndex(String projectHome) {
        Path file = getClientIndexFile(projectHome);
        if (!Files.exists(file)) return;

        try {
            ObjectMapper om = new ObjectMapper();
            ClientIndexDto dto = om.readValue(file.toFile(), ClientIndexDto.class);
            m_lastSyncedMtimeMap.clear();
            m_lastSyncedSizeMap.clear();
            if (dto.files != null) {
                m_lastSyncedMtimeMap.putAll(dto.files);
            }
            if (dto.fileSizes != null) {
                m_lastSyncedSizeMap.putAll(dto.fileSizes);
            }
        } catch (IOException e) {
            // 로그만 찍고 무시 (손상 시 재생성)
            System.err.println("CMFileSyncInfo.loadClientIndex(), failed to load: " + e.getMessage());
        }
    }

    /**
     * 동기화 시작 시 디스크의 클라이언트 메타(cursor, client-index)를 권위값으로 재적용한다.
     * loadClientCursor/loadClientIndex 는 파일이 없으면 in-memory 를 보존하므로, 먼저 fresh 로 리셋한 뒤
     * 로드한다. → 클라 실행 중 메타 파일을 삭제하고 재동기화할 때 메모리의 옛 cursor 때문에
     * "이미 동기화됨"으로 오인하던 문제 방지(재시작 없이 반영).
     *
     * @param projectHome 프로젝트 홈 디렉토리
     */
    public void reloadClientMetaFromDisk(final String projectHome) {
        // fresh 로 리셋 (파일이 삭제된 경우 stale in-memory 값을 버린다)
        m_lCursor = -1;
        m_lastSyncedMtimeMap.clear();
        m_lastSyncedSizeMap.clear();
        // 디스크에 파일이 있으면 복원
        loadClientCursor(projectHome);
        loadClientIndex(projectHome);
    }

    /**
     * 클라이언트 index 파일로 저장합니다.
     *
     * @param projectHome 프로젝트 홈 디렉토리
     * @param baseCursor  현재 기준 커서 값
     */
    public void saveClientIndex(String projectHome, long baseCursor) {
        Path file = getClientIndexFile(projectHome);
        try {
            Files.createDirectories(file.getParent());
            ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            ClientIndexDto dto = new ClientIndexDto();
            dto.baseCursor = baseCursor;
            dto.generatedAt = OffsetDateTime.now().toString();
            dto.files = new HashMap<>(m_lastSyncedMtimeMap);
            dto.fileSizes = new HashMap<>(m_lastSyncedSizeMap);
            om.writeValue(file.toFile(), dto);
        } catch (IOException e) {
            // 로그만
            System.err.println("CMFileSyncInfo.saveClientIndex(), failed to save: " + e.getMessage());
        }
    }

    /**
     * 파일 추가(CREATE) op 완료: Path로부터 hash/mtime/size를 직접 구해 인덱스·메타 파일 업데이트
     */
    public void applyCreate(String initiatorName, UUID initiatorDeviceUuid, Path path) throws IOException {
        CMFileSyncIndexRepository repo = getIndexRegistry().getOrLoad(initiatorName, initiatorDeviceUuid);
        long newChangeId = allocateNextChangeId(initiatorName);   // [10-3] 전역 할당 (dual-writer (ii): full-push)

        CMFileSyncManager syncManager = CMInfo.getInstance()
                .getServiceManager(CMFileSyncManager.class);

        Path syncHome = syncManager.getServerSyncHome(initiatorName);

        // 경로 정규화: abs(절대) / relPath(상대; 메타 기록용)
        Path abs, relPath;
        if (path.isAbsolute()) {
            abs = path.toAbsolutePath().normalize();
            relPath = syncHome.relativize(abs).normalize();
        } else {
            relPath = path.normalize();
            abs = syncHome.resolve(relPath).toAbsolutePath().normalize();
        }
        String pathStr = relPath.toString().replace('\\', '/');

        boolean isDirectory = Files.isDirectory(abs);
        String md5Hex = CMUtil.md5Hex(abs);
        long mtimeSec = Files.getLastModifiedTime(abs).toMillis() / 1000;
        long sizeBytes = Files.size(abs);

        repo.applyCreateOrModify(pathStr, isDirectory, md5Hex, mtimeSec, sizeBytes, newChangeId);
        writeCursor(initiatorName, initiatorDeviceUuid, newChangeId);
        appendChangelog(initiatorName, initiatorDeviceUuid, "CREATE", pathStr, isDirectory, md5Hex, mtimeSec, sizeBytes, newChangeId);
        repo.flushSnapshot();
    }

    /**
     * 파일 추가(CREATE) op 완료: 클라이언트로부터 받은 메타 정보로 인덱스·메타 파일 업데이트
     */
    public void applyCreateFast(String initiatorName, UUID initiatorDeviceUuid,
                                Path path, boolean isDirectory, String contentHash,
                                long mtimeEpochSec, long sizeBytes) throws IOException {
        CMFileSyncIndexRepository repo = indexRegistry.getOrLoad(initiatorName, initiatorDeviceUuid);
        long newChangeId = allocateNextChangeId(initiatorName);   // [10-3] 전역 할당 (dual-writer (ii): full-push)
        String pathStr = path.toString().replace('\\', '/');

        repo.applyCreateOrModify(pathStr, isDirectory, contentHash, mtimeEpochSec, sizeBytes, newChangeId);
        writeCursor(initiatorName, initiatorDeviceUuid, newChangeId);
        appendChangelog(initiatorName, initiatorDeviceUuid, "CREATE", pathStr, isDirectory, contentHash, mtimeEpochSec, sizeBytes, newChangeId);
        repo.flushSnapshot();
    }

    /**
     * 파일 수정(MODIFY) op 완료: Path로부터 hash/mtime/size를 직접 구해 인덱스·메타 파일 업데이트
     */
    public void applyModify(String initiatorName, UUID initiatorDeviceUuid, Path path) throws IOException {
        CMFileSyncIndexRepository repo = getIndexRegistry().getOrLoad(initiatorName, initiatorDeviceUuid);
        long newChangeId = allocateNextChangeId(initiatorName);   // [10-3] 전역 할당 (dual-writer (ii): full-push)

        CMFileSyncManager syncManager = CMInfo.getInstance()
                .getServiceManager(CMFileSyncManager.class);

        Path syncHome = syncManager.getServerSyncHome(initiatorName);

        // 경로 정규화: abs(절대) / relPath(상대; 메타 기록용)
        Path abs, relPath;
        if (path.isAbsolute()) {
            abs = path.toAbsolutePath().normalize();
            relPath = syncHome.relativize(abs).normalize();
        } else {
            relPath = path.normalize();
            abs = syncHome.resolve(relPath).toAbsolutePath().normalize();
        }
        String pathStr = relPath.toString().replace('\\', '/');

        boolean isDirectory = Files.isDirectory(abs);
        String md5Hex = CMUtil.md5Hex(abs);
        long mtimeSec = Files.getLastModifiedTime(abs).toMillis() / 1000;
        long sizeBytes = Files.size(abs);

        repo.applyCreateOrModify(pathStr, isDirectory, md5Hex, mtimeSec, sizeBytes, newChangeId);
        writeCursor(initiatorName, initiatorDeviceUuid, newChangeId);
        appendChangelog(initiatorName, initiatorDeviceUuid, "MODIFY", pathStr, isDirectory, md5Hex, mtimeSec, sizeBytes, newChangeId);
        repo.flushSnapshot();
    }

    /**
     * 파일 수정(MODIFY) op 완료: 클라이언트로부터 받은 메타 정보로 인덱스·메타 파일 업데이트
     */
    public void applyModifyFast(String initiatorName, UUID initiatorDeviceUuid,
                                Path path, boolean isDirectory, String contentHash,
                                long mtimeEpochSec, long sizeBytes) throws IOException {
        CMFileSyncIndexRepository repo = indexRegistry.getOrLoad(initiatorName, initiatorDeviceUuid);
        long newChangeId = allocateNextChangeId(initiatorName);   // [10-3] 전역 할당 (dual-writer (ii): full-push)
        String pathStr = path.toString().replace('\\', '/');

        repo.applyCreateOrModify(pathStr, isDirectory, contentHash, mtimeEpochSec, sizeBytes, newChangeId);
        writeCursor(initiatorName, initiatorDeviceUuid, newChangeId);
        appendChangelog(initiatorName, initiatorDeviceUuid, "MODIFY", pathStr, isDirectory, contentHash, mtimeEpochSec, sizeBytes, newChangeId);
        repo.flushSnapshot();
    }

    /**
     * 파일 삭제(DELETE) op 완료: 인덱스·메타 파일 업데이트
     */
    public void applyDelete(String initiatorName, UUID initiatorDeviceUuid, Path path) throws IOException {
        CMFileSyncIndexRepository repo = getIndexRegistry().getOrLoad(initiatorName, initiatorDeviceUuid);
        long newChangeId = allocateNextChangeId(initiatorName);   // [10-3] 전역 할당 (dual-writer (ii): full-push)

        CMFileSyncManager syncManager = CMInfo.getInstance()
                .getServiceManager(CMFileSyncManager.class);

        Path syncHome = syncManager.getServerSyncHome(initiatorName);

        // 경로 정규화: abs(절대) / relPath(상대; 메타 기록용)
        Path abs, relPath;
        if (path.isAbsolute()) {
            abs = path.toAbsolutePath().normalize();
            relPath = syncHome.relativize(abs).normalize();
        } else {
            relPath = path.normalize();
            abs = syncHome.resolve(relPath).toAbsolutePath().normalize();
        }
        String pathStr = relPath.toString().replace('\\', '/');

        boolean isDirectory = Files.isDirectory(abs);
        long nowSec = System.currentTimeMillis() / 1000;

        repo.applyDelete(pathStr, isDirectory, newChangeId, nowSec);
        writeCursor(initiatorName, initiatorDeviceUuid, newChangeId);
        appendChangelog(initiatorName, initiatorDeviceUuid, "DELETE", pathStr, isDirectory, null, nowSec, 0L, newChangeId);
        repo.flushSnapshot();
    }

    // PUSH 트랜잭션 commit(completePushSync)에서 외부 호출 가능하도록 public.
    // 기존 op별 applyCreate/Modify/Delete 내부 호출도 그대로 동작.
    public void writeCursor(String initiatorName, UUID initiatorDeviceUuid, long changeId) throws IOException {
        Path cursorFile = Path.of(".cm-settings", "file-sync", "server",
                initiatorName, CMUUIDConverter.uuidToString(initiatorDeviceUuid), "cursor");
        Files.createDirectories(cursorFile.getParent());
        Files.writeString(cursorFile, Long.toString(changeId),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // [NEW] 4 server: changelog 디렉토리 경로 (initiatorName 기준, 모든 디바이스 공용)
    private Path getServerChangelogDir(String initiatorName) {
        return Path.of(".cm-settings", "file-sync", "server", initiatorName);
    }

    // [NEW 10-3] 4 server: per-user 전역 changelog head 영속화 파일 (device cursor와 같은 계층, deviceUuid 없음).
    private Path getServerChangelogHeadFile(String initiatorName) {
        return getServerChangelogDir(initiatorName).resolve("changelogHead");
    }

    /**
     * [NEW 10-3] per-user 전역 changelog head를 반환합니다(2.2).
     * 값의 의미 = 사용자 공유 changelog의 max changeId. pull 비교의 기준값이자 changeId allocator 기준.
     * 첫 접근 시 메모리 캐시가 없으면 디스크(changelogHead 파일)에서 warm-up하며, 파일이 없으면
     * changelog 전체의 max changeId를 하한으로 삼는다(per-device cursor의 max가 아님).
     * 이력이 전혀 없으면 0.
     */
    public synchronized long getChangelogHead(String initiatorName) {
        Long cached = changelogHeadMap.get(initiatorName);
        if (cached != null) return cached;
        long head = warmUpChangelogHead(initiatorName);
        changelogHeadMap.put(initiatorName, head);
        return head;
    }

    /**
     * [NEW 10-3] 전역 head를 1 증가시켜 반환하고 즉시 영속화합니다(2.2, 2.3).
     * per-user push 세션 락(Phase 4) 안에서만 호출되어야 한다.
     * 반환값이 곧 새 changeId. 전역 단조 시퀀스이므로 디바이스와 무관하게 충돌 없이 할당된다.
     */
    public synchronized long allocateNextChangeId(String initiatorName) throws IOException {
        long next = getChangelogHead(initiatorName) + 1L;
        changelogHeadMap.put(initiatorName, next);
        persistChangelogHead(initiatorName, next);
        return next;
    }

    // [NEW 10-3] changelogHead warm-up: 파일이 있으면 그 값, 없으면 changelog max를 하한으로.
    private long warmUpChangelogHead(String initiatorName) {
        Path headFile = getServerChangelogHeadFile(initiatorName);
        if (Files.isRegularFile(headFile)) {
            try {
                String s = Files.readString(headFile).trim();
                if (!s.isEmpty()) return Long.parseLong(s);
            } catch (IOException | NumberFormatException e) {
                System.err.println("CMFileSyncInfo.warmUpChangelogHead(), failed to read head file: "
                        + e.getMessage());
            }
        }
        // 파일 없음/손상 → changelog 전체 max를 하한으로 초기화. 전역 할당으로 새로 발급되는 id가
        // 기존 changelog max보다 작으면 충돌하므로 반드시 max를 하한으로 삼는다(2.2).
        return computeChangelogMax(initiatorName);
    }

    // [NEW 10-3] 사용자 changelog 전체에서 max changeId를 스캔(이력 없으면 0).
    private long computeChangelogMax(String initiatorName) {
        long max = 0L;
        Path logDir = getServerChangelogDir(initiatorName);
        if (!Files.isDirectory(logDir)) return 0L;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, "changelog-*.jsonl")) {
            for (Path logFile : stream) {
                for (String line : Files.readAllLines(logFile)) {
                    if (line.isBlank()) continue;
                    long changeId = CMFileSyncChangeLogEntry.fromJsonString(line).getChangeId();
                    if (changeId > max) max = changeId;
                }
            }
        } catch (IOException e) {
            System.err.println("CMFileSyncInfo.computeChangelogMax(), failed to scan changelog: "
                    + e.getMessage());
        }
        return max;
    }

    // [NEW 10-3] 전역 head를 changelogHead 파일에 write-through.
    private void persistChangelogHead(String initiatorName, long head) throws IOException {
        Path headFile = getServerChangelogHeadFile(initiatorName);
        Files.createDirectories(headFile.getParent());
        Files.writeString(headFile, Long.toString(head),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * [NEW 10-3] per-user push 세션 소유권(lease) 획득 시도(§2.6.1).
     * 사용자별 원자 구간(CHM.compute)에서 판정한다:
     * <ul>
     *   <li>slot 비어있음 OR (now - acquiredAt) > timeout(죽은 owner lazy 회수) → 획득 성공.</li>
     *   <li>lease.owner == stateKey → 같은 세션 재획득(멱등). 성공. keep-alive 없음이라 timestamp 는 갱신하지 않는다.</li>
     *   <li>그 외(만료 전 다른 owner) → 실패(호출부가 busy 로 거절).</li>
     * </ul>
     * timeout 은 FILE_SYNC_PUSH_LEASE_TIMEOUT(초). 회수는 별도 sweeper 없이 이 tryAcquire 시점 lazy 판정으로만.
     * @return 획득(또는 멱등 재획득) 성공 여부
     */
    public boolean tryAcquirePushLease(String initiatorName, CMFileSyncStateKey stateKey) {
        long timeoutMillis = CMConfigurationInfo.getInstance().getFileSyncPushLeaseTimeout() * 1000L;
        long now = System.currentTimeMillis();
        CMFileSyncPushLease result = userPushLeases.compute(initiatorName, (name, existing) -> {
            if (existing == null || (now - existing.acquiredAtMillis()) > timeoutMillis) {
                // 비었거나 만료 → 새 owner 로 획득(만료 시 죽은 owner lease 를 회수하며 교체)
                return new CMFileSyncPushLease(stateKey, now);
            }
            // 만료 전: 같은 owner 면 멱등 유지, 다른 owner 면 그대로 유지(획득 실패)
            return existing;
        });
        return result.owner().equals(stateKey);
    }

    /**
     * [NEW 10-3] push 세션 소유권 해제(§2.6.1). 현재 lease.owner == stateKey 일 때만 제거한다.
     * 다른 owner 의 lease(만료 후 다른 세션이 이미 획득한 경우 등)는 건드리지 않는다.
     */
    public void releasePushLease(String initiatorName, CMFileSyncStateKey stateKey) {
        userPushLeases.compute(initiatorName, (name, existing) -> {
            if (existing != null && existing.owner().equals(stateKey)) {
                return null;    // 소유자 일치 → 제거
            }
            return existing;    // 없음/다른 소유자 → 유지
        });
    }

    /**
     * 서버측 changelog 파일들에서 (fromCursorExclusive, toCursorInclusive] 범위의 변경 항목을
     * changeId 오름차순으로 읽어 반환합니다. pull sync에서 서버가 클라이언트로 보낼
     * server entry list를 구성하는 데 사용됩니다.
     *
     * @param initiatorName       변경 로그를 조회할 사용자명
     * @param fromCursorExclusive 클라이언트 cursor (이 값은 제외)
     * @param toCursorInclusive   서버 cursor (이 값까지 포함)
     * @return changeId 오름차순으로 정렬된 변경 항목 리스트 (없으면 빈 리스트)
     */
    public List<CMFileSyncChangeLogEntry> readChangeLogEntries(String initiatorName,
                                                               long fromCursorExclusive,
                                                               long toCursorInclusive) {
        List<CMFileSyncChangeLogEntry> entries = new ArrayList<>();
        Path logDir = getServerChangelogDir(initiatorName);
        if (!Files.isDirectory(logDir)) {
            return entries;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, "changelog-*.jsonl")) {
            for (Path logFile : stream) {
                for (String line : Files.readAllLines(logFile)) {
                    if (line.isBlank()) continue;
                    CMFileSyncChangeLogEntry entry = CMFileSyncChangeLogEntry.fromJsonString(line);
                    long changeId = entry.getChangeId();
                    if (changeId > fromCursorExclusive && changeId <= toCursorInclusive) {
                        entries.add(entry);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("CMFileSyncInfo.readChangeLogEntries(), failed to read changelog: " + e.getMessage());
            e.printStackTrace();
        }
        entries.sort(Comparator.comparingLong(CMFileSyncChangeLogEntry::getChangeId));
        return entries;
    }

    private void appendChangelog(String initiatorName, UUID initiatorDeviceUuid,
                                 String op, String path, boolean isDirectory,
                                 String contentHash, long mtimeEpochSec, long sizeBytes,
                                 long changeId) throws IOException {
        String today = LocalDate.now().toString();
        Path logFile = getServerChangelogDir(initiatorName).resolve("changelog-" + today + ".jsonl");

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("changeId", changeId);
        entry.put("userName", initiatorName);
        entry.put("originDeviceUuid", initiatorDeviceUuid);
        entry.put("op", op);
        entry.put("path", path);
        entry.put("isDirectory", isDirectory);
        entry.put("contentHash", contentHash);
        entry.put("mtime", mtimeEpochSec);
        entry.put("size", sizeBytes);
        entry.put("tombstone", op.equals("DELETE"));
        entry.put("ts", OffsetDateTime.now().toString());
        String logEntry = new ObjectMapper().writeValueAsString(entry);

        Files.writeString(logFile, logEntry + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * PUSH 세션 종료 트랜잭션(completePushSync) 전용 batch changelog append.
     * caller가 record 모든 필드(originDeviceUuid 포함)를 채운 상태로 전달 — 본 메소드는 순수 직렬화/write-through.
     * 단일 파일 오픈으로 묶음 write (한 줄 단위 fsync 보장은 OS에 위임, 기존 appendChangelog와 동일).
     * 빈 리스트는 no-op (정상 path).
     * <br>doc 16840행 시그니처(name, deviceUuid, records) 대비 deviceUuid 파라미터 제거 — changelog 파일은
     * user 단위(getServerChangelogDir(initiatorName))이고 per-record device 정보는 record 내부에서 옴.
     */
    public void appendChangelogBatch(String initiatorName,
                                     List<CMFileSyncChangeLogEntry> records) throws IOException {
        if (records == null || records.isEmpty()) return;
        String today = LocalDate.now().toString();
        Path logFile = getServerChangelogDir(initiatorName).resolve("changelog-" + today + ".jsonl");

        StringBuilder sb = new StringBuilder();
        for (CMFileSyncChangeLogEntry record : records) {
            sb.append(record.toJsonString()).append(System.lineSeparator());
        }
        Files.writeString(logFile, sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
