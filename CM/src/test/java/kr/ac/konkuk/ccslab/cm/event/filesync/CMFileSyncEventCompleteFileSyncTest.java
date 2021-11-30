package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventCompleteFileSyncTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventCompleteFileSyncTest.marshallUnmarshall() called..");
        CMFileSyncEventCompleteFileSync fse = new CMFileSyncEventCompleteFileSync();
        fse.setUserName("ccslab");
        fse.setNumFilesCompleted(25);
        System.out.println("fse = " + fse);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEventCompleteFileSync unmarshallEvent =
                (CMFileSyncEventCompleteFileSync) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 25);

        assertEquals(fse, unmarshallEvent);
    }
}