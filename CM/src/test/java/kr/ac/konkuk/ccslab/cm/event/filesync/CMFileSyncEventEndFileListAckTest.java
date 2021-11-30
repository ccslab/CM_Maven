package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventEndFileListAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndFileListAckTest.marshallUnmarshall() called..");
        CMFileSyncEventEndFileListAck fsEvent = new CMFileSyncEventEndFileListAck();
        fsEvent.setUserName("ccslab");
        fsEvent.setNumFilesCompleted(8);
        fsEvent.setReturnCode(1);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventEndFileListAck unmarshallEvent =
                (CMFileSyncEventEndFileListAck) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 8);
        int returnCode = unmarshallEvent.getReturnCode();
        assertEquals(returnCode, 1);

        assertEquals(fsEvent, unmarshallEvent);
    }
}