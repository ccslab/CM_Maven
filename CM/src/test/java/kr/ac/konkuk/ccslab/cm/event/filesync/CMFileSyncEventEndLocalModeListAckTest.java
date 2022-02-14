package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventEndLocalModeListAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndLocalModeAckTest.marshallUnmarshall() called..");
        CMFileSyncEventEndLocalModeListAck ackEvent = new CMFileSyncEventEndLocalModeListAck();
        ackEvent.setRequester("ccslab");
        ackEvent.setNumLocalModeFiles(2);
        ackEvent.setReturnCode(1);
        System.out.println("ackEvent = " + ackEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(ackEvent);
        CMFileSyncEventEndLocalModeListAck unmarhsallEvent =
                (CMFileSyncEventEndLocalModeListAck) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals(ackEvent, unmarhsallEvent);
    }
}