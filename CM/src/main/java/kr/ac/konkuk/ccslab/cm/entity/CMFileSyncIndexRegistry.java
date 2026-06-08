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

    // 캐시된 repo 를 flush 없이 제거한다. 다음 getOrLoad 가 디스크에서 다시 warmUpFromDisk 하도록 하여,
    // 서버 실행 중 디스크의 index/changelog 가 외부에서 삭제·변경된 경우 stale in-memory 를 버린다.
    // evict/flush 와 달리 영속화하지 않으므로 stale 값이 디스크로 되돌아가지 않는다.
    // (정상 흐름에선 직전 push 세션이 flushSnapshot 으로 디스크를 갱신했고 pull 은 서버 index 를 바꾸지 않으므로
    //  재로드는 idempotent.) 클라측 CMFileSyncInfo.reloadClientMetaFromDisk 와 대칭.
    public void invalidate(String initiatorName, UUID initiatorDeviceUuid) {
        repos.remove(new CMFileSyncStateKey(initiatorName, initiatorDeviceUuid));
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
