package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventStartFileBlockChecksumTest {

    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFilesSyncEventStartFileBlockChecksumTest.marshallUnmarshall() called..");
        CMFileSyncEventStartFileBlockChecksum fse = new CMFileSyncEventStartFileBlockChecksum();
        fse.setSender("server");
        fse.setReceiver("ccslab");
        fse.setBlockSize(700);
        fse.setFileEntryIndex(0);
        fse.setTotalNumBlocks(10);
        System.out.println("fse before marshalling = " + fse);

        ByteBuffer msg = CMEventManager.marshallEvent(fse);
        CMFileSyncEventStartFileBlockChecksum fse2 =
                (CMFileSyncEventStartFileBlockChecksum) CMEventManager.unmarshallEvent(msg);
        System.out.println("fse2 after unmarshalling = " + fse2);

        assertNotNull(fse2);
        assertEquals(fse.getSender(), fse2.getSender());
        assertEquals(fse.getReceiver(), fse2.getReceiver());
        assertEquals(fse.getBlockSize(), fse2.getBlockSize());
        assertEquals(fse.getFileEntryIndex(), fse2.getFileEntryIndex());
        assertEquals(fse.getTotalNumBlocks(), fse2.getTotalNumBlocks());

        assertEquals(fse, fse2);
    }

}