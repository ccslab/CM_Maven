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

public class CMFileSyncEventFileEntriesTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventFileEntriesTest.marshallUnmarshall() called..");
        CMFileSyncEventFileEntries fsEvent = new CMFileSyncEventFileEntries();
        fsEvent.setUserName("ccslab");
        fsEvent.setNumFilesCompleted(5);
        fsEvent.setNumFiles(11);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventFileEntries unmarshallEvent =
                (CMFileSyncEventFileEntries) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);
        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 5);
        int numFiles = unmarshallEvent.getNumFiles();
        assertEquals(numFiles, 11);
        // List<CMFileSyncEntry> is null
        List<CMFileSyncEntry> entryList = unmarshallEvent.getFileEntryList();
        assertNull(entryList);

        // add an empty List<CMFileSyncEntry>
        entryList = new ArrayList<>();
        fsEvent.setFileEntryList(entryList);
        System.out.println("fsEvent = " + fsEvent);
        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEventFileEntries) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);
        List<CMFileSyncEntry> unmarshallEntryList = unmarshallEvent.getFileEntryList();
        assertNull(unmarshallEntryList);

        // add an List<CMFileSyncEntry> with 2 items
        CMFileSyncEntry entry1 = new CMFileSyncEntry();
        entry1.setPathRelativeToHome(Paths.get("testFile1"));
        entry1.setSize(100);
        entry1.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
        entryList.add(entry1);

        CMFileSyncEntry entry2 = new CMFileSyncEntry();
        entry2.setPathRelativeToHome(Paths.get("testFile2"));
        entry2.setSize(1000000);
        entry2.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
        entryList.add(entry2);

        fsEvent.setFileEntryList(entryList);
        System.out.println("fsEvent = " + fsEvent);

        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEventFileEntries) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);
        unmarshallEntryList = unmarshallEvent.getFileEntryList();

        CMFileSyncEntry unmarshallEntry1 = unmarshallEntryList.get(0);
        Path pathRelativeToHome = unmarshallEntry1.getPathRelativeToHome();
        assertEquals(unmarshallEntry1.getPathRelativeToHome(), entry1.getPathRelativeToHome());
        long size = unmarshallEntry1.getSize();
        assertEquals(unmarshallEntry1.getSize(), entry1.getSize());
        FileTime lastModifiedTime = unmarshallEntry1.getLastModifiedTime();
        assertEquals(unmarshallEntry1.getLastModifiedTime(), entry1.getLastModifiedTime());

        CMFileSyncEntry unmarshallEntry2 = unmarshallEntryList.get(1);
        assertEquals(unmarshallEntry2.getPathRelativeToHome(), entry2.getPathRelativeToHome());
        assertEquals(unmarshallEntry2.getSize(), entry2.getSize());
        assertEquals(unmarshallEntry2.getLastModifiedTime(), entry2.getLastModifiedTime());
    }
}