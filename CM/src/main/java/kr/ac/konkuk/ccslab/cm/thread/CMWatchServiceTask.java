package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
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
                                        + "skip startFullPushSync().");
                            }
                            syncInfo.setFileChangeDetected(false);
                            continue;
                        }
                        // start the file-sync task
                        // TODO: 양방향 push 동기화(startPushSync) 구현 후 교체 예정
                        boolean ret = syncManager.startFullPushSync();
                        // clear the detectedPathMap
                        detectedPathMap.clear();
                        if (!ret) syncInfo.setFileChangeDetected(true);
                        else syncInfo.setFileChangeDetected(false);
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

    // detectedPathMap 의 ENTRY_CREATE/ENTRY_MODIFY 이벤트 중, 현재 파일의 mtime+size 가
    // 마지막으로 동기화된 값(lastSyncedMtime + lastSyncedSize)과 일치하는 항목을 제거한다.
    // pull sync 가 막 디스크에 떨어뜨린 파일을 다시 push 하는 self-event 루프를 방지한다.
    // ENTRY_DELETE 는 별도 처리(파일이 없으니 mtime/size 비교 불가). DELETE 의 self-event 방지는
    // 후속 작업 — 현재는 통과시킨다.
    private void filterSelfEvents() {
        Path syncHome = syncManager.getClientSyncHome().toAbsolutePath().normalize();

        for (Map.Entry<WatchEvent.Kind<?>, List<Path>> entry : detectedPathMap.entrySet()) {
            WatchEvent.Kind<?> kind = entry.getKey();
            if (kind != StandardWatchEventKinds.ENTRY_CREATE
                    && kind != StandardWatchEventKinds.ENTRY_MODIFY) {
                continue;   // DELETE 등은 필터 대상 아님
            }
            List<Path> paths = entry.getValue();
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
