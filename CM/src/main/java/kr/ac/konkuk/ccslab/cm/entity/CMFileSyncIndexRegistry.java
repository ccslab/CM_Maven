package kr.ac.konkuk.ccslab.cm.entity;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import kr.ac.konkuk.ccslab.cm.util.CMUUIDConverter;

public class CMFileSyncIndexRegistry {
    private final ConcurrentHashMap<CMFileSyncStateKey, CMFileSyncIndexRepository> repos
            = new ConcurrentHashMap<>();
    private final CMFileSyncIndexSnapshotStore store;
    private final Path baseDir; // /srv/yourapp/index/

    public CMFileSyncIndexRegistry(CMFileSyncIndexSnapshotStore store, Path baseDir) {
        this.store = store;
        this.baseDir = baseDir;
    }

    public CMFileSyncIndexRepository getOrLoad(String initiatorName, UUID initiatorDeviceUuid) {
        var key = new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid);
        return repos.computeIfAbsent(key, k -> {
            var dir = baseDir.resolve(initiatorName)
                    .resolve(CMUUIDConverter.uuidToString(initiatorDeviceUuid));
            var repo = new CMFileSyncIndexRepository(store, dir);
            try {
                repo.warmUpFromDisk();
            } catch (Exception ignore) {
            }
            return repo;
        });
    }

    public void flush(String initiatorName, UUID initiatorDeviceUuid) {
        var repo = repos.get(new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid));
        if (repo == null) return;
        try {
            repo.flushSnapshot();
        } catch (Exception e) {
            /* log */
        }
    }

    // 선택: idle 기준으로 플러시+제거
    public void evict(BiPredicate<CMFileSyncStateKey, CMFileSyncIndexRepository> shouldEvict) {
        repos.forEach((k, r) -> {
            if (shouldEvict.test(k, r)) {
                flush(k.initiatorName(), k.initiatorDeviceUuid());
                repos.remove(k);
            }
        });
    }
}
