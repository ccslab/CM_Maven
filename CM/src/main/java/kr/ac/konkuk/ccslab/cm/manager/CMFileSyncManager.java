package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncChangeLogEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncClientEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncIndexEntry;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncIndexRepository;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncStateKey;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.entity.CMUserLoginKey;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.filesync.*;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncMode;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncProgress;
import kr.ac.konkuk.ccslab.cm.info.enums.CMTestFileModType;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncProactiveModeTask;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncPullGenerator;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncPushGenerator;
import kr.ac.konkuk.ccslab.cm.thread.CMWatchServiceTask;
import kr.ac.konkuk.ccslab.cm.util.CMUtil;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CMFileSyncManager extends CMServiceManager {

    public CMFileSyncManager() {
        super();
        m_nType = CMInfo.CM_FILE_SYNC_MANAGER;
    }

    public Path getClientSyncHome() {
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        return confInfo.getTransferedFileHome().resolve(CMFileSyncInfo.SYNC_HOME)
                .toAbsolutePath().normalize();
    }

    public Path getServerSyncHome(String userName) {
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        return confInfo.getTransferedFileHome().resolve(userName)
                .resolve(CMFileSyncInfo.SYNC_HOME).toAbsolutePath().normalize();
    }

    // currently called by client; performs the initial full push synchronization
    public synchronized boolean startFullPushSync() {

        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.startFullPushSync() called..");

        CMFileSyncInfo fsInfo = CMFileSyncInfo.getInstance();

        if (fsInfo.isSyncInProgress()) {
            System.err.println("The file sync is in progress!");
            return false;
        } else {
            // set sync progress to full (push) sync.
            fsInfo.setSyncProgress(CMFileSyncProgress.FULL_SYNC);
        }

        // set file sync home.
        Path syncHome = getClientSyncHome();
        if (Files.notExists(syncHome)) {
            try {
                Files.createDirectories(syncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // create a path list in the sync-file-home.
        List<Path> pathList = createPathList(syncHome);
        if (pathList == null) return false;
        // store the path list in the CMFileSyncInfo.
        fsInfo.setPathList(pathList);

        // update the online-mode-list
        //List<Path> onlineModeList = Objects.requireNonNull(fsInfo.getOnlineModePathList());
        List<Path> onlineModeList = fsInfo.getOnlineModePathSizeMap().keySet().stream().toList();
        Iterator<Path> iter = onlineModeList.iterator();
        while (iter.hasNext()) {
            Path onlinePath = iter.next();
            if (!pathList.contains(onlinePath)) {
                //iter.remove();
                fsInfo.getOnlineModePathSizeMap().remove(onlinePath);
            }
        }

        //boolean ret = saveOnlineModeListToFile();
        boolean ret = saveOnlineModePathSizeMapToFile();
        if (!ret) {
            System.err.println("error to save online-mode-list to file!");
        }

        // send the file list to the server
        boolean sendResult = sendFileList();
        if (!sendResult) {
            System.err.println("CMFileSyncManager.startFullPushSync(), error to send the file list.");
            return false;
        }

        return true;
    }

    // called by client to start bidirectional synchronization:
    // sends the client cursor to the server so it can decide pull vs. full-push.
    public synchronized boolean startPullSync() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.startPullSync() called..");

        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        // only the client can start a pull sync
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if (!confInfo.getSystemType().equals("CLIENT")) {
            System.err.println("CMFileSyncManager.startPullSync(), system type is not CLIENT!");
            return false;
        }

        // 동기화 시작 시 디스크 메타(cursor, client-index)를 권위값으로 재적용한다.
        // 실행 중 메타 파일을 삭제한 경우 메모리의 옛 cursor 가 남아 "이미 동기화됨"으로 오인하므로,
        // reset-then-load 로 디스크 상태(없으면 fresh)를 메모리에 반영한다.
        syncInfo.reloadClientMetaFromDisk(".");

        // create the START_PULL_SYNC event
        CMFileSyncEventStartPullSync fse = new CMFileSyncEventStartPullSync();

        // set common initiator fields
        String initiatorName = interInfo.getMyself().getName();
        UUID initiatorUuid = interInfo.getMyself().getUuid();
        UUID initiatorDeviceUuid = syncInfo.getDeviceUuid();
        fse.setInitiatorName(initiatorName);
        fse.setInitiatorUuid(initiatorUuid);
        fse.setInitiatorDeviceUuid(initiatorDeviceUuid);

        // set the client cursor (use the stored value if non-negative, otherwise 0)
        long cursor = syncInfo.getCursor();
        if (cursor < 0) cursor = 0;
        fse.setCursor(cursor);

        // send the event to the default server
        String serverName = interInfo.getDefaultServerInfo().getServerName();
        UUID serverUuid = null;
        boolean result = CMEventManager.unicastEvent(fse, serverName, serverUuid);
        if (!result) {
            System.err.println("CMFileSyncManager.startPullSync(), send error!");
            System.err.println(fse);
            return false;
        }

        // change the sync session state to PULL
        syncInfo.setSyncProgress(CMFileSyncProgress.PULL);

        return true;
    }

    // [10-3] called at the client: busy 로 push 가 거절됐을 때의 fallback pull 재시도 타이머 예약(§2.6).
    // FILE_SYNC_PUSH_LEASE_TIMEOUT(초) 뒤 startPullSync() 를 1회 실행한다. SYNC_NEEDED_NOTIFY 가 먼저 오면
    // processSYNC_NEEDED_NOTIFY 가 이 타이머를 취소한다. 발화 시의 pull 재시도가 죽은 owner lease 의
    // lazy 회수(다음 tryAcquire) 트리거도 겸한다. 기존 대기 타이머는 취소 후 재예약(중복 방지).
    public void scheduleBusyRetryPull() {
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        long timeoutSec = CMConfigurationInfo.getInstance().getFileSyncPushLeaseTimeout();

        syncInfo.cancelBusyRetryFuture();
        ScheduledExecutorService ses = CMThreadInfo.getInstance().getScheduledExecutorService();
        ScheduledFuture<?> future = ses.schedule(() -> {
            if (CMInfo._CM_DEBUG) {
                System.out.println("=== busy fallback timer fired -> startPullSync()");
            }
            if (!startPullSync()) {
                System.err.println("CMFileSyncManager.scheduleBusyRetryPull(), fallback startPullSync failed.");
            }
        }, timeoutSec, TimeUnit.SECONDS);
        syncInfo.setBusyRetryFuture(future);

        if (CMInfo._CM_DEBUG) {
            System.out.println("busy fallback pull scheduled in " + timeoutSec + "s.");
        }
    }

    // Entry point for incremental PUSH: snapshots the client's pendingPushMap (files the client
    // holds newer info for after a PULL) into pushEntryList and starts the push session by sending
    // START_PUSH_ENTRY_LIST to the server. Returns once the session is initiated (START sent);
    // entry-body transfer, per-op processing and session completion are driven by event handlers
    // on both sides.
    // Called from processCOMPLETE_PULL_SYNC after the PULL session is fully cleaned up and
    // pendingPushMap is non-empty. A future file-watcher trigger (startPushSync()) will call this
    // (or a derived helper) as well.
    // syncProgress=PUSH is NOT set here — the caller sets it before calling (mirrors the PULL-
    // completion branch). Runs on the single event-processing thread, so no synchronization.
    public boolean proceedPendingPushMap() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.proceedPendingPushMap() called..");

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();

        // only the client can start a push sync
        if (!confInfo.getSystemType().equals("CLIENT")) {
            System.err.println("CMFileSyncManager.proceedPendingPushMap(), system type is not CLIENT!");
            return false;
        }

        // defensive empty-map guard (normal callers already check; keeps standalone calls safe,
        // e.g. a future file-watcher trigger). empty => nothing to push = already in sync.
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        if (pendingPushMap == null || pendingPushMap.isEmpty()) {
            if (CMInfo._CM_DEBUG)
                System.out.println("pendingPushMap is empty. nothing to push.");
            return true;
        }

        // guard against starting a second push session while one is already in progress
        if (syncInfo.getPushEntryList() != null) {
            System.err.println("CMFileSyncManager.proceedPendingPushMap(), pushEntryList already exists; "
                    + "another push session in progress. skip.");
            return false;
        }

        // snapshot pendingPushMap.values() into pushEntryList and store it.
        // pendingPushMap itself is NOT cleared here — it is the retry truth, kept until the push
        // session commits (processCOMPLETE_PUSH_SYNC). Being a snapshot, entries added to
        // pendingPushMap by a file-watcher mid-session do not affect this session.
        List<CMFileSyncClientEntry> pushEntryList = new ArrayList<>(pendingPushMap.values());
        syncInfo.setPushEntryList(pushEntryList);
        if (CMInfo._CM_DEBUG)
            System.out.println("pushEntryList snapshot created. size = " + pushEntryList.size());

        // create and send START_PUSH_ENTRY_LIST
        CMFileSyncEventStartPushEntryList fse_spel = new CMFileSyncEventStartPushEntryList();
        fse_spel.setInitiatorName(interInfo.getMyself().getName());
        fse_spel.setInitiatorUuid(interInfo.getMyself().getUuid());
        fse_spel.setInitiatorDeviceUuid(syncInfo.getDeviceUuid());
        fse_spel.setNumTotalFiles(pushEntryList.size());

        String serverName = interInfo.getDefaultServerInfo().getServerName();
        boolean sendResult = CMEventManager.unicastEvent(fse_spel, serverName, null);
        if (!sendResult) {
            System.err.println("CMFileSyncManager.proceedPendingPushMap(), failed to send START_PUSH_ENTRY_LIST.");
            // send failed => roll back the snapshot so the next attempt isn't blocked by the guard
            syncInfo.setPushEntryList(null);
            return false;
        }
        if (CMInfo._CM_DEBUG)
            System.out.println("START_PUSH_ENTRY_LIST sent. numTotalFiles = " + pushEntryList.size());

        return true;
    }

    // WatchService-triggered PUSH entry point. Called from CMWatchServiceTask.run() after the 500ms
    // quiet window with a candidateMap that classifies the detected paths into CREATE/MODIFY/DELETE
    // (see CMWatchServiceTask.buildPushCandidateMap). Replaces the previous full-push trigger.
    // synchronized: invoked on the WatchService thread; serializes against other entry points
    // (startFullPushSync / startPullSync) on the same manager instance.
    // Returns false when the session cannot be started (already in progress, send failure, etc.) —
    // the caller interprets false as "unhandled changes remain" and sets fileChangeDetected=true so
    // a future cycle retries.
    public synchronized boolean startPushSync(Map<String, CMFileSyncClientEntry> candidateMap) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.startPushSync() called..");

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();

        if (!confInfo.getSystemType().equals("CLIENT")) {
            System.err.println("CMFileSyncManager.startPushSync(), not a CLIENT.");
            return false;
        }

        // Session-in-progress guard: any non-NONE syncProgress (FULL_SYNC / PULL / PUSH /
        // ONLINE_MODE / LOCAL_MODE) blocks a new push start. Caller retries via fileChangeDetected.
        if (syncInfo.getSyncProgress() != CMFileSyncProgress.NONE) {
            System.err.println("CMFileSyncManager.startPushSync(), sync already in progress: "
                    + syncInfo.getSyncProgress());
            return false;
        }

        // Empty candidate map (all events filtered out, or none classified) — nothing to push.
        if (candidateMap == null || candidateMap.isEmpty()) {
            if (CMInfo._CM_DEBUG)
                System.out.println("CMFileSyncManager.startPushSync(), candidateMap is empty. "
                        + "nothing to push.");
            return true;
        }

        // Replace pendingPushMap with the candidate snapshot. WatchService-trigger push has no
        // continuity with prior sessions: the current disk state after the 500ms quiet window is
        // the truth, so stale leftovers from a failed previous trigger are overwritten.
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        pendingPushMap.clear();
        pendingPushMap.putAll(candidateMap);
        if (CMInfo._CM_DEBUG) {
            System.out.println("pendingPushMap populated. size = " + pendingPushMap.size());
            pendingPushMap.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
        }

        // Officially open the PUSH session before delegating to the shared sender.
        syncInfo.setSyncProgress(CMFileSyncProgress.PUSH);

        boolean ret = proceedPendingPushMap();
        if (!ret) {
            // proceedPendingPushMap() already rolled back pushEntryList on send failure; restore
            // syncProgress so the next watch-trigger can pass the guard.
            System.err.println("CMFileSyncManager.startPushSync(), proceedPendingPushMap() failed. "
                    + "rolling back syncProgress.");
            syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
            return false;
        }
        return true;
    }

    // Server-side entry point for op-by-op processing of pushStateTable[stateKey].
    // Called from processEND_PUSH_ENTRY_LIST after the entry-count verification passes.
    // Internal op-specific helpers (DELETE → CREATE → MODIFY) are implemented incrementally.
    public boolean proceedPushStateMap(CMFileSyncStateKey stateKey, UUID initiatorUuid) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.proceedPushStateMap() called..");
            System.out.println("stateKey = " + stateKey + ", initiatorUuid = " + initiatorUuid);
        }

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if (!confInfo.getSystemType().equals("SERVER")) {
            System.err.println("CMFileSyncManager.proceedPushStateMap(), not a SERVER.");
            return false;
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<String, CMFileSyncClientEntry> pushStateMap = syncInfo.getPushStateTable().get(stateKey);
        if (pushStateMap == null) {
            System.err.println("CMFileSyncManager.proceedPushStateMap(), pushStateMap is null for stateKey = "
                    + stateKey);
            return false;
        }
        if (pushStateMap.isEmpty()) {
            if (CMInfo._CM_DEBUG)
                System.out.println("pushStateMap is empty. nothing to proceed.");
            return true;
        }

        // 단순 opHint 기반 분류. 풀 트리아지(classifyEntriesForPush — 서버 파일/server-index/클라 메타 3자 비교 +
        // 충돌 rename)는 별도 단계에서 본 분류를 대체할 예정.
        List<CMFileSyncClientEntry> deleteEntries = new ArrayList<>();
        List<CMFileSyncClientEntry> createEntries = new ArrayList<>();
        List<CMFileSyncClientEntry> modifyEntries = new ArrayList<>();
        for (CMFileSyncClientEntry entry : pushStateMap.values()) {
            CMFileSyncOp op = entry.getOpHint();
            if (op == CMFileSyncOp.DELETE) {
                deleteEntries.add(entry);
            } else if (op == CMFileSyncOp.CREATE) {
                createEntries.add(entry);
            } else if (op == CMFileSyncOp.MODIFY) {
                modifyEntries.add(entry);
            } else {
                // UNKNOWN 등은 풀 트리아지 단계에서 결정될 분기. 현재는 skip + 로그.
                System.err.println("CMFileSyncManager.proceedPushStateMap(), unclassified opHint for path = "
                        + entry.getPath() + ", op = " + op);
            }
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("classification: DELETE=" + deleteEntries.size()
                    + ", CREATE=" + createEntries.size()
                    + ", MODIFY=" + modifyEntries.size());
        }

        boolean result = true;
        if (!deleteEntries.isEmpty()) {
            boolean dResult = proceedPushDeleteEntries(stateKey, initiatorUuid, deleteEntries);
            if (!dResult) {
                System.err.println("CMFileSyncManager.proceedPushStateMap(), failed in proceedPushDeleteEntries.");
            }
            result &= dResult;
        }
        if (!createEntries.isEmpty()) {
            boolean cResult = proceedPushCreateEntries(stateKey, initiatorUuid, createEntries);
            if (!cResult) {
                System.err.println("CMFileSyncManager.proceedPushStateMap(), failed in proceedPushCreateEntries.");
            }
            result &= cResult;
        }
        if (!modifyEntries.isEmpty()) {
            boolean mResult = proceedPushModifyEntries(stateKey, initiatorUuid, modifyEntries);
            if (!mResult) {
                System.err.println("CMFileSyncManager.proceedPushStateMap(), failed in proceedPushModifyEntries.");
            }
            result &= mResult;
        }
        return result;
    }

    // All entries marked isCompleted == true. Mirrors isCompletePullSync.
    public boolean isCompletePushSync(Map<String, CMFileSyncClientEntry> pushStateMap) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.isCompletePushSync() called..");
        if (pushStateMap == null || pushStateMap.isEmpty()) return false;
        for (CMFileSyncClientEntry entry : pushStateMap.values()) {
            if (!entry.isCompleted()) return false;
        }
        return true;
    }

    // PUSH 세션 종료 트랜잭션 (changelog append + cursor 갱신 + flushSnapshot + COMPLETE_PUSH_SYNC 송신).
    // 호출 시점: 서버측, 각 op 완료 핸들러 안에서 isCompletePushSync(pushStateMap) == true 직후.
    // pushStateTable.remove는 본 메소드 책임 아님 — COMPLETE_PUSH_SYNC_ACK 수신 시점에 정리.
    // 이유: PUSH 트랜잭션의 truth는 서버 cursor. 클라가 newServerCursor 적용을 ACK로 확인할 때까지 세션 메타 보존.
    public boolean completePushSync(CMFileSyncStateKey stateKey, UUID initiatorUuid) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completePushSync() called..");
            System.out.println("stateKey = " + stateKey + ", initiatorUuid = " + initiatorUuid);
        }

        // (1) 시스템 타입 가드
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if (!confInfo.getSystemType().equals("SERVER")) {
            System.err.println("CMFileSyncManager.completePushSync(), not a SERVER.");
            return false;
        }

        // (2) syncInfo + initiator 식별 (finally 의 lease release 에서 참조하므로 try 밖에서 확보)
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        String initiatorName = stateKey.initiatorName();
        UUID initiatorDeviceUuid = stateKey.initiatorDeviceUuid();

        long newServerCursor = -1;
        boolean committed = false;      // commit 성공 여부 (fan-out 게이트)
        boolean sendResult = false;     // COMPLETE_PUSH_SYNC 송신 결과 (반환값)

        // [10-3] push 세션 lease 는 이 시점(commit 핸들러 도달)까지 보유 중. 정상/예외/early-return 무관하게
        // finally 에서 반드시 해제해 commit 실패 stall 을 timeout 없이 즉시 회수한다(§2.6). fan-out 은 release 이후.
        try {
            // (2') pushStateMap lookup — 제거 없이 lookup만 (ACK 핸들러가 정리)
            Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pushStateTable = syncInfo.getPushStateTable();
            Map<String, CMFileSyncClientEntry> pushStateMap = pushStateTable.get(stateKey);
            if (pushStateMap == null) {
                System.err.println("CMFileSyncManager.completePushSync(), "
                        + "pushStateMap not found for stateKey: " + stateKey);
                return false;
            }

            // (3) indexRepository 확보 (in-memory cursor truth)
            CMFileSyncIndexRepository indexRepository = syncInfo.getIndexRegistry()
                    .getOrLoad(initiatorName, initiatorDeviceUuid);

            // (4) 트랜잭션 commit: (i) batch changelog append, (ii) writeCursor, (iii) flushSnapshot
            List<CMFileSyncChangeLogEntry> opRecords = syncInfo.getPushOpRecordTable().get(stateKey);
            if (opRecords == null) {
                System.err.println("CMFileSyncManager.completePushSync(), "
                        + "pushOpRecordTable missing for stateKey: " + stateKey);
                return false;
            }
            try {
                // (i) 누적된 op record 일괄 changelog append
                syncInfo.appendChangelogBatch(initiatorName, opRecords);
                // (ii) cursor 파일 갱신 — 인메모리 lastChangeId는 op별 apply로 이미 누적된 최종값
                newServerCursor = indexRepository.lastChangeId();
                syncInfo.writeCursor(initiatorName, initiatorDeviceUuid, newServerCursor);
                // (iii) snapshot 영속화
                indexRepository.flushSnapshot();
            } catch (IOException e) {
                System.err.println("CMFileSyncManager.completePushSync(), commit failed.");
                e.printStackTrace();
                // 영속화 실패 시 클라에 세션 완료 통보하지 않음. 인메모리 갱신은 그대로 두고 후속 재시도 정책 대상.
                return false;
            }
            committed = true;   // COMPLETE_PUSH_SYNC 송신이 실패해도 commit 은 권위 → fan-out 수행

            if (CMInfo._CM_DEBUG) {
                System.out.println("commit succeeded. newServerCursor = " + newServerCursor);
            }

            // (5) COMPLETE_PUSH_SYNC 송신
            CMFileSyncEventCompletePushSync fse_cps = new CMFileSyncEventCompletePushSync();
            fse_cps.setInitiatorName(initiatorName);
            fse_cps.setInitiatorUuid(initiatorUuid);
            fse_cps.setInitiatorDeviceUuid(initiatorDeviceUuid);
            fse_cps.setNumFilesCompleted(pushStateMap.size());
            fse_cps.setNewServerCursor(newServerCursor);

            sendResult = CMEventManager.unicastEvent(fse_cps, initiatorName, initiatorUuid);
            if (!sendResult) {
                System.err.println("CMFileSyncManager.completePushSync(), failed to send COMPLETE_PUSH_SYNC.");
                // commit은 이미 성공. 다음 START_PULL_SYNC에서 cursor 비교로 클라가 자동 따라옴.
            } else if (CMInfo._CM_DEBUG) {
                System.out.println("COMPLETE_PUSH_SYNC sent. numFilesCompleted = " + pushStateMap.size()
                        + ", newServerCursor = " + newServerCursor);
            }
        } finally {
            // [10-3] 정상/예외/early-return 무관하게 lease 해제(§2.6). 소유자가 아니면 no-op.
            syncInfo.releasePushLease(initiatorName, stateKey);
        }

        // (6) [10-3] fan-out: release 이후 · commit 성공 시에만(§3.2). 같은 사용자의 다른 온라인 디바이스에
        // SYNC_NEEDED_NOTIFY 통지(3.1). commit 이 권위 상태이므로 COMPLETE_PUSH_SYNC 응답/ACK 와 순서 무관.
        // 통지는 힌트이며 전파 정합성은 수신측 pull(clientCursor <-> changelogHead)이 보장(유실/중복 안전).
        if (committed) {
            notifySyncNeededToOtherDevices(initiatorName, initiatorUuid, initiatorDeviceUuid, newServerCursor);
        }

        return sendResult;
    }

    // [10-3] push commit 후 같은 사용자의 다른 온라인 디바이스에 변경 통지를 fan-out 한다(3.1, 3.2).
    // origin 은 event.initiatorUuid(=A 의 login session UUID)로 식별해 제외한다. 통지 이벤트의 공통 헤더
    // initiator* 는 "cause = A"로 채운다(송신자는 서버지만 의미상 원인은 A). changelogHead 는 이번 commit
    // 으로 확정된 새 head(= newServerCursor).
    private void notifySyncNeededToOtherDevices(String initiatorName, UUID initiatorUuid,
                                                UUID initiatorDeviceUuid, long changelogHead) {
        CMMember loginUsers = CMInteractionInfo.getInstance().getLoginUsers();
        List<CMUser> userLogins = loginUsers.findMemberList(initiatorName);
        if (userLogins == null || userLogins.isEmpty()) return;

        for (CMUser login : userLogins) {
            // login session UUID 비교로 변경 디바이스(A) 자신 제외. 드물게 같은 device 의 다른 session 에
            // 통지가 가더라도 수신측 pull 이 rc 1(no-op)로 귀결되어 정합성에 영향 없다(3.1).
            if (login.getUuid() != null && login.getUuid().equals(initiatorUuid)) continue;

            CMFileSyncEventSyncNeededNotify notify = new CMFileSyncEventSyncNeededNotify();
            notify.setInitiatorName(initiatorName);
            notify.setInitiatorUuid(initiatorUuid);
            notify.setInitiatorDeviceUuid(initiatorDeviceUuid);
            notify.setChangelogHead(changelogHead);

            if (!CMEventManager.unicastEvent(notify, initiatorName, login.getUuid())) {
                System.err.println("CMFileSyncManager.notifySyncNeededToOtherDevices(), "
                        + "failed to send SYNC_NEEDED_NOTIFY to session: " + login.getUuid());
            } else if (CMInfo._CM_DEBUG) {
                System.out.println("SYNC_NEEDED_NOTIFY sent to session " + login.getUuid()
                        + ", changelogHead = " + changelogHead);
            }
        }
    }

    // Server-side CREATE trigger.
    // - Directory entries: synchronously created on the server's sync home + in-memory server-index
    //   updated + pushStateMap.isCompleted=true + a COMPLETE_PUSH_CREATE event sent (one per dir).
    // - File entries: a REQUEST_NEW_FILES batch is sent to the client (MAX_EVENT_SIZE-bounded).
    //   The actual file receipt, server-index update, isCompleted marking, and COMPLETE_PUSH_CREATE
    //   send happen later in the server's processEND_FILE_TRANSFER PUSH-CREATE branch (separate step).
    // If only directories were in createEntries and the session is now fully complete, the
    // completion transaction is entered at the end.
    public boolean proceedPushCreateEntries(CMFileSyncStateKey stateKey, UUID initiatorUuid,
                                            List<CMFileSyncClientEntry> createEntries) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.proceedPushCreateEntries() called..");
            System.out.println("stateKey = " + stateKey + ", initiatorUuid = " + initiatorUuid
                    + ", createEntries.size = " + createEntries.size());
        }

        String initiatorName = stateKey.initiatorName();
        UUID initiatorDeviceUuid = stateKey.initiatorDeviceUuid();
        Path serverSyncHome = getServerSyncHome(initiatorName);
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncIndexRepository indexRepository =
                syncInfo.getIndexRegistry().getOrLoad(initiatorName, initiatorDeviceUuid);
        Map<String, CMFileSyncClientEntry> pushStateMap = syncInfo.getPushStateTable().get(stateKey);
        if (pushStateMap == null) {
            System.err.println("CMFileSyncManager.proceedPushCreateEntries(), "
                    + "pushStateMap not found for stateKey: " + stateKey);
            return false;
        }
        List<CMFileSyncChangeLogEntry> opRecords = syncInfo.getPushOpRecordTable().get(stateKey);
        if (opRecords == null) {
            System.err.println("CMFileSyncManager.proceedPushCreateEntries(), "
                    + "pushOpRecordTable missing for stateKey: " + stateKey);
            return false;
        }

        boolean result = true;
        long now = System.currentTimeMillis() / 1000L;

        // (1) 디렉토리/파일 entry 분리 (pushStateMap 가드 동시 수행)
        List<CMFileSyncClientEntry> dirEntries = new ArrayList<>();
        List<CMFileSyncClientEntry> fileEntries = new ArrayList<>();
        for (CMFileSyncClientEntry entry : createEntries) {
            if (pushStateMap.get(entry.getPath()) == null) {
                System.err.println("CMFileSyncManager.proceedPushCreateEntries(), "
                        + "entry not found in pushStateMap: " + entry.getPath());
                result = false;
                continue;
            }
            if (entry.isDirectory()) {
                dirEntries.add(entry);
            } else {
                fileEntries.add(entry);
            }
        }

        // (2) 디렉토리 entry → 서버측 동기 처리 (deferred-commit: 파일 영속화는 completePushSync)
        boolean anyDirectoryCompleted = false;
        for (CMFileSyncClientEntry entry : dirEntries) {
            String relPathStr = entry.getPath();
            Path absPath = serverSyncHome.resolve(relPathStr).toAbsolutePath().normalize();
            try {
                Files.createDirectories(absPath);
            } catch (IOException e) {
                System.err.println("CMFileSyncManager.proceedPushCreateEntries(), "
                        + "error creating directory: " + absPath);
                e.printStackTrace();
                result = false;
                continue;
            }
            if (CMInfo._CM_DEBUG) System.out.println("created directory: " + absPath);

            long newChangeId;   // [10-3] 전역 할당 (dual-writer (i): 증분 push dir CREATE)
            try {
                newChangeId = syncInfo.allocateNextChangeId(initiatorName);
            } catch (IOException e) {
                System.err.println("CMFileSyncManager.proceedPushCreateEntries(), "
                        + "failed to allocate changeId for dir: " + relPathStr);
                e.printStackTrace();
                result = false;
                continue;
            }
            indexRepository.applyCreateOrModify(relPathStr, true, null, now, 0L, newChangeId);

            // 10-2 doc 12015~12016: 디렉토리 분기 applyCreateOrModify 직후 record add.
            // 기존 CMFileSyncInfo.appendChangelog CREATE 호출 형태 미러 — 디렉토리는 contentHash=null, size=0L.
            opRecords.add(new CMFileSyncChangeLogEntry()
                    .setChangeId(newChangeId)
                    .setUserName(initiatorName)
                    .setOriginDeviceUuid(initiatorDeviceUuid)
                    .setOp(CMFileSyncOp.CREATE)
                    .setPath(relPathStr)
                    .setDirectory(true)
                    .setContentHash(null)
                    .setMtime(now)
                    .setSize(0L)
                    .setTombstone(false)
                    .setTs(OffsetDateTime.now()));

            pushStateMap.get(relPathStr).setCompleted(true);
            anyDirectoryCompleted = true;

            CMFileSyncEventCompletePushCreate fse_cpc = new CMFileSyncEventCompletePushCreate();
            fse_cpc.setInitiatorName(initiatorName);
            fse_cpc.setInitiatorUuid(initiatorUuid);
            fse_cpc.setInitiatorDeviceUuid(initiatorDeviceUuid);
            fse_cpc.setCreatedPath(relPathStr);
            if (!CMEventManager.unicastEvent(fse_cpc, initiatorName, initiatorUuid)) {
                System.err.println("CMFileSyncManager.proceedPushCreateEntries(), "
                        + "failed to send COMPLETE_PUSH_CREATE for dir: " + relPathStr);
                result = false;
                // 디렉토리는 이미 isCompleted=true로 마킹됨 → 세션 진전 유지
            }
        }

        // (3) 파일 entry → REQUEST_NEW_FILES batch 송신
        //     (full sync requestTransferOfNewFiles 패턴 차용)
        int numRequestsCompleted = 0;
        while (numRequestsCompleted < fileEntries.size()) {
            CMFileSyncEventRequestNewFiles fse_rnf = new CMFileSyncEventRequestNewFiles();
            fse_rnf.setInitiatorName(initiatorName);
            fse_rnf.setInitiatorUuid(initiatorUuid);
            fse_rnf.setInitiatorDeviceUuid(initiatorDeviceUuid);

            int curByteNum = fse_rnf.getByteNum();
            List<Path> requestedFileList = new ArrayList<>();
            int numRequestedFiles = 0;

            while (numRequestsCompleted < fileEntries.size() && curByteNum < CMInfo.MAX_EVENT_SIZE) {
                Path relPath = Paths.get(fileEntries.get(numRequestsCompleted).getPath());
                curByteNum += CMInfo.STRING_LEN_BYTES_LEN + relPath.toString().getBytes().length;
                if (curByteNum < CMInfo.MAX_EVENT_SIZE) {
                    requestedFileList.add(relPath);
                    numRequestedFiles++;
                    numRequestsCompleted++;
                } else {
                    break;
                }
            }
            fse_rnf.setNumRequestedFiles(numRequestedFiles);
            fse_rnf.setRequestedFileList(requestedFileList);

            if (!CMEventManager.unicastEvent(fse_rnf, initiatorName, initiatorUuid)) {
                System.err.println("CMFileSyncManager.proceedPushCreateEntries(), "
                        + "failed to send REQUEST_NEW_FILES.");
                return false;
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("sent REQUEST_NEW_FILES: numRequestedFiles=" + numRequestedFiles);
            }
        }

        // (4) 디렉토리만으로 세션 완료 가능 케이스 체크
        // (createEntries가 모두 디렉토리이고 DELETE도 모두 완료된 케이스)
        if (anyDirectoryCompleted && isCompletePushSync(pushStateMap)) {
            boolean completeResult = completePushSync(stateKey, initiatorUuid);
            result &= completeResult;
        }

        return result;
    }

    // 10-2 doc 11259: PUSH 세션의 MODIFY 분기 진입점.
    // 책임: SERVER 가드 → pushStateMap 존재성 검증 → pushGeneratorMap 중복 등록 검사 →
    //      CMFileSyncPushGenerator 생성·등록 → ExecutorService 워커 위임 → 즉시 true 반환.
    // 디스크 I/O 없음. 무거운 작업(블록 체크섬 계산 + START_FILE_BLOCK_CHECKSUM 송신)은
    // 워커 스레드(CMFileSyncPushGenerator.run())에서. caller(proceedPushStateMap)가 modifyEntries 빈 가드.
    // entry별 isCompleted 마킹·COMPLETE_PUSH_MODIFY 송신·세션 종료 진입은
    // processEND_FILE_BLOCK_CHECKSUM_ACK PUSH 분기(F-6, 별도 단계) 책임.
    public boolean proceedPushModifyEntries(CMFileSyncStateKey stateKey, UUID initiatorUuid,
                                            List<CMFileSyncClientEntry> modifyEntries) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.proceedPushModifyEntries() called..");
            System.out.println("stateKey = " + stateKey + ", initiatorUuid = " + initiatorUuid
                    + ", modifyEntries.size = " + modifyEntries.size());
        }

        // (1) SERVER 가드
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if (!confInfo.getSystemType().equals("SERVER")) {
            System.err.println("CMFileSyncManager.proceedPushModifyEntries(), not a SERVER.");
            return false;
        }

        // (2) pushStateMap 존재성 검증
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<String, CMFileSyncClientEntry> pushStateMap = syncInfo.getPushStateTable().get(stateKey);
        if (pushStateMap == null) {
            System.err.println("CMFileSyncManager.proceedPushModifyEntries(), "
                    + "pushStateMap not found for stateKey: " + stateKey);
            return false;
        }

        // (3) pushGeneratorMap 중복 등록 검사 (정상 흐름이라면 F-6에서 cleanup 완료, 잔재는 비정상 신호)
        String initiatorName = stateKey.initiatorName();
        UUID initiatorDeviceUuid = stateKey.initiatorDeviceUuid();
        CMUserLoginKey loginKey = new CMUserLoginKey(initiatorName, initiatorUuid);
        Map<CMUserLoginKey, CMFileSyncPushGenerator> pushGeneratorMap = syncInfo.getPushGeneratorMap();
        if (pushGeneratorMap.containsKey(loginKey)) {
            System.err.println("CMFileSyncManager.proceedPushModifyEntries(), "
                    + "pushGenerator already exists for loginKey: " + loginKey);
            return false;
        }

        // (4) PushGenerator 인스턴스 생성 (PullGenerator와 동일 패턴 — 분해 식별자)
        CMFileSyncPushGenerator pushGenerator = new CMFileSyncPushGenerator(
                initiatorName, initiatorUuid, initiatorDeviceUuid, modifyEntries);

        // (5) loginKey로 등록
        pushGeneratorMap.put(loginKey, pushGenerator);
        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncPushGenerator created and registered for loginKey = " + loginKey
                    + ", modifyEntries.size = " + modifyEntries.size());
        }

        // (6) ExecutorService 워커에 위임 (이벤트 처리 스레드 비블로킹)
        ExecutorService es = CMThreadInfo.getInstance().getExecutorService();
        es.submit(pushGenerator);
        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncPushGenerator submitted to ExecutorService.");
        }

        // (7) 트리거 성공 반환 — 본 메소드 반환 시점에 entry별 처리 결과 미확정.
        //     실제 실패는 isCompleted 누락 → 세션 자동 미완료로 표면화.
        return true;
    }

    // Server-side DELETE handler: deletes the file on disk and updates the in-memory server-index
    // tombstone synchronously, then notifies the client with COMPLETE_PUSH_DELETE (split if needed).
    // Each processed entry is marked isCompleted=true in pushStateMap.
    // Persistence (changelog/cursor/snapshot) is deferred to completePushSync — same deferred-commit
    // policy as proceedConflictedServerEntry.
    public boolean proceedPushDeleteEntries(CMFileSyncStateKey stateKey, UUID initiatorUuid,
                                            List<CMFileSyncClientEntry> deleteEntries) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.proceedPushDeleteEntries() called..");
            System.out.println("stateKey = " + stateKey + ", initiatorUuid = " + initiatorUuid
                    + ", deleteEntries.size = " + deleteEntries.size());
        }

        String initiatorName = stateKey.initiatorName();
        UUID initiatorDeviceUuid = stateKey.initiatorDeviceUuid();
        Path serverSyncHome = getServerSyncHome(initiatorName);
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncIndexRepository indexRepository =
                syncInfo.getIndexRegistry().getOrLoad(initiatorName, initiatorDeviceUuid);
        Map<String, CMFileSyncClientEntry> pushStateMap = syncInfo.getPushStateTable().get(stateKey);
        if (pushStateMap == null) {
            System.err.println("CMFileSyncManager.proceedPushDeleteEntries(), "
                    + "pushStateMap not found for stateKey: " + stateKey);
            return false;
        }
        List<CMFileSyncChangeLogEntry> opRecords = syncInfo.getPushOpRecordTable().get(stateKey);
        if (opRecords == null) {
            System.err.println("CMFileSyncManager.proceedPushDeleteEntries(), "
                    + "pushOpRecordTable missing for stateKey: " + stateKey);
            return false;
        }

        List<String> deletedPathList = new ArrayList<>();
        boolean result = true;
        long now = System.currentTimeMillis() / 1000L;

        // (1) entry별 디스크 삭제 + 인메모리 tombstone + isCompleted 마킹
        for (CMFileSyncClientEntry entry : deleteEntries) {
            String relPathStr = entry.getPath();
            Path absPath = serverSyncHome.resolve(relPathStr).toAbsolutePath().normalize();

            // 삭제 시점에는 디스크 파일이 이미 사라질 수 있어 인덱스를 truth로 사용
            CMFileSyncIndexEntry indexEntry = indexRepository.readEntry(relPathStr);
            boolean isDirectory = (indexEntry != null) && indexEntry.isDirectory();

            try {
                Files.deleteIfExists(absPath);
            } catch (IOException e) {
                System.err.println("CMFileSyncManager.proceedPushDeleteEntries(), "
                        + "error deleting file: " + absPath);
                e.printStackTrace();
                result = false;
                // isCompleted 미마킹 → 세션은 미완료 상태 유지
                continue;
            }
            if (CMInfo._CM_DEBUG) System.out.println("deleted: " + absPath);

            // applyDelete 내부에서 Math.max(lastChangeId, ...)로 in-memory cursor 자동 전진
            long newChangeId;   // [10-3] 전역 할당 (dual-writer (i): 증분 push DELETE)
            try {
                newChangeId = syncInfo.allocateNextChangeId(initiatorName);
            } catch (IOException e) {
                System.err.println("CMFileSyncManager.proceedPushDeleteEntries(), "
                        + "failed to allocate changeId for: " + relPathStr);
                e.printStackTrace();
                result = false;
                continue;
            }
            indexRepository.applyDelete(relPathStr, isDirectory, newChangeId, now);

            // 10-2 doc 12013: applyDelete 직후 record add. 영속화는 completePushSync에서 일괄.
            // 기존 CMFileSyncInfo.appendChangelog DELETE 호출 형태(contentHash=null, mtime=now, size=0L) 미러.
            opRecords.add(new CMFileSyncChangeLogEntry()
                    .setChangeId(newChangeId)
                    .setUserName(initiatorName)
                    .setOriginDeviceUuid(initiatorDeviceUuid)
                    .setOp(CMFileSyncOp.DELETE)
                    .setPath(relPathStr)
                    .setDirectory(isDirectory)
                    .setContentHash(null)
                    .setMtime(now)
                    .setSize(0L)
                    .setTombstone(true)
                    .setTs(OffsetDateTime.now()));

            CMFileSyncClientEntry pushEntry = pushStateMap.get(relPathStr);
            if (pushEntry != null) {
                pushEntry.setCompleted(true);
            } else {
                System.err.println("CMFileSyncManager.proceedPushDeleteEntries(), "
                        + "entry not found in pushStateMap: " + relPathStr);
                // 에러 출력 후 계속 진행 (processCOMPLETE_PULL_DELETE 정책과 동일)
            }
            deletedPathList.add(relPathStr);
        }

        // (2) COMPLETE_PUSH_DELETE 송신 — byte 한도 초과 시 분할
        if (!deletedPathList.isEmpty()) {
            int listIndex = 0;
            while (listIndex < deletedPathList.size()) {
                CMFileSyncEventCompletePushDelete fse_cpd = new CMFileSyncEventCompletePushDelete();
                fse_cpd.setInitiatorName(initiatorName);
                fse_cpd.setInitiatorUuid(initiatorUuid);
                fse_cpd.setInitiatorDeviceUuid(initiatorDeviceUuid);

                List<String> subList = createSubStringListForEvent(fse_cpd.getByteNum(),
                        deletedPathList, listIndex);
                listIndex += subList.size();
                fse_cpd.setDeletedPathList(subList);

                boolean sendResult = CMEventManager.unicastEvent(fse_cpd, initiatorName, initiatorUuid);
                if (!sendResult) {
                    System.err.println("CMFileSyncManager.proceedPushDeleteEntries(), "
                            + "failed to send COMPLETE_PUSH_DELETE: subList.size = " + subList.size());
                    return false;
                }
                if (CMInfo._CM_DEBUG) {
                    System.out.println("sent COMPLETE_PUSH_DELETE: subList.size = " + subList.size());
                }
            }
        }

        // (3) 세션 전체 완료 시 종료 트랜잭션 진입
        if (isCompletePushSync(pushStateMap)) {
            boolean completeResult = completePushSync(stateKey, initiatorUuid);
            result &= completeResult;
        }

        return result;
    }

    // called by client; compares the received serverEntryList against the local clientPathList
    // and classifies each entry into the pull maps (delete/create/modify) or the pending push map.
    public boolean compareServerAndClientEntriesForPullSync() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.compareServerAndClientEntriesForPullSync() called..");

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        List<CMFileSyncChangeLogEntry> serverEntryList = syncInfo.getServerEntryList();
        List<Path> clientPathList = syncInfo.getClientPathList();
        if (serverEntryList == null || clientPathList == null) {
            System.err.println("CMFileSyncManager.compareServerAndClientEntriesForPullSync(), " +
                    "serverEntryList or clientPathList is null!");
            return false;
        }

        Path clientSyncHome = getClientSyncHome();
        Map<String, CMFileSyncClientEntry> pullModifyMap = syncInfo.getPullModifyMap();
        Map<String, CMFileSyncClientEntry> pullCreateMap = syncInfo.getPullCreateMap();
        Map<String, CMFileSyncClientEntry> pullDeleteMap = syncInfo.getPullDeleteMap();
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        String serverName = CMInteractionInfo.getInstance().getDefaultServerInfo().getServerName();

        try {
            // classify each server entry by op
            for (CMFileSyncChangeLogEntry serverEntry : serverEntryList) {
                String relPathStr = serverEntry.getPath();      // relative path (server & client)
                Path relPath = Path.of(relPathStr);
                // ignore 패턴 매칭 시 동기화 대상에서 제외 (.DS_Store 등).
                // 현 정책에선 클라 createPathList 가 이미 ignore 필터링해 서버 changelog 에 들어가지 않으므로
                // 정상 흐름에선 unreachable. 향후 per-user/per-device ignore 정책 확장(서로 다른 디바이스가
                // 같은 path 를 다르게 ignore) 시 도달 가능하므로 ack 송신해 서버 hang 방지.
                if (syncInfo.isIgnored(relPath)) {
                    if (CMInfo._CM_DEBUG)
                        System.out.println("ignored server entry: " + relPathStr);
                    ackSkippedPullEntry(relPathStr, serverEntry.getOp(), serverName);
                    continue;
                }
                Path absPath = clientSyncHome.resolve(relPath); // client absolute path
                long baseMtime = syncInfo.getLastSyncedMtime(relPathStr);
                long curMtime = syncInfo.currentMtimeSecOrMinusOne(absPath);
                long curSize = syncInfo.currentSizeOrMinusOne(absPath);
                long serverMtime = serverEntry.getMtime();
                boolean existsOnClient = clientPathList.contains(absPath);
                boolean isDir = serverEntry.isDirectory();
                // 클라 파일이 이미 서버 버전과 동일한가: 파일은 mtime+size, 디렉토리는 존재만으로 판정.
                // index 유실(client-index.json 삭제)로 baseMtime 이 -1 이어도 "서버와 동일"을 인식해
                // 불필요한 conflict-rename 을 막는다.
                boolean matchesServer = existsOnClient
                        && (isDir || (curMtime == serverMtime && curSize == serverEntry.getSize()));

                CMFileSyncClientEntry clientEntry = new CMFileSyncClientEntry();
                clientEntry.setPath(relPathStr)
                        .setSize(serverEntry.getSize())
                        .setCurMtime(curMtime)
                        .setBaseMtime(baseMtime)
                        .setServerMtime(serverMtime);

                CMFileSyncOp op = serverEntry.getOp();

                // 이미 서버와 동일(CREATE/MODIFY) → 전송 없이 lastSynced 재구축 + COMPLETE_PULL_* 송신으로 완료 마킹.
                // (서버는 serverEntryList 전체로 pullStateMap 을 만들어 완료를 기다리므로, skip 만 하면 세션이 안 끝남.)
                if (matchesServer && (op == CMFileSyncOp.CREATE || op == CMFileSyncOp.MODIFY)) {
                    if (!proceedAlreadySyncedPullEntry(relPathStr, clientEntry, curSize, op, serverName)) {
                        System.err.println("CMFileSyncManager.compareServerAndClientEntriesForPullSync(), "
                                + "proceedAlreadySyncedPullEntry failed: " + relPathStr);
                    }
                    continue;
                }
                if (op == CMFileSyncOp.MODIFY) {
                    if (!existsOnClient || curMtime != baseMtime) {
                        // conflict: local file missing or locally modified since last sync
                        boolean conflictResult = proceedConflictedClientEntry(clientEntry);
                        if (!conflictResult) {
                            System.err.println("CMFileSyncManager.compareServerAndClientEntriesForPullSync(), " +
                                    "failed to proceed conflict entry: " + relPathStr + ", skip.");
                            // 클라가 처리 못 한 entry 라도 서버 pullStateMap 완료 마킹은 필요 (hang 방지).
                            ackSkippedPullEntry(relPathStr, CMFileSyncOp.MODIFY, serverName);
                            continue;   // rename 실패 시 pullCreateMap에 추가하지 않고 건너뜀
                        }
                        pullCreateMap.put(relPathStr, clientEntry);
                    } else {
                        pullModifyMap.put(relPathStr, clientEntry);
                    }
                } else if (op == CMFileSyncOp.CREATE) {
                    if (existsOnClient) {
                        // conflict: 기존 클라 파일 보호
                        boolean conflictResult = proceedConflictedClientEntry(clientEntry);
                        if (!conflictResult) {
                            System.err.println("CMFileSyncManager.compareServerAndClientEntriesForPullSync(), " +
                                    "failed to proceed conflict entry: " + relPathStr + ", skip.");
                            // 클라가 처리 못 한 entry 라도 서버 pullStateMap 완료 마킹은 필요 (hang 방지).
                            ackSkippedPullEntry(relPathStr, CMFileSyncOp.CREATE, serverName);
                            continue;   // rename 실패 -> 기존 클라 파일 보호
                        }
                    }
                    pullCreateMap.put(relPathStr, clientEntry);
                } else if (op == CMFileSyncOp.DELETE) {
                    if (!existsOnClient) {
                        // 클라 디스크엔 이미 없음 (서버 dedup 이후엔 외부 수동 삭제 / 다중 디바이스 정합성 케이스).
                        // 서버 pullStateMap 완료 마킹 위해 ack 만 송신.
                        ackSkippedPullEntry(relPathStr, CMFileSyncOp.DELETE, serverName);
                    } else if (baseMtime == serverMtime && curMtime == serverMtime) {
                        // 마지막 동기화 이후 변경 없음 -> 안전하게 삭제 대상
                        pullDeleteMap.put(relPathStr, clientEntry);
                    } else {
                        // 로컬에서 수정됨 -> conflict (삭제하지 않고 rename + push)
                        boolean conflictResult = proceedConflictedClientEntry(clientEntry);
                        if (!conflictResult) {
                            System.err.println("CMFileSyncManager.compareServerAndClientEntriesForPullSync(), " +
                                    "failed to proceed conflict entry: " + relPathStr + ", skip.");
                            // rename 실패 -> 삭제도, push도 하지 않음 (현상 유지). 단, 서버 ack 는 필요.
                            ackSkippedPullEntry(relPathStr, CMFileSyncOp.DELETE, serverName);
                        }
                    }
                }
            }

            if (CMInfo._CM_DEBUG) {
                System.out.println("pullModifyMap = " + pullModifyMap);
                System.out.println("pullCreateMap = " + pullCreateMap);
                System.out.println("pullDeleteMap = " + pullDeleteMap);
                System.out.println("pendingPushMap = " + pendingPushMap);
            }

            // pullModifyMap 대상을 제외한 client path 중 baseMtime보다 큰 curMtime인 파일을
            // pendingPushMap에 추가 (이 클라이언트에서 최신 수정된 파일들)
            scanLocalPushCandidates(clientPathList, pullModifyMap.keySet());
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.compareServerAndClientEntriesForPullSync(), I/O error!");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // Scans the client sync home for local PUSH candidates — files whose current mtime is newer
    // than the client base snapshot (lastSynced) — and adds each to pendingPushMap with a
    // CREATE/MODIFY opHint (CREATE when baseMtime==-1, i.e. unknown to the base snapshot).
    // excludePaths holds relPaths already handled by the pull flow (e.g. pullModifyMap keys) that
    // must be skipped; pass an empty set for a standalone scan (the START_PULL_SYNC_ACK
    // returnCode==1 path, where no pull runs). Returns the number of candidates added.
    // mtime/size use the shared helpers, so a vanished/unreadable file yields -1 and is skipped.
    public int scanLocalPushCandidates(List<Path> clientPathList, Set<String> excludePaths) throws IOException {
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Path clientSyncHome = getClientSyncHome();
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        int numAdded = 0;

        for (Path absPath : clientPathList) {
            Path relPath = clientSyncHome.relativize(absPath);
            String relPathStr = relPath.toString().replace('\\', '/');

            if (excludePaths.contains(relPathStr)) continue;

            long baseMtime = syncInfo.getLastSyncedMtime(relPathStr);
            long curMtime = syncInfo.currentMtimeSecOrMinusOne(absPath);
            long curSize = syncInfo.currentSizeOrMinusOne(absPath);

            if (curMtime > baseMtime) {
                CMFileSyncClientEntry clientEntry = new CMFileSyncClientEntry();
                clientEntry.setPath(relPathStr)
                        .setSize(curSize)
                        .setCurMtime(curMtime)
                        .setBaseMtime(baseMtime)
                        .setOpHint(baseMtime == -1 ? CMFileSyncOp.CREATE : CMFileSyncOp.MODIFY)
                        .setDirectory(Files.isDirectory(absPath));
                pendingPushMap.put(relPathStr, clientEntry);
                numAdded++;
                if (CMInfo._CM_DEBUG)
                    System.out.println("pendingPushMap added: key = " + relPathStr + ", value = " + clientEntry);
            }
        }
        return numAdded;
    }

    // Detects local DELETEs for PUSH: relPaths present in the client base snapshot (lastSynced) but
    // no longer on disk. Adds each as a DELETE-opHint entry to pendingPushMap (size=0/curMtime=-1;
    // isDirectory left false — the server uses its own index as the truth for dir-ness when applying
    // the delete). Only safe to call when the server has no pending changes for this client
    // (START_PULL_SYNC_ACK returnCode==1): then every local deletion is unambiguously client-
    // originated. NOT used by the pull flow, where a missing local file may instead be a server
    // CREATE/MODIFY to re-fetch (conflict) rather than a delete to push. existingRelPaths is the set
    // of relPaths currently on disk (derived from clientPathList). Returns the number added.
    public int scanLocalPushDeletes(Set<String> existingRelPaths) {
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        int numAdded = 0;

        // snapshot the keys: the base snapshot is read-only here, but copy defensively
        for (String relPathStr : new ArrayList<>(syncInfo.getLastSyncedMtimeMap().keySet())) {
            if (existingRelPaths.contains(relPathStr)) continue;   // still on disk -> not deleted
            if (pendingPushMap.containsKey(relPathStr)) continue;  // already classified

            CMFileSyncClientEntry entry = new CMFileSyncClientEntry();
            entry.setPath(relPathStr)
                    .setSize(0L)
                    .setCurMtime(-1L)
                    .setBaseMtime(syncInfo.getLastSyncedMtime(relPathStr))
                    .setOpHint(CMFileSyncOp.DELETE)
                    .setDirectory(false);
            pendingPushMap.put(relPathStr, entry);
            numAdded++;
            if (CMInfo._CM_DEBUG)
                System.out.println("pendingPushMap DELETE added: key = " + relPathStr);
        }
        return numAdded;
    }

    // Derives the on-disk relPath set from clientPathList and folds client-originated DELETEs into
    // pendingPushMap via scanLocalPushDeletes. Shared by the two safe call sites: the returnCode==1
    // standalone path (startPushSyncForLocalChanges) and the post-PULL path (processCOMPLETE_PULL_SYNC).
    // Both invoke it only once the server has no pending changes left to disambiguate, so a base-
    // snapshot path missing from disk is unambiguously a local delete. Returns the number added.
    public int addLocalPushDeletes(List<Path> clientPathList) {
        Path clientSyncHome = getClientSyncHome();
        Set<String> existingRelPaths = new HashSet<>();
        for (Path absPath : clientPathList) {
            existingRelPaths.add(clientSyncHome.relativize(absPath).toString().replace('\\', '/'));
        }
        return scanLocalPushDeletes(existingRelPaths);
    }

    // Standalone PUSH entry point used when the PULL flow is skipped because the server reports the
    // client cursor already matches (START_PULL_SYNC_ACK returnCode==1): the cursor only reflects
    // server-applied changes, so it never signals client-local additions/edits. This builds the
    // current client path list and scans for local push candidates; if any exist it starts a push
    // session (mirroring the push branch of processCOMPLETE_PULL_SYNC), otherwise it ends the
    // session by setting syncProgress=NONE. Runs on the event-processing thread.
    public boolean startPushSyncForLocalChanges() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.startPushSyncForLocalChanges() called..");

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if (!confInfo.getSystemType().equals("CLIENT")) {
            System.err.println("CMFileSyncManager.startPushSyncForLocalChanges(), system type is not CLIENT!");
            return false;
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        // build the current client path list (same as the pull flow does after END_SERVER_ENTRY_LIST)
        Path syncHome = getClientSyncHome();
        List<Path> clientPathList = createPathList(syncHome);
        if (clientPathList == null) {
            System.err.println("CMFileSyncManager.startPushSyncForLocalChanges(), clientPathList is null!");
            return false;
        }
        syncInfo.setClientPathList(clientPathList);

        // scan for local CREATE/MODIFY push candidates; no pull is running, so nothing to exclude
        int numCandidates;
        try {
            numCandidates = scanLocalPushCandidates(clientPathList, Collections.emptySet());
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.startPushSyncForLocalChanges(), I/O error during scan!");
            e.printStackTrace();
            return false;
        }

        // detect local DELETEs: base-snapshot paths no longer on disk. Safe here because returnCode==1
        // means the server has no pending changes for this client, so any local deletion is
        // unambiguously client-originated (no server CREATE/MODIFY to conflict with).
        numCandidates += addLocalPushDeletes(clientPathList);

        // nothing to push -> the client really is in sync; end the session
        if (numCandidates == 0) {
            if (CMInfo._CM_DEBUG)
                System.out.println("no local push candidates; nothing to sync.");
            syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
            return true;
        }

        // local changes exist -> start a push session (caller-set-PUSH convention as in the
        // processCOMPLETE_PULL_SYNC push branch: set PUSH before proceedPendingPushMap()).
        syncInfo.setSyncProgress(CMFileSyncProgress.PUSH);
        boolean pushStarted = proceedPendingPushMap();
        if (!pushStarted) {
            // proceedPendingPushMap() rolls back its own snapshot on send failure; reset state here
            System.err.println("CMFileSyncManager.startPushSyncForLocalChanges(), proceedPendingPushMap failed!");
            syncInfo.setSyncProgress(CMFileSyncProgress.NONE);
            return false;
        }
        return true;
    }

    // compareServerAndClientEntriesForPullSync 의 모든 skip 분기(isIgnored / conflict-rename 실패 /
    // DELETE-but-not-on-client) 에서 호출. 서버 pullStateMap[path] 는 entryList 진입 시점에 만들어져
    // 클라 ack 없이는 영원히 completed=false 로 남아 COMPLETE_PULL_SYNC 가 트리거되지 않으므로,
    // 처리 못 했더라도 op 에 맞는 COMPLETE_PULL_* 만 보내 세션을 풀어준다. proceedAlreadySyncedPullEntry
    // 와 달리 lastSynced 는 절대 건드리지 않는다 — 이 분기들은 모두 "클라 디스크 상태가 서버와 다름"
    // 또는 "동기화 대상이 아님" 이므로 base snapshot 에 들어가면 안 됨. DELETE 는 이벤트가 list-shaped 라
    // 단일 path 를 1-원소 List 로 감싼다.
    private boolean ackSkippedPullEntry(String relPathStr, CMFileSyncOp op, String serverName) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.ackSkippedPullEntry() called: " + relPathStr
                    + ", op=" + op);

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        String myName = interInfo.getMyself().getName();
        UUID myUuid = interInfo.getMyself().getUuid();
        UUID myDeviceUuid = syncInfo.getDeviceUuid();

        boolean sendResult;
        switch (op) {
            case CREATE -> {
                CMFileSyncEventCompletePullCreate fse = new CMFileSyncEventCompletePullCreate();
                fse.setInitiatorName(myName);
                fse.setInitiatorUuid(myUuid);
                fse.setInitiatorDeviceUuid(myDeviceUuid);
                fse.setCreatedPath(relPathStr);
                sendResult = CMEventManager.unicastEvent(fse, serverName, null);
            }
            case MODIFY -> {
                CMFileSyncEventCompletePullModify fse = new CMFileSyncEventCompletePullModify();
                fse.setInitiatorName(myName);
                fse.setInitiatorUuid(myUuid);
                fse.setInitiatorDeviceUuid(myDeviceUuid);
                fse.setModifiedPath(relPathStr);
                sendResult = CMEventManager.unicastEvent(fse, serverName, null);
            }
            case DELETE -> {
                CMFileSyncEventCompletePullDelete fse = new CMFileSyncEventCompletePullDelete();
                fse.setInitiatorName(myName);
                fse.setInitiatorUuid(myUuid);
                fse.setInitiatorDeviceUuid(myDeviceUuid);
                fse.setDeletedPathList(List.of(relPathStr));
                sendResult = CMEventManager.unicastEvent(fse, serverName, null);
            }
            default -> {
                System.err.println("CMFileSyncManager.ackSkippedPullEntry(), unsupported op = "
                        + op + " for " + relPathStr);
                return false;
            }
        }
        if (!sendResult)
            System.err.println("CMFileSyncManager.ackSkippedPullEntry(), send error for " + relPathStr);
        return sendResult;
    }

    // 클라 파일이 이미 서버 버전과 동일할 때 호출: 데이터 전송 없이 client-index(lastSynced)를 재구축하고
    // op 에 맞는 COMPLETE_PULL_CREATE/MODIFY 를 서버로 보내 pull 완료를 마킹한다.
    // index 유실(client-index.json 삭제) 후 재동기화 시 동일 파일이 conflict-rename 되는 문제를 막는다.
    // online-mode helper(proceedOnlinePull*Entry)의 "전송 없이 완료" 패턴과 동형 (단 online list 등록 없음,
    // lastSynced 는 mtime+size 둘 다 기록). lastSynced mtime 은 entry.getCurMtime() 사용 — matchesServer 가
    // 파일은 curMtime==serverMtime 을 보장하고 디렉토리는 디스크 mtime 이 정합값.
    private boolean proceedAlreadySyncedPullEntry(String relPathStr, CMFileSyncClientEntry entry,
                                                  long curSize, CMFileSyncOp op, String serverName) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.proceedAlreadySyncedPullEntry() called: " + relPathStr
                    + ", op=" + op);

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        String myName = interInfo.getMyself().getName();
        UUID myUuid = interInfo.getMyself().getUuid();
        UUID myDeviceUuid = syncInfo.getDeviceUuid();

        // client-index(lastSynced) 재구축 (mtime+size)
        syncInfo.setLastSynced(relPathStr, entry.getCurMtime(), curSize);
        entry.setCompleted(true);

        boolean sendResult;
        if (op == CMFileSyncOp.CREATE) {
            CMFileSyncEventCompletePullCreate fse = new CMFileSyncEventCompletePullCreate();
            fse.setInitiatorName(myName);
            fse.setInitiatorUuid(myUuid);
            fse.setInitiatorDeviceUuid(myDeviceUuid);
            fse.setCreatedPath(relPathStr);
            sendResult = CMEventManager.unicastEvent(fse, serverName, null);
        } else {   // MODIFY
            CMFileSyncEventCompletePullModify fse = new CMFileSyncEventCompletePullModify();
            fse.setInitiatorName(myName);
            fse.setInitiatorUuid(myUuid);
            fse.setInitiatorDeviceUuid(myDeviceUuid);
            fse.setModifiedPath(relPathStr);
            sendResult = CMEventManager.unicastEvent(fse, serverName, null);
        }
        if (!sendResult)
            System.err.println("CMFileSyncManager.proceedAlreadySyncedPullEntry(), send error for " + relPathStr);
        return sendResult;
    }

    // called by client; marks a conflicting local file by renaming it with a
    // "-conflict-yyyyMMdd-HHmmss" suffix and queues the renamed file into pendingPushMap.
    // The parameter entry may belong to another pull map, so a new CMFileSyncClientEntry
    // is created for pendingPushMap to keep the original untouched.
    private boolean proceedConflictedClientEntry(CMFileSyncClientEntry entry) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.proceedConflictedClientEntry() called..");
            System.out.println("entry = " + entry);
        }

        // (1) 필요 변수 설정
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<String, CMFileSyncClientEntry> pendingPushMap = syncInfo.getPendingPushMap();
        String relPathStr = entry.getPath();
        Path clientSyncHome = getClientSyncHome();
        Path absPath = clientSyncHome.resolve(relPathStr).toAbsolutePath().normalize();

        // (2) conflict suffix 생성: "-conflict-yyyyMMdd-HHmmss"
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String conflictSuffix = "-conflict-" + timestamp;

        // (3) 새 파일명 생성 (확장자 앞에 suffix 삽입)
        String originalFileName = absPath.getFileName().toString();
        int dotIndex = originalFileName.lastIndexOf('.');
        String stem = (dotIndex >= 0) ? originalFileName.substring(0, dotIndex) : originalFileName;
        String ext = (dotIndex >= 0) ? originalFileName.substring(dotIndex) : "";

        // (4) 파일명 255자 제한 방어
        String baseConflictName = stem + conflictSuffix + ext;
        if (baseConflictName.length() > 255) {
            int excess = baseConflictName.length() - 255;
            stem = stem.substring(0, Math.max(1, stem.length() - excess));
            baseConflictName = stem + conflictSuffix + ext;
        }

        // (5) 중복 파일명 처리: 이미 존재하면 numbering
        String conflictFileName = baseConflictName;
        Path conflictAbsPath = absPath.resolveSibling(conflictFileName);
        int count = 2;
        while (Files.exists(conflictAbsPath)) {
            conflictFileName = stem + conflictSuffix + "(" + count + ")" + ext;
            conflictAbsPath = absPath.resolveSibling(conflictFileName);
            count++;
        }

        // (6) 실제 파일 rename
        if (!Files.exists(absPath)) {
            // DELETE 충돌: 파일이 없으면 rename 불필요, pendingPushMap 추가도 스킵
            if (CMInfo._CM_DEBUG)
                System.out.println("file does not exist (DELETE conflict), skip rename: " + absPath);
            return true;    // 처리할 파일이 없는 것이므로 정상 완료
        }
        try {
            Files.move(absPath, conflictAbsPath);
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.proceedConflictedClientEntry(), " +
                    "error renaming file: " + absPath + " -> " + conflictAbsPath);
            e.printStackTrace();
            return false;
        }
        if (CMInfo._CM_DEBUG)
            System.out.println("renamed: " + absPath + " -> " + conflictAbsPath);

        // (7) renamed entry 새로 생성 (원본 entry 불변 유지)
        String conflictRelPathStr = clientSyncHome.relativize(conflictAbsPath)
                .toString().replace('\\', '/');
        // size는 원본 entry의 서버 측 truth가 아닌 클라 conflict 파일의 디스크 truth 사용
        // (MODIFY/DELETE conflict는 정의상 클라 측 수정이 있어 두 값이 다를 수 있음).
        // 측정 실패 시 원본 size로 fallback (rename은 이미 성공했으므로 데이터 보존 우선).
        long renamedSize;
        try {
            renamedSize = syncInfo.currentSizeOrMinusOne(conflictAbsPath);
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.proceedConflictedClientEntry(), "
                    + "error reading size of " + conflictAbsPath + ", falling back to original entry size.");
            e.printStackTrace();
            renamedSize = entry.getSize();
        }
        CMFileSyncClientEntry renamedEntry = new CMFileSyncClientEntry();
        renamedEntry.setPath(conflictRelPathStr)
                .setSize(renamedSize)
                .setCurMtime(entry.getCurMtime())
                .setBaseMtime(-1L)
                .setOpHint(CMFileSyncOp.CREATE)
                .setDirectory(Files.isDirectory(conflictAbsPath));
        renamedEntry.setCompleted(false);

        // (8) pendingPushMap에 추가
        pendingPushMap.put(conflictRelPathStr, renamedEntry);
        if (CMInfo._CM_DEBUG) {
            System.out.println("added to pendingPushMap: key = " + conflictRelPathStr);
            System.out.println("renamedEntry = " + renamedEntry);
        }

        return true;
    }

    // called by client; processes the pull maps (delete/create/modify).
    // Except for the delete map, processing here only *initiates* the work
    // (e.g., starting a generator thread); completion happens elsewhere.
    public boolean proceedPullMaps() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.proceedPullMaps() called..");

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        boolean result;

        // only the client processes pull maps
        if (!confInfo.getSystemType().equals("CLIENT")) {
            System.err.println("CMFileSyncManager.proceedPullMaps(), system type is not CLIENT!");
            return false;
        }

        // pullDeleteMap 처리
        result = proceedPullDeleteMap();
        if (!result)
            System.err.println("CMFileSyncManager.proceedPullMaps(), proceedPullDeleteMap() failed!");

        // pullCreateMap 처리
        result &= proceedPullCreateMap();
        if (!result)
            System.err.println("CMFileSyncManager.proceedPullMaps(), proceedPullCreateMap() failed!");

        // pullModifyMap 처리
        result &= proceedPullModifyMap();
        if (!result)
            System.err.println("CMFileSyncManager.proceedPullMaps(), proceedPullModifyMap() failed!");

        return result;
    }

    // called by client; deletes the files registered in pullDeleteMap, marks each entry
    // completed, removes their client-index metadata, then reports the deleted list to the
    // server via COMPLETE_PULL_DELETE events (chunked if long).
    private boolean proceedPullDeleteMap() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.proceedPullDeleteMap() called..");

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        Map<String, CMFileSyncClientEntry> pullDeleteMap = syncInfo.getPullDeleteMap();
        Path syncHome = getClientSyncHome();
        Map<String, Long> lastSyncedMtimeMap = syncInfo.getLastSyncedMtimeMap();
        int numDeletedFiles = 0;
        String myName = interInfo.getMyself().getName();
        UUID myUuid = interInfo.getMyself().getUuid();
        String serverName = interInfo.getDefaultServerInfo().getServerName();

        if (pullDeleteMap.isEmpty()) {
            if (CMInfo._CM_DEBUG)
                System.out.println("pullDeleteMap is empty, nothing to delete.");
            return true;
        }

        Set<String> keys = pullDeleteMap.keySet();
        for (String relPath : keys) {
            Path absPath = syncHome.resolve(relPath).normalize();
            try {
                if (Files.exists(absPath)) {
                    // WatchService 가 발생시킬 ENTRY_DELETE 이벤트가 self-event 임을 표시.
                    // 반드시 Files.delete() 직전에 등록 (race 방지).
                    syncInfo.addPendingPullDelete(relPath);
                    Files.delete(absPath);
                } else {
                    // online 모드이거나 이미 외부에서 삭제된 경우 — 정상 처리
                    // 이 경우 DELETE 이벤트가 안 나오므로 pendingPullDelete 등록하지 않음.
                    if (CMInfo._CM_DEBUG)
                        System.out.println("file does not exist locally, skip delete: " + absPath);
                }
                // online list에 등록되어 있으면 제거 (온라인 모드 리스트 파일 반영은 COMPLETE_PULL_SYNC 처리 때)
                removeFromOnlineList(relPath);
                // client entry 완료 처리
                CMFileSyncClientEntry entry = pullDeleteMap.get(relPath);
                entry.setCompleted(true);
                numDeletedFiles++;
                // client-index 인메모리 메타 정보 삭제 (mtime + size 모두)
                lastSyncedMtimeMap.remove(relPath);
                syncInfo.removeLastSyncedSize(relPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // pullDeleteMap의 모든 파일을 삭제했는지 확인
        if (numDeletedFiles != keys.size()) {
            System.err.println("CMFileSyncManager.proceedPullDeleteMap(), not all files deleted: "
                    + numDeletedFiles + "/" + keys.size());
            return false;
        }

        // COMPLETE_PULL_DELETE 이벤트 전송 루프 (긴 경우 나눠서 전송)
        int listIndex = 0;
        boolean sendResult;
        List<String> deletedPathList = new ArrayList<>(keys);
        while (listIndex < deletedPathList.size()) {
            CMFileSyncEventCompletePullDelete fse_cpd = new CMFileSyncEventCompletePullDelete();
            // 공통 필드 설정
            fse_cpd.setInitiatorName(myName);
            fse_cpd.setInitiatorUuid(myUuid);
            fse_cpd.setInitiatorDeviceUuid(syncInfo.getDeviceUuid());
            // 이벤트에 넣을 string path 서브 리스트 구하기
            List<String> subList = createSubStringListForEvent(fse_cpd.getByteNum(), deletedPathList, listIndex);
            listIndex += subList.size();
            // set the subList to the event
            fse_cpd.setDeletedPathList(subList);
            // send the event
            sendResult = CMEventManager.unicastEvent(fse_cpd, serverName, null);
            if (!sendResult) {
                System.err.println("CMFileSyncManager.proceedPullDeleteMap(), send error: " + fse_cpd);
                return false;
            }
            if (CMInfo._CM_DEBUG)
                System.out.println("sent event = " + fse_cpd);
        }

        return true;
    }

    // called by client; pullCreateMap 의 offline 모드 entry 들을 모아 서버에 REQUEST_PULL_CREATES
    // 이벤트를 전송한다. 서버가 자기 sync home 에서 직접 path 를 조립해 pushFile() 로 보낸다.
    // 요청 파일 개수가 많아 MAX_EVENT_SIZE 를 넘으면 여러 이벤트로 분할 전송한다.
    // online-mode entry 들은 기존처럼 인라인 처리 (실제 전송 없음).
    // 수신 완료 시점에 checkCompletePullCreate() 가 호출된다.
    private boolean proceedPullCreateMap() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.proceedPullCreateMap() called..");

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Map<String, CMFileSyncClientEntry> pullCreateMap = syncInfo.getPullCreateMap();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        String serverName = interInfo.getDefaultServerInfo().getServerName();

        if (pullCreateMap.isEmpty()) {
            if (CMInfo._CM_DEBUG)
                System.out.println("pullCreateMap is empty, nothing to create.");
            return true;
        }

        // [10-3] 4.2: pull CREATE 는 pull 종류(일반/전파)와 무관하게 항상 online 모드로 처리한다.
        // 신규 파일은 수신 디바이스에 모드 설정이 없어 isOnlineMode 가 false 를 반환하지만, "신규=online"
        // 정적 기본값을 적용해 데이터 전송 없이 online 엔트리로 생성한다(전송 비용 감소 — 상용 동기화 서비스 추세).
        // 이 정책 하에서 기존 offline pull-create 경로(sendRequestPullCreates / REQUEST_PULL_CREATES)는
        // CREATE 에서 더 이상 사용되지 않는다(서버 핸들러/이벤트는 잔존; 향후 활성도 기반 동적 승격은 8 후속).
        boolean sendResult = true;
        for (String relPathStr : pullCreateMap.keySet()) {
            boolean ret = proceedOnlinePullCreateEntry(relPathStr, pullCreateMap.get(relPathStr), serverName);
            sendResult &= ret;
            if (!ret)
                System.err.println("CMFileSyncManager.proceedPullCreateMap(), online failed for: " + relPathStr);
        }

        return sendResult;
    }

    // offline pull-create 대상 경로 리스트를 MAX_EVENT_SIZE 기준 chunk 로 분할해
    // REQUEST_PULL_CREATES 이벤트를 반복 전송한다.
    // CMFileSyncGenerator.requestTransferOfNewFiles() 의 분할 패턴과 동일.
    private boolean sendRequestPullCreates(List<Path> relPathList, String serverName,
                                           CMInteractionInfo interInfo, CMFileSyncInfo syncInfo) {
        String initiatorName = interInfo.getMyself().getName();
        UUID initiatorUuid = interInfo.getMyself().getUuid();
        UUID initiatorDeviceUuid = syncInfo.getDeviceUuid();

        int numRequestsCompleted = 0;
        while (numRequestsCompleted < relPathList.size()) {
            CMFileSyncEventRequestPullCreates fse = new CMFileSyncEventRequestPullCreates();
            fse.setInitiatorName(initiatorName);
            fse.setInitiatorUuid(initiatorUuid);
            fse.setInitiatorDeviceUuid(initiatorDeviceUuid);

            int curByteNum = fse.getByteNum();
            List<Path> chunk = new ArrayList<>();
            int numRequestedFiles = 0;
            while (numRequestsCompleted < relPathList.size() && curByteNum < CMInfo.MAX_EVENT_SIZE) {
                Path path = relPathList.get(numRequestsCompleted);
                curByteNum += CMInfo.STRING_LEN_BYTES_LEN + path.toString().getBytes().length;
                if (curByteNum < CMInfo.MAX_EVENT_SIZE) {
                    chunk.add(path);
                    numRequestedFiles++;
                    numRequestsCompleted++;
                } else {
                    break;
                }
            }
            // chunk 가 비어있는데 진행이 안 되면 단일 경로가 MAX_EVENT_SIZE 를 초과한 경우.
            // 무한루프 방지를 위해 강제로 1개 담아 보내고 실패 처리한다.
            if (chunk.isEmpty()) {
                System.err.println("CMFileSyncManager.sendRequestPullCreates(), single path exceeds MAX_EVENT_SIZE: "
                        + relPathList.get(numRequestsCompleted));
                return false;
            }
            fse.setNumRequestedFiles(numRequestedFiles);
            fse.setRequestedFileList(chunk);

            boolean ret = CMEventManager.unicastEvent(fse, serverName, null);
            if (!ret) {
                System.err.println("CMFileSyncManager.sendRequestPullCreates(), send error: " + fse);
                return false;
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("sent REQUEST_PULL_CREATES event = " + fse);
            }
        }
        return true;
    }

    // called by client on file-receive completion; finds the matching pullCreateMap entry by
    // file name, moves the received file from transferedFileHome to FileSyncHome/<relPath>
    // (preserving subdirectories), marks it completed, updates the in-memory client-index,
    // then sends COMPLETE_PULL_CREATE to the server (file sender).
    // NOTE: matching is by file name suffix — a future improvement should carry the full
    // relative path in CMFileEvent to avoid ambiguity across subdirectories.
    public boolean checkCompletePullCreate(CMFileEvent fe) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.checkCompletePullCreate() called..");

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        String fileName = fe.getFileName();
        Map<String, CMFileSyncClientEntry> pullCreateMap = syncInfo.getPullCreateMap();

        // pull sync not in progress — nothing to do
        if (pullCreateMap.isEmpty()) return true;

        // find the pullCreateMap key whose relative path ends with the received file name
        String foundKey = null;
        for (String relPath : pullCreateMap.keySet()) {
            if (relPath.equals(fileName) || relPath.endsWith("/" + fileName)) {
                foundKey = relPath;
                break;
            }
        }
        if (foundKey == null) {
            System.err.println("CMFileSyncManager.checkCompletePullCreate(), "
                    + "no pullCreateMap entry found for file: " + fileName);
            return false;
        }

        // 수신된 파일을 transferedFileHome/<fileName> 에서 FileSyncHome/<relPath> 로 이동.
        // 서버측 pushFile() 은 sync home 의 절대경로를 보내지만, 클라 수신측은
        // transferedFileHome 평면 구조에 파일명만 저장하므로 (CMFileTransferManager.java:2170-2173)
        // 여기서 sync home 의 상대경로 위치로 옮긴다.
        CMFileSyncClientEntry entry = pullCreateMap.get(foundKey);
        String relPathStr = entry.getPath();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        Path srcPath = confInfo.getTransferedFileHome().resolve(fileName).toAbsolutePath().normalize();
        Path dstPath = getClientSyncHome().resolve(relPathStr).toAbsolutePath().normalize();
        try {
            Path parentDir = dstPath.getParent();
            if (parentDir != null && Files.notExists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.checkCompletePullCreate(), move failed: "
                    + srcPath + " -> " + dstPath);
            e.printStackTrace();
            return false;
        }

        // mark entry completed
        entry.setCompleted(true);

        // 서버 mtime을 수신 파일에 보존 + 인메모리 client-index 갱신 (self-event 필터용).
        // 파일 전송은 mtime을 보존하지 않으므로(서버 = source of truth) 여기서 명시 적용 — pull-MODIFY(handler 1829)
        // /online-CREATE(1465) 와 동일 출처. baseMtime(lastSynced)을 serverMtime 으로 맞춰야 분류의 DELETE 규칙
        // (baseMtime == serverMtime)이 정상 동작 → pull-create 한 파일이 나중에 서버에서 삭제될 때 가짜 conflict 방지.
        long serverMtime = entry.getServerMtime();
        try {
            long curSize = syncInfo.currentSizeOrMinusOne(dstPath);
            if (serverMtime > 0) {
                Files.setLastModifiedTime(dstPath, FileTime.fromMillis(serverMtime * 1000L));
                syncInfo.setLastSynced(relPathStr, serverMtime, curSize);
            } else {
                // serverMtime 비정상 → 보존 skip, 디스크 측정값으로 폴백(기존 동작)
                System.err.println("CMFileSyncManager.checkCompletePullCreate(), invalid serverMtime for "
                        + relPathStr + ": " + serverMtime + " — fallback to disk mtime");
                syncInfo.setLastSynced(relPathStr, syncInfo.currentMtimeSecOrMinusOne(dstPath), curSize);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // send COMPLETE_PULL_CREATE to the server (file sender)
        CMFileSyncEventCompletePullCreate fse_cpc = new CMFileSyncEventCompletePullCreate();
        fse_cpc.setInitiatorName(fe.getFileReceiver());
        fse_cpc.setInitiatorUuid(fe.getFileReceiverUuid());
        fse_cpc.setInitiatorDeviceUuid(syncInfo.getDeviceUuid());
        fse_cpc.setCreatedPath(foundKey);
        boolean sendResult = CMEventManager.unicastEvent(fse_cpc, fe.getFileSender(), fe.getFileSenderUuid());
        if (!sendResult) {
            System.err.println("CMFileSyncManager.checkCompletePullCreate(), send error: " + fse_cpc);
            return false;
        }

        return true;
    }

    // called by server; returns true only if every entry in the given pullStateMap is completed.
    public boolean isCompletePullSync(Map<String, CMFileSyncClientEntry> pullStateMap) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.isCompletePullSync() called..");

        boolean isCompleted = true;
        for (CMFileSyncClientEntry entry : pullStateMap.values()) {
            isCompleted &= entry.isCompleted();
        }
        return isCompleted;
    }

    // called by server; removes the pullStateMap for the given client from pullStateTable,
    // then sends COMPLETE_PULL_SYNC to that client.
    public boolean completePullSync(String initiatorName, UUID initiatorUuid, UUID initiatorDeviceUuid) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.completePullSync() called..");

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);

        Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pullStateTable = syncInfo.getPullStateTable();
        Map<String, CMFileSyncClientEntry> pullStateMap = pullStateTable.remove(stateKey);
        if (pullStateMap == null) {
            System.err.println("CMFileSyncManager.completePullSync(), pullStateMap not found for: " + stateKey);
            return false;
        }

        CMFileSyncEventCompletePullSync fse_cps = new CMFileSyncEventCompletePullSync();
        fse_cps.setInitiatorName(initiatorName);
        fse_cps.setInitiatorUuid(initiatorUuid);
        fse_cps.setInitiatorDeviceUuid(initiatorDeviceUuid);
        fse_cps.setNumFilesCompleted(pullStateMap.size());
        boolean sendResult = CMEventManager.unicastEvent(fse_cps, initiatorName, initiatorUuid);
        if (!sendResult)
            System.err.println("CMFileSyncManager.completePullSync(), send error: " + fse_cps);

        return sendResult;
    }

    // online-mode MODIFY helper: skips block-checksum flow; keeps the 0바이트 placeholder on disk,
    // resyncs the placeholder mtime + onlineModePathSizeMap size + base snapshot to the new server
    // state, marks entry completed, and sends COMPLETE_PULL_MODIFY to the server.
    private boolean proceedOnlinePullModifyEntry(String relPathStr, CMFileSyncClientEntry entry, String serverName) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.proceedOnlinePullModifyEntry() called: " + relPathStr);

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();

        // online 모드 MODIFY: 파일 데이터는 받지 않고 0바이트 placeholder 를 그대로 둔다.
        // placeholder 는 online 진입 시점(pull CREATE / online 전환)에 이미 디스크에 존재하므로 새로 만들지 않되,
        // (1) 디스크 placeholder 의 mtime 을 새 serverMtime 으로 갱신하고,
        // (2) onlineModePathSizeMap 의 기록 크기를 새 서버 크기로 갱신해 정합을 유지한다.
        long serverMtime = entry.getServerMtime();
        Path absPath = getClientSyncHome().resolve(relPathStr).toAbsolutePath().normalize();
        long baseMtime;
        try {
            if (serverMtime > 0) {
                // (1) 디스크 placeholder mtime 갱신 → lastSynced 와 일치 → WatchService self-event 필터가 걸러냄
                if (Files.exists(absPath)) {
                    Files.setLastModifiedTime(absPath, FileTime.fromMillis(serverMtime * 1000L));
                }
                baseMtime = serverMtime;
            } else {
                // serverMtime 비정상 → 보존 skip, 디스크 측정값으로 폴백 (pull CREATE 와 동일 정책)
                System.err.println("CMFileSyncManager.proceedOnlinePullModifyEntry(), invalid serverMtime for "
                        + relPathStr + ": " + serverMtime + " — fallback to disk mtime");
                baseMtime = syncInfo.currentMtimeSecOrMinusOne(absPath);
            }
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.proceedOnlinePullModifyEntry(), placeholder mtime update failed: " + absPath);
            e.printStackTrace();
            return false;
        }

        // (2) onlineModePathSizeMap 기록 크기를 새 서버 크기로 갱신 (stale size 방지)
        addToOnlineList(relPathStr, entry.getSize());

        // base snapshot(lastSynced)을 디스크 placeholder(mtime=serverMtime, size=0)에 맞춘다.
        // size=0 이어야 self-event 필터(mtime+size 동시 일치)와 push-candidate 판정이 정상 동작.
        syncInfo.setLastSynced(relPathStr, baseMtime, 0L);

        // entry 완료 처리
        entry.setCompleted(true);

        // COMPLETE_PULL_MODIFY 이벤트 생성 및 전송
        CMFileSyncEventCompletePullModify fse_cpm = new CMFileSyncEventCompletePullModify();
        fse_cpm.setInitiatorName(interInfo.getMyself().getName());
        fse_cpm.setInitiatorUuid(interInfo.getMyself().getUuid());
        fse_cpm.setInitiatorDeviceUuid(syncInfo.getDeviceUuid());
        fse_cpm.setModifiedPath(relPathStr);

        boolean sendResult = CMEventManager.unicastEvent(fse_cpm, serverName, null);
        if (!sendResult)
            System.err.println("CMFileSyncManager.proceedOnlinePullModifyEntry(), send error: " + fse_cpm);

        return sendResult;
    }

    private boolean proceedPullModifyMap() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.proceedPullModifyMap() called..");

        // (1) 필요 변수
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();

        // (2) system type 확인
        if (!confInfo.getSystemType().equals("CLIENT")) {
            System.err.println("CMFileSyncManager.proceedPullModifyMap(), not a CLIENT.");
            return false;
        }

        // (3) pullModifyMap 비어있으면 즉시 true
        Map<String, CMFileSyncClientEntry> pullModifyMap = syncInfo.getPullModifyMap();
        if (pullModifyMap == null || pullModifyMap.isEmpty()) {
            if (CMInfo._CM_DEBUG)
                System.out.println("pullModifyMap is empty. nothing to do.");
            return true;
        }

        // (3.5) online/local 분리 처리
        List<CMFileSyncClientEntry> localModifyList = new ArrayList<>();
        boolean result = true;
        String serverName = interInfo.getDefaultServerInfo().getServerName();

        for (Map.Entry<String, CMFileSyncClientEntry> mapEntry : pullModifyMap.entrySet()) {
            String relPathStr = mapEntry.getKey();
            CMFileSyncClientEntry entry = mapEntry.getValue();
            Path absPath = getClientSyncHome().resolve(relPathStr).toAbsolutePath().normalize();
            if (isOnlineMode(absPath)) {
                result &= proceedOnlinePullModifyEntry(relPathStr, entry, serverName);
            } else {
                localModifyList.add(entry);
            }
        }

        if (localModifyList.isEmpty()) {
            if (CMInfo._CM_DEBUG)
                System.out.println("no local-mode MODIFY entries -- skip pullGenerator.");
            return result;
        }

        // (4) 이미 pullGenerator가 동작 중이면 중복 시작 방지
        if (syncInfo.getPullGenerator() != null) {
            System.err.println("pullGenerator already exists; skip.");
            return false;
        }

        // (5) generator 생성에 필요한 정보 수집
        String initiatorName = interInfo.getMyself().getName();
        UUID initiatorUuid = interInfo.getMyself().getUuid();
        UUID initiatorDeviceUuid = syncInfo.getDeviceUuid();

        // (6) generator 생성 및 등록
        CMFileSyncPullGenerator pullGen = new CMFileSyncPullGenerator(
                initiatorName, initiatorUuid, initiatorDeviceUuid,
                serverName, localModifyList);
        syncInfo.setPullGenerator(pullGen);

        // (7) executor에 submit
        ExecutorService es = CMThreadInfo.getInstance().getExecutorService();
        es.submit(pullGen);

        if (CMInfo._CM_DEBUG)
            System.out.println("pullGenerator submitted with " + localModifyList.size() + " entries.");

        return true;
    }

    // called by client; removes the given relative path from the online-mode-map.
    // (the legacy online-mode list was replaced by onlineModePathSizeMap.)
    public boolean removeFromOnlineList(String relPathStr) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.removeFromOnlineList() called: " + relPathStr);

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Path absPath = getClientSyncHome().resolve(relPathStr).toAbsolutePath().normalize();

        // online-mode-map 갱신
        Map<Path, Long> onlineModePathSizeMap = syncInfo.getOnlineModePathSizeMap();
        if (onlineModePathSizeMap != null) {
            onlineModePathSizeMap.remove(absPath);
        }

        return true;
    }

    // called by client; adds the given relative path (with size) to the online-mode-map.
    // (used for online-mode CREATE handling in pull sync.)
    public boolean addToOnlineList(String relPathStr, long size) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.addToOnlineList() called: " + relPathStr + ", size=" + size);

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Path absPath = getClientSyncHome().resolve(relPathStr).toAbsolutePath().normalize();

        // online-mode-map 갱신
        Map<Path, Long> onlineModePathSizeMap = syncInfo.getOnlineModePathSizeMap();
        if (onlineModePathSizeMap != null) {
            onlineModePathSizeMap.put(absPath, size);
        }

        return true;
    }

    // online-mode CREATE helper: 파일 전송 없이 인메모리 client-index 업데이트 +
    // isCompleted=true + COMPLETE_PULL_CREATE 송신.
    // checkCompletePullCreate()는 실제 파일 수신을 트리거로 호출되므로 online 모드에서는
    // 호출되지 않는다. 따라서 본 헬퍼가 그 책임을 직접 수행한다.
    private boolean proceedOnlinePullCreateEntry(String relPathStr, CMFileSyncClientEntry entry, String serverName) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.proceedOnlinePullCreateEntry() called: " + relPathStr);

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();

        // online 모드 계약: 파일 데이터는 받지 않되 0바이트 placeholder 파일을 디스크에 만든다
        // (기존 online 전환 processONLINE_MODE_LIST_ACK 가 원본을 truncate(0) 하는 것과 동형).
        // placeholder 가 없으면 createPathList(디스크 스캔)에 잡히지 않아, 이후 push 단계의
        // scanLocalPushDeletes 가 "base snapshot 엔 있는데 디스크엔 없음" → 로컬 삭제로 오판하고
        // 그 DELETE 가 서버로 push 되어 원본이 지워진다.
        long serverMtime = entry.getServerMtime();
        Path absPath = getClientSyncHome().resolve(relPathStr).toAbsolutePath().normalize();
        long baseMtime;
        try {
            Path parentDir = absPath.getParent();
            if (parentDir != null && Files.notExists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            // 0바이트 placeholder 생성 (이미 있으면 0으로 자름)
            Files.newByteChannel(absPath, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).close();
            // 서버 mtime 을 placeholder 에 보존 → lastSynced 와 일치 → WatchService self-event 필터가 걸러냄.
            if (serverMtime > 0) {
                Files.setLastModifiedTime(absPath, FileTime.fromMillis(serverMtime * 1000L));
                baseMtime = serverMtime;
            } else {
                // serverMtime 비정상 → 보존 skip, 디스크 측정값으로 폴백 (checkCompletePullCreate 와 동일 정책)
                System.err.println("CMFileSyncManager.proceedOnlinePullCreateEntry(), invalid serverMtime for "
                        + relPathStr + ": " + serverMtime + " — fallback to disk mtime");
                baseMtime = syncInfo.currentMtimeSecOrMinusOne(absPath);
            }
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.proceedOnlinePullCreateEntry(), placeholder create failed: " + absPath);
            e.printStackTrace();
            return false;
        }

        // 온라인 모드 리스트에 추가 (원본(실제) 크기 기록 — 나중에 local 모드 재요청 시 사용)
        addToOnlineList(relPathStr, entry.getSize());

        // base snapshot(lastSynced)을 디스크 placeholder 상태(mtime=serverMtime, size=0)에 맞춘다.
        // size=0 이어야 WatchService self-event 필터(mtime+size 동시 일치)와 push-candidate 판정이 정상 동작.
        syncInfo.setLastSynced(relPathStr, baseMtime, 0L);

        // entry 완료 처리
        entry.setCompleted(true);

        // COMPLETE_PULL_CREATE 이벤트 생성 및 전송
        CMFileSyncEventCompletePullCreate fse_cpc = new CMFileSyncEventCompletePullCreate();
        fse_cpc.setInitiatorName(interInfo.getMyself().getName());
        fse_cpc.setInitiatorUuid(interInfo.getMyself().getUuid());
        fse_cpc.setInitiatorDeviceUuid(syncInfo.getDeviceUuid());
        fse_cpc.setCreatedPath(relPathStr);

        boolean sendResult = CMEventManager.unicastEvent(fse_cpc, serverName, null);
        if (!sendResult)
            System.err.println("CMFileSyncManager.proceedOnlinePullCreateEntry(), send error: " + fse_cpc);

        return sendResult;
    }

    public List<Path> createPathList(Path syncHome) {

        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.createPathList() called..");

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Path syncHomeNorm = syncHome.toAbsolutePath().normalize();

        List<Path> pathList;
        try {
            // change to absolute path -> sorted -> change to a list
            pathList = Files.walk(syncHome)
                    .filter(path -> !path.equals(syncHome))
                    .map(path -> path.toAbsolutePath().normalize())
                    // ignore 패턴에 매칭되는 경로 제외 (.DS_Store 등 OS 자동 생성 파일)
                    .filter(path -> !syncInfo.isIgnored(syncHomeNorm.relativize(path)))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (pathList.isEmpty()) {
            if (CMInfo._CM_DEBUG) {
                System.err.println("CMFileSyncManager.createPathList(), The sync-home is empty.");
            }
        }

        if (CMInfo._CM_DEBUG) {
            for (Path p : pathList)
                System.out.println(p);
        }

        return pathList;
    }

    private List<String> toRelativeStringList(List<Path> paths, Path basePath) {
        List<Path> relativePaths = toRelativePathList(paths, basePath);
        return relativePaths.stream()
                .map(p -> p.toString().replace('\\', '/'))
                .collect(Collectors.toList());
    }

    private List<Path> toRelativePathList(List<Path> paths, Path basePath) {
        Objects.requireNonNull(paths);
        Objects.requireNonNull(basePath);

        Path normalizedBasePath = basePath.toAbsolutePath().normalize();
        List<Path> relativePaths = new ArrayList<>(paths.size());

        for (Path path : paths) {
            Objects.requireNonNull(path);
            Path normalizedPath = path.normalize();

            Path relativePath;
            if (path.isAbsolute()) {
                Path normalizedAbsolutePath = normalizedPath.toAbsolutePath().normalize();
                if (!normalizedAbsolutePath.startsWith(normalizedBasePath)) {
                    throw new IllegalArgumentException(
                            String.format("Path %s is not a descendant of base path %s",
                                    normalizedAbsolutePath, normalizedBasePath));
                }
                relativePath = normalizedBasePath.relativize(normalizedAbsolutePath);
            } else {
                relativePath = normalizedPath;
            }
            relativePaths.add(relativePath);
        }
        return relativePaths;
    }

    // currently called by client
    private boolean sendFileList() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncManager.sendFileList() called..");

        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        String initiatorName;
        String receiverName;
        List<Path> pathList;

        UUID initiatorUuid = interInfo.getMyself().getUuid();
        UUID initiatorDeviceUuid = syncInfo.getDeviceUuid();
        UUID receiverUuid = null;

        // create START_FILE_LIST event.
        CMFileSyncEventStartFileList fse = new CMFileSyncEventStartFileList();
        // get initiator name
        initiatorName = interInfo.getMyself().getName();
        // get default server name (양방향 동기화 때 수정 필요)
        receiverName = interInfo.getDefaultServerInfo().getServerName();
        // set common initiator, initiator uuid, initiator device uuid
        fse.setInitiatorName(initiatorName);
        fse.setInitiatorUuid(initiatorUuid);
        fse.setInitiatorDeviceUuid(initiatorDeviceUuid);
        // get path list
        pathList = syncInfo.getPathList();
        if (pathList == null)
            fse.setNumTotalFiles(0);
        else
            fse.setNumTotalFiles(pathList.size());

        // send the event
        boolean sendResult = CMEventManager.unicastEvent(fse, receiverName, receiverUuid);
        if (!sendResult) {
            System.err.println("CMFileSyncManager.sendFileList(), send error!");
            System.err.println(fse);
            return false;
        }
        return true;
    }

    // currently called by server
    public void checkNewTransferForSync(CMFileEvent fe) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.checkNewTransferForSync() called..");
            System.out.println("file event = " + fe);
        }
        // get the file name
        String fileName = fe.getFileName();
        // get the new file list
        String fileSender = fe.getFileSender();
        UUID fileSenderUuid = fe.getFileSenderUuid();
        CMUserLoginKey loginKey = new CMUserLoginKey(fileSender, fileSenderUuid);
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        if (syncGenerator == null) {
            if(CMInfo._CM_DEBUG)
                System.err.println("The sync generator for (" + fileSender + ", " + fileSenderUuid + ") is null!");
            return;
        }
        List<CMFileSyncEntry> newInitiatorPathEntryList = syncGenerator.getNewInitiatorPathEntryList();
        Objects.requireNonNull(newInitiatorPathEntryList);

        // search for the entry in the newInitiatorPathEntryList
        CMFileSyncEntry foundEntry = null;
        Path foundPath = null;
        for (CMFileSyncEntry entry : newInitiatorPathEntryList) {
            if (entry.getPathRelativeToHome().endsWith(fileName)) {
                foundEntry = entry;
                foundPath = entry.getPathRelativeToHome();
                break;
            }
        }
        if (foundPath != null) {
            // get the file-transfer home and sync home
            CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
            Path transferFileHome;
            Path syncHome;
            if (confInfo.getSystemType().equals("SERVER")) {
                transferFileHome = confInfo.getTransferedFileHome().resolve(fileSender);
                syncHome = getServerSyncHome(fileSender);
            } else {
                transferFileHome = confInfo.getTransferedFileHome();
                syncHome = getClientSyncHome();
            }
            // move the transferred file to the sync home (including sub-directories)
            try {
                //Files.move(transferFileHome.resolve(fileName), syncHome.resolve(foundPath));
                Files.move(transferFileHome.resolve(fileName), syncHome.resolve(foundPath),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // set the last-modified-time of the corresponding client file entry
            try {
                Files.setLastModifiedTime(syncHome.resolve(foundPath), foundEntry.getLastModifiedTime());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // complete the new-file-transfer
            boolean result = completeNewFileTransfer(loginKey, foundPath);
            if (result) {
                // remove the completed newInitiatorPathEntry from the list
                final Path finalFoundPath = foundPath;
                boolean removeResult = newInitiatorPathEntryList.removeIf(entry ->
                        entry.getPathRelativeToHome().equals(finalFoundPath));

                if (!removeResult) {
                    System.err.println("remove error from the new-initiator-path-entry-list: " + foundPath);
                    return;
                }
                // check if the file-sync is complete or not
                if (isCompleteFileSync(loginKey)) {
                    // complete the file-sync task
                    completeFileSync(loginKey);
                }
            }
        }
    }

    // 10-2 doc 10802~11218: 파일 전송 완료(END_FILE_TRANSFER / _CHAN) 시점에 incremental PUSH-CREATE 대상인지
    // 판별 → 파일 이동 → 인메모리 server-index 갱신 → pushOpRecordTable record add (영속화는 completePushSync에서)
    // → pushStateMap isCompleted 마킹 → COMPLETE_PUSH_CREATE 송신 → 세션 완료 체크.
    // checkNewTransferForSync(full sync 신규 파일)와 대칭. 서로 다른 자료구조(syncGeneratorMap vs pushStateTable)
    // 검사 → 상호 배타적, 같은 이벤트에서 양쪽 호출해도 한쪽만 동작.
    public void checkCompletePushCreate(CMFileEvent fe) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.checkCompletePushCreate() called..");
            System.out.println("file event = " + fe);
        }

        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if (!confInfo.getSystemType().equals("SERVER")) return;

        String fileSender = fe.getFileSender();
        UUID fileSenderUuid = fe.getFileSenderUuid();
        String fileName = fe.getFileName();
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();

        Map<CMFileSyncStateKey, Map<String, CMFileSyncClientEntry>> pushStateTable = syncInfo.getPushStateTable();
        if (pushStateTable == null || pushStateTable.isEmpty()) return;

        // loginKey로 stateKey 역방향 조회 (doc 11010~11024)
        CMUserLoginKey loginKey = new CMUserLoginKey(fileSender, fileSenderUuid);
        CMFileSyncStateKey matchedStateKey = syncInfo.getPushLoginKeyToStateKeyMap().get(loginKey);
        if (matchedStateKey == null) {
            // PUSH 세션 없음 → full sync 또는 무관한 전송
            return;
        }
        Map<String, CMFileSyncClientEntry> matchedPushStateMap = pushStateTable.get(matchedStateKey);
        if (matchedPushStateMap == null) {
            System.err.println("CMFileSyncManager.checkCompletePushCreate(), "
                    + "pushStateMap not found for stateKey: " + matchedStateKey);
            return;
        }

        // pushStateMap에서 fileName endsWith 매칭으로 relPathStr 탐색 (checkNewTransferForSync 동일 정책)
        String matchedRelPathStr = null;
        CMFileSyncClientEntry matchedEntry = null;
        for (Map.Entry<String, CMFileSyncClientEntry> mapEntry : matchedPushStateMap.entrySet()) {
            if (Paths.get(mapEntry.getKey()).endsWith(fileName)) {
                matchedRelPathStr = mapEntry.getKey();
                matchedEntry = mapEntry.getValue();
                break;
            }
        }
        if (matchedRelPathStr == null) {
            // PUSH-CREATE 대상 아님
            return;
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("PUSH-CREATE transfer detected: fileName=" + fileName
                    + ", relPath=" + matchedRelPathStr);
        }

        // 수신 파일 이동: transferFileHome → serverSyncHome
        Path transferFileHome = confInfo.getTransferedFileHome().resolve(fileSender);
        Path serverSyncHome = getServerSyncHome(fileSender);
        Path relPath = Paths.get(matchedRelPathStr);
        Path absDestPath = serverSyncHome.resolve(relPath).toAbsolutePath().normalize();
        try {
            // 디렉토리 entry 처리 순서 보장이 없으므로 상위 디렉토리 방어적 생성
            Files.createDirectories(absDestPath.getParent());
            Files.move(transferFileHome.resolve(fileName), absDestPath,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.checkCompletePushCreate(), "
                    + "failed to move file: " + fileName + " -> " + absDestPath);
            e.printStackTrace();
            return;
        }

        // 인메모리 server-index 갱신 (영속화는 completePushSync에서 일괄)
        UUID initiatorDeviceUuid = matchedStateKey.initiatorDeviceUuid();
        CMFileSyncIndexRepository indexRepository =
                syncInfo.getIndexRegistry().getOrLoad(fileSender, initiatorDeviceUuid);
        long newChangeId;   // [10-3] 전역 할당 (dual-writer (i): 증분 push file CREATE)
        try {
            newChangeId = syncInfo.allocateNextChangeId(fileSender);
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.checkCompletePushCreate(), "
                    + "failed to allocate changeId for: " + matchedRelPathStr);
            e.printStackTrace();
            return;
        }
        long mtimeSec;
        long sizeBytes;
        String md5Hex;
        try {
            mtimeSec = Files.getLastModifiedTime(absDestPath).toMillis() / 1000L;
            sizeBytes = Files.size(absDestPath);
            md5Hex = CMUtil.md5Hex(absDestPath);
        } catch (IOException e) {
            System.err.println("CMFileSyncManager.checkCompletePushCreate(), "
                    + "failed to read file metadata: " + absDestPath);
            e.printStackTrace();
            return;
        }
        indexRepository.applyCreateOrModify(matchedRelPathStr, false, md5Hex, mtimeSec, sizeBytes, newChangeId);

        // 10-2 doc 12015: 파일 CREATE 분기 record add. 영속화는 completePushSync.
        List<CMFileSyncChangeLogEntry> opRecords = syncInfo.getPushOpRecordTable().get(matchedStateKey);
        if (opRecords != null) {
            opRecords.add(new CMFileSyncChangeLogEntry()
                    .setChangeId(newChangeId)
                    .setUserName(fileSender)
                    .setOriginDeviceUuid(initiatorDeviceUuid)
                    .setOp(CMFileSyncOp.CREATE)
                    .setPath(matchedRelPathStr)
                    .setDirectory(false)
                    .setContentHash(md5Hex)
                    .setMtime(mtimeSec)
                    .setSize(sizeBytes)
                    .setTombstone(false)
                    .setTs(OffsetDateTime.now()));
        } else {
            System.err.println("CMFileSyncManager.checkCompletePushCreate(), "
                    + "pushOpRecordTable missing for stateKey: " + matchedStateKey);
        }

        // pushStateMap entry 완료 마킹
        matchedEntry.setCompleted(true);

        // COMPLETE_PUSH_CREATE 송신 (실패 시 로그만, 세션 완료 체크는 계속 — doc 11207~11210)
        CMFileSyncEventCompletePushCreate fse_cpc = new CMFileSyncEventCompletePushCreate();
        fse_cpc.setInitiatorName(fileSender);
        fse_cpc.setInitiatorUuid(fileSenderUuid);
        fse_cpc.setInitiatorDeviceUuid(initiatorDeviceUuid);
        fse_cpc.setCreatedPath(matchedRelPathStr);
        boolean sendResult = CMEventManager.unicastEvent(fse_cpc, fileSender, fileSenderUuid);
        if (!sendResult) {
            System.err.println("CMFileSyncManager.checkCompletePushCreate(), "
                    + "failed to send COMPLETE_PUSH_CREATE for: " + matchedRelPathStr);
        }

        // PUSH 세션 전체 완료 여부 체크
        if (isCompletePushSync(matchedPushStateMap)) {
            completePushSync(matchedStateKey, fileSenderUuid);
        }
    }

    // called by the server
    public boolean completeNewFileTransfer(CMUserLoginKey loginKey, Path path) {
        String initiatorName = loginKey.getUserName();
        UUID initiatorUuid = loginKey.getUuid();
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completeNewFileTransfer() called..");
            System.out.println("loginKey = " + loginKey);
            System.out.println("path = " + path);
        }
        // get CMFileSyncGenerator
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        if (syncGenerator == null) {
            System.err.println("syncGenerator is null!");
            return false;
        }
        // set the isNewFileCompletedHashMap element
        syncGenerator.getIsNewFileCompletedMap().put(path, true);
        // update numNewFilesCompleted
        int numNewFilesCompleted = syncGenerator.getNumNewFilesCompleted();
        numNewFilesCompleted++;
        syncGenerator.setNumNewFilesCompleted(numNewFilesCompleted);

        // 메타 정보 파일 업데이트
        UUID deviceUuid = syncGenerator.getInitiatorDeviceUuid();
        try {
            syncInfo.applyCreate(initiatorName, deviceUuid, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // cursor 구하기
        long lastChangeId = syncInfo.getIndexRegistry().getOrLoad(initiatorName, deviceUuid).lastChangeId();

        // create a COMPLETE_NEW_FILE event
        CMFileSyncEventCompleteNewFile fse = new CMFileSyncEventCompleteNewFile();
        // 공통 필드 설정
        fse.setInitiatorName(initiatorName);
        fse.setInitiatorUuid(initiatorUuid);
        fse.setInitiatorDeviceUuid(deviceUuid);
        // 나머지 필드 설정
        fse.setCompletedPath(path.toString().replace('\\', '/'));
        fse.setCursor(lastChangeId);

        // send the event
        boolean ret = CMEventManager.unicastEvent(fse, initiatorName, initiatorUuid);
        if (!ret) {
            System.err.println("send error: " + fse);
            return false;
        }

        return true;
    }

    // called at the server
    public boolean skipUpdateFile(CMUserLoginKey loginKey, Path basisFile) {
        String initiatorName = loginKey.getUserName();
        UUID initiatorUuid = loginKey.getUuid();
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.skipUpdateFile() called..");
            System.out.println("initiatorName = " + initiatorName);
            System.out.println("initiatorUuid = " + initiatorUuid);
            System.out.println("basisFile = " + basisFile);
        }
        // get CMFileSyncGenerator
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        Objects.requireNonNull(syncGenerator);
        // set the isUpdateFileCompletedMap element
        syncGenerator.getIsUpdateFileCompletedMap().put(basisFile, true);
        // update numUpdateFilesCompleted
        int numUpdateFilesCompleted = syncGenerator.getNumUpdateFilesCompleted();
        numUpdateFilesCompleted++;
        syncGenerator.setNumUpdateFilesCompleted(numUpdateFilesCompleted);

        // create a SKIP_UPDATE_FILE event
        CMFileSyncEventSkipUpdateFile fse = new CMFileSyncEventSkipUpdateFile();
        // 공통 필드 설정
        fse.setInitiatorName(initiatorName);
        fse.setInitiatorUuid(initiatorUuid);
        fse.setInitiatorDeviceUuid(syncGenerator.getInitiatorDeviceUuid());

        // get the relative path of the basis file path
        Path syncHome;
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        if (confInfo.getSystemType().equals("SERVER")) {
            syncHome = getServerSyncHome(initiatorName);
        } else {
            syncHome = getClientSyncHome();
        }
        Path relativePath = basisFile.subpath(syncHome.getNameCount(), basisFile.getNameCount());
        // set the relative path to the event
        fse.setSkippedPath(relativePath.toString().replace('\\', '/'));

        return CMEventManager.unicastEvent(fse, initiatorName, initiatorUuid);
    }

    // called by the server
    public boolean completeUpdateFile(CMUserLoginKey loginKey, Path path) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completeUpdateFile() called..");
            System.out.println("loginKey = " + loginKey);
            System.out.println("path = " + path);
        }
        // get CMFileSyncGenerator
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        Objects.requireNonNull(syncGenerator);
        // set the isUpdateFileCompletedMap element
        syncGenerator.getIsUpdateFileCompletedMap().put(path, true);
        // update numUpdateFilesCompleted
        int numUpdateFilesCompleted = syncGenerator.getNumUpdateFilesCompleted();
        numUpdateFilesCompleted++;
        syncGenerator.setNumUpdateFilesCompleted(numUpdateFilesCompleted);

        String initiatorName = loginKey.getUserName();
        UUID initiatorUuid = loginKey.getUuid();

        // device uuid 구하기
        UUID deviceUuid = syncGenerator.getInitiatorDeviceUuid();
        // 동기화 메타 파일 및 인메모리 정보 업데이트
        try {
            syncInfo.applyModify(initiatorName, deviceUuid, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // create a COMPLETE_UPDATE_FILE event
        CMFileSyncEventCompleteUpdateFile fse = new CMFileSyncEventCompleteUpdateFile();
        // 공통 필드 설정
        fse.setInitiatorName(initiatorName);
        fse.setInitiatorUuid(initiatorUuid);
        fse.setInitiatorDeviceUuid(deviceUuid);

        // get the relative path of the basis file path
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        Path syncHome;
        if (confInfo.getSystemType().equals("SERVER")) {
            syncHome = getServerSyncHome(initiatorName);
        } else {
            syncHome = getClientSyncHome();
        }
        Path relativePath = path.subpath(syncHome.getNameCount(), path.getNameCount());
        // set the relative path to the event
        fse.setCompletedPath(relativePath.toString().replace('\\', '/'));
        // cursor 구하기
        long lastChangeId = syncInfo.getIndexRegistry().getOrLoad(initiatorName, deviceUuid).lastChangeId();
        fse.setCursor(lastChangeId);

        boolean ret = CMEventManager.unicastEvent(fse, initiatorName, initiatorUuid);
        return ret;
    }

    // called by the server
    public boolean completeDeleteFiles(CMUserLoginKey loginKey, UUID initiatorDeviceUuid,
                                       List<Path> deletedPathList) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completeDeleteFiles() called..");
            System.out.println("loginKey = " + loginKey);
            System.out.println("initiatorDeviceUuid = " + initiatorDeviceUuid);
            System.out.println("deletedPathList = " + deletedPathList);
        }
        // initiatorName, initiatorUuid 구하기
        String initiatorName = loginKey.getUserName();
        UUID initiatorUuid = loginKey.getUuid();
        // sync info 구하기
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        // sync generator 구하기
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);

        // 동기화 메타 파일 및 인메모리 정보 업데이트 (각 삭제된 파일에 대해)
        for (Path path : deletedPathList) {
            try {
                syncInfo.applyDelete(initiatorName, initiatorDeviceUuid, path);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        // cursor 구하기 (applyDelete 후)
        long lastChangeId = syncInfo.getIndexRegistry().getOrLoad(initiatorName, initiatorDeviceUuid).lastChangeId();

        // 이벤트 전송 루프 시작
        int listIndex = 0;
        boolean sendResult = false;
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        Path syncHome;
        if (confInfo.getSystemType().equals("SERVER")) {
            syncHome = getServerSyncHome(initiatorName);
        } else {
            syncHome = getClientSyncHome();
        }
        List<String> relativeDeletedPathList = toRelativeStringList(deletedPathList, syncHome);
        while (listIndex < relativeDeletedPathList.size()) {
            // 이벤트 객체 생성
            CMFileSyncEventCompleteDeleteFiles fse = new CMFileSyncEventCompleteDeleteFiles();
            // 공통 필드 설정
            fse.setInitiatorName(initiatorName);
            fse.setInitiatorUuid(initiatorUuid);
            fse.setInitiatorDeviceUuid(initiatorDeviceUuid);

            // 이벤트에 넣을 path 서브리스트 구하기
            List<String> subList = createSubStringListForEvent(fse.getByteNum(), relativeDeletedPathList, listIndex);
            // update the listIndex
            listIndex += subList.size();
            // set the subList to the event
            fse.setDeletedPathList(subList);
            // cursor 설정
            fse.setCursor(lastChangeId);
            // send the event
            sendResult = CMEventManager.unicastEvent(fse, initiatorName, initiatorUuid);
            if (!sendResult) {
                System.err.println("send error: " + fse);
                return false;
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("sent event = " + fse);
            }
        }

        return true;
    }

    // called by the server
    public boolean isCompleteFileSync(CMUserLoginKey loginKey) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.isCompleteFileSync() called..");
            System.out.println("loginKey = " + loginKey);
        }

        List<CMFileSyncEntry> newInitiatorPathEntryList = null;
        List<Path> basisFileList = null;
        List<CMFileSyncEntry> fileEntryList = null;
        int numNewFilesCompleted = 0;
        int numUpdateFilesCompleted = 0;
        int numFilesCompleted = 0;
        int numNewFilesNotCompleted = 0;
        int numUpdateFilesNotCompleted = 0;
        Map<Path, Boolean> isNewFileCompletedMap = null;
        Map<Path, Boolean> isUpdateFileCompletedMap = null;

        // get CMFileSyncGenerator object
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        String initiatorName = loginKey.getUserName();
        UUID initiatorUuid = loginKey.getUuid();
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        if (syncGenerator == null) {
            System.err.println("syncGenerator is null!");
            return false;
        }

        // compare the number of new files completed to the size of the new-file list
        newInitiatorPathEntryList = syncGenerator.getNewInitiatorPathEntryList();
        numNewFilesCompleted = syncGenerator.getNumNewFilesCompleted();
        if (!newInitiatorPathEntryList.isEmpty()) {
            if (CMInfo._CM_DEBUG) {
                System.err.println("numNewFilesCompleted = " + numNewFilesCompleted);
                System.err.println("size of newInitiatorPathEntryList = " + newInitiatorPathEntryList.size());
            }
            return false;
        }
        // get basis file list
        UUID initiatorDeviceUuid = syncGenerator.getInitiatorDeviceUuid();
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        basisFileList = syncInfo.getBasisFileListMap().get(stateKey);
        // compare the number of updated files to the size of the basis-file list
        numUpdateFilesCompleted = syncGenerator.getNumUpdateFilesCompleted();
        if (basisFileList != null && numUpdateFilesCompleted < basisFileList.size()) {
            if (CMInfo._CM_DEBUG) {
                System.err.println("numUpdateFilesCompleted = " + numUpdateFilesCompleted);
                System.err.println("size of basisFileList = " + basisFileList.size());
            }
            return false;
        }
        // compare the number of files of which sync is completed to the size of client file-entry list
        fileEntryList = syncInfo.getInitiatorPathEntryListMap().get(stateKey);
        numFilesCompleted = numNewFilesCompleted + numUpdateFilesCompleted;
        if (fileEntryList != null && numFilesCompleted < fileEntryList.size()) {
            System.err.println("numFilesCompleted = " + numFilesCompleted);
            System.err.println("size of client file-entry list = " + fileEntryList.size());
            return false;
        }
        // check each element of the isNewFileCompletedMap
        isNewFileCompletedMap = syncGenerator.getIsNewFileCompletedMap();
        numNewFilesNotCompleted = 0;
        for (Map.Entry<Path, Boolean> entry : isNewFileCompletedMap.entrySet()) {
            Path k = entry.getKey();
            Boolean v = entry.getValue();
            if (!v) {
                numNewFilesNotCompleted++;
                System.err.println("new file path='" + k + '\'' + ", value=" + v);
            }
        }
        if (numNewFilesNotCompleted > 0) {
            System.err.println("numNewFilesNotCompleted = " + numNewFilesNotCompleted);
            return false;
        }
        // check each element of the isUpdateFileCompletedMap
        isUpdateFileCompletedMap = syncGenerator.getIsUpdateFileCompletedMap();
        numUpdateFilesNotCompleted = 0;
        for (Map.Entry<Path, Boolean> entry : isUpdateFileCompletedMap.entrySet()) {
            Path k = entry.getKey();
            Boolean v = entry.getValue();
            if (!v) {
                numUpdateFilesNotCompleted++;
                System.err.println("update file path='" + k + '\'' + ", value=" + v);
            }
        }
        if (numUpdateFilesNotCompleted > 0) {
            System.err.println("numUpdateFilesNotCompleted = " + numUpdateFilesNotCompleted);
            return false;
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("The sync of all files is completed.");
        }

        return true;
    }

    // called by the server
    public boolean completeFileSync(CMUserLoginKey loginKey) {
        String initiatorName = loginKey.getUserName();
        UUID initiatorUuid = loginKey.getUuid();
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.completeFileSync() called..");
            System.out.println("initiatorName = " + initiatorName);
            System.out.println("initiatorUuid = " + initiatorUuid);
        }
        // [10-3] full-push 세션 종료 = per-user push lease 해제 지점(§2.6 (ii)). deviceUuid 는 generator 에서
        // 얻어 stateKey 를 구성하고, 정상/실패 무관하게 finally 에서 해제한다(deleteFileSyncInfo 가 generator 를
        // 제거하므로 반드시 그 전에 캡처).
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncGenerator gen = syncInfo.getSyncGeneratorMap().get(loginKey);
        CMFileSyncStateKey leaseKey = (gen != null)
                ? new CMFileSyncStateKey(initiatorName, gen.getInitiatorDeviceUuid())
                : null;
        try {
            // send the file-sync completion event
            boolean result = sendCompleteFileSync(loginKey);
            if (!result) return false;
            deleteFileSyncInfo(loginKey);
            return true;
        } finally {
            if (leaseKey != null) {
                syncInfo.releasePushLease(initiatorName, leaseKey);
            }
        }
    }

    // called by the server
    private boolean sendCompleteFileSync(CMUserLoginKey loginKey) {
        String initiatorName = loginKey.getUserName();
        UUID initiatorUuid = loginKey.getUuid();
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.sendCompleteFileSync() called..");
            System.out.println("initiatorName = " + initiatorName);
            System.out.println("initiatorUuid = " + initiatorUuid);
        }

        // get the CMFileSyncGenerator reference
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        if (syncGenerator == null) {
            System.err.println("syncGenerator is null!");
            return false;
        }

        // device uuid 구하기
        UUID deviceUuid = syncGenerator.getInitiatorDeviceUuid();
        // cursor 구하기
        long lastChangeId = syncInfo.getIndexRegistry().getOrLoad(initiatorName, deviceUuid).lastChangeId();

        // create a COMPLETE_FILE_SYNC event
        int numFilesCompleted = syncGenerator.getNumNewFilesCompleted() + syncGenerator.getNumUpdateFilesCompleted();
        CMFileSyncEventCompleteFileSync fse = new CMFileSyncEventCompleteFileSync();
        // 공통 필드 설정
        fse.setInitiatorName(initiatorName);
        fse.setInitiatorUuid(initiatorUuid);
        fse.setInitiatorDeviceUuid(deviceUuid);
        // 나머지 필드 설정
        fse.setNumFilesCompleted(numFilesCompleted);
        fse.setCursor(lastChangeId);

        // send the event
        return CMEventManager.unicastEvent(fse, initiatorName, initiatorUuid);
    }

    // called by the server
    private void deleteFileSyncInfo(CMUserLoginKey loginKey) {
        String initiatorName = loginKey.getUserName();
        UUID initiatorUuid = loginKey.getUuid();
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.deleteFileSyncInfo() called..");
            System.out.println("initiatorName = " + initiatorName);
            System.out.println("initiatorUuid = " + initiatorUuid);
        }
        // get CMFileSyncInfo reference
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        // remove elements in fileEntryListMap
        CMFileSyncGenerator syncGenerator = syncInfo.getSyncGeneratorMap().get(loginKey);
        if (syncGenerator == null) {
            System.err.println("syncGenerator is null!");
            return;
        }
        CMFileSyncStateKey stateKey = new CMFileSyncStateKey(initiatorName, syncGenerator.getInitiatorDeviceUuid());
        syncInfo.getInitiatorPathEntryListMap().remove(stateKey);
        // remove elements in syncGeneratorMap
        syncInfo.getSyncGeneratorMap().remove(loginKey);
    }

    // called by the server
    public int calculateWeakChecksum(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateWeakChecksum() called..");
            System.out.println("ByteBuffer remaining size = " + buffer.remaining());
        }
        int[] abs = calculateWeakChecksumElements(buffer);

        if (CMInfo._CM_DEBUG) {
            System.out.println("weak checksum = " + abs[2]);
        }
        return abs[2];
    }

    // called by the client
    // reference: http://tutorials.jenkov.com/rsync/checksums.html
    public int[] calculateWeakChecksumElements(ByteBuffer buffer) {
        Objects.requireNonNull(buffer);
        if (CMInfo._CM_DEBUG_2) {
            System.out.println("=== CMFileSyncManager.calculateWeakChecksumElements() called..");
            System.out.println("ByteBuffer remaining size = " + buffer.remaining());
        }

        int A = 0;
        int B = 0;
        int S = 0;
        int[] abs = new int[3]; // abs[0] = A, abs[1] = B, abs[2] = S
        int M = (int) Math.pow(2.0, 16.0);
        if (CMInfo._CM_DEBUG_2) {
            System.out.println("initial A = " + A + ", B = " + B + ", S = " + S);
            System.out.println("M = " + M);
            System.out.print("initial abs = ");
            for (int e : abs) System.out.print(e + " ");
            System.out.println();
        }

        // repeat to update A and B for each block data
        while (buffer.hasRemaining()) {
            A += buffer.get();
            B += A;
        }
        // get mod M value of A and B
        A = A % M;
        B = B % M;
        abs[0] = A;
        abs[1] = B;
        // get checksum (S) based on A and B
        S = A + M * B;
        abs[2] = S;
        if (CMInfo._CM_DEBUG_2) {
            System.out.println("A = " + A + ", B = " + B + ", S = " + S);
            System.out.println("abs = " + Arrays.toString(abs));
        }

        return abs;
    }

    public byte[] calculateStrongChecksum(ByteBuffer buffer) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateStrongChecksum() called..");
            System.out.println("ByteBuffer remaining size = " + buffer.remaining());
        }
        // get MD5
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        //md.update(buffer.array());
        md.update(buffer);
        byte[] digest = md.digest();

        if (CMInfo._CM_DEBUG) {
            String checksum = DatatypeConverter.printHexBinary(digest).toUpperCase();
            System.out.println("checksum hex binary = " + checksum);
            System.out.println("checksum array string = " + Arrays.toString(digest));
            System.out.println("length = " + digest.length + "bytes.");
        }

        return digest;
    }

    /*
    public byte[] calculateFileChecksum(Path path) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateFileChecksum() called..");
            System.out.println("path = " + path);
        }
        // get MD5
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path.toFile());
            byte[] byteArray = new byte[8192];
            int bytesCount = 0;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                md.update(byteArray, 0, bytesCount);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] bytes = md.digest();

        if (CMInfo._CM_DEBUG) {
            String checksum = DatatypeConverter.printHexBinary(bytes).toUpperCase();
            System.out.println("checksum = " + checksum);
        }

        return bytes;
    }
    */

    // called by the client
    public int[] updateWeakChecksum(int oldA, int oldB, byte oldStartByte, byte newEndByte, int blockSize) {
        if (CMInfo._CM_DEBUG_2) {
            System.out.println("=== CMFileSyncManager.updateWeakChecksum() called..");
            System.out.println("oldA = " + oldA);
            System.out.println("oldB = " + oldB);
            System.out.println("oldStartByte = " + oldStartByte);
            System.out.println("newEndByte = " + newEndByte);
            System.out.println("blockSize = " + blockSize);
        }
        // calculate rolling checksum from the previous checksum value
        int A, B, S;
        int M = (int) Math.pow(2.0, 16.0);
        int[] newABS = new int[3];

        A = oldA;
        A -= oldStartByte;
        A += newEndByte;
        A %= M;

        B = oldB;
        B -= blockSize * oldStartByte;
        B += A;
        B %= M;

        S = A + M * B;

        newABS[0] = A;
        newABS[1] = B;
        newABS[2] = S;

        if (CMInfo._CM_DEBUG_2) {
            System.out.println("A = " + A + ", B = " + B + ", S = " + S);
            System.out.println("newABS = " + Arrays.toString(newABS));
        }

        return newABS;
    }

    // calculate a checksum of a file (that is the sum of block weak checksum values)
    // The server will validate the newly created file with this file checksum.
    public int calculateWeakChecksum(Path path, int blockSize) {

        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateWeakChecksum(Path, int) called..");
            System.out.println("path = " + path);
            System.out.println("blockSize = " + blockSize);
        }
        // assign a ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        // assign related variables
        int[] weakChecksumABS;
        int fileChecksum = 0;
        int M = (int) Math.pow(2.0, 16.0);
        SeekableByteChannel channel = null;
        try {
            // open the file channel
            channel = Files.newByteChannel(path, StandardOpenOption.READ);
            // repeat to calculate a block checksum and add it to the file checksum value
            while (channel.position() < channel.size()) {
                // read the next block of the file and write to the buffer
                buffer.clear();
                channel.read(buffer);
                // calculate the weak checksum of the block
                buffer.flip();
                weakChecksumABS = calculateWeakChecksumElements(buffer);
                // add the block checksum to the current file checksum value
                fileChecksum += weakChecksumABS[2];
                fileChecksum %= M;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("fileChecksum = " + fileChecksum);
        }

        return fileChecksum;
    }

    public Path getTempPathOfBasisFile(Path basisFilePath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getTempPathOfBasisFile() called..");
            System.out.println("basisFilePath = " + basisFilePath);
        }

        String fileName = basisFilePath.getFileName().toString();
        String tempFileName = CMInfo.TEMP_FILE_PREFIX + fileName;
        Path tempBasisFilePath = basisFilePath.resolveSibling(tempFileName);

        if (CMInfo._CM_DEBUG) {
            System.out.println("tempBasisFilePath = " + tempBasisFilePath);
        }

        return tempBasisFilePath;
    }

    public boolean startWatchService() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startWatchService() called..");
        }
        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        // check if the WatchService is already started
        if (!syncInfo.isWatchServiceTaskDone()) {
            System.out.println("The watch service is already running..");
            return true;
        }

        // get ExecutorService reference
        ExecutorService es = CMThreadInfo.getInstance().getExecutorService();
        Objects.requireNonNull(es);
        // create WatchService and store the reference
        final WatchService watchService;
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        syncInfo.setWatchService(watchService);

        // check sync home directory
        Path syncHome = Objects.requireNonNull(getClientSyncHome());
        if (Files.notExists(syncHome)) {
            try {
                Files.createDirectories(syncHome);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        // create a WatchServiceTask
        CMWatchServiceTask watchTask = new CMWatchServiceTask(syncHome);
        // start the WatchServiceTask
        Future<?> future = es.submit(watchTask);
        if (future == null) {
            System.err.println("error submitting watch-service task to the ExecutorService!");
            return false;
        }
        // store the Future<?> to the syncInfo
        syncInfo.setWatchServiceFuture(future);

        return true;
    }

    public boolean stopWatchService() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.stopWatchService() called..");
        }

        // get syncInfo
        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());

        // check if the watch service is already done
        if (syncInfo.isWatchServiceTaskDone()) {
            System.out.println("The watch service has already stopped..");
            return true;
        }

        // get WatchService reference
        WatchService watchService = syncInfo.getWatchService();
        if (watchService == null) {
            System.err.println("WatchService reference is null!");
            return false;
        }
        // stop the WatchService
        try {
            watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // get the Future<?> reference of the watch-service task
        Future<?> watchFuture = syncInfo.getWatchServiceFuture();
        if (watchFuture == null) {
            System.err.println("Future<?> of the watch-service task is null!");
            return false;
        }
        // wait until the task is done
        try {
            watchFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
            return false;
        }
        // check if the watch-service task is done in the ExecutorService
        if (!syncInfo.isWatchServiceTaskDone()) {
            System.err.println("The watch-service task is not done!");
            return false;
        }
        // initialize the WatchService and WatchService task references
        syncInfo.setWatchService(null);
        syncInfo.setWatchServiceFuture(null);

        return true;
    }

    public boolean requestOnlineMode(List<Path> pathList) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.requestOnlineMode() called..");
            System.out.println("pathList = " + pathList);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        // check current file-sync mode
        if (syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.err.println("Current file-sync mode is OFF!");
            return false;
        }

        // check if current file-sync status
        if (syncInfo.isSyncInProgress()) {
            System.err.println("Currently file-sync task is working! You should wait!");
            return false;
        }
        // check if the watch service is running
        if (syncInfo.isWatchServiceTaskDone()) {
            System.err.println("The file-sync monitoring stops! You should start the file-sync!");
            return false;
        }

        // check argument
        if (pathList == null) {
            System.err.println("The argument pathList is null!");
            return false;
        }

        // change the file-sync status
        syncInfo.setSyncProgress(CMFileSyncProgress.ONLINE_MODE);
        // stop the watch service
        boolean ret = stopWatchService();
        if (!ret) {
            System.err.println("error stopping WatchService!");
            return false;
        }

        //// extending all directory paths in the path list to file paths

        // extracting file-only list in the argument path list
        List<Path> fileOnlyList = pathList.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());
        // extracting directory-only list in the argument path list
        List<Path> dirOnlyList = pathList.stream()
                .filter(path -> Files.isDirectory(path))
                .collect(Collectors.toList());
        // extend each directory to a list of files and added to the file-only list
        ret = false;
        for (Path dir : dirOnlyList) {
            try {
                List<Path> fileList = Files.walk(dir)
                        .filter(path -> !Files.isDirectory(path))
                        .collect(Collectors.toList());
                if (!fileList.isEmpty()) {
                    ret = fileOnlyList.addAll(fileList);
                    if (!ret) {
                        System.err.println("error to add files in " + dir);
                        continue;
                    }
                    if (CMInfo._CM_DEBUG) {
                        System.out.println("files in " + dir + " added to the file-only list");
                        for (Path path : fileList)
                            System.out.println("path = " + path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("file only list: ");
            for (Path path : fileOnlyList)
                System.out.println("path = " + path);
        }

        /*// take out path element that is already the online mode in the file-only list
        List<Path> onlineModePathList = Objects.requireNonNull(syncInfo.getOnlineModePathList());
        List<Path> filteredFileOnlyList = fileOnlyList.stream()
                .filter(path -> !onlineModePathList.contains(path))
                .collect(Collectors.toList());

        if(filteredFileOnlyList.isEmpty()) {
            System.err.println("Selected files are already online mode ones!");
            return false;
        }

        if(CMInfo._CM_DEBUG) {
            System.out.println("filtered file only list: ");
            for(Path path : filteredFileOnlyList)
                System.out.println("path = " + path);
        }*/

        //// create and send an online-mode-list event

        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        String initiatorName = interInfo.getMyself().getName();
        UUID initiatorUuid = interInfo.getMyself().getUuid();
        UUID initiatorDeviceUuid = syncInfo.getDeviceUuid();
        String serverName = interInfo.getDefaultServerInfo().getServerName();
        // convert to relative string list (forward-slash normalized for cross-OS transfer)
        List<String> relativeFileOnlyList = toRelativeStringList(fileOnlyList, getClientSyncHome());
        // event transmission loop
        int listIndex = 0;
        boolean sendResult = false;
        while (listIndex < relativeFileOnlyList.size()) {
            // create an event
            CMFileSyncEventOnlineModeList listEvent = new CMFileSyncEventOnlineModeList();
            // 공통 필드 설정
            listEvent.setInitiatorName(initiatorName);
            listEvent.setInitiatorUuid(initiatorUuid);
            listEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);

            // get relative path list to be added to this event
            List<String> subList = createSubStringListForEvent(listEvent.getByteNum(), relativeFileOnlyList, listIndex);
            // update the listIndex
            listIndex += subList.size();
            // set the sublist to the event
            listEvent.setRelativePathList(subList);
            // send the event
            sendResult = CMEventManager.unicastEvent(listEvent, serverName, null);
            if (!sendResult) {
                System.err.println("send error: " + listEvent);
                return false;
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("sent listEvent = " + listEvent);
            }
        }

        // add filteredFileOnlyList to the online-mode-request queue
        ConcurrentLinkedQueue<Path> onlineModeRequestQueue = syncInfo.getOnlineModeRequestQueue();
        Objects.requireNonNull(onlineModeRequestQueue);
        ret = onlineModeRequestQueue.addAll(fileOnlyList);
        if (!ret) {
            System.err.println("error to add filteredFileOnlyList to the online-mode-request queue!");
            return false;
        }

        return true;
    }

    private List<String> createSubStringListForEvent(int initialByteNum, List<String> relativeStringList, int listIndex) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.createSubStringListForEvent() called..");
            System.out.println("initialByteNum = " + initialByteNum);
            System.out.println("relativeStringList = " + relativeStringList);
            System.out.println("listIndex = " + listIndex);
        }

        int curByteNum = initialByteNum;
        List<String> subList = new ArrayList<>();

        boolean ret = false;
        for (int i = listIndex; i < relativeStringList.size(); i++) {
            String relativeStr = relativeStringList.get(i);
            curByteNum += CMInfo.STRING_LEN_BYTES_LEN + relativeStr.getBytes().length;
            if (curByteNum < CMInfo.MAX_EVENT_SIZE) {
                ret = subList.add(relativeStr);
                if (!ret) {
                    System.err.println("error to add " + relativeStr);
                    return null;
                }
            } else {
                break;
            }
        }
        return subList;
    }

    public boolean requestLocalMode(List<Path> pathList) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.requestLocalMode() called..");
            System.out.println("pathList = " + pathList);
        }

        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        // check current file-sync mode
        if (syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.err.println("Current file-sync mode is OFF!");
            return false;
        }

        // check if current file-sync status
        if (syncInfo.isSyncInProgress()) {
            System.err.println("Currently file-sync task is working! You should wait!");
            return false;
        }
        // check if the watch service is running
        if (syncInfo.isWatchServiceTaskDone()) {
            System.err.println("The file-sync monitoring stops! You should start the file-sync!");
            return false;
        }

        // check argument
        if (pathList == null) {
            System.err.println("The argument pathList is null!");
            return false;
        }

        // change the file-sync status
        syncInfo.setSyncProgress(CMFileSyncProgress.LOCAL_MODE);
        // stop the watch service
        boolean ret = stopWatchService();
        if (!ret) {
            System.err.println("error stopping WatchService!");
            return false;
        }

        //// extending all directory paths in the path list to file paths

        // extracting file-only list in the argument path list
        List<Path> fileOnlyList = pathList.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());
        // extracting directory-only list in the argument path list
        List<Path> dirOnlyList = pathList.stream()
                .filter(path -> Files.isDirectory(path))
                .collect(Collectors.toList());
        // extend each directory to a list of files and added to the file-only list
        ret = false;
        for (Path dir : dirOnlyList) {
            try {
                List<Path> fileList = Files.walk(dir)
                        .filter(path -> !Files.isDirectory(path))
                        .collect(Collectors.toList());
                if (!fileList.isEmpty()) {
                    ret = fileOnlyList.addAll(fileList);
                    if (!ret) {
                        System.err.println("error to add files in " + dir);
                        continue;
                    }
                    if (CMInfo._CM_DEBUG) {
                        System.out.println("files in " + dir + " added to the file-only list");
                        for (Path path : fileList)
                            System.out.println("path = " + path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("file only list: ");
            for (Path path : fileOnlyList)
                System.out.println("path = " + path);
        }
/*
        // take out path element that is already the local mode in the file-only list
        // The local mode files are not in the online mode list.
        List<Path> onlineModePathList = Objects.requireNonNull(syncInfo.getOnlineModePathList());
        List<Path> filteredFileOnlyList = fileOnlyList.stream()
                .filter(path -> onlineModePathList.contains(path))
                .collect(Collectors.toList());

        if(filteredFileOnlyList.isEmpty()) {
            System.err.println("Selected files are already local mode ones!");
            return false;
        }

        if(CMInfo._CM_DEBUG) {
            System.out.println("filtered file only list: ");
            for(Path path : filteredFileOnlyList)
                System.out.println("path = " + path);
        }
*/

        //// create and send a local-mode-list event

        // get the user and server names
        CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
        String initiatorName = interInfo.getMyself().getName();
        UUID initiatorUuid = interInfo.getMyself().getUuid();
        UUID initiatorDeviceUuid = syncInfo.getDeviceUuid();
        String serverName = interInfo.getDefaultServerInfo().getServerName();
        // convert to relative string list (forward-slash normalized for cross-OS transfer)
        List<String> relativeFileOnlyList = toRelativeStringList(fileOnlyList, getClientSyncHome());
        // event transmission loop
        int listIndex = 0;
        boolean sendResult = false;
        while (listIndex < relativeFileOnlyList.size()) {
            // create an event
            CMFileSyncEventLocalModeList listEvent = new CMFileSyncEventLocalModeList();
            // 공통 필드 설정
            listEvent.setInitiatorName(initiatorName);
            listEvent.setInitiatorUuid(initiatorUuid);
            listEvent.setInitiatorDeviceUuid(initiatorDeviceUuid);

            // get relative path list to be added to this event
            List<String> subList = createSubStringListForEvent(listEvent.getByteNum(), relativeFileOnlyList, listIndex);
            // update the listIndex
            listIndex += subList.size();
            // set the sublist to the event
            listEvent.setRelativePathList(subList);
            // send the event
            sendResult = CMEventManager.unicastEvent(listEvent, serverName, null);
            if (!sendResult) {
                System.err.println("send error: " + listEvent);
                return false;
            }
            if (CMInfo._CM_DEBUG) {
                System.out.println("sent listEvent = " + listEvent);
            }
        }

        // add filteredFileOnlyList to the local-mode-request queue
        ConcurrentLinkedQueue<Path> localModeRequestQueue = syncInfo.getLocalModeRequestQueue();
        Objects.requireNonNull(localModeRequestQueue);
        ret = localModeRequestQueue.addAll(fileOnlyList);
        if (!ret) {
            System.err.println("error to add filteredFileOnlyList to the local-mode-request queue!");
            return false;
        }

        return true;
    }

    // called at the client
    public void checkTransferForLocalMode(CMFileEvent fe) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.checkTransferForLocalMode() called..");
            System.out.println("fe = " + fe);
        }

        // get transferred file info
        String fileName = fe.getFileName();
        String fileSender = fe.getFileSender();
        UUID fileSenderUuid = fe.getFileSenderUuid();
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        Path transferFileHome = confInfo.getTransferedFileHome();

        // get local-mode-request queue
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        ConcurrentLinkedQueue<Path> localModeRequestQueue = syncInfo.getLocalModeRequestQueue();
        Objects.requireNonNull(localModeRequestQueue);

        // compare the queue head (absolute path) and the transferred file name
        Path headPath = localModeRequestQueue.peek();
        if (headPath == null) {
            if (CMInfo._CM_DEBUG) {
                System.err.println("Local-mode-request queue is empty!");
            }
            return;
        }
        if (!headPath.endsWith(fileName)) {
            System.err.println("Head of local-mode-request queue does not match the transferred file name!");
            System.out.println("headPath = " + headPath);
            System.out.println("fileName = " + fileName);
            return;
        }

        try {
            // save the last modified time of the queue head path
            FileTime lastModifiedTime = Files.getLastModifiedTime(headPath);
            // move the transferred file to the sync home
            Files.move(transferFileHome.resolve(fileName), headPath, StandardCopyOption.REPLACE_EXISTING);
            // restore the last modified time
            Files.setLastModifiedTime(headPath, lastModifiedTime);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // delete head from the request queue
        localModeRequestQueue.remove();
        // delete path from the online-mode-list
/*
        List<Path> onlineModeList = Objects.requireNonNull(syncInfo.getOnlineModePathList());
        boolean ret = onlineModeList.remove(headPath);
        if (!ret) {
            System.err.println("remove error from the online-mode-list: " + headPath);
        }
*/
        // delete path from the online-mode-map
        Map<Path, Long> onlineModePathSizeMap = syncInfo.getOnlineModePathSizeMap();
        Objects.requireNonNull(onlineModePathSizeMap);
        Long size = onlineModePathSizeMap.remove(headPath);
        if (size == null) {
            System.err.println("remove error from the online-mode-map: " + headPath);
        }

        // check if the queue is not empty
        if (!localModeRequestQueue.isEmpty()) {
            System.out.println("Local-mode-request queue is not empty.");
            return;
        }
        // if the queue is empty, create and send an end-local-mode-list event
        CMFileSyncEventEndLocalModeList endEvent = new CMFileSyncEventEndLocalModeList();
        // 공통 필드 설정
        endEvent.setInitiatorName(fe.getFileReceiver());
        endEvent.setInitiatorUuid(fe.getFileReceiverUuid());
        endEvent.setInitiatorDeviceUuid(syncInfo.getDeviceUuid());

        // filter only file type from the path list
        List<Path> pathList = Objects.requireNonNull(syncInfo.getPathList());
        List<Path> filteredPathList = pathList.stream()
                .filter(path -> !Files.isDirectory(path))
                .collect(Collectors.toList());
        // get the number of local-mode files
        //int numLocalModeFiles = filteredPathList.size() - syncInfo.getOnlineModePathList().size();
        int numLocalModeFiles = filteredPathList.size() - syncInfo.getOnlineModePathSizeMap().size();
        endEvent.setNumLocalModeFiles(numLocalModeFiles);

        boolean ret = CMEventManager.unicastEvent(endEvent, fileSender, fileSenderUuid);
        if (!ret) {
            System.err.println("send error: " + endEvent);
        }

        return;
    }

    // called at the client
    public boolean startFileSync(CMFileSyncMode fileSyncMode) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startFileSync() called..");
            System.out.println("fileSyncMode = " + fileSyncMode);
        }

        // check the argument
        if (fileSyncMode == CMFileSyncMode.OFF) {
            System.err.println("The argument file-sync mode is OFF!");
            return false;
        }

        // get CMFileSyncInfo reference
        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());

        // check the current file-sync mode
        if (syncInfo.getCurrentMode() != CMFileSyncMode.OFF) {
            System.err.println("Current file-sync mode (" + syncInfo.getCurrentMode() + ") has already started!");
            return false;
        }

        // get the online-mode-list-file path
        //Path storedListPath = Paths.get(CMInfo.SETTINGS_DIR, CMFileSyncInfo.ONLINE_MODE_LIST_FILE_NAME);
        Path storedListPath = Paths.get(CMInfo.SETTINGS_DIR, CMFileSyncInfo.ONLINE_MODE_MAP_FILE);
        // load the online-mode-list file
/*
        List<Path> onlineModePathList = loadPathListFromFile(storedListPath);
        if (onlineModePathList != null) {
            // set to CMFileSyncInfo
            syncInfo.setOnlineModePathList(onlineModePathList);
        } else {
            System.err.println("The loaded online-mode-path-list is null!");
        }
*/
        // load the online-mode-map file
        Map<Path, Long> onlineModePathSizeMap = loadPathSizeMapFromFile(storedListPath);
        if (onlineModePathSizeMap != null) {
            // set to CMFileSyncInfo
            syncInfo.setOnlineModePathSizeMap(onlineModePathSizeMap);
        } else {
            System.err.println("The loaded online-mode-map is null!");
        }

        // start the watch service
        boolean ret = startWatchService();
        if (!ret) {
            System.err.println("error starting watch service!");
            return false;
        }

        // check if the file-sync mode is AUTO
        if (fileSyncMode == CMFileSyncMode.AUTO) {
            // start the proactive mode task
            ret = startProactiveMode();
            if (!ret) {
                System.err.println("error to start proactive mode!");
                return false;
            }
            // update the current file-sync mode to AUTO
            syncInfo.setCurrentMode(CMFileSyncMode.AUTO);
        } else {
            // update the current file-sync mode to MANUAL
            syncInfo.setCurrentMode(CMFileSyncMode.MANUAL);
        }

        // conduct file-sync task once (client starts bidirectional sync via pull handshake)
        ret = startPullSync();
        if (!ret) {
            System.err.println("error starting file-sync!");
            return false;
        }

        return true;
    }

    // called at the client
    private boolean startProactiveMode() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startProactiveMode() called..");
        }
        // check if a proactive mode task is already running
        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        if (!syncInfo.isProactiveModeTaskDone()) {
            System.err.println("A proactive mode task is already running!");
            return false;
        }

        // get the scheduled-executor-service reference
        ScheduledExecutorService ses = CMThreadInfo.getInstance().getScheduledExecutorService();
        Objects.requireNonNull(ses);
        // get directory-activation-monitoring-period
        CMConfigurationInfo confInfo = Objects.requireNonNull(CMConfigurationInfo.getInstance());
        long period = confInfo.getDirActivationMonitoringPeriod();
        TimeUnit unit = confInfo.getDirActivationMonitoringPeriodUnit();
        // create a scheduled proactive mode task
        CMFileSyncProactiveModeTask proactiveModeTask = new CMFileSyncProactiveModeTask(
        );
        ScheduledFuture<?> scheduledFuture = ses.scheduleWithFixedDelay(proactiveModeTask, period, period, unit);
        if (scheduledFuture == null) {
            System.err.println("error to call scheduleWithFixedDelay()!");
            return false;
        }
        // set the task future reference
        syncInfo.setProactiveModeTaskFuture(scheduledFuture);

        return true;
    }

    // called at the client
    private boolean stopProactiveMode() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.stopProactiveMode() called..");
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        // check if the proactive-mode task has already done
        if (syncInfo.isProactiveModeTaskDone()) {
            System.out.println("The proactive-mode task has already done.");
            return true;
        }

        // get Future reference of the proactive-mode task
        ScheduledFuture<?> proactiveModeTaskFuture = syncInfo.getProactiveModeTaskFuture();
        if (proactiveModeTaskFuture == null) {
            System.err.println("proactiveModeTaskFuture is null!");
            return false;
        }
        // cancel the proactive-mode task
        proactiveModeTaskFuture.cancel(true);
        try {
            proactiveModeTaskFuture.get(5, TimeUnit.SECONDS);
        } catch (CancellationException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
            return false;
        }
        // check the state of the proactive-mode task
        if (!syncInfo.isProactiveModeTaskDone()) {
            System.err.println("The proactive-mode task is still running!");
            return false;
        }

        // initialize the future reference in CMFileSyncInfo
        syncInfo.setProactiveModeTaskFuture(null);

        return true;
    }

/*    private List<Path> loadPathListFromFile(Path listPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.loadPathListFromFile() called..");
            System.out.println("storedListPath = " + listPath);
        }

        if (listPath == null) {
            System.err.println("The argument list path is null!");
            return null;
        }
        if (!Files.exists(listPath)) {
            System.err.println("The argument (" + listPath + ") does not exists!");
            return null;
        }

        List<Path> list;
        try {
            list = Files.lines(listPath).map(Path::of).collect(Collectors.toList());

            if (CMInfo._CM_DEBUG) {
                System.out.println("--- loaded online-mode path list: ");
                for (Path path : list)
                    System.out.println("path = " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return list;
    }*/

    // called at the client
    private Map<Path, Long> loadPathSizeMapFromFile(Path storedPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.loadPathSizeMapFromFile() called..");
            System.out.println("storedPath = " + storedPath);
        }
        // check if the argument is null
        if (storedPath == null) {
            System.err.println("The argument path is null!");
            return null;
        }
        // check if the argument path exists
        if (!Files.exists(storedPath)) {
            System.err.println("The argument (" + storedPath + ") does not exists!");
            return null;
        }
        // read line-by-line from the file and add (key, value) to the online-mode-map
        Map<Path, Long> pathSizeMap;
        try {
            pathSizeMap = Files.lines(storedPath)
                    .map(line -> line.split(" "))
                    .collect(Collectors.toMap(tokens -> Paths.get(tokens[0]),
                            tokens -> Long.parseLong(tokens[1])));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return pathSizeMap;
    }

    // called at the client
    public boolean stopFileSync() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.stopFileSync() called..");
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());

        // check current file-sync mode
        if (syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.out.println("Current file-sync mode is already " + syncInfo.getCurrentMode());
            return true;
        }

        // save the online-mode-path list to the file
        //boolean ret = saveOnlineModeListToFile();
        // save the online-mode-map to the file
        boolean ret = saveOnlineModePathSizeMapToFile();
        if (!ret) {
            System.err.println("error to save the online-mode-path-list to file!");
        }

        // set sync progress to none
        syncInfo.setSyncProgress(CMFileSyncProgress.NONE);

        // check file-sync mode
        if (syncInfo.getCurrentMode() == CMFileSyncMode.AUTO) {
            ret = stopProactiveMode();
            if (!ret) {
                System.err.println("error to stop proactive-mode task!");
                return false;
            }
        }

        // stop the watch service
        ret = stopWatchService();
        if (!ret) {
            System.err.println("error to stop watch service!");
            return false;
        }

        // update the current file-sync mode
        syncInfo.setCurrentMode(CMFileSyncMode.OFF);

        return true;
    }

    // called at the client
    public boolean saveOnlineModePathSizeMapToFile() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.saveOnlineModePathSizeMapToFile() called..");
        }
        // get online-mode-map
        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        Map<Path, Long> onlineModePathSizeMap = syncInfo.getOnlineModePathSizeMap();
        Objects.requireNonNull(onlineModePathSizeMap);
        // get online-mode-map file path
        Path storedPath = Paths.get(CMInfo.SETTINGS_DIR, CMFileSyncInfo.ONLINE_MODE_MAP_FILE);
        // save map to the file
        boolean ret = savePathSizeMapToFile(onlineModePathSizeMap, storedPath);
        if (!ret) {
            System.err.println("error to save the online-mode-map to file!");
            return false;
        }
        return true;
    }

    // called at the client
    private boolean savePathSizeMapToFile(Map<Path, Long> map, Path storedPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.savePathSizeMapToFile() called..");
            System.out.println("map = " + map);
            System.out.println("storedPath = " + storedPath);
        }
        // check arguments
        if (map == null || storedPath == null) {
            System.err.println("The argument map or path is null!");
            return false;
        }
        // check the stored file
        if (!Files.exists(storedPath)) {
            Path parentPath = storedPath.getParent();
            if (parentPath != null) {
                try {
                    Files.createDirectories(parentPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        // create or open file
        try (BufferedWriter writer = Files.newBufferedWriter(storedPath)) {
            for (Map.Entry<Path, Long> entry : map.entrySet()) {
                Path path = entry.getKey();
                long size = entry.getValue();
                writer.write(path.toString() + " " + Long.toString(size));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean createTestFile(Path path, long size) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.crateTestFile() called..");
            System.out.println("path = " + path);
            System.out.println("size = " + size);
        }

        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {

            // declare relevant variables
            final int arraySize = 1024;
            byte[] byteArray = new byte[arraySize];
            long remainingBytes = size;
            ByteBuffer byteBuffer = ByteBuffer.allocate(arraySize);
            int numBytesWritten = 0;

            // create a Random object
            Random random = new Random();
            while (remainingBytes > 0) {
                // get random bytes array
                random.nextBytes(byteArray);
                // init byteBuffer
                byteBuffer.clear();
                // write array to byteBuffer
                if (remainingBytes < arraySize) {
                    byteBuffer.put(byteArray, 0, (int) remainingBytes);
                } else {
                    byteBuffer.put(byteArray);
                }
                // write byteBuffer to the file channel
                byteBuffer.flip();
                numBytesWritten = channel.write(byteBuffer);
                // update remainingBytes
                remainingBytes -= numBytesWritten;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

/*
    public boolean createModifiedTestFile(Path path, Path modPath, int percentage) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.createModifiedTestFile() called..");
            System.out.println("path = " + path);
            System.out.println("modPath = " + modPath);
            System.out.println("percentage = " + percentage + " %");
        }

        // copy source file (path) to target file (modPath)
        try {
            Files.copy(path, modPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try (SeekableByteChannel channel = Files.newByteChannel(modPath, StandardOpenOption.WRITE)) {
            // get the file size
            long size = channel.size();
            // get the modification size
            long modifiedSize = size * percentage / 100;
            // declare relevant variables
            final int arraySize = 1024;
            byte[] byteArray = new byte[arraySize];
            long remainingBytes = modifiedSize;
            ByteBuffer byteBuffer = ByteBuffer.allocate(arraySize);
            int numBytesWritten = 0;

            // create a Random object
            Random random = new Random();
            while (remainingBytes > 0) {
                // get random bytes array
                random.nextBytes(byteArray);
                // init byteBuffer
                byteBuffer.clear();
                // write array to byteBuffer
                if (remainingBytes < arraySize) {
                    byteBuffer.put(byteArray, 0, (int) remainingBytes);
                } else {
                    byteBuffer.put(byteArray);
                }
                // write byteBuffer to the file channel
                byteBuffer.flip();
                numBytesWritten = channel.write(byteBuffer);
                // update remainingBytes
                remainingBytes -= numBytesWritten;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
*/

    public boolean createModifiedTestFile(Path path, Path modPath, CMTestFileModType modType, int percentage) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.createModifiedTestFile() called..");
            System.out.println("path = " + path);
            System.out.println("modPath = " + modPath);
            System.out.println("modType = " + modType);
            System.out.println("percentage = " + percentage);
        }

        // copy the source file (path) to the target file (modPath)
        try {
            Files.copy(path, modPath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        SeekableByteChannel channel = null;
        try {
            // open the target channel
            if(modType == CMTestFileModType.APPEND)
                channel = Files.newByteChannel(modPath, StandardOpenOption.APPEND);
            else channel = Files.newByteChannel(modPath, StandardOpenOption.WRITE);

            // calculate number of bytes to be modified
            long size = channel.size();
            long modifiedSize = size * percentage / 100;

            // if modType is TRUNC, truncate the target file
            if(modType == CMTestFileModType.TRUNC) {
                channel.truncate(size - modifiedSize);
            }
            else {
                // declare variables to write new bytes
                final int arraySize = 1024;
                byte[] byteArray = new byte[arraySize];
                long remainingBytes = modifiedSize;
                ByteBuffer byteBuffer = ByteBuffer.allocate(arraySize);
                int numBytesWritten = 0;
                // write new random bytes to the target channel until the remaining bytes becomes 0
                Random random = new Random();
                while(remainingBytes > 0) {
                    // get random bytes array
                    random.nextBytes(byteArray);
                    // clear byteBuffer
                    byteBuffer.clear();
                    // write byteArray to byteBuffer
                    if(remainingBytes < arraySize) byteBuffer.put(byteArray, 0, (int) remainingBytes);
                    else byteBuffer.put(byteArray);
                    // write byteBuffer to the file channel
                    byteBuffer.flip();
                    numBytesWritten = channel.write(byteBuffer);
                    // update remainingBytes
                    remainingBytes -= numBytesWritten;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    // URL: https://stackoverflow.com/questions/2972986/
    // how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java/19447758#19447758
    public void closeDirectBuffer(ByteBuffer cb) {
        if (cb == null || !cb.isDirect()) return;
        // we could use this type cast and call functions without reflection code,
        // but static import from sun.* package is risky for non-SUN virtual machine.
        //try { ((sun.nio.ch.DirectBuffer)cb).cleaner().clean(); } catch (Exception ex) { }

        // JavaSpecVer: 1.6, 1.7, 1.8, 9, 10
        boolean isOldJDK = System.getProperty("java.specification.version", "99").startsWith("1.");
        try {
            if (isOldJDK) {
                Method cleaner = cb.getClass().getMethod("cleaner");
                cleaner.setAccessible(true);
                Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
                clean.setAccessible(true);
                clean.invoke(cleaner.invoke(cb));
            } else {
                Class unsafeClass;
                try {
                    unsafeClass = Class.forName("sun.misc.Unsafe");
                } catch (Exception ex) {
                    // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
                    // but that method should be added if sun.misc.Unsafe is removed.
                    unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                }
                Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
                clean.setAccessible(true);
                Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                Object theUnsafe = theUnsafeField.get(null);
                clean.invoke(theUnsafe, cb);
            }
        } catch (Exception ex) {
        }
        cb = null;
    }

    // called at the client
    public synchronized List<Path> getSyncDirectoryList() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getSyncDirectoryList() called..");
        }

        // get file-sync home
        Path syncHome = getClientSyncHome();
        Objects.requireNonNull(syncHome);
        // get the list of directory paths
        List<Path> dirList;
        try {
            dirList = Files.walk(syncHome)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (CMInfo._CM_DEBUG) {
            dirList.forEach(System.out::println);
        }

        return dirList;
    }

    // called at the client
    public double calculateDirActivationRatio(Path dir) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.calculateDirActivationRatio() called..");
            System.out.println("dir = " + dir);
        }
        // check the argument
        if (!Files.isDirectory(dir)) {
            System.err.println(dir + " is not a directory!");
        }
        // get list of normal files (not directory types)
        List<Path> fileList;
        try {
            fileList = Files.walk(dir, 1)
                    .filter(p -> !Files.isDirectory(p))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        if (fileList.isEmpty()) {
            System.out.println(dir + " does not have a normal file.");
            return 0;
        }

        // get duration-since-last-access threshold (DSLAT)
        CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
        Objects.requireNonNull(confInfo);
        long durationSinceLastAccessThreshold = confInfo.getDurationSinceLastAccessThreshold();
        // get the unit of duration since last access threshold (DSLATU)
        TimeUnit unit = confInfo.getDurationSinceLastAccessThresholdUnit();
        if (CMInfo._CM_DEBUG) {
            System.out.println("durationSinceLastAccessThreshold = " + durationSinceLastAccessThreshold);
            System.out.println("unit = " + unit);
        }

        // get the sum of file activation ratio
        double totalActivationRatio = 0;
        // get current time by DSLATU
        long currentTime = System.currentTimeMillis();
        long ct = unit.convert(currentTime, TimeUnit.MILLISECONDS);
        if (CMInfo._CM_DEBUG) {
            System.out.println("currentTime = " + currentTime);
            System.out.println("ct = " + ct + " " + unit);
        }
        for (Path path : fileList) {
            // get the last access time
            BasicFileAttributes attr = null;
            try {
                attr = Files.readAttributes(path, BasicFileAttributes.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileTime lastAccessTime = attr.lastAccessTime();
            long lat = lastAccessTime.to(unit);

            // calculate file activation ratio
            double fileActivationRatio = (double) durationSinceLastAccessThreshold / (ct - lat);
            if (fileActivationRatio > 1) fileActivationRatio = 1;
            // sum file activation ratio
            totalActivationRatio += fileActivationRatio;

            if (CMInfo._CM_DEBUG) {
                System.out.println("----- path = " + path);
                System.out.println("lastAccessTime = " + lastAccessTime);
                System.out.println("lat = " + lat + " " + unit);
                System.out.println("fileActivationRatio = " + fileActivationRatio);
                System.out.println("totalActivationRatio = " + totalActivationRatio);
            }
        }
        // get DAR (= average of FAR)
        double dirActivationRatio = totalActivationRatio / fileList.size();
        if (CMInfo._CM_DEBUG) {
            System.out.println("dirActivationRatio = " + dirActivationRatio);
        }

        return dirActivationRatio;
    }

    // called at the client
    public boolean startProactiveOnlineMode(Path dir) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startProactiveOnlineMode() called..");
            System.out.println("dir = " + dir);
        }
        // get the file-sync home dir
        Path syncHome = Objects.requireNonNull(getClientSyncHome());
        // get the root drive of the sync home
        Path root = Objects.requireNonNull(syncHome.getRoot());
        // get total space of the root drive
        long totalSpace = root.toFile().getTotalSpace();
        ///// get total space for file-sync
        // get file-sync storage ratio
        CMConfigurationInfo confInfo = Objects.requireNonNull(CMConfigurationInfo.getInstance());
        long fileSyncStorage = confInfo.getFileSyncStorage() * 1024 * 1024; // MB to Bytes
        if (fileSyncStorage == 0) {
            System.err.println("File-sync storage is 0! It must be greater than 0!");
            return false;
        }
        if (fileSyncStorage > totalSpace) {
            System.err.println("file-sync storage(" + fileSyncStorage + ") > total-space(" + totalSpace + ")!");
            return false;
        }

        /////
        ///// get used-sync-space ratio
        // get used space of the file-sync home dir
        long usedSyncSpace = getDirectorySize(syncHome);
        // calculate used sync-space ratio
        double usedStorageRatio = usedSyncSpace / (double) fileSyncStorage;
        /////
        // get used-sync-space-ratio threshold
        double usedStorageRatioThreshold = confInfo.getUsedStorageRatioThreshold();

        if (CMInfo._CM_DEBUG) {
            System.out.println("* syncHome = " + syncHome);
            System.out.println("* root = " + root);
            System.out.println("* totalSpace = " + totalSpace + " Bytes.");
            System.out.println("* fileSyncStorage = " + fileSyncStorage + " Bytes.");
            System.out.println("* usedSyncSpace = " + usedSyncSpace);
            System.out.println("* usedStorageRatio = " + usedStorageRatio);
            System.out.println("* usedStorageRatioThreshold = " + usedStorageRatioThreshold);
        }

        // check used-sync-space-ratio and threshold
        if (usedStorageRatio <= usedStorageRatioThreshold) {
            System.out.println("** No need to change any file to the online mode.");
            return true;
        }

        ///// get a list of local-mode files sorted by ascending order of last-access-time
        // get local-mode file list
        CMFileSyncInfo syncInfo = CMFileSyncInfo.getInstance();
        Objects.requireNonNull(syncInfo);
        List<Path> pathList = syncInfo.getPathList();
        List<Path> onlineModePathList = syncInfo.getOnlineModePathSizeMap().keySet().stream().toList();
        List<Path> localModePathList = pathList.stream()
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> !onlineModePathList.contains(p))
                .collect(Collectors.toList());
        if (CMInfo._CM_DEBUG) {
            System.out.println("** local-mode path list");
            localModePathList.forEach(System.out::println);
        }
        // check local-mode path list
        if (localModePathList.isEmpty()) {
            System.err.println("The local-mode path list is empty!");
            return false;
        }
        // sort the local-mode path list by ascending order of last access time
        localModePathList.sort((p1, p2) -> {
                    BasicFileAttributes attr1 = null;
                    BasicFileAttributes attr2 = null;
                    try {
                        attr1 = Files.readAttributes(p1, BasicFileAttributes.class);
                        attr2 = Files.readAttributes(p2, BasicFileAttributes.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FileTime accessTime1 = attr1.lastAccessTime();
                    FileTime accessTime2 = attr2.lastAccessTime();
                    return accessTime1.compareTo(accessTime2);
                }
        );
        if (CMInfo._CM_DEBUG) {
            System.out.println("** sorted local-mode path list");
            localModePathList.forEach(System.out::println);
        }
        /////

        ///// get the list of local-mode files to become online mode.
        long usedSyncSpaceToBeUpdated = usedSyncSpace;
        double usedStorageRatioToBeUpdated;
        List<Path> pathListToBeOnline = new ArrayList<>();
        // add files from the local-mode list to the list to be online mode.
        for (Path path : localModePathList) {
            try {
                usedSyncSpaceToBeUpdated -= Files.size(path);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            usedStorageRatioToBeUpdated = usedSyncSpaceToBeUpdated / (double) fileSyncStorage;
            pathListToBeOnline.add(path);

            if (CMInfo._CM_DEBUG) {
                System.out.println("** path = " + path + " : added to the list to be online.");
                System.out.println("usedSyncSpaceToBeUpdated = " + usedSyncSpaceToBeUpdated);
                System.out.println("usedStorageRatioToBeUpdated = " + usedStorageRatioToBeUpdated);
            }
            if (usedStorageRatioToBeUpdated <= usedStorageRatioThreshold) {
                System.out.println("** Now USR-updated(" + usedStorageRatioToBeUpdated + ") <= USRT("
                        + usedStorageRatioThreshold + ")");
                break;
            }
        }
        /////

        // request online-mode for the list
        boolean ret = requestOnlineMode(pathListToBeOnline);
        if (!ret) {
            System.err.println("error from requestOnlineMode()!");
            return false;
        }

        return true;
    }

    // called at the client
    private long getDirectorySize(Path path) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getDirectorySize() called..");
            System.out.println("path = " + path);
        }
        long size;
        try {
            size = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("size = " + size + " Bytes.");
        }

        return size;
    }

    // called at the client
    public boolean startProactiveLocalMode(Path dir) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.startProactiveLocalMode() called..");
            System.out.println("dir = " + dir);
        }
        // get the file-sync home dir
        Path syncHome = Objects.requireNonNull(getClientSyncHome());
        // get the root drive of the sync home
        Path root = Objects.requireNonNull(syncHome.getRoot());
        // get total space of the root drive
        long totalSpace = root.toFile().getTotalSpace();
        ///// get total space for file-sync
        // get file-sync storage ratio
        CMConfigurationInfo confInfo = Objects.requireNonNull(CMConfigurationInfo.getInstance());
        long fileSyncStorage = confInfo.getFileSyncStorage() * 1024 * 1024; // MB to Bytes
        if (fileSyncStorage == 0) {
            System.err.println("File-sync storage is 0! It must be greater than 0!");
            return false;
        }
        if (fileSyncStorage > totalSpace) {
            System.err.println("file-sync storage(" + fileSyncStorage + ") > total-space(" + totalSpace + ")!");
            return false;
        }

        /////
        ///// get used-sync-space ratio
        // get used space of the file-sync home dir
        long usedSyncSpace = getDirectorySize(syncHome);
        // calculate used sync-space ratio
        double usedStorageRatio = usedSyncSpace / (double) fileSyncStorage;
        /////
        // get used-sync-space-ratio threshold
        double usedStorageRatioThreshold = confInfo.getUsedStorageRatioThreshold();

        if (CMInfo._CM_DEBUG) {
            System.out.println("* syncHome = " + syncHome);
            System.out.println("* root = " + root);
            System.out.println("* totalSpace = " + totalSpace + " Bytes.");
            System.out.println("* fileSyncStorage = " + fileSyncStorage + " Bytes.");
            System.out.println("* usedSyncSpace = " + usedSyncSpace);
            System.out.println("* usedStorageRatio = " + usedStorageRatio);
            System.out.println("* usedStorageRatioThreshold = " + usedStorageRatioThreshold);
        }

        // check used-sync-space-ratio and threshold
        if (usedStorageRatio > usedStorageRatioThreshold) {
            System.out.println("** Not enough sync space to change any online-mode file to the local mode.");
            return true;
        }
        // get max-delay-access threshold
        long maxAccessDelayThreshold = confInfo.getMaxAccessDelayThreshold();
        if (CMInfo._CM_DEBUG) {
            System.out.println("maxAccessDelayThreshold = " + maxAccessDelayThreshold + " ms.");
        }
        // get input throughput from the server
        String defaultServer = CMInteractionInfo.getInstance().getDefaultServerInfo().getServerName();
        // get input throughput (MBps)
        CMInfo cmInfo = CMInfo.getInstance();
        double inputThroughput = CMCommManager.measureInputThroughput(defaultServer, null);
        // calculate minimum size (Bytes) of a file to be local mode
        long minFileSizeForLocalMode = (long) (inputThroughput * 1000000 * maxAccessDelayThreshold / 1000);
        if (CMInfo._CM_DEBUG) {
            System.out.println(String.format("inputThroughput from [%s] = %.2f MBps%n",
                    defaultServer, inputThroughput));
            System.out.println("minFileSizeForLocalMode = " + minFileSizeForLocalMode + " Bytes.");
        }
        // get online-mode file list
        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        List<Path> onlineModePathList = syncInfo.getOnlineModePathSizeMap().keySet().stream().toList();
        if (onlineModePathList.isEmpty()) {
            System.err.println("The online-mode-path list is empty!");
            return false;
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("** online-mode-path list");
            onlineModePathList.forEach(System.out::println);
        }

        // get list of online-mode files of size greater than minimum size
        // and sorted by descending order of last access time
        List<Path> bigSortedOnlineModePathList = onlineModePathList.stream()
                .filter(p -> syncInfo.getOnlineModePathSizeMap().get(p) >= minFileSizeForLocalMode)
                .sorted((p1, p2) -> {
                    BasicFileAttributes attr1 = null;
                    BasicFileAttributes attr2 = null;
                    try {
                        attr1 = Files.readAttributes(p1, BasicFileAttributes.class);
                        attr2 = Files.readAttributes(p2, BasicFileAttributes.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FileTime accessTime1 = attr1.lastAccessTime();
                    FileTime accessTime2 = attr2.lastAccessTime();
                    return accessTime2.compareTo(accessTime1);
                })
                .collect(Collectors.toList());
        if (bigSortedOnlineModePathList.isEmpty()) {
            System.err.println("sorted online-mode-path list greater than min size is empty!");
            return false;
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("** sorted online-mode-path list greater than min size");
            bigSortedOnlineModePathList.forEach(System.out::println);
        }

        ///// get the list of online-mode files to become local mode
        long usedSyncSpaceToBeUpdated = usedSyncSpace;
        double usedStorageRatioToBeUpdated = usedSyncSpace / (double) fileSyncStorage;
        List<Path> pathListToBeLocal = new ArrayList<>();
        // add files from the sorted BIG online-mode list to the list to be local mode.
        for (Path path : bigSortedOnlineModePathList) {
            usedSyncSpaceToBeUpdated += syncInfo.getOnlineModePathSizeMap().get(path);
            usedStorageRatioToBeUpdated = usedSyncSpaceToBeUpdated / (double) fileSyncStorage;
            pathListToBeLocal.add(path);

            if (CMInfo._CM_DEBUG) {
                System.out.println("** path = " + path + " : added to the list to be local.");
                System.out.println("usedSyncSpaceToBeUpdated = " + usedSyncSpaceToBeUpdated);
                System.out.println("usedStorageRatioToBeUpdated = " + usedStorageRatioToBeUpdated);
            }

            if (usedStorageRatioToBeUpdated > usedStorageRatioThreshold) {
                System.out.println("** Now USR-updated(" + usedStorageRatioToBeUpdated + ") > USRT("
                        + usedStorageRatioThreshold + ")");
                break;
            }
        }
        /////
        // check used-storage-ratio updated
        if (usedStorageRatioToBeUpdated > usedStorageRatioThreshold) {
            boolean ret = requestLocalMode(pathListToBeLocal);
            return ret;
        }

        ///// Here, USR <= USRT, that is, more online-mode files can be added to the list to become local
        if (CMInfo._CM_DEBUG) {
            System.out.println("** Still USR <= USRT");
        }
        // get list of the remaining online-mode files sorted by descending order of last access time
        List<Path> smallSortedOnlineModePathList = onlineModePathList.stream()
                .filter(p -> syncInfo.getOnlineModePathSizeMap().get(p) < minFileSizeForLocalMode)
                .sorted((p1, p2) -> {
                    BasicFileAttributes attr1 = null;
                    BasicFileAttributes attr2 = null;
                    try {
                        attr1 = Files.readAttributes(p1, BasicFileAttributes.class);
                        attr2 = Files.readAttributes(p2, BasicFileAttributes.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    FileTime accessTime1 = attr1.lastAccessTime();
                    FileTime accessTime2 = attr2.lastAccessTime();
                    return accessTime2.compareTo(accessTime1);
                })
                .collect(Collectors.toList());

        // add files from the sorted SMALL online-mode list to the list to be local mode.
        for (Path path : smallSortedOnlineModePathList) {
            usedSyncSpaceToBeUpdated += syncInfo.getOnlineModePathSizeMap().get(path);
            usedStorageRatioToBeUpdated = usedSyncSpaceToBeUpdated / (double) fileSyncStorage;
            pathListToBeLocal.add(path);

            if (CMInfo._CM_DEBUG) {
                System.out.println("** path = " + path + " : added to the list to be local.");
                System.out.println("usedSyncSpaceToBeUpdated = " + usedSyncSpaceToBeUpdated);
                System.out.println("usedStorageRatioToBeUpdated = " + usedStorageRatioToBeUpdated);
            }

            if (usedStorageRatioToBeUpdated > usedStorageRatioThreshold) {
                System.out.println("** Now USR-updated(" + usedStorageRatioToBeUpdated + ") > USRT("
                        + usedStorageRatioThreshold + ")");
                break;
            }
        }
        /////
        // request local-mode
        boolean ret = requestLocalMode(pathListToBeLocal);
        return ret;
    }

    // called at the client
    public boolean isOnlineMode(Path path) {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.isOnlineMode() called..");
            System.out.println("path = " + path);
        }

        // get online-mode path-size map
        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        Map<Path, Long> onlineModePathSizeMap = syncInfo.getOnlineModePathSizeMap();
        Objects.requireNonNull(onlineModePathSizeMap);
        final Long size = onlineModePathSizeMap.get(path);
        if (CMInfo._CM_DEBUG) {
            System.out.println("size = " + size);
        }
        if (size == null) {
            return false;
        }
        return true;
    }

    // called at the client
    private void writeCommonTestInfoToFile(Path resultPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.writeCommonTestInfoToFile() called..");
            System.out.println("resultPath = " + resultPath);
        }

        final CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        final CMConfigurationInfo confInfo = Objects.requireNonNull(CMConfigurationInfo.getInstance());

        try (BufferedWriter bw = Files.newBufferedWriter(resultPath);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.println("Current file-sync mode: " + syncInfo.getCurrentMode());
            pw.println("DIR_ACTIVATION_MONITORING_PERIOD: " + confInfo.getDirActivationMonitoringPeriod());
            pw.println("DIR_ACTIVATION_MONITORING_PERIOD_UNIT: " + confInfo.getDirActivationMonitoringPeriodUnit());
            pw.println("DURATION_SINCE_LAST_ACCESS_THRESHOLD: " + confInfo.getDurationSinceLastAccessThreshold());
            pw.println("DURATION_SINCE_LAST_ACCESS_THRESHOLD_UNIT: "
                    + confInfo.getDurationSinceLastAccessThresholdUnit());
            pw.println("ONLINE_MODE_THRESHOLD: " + confInfo.getOnlineModeThreshold());
            pw.println("LOCAL_MODE_THRESHOLD: " + confInfo.getLocalModeThreshold());
            pw.println("FILE_SYNC_STORAGE: " + confInfo.getFileSyncStorage());
            pw.println("USED_STORAGE_RATIO_THRESHOLD: " + confInfo.getUsedStorageRatioThreshold());
            pw.println("MAX_ACCESS_DELAY_THRESHOLD: " + confInfo.getMaxAccessDelayThreshold());
            pw.println("----------------------------------------------");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("Current file-sync mode: " + syncInfo.getCurrentMode());
            System.out.println("DIR_ACTIVATION_MONITORING_PERIOD: " + confInfo.getDirActivationMonitoringPeriod());
            System.out.println("DIR_ACTIVATION_MONITORING_PERIOD_UNIT: "
                    + confInfo.getDirActivationMonitoringPeriodUnit());
            System.out.println("DURATION_SINCE_LAST_ACCESS_THRESHOLD: " + confInfo.getDurationSinceLastAccessThreshold());
            System.out.println("DURATION_SINCE_LAST_ACCESS_THRESHOLD: "
                    + confInfo.getDurationSinceLastAccessThresholdUnit());
            System.out.println("ONLINE_MODE_THRESHOLD: " + confInfo.getOnlineModeThreshold());
            System.out.println("LOCAL_MODE_THRESHOLD: " + confInfo.getLocalModeThreshold());
            System.out.println("FILE_SYNC_STORAGE: " + confInfo.getFileSyncStorage());
            System.out.println("USED_STORAGE_RATIO_THRESHOLD: " + confInfo.getUsedStorageRatioThreshold());
            System.out.println("MAX_ACCESS_DELAY_THRESHOLD: " + confInfo.getMaxAccessDelayThreshold());
        }

    }

    // called at the client
    private Path[] checkAndCreateTestFileNameArray(Path testFileDir, final int NUM_TEST_FILES) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.checkAndCreateTestFileNameArray() called..");
            System.out.println("testFileDir = " + testFileDir);
            System.out.println("NUM_TEST_FILES = " + NUM_TEST_FILES);
        }
        final Path[] testFileNameArray;
        try {
            testFileNameArray = Files.walk(testFileDir)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .toArray(Path[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (testFileNameArray.length != NUM_TEST_FILES) {
            System.err.println("Number of test files(" + testFileNameArray.length +
                    ") is different from NUM_TEST_FILES(" + NUM_TEST_FILES + ")!");
            return null;
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println(Arrays.toString(testFileNameArray));
        }

        return testFileNameArray;
    }

    // called at the client
    private boolean testAddFile(final long sleepTime, Path srcPath, Path targetPath, Path resultPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.testAddFile() called..");
            System.out.println("srcPath = " + srcPath);
            System.out.println("targetPath = " + targetPath);
            System.out.println("resultPath = " + resultPath);
        }

        // wait for file-addition period
        if (CMInfo._CM_DEBUG) {
            System.out.println("** waiting for file-addition period: " + sleepTime/1000 + " seconds.");
        }
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // write the file-add event to the result file
        Path syncHome = getClientSyncHome();
        try (BufferedWriter bw = Files.newBufferedWriter(resultPath, StandardOpenOption.APPEND);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.println(targetPath.getFileName() + ", add, local, " + getDirectorySize(syncHome) + " Bytes");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("**" + targetPath.getFileName() + ", add, local, "
                    + getDirectorySize(syncHome) + " Bytes");
        }

        return true;
    }

    // called at the client
    private boolean testAccessFile(final long sleepTime, Path srcPath, Path targetPath, Path resultPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.testAccessFile() called..");
            System.out.println("srcPath = " + srcPath);
            System.out.println("targetPath = " + targetPath);
            System.out.println("resultPath = " + resultPath);
        }
        // wait for file-access period
        if (CMInfo._CM_DEBUG) {
            System.out.println("** waiting for file-access period: " + sleepTime/1000 + " seconds.");
        }
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // access a file (file copy or request for local-mode)
        if (!Files.exists(targetPath)) {
            System.err.println("A file to be accessed does not exist!");
            System.err.println(targetPath);
            return false;
        }
        boolean isOnlineMode = isOnlineMode(targetPath);
        if (isOnlineMode) {
            List<Path> list = new ArrayList<>();
            list.add(targetPath);
            boolean ret = requestLocalMode(list);
            if (!ret) {
                System.err.println("Error to request local mode!");
                return false;
            }
        } else {
            try {
                Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // write the file-access event to the result file
        Path syncHome = getClientSyncHome();
        try (BufferedWriter bw = Files.newBufferedWriter(resultPath, StandardOpenOption.APPEND);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.print(targetPath.getFileName() + ", access, ");
            if (isOnlineMode) pw.print("online, ");
            else pw.print("local, ");
            pw.println(getDirectorySize(syncHome) + " Bytes");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("**" + targetPath.getFileName() + ", access, ");
            if (isOnlineMode) System.out.print("online, ");
            else System.out.print("local, ");
            System.out.println(getDirectorySize(syncHome) + " Bytes");
        }

        return true;
    }

    // called at the client
    private boolean testAccessNoFile(final long sleepTime, Path resultPath) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncManager.testAccessNoFile() called..");
            System.out.println("resultPath = " + resultPath);
        }

        // wait until the next recording to the result file
        if (CMInfo._CM_DEBUG) {
            System.out.println("** waiting for next no-access record: "+sleepTime/1000+" seconds.");
        }
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // write current access state to the result file
        Path syncHome = getClientSyncHome();
        try (BufferedWriter bw = Files.newBufferedWriter(resultPath, StandardOpenOption.APPEND);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.println("no file access, " + getDirectorySize(syncHome) + " Bytes");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (CMInfo._CM_DEBUG) {
            System.out.println("** no file access, " + getDirectorySize(syncHome) + " Bytes");
        }

        return true;
    }

    // called at the client
    public boolean simulateDeactivatingFileAccess(String fileName) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.simulateDeactivatingFileAccess() called..");
            System.out.println("fileName = " + fileName);
        }

        // declare constants for the file-access simulation
        final String TEST_FILE_DIR_NAME = "test-file-access";
        final int NUM_TEST_FILES = 10;
        final int FILE_ADD_PERIOD = 5;
        final int FILE_ACCESS_PERIOD = 2;
        final int ACTIVATION_PERIOD = 3 * 24;
        final int DEACTIVATION_PERIOD = 3 * 24;

        // check the existence of the test-file directory
        final CMConfigurationInfo confInfo = Objects.requireNonNull(CMConfigurationInfo.getInstance());
        final Path testFileDir = confInfo.getTransferedFileHome().resolve(TEST_FILE_DIR_NAME)
                .toAbsolutePath().normalize();
        if (!Files.exists(testFileDir)) {
            System.err.println("Test-file directory (" + testFileDir + ") not exists!");
            return false;
        }

        // check the test files in the test-file directory
        final Path[] testFileNameArray = checkAndCreateTestFileNameArray(testFileDir, NUM_TEST_FILES);
        if (testFileNameArray == null) {
            System.err.println("testFileNameArray is null!");
            return false;
        }

        // check if the sync-home directory is empty
        final CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        final Path syncHome = getClientSyncHome();
        if (!syncInfo.getPathList().isEmpty()) {
            System.err.println("The sync home must be empty to start the file-access test!");
            return false;
        }
        // print out the current working directory where the result file will be created.
        if (CMInfo._CM_DEBUG) {
            System.out.println("current working directory: " + System.getProperty("user.dir"));
        }
        // create the result file
        final Path resultPath;
        try {
            resultPath = Files.createFile(Path.of(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // write common info to the result file
        writeCommonTestInfoToFile(resultPath);

        ///// file-access activation test
        int totalElapsedSeconds = 0;
        int fileIndex = 0;
        boolean ret = false;
        while (totalElapsedSeconds < ACTIVATION_PERIOD) {
            // add a new file (copy a file from the test dir to the sync home)
            Path srcPath = testFileDir.resolve(testFileNameArray[fileIndex]);
            Path targetPath = syncHome.resolve(testFileNameArray[fileIndex]);
            ret = testAddFile(FILE_ADD_PERIOD*1000, srcPath, targetPath, resultPath);
            if (!ret) {
                System.err.println("testAddFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ADD_PERIOD;

            // access a file (file copy or request for local-mode)
            ret = testAccessFile(FILE_ACCESS_PERIOD*1000, srcPath, targetPath, resultPath);
            if (!ret) {
                System.err.println("testAccessFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ACCESS_PERIOD;

            // update file index to be tested
            fileIndex = (fileIndex + 1) % testFileNameArray.length;
        }
        /////
        ///// file-access deactivation test
        totalElapsedSeconds = 0;
        while (totalElapsedSeconds < DEACTIVATION_PERIOD) {

            ret = testAccessNoFile((FILE_ADD_PERIOD+FILE_ACCESS_PERIOD)*1000, resultPath);
            if(!ret) {
                System.err.println("testAccessNoFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ADD_PERIOD + FILE_ACCESS_PERIOD;
        }
        /////
        // print out the result file
        if (CMInfo._CM_DEBUG) {
            System.out.println("---------------- end of deactivating file-access test");
            try {
                Files.readAllLines(resultPath).forEach(System.out::println);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("----------------");
        }

        return true;
    }

    // called at the client
    public boolean simulateActivatingFileAccess(String fileName) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.simulateActivatingFileAccess() called..");
            System.out.println("fileName = " + fileName);
        }

        // declare constants for the file-access simulation
        final String TEST_FILE_DIR_NAME = "test-file-access";
        final int NUM_TEST_FILES = 10;
        final int FILE_ACCESS_PERIOD = 7;
        final int ACTIVATION_PERIOD = 3 * 24;
        final int DEACTIVATION_PERIOD = 3 * 24;

        // check the existence of the test-file directory
        final CMConfigurationInfo confInfo = Objects.requireNonNull(CMConfigurationInfo.getInstance());
        final Path testFileDir = confInfo.getTransferedFileHome().resolve(TEST_FILE_DIR_NAME)
                .toAbsolutePath().normalize();
        if (!Files.exists(testFileDir)) {
            System.err.println("Test-file directory (" + testFileDir + ") not exists!");
            return false;
        }

        // check the test files in the test-file directory
        final Path[] testFileNameArray = checkAndCreateTestFileNameArray(testFileDir, NUM_TEST_FILES);
        if (testFileNameArray == null) {
            System.err.println("testFileNameArray is null!");
            return false;
        }

        // check if the sync-home directory is empty
        final CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        final Path syncHome = getClientSyncHome();
        if (!syncInfo.getPathList().isEmpty()) {
            System.err.println("The sync home must be empty to start the file-access test!");
            return false;
        }

        // copy test directory files to the sync home
        for(Path name : testFileNameArray) {
            Path srcPath = testFileDir.resolve(name);
            Path targetPath = syncHome.resolve(name);
            try {
                Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // wait until the file-sync completes
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // request online-mode for all files in the sync home
        try {
            List<Path> pathList = Files.walk(syncHome)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            boolean ret = requestOnlineMode(pathList);
            if(!ret) {
                System.err.println("error to request online mode!");
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // print out the current working directory where the result file will be created.
        if (CMInfo._CM_DEBUG) {
            System.out.println("current working directory: " + System.getProperty("user.dir"));
        }
        // create the result file
        final Path resultPath;
        try {
            resultPath = Files.createFile(Path.of(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // write common info to the result file
        writeCommonTestInfoToFile(resultPath);

        ///// file-access deactivation test
        int totalElapsedSeconds = 0;
        boolean ret = false;
        while(totalElapsedSeconds < DEACTIVATION_PERIOD) {
            // after sleep time, record current state to the result file
            ret = testAccessNoFile(FILE_ACCESS_PERIOD*1000, resultPath);
            if(!ret) {
                System.err.println("testAccessNoFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ACCESS_PERIOD;
        }
        /////
        ///// file-access activation test
        totalElapsedSeconds = 0;
        int fileIndex = 0;
        while(totalElapsedSeconds < ACTIVATION_PERIOD) {
            // after sleep time, access a file
            Path srcPath = testFileDir.resolve(testFileNameArray[fileIndex]);
            Path targetPath = syncHome.resolve(testFileNameArray[fileIndex]);
            ret = testAccessFile(FILE_ACCESS_PERIOD*1000, srcPath, targetPath, resultPath);
            if(!ret) {
                System.err.println("testAccessFile() error!");
                return false;
            }
            // update total elapsed seconds
            totalElapsedSeconds += FILE_ACCESS_PERIOD;
            // update file index to be accessed
            fileIndex = (fileIndex + 1) % testFileNameArray.length;
        }
        /////
        // print out the result file
        if (CMInfo._CM_DEBUG) {
            System.out.println("---------------- end of activating file-access test");
            try {
                Files.readAllLines(resultPath).forEach(System.out::println);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("----------------");
        }

        return true;
    }

    public void clearSyncHome() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.clearSyncHome() called..");
        }

        Path syncHome = getClientSyncHome();
        try {
            Files.walk(syncHome)
                    .filter(p -> !(p.equals(syncHome)))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // called at the client
    public List<Path> getOnlineModeFiles() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getOnlineModeFiles() called..");
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        if(syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.err.println("Current file sync mode is OFF!");
            System.err.println("You should start file sync to get the updated online mode file list.");
            return null;
        }

        List<Path> onlineModeFiles = syncInfo.getOnlineModePathSizeMap().keySet().stream().toList();

        if(CMInfo._CM_DEBUG) {
            System.out.println("-- online mode file list");
            for(Path path : onlineModeFiles)
                System.out.println(path);
        }
        return onlineModeFiles;
    }

    // called at the client
    public List<Path> getLocalModeFiles() {
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncManager.getLocalModeFiles() called..");
        }

        CMFileSyncInfo syncInfo = Objects.requireNonNull(CMFileSyncInfo.getInstance());
        if(syncInfo.getCurrentMode() == CMFileSyncMode.OFF) {
            System.err.println("Current file sync mode is OFF!");
            System.err.println("You should start file sync to get the updated local mode file list.");
            return null;
        }

        List<Path> onlineModeFiles = getOnlineModeFiles();
        List<Path> pathList = syncInfo.getPathList();

        List<Path> localModeFiles = pathList.stream()
                .filter(p -> !Files.isDirectory(p))
                .filter(p -> !onlineModeFiles.contains(p)).toList();

        if(CMInfo._CM_DEBUG) {
            System.out.println("-- local mode file list");
            for(Path path : localModeFiles)
                System.out.println(path);
        }

        return localModeFiles;
    }
}
