package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CMFileSyncEventFileEntriesAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventFileEntriesAckTest.marshallUnmarshall() called..");
        CMFileSyncEventFileEntriesAck fsEvent = new CMFileSyncEventFileEntriesAck();
        fsEvent.setID(CMFileSyncEvent.FILE_ENTRIES_ACK);
        fsEvent.setUserName("ccslab");
        fsEvent.setNumFilesCompleted(5);
        fsEvent.setNumFiles(11);
        fsEvent.setReturnCode(1);

        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventFileEntriesAck unmarshallEvent =
                (CMFileSyncEventFileEntriesAck) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 5);
        int numFiles = unmarshallEvent.getNumFiles();
        assertEquals(numFiles, 11);
        int returnCode = unmarshallEvent.getReturnCode();
        assertEquals(returnCode, 1);

        assertEquals(fsEvent, unmarshallEvent);
    }
}