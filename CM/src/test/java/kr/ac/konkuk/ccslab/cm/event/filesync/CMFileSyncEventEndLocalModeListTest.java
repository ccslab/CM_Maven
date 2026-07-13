package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventEndLocalModeListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndLocalModeTest.marshallUnmarshall() called..");
        CMFileSyncEventEndLocalModeList endEvent = new CMFileSyncEventEndLocalModeList();
        endEvent.setInitiatorName("ccslab");
        endEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        endEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        endEvent.setNumLocalModeFiles(2);
        System.out.println("endEvent = " + endEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(endEvent);
        CMFileSyncEventEndLocalModeList unmarhsallEvent =
                (CMFileSyncEventEndLocalModeList) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals("ccslab", unmarhsallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarhsallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarhsallEvent.getInitiatorDeviceUuid());
        assertEquals(endEvent, unmarhsallEvent);
    }
}
