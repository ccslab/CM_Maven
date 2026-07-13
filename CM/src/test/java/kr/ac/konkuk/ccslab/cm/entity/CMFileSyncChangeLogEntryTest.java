package kr.ac.konkuk.ccslab.cm.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncChangeLogEntryTest {

    private CMFileSyncChangeLogEntry entry;
    private final UUID testUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private final OffsetDateTime testTs = OffsetDateTime.of(2026, 4, 4, 15, 30, 0, 0, ZoneOffset.ofHours(9));

    @Before
    public void setUp() {
        entry = new CMFileSyncChangeLogEntry()
                .setChangeId(1)
                .setUserName("ccslab")
                .setOriginDeviceUuid(testUuid)
                .setOp(CMFileSyncOp.CREATE)
                .setPath("docs/readme.txt")
                .setDirectory(false)
                .setContentHash("d41d8cd98f00b204e9800998ecf8427e")
                .setMtime(1711814400)
                .setSize(1024)
                .setTombstone(false)
                .setTs(testTs);
    }

    @Test
    public void testDefaultConstructor() {
        CMFileSyncChangeLogEntry defaultEntry = new CMFileSyncChangeLogEntry();
        assertEquals(0, defaultEntry.getChangeId());
        assertNull(defaultEntry.getUserName());
        assertNull(defaultEntry.getOriginDeviceUuid());
        assertEquals(CMFileSyncOp.UNKNOWN, defaultEntry.getOp());
        assertNull(defaultEntry.getPath());
        assertFalse(defaultEntry.isDirectory());
        assertNull(defaultEntry.getContentHash());
        assertEquals(0, defaultEntry.getMtime());
        assertEquals(0, defaultEntry.getSize());
        assertFalse(defaultEntry.isTombstone());
        assertNull(defaultEntry.getTs());
    }

    @Test
    public void testFluentSettersAndGetters() {
        assertEquals(1, entry.getChangeId());
        assertEquals("ccslab", entry.getUserName());
        assertEquals(testUuid, entry.getOriginDeviceUuid());
        assertEquals(CMFileSyncOp.CREATE, entry.getOp());
        assertEquals("docs/readme.txt", entry.getPath());
        assertFalse(entry.isDirectory());
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", entry.getContentHash());
        assertEquals(1711814400, entry.getMtime());
        assertEquals(1024, entry.getSize());
        assertFalse(entry.isTombstone());
        assertEquals(testTs, entry.getTs());
    }

    @Test
    public void testFluentSetterReturnType() {
        CMFileSyncChangeLogEntry result = new CMFileSyncChangeLogEntry()
                .setChangeId(1)
                .setUserName("user")
                .setOp(CMFileSyncOp.MODIFY);
        assertNotNull(result);
        assertEquals(1, result.getChangeId());
    }

    @Test
    public void testEqualsSameChangeIdAndUserName() {
        CMFileSyncChangeLogEntry other = new CMFileSyncChangeLogEntry()
                .setChangeId(1)
                .setUserName("ccslab")
                .setOp(CMFileSyncOp.MODIFY)
                .setPath("other/path.txt")
                .setSize(9999);
        assertEquals(entry, other);
    }

    @Test
    public void testEqualsDifferentChangeId() {
        CMFileSyncChangeLogEntry other = new CMFileSyncChangeLogEntry()
                .setChangeId(2)
                .setUserName("ccslab");
        assertNotEquals(entry, other);
    }

    @Test
    public void testEqualsDifferentUserName() {
        CMFileSyncChangeLogEntry other = new CMFileSyncChangeLogEntry()
                .setChangeId(1)
                .setUserName("other");
        assertNotEquals(entry, other);
    }

    @Test
    public void testEqualsNull() {
        assertNotEquals(null, entry);
    }

    @Test
    public void testEqualsSameInstance() {
        assertEquals(entry, entry);
    }

    @Test
    public void testEqualsDifferentType() {
        assertNotEquals("not an entry", entry);
    }

    @Test
    public void testHashCodeConsistentWithEquals() {
        CMFileSyncChangeLogEntry other = new CMFileSyncChangeLogEntry()
                .setChangeId(1)
                .setUserName("ccslab");
        assertEquals(entry.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCodeDifferent() {
        CMFileSyncChangeLogEntry other = new CMFileSyncChangeLogEntry()
                .setChangeId(2)
                .setUserName("ccslab");
        assertNotEquals(entry.hashCode(), other.hashCode());
    }

    @Test
    public void testCompareToAscending() {
        CMFileSyncChangeLogEntry entry1 = new CMFileSyncChangeLogEntry().setChangeId(1);
        CMFileSyncChangeLogEntry entry2 = new CMFileSyncChangeLogEntry().setChangeId(3);
        CMFileSyncChangeLogEntry entry3 = new CMFileSyncChangeLogEntry().setChangeId(2);

        List<CMFileSyncChangeLogEntry> list = new ArrayList<>();
        list.add(entry2);
        list.add(entry3);
        list.add(entry1);
        Collections.sort(list);

        assertEquals(1, list.get(0).getChangeId());
        assertEquals(2, list.get(1).getChangeId());
        assertEquals(3, list.get(2).getChangeId());
    }

    @Test
    public void testCompareToEqual() {
        CMFileSyncChangeLogEntry other = new CMFileSyncChangeLogEntry().setChangeId(1);
        assertEquals(0, entry.compareTo(other));
    }

    @Test
    public void testToString() {
        String str = entry.toString();
        assertTrue(str.contains("CMFileSyncChangeLogEntry{"));
        assertTrue(str.contains("changeId=1"));
        assertTrue(str.contains("userName='ccslab'"));
        assertTrue(str.contains("op=CREATE"));
        assertTrue(str.contains("path='docs/readme.txt'"));
    }

    @Test
    public void testToJsonString() throws JsonProcessingException {
        String json = entry.toJsonString();
        assertTrue(json.contains("\"changeId\":1"));
        assertTrue(json.contains("\"userName\":\"ccslab\""));
        assertTrue(json.contains("\"originDeviceUuid\":\"" + testUuid + "\""));
        assertTrue(json.contains("\"op\":\"CREATE\""));
        assertTrue(json.contains("\"path\":\"docs/readme.txt\""));
        assertTrue(json.contains("\"isDirectory\":false"));
        assertTrue(json.contains("\"contentHash\":\"d41d8cd98f00b204e9800998ecf8427e\""));
        assertTrue(json.contains("\"mtime\":1711814400"));
        assertTrue(json.contains("\"size\":1024"));
        assertTrue(json.contains("\"tombstone\":false"));
        assertTrue(json.contains("\"ts\":\"" + testTs + "\""));
    }

    @Test
    public void testFromJsonString() throws JsonProcessingException {
        String json = entry.toJsonString();
        CMFileSyncChangeLogEntry parsed = CMFileSyncChangeLogEntry.fromJsonString(json);

        assertEquals(entry.getChangeId(), parsed.getChangeId());
        assertEquals(entry.getUserName(), parsed.getUserName());
        assertEquals(entry.getOriginDeviceUuid(), parsed.getOriginDeviceUuid());
        assertEquals(entry.getOp(), parsed.getOp());
        assertEquals(entry.getPath(), parsed.getPath());
        assertEquals(entry.isDirectory(), parsed.isDirectory());
        assertEquals(entry.getContentHash(), parsed.getContentHash());
        assertEquals(entry.getMtime(), parsed.getMtime());
        assertEquals(entry.getSize(), parsed.getSize());
        assertEquals(entry.isTombstone(), parsed.isTombstone());
        assertEquals(entry.getTs(), parsed.getTs());
    }

    @Test
    public void testFromJsonStringDeleteOp() throws JsonProcessingException {
        CMFileSyncChangeLogEntry deleteEntry = new CMFileSyncChangeLogEntry()
                .setChangeId(5)
                .setUserName("ccslab")
                .setOriginDeviceUuid(testUuid)
                .setOp(CMFileSyncOp.DELETE)
                .setPath("old/file.txt")
                .setDirectory(false)
                .setContentHash(null)
                .setMtime(1711814400)
                .setSize(0)
                .setTombstone(true)
                .setTs(testTs);

        String json = deleteEntry.toJsonString();
        CMFileSyncChangeLogEntry parsed = CMFileSyncChangeLogEntry.fromJsonString(json);

        assertEquals(CMFileSyncOp.DELETE, parsed.getOp());
        assertNull(parsed.getContentHash());
        assertTrue(parsed.isTombstone());
        assertEquals(0, parsed.getSize());
    }

    @Test
    public void testRoundTripJsonDirectoryEntry() throws JsonProcessingException {
        CMFileSyncChangeLogEntry dirEntry = new CMFileSyncChangeLogEntry()
                .setChangeId(10)
                .setUserName("ccslab")
                .setOriginDeviceUuid(testUuid)
                .setOp(CMFileSyncOp.CREATE)
                .setPath("new/folder")
                .setDirectory(true)
                .setContentHash(null)
                .setMtime(1711900000)
                .setSize(0)
                .setTombstone(false)
                .setTs(testTs);

        String json = dirEntry.toJsonString();
        CMFileSyncChangeLogEntry parsed = CMFileSyncChangeLogEntry.fromJsonString(json);

        assertTrue(parsed.isDirectory());
        assertEquals("new/folder", parsed.getPath());
    }
}
