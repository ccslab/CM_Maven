package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;

import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;

public class CMFileSyncInfo {

    public static final String SYNC_HOME = "FileSyncHome";
    private boolean syncInProgress;
    private List<Path> pathList;        // 4 client
    private Hashtable<String, List<CMFileSyncEntry>> fileEntryListHashtable;    // 4 server

    public CMFileSyncInfo() {
        syncInProgress = false;
        pathList = null;
        fileEntryListHashtable = new Hashtable<>();
    }

    public boolean isSyncInProgress() {
        return syncInProgress;
    }

    public void setSyncInProgress(boolean syncInProgress) {
        this.syncInProgress = syncInProgress;
    }

    public List<Path> getPathList() {
        return pathList;
    }

    public void setPathList(List<Path> pathList) {
        this.pathList = pathList;
    }

    public Hashtable<String, List<CMFileSyncEntry>> getFileEntryListHashtable() {
        return fileEntryListHashtable;
    }

    public void setFileEntryListHashtable(Hashtable<String, List<CMFileSyncEntry>> fileEntryListHashtable) {
        this.fileEntryListHashtable = fileEntryListHashtable;
    }
}
