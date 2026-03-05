package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventSkipUpdateFileTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventSkipUpdateFileTest.marshallUnmarshall() called..");
        CMFileSyncEventSkipUpdateFile fse = new CMFileSyncEventSkipUpdateFile();
        fse.setInitiatorName("ccslab");
        fse.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fse.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fse.setSkippedPath(Paths.get("test1.txt"));
        System.out.println("fse = " + fse);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEventSkipUpdateFile unmarshallEvent =
                (CMFileSyncEventSkipUpdateFile) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());
        Path skippedPath = unmarshallEvent.getSkippedPath();
        assertEquals(Paths.get("test1.txt"), skippedPath);

        assertEquals(fse, unmarshallEvent);
    }
}
