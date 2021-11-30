package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventFileBlockChecksumTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventFileBlockChecksumTest.marshallUnmarshall() called..");
        CMFileSyncEventFileBlockChecksum fse = new CMFileSyncEventFileBlockChecksum();
        fse.setFileEntryIndex(1);
        fse.setStartBlockIndex(0);
        fse.setNumCurrentBlocks(0); // array is null..
        fse.setTotalNumBlocks(10);
        // checksum array is null..
        ByteBuffer msg = CMEventManager.marshallEvent(fse);
        System.out.println("fse after marshalling = " + fse);

        CMFileSyncEventFileBlockChecksum fse2 = (CMFileSyncEventFileBlockChecksum) CMEventManager.unmarshallEvent(msg);
        System.out.println("fse2 after unmarshalling = " + fse2);
        assertNotNull(fse2);
        assertEquals(fse.getFileEntryIndex(), fse2.getFileEntryIndex());
        assertEquals(fse.getNumCurrentBlocks(), fse2.getNumCurrentBlocks());
        assertEquals(fse.getStartBlockIndex(), fse2.getStartBlockIndex());
        assertEquals(fse.getTotalNumBlocks(), fse2.getTotalNumBlocks());

        // checksum array is not null..
        CMFileSyncBlockChecksum[] checksumArray = new CMFileSyncBlockChecksum[2];
        checksumArray[0] = new CMFileSyncBlockChecksum();
        checksumArray[0].setWeakChecksum(1);
        checksumArray[0].setBlockIndex(5);
        byte[] bytes = new byte[5];
        for(int i = 0; i < bytes.length; i++)
            bytes[i] = (byte)i;
        checksumArray[0].setStrongChecksum(bytes);

        checksumArray[1] = new CMFileSyncBlockChecksum();
        checksumArray[1].setWeakChecksum(2);
        checksumArray[1].setBlockIndex(6);
        byte[] bytes2 = new byte[5];
        for(int i = 0; i < bytes2.length; i++)
            bytes2[i] = (byte)(i*i);
        checksumArray[1].setStrongChecksum(bytes2);

        fse.setNumCurrentBlocks(2);
        fse.setChecksumArray(checksumArray);

        msg = CMEventManager.marshallEvent(fse);
        System.out.println("fse after marshalling = " + fse);
        fse2 = (CMFileSyncEventFileBlockChecksum) CMEventManager.unmarshallEvent(msg);
        System.out.println("fse2 after unmarshalling = " + fse2);
        assertNotNull(fse2);
        assertEquals(fse.getNumCurrentBlocks(), fse2.getNumCurrentBlocks());
        assertArrayEquals(fse.getChecksumArray(), fse2.getChecksumArray());

        assertEquals(fse, fse2);
    }
}