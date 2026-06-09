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

    private final Path syncPath;
    private final CMFileSyncManager syncManager;
    private final CMFileSyncInfo syncInfo;
    private final WatchService watchService;
    private final Map<WatchKey, Path> directoryMap;
    private final Map<WatchEvent.Kind<?>, List<Path>> detectedPathMap;

    public CMWatchServiceTask(Path syncPath) {
        this.syncPath = syncPath;
        this.syncManager = CMInfo.getInstance().getServiceManager(CMFileSyncManager.class);
        this.syncInfo = CMFileSyncInfo.getInstance();
        this.watchService = syncInfo.getWatchService();

        directoryMap = new HashMap<>();
        detectedPathMap = new HashMap<>();
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
                if (detectedPathMap.isEmpty()) {
                    // if there is no change-detected path in the previous monitoring
                    key = watchService.take();
                } else {
                    // if there is any change-detected path in the previous monitoring
                    key = watchService.poll(500, TimeUnit.MILLISECONDS);
                    if (key == null) {
                        // if there is no more change-detected path
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
                        if (detectedPathMap.isEmpty()) {
                            // 전부 self-event 였음 → push 트리거하지 않음
                            if (CMInfo._CM_DEBUG) {
                                System.out.println("CMWatchServiceTask: all events filtered as self-events; "
                                        + "skip startPushSync().");
                            }
                            syncInfo.setFileChangeDetected(false);
                            continue;
                        }
                        // detectedPathMap → push 후보 맵 (1차 분류: CREATE/MODIFY/DELETE).
                        Map<String, CMFileSyncClientEntry> candidateMap = buildPushCandidateMap();
                        // start an incremental push session with the candidate snapshot.
                        boolean ret = syncManager.startPushSync(candidateMap);
                        // clear the detectedPathMap regardless of success: on session-start failure
                        // fileChangeDetected=true triggers a retry, and old events stay out of the
                        // next cycle's classification.
                        detectedPathMap.clear();
                        syncInfo.setFileChangeDetected(!ret);
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

    // detectedPathMap (WatchEvent.Kind → List<absPath>) 를 push 세션 1차 후보 맵
    // (relPath → CMFileSyncClientEntry) 으로 변환한다. CMFileSyncManager.startPushSync()
    // 의 입력으로 쓰인다.
    //
    // 분류 정책:
    //   - 처리 순서를 CREATE → MODIFY → DELETE 로 고정해 같은 경로가 여러 kind 에 나타나면
    //     뒤에 처리되는 쪽이 덮어쓰도록 한다. 결과적으로 같은 사이클 내 CREATE+DELETE / MODIFY+DELETE
    //     중첩은 net DELETE 가 된다.
    //   - DELETE 이벤트가 아닌데 디스크가 디렉터리면 스킵 (push 대상은 파일만).
    //   - DELETE 이벤트이거나 CREATE/MODIFY 인데 디스크에 파일이 없으면 opHint = DELETE.
    //     curMtime/size 를 -1/0 으로 강제 보정 (디스크 truth 와 일치시킴).
    //   - 파일이 존재하면 baseMtime 으로 재분류한다 (WatchService 의 kind 보고와 무관):
    //     baseMtime == -1 (base snapshot 에 없는 경로) → CREATE,
    //     baseMtime != -1 (이미 동기화됐던 경로)        → MODIFY.
    //     scanLocalPushCandidates 의 분류 기준과 동일.
    //
    // IOException 은 path 단위로 잡아 해당 entry 만 스킵한다.
    private Map<String, CMFileSyncClientEntry> buildPushCandidateMap() {
        Map<String, CMFileSyncClientEntry> result = new LinkedHashMap<>();

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

                CMFileSyncOp opHint;
                if (kind == StandardWatchEventKinds.ENTRY_DELETE || !fileExists) {
                    opHint = CMFileSyncOp.DELETE;
                    curMtime = -1L;
                    size = 0L;
                } else {
                    opHint = (baseMtime == -1L) ? CMFileSyncOp.CREATE : CMFileSyncOp.MODIFY;
                }

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
                    + result.size());
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
