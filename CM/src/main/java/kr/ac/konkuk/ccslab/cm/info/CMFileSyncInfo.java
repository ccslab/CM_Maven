package kr.ac.konkuk.ccslab.cm.info;

import java.nio.file.Path;
import java.util.List;

public class CMFileSyncInfo {

    public static final String SYNC_HOME = "FileSyncHome";
    private boolean syncInProgress;
    private List<Path> pathList;

    public CMFileSyncInfo() {
        syncInProgress = false;
        pathList = null;
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
}
