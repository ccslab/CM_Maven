package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncClientEntry;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

// buildPushCandidateMap() 단위 테스트.
// 리플렉션으로 private 메소드/필드(detectedPathMap, pendingDeleteQueue, cycleCounter) 에 직접
// 접근해 grace period reconcile 및 분류 로직만 좁게 검증한다. WatchService / syncManager 는
// 호출 경로 밖이므로 null 상태로 두고, 실제 디스크 fixture 와 CMFileSyncInfo 싱글톤의
// lastSyncedMtimeMap 만 외부 입력으로 사용한다.
public class CMWatchServiceTaskBuildPushCandidateMapTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path syncHome;
    private CMWatchServiceTask task;
    private CMFileSyncInfo syncInfo;

    private Method buildPushCandidateMap;
    private Field detectedPathMapField;
    private Field pendingDeleteQueueField;
    private Field cycleCounterField;

    @Before
    public void setUp() throws Exception {
        syncHome = tmp.getRoot().toPath().toAbsolutePath().normalize();
        syncInfo = CMFileSyncInfo.getInstance();
        // 싱글톤 잔존 상태 리셋 (다른 테스트에서 남긴 base snapshot 영향 차단)
        syncInfo.getLastSyncedMtimeMap().clear();

        task = new CMWatchServiceTask(syncHome);

        Class<?> cls = CMWatchServiceTask.class;
        buildPushCandidateMap = cls.getDeclaredMethod("buildPushCandidateMap");
        buildPushCandidateMap.setAccessible(true);

        detectedPathMapField = cls.getDeclaredField("detectedPathMap");
        detectedPathMapField.setAccessible(true);

        pendingDeleteQueueField = cls.getDeclaredField("pendingDeleteQueue");
        pendingDeleteQueueField.setAccessible(true);

        cycleCounterField = cls.getDeclaredField("cycleCounter");
        cycleCounterField.setAccessible(true);
    }

    @After
    public void tearDown() {
        syncInfo.getLastSyncedMtimeMap().clear();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, CMFileSyncClientEntry> invokeBuild() throws Exception {
        return (Map<String, CMFileSyncClientEntry>) buildPushCandidateMap.invoke(task);
    }

    @SuppressWarnings("unchecked")
    private Map<WatchEvent.Kind<?>, List<Path>> getDetectedPathMap() throws Exception {
        return (Map<WatchEvent.Kind<?>, List<Path>>) detectedPathMapField.get(task);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPendingDeleteQueue() throws Exception {
        return (Map<String, Object>) pendingDeleteQueueField.get(task);
    }

    private long pendingDeleteFirstCycle(String relPath) throws Exception {
        Object pd = getPendingDeleteQueue().get(relPath);
        assertNotNull("pendingDeleteQueue 에 " + relPath + " 가 있어야 함", pd);
        Field f = pd.getClass().getDeclaredField("firstDetectedCycle");
        f.setAccessible(true);
        return f.getLong(pd);
    }

    private long pendingDeleteBaseMtime(String relPath) throws Exception {
        Object pd = getPendingDeleteQueue().get(relPath);
        assertNotNull("pendingDeleteQueue 에 " + relPath + " 가 있어야 함", pd);
        Field f = pd.getClass().getDeclaredField("baseMtime");
        f.setAccessible(true);
        return f.getLong(pd);
    }

    private void setCycleCounter(long n) throws Exception {
        cycleCounterField.setLong(task, n);
    }

    private void addEvent(WatchEvent.Kind<?> kind, Path absPath) throws Exception {
        Map<WatchEvent.Kind<?>, List<Path>> map = getDetectedPathMap();
        map.computeIfAbsent(kind, k -> new ArrayList<>()).add(absPath);
    }

    // mtime 을 초 단위로 정확히 설정 (Files.getLastModifiedTime().toMillis()/1000 회복 보장)
    private Path createFile(String relPath, long mtimeSec, String content) throws IOException {
        Path abs = syncHome.resolve(relPath);
        Path parent = abs.getParent();
        if (parent != null && !parent.equals(syncHome)) {
            Files.createDirectories(parent);
        }
        Files.writeString(abs, content);
        Files.setLastModifiedTime(abs, FileTime.fromMillis(mtimeSec * 1000L));
        return abs;
    }

    // ── 단계 2: 단순 분류 ────────────────────────────────────────────────

    @Test
    public void createEvent_noBaseMtime_classifiedAsCreate() throws Exception {
        Path f = createFile("a.txt", 1000L, "hello");
        addEvent(StandardWatchEventKinds.ENTRY_CREATE, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertEquals(1, result.size());
        CMFileSyncClientEntry e = result.get("a.txt");
        assertNotNull(e);
        assertEquals(CMFileSyncOp.CREATE, e.getOpHint());
        assertEquals(-1L, e.getBaseMtime());
        assertEquals(1000L, e.getCurMtime());
        assertEquals(5L, e.getSize());
        assertTrue(getPendingDeleteQueue().isEmpty());
    }

    @Test
    public void modifyEvent_withBaseMtime_classifiedAsModify() throws Exception {
        Path f = createFile("a.txt", 2000L, "world!");
        syncInfo.setLastSyncedMtime("a.txt", 1000L);
        addEvent(StandardWatchEventKinds.ENTRY_MODIFY, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        CMFileSyncClientEntry e = result.get("a.txt");
        assertEquals(CMFileSyncOp.MODIFY, e.getOpHint());
        assertEquals(1000L, e.getBaseMtime());
        assertEquals(2000L, e.getCurMtime());
        assertEquals(6L, e.getSize());
    }

    @Test
    public void createEvent_butBaseMtimeExists_reclassifiedAsModify() throws Exception {
        // WatchService 가 ENTRY_CREATE 로 보고해도 baseMtime 이 있으면 MODIFY 로 재분류해야 한다
        // (scanLocalPushCandidates 와 동일한 기준).
        Path f = createFile("a.txt", 2000L, "hi");
        syncInfo.setLastSyncedMtime("a.txt", 500L);
        addEvent(StandardWatchEventKinds.ENTRY_CREATE, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertEquals(CMFileSyncOp.MODIFY, result.get("a.txt").getOpHint());
        assertEquals(500L, result.get("a.txt").getBaseMtime());
    }

    @Test
    public void directoryCreate_skipped() throws Exception {
        Path dir = syncHome.resolve("subdir");
        Files.createDirectory(dir);
        addEvent(StandardWatchEventKinds.ENTRY_CREATE, dir);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertTrue(result.isEmpty());
        assertTrue(getPendingDeleteQueue().isEmpty());
    }

    // ── 단계 2: DELETE 후보 → grace 큐 ────────────────────────────────────

    @Test
    public void deleteEvent_enqueuedNotEmitted() throws Exception {
        // 이전에 동기화돼 있던 파일이 사라지고 DELETE 이벤트만 도착한 표준 시나리오.
        syncInfo.setLastSyncedMtime("a.txt", 1000L);
        Path f = syncHome.resolve("a.txt"); // 디스크에는 실제로 생성하지 않음
        setCycleCounter(5L);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertTrue("DELETE 는 즉시 emit 하지 않고 큐로 들어가야 함", result.isEmpty());
        assertEquals(1, getPendingDeleteQueue().size());
        assertEquals(5L, pendingDeleteFirstCycle("a.txt"));
        assertEquals("base snapshot 의 mtime 이 큐에 보존돼야 함",
                1000L, pendingDeleteBaseMtime("a.txt"));
    }

    @Test
    public void sameCycle_createPlusDelete_netDeleteQueued() throws Exception {
        // 한 사이클 내 CREATE + DELETE → net DELETE 가 우선. 디스크는 부재 상태.
        Path f = syncHome.resolve("a.txt");
        Files.writeString(f, "x");
        Files.delete(f);
        setCycleCounter(3L);
        addEvent(StandardWatchEventKinds.ENTRY_CREATE, f);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertTrue("net DELETE → result 비어야 함", result.isEmpty());
        assertEquals(1, getPendingDeleteQueue().size());
        assertEquals(3L, pendingDeleteFirstCycle("a.txt"));
    }

    @Test
    public void modifyEvent_butFileMissingAtScanTime_routedToDeleteQueue() throws Exception {
        // MODIFY 이벤트가 도착했지만 scan 시점에 파일이 사라진 경우 (짧은 MODIFY → DELETE).
        syncInfo.setLastSyncedMtime("a.txt", 1000L);
        Path f = syncHome.resolve("a.txt");
        setCycleCounter(2L);
        addEvent(StandardWatchEventKinds.ENTRY_MODIFY, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertTrue(result.isEmpty());
        assertEquals(1, getPendingDeleteQueue().size());
        assertEquals(2L, pendingDeleteFirstCycle("a.txt"));
    }

    @Test
    public void deleteEventButFileExistsAtScanTime_enqueuedThenRevivedNextCycle() throws Exception {
        // DELETE 이벤트가 도착했지만 scan 시점에는 파일이 존재 (짧은 DELETE → CREATE,
        // CREATE 이벤트는 다음 사이클로 밀린 변형). kind == DELETE 가 우선해 큐로 enqueue,
        // 다음 사이클 reconcile 에서 파일 부활 감지해 drop.
        Path f = createFile("a.txt", 2000L, "x");
        syncInfo.setLastSyncedMtime("a.txt", 500L);
        setCycleCounter(5L);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();
        assertTrue("DELETE 이벤트 → 큐", result.isEmpty());
        assertEquals(1, getPendingDeleteQueue().size());

        // 사이클 진행 (run() 의 detectedPathMap.clear 흉내)
        getDetectedPathMap().clear();
        setCycleCounter(6L);

        Map<String, CMFileSyncClientEntry> result2 = invokeBuild();

        assertTrue("파일 부활 → 큐 drop, push 없음", result2.isEmpty());
        assertTrue(getPendingDeleteQueue().isEmpty());
    }

    // ── 단계 1: reconcile ─────────────────────────────────────────────────

    @Test
    public void graceNotExpired_keptInQueue() throws Exception {
        // 사이클 5 에 enqueue. 사이클 카운터를 같게 유지한 채 reconcile → age=0 < grace(1).
        syncInfo.setLastSyncedMtime("a.txt", 1000L);
        Path f = syncHome.resolve("a.txt");
        setCycleCounter(5L);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);
        invokeBuild();
        assertEquals(1, getPendingDeleteQueue().size());

        // cycleCounter 그대로 (5) — age = 0 < 1 → 유지
        getDetectedPathMap().clear();
        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertTrue(result.isEmpty());
        assertEquals("age 미경과 → 큐 유지", 1, getPendingDeleteQueue().size());
        assertEquals(5L, pendingDeleteFirstCycle("a.txt"));
    }

    @Test
    public void graceExpired_promotedToConfirmedDelete() throws Exception {
        syncInfo.setLastSyncedMtime("a.txt", 1000L);
        Path f = syncHome.resolve("a.txt");
        setCycleCounter(5L);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);
        invokeBuild();
        assertEquals(1, getPendingDeleteQueue().size());

        // 사이클 6: 파일 여전히 부재, age = 6-5 = 1 >= grace(1) → 승격
        getDetectedPathMap().clear();
        setCycleCounter(6L);
        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertEquals(1, result.size());
        CMFileSyncClientEntry e = result.get("a.txt");
        assertEquals(CMFileSyncOp.DELETE, e.getOpHint());
        assertEquals(-1L, e.getCurMtime());
        assertEquals(0L, e.getSize());
        assertEquals("승격된 entry 의 baseMtime 은 큐 enqueue 시점의 값을 유지해야 함",
                1000L, e.getBaseMtime());
        assertTrue("승격 후 큐에서 제거", getPendingDeleteQueue().isEmpty());
    }

    @Test
    public void fileRevives_queueEntryDropped_andEventReclassifiedAsModify() throws Exception {
        // cycle-split atomic save 의 정확한 시나리오:
        //   사이클 N: DELETE 이벤트 → 큐 enqueue
        //   사이클 N+1: CREATE 이벤트 + 파일 존재 → 큐 drop + MODIFY 분류 (baseMtime 유지)
        syncInfo.setLastSyncedMtime("a.txt", 1000L);
        Path f = syncHome.resolve("a.txt");
        setCycleCounter(5L);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);
        invokeBuild();
        assertEquals(1, getPendingDeleteQueue().size());

        // 다음 사이클: 파일 부활 + CREATE 이벤트 도착
        Files.writeString(f, "back");
        Files.setLastModifiedTime(f, FileTime.fromMillis(2000L * 1000L));
        getDetectedPathMap().clear();
        setCycleCounter(6L);
        addEvent(StandardWatchEventKinds.ENTRY_CREATE, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertTrue("큐 entry 가 drop 되어야 함", getPendingDeleteQueue().isEmpty());
        assertEquals(1, result.size());
        CMFileSyncClientEntry e = result.get("a.txt");
        assertEquals("baseMtime 유지 (DELETE 가 push 되지 않음) → MODIFY 로 재분류",
                CMFileSyncOp.MODIFY, e.getOpHint());
        assertEquals(1000L, e.getBaseMtime());
        assertEquals(2000L, e.getCurMtime());
    }

    @Test
    public void multipleDeleteEventsSameCycle_dedupedPreservesFirstDetectedCycle() throws Exception {
        // OS 가 같은 path 에 대해 DELETE 이벤트를 두 번 emit 한 경우 (드물지만 가능).
        // containsKey 가드로 firstDetectedCycle 가 보존되어야 한다.
        syncInfo.setLastSyncedMtime("a.txt", 1000L);
        Path f = syncHome.resolve("a.txt");
        setCycleCounter(5L);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertTrue(result.isEmpty());
        assertEquals("중복 enqueue 방지 — 항목 1개", 1, getPendingDeleteQueue().size());
        assertEquals(5L, pendingDeleteFirstCycle("a.txt"));
    }

    @Test
    public void promotionPlusDeleteEventSameCycle_promotionPreservedNoReEnqueue() throws Exception {
        // 사이클 N+1 의 단계 1 이 큐 항목을 promote 한 직후, 같은 사이클 단계 2 에서 또
        // ENTRY_DELETE 이벤트가 도착한 경우 — promotion 결과가 result 에 남아 있어야 하고
        // 큐에 다시 enqueue 되어서는 안 된다 (existing.getOpHint() == DELETE 가드).
        syncInfo.setLastSyncedMtime("a.txt", 500L);
        Path f = syncHome.resolve("a.txt");
        setCycleCounter(5L);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);
        invokeBuild();
        assertEquals(1, getPendingDeleteQueue().size());

        // 사이클 6: reconcile 이 promote, 동시에 새 DELETE 이벤트 도착
        getDetectedPathMap().clear();
        setCycleCounter(6L);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertEquals(1, result.size());
        assertEquals(CMFileSyncOp.DELETE, result.get("a.txt").getOpHint());
        assertEquals(500L, result.get("a.txt").getBaseMtime());
        assertTrue("같은 사이클의 후속 DELETE 이벤트는 dedup", getPendingDeleteQueue().isEmpty());
    }

    @Test
    public void nestedPath_relativizedWithForwardSlash() throws Exception {
        // sub/a.txt 같은 nested path 의 relPath 생성과 분류가 동작하는지 확인.
        Path f = createFile("sub/a.txt", 1000L, "abc");
        addEvent(StandardWatchEventKinds.ENTRY_CREATE, f);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertEquals(1, result.size());
        assertNotNull("'sub/a.txt' 키로 조회되어야 함 (forward-slash)",
                result.get("sub/a.txt"));
        assertEquals(CMFileSyncOp.CREATE, result.get("sub/a.txt").getOpHint());
    }
}
