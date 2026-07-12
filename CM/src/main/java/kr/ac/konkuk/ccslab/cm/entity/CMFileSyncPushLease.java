package kr.ac.konkuk.ccslab.cm.entity;

/**
 * [NEW 10-3] per-user push 세션 소유권 레코드(§2.6.1).
 * 한 사용자(initiatorName)에 대해 push 세션은 동시에 하나만 진행되도록 직렬화하기 위해,
 * 서버가 CMFileSyncInfo.userPushLeases 에 (initiatorName -> 이 레코드)로 보관한다.
 * <br>{@code owner} = 세션을 소유한 (initiatorName, initiatorDeviceUuid) stateKey.
 * {@code acquiredAtMillis} = 획득 시각(System.currentTimeMillis). timeout lazy 회수 판정에 사용.
 * <br>획득 시 한 번 세팅하고 변경하지 않으므로 불변(record). 후속에 keep-alive 로 lastActivity 를
 * 갱신하게 되면 그때 일반 class 로 전환한다(§8 후속).
 * @author CCSLab, Konkuk University
 */
public record CMFileSyncPushLease(CMFileSyncStateKey owner, long acquiredAtMillis) {
}
