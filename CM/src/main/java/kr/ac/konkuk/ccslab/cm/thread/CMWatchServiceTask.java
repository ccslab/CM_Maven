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
                if (detectedPathMap.isEmpty() && pendingDeleteQueue.isEmpty()) {
                    // 새 이벤트도 없고 grace 큐도 비었으면 무기한 대기
                    key = watchService.take();
                } else {
                    // 새 이벤트 누적 중이거나 grace 큐에 promotion 대기 중이면 500ms poll 유지
                    // (큐만 비어 있지 않은 경우에도 다음 사이클에서 reconcile/promotion 을 해야 함)
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
                            // 큐에 grace 대기 항목이 남아 있으면 외부 관찰자에게는 "변경 대기" 로 표시.
                            // 다음 사이클에서 promotion 시도가 이어진다.
                            syncInfo.setFileChangeDetected(!pendingDeleteQueue.isEmpty());
                            continue;
                        }

                        boolean ret = syncManager.startPushSync(candidateMap);
                        // 세션 시작 실패면 retry 필요 (true). 성공이라도 큐에 잔여 grace 대기가 있으면
                        // 또 사이클을 돌려야 하므로 true 유지.
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
                // process OVERFLOW event
                if (kind == StandardWatchEventKinds.OVERFLOW)
                    continue;
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

        if (CMInfo._CM_DEBUG) {
            System.out.println("CMWatchServiceTask.buildPushCandidateMap() result: size = "
                    + result.size() + ", pendingDeleteQueue size = " + pendingDeleteQueue.size()
                    + " (cycle " + cycleCounter + ")");
            result.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
        }

        return result;
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
