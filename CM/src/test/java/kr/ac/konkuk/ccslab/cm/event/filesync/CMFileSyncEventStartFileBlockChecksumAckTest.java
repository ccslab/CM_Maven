package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventStartFileBlockChecksumAckTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventStartFileBlockChecksumAckTest.marshallUnmarshall() called..");
        CMFileSyncEventStartFileBlockChecksumAck fse = new CMFileSyncEventStartFileBlockChecksumAck();
        fse.setInitiatorName("ccslab");
        fse.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fse.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fse.setBlockSize(700);
        fse.setFileEntryIndex(3);
        fse.setTotalNumBlocks(11);
        fse.setReturnCode(1);
        System.out.println("fse before marshalling = " + fse);

        ByteBuffer msg = CMEventManager.marshallEvent(fse);
        CMFileSyncEventStartFileBlockChecksumAck fse2 =
                (CMFileSyncEventStartFileBlockChecksumAck) CMEventManager.unmarshallEvent(msg);
        System.out.println("fse2 after unmarshalling = " + fse2);
        assertNotNull(fse2);
        assertEquals("ccslab", fse2.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), fse2.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), fse2.getInitiatorDeviceUuid());
        assertEquals(fse.getBlockSize(), fse2.getBlockSize());
        assertEquals(fse.getFileEntryIndex(), fse2.getFileEntryIndex());
        assertEquals(fse.getTotalNumBlocks(), fse2.getTotalNumBlocks());
        assertEquals(fse.getReturnCode(), fse2.getReturnCode());

        assertEquals(fse, fse2);
    }

}
