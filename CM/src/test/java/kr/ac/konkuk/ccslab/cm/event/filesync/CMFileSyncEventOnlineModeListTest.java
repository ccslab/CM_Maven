package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventOnlineModeListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventOnlineModeListTest.marshallUnmarshall() called..");
        CMFileSyncEventOnlineModeList listEvent = new CMFileSyncEventOnlineModeList();
        listEvent.setInitiatorName("ccslab");
        listEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        listEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        List<String> pathList = List.of("test1.txt", "test2.txt");
        listEvent.setRelativePathList(pathList);
        System.out.println("onlineModeListEvent = " + listEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(listEvent);
        CMFileSyncEventOnlineModeList unmarhsallEvent =
                (CMFileSyncEventOnlineModeList) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals("ccslab", unmarhsallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarhsallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarhsallEvent.getInitiatorDeviceUuid());
        assertEquals(listEvent, unmarhsallEvent);
    }

    @Test(expected=NullPointerException.class)
    public void marshallException() {
        System.out.println("===== CMFileSyncEventOnlineModeListTest.marshallException() called..");
        CMFileSyncEventOnlineModeList listEvent = new CMFileSyncEventOnlineModeList();
        listEvent.setInitiatorName("ccslab");

        ByteBuffer buffer = CMEventManager.marshallEvent(listEvent);
    }
}
