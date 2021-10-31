package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class CMFileSyncManagerTest {

    CMInfo cmInfo;
    CMFileSyncManager fileSyncManager;

    @Before
    public void setUp() {
        cmInfo = new CMInfo();
        fileSyncManager = new CMFileSyncManager(cmInfo);
    }

    @Test
    public void getClientSyncHome() {

        System.out.println("====== called getClientSyncHome()");
        CMConfigurator.init("cm-client.conf", cmInfo);

        Path clientSyncHome = fileSyncManager.getClientSyncHome();
        System.out.println("client sync home: "+clientSyncHome.toString());
        assertNotNull(clientSyncHome);
    }

    @Test
    public void getServerSyncHome() {

        System.out.println("====== called getServerSyncHome()");
        CMConfigurator.init("cm-server.conf", cmInfo);

        Path serverSyncHome = fileSyncManager.getServerSyncHome("ccslab");
        System.out.println("server sync home for ccslab: "+serverSyncHome.toString());
        assertNotNull(serverSyncHome);
    }
}