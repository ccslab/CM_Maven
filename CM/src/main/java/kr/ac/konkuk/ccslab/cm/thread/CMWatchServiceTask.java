package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CMWatchServiceTask implements Runnable {

    private final Path syncPath;
    private final CMFileSyncManager syncManager;
    private final CMFileSyncInfo syncInfo;
    private final WatchService watchService;
    private final Map<WatchKey, Path> directoryMap;

    public CMWatchServiceTask(Path syncPath, WatchService watchService, CMFileSyncManager syncManager,
                              CMFileSyncInfo syncInfo) {
        this.syncPath = syncPath;
        this.watchService = watchService;
        this.syncManager = syncManager;
        this.syncInfo = syncInfo;

        directoryMap = new HashMap<>();
    }

    @Override
    public void run() {
        if(CMInfo._CM_DEBUG) {
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

        while(true) {
            final WatchKey key;
            try {
                key = watchService.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            } catch (ClosedWatchServiceException e) {
                e.printStackTrace();
                break;
            }

            if(key == null) {
                syncInfo.setFileChangeDetected(false);
                continue;
            }

            for(WatchEvent<?> watchEvent : key.pollEvents()) {
                // get event type
                final WatchEvent.Kind<?> kind = watchEvent.kind();
                // get file name
                final WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
                final Path filename = watchEventPath.context();
                // process OVERFLOW event
                if(kind == StandardWatchEventKinds.OVERFLOW)
                    continue;
                // process CREATE event of a new sub-directory
                if(kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    final Path directory = directoryMap.get(key);
                    final Path child = directory.resolve(filename);
                    if(Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        try {
                            registerTree(child);
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                }
                // print the result
                System.out.println(kind + "->" + filename);
            }

            // start the file-sync if possible
            if(syncManager.sync()) {
                syncInfo.setFileChangeDetected(false);
            }
            else {
                syncInfo.setFileChangeDetected(true);
            }

            // initialize the key
            boolean valid = key.reset();
            // if the key is not valid, remove the key
            if(!valid) {
                directoryMap.remove(key);
                if(directoryMap.isEmpty())
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

        if(CMInfo._CM_DEBUG) {
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
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMWatchServiceTask.registerTree() called..");
        }

        // register all sub-directories of start to the watch service
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if(CMInfo._CM_DEBUG) {
                    System.out.println("Registering: " + dir);
                }
                registerPath(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
