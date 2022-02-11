package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventEndOnlineModeListAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndOnlineModeAckTest.marshallUnmarshall() called..");
        CMFileSyncEventEndOnlineModeListAck ackEvent = new CMFileSyncEventEndOnlineModeListAck();
        ackEvent.setRequester("ccslab");
        ackEvent.setNumOnlineModeFiles(2);
        ackEvent.setReturnCode(1);
        System.out.println("ackEvent = " + ackEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(ackEvent);
        CMFileSyncEventEndOnlineModeListAck unmarhsallEvent =
                (CMFileSyncEventEndOnlineModeListAck) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals(ackEvent, unmarhsallEvent);
    }
}