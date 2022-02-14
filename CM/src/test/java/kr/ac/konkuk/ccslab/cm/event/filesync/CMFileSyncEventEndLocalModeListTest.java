package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventEndLocalModeListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndLocalModeTest.marshallUnmarshall() called..");
        CMFileSyncEventEndLocalModeList endEvent = new CMFileSyncEventEndLocalModeList();
        endEvent.setRequester("ccslab");
        endEvent.setNumLocalModeFiles(2);
        System.out.println("endEvent = " + endEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(endEvent);
        CMFileSyncEventEndLocalModeList unmarhsallEvent =
                (CMFileSyncEventEndLocalModeList) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals(endEvent, unmarhsallEvent);
    }
}