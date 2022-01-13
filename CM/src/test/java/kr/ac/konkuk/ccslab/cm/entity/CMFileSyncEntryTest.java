package kr.ac.konkuk.ccslab.cm.entity;

import kr.ac.konkuk.ccslab.cm.info.enums.CMFileType;
import org.junit.Test;

import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import static org.junit.Assert.*;

public class CMFileSyncEntryTest {

    @Test
    public void testEquals() {
        CMFileSyncEntry entry1 = new CMFileSyncEntry();
        entry1.setType(CMFileType.FILE).setLastModifiedTime(FileTime.fromMillis(1000))
                .setPathRelativeToHome(Paths.get("test.txt")).setSize(1111);
        System.out.println("entry1 = " + entry1);

        CMFileSyncEntry entry2 = new CMFileSyncEntry();
        entry2.setType(CMFileType.FILE).setLastModifiedTime(FileTime.fromMillis(1000))
                .setPathRelativeToHome(Paths.get("test.txt")).setSize(1111);
        System.out.println("entry2 = " + entry2);

        assertEquals(entry1, entry2);

    }

}