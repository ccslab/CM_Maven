package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventLocalModeListAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventLocalModeListAckTest.marshallUnmarshall() called..");
        CMFileSyncEventLocalModeListAck listAckEvent = new CMFileSyncEventLocalModeListAck();
        listAckEvent.setInitiatorName("ccslab");
        listAckEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        listAckEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        List<Path> pathList = List.of(Path.of("test1.txt"), Path.of("test2.txt"));
        listAckEvent.setRelativePathList(pathList);
        listAckEvent.setReturnCode(1);
        System.out.println("localModeListAckEvent = " + listAckEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(listAckEvent);
        CMFileSyncEventLocalModeListAck unmarhsallEvent =
                (CMFileSyncEventLocalModeListAck) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals("ccslab", unmarhsallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarhsallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarhsallEvent.getInitiatorDeviceUuid());
        assertEquals(listAckEvent, unmarhsallEvent);
    }

    @Test(expected=NullPointerException.class)
    public void marshallException() {
        System.out.println("===== CMFileSyncEventLocalModeListAckTest.marshallException() called..");
        CMFileSyncEventLocalModeListAck listAckEvent = new CMFileSyncEventLocalModeListAck();

        ByteBuffer buffer = CMEventManager.marshallEvent(listAckEvent);
    }
}
