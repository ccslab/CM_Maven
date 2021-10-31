package kr.ac.konkuk.ccslab.cm.info;

public class CMFileSyncInfo {

    public static final String SYNC_HOME = "FileSyncHome";
    private boolean syncInProgress;

    public CMFileSyncInfo() {
        syncInProgress = false;
    }

    public boolean isSyncInProgress() {
        return syncInProgress;
    }

    public void setSyncInProgress(boolean syncInProgress) {
        this.syncInProgress = syncInProgress;
    }
}
