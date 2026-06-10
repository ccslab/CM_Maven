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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

// enqueueExistingFilesUnder() 단위 테스트 (등록 레이스 보완 / Case 2).
// 디렉토리와 내용이 원자적으로 등장(폴더 통째 move / 압축 해제 / git checkout)하면 내부
// 파일들의 ENTRY_CREATE 이벤트가 registerTree 등록 이전에 지나가 영영 안 올 수 있다.
// enqueueExistingFilesUnder 가 그 기존 파일들을 detectedPathMap[ENTRY_CREATE] 에 직접
// 적재하는지, 그리고 buildPushCandidateMap 이 그들을 CREATE 후보로 분류하는지 검증한다.
// 리플렉션으로 private 메소드/필드에 접근한다(기존 buildPushCandidateMap 테스트와 동일 패턴).
// watchService/syncManager 는 호출 경로 밖이라 null 로 둔다 — 본 메소드는 syncPath 와
// CMFileSyncInfo 싱글톤만 참조한다.
public class CMWatchServiceTaskEnqueueExistingFilesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path syncHome;
    private CMWatchServiceTask task;
    private CMFileSyncInfo syncInfo;

    private Method enqueueExistingFilesUnder;
    private Method buildPushCandidateMap;
    private java.lang.reflect.Field detectedPathMapField;

    @Before
    public void setUp() throws Exception {
        syncHome = tmp.getRoot().toPath().toAbsolutePath().normalize();
        syncInfo = CMFileSyncInfo.getInstance();
        syncInfo.getLastSyncedMtimeMap().clear();

        task = new CMWatchServiceTask(syncHome);

        Class<?> cls = CMWatchServiceTask.class;
        enqueueExistingFilesUnder = cls.getDeclaredMethod("enqueueExistingFilesUnder", Path.class);
        enqueueExistingFilesUnder.setAccessible(true);
        buildPushCandidateMap = cls.getDeclaredMethod("buildPushCandidateMap");
        buildPushCandidateMap.setAccessible(true);
        detectedPathMapField = cls.getDeclaredField("detectedPathMap");
        detectedPathMapField.setAccessible(true);
    }

    @After
    public void tearDown() {
        syncInfo.getLastSyncedMtimeMap().clear();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────

    private Path createFile(String relPath, String content) throws IOException {
        Path abs = syncHome.resolve(relPath);
        Path parent = abs.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(abs, content);
        return abs;
    }

    private void invokeEnqueue(Path dir) throws Exception {
        enqueueExistingFilesUnder.invoke(task, dir);
    }

    @SuppressWarnings("unchecked")
    private Map<String, CMFileSyncClientEntry> invokeBuild() throws Exception {
        return (Map<String, CMFileSyncClientEntry>) buildPushCandidateMap.invoke(task);
    }

    @SuppressWarnings("unchecked")
    private List<Path> createListInDetectedMap() throws Exception {
        Map<WatchEvent.Kind<?>, List<Path>> map =
                (Map<WatchEvent.Kind<?>, List<Path>>) detectedPathMapField.get(task);
        List<Path> list = map.get(StandardWatchEventKinds.ENTRY_CREATE);
        return list == null ? List.of() : list;
    }

    // ── 테스트 ────────────────────────────────────────────────────────────

    @Test
    public void existingFlatFiles_enqueuedAndClassifiedAsCreate() throws Exception {
        // test_files/ 안에 파일 2개가 이미 존재하는 상태로 디렉토리가 등장한 시나리오
        createFile("test_files/a.png", "aaa");
        createFile("test_files/b.png", "bbbb");

        invokeEnqueue(syncHome.resolve("test_files"));

        List<Path> created = createListInDetectedMap();
        assertEquals("기존 파일 2개가 enqueue 되어야 함", 2, created.size());

        Map<String, CMFileSyncClientEntry> result = invokeBuild();
        assertEquals(2, result.size());
        CMFileSyncClientEntry a = result.get("test_files/a.png");
        CMFileSyncClientEntry b = result.get("test_files/b.png");
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(CMFileSyncOp.CREATE, a.getOpHint());
        assertEquals(CMFileSyncOp.CREATE, b.getOpHint());
        assertEquals(3L, a.getSize());
        assertEquals(4L, b.getSize());
    }

    @Test
    public void nestedFiles_allEnqueued() throws Exception {
        createFile("test_files/x.txt", "x");
        createFile("test_files/sub/y.txt", "yy");
        createFile("test_files/sub/deep/z.txt", "zzz");

        invokeEnqueue(syncHome.resolve("test_files"));

        Map<String, CMFileSyncClientEntry> result = invokeBuild();
        assertEquals(3, result.size());
        assertTrue(result.containsKey("test_files/x.txt"));
        assertTrue(result.containsKey("test_files/sub/y.txt"));
        assertTrue(result.containsKey("test_files/sub/deep/z.txt"));
        result.values().forEach(e -> assertEquals(CMFileSyncOp.CREATE, e.getOpHint()));
    }

    @Test
    public void directoriesThemselves_notEnqueued() throws Exception {
        createFile("test_files/sub/y.txt", "yy");

        invokeEnqueue(syncHome.resolve("test_files"));

        // enqueue 된 항목은 모두 일반 파일이어야 한다 (디렉토리 엔트리 X)
        for (Path p : createListInDetectedMap()) {
            assertFalse("디렉토리가 enqueue 되면 안 됨: " + p, Files.isDirectory(p));
        }
    }

    @Test
    public void ignoredFiles_skipped() throws Exception {
        createFile("test_files/keep.txt", "k");
        createFile("test_files/.DS_Store", "junk");      // 기본 ignore 패턴
        createFile("test_files/._resource", "junk");     // "._*" 패턴

        invokeEnqueue(syncHome.resolve("test_files"));

        Map<String, CMFileSyncClientEntry> result = invokeBuild();
        Set<String> keys = result.keySet();
        assertEquals("ignore 대상 제외하고 1개만 남아야 함: " + keys, 1, keys.size());
        assertTrue(keys.contains("test_files/keep.txt"));
    }

    @Test
    public void alreadySyncedFile_stillEnqueuedButReclassifiedAsModify() throws Exception {
        // enqueue 자체는 base snapshot 을 보지 않으므로 적재하지만, baseMtime 이 있으면
        // buildPushCandidateMap 이 MODIFY 로 재분류한다. (self-event 최종 필터는 run() 의
        // filterSelfEvents 가 담당 — 여기 경로 밖이라 검증 대상 아님.)
        Path f = createFile("test_files/seen.txt", "s");
        long mtime = Files.getLastModifiedTime(f).toMillis() / 1000;
        syncInfo.setLastSyncedMtime("test_files/seen.txt", mtime - 100);

        invokeEnqueue(syncHome.resolve("test_files"));

        Map<String, CMFileSyncClientEntry> result = invokeBuild();
        CMFileSyncClientEntry e = result.get("test_files/seen.txt");
        assertNotNull(e);
        assertEquals(CMFileSyncOp.MODIFY, e.getOpHint());
    }

    @Test
    public void emptyDirectory_enqueuesNothing() throws Exception {
        Files.createDirectories(syncHome.resolve("test_files/emptysub"));

        invokeEnqueue(syncHome.resolve("test_files"));

        assertTrue("빈 디렉토리는 아무것도 enqueue 하지 않아야 함", createListInDetectedMap().isEmpty());
        assertTrue(invokeBuild().isEmpty());
    }
}
