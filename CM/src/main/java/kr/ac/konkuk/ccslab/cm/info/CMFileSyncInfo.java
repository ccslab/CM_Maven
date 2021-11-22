package kr.ac.konkuk.ccslab.cm.info;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.thread.CMFileSyncGenerator;

import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;

public class CMFileSyncInfo {

    public static final String SYNC_HOME = "FileSyncHome";
    public static final int BLOCK_SIZE = 700;   // Bytes, from rsync
    public static final int MAX_BLOCK_SIZE = 1 << 17;   // 2^17 = 131,072 (from rsync version > 30)

    private boolean syncInProgress;
    private List<Path> pathList;        // 4 client
    private Hashtable<Path, Boolean> isFileSyncCompletedHashtable;  // 4 client
    private Hashtable<Integer, CMFileSyncBlockChecksum[]> blockChecksumHashtable;   // 4 client
    private Hashtable<Integer, Hashtable<Short, Integer>> fileIndexToHashToBlockIndexHashtable; // 4 client

    private Hashtable<String, List<CMFileSyncEntry>> fileEntryListHashtable;    // 4 server
    private Hashtable<String, CMFileSyncGenerator> syncGeneratorHashtable;      // 4 server

    public CMFileSyncInfo() {
        syncInProgress = false;
        pathList = null;
        isFileSyncCompletedHashtable = new Hashtable<>();
        blockChecksumHashtable = new Hashtable<>();
        fileIndexToHashToBlockIndexHashtable = new Hashtable<>();

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
