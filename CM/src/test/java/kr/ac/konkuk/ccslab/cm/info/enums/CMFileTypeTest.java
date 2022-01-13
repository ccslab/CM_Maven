package kr.ac.konkuk.ccslab.cm.info.enums;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class CMFileTypeTest {

    @Test
    public void getTypeTest() {
        CMFileType type = CMFileType.getType(Paths.get("."));
        System.out.println("type for . = " + type);
        assertEquals(type, CMFileType.DIR);

        type = CMFileType.getType(Paths.get("test.txt"));   // test.txt does not exist.
        System.out.println("type for test.txt = " + type);
        assertEquals(type, CMFileType.FILE);
    }

}