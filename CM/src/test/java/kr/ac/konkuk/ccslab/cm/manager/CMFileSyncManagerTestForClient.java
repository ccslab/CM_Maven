package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class CMFileSyncManagerTestForClient {

    CMInfo cmInfo;
    CMFileSyncManager fileSyncManager;

    @Before
    public void setUp() {
        System.out.println("===== called setUp()");
        cmInfo = new CMInfo();
        CMConfigurator.init("cm-client.conf", cmInfo);
        fileSyncManager = new CMFileSyncManager(cmInfo);
    }

    @Test
    public void getClientSyncHome() {

        System.out.println("====== called getClientSyncHome()");
        Path clientSyncHome = fileSyncManager.getClientSyncHome();
        System.out.println("client sync home: "+clientSyncHome.toString());
        assertNotNull(clientSyncHome);
    }

    @Test
    public void startFileSync() {
        System.out.println("===== called startFileSync()");
        assertTrue(fileSyncManager.startFileSync());
    }

    @Test
    public void createPathList() {
        System.out.println("===== called createPathList()");
        Path syncHome = fileSyncManager.getClientSyncHome();
        assertNotNull(fileSyncManager.createPathList(syncHome));
    }
}