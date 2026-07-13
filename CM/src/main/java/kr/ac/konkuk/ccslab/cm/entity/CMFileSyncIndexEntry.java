package kr.ac.konkuk.ccslab.cm.entity;

import kr.ac.konkuk.ccslab.cm.info.enums.CMFileType;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

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
    public CMFileSyncEntry toProtocolEntry() {
        return new CMFileSyncEntry()
                .setPathRelativeToHome(Path.of(this.path))
                .setSize(this.size)
                .setLastModifiedTime(FileTime.from(Instant.ofEpochSecond(this.mtime)))
                .setType(this.isDirectory ? CMFileType.DIR : CMFileType.FILE);
    }
}
