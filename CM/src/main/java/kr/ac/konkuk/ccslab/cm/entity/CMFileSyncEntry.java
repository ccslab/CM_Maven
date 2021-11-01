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

    public void setPathRelativeToHome(Path pathRelativeToHome) {
        this.pathRelativeToHome = pathRelativeToHome;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(FileTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public int compareTo(CMFileSyncEntry o) {
        return pathRelativeToHome.compareTo(o.getPathRelativeToHome());
    }
}
