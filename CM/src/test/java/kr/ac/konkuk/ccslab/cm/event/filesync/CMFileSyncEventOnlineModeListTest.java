package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class CMFileSyncEventOnlineModeListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventOnlineModeListTest.marshallUnmarshall() called..");
        CMFileSyncEventOnlineModeList onlineModeListEvent = new CMFileSyncEventOnlineModeList();
        onlineModeListEvent.setRequester("ccslab");
        List<Path> pathList = List.of(Path.of("test1.txt"), Path.of("test2.txt"));
        onlineModeListEvent.setRelativePathList(pathList);
        System.out.println("onlineModeListEvent = " + onlineModeListEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(onlineModeListEvent);
        CMFileSyncEventOnlineModeList unmarhsallEvent =
                (CMFileSyncEventOnlineModeList) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals(onlineModeListEvent, unmarhsallEvent);
    }
}