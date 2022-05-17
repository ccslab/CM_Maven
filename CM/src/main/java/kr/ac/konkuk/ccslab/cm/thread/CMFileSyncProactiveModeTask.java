package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

import java.nio.file.Path;
import java.util.List;

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
        if(CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncProactiveModeTask.run() called..");
        }
        ///// get directory-activation-ratio of file-sync home and its subdirectories
        // get file-sync directory and its subdirectories
        List<Path> directoryList = syncManager.getSyncDirectoryList();
        if (directoryList == null) {
            System.err.println("file-sync directory list is null!");
            return;
        }

        double onlineModeThreshold = confInfo.getOnlineModeThreshold();
        double localModeThreshold = confInfo.getLocalModeThreshold();
        if(CMInfo._CM_DEBUG) {
            System.out.println("onlineModeThreshold = " + onlineModeThreshold);
            System.out.println("localModeThreshold = " + localModeThreshold);
        }

        boolean ret = false;
        for(Path dir : directoryList) {
            // get directory-activation-ratio
            double dirActivationRatio = syncManager.calculateDirActivationRatio(dir);
            if(CMInfo._CM_DEBUG) {
                System.out.println("*** dir = " + dir);
                System.out.println("* dirActivationRatio = " + dirActivationRatio);
            }
            // check directory-activation-ratio
            if(dirActivationRatio == 0) {
                if(CMInfo._CM_DEBUG) {
                    System.out.println("* No need to start proactive (online or local) mode!");
                }
                continue;
            }
            if(dirActivationRatio < onlineModeThreshold) {
                ret = syncManager.startProactiveOnlineMode(dir);
                if(!ret) {
                    System.err.println("* error to start proactive online mode!: " + dir);
                }
            }
            else if(dirActivationRatio >= localModeThreshold) {
                ret = syncManager.startProactiveLocalMode(dir);
                if(!ret) {
                    System.err.println("* error to start proactive local mode!: " + dir);
                }
            }
            else {
                if(CMInfo._CM_DEBUG) {
                    System.out.println("* No need to start proactive (online or local) mode!");
                }
            }
        }

    }
}
