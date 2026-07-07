package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventSyncNeededNotifyTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventSyncNeededNotifyTest.marshallUnmarshall() called..");
        CMFileSyncEventSyncNeededNotify fsEvent = new CMFileSyncEventSyncNeededNotify();
        fsEvent.setInitiatorName("ccslab");
        fsEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fsEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fsEvent.setChangelogHead(42L);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventSyncNeededNotify unmarshallEvent =
                (CMFileSyncEventSyncNeededNotify) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());
        assertEquals(42L, unmarshallEvent.getChangelogHead());

        assertEquals(fsEvent, unmarshallEvent);
        assertEquals(fsEvent.hashCode(), unmarshallEvent.hashCode());
    }
}