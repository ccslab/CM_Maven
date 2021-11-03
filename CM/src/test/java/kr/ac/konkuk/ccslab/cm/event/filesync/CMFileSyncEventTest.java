package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CMFileSyncEventTest {

    @Test
    public void testSettersGetters() {
        System.out.println("===== called testSettersGetters()");

        CMFileSyncEvent fsEvent = new CMFileSyncEvent();
        fsEvent.setUserName("ccslab");
        String userName = fsEvent.getUserName();
        assertSame(userName, "ccslab");

        fsEvent.setNumFiles(100);
        int numFiles = fsEvent.getNumFiles();
        assertSame(numFiles, 100);

        fsEvent.setNumTotalFiles(100);
        int numTotalFiles = fsEvent.getNumTotalFiles();
        assertSame(numTotalFiles, 100);

        fsEvent.setNumFilesCompleted(100);
        int numFilesCompleted = fsEvent.getNumFilesCompleted();
        assertSame(numFilesCompleted, 100);

        fsEvent.setReturnCode(1);
        int returnCode = fsEvent.getReturnCode();
        assertSame(returnCode, 1);

        List<CMFileSyncEntry> entryList = new ArrayList<>();
        fsEvent.setFileEntryList(entryList);
        assertSame(entryList, fsEvent.getFileEntryList());
    }

    @Test
    public void testGetByteNum() {
        System.out.println("===== called testGetByteNum()");
    }

    @Test
    public void testMarshall() {
        System.out.println("===== called testMarsahll()");
    }

    @Test
    public void testUnmarshall() {
        System.out.println("===== called testUnmarshall()");
    }
}