package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class CMFileSyncEventEndOnlineModeTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndOnlineModeTest.marshallUnmarshall() called..");
        CMFileSyncEventEndOnlineMode endEvent = new CMFileSyncEventEndOnlineMode();
        endEvent.setRequester("ccslab");
        endEvent.setNumOnlineModeFiles(2);
        System.out.println("endEvent = " + endEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(endEvent);
        CMFileSyncEventEndOnlineMode unmarhsallEvent =
                (CMFileSyncEventEndOnlineMode) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals(endEvent, unmarhsallEvent);
    }
}