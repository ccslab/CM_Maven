package kr.ac.konkuk.ccslab.cm.info;

import com.fasterxml.jackson.core.JsonProcessingException;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncChangeLogEntry;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Tests for {@link CMFileSyncInfo#readChangeLogEntries(String, long, long)}.
 * <p>
 * The reader resolves changelog files at the CWD-relative path
 * {@code .cm-settings/file-sync/server/{initiatorName}/changelog-*.jsonl}, so the
 * fixtures are written under a unique test-only user dir (gitignored) and removed afterward.
 */
public class CMFileSyncInfoReadChangeLogEntriesTest {

    private static final String TEST_USER = "__cmtest_readchangelog_user__";
    private static final UUID DEVICE = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private CMFileSyncInfo syncInfo;
    private Path testUserDir;

    @Before
    public void setUp() throws IOException {
        syncInfo = CMFileSyncInfo.getInstance();
        testUserDir = Path.of(".cm-settings", "file-sync", "server", TEST_USER);
        deleteRecursively(testUserDir);
        Files.createDirectories(testUserDir);

        // file 1: changeIds 1..3, "a.txt" appears twice (CREATE then MODIFY)
        writeChangelog("changelog-2026-05-28.jsonl",
                entry(1, CMFileSyncOp.CREATE, "a.txt", 100),
                entry(2, CMFileSyncOp.CREATE, "b.txt", 200),
                entry(3, CMFileSyncOp.MODIFY, "a.txt", 150));

        // file 2: changeIds 4..5, with a trailing blank line that must be skipped
        Path file2 = writeChangelog("changelog-2026-05-29.jsonl",
                entry(4, CMFileSyncOp.DELETE, "b.txt", 0),
                entry(5, CMFileSyncOp.CREATE, "c.txt", 300));
        Files.writeString(file2, System.lineSeparator(), StandardOpenOption.APPEND);

        // non-matching file that must be ignored by the changelog-*.jsonl glob
        Files.writeString(testUserDir.resolve("notes.txt"), "not a changelog");
    }

    @After
    public void tearDown() throws IOException {
        deleteRecursively(testUserDir);
    }

    @Test
    public void testReadsRangeSortedAndAggregatesAcrossFiles() {
        // (1, 4] -> ids 2, 3, 4 ; spans both files ; from-exclusive, to-inclusive
        List<CMFileSyncChangeLogEntry> result = syncInfo.readChangeLogEntries(TEST_USER, 1, 4);

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).getChangeId());
        assertEquals(3, result.get(1).getChangeId());
        assertEquals(4, result.get(2).getChangeId());
    }

    @Test
    public void testFromIsExclusiveAndToIsInclusive() {
        // (2, 3] -> only id 3
        List<CMFileSyncChangeLogEntry> result = syncInfo.readChangeLogEntries(TEST_USER, 2, 3);

        assertEquals(1, result.size());
        CMFileSyncChangeLogEntry only = result.get(0);
        assertEquals(3, only.getChangeId());
        assertEquals("a.txt", only.getPath());
        assertEquals(CMFileSyncOp.MODIFY, only.getOp());
    }

    @Test
    public void testReturnsEveryEntryInRangeWithoutDedup() {
        // (0, 5] -> all 5 entries, sorted ascending; "a.txt" appears at both id 1 and id 3
        List<CMFileSyncChangeLogEntry> result = syncInfo.readChangeLogEntries(TEST_USER, 0, 5);

        assertEquals(5, result.size());
        for (int i = 0; i < result.size(); i++) {
            assertEquals(i + 1, result.get(i).getChangeId());
        }
        long aTxtCount = result.stream().filter(e -> "a.txt".equals(e.getPath())).count();
        assertEquals("reader must not dedup by path; dedup happens in startServerEntryList", 2, aTxtCount);
    }

    @Test
    public void testEmptyWhenNoEntriesInRange() {
        // (5, 10] -> nothing (max id is 5, and 5 is excluded)
        List<CMFileSyncChangeLogEntry> result = syncInfo.readChangeLogEntries(TEST_USER, 5, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEmptyWhenChangelogDirDoesNotExist() {
        List<CMFileSyncChangeLogEntry> result =
                syncInfo.readChangeLogEntries("__cmtest_nonexistent_user__", 0, 100);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---- helpers ----

    private CMFileSyncChangeLogEntry entry(long changeId, CMFileSyncOp op, String path, long size) {
        return new CMFileSyncChangeLogEntry()
                .setChangeId(changeId)
                .setUserName(TEST_USER)
                .setOriginDeviceUuid(DEVICE)
                .setOp(op)
                .setPath(path)
                .setMtime(1_700_000_000 + changeId)
                .setSize(size)
                .setTombstone(op == CMFileSyncOp.DELETE);
    }

    private Path writeChangelog(String fileName, CMFileSyncChangeLogEntry... entries) throws IOException {
        Path file = testUserDir.resolve(fileName);
        StringBuilder sb = new StringBuilder();
        for (CMFileSyncChangeLogEntry e : entries) {
            try {
                sb.append(e.toJsonString()).append(System.lineSeparator());
            } catch (JsonProcessingException ex) {
                throw new IOException(ex);
            }
        }
        Files.writeString(file, sb.toString());
        return file;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignore) {
                }
            });
        }
    }
}
