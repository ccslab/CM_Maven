package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileType;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventFileEntriesTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventFileEntriesTest.marshallUnmarshall() called..");
        CMFileSyncEventFileEntries fsEvent = new CMFileSyncEventFileEntries();
        fsEvent.setInitiatorName("ccslab");
        fsEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fsEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fsEvent.setNumFilesCompleted(5);
        fsEvent.setNumFiles(11);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventFileEntries unmarshallEvent =
                (CMFileSyncEventFileEntries) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);
        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(5, unmarshallEvent.getNumFilesCompleted());
        assertEquals(11, unmarshallEvent.getNumFiles());
        // List<CMFileSyncEntry> is null
        List<CMFileSyncEntry> entryList = unmarshallEvent.getInitiatorPathEntryList();
        assertNull(entryList);

        // add an empty List<CMFileSyncEntry>
        entryList = new ArrayList<>();
        fsEvent.setInitiatorPathEntryList(entryList);
        System.out.println("fsEvent = " + fsEvent);
        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEventFileEntries) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);
        List<CMFileSyncEntry> unmarshallEntryList = unmarshallEvent.getInitiatorPathEntryList();
        assertNull(unmarshallEntryList);

        // add a List<CMFileSyncEntry> with 2 items
        CMFileSyncEntry entry1 = new CMFileSyncEntry();
        entry1.setPathRelativeToHome(Paths.get("testFile1"));
        entry1.setSize(100);
        entry1.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
        entry1.setType(CMFileType.FILE);
        entryList.add(entry1);

        CMFileSyncEntry entry2 = new CMFileSyncEntry();
        entry2.setPathRelativeToHome(Paths.get("testFile2"));
        entry2.setSize(1000000);
        entry2.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
        entry2.setType(CMFileType.DIR);
        entryList.add(entry2);

        fsEvent.setInitiatorPathEntryList(entryList);
        System.out.println("fsEvent = " + fsEvent);

        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEventFileEntries) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);
        unmarshallEntryList = unmarshallEvent.getInitiatorPathEntryList();

        CMFileSyncEntry unmarshallEntry1 = unmarshallEntryList.get(0);
        assertEquals(entry1.getPathRelativeToHome(), unmarshallEntry1.getPathRelativeToHome());
        assertEquals(entry1.getSize(), unmarshallEntry1.getSize());
        assertEquals(entry1.getLastModifiedTime(), unmarshallEntry1.getLastModifiedTime());
        assertEquals(entry1, unmarshallEntry1);

        CMFileSyncEntry unmarshallEntry2 = unmarshallEntryList.get(1);
        assertEquals(entry2.getPathRelativeToHome(), unmarshallEntry2.getPathRelativeToHome());
        assertEquals(entry2.getSize(), unmarshallEntry2.getSize());
        assertEquals(entry2.getLastModifiedTime(), unmarshallEntry2.getLastModifiedTime());
        assertEquals(entry2, unmarshallEntry2);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());

        assertEquals(fsEvent, unmarshallEvent);
    }
}
