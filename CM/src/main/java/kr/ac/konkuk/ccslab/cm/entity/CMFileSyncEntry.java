package kr.ac.konkuk.ccslab.cm.entity;


import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class CMFileSyncEntry implements Comparable<CMFileSyncEntry> {

    private Path pathRelativeToHome;    // remaining path except the sync home
    private long size;  // the file size
    private FileTime lastModifiedTime;  // last modified time

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
                '}';
    }
}
