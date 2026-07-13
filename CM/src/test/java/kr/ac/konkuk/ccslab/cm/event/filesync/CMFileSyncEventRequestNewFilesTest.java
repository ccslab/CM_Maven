package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventRequestNewFilesTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventRequestNewFilesTest.marshallUnmarshall() called..");
        CMFileSyncEventRequestNewFiles fse = new CMFileSyncEventRequestNewFiles();
        fse.setInitiatorName("ccslab");
        fse.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fse.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fse.setNumRequestedFiles(5);
        // requested list is null
        fse.setRequestedFileList(null);
        System.out.println("fse = " + fse);

        // the case that the requested file list is null
        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEventRequestNewFiles unmarshallEvent =
                (CMFileSyncEventRequestNewFiles) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(5, unmarshallEvent.getNumRequestedFiles());
        List<Path> unmarshallRequestedFileList;
        assertNull(unmarshallEvent.getRequestedFileList());

        // the case that the requested file list exists
        List<Path> requestedFileList = new ArrayList<>();
        requestedFileList.add(Paths.get("testFile1.txt"));
        requestedFileList.add(Paths.get("subdir/testFile2.txt"));
        fse.setRequestedFileList(requestedFileList);
        System.out.println("fse = " + fse);

        byteBuffer = CMEventManager.marshallEvent(fse);
        unmarshallEvent = (CMFileSyncEventRequestNewFiles) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());
        assertEquals(5, unmarshallEvent.getNumRequestedFiles());
        unmarshallRequestedFileList = unmarshallEvent.getRequestedFileList();
        assertEquals(Paths.get("testFile1.txt"), unmarshallRequestedFileList.get(0));
        assertEquals(Paths.get("subdir/testFile2.txt"), unmarshallRequestedFileList.get(1));

        assertEquals(fse, unmarshallEvent);
    }
}
