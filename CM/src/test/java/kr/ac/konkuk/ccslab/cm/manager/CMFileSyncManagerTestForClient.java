package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMTestFileModType;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        assertTrue(fileSyncManager.sync());
    }

    @Test
    public void createPathList() {
        System.out.println("===== called createPathList()");
        Path syncHome = fileSyncManager.getClientSyncHome();
        assertNotNull(fileSyncManager.createPathList(syncHome));
    }

/*

    @Test
    public void createTestFile() {
        System.out.println("==== called createTestFile()");
        Path dir = cmInfo.getConfigurationInfo().getTransferedFileHome().resolve("test-file-sync");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean ret;
        try {
            Path file1 = dir.resolve("10k.test");
            ret = fileSyncManager.createTestFile(file1, 10*1024L);
            assertTrue(ret);
            assertEquals(Files.size(file1), 10*1024L);

            Path file2 = dir.resolve("100k.test");
            ret = fileSyncManager.createTestFile(file2, 100*1024L);
            assertTrue(ret);
            assertEquals(Files.size(file2), 100*1024L);

            Path file3 = dir.resolve("1m.test");
            ret = fileSyncManager.createTestFile(file3, 1024*1024L);
            assertTrue(ret);
            assertEquals(Files.size(file3), 1024*1024L);

            Path file4 = dir.resolve("10m.test");
            ret = fileSyncManager.createTestFile(file4, 10*1024*1024L);
            assertTrue(ret);
            assertEquals(Files.size(file4), 10*1024*1024L);

            Path file5 = dir.resolve("100m.test");
            ret = fileSyncManager.createTestFile(file5, 100*1024*1024L);
            assertTrue(ret);
            assertEquals(Files.size(file5), 100*1024*1024L);

            Path file6 = dir.resolve("1g.test");
            ret = fileSyncManager.createTestFile(file6, 1024*1024*1024L);
            assertTrue(ret);
            assertEquals(Files.size(file6), 1024*1024*1024L);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createModifiedTestFile() {
        System.out.println("==== called createModifiedTestFile()");
        Path dir = cmInfo.getConfigurationInfo().getTransferedFileHome().resolve("test-file-sync");
        boolean ret;
        String[] fileNameArray = {"10k.test", "100k.test", "1m.test", "10m.test", "100m.test", "1g.test"};

        for(String name: fileNameArray) {
            String prefix = name.substring(0, name.lastIndexOf(".test"));
            String postfix = name.substring(name.lastIndexOf(".test"));
            for(int i = 10; i<=100; i+=10) {
                String modName = prefix+"-"+i+postfix;
                ret = fileSyncManager.createModifiedTestFile(dir.resolve(name), dir.resolve(modName), i);
                assertTrue(ret);
                try {
                    assertTrue(Files.mismatch(dir.resolve(name), dir.resolve(modName)) >= 0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
*/

/*
    @Test
    public void createAppendedTestFile() {
        System.out.println("=== called createAppendedTestFile()");
        Path dir = cmInfo.getConfigurationInfo().getTransferedFileHome().resolve("test-appended-file-sync-2");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean ret;
        String[] fileNameArray = {"100k.test", "1m.test", "10m.test"};

        try {
            // create files
            Path file2 = dir.resolve("100k.test");
            ret = fileSyncManager.createTestFile(file2, 100*1024L);
            assertTrue(ret);
            assertEquals(Files.size(file2), 100*1024L);

            Path file3 = dir.resolve("1m.test");
            ret = fileSyncManager.createTestFile(file3, 1024*1024L);
            assertTrue(ret);
            assertEquals(Files.size(file3), 1024*1024L);

            Path file4 = dir.resolve("10m.test");
            ret = fileSyncManager.createTestFile(file4, 10*1024*1024L);
            assertTrue(ret);
            assertEquals(Files.size(file4), 10*1024*1024L);

            for(String name : fileNameArray) {
                // create appended files
                String prefix = name.substring(0, name.lastIndexOf(".test"));
                String postfix = name.substring(name.lastIndexOf(".test"));
                for(int i = 100; i<=900; i+=200) {
                    String modName = prefix+"-"+i+"-appended"+postfix;
                    ret = fileSyncManager.createModifiedTestFile(dir.resolve(name), dir.resolve(modName),
                            CMTestFileModType.APPEND, i);
                    try {
                        assertTrue(Files.mismatch(dir.resolve(name), dir.resolve(modName)) >= 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
*/

/*
    @Test
    public void createTruncatedTestFile() {
        System.out.println("=== called createTruncatedTestFile()");
        Path dir = cmInfo.getConfigurationInfo().getTransferedFileHome().resolve("test-trunc-file-sync");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        boolean ret;
        String name = "10m.test";

        try {
            // create a 10m file
            Path file4 = dir.resolve(name);
            //ret = fileSyncManager.createTestFile(file4, 10 * 1024 * 1024L);
            ret = fileSyncManager.createTestFile(file4, 1024L);
            assertTrue(ret);
            //assertEquals(Files.size(file4), 10 * 1024 * 1024L);
            assertEquals(Files.size(file4), 1024L);
            // create appended files
            String prefix = name.substring(0, name.lastIndexOf(".test"));
            String postfix = name.substring(name.lastIndexOf(".test"));
            for(int i = 10; i<=100; i+=10) {
                String modName = prefix+"-"+i+"-trunc"+postfix;
                ret = fileSyncManager.createModifiedTestFile(dir.resolve(name), dir.resolve(modName),
                        CMTestFileModType.TRUNC, i);
                try {
                    assertTrue(Files.mismatch(dir.resolve(name), dir.resolve(modName)) >= 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
*/

    @Test
    public void getSyncDirectoryList() {
        System.out.println("===== called getSyncDirectoryList()");
        List<Path> dirList = fileSyncManager.getSyncDirectoryList();
        assertNotNull(dirList);
    }

    @Test
    public void calculateDirActivationRatio() {
        System.out.println("===== called calculateDirActivationRatio()");
        Path syncHome = fileSyncManager.getClientSyncHome();
        double DAR = fileSyncManager.calculateDirActivationRatio(syncHome);

    }

}