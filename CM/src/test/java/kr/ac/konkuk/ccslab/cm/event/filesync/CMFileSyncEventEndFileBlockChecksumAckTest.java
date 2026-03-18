package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventEndFileBlockChecksumAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventEndFileBlockChecksumAckTest.marshallUnmarshall() called..");

        byte[] bytes = {1,2,3,4,5,6,7};
        CMFileSyncEventEndFileBlockChecksumAck fse = new CMFileSyncEventEndFileBlockChecksumAck();
        fse.setInitiatorName("ccslab");
        fse.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fse.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
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
        assertEquals("ccslab", fse2.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), fse2.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), fse2.getInitiatorDeviceUuid());
        assertEquals(fse.getBlockSize(), fse2.getBlockSize());
        assertArrayEquals(fse.getFileChecksum(), fse2.getFileChecksum());
        assertEquals(fse.getFileEntryIndex(), fse2.getFileEntryIndex());
        assertEquals(fse.getTotalNumBlocks(), fse2.getTotalNumBlocks());
        assertEquals(fse.getReturnCode(), fse2.getReturnCode());

        assertEquals(fse, fse2);
    }

}
