package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncClientEntry;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncProgress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

// CMFileSyncManager.startPushSync() 의 early-return 가드 4개와 롤백 분기 검증.
// 성공 경로는 CMEventManager 의 실제 네트워크 송신까지 가야 하므로 본 단위 테스트 범위 밖
// (통합 테스트 영역). 여기서는 가드/롤백만 좁게 본다.
//   ① CLIENT 가 아니면 false
//   ② syncProgress != NONE 이면 false
//   ③ candidateMap 이 비거나 null 이면 true (push 없음 = 동기화된 상태)
//   ④ proceedPendingPushMap() 실패 시 syncProgress 를 NONE 으로 롤백
public class CMFileSyncManagerStartPushSyncTest {

    private CMFileSyncManager manager;
    private CMFileSyncInfo syncInfo;
    private CMConfigurationInfo confInfo;
    private String originalSystemType;
    private CMFileSyncProgress originalProgress;

    @Before
    public void setUp() {
        syncInfo = CMFileSyncInfo.getInstance();
        confInfo = CMConfigurationInfo.getInstance();
        originalSystemType = confInfo.getSystemType();
        originalProgress = syncInfo.getSyncProgress();

        // 표준 시작 상태: CLIENT + NONE + 빈 pendingPushMap + 빈 pushEntryList
        confInfo.setSystemType("CLIENT");
        syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
        syncInfo.getPendingPushMap().clear();
        syncInfo.setPushEntryList(null);

        manager = new CMFileSyncManager();
    }

    @After
    public void tearDown() {
        // 싱글톤 상태를 원복 (테스트 간 누수 차단)
        confInfo.setSystemType(originalSystemType);
        syncInfo.setSyncProgress(originalProgress);
        syncInfo.getPendingPushMap().clear();
        syncInfo.setPushEntryList(null);
    }

    private CMFileSyncClientEntry entry(String path) {
        return new CMFileSyncClientEntry()
                .setPath(path)
                .setCurMtime(1000L)
                .setBaseMtime(-1L)
                .setSize(5L)
                .setOpHint(CMFileSyncOp.CREATE);
    }

    private Map<String, CMFileSyncClientEntry> candidateMapOf(String path) {
        Map<String, CMFileSyncClientEntry> m = new HashMap<>();
        m.put(path, entry(path));
        return m;
    }

    // ── ① CLIENT 가드 ────────────────────────────────────────────────────

    @Test
    public void notClient_returnsFalse_noStateMutation() {
        confInfo.setSystemType("SERVER");
        Map<String, CMFileSyncClientEntry> input = candidateMapOf("a.txt");

        boolean ret = manager.startPushSync(input);

        assertFalse(ret);
        assertEquals(CMFileSyncProgress.NONE, syncInfo.getSyncProgress());
        assertTrue("CLIENT 가드 fail → pendingPushMap 에 적재되면 안 됨",
                syncInfo.getPendingPushMap().isEmpty());
    }

    // ── ② 세션 진행 중 가드 ──────────────────────────────────────────────

    @Test
    public void syncProgressPull_returnsFalse_progressUnchanged() {
        syncInfo.setSyncProgress(CMFileSyncProgress.PULL);
        Map<String, CMFileSyncClientEntry> input = candidateMapOf("a.txt");

        boolean ret = manager.startPushSync(input);

        assertFalse(ret);
        assertEquals("진행 중 PULL 상태를 건드리면 안 됨",
                CMFileSyncProgress.PULL, syncInfo.getSyncProgress());
        assertTrue("clear+putAll 분기 미실행 → pendingPushMap 미변경",
                syncInfo.getPendingPushMap().isEmpty());
    }

    @Test
    public void syncProgressFullSync_returnsFalse_progressUnchanged() {
        // FULL_SYNC 도 동일하게 차단되어야 한다 (NONE 만 통과).
        syncInfo.setSyncProgress(CMFileSyncProgress.FULL_SYNC);
        Map<String, CMFileSyncClientEntry> input = candidateMapOf("a.txt");

        boolean ret = manager.startPushSync(input);

        assertFalse(ret);
        assertEquals(CMFileSyncProgress.FULL_SYNC, syncInfo.getSyncProgress());
        assertTrue(syncInfo.getPendingPushMap().isEmpty());
    }

    // ── ③ 빈/null candidateMap 가드 ──────────────────────────────────────

    @Test
    public void emptyCandidateMap_returnsTrue_noStateMutation() {
        boolean ret = manager.startPushSync(Collections.emptyMap());

        assertTrue("빈 맵 → 동기화된 상태로 간주, true 반환", ret);
        assertEquals(CMFileSyncProgress.NONE, syncInfo.getSyncProgress());
        assertTrue(syncInfo.getPendingPushMap().isEmpty());
    }

    @Test
    public void nullCandidateMap_returnsTrue_noStateMutation() {
        boolean ret = manager.startPushSync(null);

        assertTrue("null → 동기화된 상태로 간주, true 반환", ret);
        assertEquals(CMFileSyncProgress.NONE, syncInfo.getSyncProgress());
        assertTrue(syncInfo.getPendingPushMap().isEmpty());
    }

    // ── ④ proceedPendingPushMap 실패 시 syncProgress 롤백 ────────────────

    @Test
    public void proceedFails_rollsBackSyncProgressToNone_pendingPushMapKept() {
        // proceedPendingPushMap() 의 실패 분기를 안정적으로 재현하기 위해 subclass override.
        // CMEventManager 의 실제 네트워크 송신 의존을 피하고 가드/롤백 로직만 좁게 검증한다.
        // 추가로 호출 시점의 syncProgress 를 캡처해 set(PUSH) → proceed() → set(NONE) 순서도 확인.
        CMFileSyncProgress[] capturedProgress = new CMFileSyncProgress[1];
        CMFileSyncManager failingManager = new CMFileSyncManager() {
            @Override
            public boolean proceedPendingPushMap() {
                capturedProgress[0] = CMFileSyncInfo.getInstance().getSyncProgress();
                return false;
            }
        };

        Map<String, CMFileSyncClientEntry> input = candidateMapOf("a.txt");
        boolean ret = failingManager.startPushSync(input);

        assertFalse(ret);
        assertEquals("proceedPendingPushMap 호출 시점에 syncProgress 가 PUSH 였어야 함",
                CMFileSyncProgress.PUSH, capturedProgress[0]);
        assertEquals("롤백 — syncProgress 가 NONE 으로 복원되어야 다음 watch 트리거가 통과 가능",
                CMFileSyncProgress.NONE, syncInfo.getSyncProgress());

        // pendingPushMap 은 의도적으로 비우지 않는다 (다음 startPushSync 호출의 clear+putAll 이
        // 덮어쓰는 설계). candidateMap 의 entry 가 그대로 남아 있는지 확인.
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        assertEquals(1, pendingPushMap.size());
        assertNotNull(pendingPushMap.get("a.txt"));
        assertSame("putAll 로 동일 instance 가 적재되어야 함",
                input.get("a.txt"), pendingPushMap.get("a.txt"));
    }
}
