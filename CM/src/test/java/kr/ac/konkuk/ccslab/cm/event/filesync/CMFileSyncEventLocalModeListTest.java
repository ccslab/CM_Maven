package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class CMFileSyncEventLocalModeListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventLocalModeListTest.marshallUnmarshall() called..");
        CMFileSyncEventLocalModeList listEvent = new CMFileSyncEventLocalModeList();
        listEvent.setRequester("ccslab");
        List<Path> pathList = List.of(Path.of("test1.txt"), Path.of("test2.txt"));
        listEvent.setRelativePathList(pathList);
        System.out.println("localModeListEvent = " + listEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(listEvent);
        CMFileSyncEventLocalModeList unmarhsallEvent =
                (CMFileSyncEventLocalModeList) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals(listEvent, unmarhsallEvent);
    }

    @Test(expected=NullPointerException.class)
    public void marshallException() {
        System.out.println("===== CMFileSyncEventLocalModeListTest.marshallException() called..");
        CMFileSyncEventLocalModeList listEvent = new CMFileSyncEventLocalModeList();
        listEvent.setRequester("ccslab");

        ByteBuffer buffer = CMEventManager.marshallEvent(listEvent);
    }
}