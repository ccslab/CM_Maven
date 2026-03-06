package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventCompleteDeleteFilesTest {

    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventCompleteDeleteFilesTest.marshallUnmarshall() called..");
        CMFileSyncEventCompleteDeleteFiles fse = new CMFileSyncEventCompleteDeleteFiles();
        fse.setInitiatorName("ccslab");
        fse.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fse.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        List<Path> deletedPaths = Arrays.asList(
                Paths.get("deleted1.txt"),
                Paths.get("subdir/deleted2.txt")
        );
        fse.setDeletedPathList(deletedPaths);
        fse.setCursor(42L);
        System.out.println("fse = " + fse);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEventCompleteDeleteFiles unmarshallEvent =
                (CMFileSyncEventCompleteDeleteFiles) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());
        assertEquals(deletedPaths, unmarshallEvent.getDeletedPathList());
        assertEquals(42L, unmarshallEvent.getCursor());

        assertEquals(fse, unmarshallEvent);
    }

    @Test
    public void marshallUnmarshall_emptyList() {
        System.out.println("===== CMFileSyncEventCompleteDeleteFilesTest.marshallUnmarshall_emptyList() called..");
        CMFileSyncEventCompleteDeleteFiles fse = new CMFileSyncEventCompleteDeleteFiles();
        fse.setInitiatorName("ccslab");
        fse.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fse.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fse.setDeletedPathList(null);
        fse.setCursor(0L);
        System.out.println("fse = " + fse);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEventCompleteDeleteFiles unmarshallEvent =
                (CMFileSyncEventCompleteDeleteFiles) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertNull(unmarshallEvent.getDeletedPathList());
        assertEquals(0L, unmarshallEvent.getCursor());
    }
}
