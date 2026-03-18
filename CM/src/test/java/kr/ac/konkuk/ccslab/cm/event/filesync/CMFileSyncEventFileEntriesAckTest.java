package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventFileEntriesAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventFileEntriesAckTest.marshallUnmarshall() called..");
        CMFileSyncEventFileEntriesAck fsEvent = new CMFileSyncEventFileEntriesAck();
        fsEvent.setID(CMFileSyncEvent.FILE_ENTRIES_ACK);
        fsEvent.setInitiatorName("ccslab");
        fsEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fsEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fsEvent.setNumFilesCompleted(5);
        fsEvent.setNumFiles(11);
        fsEvent.setReturnCode(1);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventFileEntriesAck unmarshallEvent =
                (CMFileSyncEventFileEntriesAck) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());
        assertEquals(5, unmarshallEvent.getNumFilesCompleted());
        assertEquals(11, unmarshallEvent.getNumFiles());
        assertEquals(1, unmarshallEvent.getReturnCode());

        assertEquals(fsEvent, unmarshallEvent);
    }
}
