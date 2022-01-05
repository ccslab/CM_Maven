package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class CMFileSyncEventSkipUpdateFileTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventSkipUpdateFileTest.marshallUnmarshall() called..");
        CMFileSyncEventSkipUpdateFile fse = new CMFileSyncEventSkipUpdateFile();
        fse.setUserName("ccslab");
        fse.setSkippedPath(Paths.get("test1.txt"));
        System.out.println("fse = " + fse);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEventSkipUpdateFile unmarshallEvent =
                (CMFileSyncEventSkipUpdateFile) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        Path skippedPath = unmarshallEvent.getSkippedPath();
        assertEquals(skippedPath, Paths.get("test1.txt"));

        assertEquals(fse, unmarshallEvent);
    }
}