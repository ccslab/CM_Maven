package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventEndOnlineModeListTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndOnlineModeTest.marshallUnmarshall() called..");
        CMFileSyncEventEndOnlineModeList endEvent = new CMFileSyncEventEndOnlineModeList();
        endEvent.setRequester("ccslab");
        endEvent.setNumOnlineModeFiles(2);
        System.out.println("endEvent = " + endEvent);

        ByteBuffer buffer = CMEventManager.marshallEvent(endEvent);
        CMFileSyncEventEndOnlineModeList unmarhsallEvent =
                (CMFileSyncEventEndOnlineModeList) CMEventManager.unmarshallEvent(buffer);
        assertNotNull(unmarhsallEvent);
        System.out.println("unmarhsallEvent = " + unmarhsallEvent);
        assertEquals(endEvent, unmarhsallEvent);
    }
}