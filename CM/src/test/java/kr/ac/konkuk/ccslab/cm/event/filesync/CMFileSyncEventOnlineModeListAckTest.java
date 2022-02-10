package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class CMFileSyncEventOnlineModeListAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventOnlineModeListAckTest.marshallUnmarshall() called..");
        CMFileSyncEventOnlineModeListAck listAckEvent = new CMFileSyncEventOnlineModeListAck();
        listAckEvent.setRequester("ccslab");
        List<Path> pathList = List.of(Path.of("test1.txt"), Path.of("test2.txt"));
        listAckEvent.setRelativePathList(pathList);
        listAckEvent.setReturnCode(1);
        System.out.println("onlineModeListEvent = " + listAckEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(listAckEvent);
        CMFileSyncEventOnlineModeListAck unmarhsallEvent =
                (CMFileSyncEventOnlineModeListAck) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals(listAckEvent, unmarhsallEvent);
    }

    @Test(expected=NullPointerException.class)
    public void marshallException() {
        System.out.println("===== CMFileSyncEventOnlineModeListAckTest.marshallException() called..");
        CMFileSyncEventOnlineModeListAck listAckEvent = new CMFileSyncEventOnlineModeListAck();

        ByteBuffer buffer = CMEventManager.marshallEvent(listAckEvent);
    }
}