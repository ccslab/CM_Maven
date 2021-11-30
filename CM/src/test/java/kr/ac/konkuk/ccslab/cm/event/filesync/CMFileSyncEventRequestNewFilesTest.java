package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CMFileSyncEventRequestNewFilesTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventRequestNewFilesTest.marshallUnmarshall() called..");
        CMFileSyncEventRequestNewFiles fse = new CMFileSyncEventRequestNewFiles();
        fse.setRequesterName("ccslab");
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

        String requesterName = unmarshallEvent.getRequesterName();
        assertEquals(requesterName, "ccslab");
        int numRequestedFiles = unmarshallEvent.getNumRequestedFiles();
        assertEquals(numRequestedFiles, 5);
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

        requesterName = unmarshallEvent.getRequesterName();
        assertEquals(requesterName, "ccslab");
        numRequestedFiles = unmarshallEvent.getNumRequestedFiles();
        assertEquals(numRequestedFiles, 5);
        unmarshallRequestedFileList = unmarshallEvent.getRequestedFileList();
        assertEquals(unmarshallRequestedFileList.get(0), Paths.get("testFile1.txt"));
        assertEquals(unmarshallRequestedFileList.get(1), Paths.get("subdir/testFile2.txt"));

        assertEquals(fse, unmarshallEvent);
    }
}