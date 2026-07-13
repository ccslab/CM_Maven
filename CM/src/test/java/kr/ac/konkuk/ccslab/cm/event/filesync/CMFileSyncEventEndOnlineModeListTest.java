package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventEndOnlineModeListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndOnlineModeTest.marshallUnmarshall() called..");
        CMFileSyncEventEndOnlineModeList endEvent = new CMFileSyncEventEndOnlineModeList();
        endEvent.setInitiatorName("ccslab");
        endEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        endEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        endEvent.setNumOnlineModeFiles(2);
        System.out.println("endEvent = " + endEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(endEvent);
        CMFileSyncEventEndOnlineModeList unmarhsallEvent =
                (CMFileSyncEventEndOnlineModeList) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals("ccslab", unmarhsallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarhsallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarhsallEvent.getInitiatorDeviceUuid());
        assertEquals(endEvent, unmarhsallEvent);
    }
}
