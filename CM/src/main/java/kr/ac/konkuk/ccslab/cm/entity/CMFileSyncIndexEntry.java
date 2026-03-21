package kr.ac.konkuk.ccslab.cm.entity;

public record CMFileSyncIndexEntry(
        String path,
        boolean isDirectory,
        String contentHash,
        long mtime,
        long size,
        boolean tombstone,
        int version,
        long lastChangeId
) {
}
