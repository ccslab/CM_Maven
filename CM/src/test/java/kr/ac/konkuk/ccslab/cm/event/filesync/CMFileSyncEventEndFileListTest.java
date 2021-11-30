package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventEndFileListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndFileListTest.marshallUnmarshall() called..");
        CMFileSyncEventEndFileList fsEvent = new CMFileSyncEventEndFileList();
        fsEvent.setUserName("ccslab");
        fsEvent.setNumFilesCompleted(20);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventEndFileList unmarshallEvent =
                (CMFileSyncEventEndFileList) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 20);

        assertEquals(fsEvent, unmarshallEvent);
    }
}