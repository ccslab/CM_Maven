package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class CMFileSyncEventUpdateExistingFileTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventUpdateExistingFileTest.marshallUnmarshall() called..");
        CMFileSyncEventUpdateExistingFile fse = new CMFileSyncEventUpdateExistingFile();
        fse.setFileEntryIndex(8);
        fse.setMatchBlockIndex(115);
        fse.setNumNonMatchBytes(10);
        byte[] bytes = {1,2,3,4,5,6,7,8,9,10};
        fse.setNonMatchBytes(bytes);

        ByteBuffer msg = CMEventManager.marshallEvent(fse);
        System.out.println("fse after marshalling = " + fse);
        CMFileSyncEventUpdateExistingFile fse2 =
                (CMFileSyncEventUpdateExistingFile) CMEventManager.unmarshallEvent(msg);
        System.out.println("fse2 after unmarshalling = " + fse2);
        assertNotNull(fse2);
        assertEquals(fse.getFileEntryIndex(), fse2.getFileEntryIndex());
        assertEquals(fse.getMatchBlockIndex(), fse2.getMatchBlockIndex());
        assertEquals(fse.getNumNonMatchBytes(), fse2.getNumNonMatchBytes());
        assertArrayEquals(fse.getNonMatchBytes(), fse2.getNonMatchBytes());

        assertEquals(fse, fse2);
    }
}