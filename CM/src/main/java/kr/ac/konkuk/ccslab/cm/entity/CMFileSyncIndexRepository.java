package kr.ac.konkuk.ccslab.cm.entity;

import java.io.IOException;
import java.nio.file.Path;

public class CMFileSyncIndexRepository {
    private final CMFileSyncInMemoryIndex mem = new CMFileSyncInMemoryIndex();
    private final CMFileSyncIndexSnapshotStore store;
    private final Path dir; // /srv/.../<user>/<device>/

    public CMFileSyncIndexRepository(CMFileSyncIndexSnapshotStore store, Path dir) {
        this.store = store;
        this.dir = dir;
    }

    public void warmUpFromDisk() throws IOException {
        var snap = store.loadOrEmpty(dir);
        snap.entries().values().forEach(mem::upsert);
    }

    // ChangeLog 적용 경로(단일 writer에서만 호출)
    public void applyCreateOrModify(String path, boolean isDirectory, String hash,
                                    long mtime, long size, long changeId) {
        var prev = mem.get(path);
        int v = prev == null ? 1 : prev.version() + 1;
        mem.upsert(new CMFileSyncIndexEntry(path, isDirectory, hash, mtime, size, false, v, changeId));
    }

    public void applyDelete(String path, boolean isDirectory, long changeId, long now) {
        mem.delete(path, isDirectory, changeId, now);
    }

    public void applyRename(String from, String to, boolean isDirectory, String hash,
                            long mtime, long size, long changeId) {
        var prevTo = mem.get(to);
        int v = prevTo == null ? 1 : prevTo.version() + 1;
        var meta = new CMFileSyncIndexEntry(to, isDirectory, hash, mtime, size, false, v, changeId);
        mem.rename(from, to, meta);
    }

    public void flushSnapshot() throws IOException {
        store.saveAtomically(dir, mem.snapshot());
    }

    // 읽기용
    public CMFileSyncIndexEntry readEntry(String path) {
        return mem.get(path);
    }

    public CMFileSyncIndexSnapshot readSnapshot() {
        return mem.snapshot();
    }

    public long lastChangeId() {
        return mem.lastChangeId();
    }
}
