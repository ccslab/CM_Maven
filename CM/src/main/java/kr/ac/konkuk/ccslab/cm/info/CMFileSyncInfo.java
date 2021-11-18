package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;

import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;

public class CMFileSyncInfo {

    public static final String SYNC_HOME = "FileSyncHome";
    private boolean syncInProgress;
    private List<Path> pathList;        // 4 client
    private Hashtable<Path, Boolean> isFileSyncCompletedHashtable;  // 4 client

    private Hashtable<String, List<CMFileSyncEntry>> fileEntryListHashtable;    // 4 server
    private Hashtable<String, CMFileSyncGenerator> syncGeneratorHashtable;      // 4 server

    public CMFileSyncInfo() {
        syncInProgress = false;
        pathList = null;
        isFileSyncCompletedHashtable = new Hashtable<>();
        fileEntryListHashtable = new Hashtable<>();
        syncGeneratorHashtable = new Hashtable<>();
    }

    public boolean isSyncInProgress() {
        return syncInProgress;
    }

    public void setSyncInProgress(boolean syncInProgress) {
        this.syncInProgress = syncInProgress;
        if(CMInfo._CM_DEBUG) {
            System.out.println("CMFileSyncInfo.setSyncInProgress( "+syncInProgress+" ) called ..");
        }
    }

    public List<Path> getPathList() {
        return pathList;
    }

    public void setPathList(List<Path> pathList) {
        this.pathList = pathList;
    }

    public Hashtable<Path, Boolean> getIsFileSyncCompletedHashtable() {
        return isFileSyncCompletedHashtable;
    }

    public Hashtable<String, List<CMFileSyncEntry>> getFileEntryListHashtable() {
        return fileEntryListHashtable;
    }

    public Hashtable<String, CMFileSyncGenerator> getSyncGeneratorHashtable() {
        return syncGeneratorHashtable;
    }
}
