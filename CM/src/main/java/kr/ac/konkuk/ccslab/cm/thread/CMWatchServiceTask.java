package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncClientEntry;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CMWatchServiceTask implements Runnable {

    // DELETE grace period (단위: 사이클). 사이클 길이는 watchService.poll 의 500ms 와 같으므로
    // 1 = 약 500ms 의 유예. cycle-split atomic save (DELETE → CREATE 가 두 사이클로 쪼개짐) 의
    // 전반부 DELETE 를 흡수해 transient DELETE+CREATE 부풀음을 방지한다. 0 으로 두면 즉시
    // 승격되지만 reconcile-then-enqueue 순서상 최소 1 사이클 지연은 항상 발생한다.
    private static final long DELETE_GRACE_CYCLES = 1;

    private final Path syncPath;
    private final CMFileSyncManager syncManager;
    private final CMFileSyncInfo syncInfo;
    private final WatchService watchService;
    private final Map<WatchKey, Path> directoryMap;
    private final Map<WatchEvent.Kind<?>, List<Path>> detectedPathMap;

    // grace 큐: relPath → PendingDelete. detectedPathMap 와 달리 사이클 간 보존된다.
    // buildPushCandidateMap 단계 1 에서 reconcile (파일 부활 시 drop, grace 경과 + 파일 부재 시
    // 확정 DELETE 로 승격) 하고, 단계 2 에서 DELETE 후보가 발견될 때 enqueue 한다.
    // task 종료 시 잔여 항목은 폐기 — 다음 sync 시 scanLocalPushDeletes 가 lastSyncedMtimeMap 과
    // 디스크를 비교해 동일한 DELETE 를 재감지하므로 데이터 누락은 없다.
    private final Map<String, PendingDelete> pendingDeleteQueue;

    // 보류 push 버퍼: relPath → 이미 분류 완료된 CMFileSyncClientEntry. startPushSync 가 세션
    // 진행 중이라 거절(false)했을 때, buildPushCandidateMap 이 만든 후보를 잃지 않도록 여기 보존한다.
    // detectedPathMap 은 매 사이클 비워지고 pendingDeleteQueue 의 promotion 된 항목도 큐에서 빠지므로,
    // 이 버퍼가 없으면 거절된 후보(CREATE/MODIFY/promoted DELETE)는 영구 유실된다(워치 트리거 push
    // 에는 디스크 full-rescan 폴백이 없음). buildPushCandidateMap 단계 3 에서 현재 디스크 기준으로
    // 재검증해 result 에 다시 합류시킨다. 버퍼 lifecycle (성공 시 clear / 거절 시 result 로 갱신) 은
    // run() 이 소유한다. 이미 promotion 됐던 DELETE 는 grace 재연장 없이 즉시 재시도된다.
    private final Map<String, CMFileSyncClientEntry> deferredPushMap;
    private long cycleCounter;

    private static final class PendingDelete {
        final long firstDetectedCycle;
        final long baseMtime;

        PendingDelete(long firstDetectedCycle, long baseMtime) {
            this.firstDetectedCycle = firstDetectedCycle;
            this.baseMtime = baseMtime;
        }

        @Override
        public String toString() {
            return "PendingDelete{cycle=" + firstDetectedCycle + ", baseMtime=" + baseMtime + "}";
        }
    }

    public CMWatchServiceTask(Path syncPath) {
        this.syncPath = syncPath;
        this.syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        this.syncInfo = CMFileSyncInfo.getInstance();
        this.watchService = syncInfo.getWatchService();

        directoryMap = new HashMap<>();
        detectedPathMap = new HashMap<>();
        pendingDeleteQueue = new LinkedHashMap<>();
        deferredPushMap = new LinkedHashMap<>();
        cycleCounter = 0L;
    }

    @Override
    public void run() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMWatchServiceTask.run() called..");
            System.out.println("syncPath = " + syncPath);
        }

        // register sync path to the watchService
        try {
            registerTree(syncPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            final WatchKey key;
            try {
                if (detectedPathMap.isEmpty() && pendingDeleteQueue.isEmpty()
                        && deferredPushMap.isEmpty()) {
                    // 새 이벤트도 없고 grace 큐/보류 버퍼도 비었으면 무기한 대기
                    key = watchService.take();
                } else {
                    // 새 이벤트 누적 중이거나 grace 큐 promotion 대기 / 보류 버퍼 재시도 대기 중이면
                    // 500ms poll 유지 (세션이 풀리면 timeout 사이클에서 자동 재시도된다)
                    key = watchService.poll(500, TimeUnit.MILLISECONDS);
                    if (key == null) {
                        cycleCounter++;
                        if (CMInfo._CM_DEBUG) {
                            detectedPathMap.forEach((eventKey, listValue) -> {
                                System.out.println("key = " + eventKey);
                                listValue.forEach(System.out::println);
                            });
                        }
                        // pull sync 가 방금 디스크에 떨어뜨린 파일들이 ENTRY_CREATE/MODIFY 로 잡힌
                        // 경우는 push 대상이 아니다. 현재 mtime+size 가 lastSynced* 와 일치하면
                        // self-event 로 간주해서 제거한다.
                        filterSelfEvents();
                        // detectedPathMap 이 비어 있어도 grace 큐에 promotion 대상이 있을 수 있으므로
                        // 항상 buildPushCandidateMap 을 호출한다.
                        Map<String, CMFileSyncClientEntry> candidateMap = buildPushCandidateMap();
                        // detectedPathMap 은 항상 비운다 (이번 사이클에서 이미 분류 완료). 큐는 사이클 간
                        // 보존되며 자체적으로 reconcile/enqueue 로 lifecycle 관리.
                        detectedPathMap.clear();

                        if (candidateMap.isEmpty()) {
                            if (CMInfo._CM_DEBUG) {
                                System.out.println("CMWatchServiceTask: candidateMap empty; "
                                        + "pendingDeleteQueue size = " + pendingDeleteQueue.size());
                            }
                            // 단계 3 에서 deferred 의 옛 내용은 result(여기선 비어 있음)/grace 큐로
                            // 모두 재표현됐다. 보낼 게 없으므로 버퍼를 비운다 — 잔여 actionable
                            // 항목은 pendingDeleteQueue 가 보유한다.
                            deferredPushMap.clear();
                            // 큐에 grace 대기 항목이 남아 있으면 외부 관찰자에게는 "변경 대기" 로 표시.
                            // 다음 사이클에서 promotion 시도가 이어진다.
                            syncInfo.setFileChangeDetected(!pendingDeleteQueue.isEmpty());
                            continue;
                        }

                        boolean ret = syncManager.startPushSync(candidateMap);
                        // 성공: candidateMap 이 세션 pendingPushMap 으로 넘어갔으니 버퍼 비움.
                        // 거절(세션 진행 중): candidateMap 전체를 버퍼에 보존해 다음 사이클에 재시도
                        // (detectedPathMap 은 이미 비워졌으므로 이 버퍼가 유일한 유실 방지 경로).
                        deferredPushMap.clear();
                        if (!ret) {
                            deferredPushMap.putAll(candidateMap);
                        }
                        // retry 필요 조건: 시작 실패(거절 시 deferred 에 보존됨) 또는 grace 대기 잔여.
                        // 거절이면 !ret 가 true 라 다음 사이클 poll 이 유지되고(83줄 대기 조건이
                        // deferredPushMap 도 확인), 세션이 풀리면 timeout 사이클에서 재시도된다.
                        syncInfo.setFileChangeDetected(!ret || !pendingDeleteQueue.isEmpty());
                        continue; // restart monitoring
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            } catch (ClosedWatchServiceException e) {
                if (CMInfo._CM_DEBUG) {
                    System.out.println("CMWatchServiceTask is closed by another thread.");
                }
                break;
            }

            for (WatchEvent<?> watchEvent : key.pollEvents()) {
                // get event type
                final WatchEvent.Kind<?> kind = watchEvent.kind();
                // get file name
                final WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
                final Path filename = watchEventPath.context();
                // process OVERFLOW event — watcher 가 이 key 의 디렉토리에서 알 수 없는 만큼의
                // 이벤트를 유실했다. 놓친 CREATE/MODIFY 를 복구하기 위해 해당 서브트리를
                // 재등록(register 는 멱등 — 그 사이 새로 생긴 하위 디렉토리도 흡수)하고 기존
                // 파일을 재적재한다. 이미 동기화된 파일은 filterSelfEvents 가 mtime+size 일치로
                // 걸러내므로, 실험에서 흔한 대량 복사발 overflow 의 누락만 정확히 복구된다.
                // (유실된 DELETE 복구는 lastSynced 인덱스 기반 full reconcile 백스톱이 필요 —
                //  별도 작업. 여기서는 다루지 않는다.)
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    Path overflowed = directoryMap.get(key);
                    if (overflowed != null) {
                        try {
                            registerTree(overflowed);
                            enqueueExistingFilesUnder(overflowed);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                // process CREATE event of a new sub-directory
                final Path directory = directoryMap.get(key);
                final Path child = directory.resolve(filename);
                // ignore 패턴 매칭 시 이벤트 수집 자체를 건너뜀 (.DS_Store 등 OS 자동 생성 파일)
                Path syncHomeNorm = syncManager.getClientSyncHome().toAbsolutePath().normalize();
                Path childNorm = child.toAbsolutePath().normalize();
                if (childNorm.startsWith(syncHomeNorm)
                        && syncInfo.isIgnored(syncHomeNorm.relativize(childNorm))) {
                    continue;
                }
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(child)) {
                        try {
                            registerTree(child);
                            // 등록 이전에 이미 디렉토리 안에 들어와 있던 파일들 — 폴더 통째
                            // move/압축해제/git checkout 처럼 디렉토리와 내용이 원자적으로
                            // 등장하면 그 파일들의 ENTRY_CREATE 는 영영 안 올 수 있으므로,
                            // 등록 직후 직접 push 후보로 적재한다 (등록 레이스 보완).
                            enqueueExistingFilesUnder(child);
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                }

                // get the path list of the detectedPathMap
                List<Path> detectedPathList = detectedPathMap.get(kind);
                if (detectedPathList == null) {
                    detectedPathList = new ArrayList<>();
                    detectedPathMap.put(kind, detectedPathList);
                }
                // add the detected path to the list
                detectedPathList.add(child);

                // print the result
                if(CMInfo._CM_DEBUG) {
                    System.out.println(kind + "->" + child);
                }
            }

            // initialize the key
            boolean valid = key.reset();
            // if the key is not valid, remove the key
            if (!valid) {
                directoryMap.remove(key);
                if (directoryMap.isEmpty())
                    break;
            }
        }
        // close the watch service
        try {
            watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // initialize watch service
        syncInfo.setWatchService(null);

        if (CMInfo._CM_DEBUG) {
            System.out.println("CMWatchServiceTask.run() ended..");
        }
    }

    // pull-delete 후 ENTRY_DELETE 이벤트가 안 들어오는 경우의 leak 방지용 TTL (ms).
    private static final long PENDING_PULL_DELETE_TTL_MS = 30_000L;

    // detectedPathMap 의 self-event 항목을 제거한다 — pull sync 가 막 디스크에 적용한 변경이
    // 다시 push 흐름으로 돌아오는 루프를 방지한다.
    //   CREATE/MODIFY: 현재 mtime+size 가 lastSynced* 와 일치하면 self-event 로 간주.
    //   DELETE:        pendingPullDeletePaths 에 등록된 path 면 self-event 로 간주.
    private void filterSelfEvents() {
        // 등록은 됐는데 이벤트가 끝내 안 온 항목 정리 (leak 방지)
        syncInfo.sweepStalePendingPullDeletes(PENDING_PULL_DELETE_TTL_MS);

        Path syncHome = syncManager.getClientSyncHome().toAbsolutePath().normalize();

        for (Map.Entry<WatchEvent.Kind<?>, List<Path>> entry : detectedPathMap.entrySet()) {
            WatchEvent.Kind<?> kind = entry.getKey();
            List<Path> paths = entry.getValue();

            if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                Iterator<Path> iter = paths.iterator();
                while (iter.hasNext()) {
                    Path absPath = iter.next().toAbsolutePath().normalize();
                    if (!absPath.startsWith(syncHome)) continue;
                    String relPath = syncHome.relativize(absPath).toString().replace('\\', '/');
                    if (syncInfo.consumePendingPullDelete(relPath)) {
                        if (CMInfo._CM_DEBUG) {
                            System.out.println("CMWatchServiceTask: filtered self-DELETE for " + relPath);
                        }
                        iter.remove();
                    }
                }
                continue;
            }

            if (kind != StandardWatchEventKinds.ENTRY_CREATE
                    && kind != StandardWatchEventKinds.ENTRY_MODIFY) {
                continue;   // OVERFLOW 등 그 외 종류는 필터 대상 아님
            }
            Iterator<Path> iter = paths.iterator();
            while (iter.hasNext()) {
                Path absPath = iter.next().toAbsolutePath().normalize();
                if (!absPath.startsWith(syncHome)) continue;
                if (!Files.isRegularFile(absPath)) continue;

                String relPath = syncHome.relativize(absPath).toString().replace('\\', '/');
                long lastMtime = syncInfo.getLastSyncedMtime(relPath);
                long lastSize = syncInfo.getLastSyncedSize(relPath);
                if (lastMtime < 0 || lastSize < 0) continue;    // 모른다 → 통과

                long curMtime;
                long curSize;
                try {
                    curMtime = syncInfo.currentMtimeSecOrMinusOne(absPath);
                    curSize = syncInfo.currentSizeOrMinusOne(absPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                if (curMtime == lastMtime && curSize == lastSize) {
                    if (CMInfo._CM_DEBUG) {
                        System.out.println("CMWatchServiceTask: filtered self-event ("
                                + kind + ") for " + relPath);
                    }
                    iter.remove();
                }
            }
        }
        // 비어있는 kind 항목 정리
        detectedPathMap.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    // detectedPathMap (WatchEvent.Kind → List<absPath>) 와 pendingDeleteQueue 를 push 세션
    // 1차 후보 맵 (relPath → CMFileSyncClientEntry) 으로 변환한다.
    // CMFileSyncManager.startPushSync() 의 입력으로 쓰인다.
    //
    // 단계 1: pendingDeleteQueue reconcile (이전 사이클까지 쌓인 grace 대기 DELETE 처리)
    //   - 디스크에 파일이 다시 존재 → split MODIFY 의 후반부 도착으로 간주, 큐에서 drop.
    //     이번 사이클의 CREATE/MODIFY 이벤트는 단계 2 에서 정상 MODIFY 로 분류됨
    //     (baseMtime 유지 — DELETE 가 아직 push 되지 않았으므로 client-index 그대로).
    //   - 파일 여전히 부재 + 사이클 age >= DELETE_GRACE_CYCLES → 확정 DELETE 로 result 적재 + 큐 drop.
    //   - 그 외 → 큐에 유지 (다음 사이클 reconcile 에서 다시 평가).
    //
    // 단계 2: detectedPathMap 분류 (CREATE → MODIFY → DELETE 처리 순서)
    //   - CREATE/MODIFY 이벤트인데 디스크가 디렉터리면 스킵 (push 대상은 파일만).
    //     DELETE 는 디렉터리 여부 판단 불가 — 그대로 통과시키고 아래에서 DELETE 로 처리.
    //   - DELETE 이벤트이거나 CREATE/MODIFY 인데 디스크에 파일이 없으면 → grace 큐에 enqueue.
    //     같은 사이클의 CREATE/MODIFY 가 먼저 result 에 들어가 있으면 net DELETE 가 우선이므로
    //     result 에서 제거. 단계 1 에서 promotion 된 path 와 충돌하면 promotion 결과 유지.
    //     이미 큐에 있는 path 는 firstDetectedCycle 보존 (grace 연장 방지).
    //   - 파일 존재 → baseMtime 으로 재분류: -1 (base snapshot 에 없는 경로) → CREATE,
    //     != -1 (이미 동기화됐던 경로) → MODIFY. scanLocalPushCandidates 와 동일 기준.
    //
    // IOException 은 path 단위로 잡아 해당 entry 만 스킵한다.
    private Map<String, CMFileSyncClientEntry> buildPushCandidateMap() {
        Map<String, CMFileSyncClientEntry> result = new LinkedHashMap<>();

        // ── 단계 1: pendingDeleteQueue reconcile ─────────────────────────
        Iterator<Map.Entry<String, PendingDelete>> qit = pendingDeleteQueue.entrySet().iterator();
        while (qit.hasNext()) {
            Map.Entry<String, PendingDelete> qe = qit.next();
            String relPath = qe.getKey();
            PendingDelete pd = qe.getValue();
            Path absPath = syncPath.resolve(relPath);

            if (Files.exists(absPath)) {
                // 파일 부활 → split DELETE/CREATE 의 후반부 도착으로 처리. 큐에서 drop.
                if (CMInfo._CM_DEBUG)
                    System.out.println("pendingDelete revived (file present): " + relPath
                            + " " + pd);
                qit.remove();
                continue;
            }

            long age = cycleCounter - pd.firstDetectedCycle;
            if (age >= DELETE_GRACE_CYCLES) {
                CMFileSyncClientEntry entry = new CMFileSyncClientEntry()
                        .setPath(relPath)
                        .setCurMtime(-1L)
                        .setBaseMtime(pd.baseMtime)
                        .setSize(0L)
                        .setOpHint(CMFileSyncOp.DELETE);
                result.put(relPath, entry);
                qit.remove();
                if (CMInfo._CM_DEBUG)
                    System.out.println("pendingDelete promoted (age=" + age + "): " + relPath);
            }
            // else: grace 미경과 — 큐에 유지하고 다음 사이클에서 재평가
        }

        // ── 단계 2: detectedPathMap 분류 ─────────────────────────────────
        WatchEvent.Kind<?>[] orderedKinds = {
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        };

        for (WatchEvent.Kind<?> kind : orderedKinds) {
            List<Path> pathList = detectedPathMap.get(kind);
            if (pathList == null) continue;

            for (Path absPath : pathList) {
                // CREATE/MODIFY 인데 디스크가 디렉터리면 스킵. DELETE 는 디렉터리 여부 판단 불가
                // (이미 사라졌을 수 있음) — 그대로 통과시키고 아래에서 DELETE 로 처리한다.
                if (kind != StandardWatchEventKinds.ENTRY_DELETE && Files.isDirectory(absPath)) {
                    continue;
                }

                String relPathStr = syncPath.relativize(absPath).toString().replace('\\', '/');

                boolean fileExists = Files.exists(absPath);
                long curMtime;
                long size;
                try {
                    curMtime = fileExists ? Files.getLastModifiedTime(absPath).toMillis() / 1000 : -1L;
                    size = fileExists ? Files.size(absPath) : 0L;
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                long baseMtime = syncInfo.getLastSyncedMtime(relPathStr);

                if (kind == StandardWatchEventKinds.ENTRY_DELETE || !fileExists) {
                    // DELETE 후보 → 즉시 result 에 포함 X, grace 큐에 enqueue.
                    // 단계 1 에서 promotion 된 항목과 충돌 시 (큐에서 끌어올린 후 DELETE 이벤트가
                    // 또 도착) promotion 결과 유지.
                    CMFileSyncClientEntry existing = result.get(relPathStr);
                    if (existing != null && existing.getOpHint() == CMFileSyncOp.DELETE) {
                        continue;
                    }
                    // 같은 사이클의 CREATE/MODIFY 가 먼저 result 에 들어간 경우 net DELETE 가 우선
                    result.remove(relPathStr);
                    // 중복 enqueue 방지 — 이미 큐에 있으면 firstDetectedCycle 보존 (grace 연장 방지)
                    if (!pendingDeleteQueue.containsKey(relPathStr)) {
                        pendingDeleteQueue.put(relPathStr,
                                new PendingDelete(cycleCounter, baseMtime));
                        if (CMInfo._CM_DEBUG)
                            System.out.println("DELETE queued (grace, cycle=" + cycleCounter
                                    + "): " + relPathStr);
                    }
                    continue;
                }

                // 파일 존재 → CREATE / MODIFY
                CMFileSyncOp opHint = (baseMtime == -1L) ? CMFileSyncOp.CREATE : CMFileSyncOp.MODIFY;

                CMFileSyncClientEntry entry = new CMFileSyncClientEntry()
                        .setPath(relPathStr)
                        .setCurMtime(curMtime)
                        .setBaseMtime(baseMtime)
                        .setSize(size)
                        .setOpHint(opHint);

                result.put(relPathStr, entry);
            }
        }

        // ── 단계 3: deferredPushMap 재투입 (이전 사이클 startPushSync 거절분) ──────
        // 세션 진행 중 거절로 보류됐던 후보를 현재 디스크 기준으로 재검증해 result 에 합류시킨다.
        // 이번 사이클 신선한 이벤트로 이미 처리된 path (result 또는 grace 큐) 는 신선한 쪽이 우선.
        // 버퍼 자체는 여기서 비우지 않는다 — lifecycle 은 run() 이 소유한다.
        for (CMFileSyncClientEntry deferred : deferredPushMap.values()) {
            String relPathStr = deferred.getPath();
            if (result.containsKey(relPathStr) || pendingDeleteQueue.containsKey(relPathStr)) {
                continue;   // 신선한 분류 우선
            }
            Path absPath = syncPath.resolve(relPathStr);

            if (!Files.exists(absPath)) {
                if (deferred.getOpHint() == CMFileSyncOp.DELETE) {
                    // 이미 promotion 됐던 DELETE — grace 재연장 없이 즉시 재시도.
                    result.put(relPathStr, deferred);
                } else {
                    // 보류된 CREATE/MODIFY 의 파일이 그 사이 삭제됨 (드문 엣지: push 전 삭제).
                    // 새 DELETE 로 grace 큐에 enqueue. 중복 enqueue 방지.
                    if (!pendingDeleteQueue.containsKey(relPathStr)) {
                        pendingDeleteQueue.put(relPathStr, new PendingDelete(cycleCounter,
                                syncInfo.getLastSyncedMtime(relPathStr)));
                        if (CMInfo._CM_DEBUG)
                            System.out.println("deferred CREATE/MODIFY vanished -> DELETE queued: "
                                    + relPathStr);
                    }
                }
                continue;
            }

            // 파일 존재 → 현재 디스크 기준 CREATE/MODIFY 재분류 (stale mtime/size 방지).
            if (Files.isDirectory(absPath)) continue;   // 디렉터리화됐으면 스킵
            long curMtime;
            long size;
            try {
                curMtime = Files.getLastModifiedTime(absPath).toMillis() / 1000;
                size = Files.size(absPath);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            // self-event 가드: 현재 mtime+size 가 base snapshot 과 동일하면 push 불필요
            // (보류 중 다른 세션(PULL 등)이 동일 내용으로 갱신했거나 변경이 무효화된 경우).
            // detectedPathMap 경로의 filterSelfEvents 와 동일한 의미를 deferred 에도 적용한다.
            long lastMtime = syncInfo.getLastSyncedMtime(relPathStr);
            long lastSize = syncInfo.getLastSyncedSize(relPathStr);
            if (lastMtime >= 0 && lastSize >= 0 && curMtime == lastMtime && size == lastSize) {
                if (CMInfo._CM_DEBUG)
                    System.out.println("deferred self-event dropped: " + relPathStr);
                continue;
            }
            CMFileSyncOp opHint = (lastMtime == -1L) ? CMFileSyncOp.CREATE : CMFileSyncOp.MODIFY;
            result.put(relPathStr, new CMFileSyncClientEntry()
                    .setPath(relPathStr)
                    .setCurMtime(curMtime)
                    .setBaseMtime(lastMtime)
                    .setSize(size)
                    .setOpHint(opHint));
        }

        if (CMInfo._CM_DEBUG) {
            System.out.println("CMWatchServiceTask.buildPushCandidateMap() result: size = "
                    + result.size() + ", pendingDeleteQueue size = " + pendingDeleteQueue.size()
                    + " (cycle " + cycleCounter + ")");
            result.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
        }

        return result;
    }

    // 런타임에 새로 생성된 디렉토리(ENTRY_CREATE)가 등록 시점에 이미 품고 있던 파일들을
    // detectedPathMap[ENTRY_CREATE] 에 직접 적재한다. registerTree 는 하위 디렉토리에 워치를
    // 걸 뿐 기존 파일을 enqueue 하지 않으므로, 디렉토리와 내용이 사실상 원자적으로 등장하면
    // (폴더 통째 move / 압축 해제 / git checkout) 내부 파일들의 CREATE 이벤트가 등록 이전에
    // 지나가 영영 도착하지 않는 레이스가 생긴다 — 이를 보완한다. 디렉토리 자신은
    // buildPushCandidateMap 에서 스킵되므로 일반 파일만 넣는다.
    //
    // 시작 시 전체 트리 등록(run() 의 registerTree(syncPath)) 에는 호출하지 않는다 — 동기화 홈
    // 전체가 CREATE 로 잡혀 통째 push 되는 것을 막기 위함. 이미 동기화됐던 파일이 섞여 들어와도
    // filterSelfEvents 가 mtime+size 일치로 걸러내므로 중복 push 는 발생하지 않는다.
    // ignore 판정은 메인 루프와 동일하게 syncPath 기준 상대경로로 수행한다(syncPath ==
    // clientSyncHome). 복사 진행 중 일시적으로 못 읽는 파일은 visitFileFailed 에서 스킵한다.
    private void enqueueExistingFilesUnder(Path dir) throws IOException {
        List<Path> createList = detectedPathMap.computeIfAbsent(
                StandardWatchEventKinds.ENTRY_CREATE, k -> new ArrayList<>());
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile())
                    return FileVisitResult.CONTINUE;
                if (file.startsWith(syncPath) && syncInfo.isIgnored(syncPath.relativize(file)))
                    return FileVisitResult.CONTINUE;
                createList.add(file);
                if (CMInfo._CM_DEBUG)
                    System.out.println("enqueueExistingFilesUnder enqueued: " + file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // 복사 진행 중 일시적으로 못 읽는 파일은 스킵 — 이후 이벤트/재스캔으로 다시 잡힌다.
                if (CMInfo._CM_DEBUG)
                    System.out.println("enqueueExistingFilesUnder skipped (visitFileFailed): " + file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerPath(Path path) throws IOException {

        WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

        // store the pair of (key, path)
        directoryMap.put(key, path);
    }

    private void registerTree(Path start) throws IOException {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMWatchServiceTask.registerTree() called..");
        }

        // register all sub-directories of start to the watch service
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (CMInfo._CM_DEBUG) {
                    System.out.println("Registering: " + dir);
                }
                registerPath(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
