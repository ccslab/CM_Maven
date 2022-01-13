package kr.ac.konkuk.ccslab.cm.entity;


import kr.ac.konkuk.ccslab.cm.info.enums.CMFileType;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class CMFileSyncEntry implements Comparable<CMFileSyncEntry> {

    private Path pathRelativeToHome;    // remaining path except the sync home
    private long size;  // the file size
    private FileTime lastModifiedTime;  // last modified time
    private CMFileType type;    // file type enum

    public CMFileSyncEntry() {
        pathRelativeToHome = null;
        size = 0;
        lastModifiedTime = null;
        type = null;
    }

    public Path getPathRelativeToHome() {
        return pathRelativeToHome;
    }

    public CMFileSyncEntry setPathRelativeToHome(Path pathRelativeToHome) {
        this.pathRelativeToHome = pathRelativeToHome;
        return this;
    }

    public long getSize() {
        return size;
    }

    public CMFileSyncEntry setSize(long size) {
        this.size = size;
        return this;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public CMFileSyncEntry setLastModifiedTime(FileTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
        return this;
    }

    public CMFileType getType() {
        return type;
    }

    public CMFileSyncEntry setType(CMFileType type) {
        this.type = type;
        return this;
    }

    @Override
    public int compareTo(CMFileSyncEntry o) {
        return pathRelativeToHome.compareTo(o.getPathRelativeToHome());
    }

    @Override
    public String toString() {
        return "CMFileSyncEntry{" +
                "pathRelativeToHome=" + pathRelativeToHome +
                ", size=" + size +
                ", lastModifiedTime=" + lastModifiedTime +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(obj == this) return true;
        if(!(obj instanceof CMFileSyncEntry entry)) return false;
        return entry.getLastModifiedTime().equals(lastModifiedTime) &&
                entry.getSize() == size &&
                entry.getPathRelativeToHome().equals(pathRelativeToHome) &&
                entry.getType().equals(type);
    }
}
