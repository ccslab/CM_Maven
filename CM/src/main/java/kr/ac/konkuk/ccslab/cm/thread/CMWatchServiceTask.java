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

    public CMWatchServiceTask(Path syncPath, WatchService watchService, CMFileSyncManager syncManager,
                              CMFileSyncInfo syncInfo) {
        this.syncPath = syncPath;
        this.watchService = watchService;
        this.syncManager = syncManager;
        this.syncInfo = syncInfo;

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
                        // start the file-sync task
                        boolean ret = syncManager.sync();
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
