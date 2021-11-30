package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventStartFileListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventStartFileListTest.marshallUnmarshall() called..");
        CMFileSyncEventStartFileList sfle = new CMFileSyncEventStartFileList();
        sfle.setUserName("ccslab");
        sfle.setNumTotalFiles(50);
        System.out.println("sfle = " + sfle);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(sfle);
        CMFileSyncEventStartFileList unmarshallEvent = (CMFileSyncEventStartFileList) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numTotalFiles = unmarshallEvent.getNumTotalFiles();
        assertEquals(numTotalFiles, 50);

        assertEquals(sfle, unmarshallEvent);
    }
}