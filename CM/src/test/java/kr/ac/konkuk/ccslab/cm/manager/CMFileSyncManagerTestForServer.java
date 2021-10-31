package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class CMFileSyncManagerTestForServer {
    CMInfo cmInfo;
    CMFileSyncManager fileSyncManager;

    @Before
    public void setUp() {
        System.out.println("===== called setUp()");
        cmInfo = new CMInfo();
        CMConfigurator.init("cm-server.conf", cmInfo);
        fileSyncManager = new CMFileSyncManager(cmInfo);
    }

    @Test
    public void getServerSyncHome() {

        System.out.println("====== called getServerSyncHome()");
        Path serverSyncHome = fileSyncManager.getServerSyncHome("ccslab");
        System.out.println("server sync home for ccslab: "+serverSyncHome.toString());
        assertNotNull(serverSyncHome);
    }

}