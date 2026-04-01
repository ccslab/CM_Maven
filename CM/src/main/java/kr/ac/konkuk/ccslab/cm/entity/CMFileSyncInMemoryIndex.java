package kr.ac.konkuk.ccslab.cm.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CMFileSyncInMemoryIndex {
    private volatile long lastChangeId = 0L;
    private final ConcurrentHashMap<String, CMFileSyncIndexEntry> table = new ConcurrentHashMap<>();

    // --- 단일 writer 경로 ---
    public void upsert(CMFileSyncIndexEntry e) {
        table.put(e.path(), e);
        lastChangeId = Math.max(lastChangeId, e.lastChangeId());
    }

    public void delete(String path, boolean isDirectory, long changeId, long now) {
        CMFileSyncIndexEntry prev = table.get(path);
        int v = (prev == null) ? 1 : prev.version() + 1;
        boolean isDir = (prev == null) ? isDirectory : prev.isDirectory();
        String hash = (prev == null) ? null : prev.contentHash();
        table.put(path, new CMFileSyncIndexEntry(
                path, isDir, hash, now, 0L, true, v, changeId));
        lastChangeId = Math.max(lastChangeId, changeId);
    }

    public void rename(String from, String to, CMFileSyncIndexEntry newMeta) {
        CMFileSyncIndexEntry prev = table.get(from);
        int vOld = prev == null ? 1 : prev.version() + 1;
        boolean isDir = (prev == null) ? newMeta.isDirectory() : prev.isDirectory();
        String hash = (prev == null) ? null : prev.contentHash();

        table.put(from, new CMFileSyncIndexEntry(from, isDir, hash, newMeta.mtime(),
                0L, true, vOld, newMeta.lastChangeId()));
        upsert(newMeta);
    }

    // --- 다중 reader: 스냅샷 제공 ---
    public CMFileSyncIndexSnapshot snapshot() {
        return new CMFileSyncIndexSnapshot(
                lastChangeId, Map.copyOf(table),
                java.time.OffsetDateTime.now().toString());
    }

    public CMFileSyncIndexEntry get(String path) {
        return table.get(path);
    }

    public long lastChangeId() {
        return lastChangeId;
    }
}
