package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventCompletePullCreateTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventCompletePullCreateTest.marshallUnmarshall() called..");
        CMFileSyncEventCompletePullCreate fsEvent = new CMFileSyncEventCompletePullCreate();
        fsEvent.setInitiatorName("ccslab");
        fsEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fsEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fsEvent.setCreatedPath("dir/newfile.txt");
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventCompletePullCreate unmarshallEvent =
                (CMFileSyncEventCompletePullCreate) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());
        assertEquals("dir/newfile.txt", unmarshallEvent.getCreatedPath());

        assertEquals(fsEvent, unmarshallEvent);
    }
}
