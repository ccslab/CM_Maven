package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class CMFileSyncEventLocalModeListAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventLocalModeListAckTest.marshallUnmarshall() called..");
        CMFileSyncEventLocalModeListAck listAckEvent = new CMFileSyncEventLocalModeListAck();
        listAckEvent.setRequester("ccslab");
        List<Path> pathList = List.of(Path.of("test1.txt"), Path.of("test2.txt"));
        listAckEvent.setRelativePathList(pathList);
        listAckEvent.setReturnCode(1);
        System.out.println("onlineModeListEvent = " + listAckEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(listAckEvent);
        CMFileSyncEventLocalModeListAck unmarhsallEvent =
                (CMFileSyncEventLocalModeListAck) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals(listAckEvent, unmarhsallEvent);
    }

    @Test(expected=NullPointerException.class)
    public void marshallException() {
        System.out.println("===== CMFileSyncEventLocalModeListAckTest.marshallException() called..");
        CMFileSyncEventLocalModeListAck listAckEvent = new CMFileSyncEventLocalModeListAck();

        ByteBuffer buffer = CMEventManager.marshallEvent(listAckEvent);
    }
}