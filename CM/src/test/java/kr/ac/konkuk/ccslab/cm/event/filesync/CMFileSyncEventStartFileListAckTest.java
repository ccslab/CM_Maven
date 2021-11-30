package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventStartFileListAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventStartFileListAckTest.marshallUnmarshall() called..");
        CMFileSyncEventStartFileListAck fsEvent = new CMFileSyncEventStartFileListAck();
        fsEvent.setUserName("ccslab");
        fsEvent.setNumTotalFiles(50);
        fsEvent.setReturnCode(1);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventStartFileListAck unmarshallEvent =
                (CMFileSyncEventStartFileListAck) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numTotalFiles = unmarshallEvent.getNumTotalFiles();
        assertEquals(numTotalFiles, 50);
        int returnCode = unmarshallEvent.getReturnCode();
        assertEquals(returnCode, 1);

        assertEquals(fsEvent, unmarshallEvent);
    }
}