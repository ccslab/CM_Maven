package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncChangeLogEntry;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventServerEntriesTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventServerEntriesTest.marshallUnmarshall() called..");
        CMFileSyncEventServerEntries fsEvent = new CMFileSyncEventServerEntries();
        fsEvent.setInitiatorName("ccslab");
        fsEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fsEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fsEvent.setNumFilesCompleted(3);
        fsEvent.setNumFiles(2);
        System.out.println("fsEvent = " + fsEvent);

        // null list -> null after roundtrip
        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventServerEntries unmarshallEvent =
                (CMFileSyncEventServerEntries) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        assertEquals(3, unmarshallEvent.getNumFilesCompleted());
        assertEquals(2, unmarshallEvent.getNumFiles());
        assertNull(unmarshallEvent.getServerEntryList());

        // entry with all fields populated (CREATE)
        CMFileSyncChangeLogEntry entry1 = new CMFileSyncChangeLogEntry()
                .setChangeId(10L)
                .setUserName("ccslab")
                .setOriginDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000099"))
                .setOp(CMFileSyncOp.CREATE)
                .setPath("dir/file1.txt")
                .setDirectory(false)
                .setContentHash("abc123")
                .setMtime(1700000000L)
                .setSize(2048L)
                .setTombstone(false)
                .setTs(OffsetDateTime.parse("2026-05-18T15:24:15+09:00"));

        // entry with nullable fields left null (DELETE / tombstone)
        CMFileSyncChangeLogEntry entry2 = new CMFileSyncChangeLogEntry()
                .setChangeId(11L)
                .setUserName("ccslab")
                .setOp(CMFileSyncOp.DELETE)
                .setPath("dir/file2.txt")
                .setDirectory(false)
                .setTombstone(true);

        List<CMFileSyncChangeLogEntry> entryList = new ArrayList<>();
        entryList.add(entry1);
        entryList.add(entry2);
        fsEvent.setServerEntryList(entryList);
        System.out.println("fsEvent = " + fsEvent);

        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEventServerEntries) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        List<CMFileSyncChangeLogEntry> unmarshallList = unmarshallEvent.getServerEntryList();
        assertNotNull(unmarshallList);
        assertEquals(2, unmarshallList.size());

        CMFileSyncChangeLogEntry u1 = unmarshallList.get(0);
        assertEquals(10L, u1.getChangeId());
        assertEquals("ccslab", u1.getUserName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000099"), u1.getOriginDeviceUuid());
        assertEquals(CMFileSyncOp.CREATE, u1.getOp());
        assertEquals("dir/file1.txt", u1.getPath());
        assertFalse(u1.isDirectory());
        assertEquals("abc123", u1.getContentHash());
        assertEquals(1700000000L, u1.getMtime());
        assertEquals(2048L, u1.getSize());
        assertFalse(u1.isTombstone());
        assertEquals(OffsetDateTime.parse("2026-05-18T15:24:15+09:00"), u1.getTs());

        CMFileSyncChangeLogEntry u2 = unmarshallList.get(1);
        assertEquals(11L, u2.getChangeId());
        assertEquals("ccslab", u2.getUserName());
        assertNull(u2.getOriginDeviceUuid());
        assertEquals(CMFileSyncOp.DELETE, u2.getOp());
        assertEquals("dir/file2.txt", u2.getPath());
        assertNull(u2.getContentHash());
        assertTrue(u2.isTombstone());
        assertNull(u2.getTs());

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());

        assertEquals(fsEvent, unmarshallEvent);
    }
}
