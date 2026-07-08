package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventStartPushEntryListAckTest {
    @Test
    public void marshallUnmarshall() {
        CMFileSyncEventStartPushEntryListAck fsEvent = new CMFileSyncEventStartPushEntryListAck();
        fsEvent.setInitiatorName("ccslab");
        fsEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fsEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fsEvent.setNumTotalFiles(5);
        fsEvent.setReturnCode(1);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventStartPushEntryListAck unmarshallEvent =
                (CMFileSyncEventStartPushEntryListAck) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());
        assertEquals(5, unmarshallEvent.getNumTotalFiles());
        assertEquals(1, unmarshallEvent.getReturnCode());
        assertEquals(fsEvent, unmarshallEvent);
    }

    // [10-3] busy(returnCode=2, 증분 push lease 거절) 값이 marshalling 왕복에서 보존되는지 확인.
    @Test
    public void marshallUnmarshallBusy() {
        CMFileSyncEventStartPushEntryListAck fsEvent = new CMFileSyncEventStartPushEntryListAck();
        fsEvent.setInitiatorName("ccslab");
        fsEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fsEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fsEvent.setNumTotalFiles(3);
        fsEvent.setReturnCode(2);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventStartPushEntryListAck unmarshallEvent =
                (CMFileSyncEventStartPushEntryListAck) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        assertEquals(2, unmarshallEvent.getReturnCode());
        assertEquals(fsEvent, unmarshallEvent);
    }
}
