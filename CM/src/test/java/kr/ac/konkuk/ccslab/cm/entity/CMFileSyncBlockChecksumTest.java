package kr.ac.konkuk.ccslab.cm.entity;

import org.junit.Test;

import static org.junit.Assert.*;

public class CMFileSyncBlockChecksumTest {

    @Test
    public void testEquals() {
        System.out.println("CMFileSyncBlockChecksumTest.testEquals() called..");
        CMFileSyncBlockChecksum chksum1 = new CMFileSyncBlockChecksum();
        chksum1.setBlockIndex(0);
        chksum1.setWeakChecksum(1);
        byte[] bytes = new byte[5];
        for(int i = 0; i < bytes.length; i++)
            bytes[i] = (byte)i;
        chksum1.setStrongChecksum(bytes);
        System.out.println("chksum1 = " + chksum1);

        CMFileSyncBlockChecksum chksum2 = new CMFileSyncBlockChecksum();
        chksum2.setBlockIndex(0);
        chksum2.setWeakChecksum(1);
        byte[] bytes2 = new byte[5];
        for(int i = 0; i < bytes2.length; i++)
            bytes2[i] = (byte)(i);
        chksum2.setStrongChecksum(bytes2);
        System.out.println("chksum2 = " + chksum2);

        assertArrayEquals(bytes, bytes2);
        assertEquals(chksum1, chksum2);
    }
}