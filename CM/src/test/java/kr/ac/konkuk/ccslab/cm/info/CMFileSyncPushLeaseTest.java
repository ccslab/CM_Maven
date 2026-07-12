package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncStateKey;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * [10-3] per-user push 세션 lease(§2.6.1)의 tryAcquire/release/timeout 판정 단위 테스트.
 * 순수 로컬 — 서버/네트워크 불필요. 테스트 간 간섭을 피하려 메소드별 고유 initiatorName 을 쓴다.
 */
public class CMFileSyncPushLeaseTest {
    private CMFileSyncInfo syncInfo;
    private CMConfigurationInfo confInfo;

    @Before
    public void setUp() {
        syncInfo = CMFileSyncInfo.getInstance();
        confInfo = CMConfigurationInfo.getInstance();
        // timeout 테스트 외에는 살아있는 lease 가 회수되지 않도록 넉넉히.
        confInfo.setFileSyncPushLeaseTimeout(300);
    }

    @Test
    public void acquireBlocksOtherDeviceThenReleaseAllowsIt() {
        String user = "lease-user-block";
        CMFileSyncStateKey devA = new CMFileSyncStateKey(user, UUID.randomUUID());
        CMFileSyncStateKey devB = new CMFileSyncStateKey(user, UUID.randomUUID());

        assertTrue("A 가 빈 slot 을 획득", syncInfo.tryAcquirePushLease(user, devA));
        assertFalse("만료 전 다른 device B 는 busy 로 거절", syncInfo.tryAcquirePushLease(user, devB));

        syncInfo.releasePushLease(user, devA);
        assertTrue("A 해제 후 B 가 획득", syncInfo.tryAcquirePushLease(user, devB));

        syncInfo.releasePushLease(user, devB);
    }

    @Test
    public void sameOwnerReacquireIsIdempotent() {
        String user = "lease-user-idem";
        CMFileSyncStateKey devA = new CMFileSyncStateKey(user, UUID.randomUUID());

        assertTrue(syncInfo.tryAcquirePushLease(user, devA));
        assertTrue("같은 세션 재획득은 멱등 성공", syncInfo.tryAcquirePushLease(user, devA));

        syncInfo.releasePushLease(user, devA);
    }

    @Test
    public void expiredLeaseIsReclaimed() throws InterruptedException {
        String user = "lease-user-expire";
        confInfo.setFileSyncPushLeaseTimeout(0);    // 즉시 만료(경과 > 0 이면 회수)
        CMFileSyncStateKey devA = new CMFileSyncStateKey(user, UUID.randomUUID());
        CMFileSyncStateKey devB = new CMFileSyncStateKey(user, UUID.randomUUID());

        assertTrue(syncInfo.tryAcquirePushLease(user, devA));
        Thread.sleep(10);   // A lease 만료 유도
        assertTrue("만료된 A lease 를 B 가 lazy 회수하며 획득", syncInfo.tryAcquirePushLease(user, devB));

        syncInfo.releasePushLease(user, devB);
    }

    @Test
    public void releaseByNonOwnerIsNoop() {
        String user = "lease-user-nonowner";
        CMFileSyncStateKey devA = new CMFileSyncStateKey(user, UUID.randomUUID());
        CMFileSyncStateKey devB = new CMFileSyncStateKey(user, UUID.randomUUID());

        assertTrue(syncInfo.tryAcquirePushLease(user, devA));
        syncInfo.releasePushLease(user, devB);  // 비소유자 release → no-op
        assertFalse("A 의 lease 는 그대로 → B 는 여전히 busy", syncInfo.tryAcquirePushLease(user, devB));

        syncInfo.releasePushLease(user, devA);
    }
}
