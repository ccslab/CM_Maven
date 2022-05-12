package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

public class CMFileSyncProactiveModeTask implements Runnable {

    final private CMFileSyncManager syncManager;
    final private CMFileSyncInfo syncInfo;
    final private CMConfigurationInfo confInfo;

    public CMFileSyncProactiveModeTask(CMFileSyncManager syncManager, CMFileSyncInfo syncInfo,
                                       CMConfigurationInfo confInfo) {
        this.syncManager = syncManager;
        this.syncInfo = syncInfo;
        this.confInfo = confInfo;
    }

    @Override
    public void run() {
        // TODO: not yet
        System.err.println("CMFileSyncProactiveModeTask.run() not implemented yet!");
    }
}
