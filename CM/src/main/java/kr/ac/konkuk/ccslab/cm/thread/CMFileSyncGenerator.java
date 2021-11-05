package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMFileSyncGenerator implements Runnable {
    private String userName;
    private CMInfo cmInfo;

    public CMFileSyncGenerator(String userName, CMInfo cmInfo) {
        this.userName = userName;
        this.cmInfo = cmInfo;
    }

    @Override
    public void run() {
        // from here
    }
}
