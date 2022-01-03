package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventEndFileBlockChecksumAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndFileBlockChecksumAckTest.marshallUnmarshall() called..");

        byte[] bytes = {1,2,3,4,5,6,7};
        CMFileSyncEventEndFileBlockChecksumAck fse = new CMFileSyncEventEndFileBlockChecksumAck();
        fse.setFileChecksum(bytes);
        fse.setBlockSize(700);
        fse.setFileEntryIndex(5);
        fse.setTotalNumBlocks(100);
        fse.setReturnCode(1);

        ByteBuffer msg = CMEventManager.marshallEvent(fse);
        System.out.println("fse after marshalling = " + fse);
        CMFileSyncEventEndFileBlockChecksumAck fse2 =
                (CMFileSyncEventEndFileBlockChecksumAck) CMEventManager.unmarshallEvent(msg);
        System.out.println("fse2 after unmarshalling = " + fse2);

        assertNotNull(fse2);
        assertEquals(fse.getBlockSize(), fse2.getBlockSize());
        assertArrayEquals(fse.getFileChecksum(), fse2.getFileChecksum());
        assertEquals(fse.getFileEntryIndex(), fse2.getFileEntryIndex());
        assertEquals(fse.getTotalNumBlocks(), fse2.getTotalNumBlocks());
        assertEquals(fse.getReturnCode(), fse2.getReturnCode());

        assertEquals(fse, fse2);
    }

}