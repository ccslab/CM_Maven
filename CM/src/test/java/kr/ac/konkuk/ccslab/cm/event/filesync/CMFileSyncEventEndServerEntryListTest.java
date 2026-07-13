package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventEndServerEntryListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndServerEntryListTest.marshallUnmarshall() called..");
        CMFileSyncEventEndServerEntryList fsEvent = new CMFileSyncEventEndServerEntryList();
        fsEvent.setInitiatorName("ccslab");
        fsEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fsEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fsEvent.setNumFilesCompleted(42);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventEndServerEntryList unmarshallEvent =
                (CMFileSyncEventEndServerEntryList) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());
        assertEquals(42, unmarshallEvent.getNumFilesCompleted());

        assertEquals(fsEvent, unmarshallEvent);
    }
}
