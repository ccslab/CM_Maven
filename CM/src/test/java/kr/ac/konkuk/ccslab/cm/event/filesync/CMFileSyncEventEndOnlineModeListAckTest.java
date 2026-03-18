package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventEndOnlineModeListAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndOnlineModeAckTest.marshallUnmarshall() called..");
        CMFileSyncEventEndOnlineModeListAck ackEvent = new CMFileSyncEventEndOnlineModeListAck();
        ackEvent.setInitiatorName("ccslab");
        ackEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ackEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        ackEvent.setNumOnlineModeFiles(2);
        ackEvent.setReturnCode(1);
        System.out.println("ackEvent = " + ackEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(ackEvent);
        CMFileSyncEventEndOnlineModeListAck unmarhsallEvent =
                (CMFileSyncEventEndOnlineModeListAck) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals("ccslab", unmarhsallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarhsallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarhsallEvent.getInitiatorDeviceUuid());
        assertEquals(ackEvent, unmarhsallEvent);
    }
}
