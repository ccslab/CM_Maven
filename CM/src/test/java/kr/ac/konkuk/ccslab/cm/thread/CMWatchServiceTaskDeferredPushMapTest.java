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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// buildPushCandidateMap() 단계 3 (deferredPushMap 재투입 / Case 1) 단위 테스트.
// startPushSync 가 세션 진행 중이라 거절했을 때 보존된 후보를, 다음 사이클에 현재 디스크
// 기준으로 재검증해 result 에 합류시키는지 검증한다. 핵심은 이미 promotion 됐던 DELETE 가
// grace 재연장 없이 즉시 재시도되는 것. 리플렉션으로 private 메소드/필드에 접근한다.
public class CMWatchServiceTaskDeferredPushMapTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path syncHome;
    private CMWatchServiceTask task;
    private CMFileSyncInfo syncInfo;

    private Method buildPushCandidateMap;
    private Field detectedPathMapField;
    private Field pendingDeleteQueueField;
    private Field deferredPushMapField;
    private Field cycleCounterField;

    @Before
    public void setUp() throws Exception {
        syncHome = tmp.getRoot().toPath().toAbsolutePath().normalize();
        syncInfo = CMFileSyncInfo.getInstance();
        syncInfo.getLastSyncedMtimeMap().clear();
        syncInfo.getLastSyncedSizeMap().clear();

        task = new CMWatchServiceTask(syncHome);

        Class<?> cls = CMWatchServiceTask.class;
        buildPushCandidateMap = cls.getDeclaredMethod("buildPushCandidateMap");
        buildPushCandidateMap.setAccessible(true);
        detectedPathMapField = cls.getDeclaredField("detectedPathMap");
        detectedPathMapField.setAccessible(true);
        pendingDeleteQueueField = cls.getDeclaredField("pendingDeleteQueue");
        pendingDeleteQueueField.setAccessible(true);
        deferredPushMapField = cls.getDeclaredField("deferredPushMap");
        deferredPushMapField.setAccessible(true);
        cycleCounterField = cls.getDeclaredField("cycleCounter");
        cycleCounterField.setAccessible(true);
    }

    @After
    public void tearDown() {
        syncInfo.getLastSyncedMtimeMap().clear();
        syncInfo.getLastSyncedSizeMap().clear();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, CMFileSyncClientEntry> invokeBuild() throws Exception {
        return (Map<String, CMFileSyncClientEntry>) buildPushCandidateMap.invoke(task);
    }

    @SuppressWarnings("unchecked")
    private Map<String, CMFileSyncClientEntry> deferredPushMap() throws Exception {
        return (Map<String, CMFileSyncClientEntry>) deferredPushMapField.get(task);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pendingDeleteQueue() throws Exception {
        return (Map<String, Object>) pendingDeleteQueueField.get(task);
    }

    private void addEvent(WatchEvent.Kind<?> kind, Path absPath) throws Exception {
        @SuppressWarnings("unchecked")
        Map<WatchEvent.Kind<?>, List<Path>> map =
                (Map<WatchEvent.Kind<?>, List<Path>>) detectedPathMapField.get(task);
        map.computeIfAbsent(kind, k -> new ArrayList<>()).add(absPath);
    }

    private void setCycleCounter(long n) throws Exception {
        cycleCounterField.setLong(task, n);
    }

    private Path createFile(String relPath, long mtimeSec, String content) throws IOException {
        Path abs = syncHome.resolve(relPath);
        Path parent = abs.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(abs, content);
        Files.setLastModifiedTime(abs, FileTime.fromMillis(mtimeSec * 1000L));
        return abs;
    }

    private void putDeferred(String relPath, CMFileSyncOp op, long curMtime, long baseMtime,
                            long size) throws Exception {
        CMFileSyncClientEntry e = new CMFileSyncClientEntry()
                .setPath(relPath).setOpHint(op).setCurMtime(curMtime)
                .setBaseMtime(baseMtime).setSize(size);
        deferredPushMap().put(relPath, e);
    }

    // ── 테스트 ────────────────────────────────────────────────────────────

    @Test
    public void deferredCreate_filePresent_reattemptedAsCreate() throws Exception {
        createFile("a.txt", 1000L, "hello");
        putDeferred("a.txt", CMFileSyncOp.CREATE, 1000L, -1L, 5L);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        CMFileSyncClientEntry e = result.get("a.txt");
        assertNotNull("보류 CREATE 가 재투입되어야 함", e);
        assertEquals(CMFileSyncOp.CREATE, e.getOpHint());
        assertEquals(1000L, e.getCurMtime());
        assertEquals(5L, e.getSize());
        assertEquals(-1L, e.getBaseMtime());
    }

    @Test
    public void deferredModify_filePresent_reattemptedAsModifyWithFreshDiskState() throws Exception {
        // 보류 당시 size 와 다르게 디스크가 바뀌어 있어도 현재 디스크 기준으로 재분류해야 함
        createFile("a.txt", 2000L, "changed-content");   // 15 bytes
        syncInfo.setLastSynced("a.txt", 1000L, 5L);
        putDeferred("a.txt", CMFileSyncOp.MODIFY, 1500L, 1000L, 9L);   // stale curMtime/size

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        CMFileSyncClientEntry e = result.get("a.txt");
        assertNotNull(e);
        assertEquals(CMFileSyncOp.MODIFY, e.getOpHint());
        assertEquals("현재 디스크 mtime 반영", 2000L, e.getCurMtime());
        assertEquals("현재 디스크 size 반영", 15L, e.getSize());
        assertEquals(1000L, e.getBaseMtime());
    }

    @Test
    public void deferredDelete_fileAbsent_reattemptedImmediately_noGraceReextension() throws Exception {
        // 핵심: 이미 promotion 됐던 DELETE 는 grace 큐를 다시 거치지 않고 즉시 result 로 복귀.
        syncInfo.setLastSynced("gone.txt", 1000L, 3L);
        putDeferred("gone.txt", CMFileSyncOp.DELETE, -1L, 1000L, 0L);
        setCycleCounter(7L);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        CMFileSyncClientEntry e = result.get("gone.txt");
        assertNotNull("보류 DELETE 가 즉시 재시도되어야 함", e);
        assertEquals(CMFileSyncOp.DELETE, e.getOpHint());
        assertTrue("grace 큐로 재진입(재연장)하면 안 됨", pendingDeleteQueue().isEmpty());
    }

    @Test
    public void deferredCreateOrModify_fileVanished_routedToGraceQueue() throws Exception {
        // 보류된 CREATE 의 파일이 그 사이 삭제됨 → 새 DELETE 로 grace 큐에 enqueue (result X)
        syncInfo.setLastSynced("v.txt", 1000L, 4L);
        putDeferred("v.txt", CMFileSyncOp.MODIFY, 1500L, 1000L, 4L);   // 디스크엔 파일 없음
        setCycleCounter(3L);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertNull("DELETE 후보는 grace 단계라 result 에 없어야 함", result.get("v.txt"));
        assertTrue("grace 큐에 enqueue 되어야 함", pendingDeleteQueue().containsKey("v.txt"));
    }

    @Test
    public void freshEvent_winsOverDeferred_noDuplicate() throws Exception {
        // 같은 path 가 신선 이벤트(detectedPathMap)와 deferred 양쪽에 있으면 신선한 쪽 우선
        Path f = createFile("a.txt", 3000L, "fresh!!");   // 7 bytes
        syncInfo.setLastSynced("a.txt", 1000L, 5L);
        addEvent(StandardWatchEventKinds.ENTRY_MODIFY, f);
        putDeferred("a.txt", CMFileSyncOp.CREATE, 999L, -1L, 5L);   // 다른(오래된) 분류

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertEquals(1, result.size());
        CMFileSyncClientEntry e = result.get("a.txt");
        assertEquals("신선한 MODIFY 분류 유지", CMFileSyncOp.MODIFY, e.getOpHint());
        assertEquals(3000L, e.getCurMtime());
        assertEquals(7L, e.getSize());
    }

    @Test
    public void deferredSelfEvent_dropped() throws Exception {
        // 보류 중 디스크가 base snapshot 과 동일해짐(mtime+size 일치) → push 불필요, drop
        createFile("a.txt", 1000L, "hello");   // mtime 1000, size 5
        syncInfo.setLastSynced("a.txt", 1000L, 5L);
        putDeferred("a.txt", CMFileSyncOp.MODIFY, 900L, 1000L, 5L);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        assertTrue("self-event 는 result 에서 제외되어야 함", result.isEmpty());
    }

    @Test
    public void deferredPath_alreadyInGraceQueue_skipped() throws Exception {
        // 같은 path 가 이미 grace 큐에 있으면(이번 사이클 DELETE 이벤트) deferred 는 스킵
        Path f = createFile("a.txt", 2000L, "hi");
        syncInfo.setLastSynced("a.txt", 1000L, 5L);
        // 먼저 DELETE 이벤트로 grace 큐 진입시키기 위해 파일을 지운 뒤 DELETE 이벤트 추가
        Files.delete(f);
        addEvent(StandardWatchEventKinds.ENTRY_DELETE, f);
        putDeferred("a.txt", CMFileSyncOp.MODIFY, 2000L, 1000L, 2L);
        setCycleCounter(5L);

        Map<String, CMFileSyncClientEntry> result = invokeBuild();

        // 단계 2 에서 grace 큐에 들어가고, 단계 3 은 grace 큐에 있으므로 deferred 를 스킵 →
        // result 에는 MODIFY 가 끼어들지 않아야 한다.
        assertFalse(result.containsKey("a.txt"));
        assertTrue(pendingDeleteQueue().containsKey("a.txt"));
    }
}
