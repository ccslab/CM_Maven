package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class CMFileSyncEventCompleteUpdateFileTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventCompleteUpdateFileTest.marshallUnmarshall() called..");
        CMFileSyncEventCompleteUpdateFile fse = new CMFileSyncEventCompleteUpdateFile();
        fse.setUserName("ccslab");
        fse.setCompletedPath(Paths.get("test1.txt"));
        System.out.println("fse = " + fse);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEventCompleteUpdateFile unmarshallEvent =
                (CMFileSyncEventCompleteUpdateFile) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        Path completedPath = unmarshallEvent.getCompletedPath();
        assertEquals(completedPath, Paths.get("test1.txt"));

        assertEquals(fse, unmarshallEvent);
    }
}