package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import static kr.ac.konkuk.ccslab.cm.manager.CMEventManager.marshallEvent;
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
        // START_FILE_LIST
        CMFileSyncEvent fsEvent = new CMFileSyncEvent();
        fsEvent.setID(CMFileSyncEvent.START_FILE_LIST);
        fsEvent.setUserName("ccslab");
        fsEvent.setNumTotalFiles(10);
        int byteNum = fsEvent.getByteNum();
        System.out.println("fsEvent = " + fsEvent);
        System.out.println("START_FILE_LIST byteNum: "+byteNum); // # header: 24
        assertEquals(byteNum, 36);

        // START_FILE_LIST_ACK
        fsEvent.setID(CMFileSyncEvent.START_FILE_LIST_ACK);
        fsEvent.setReturnCode(1);
        byteNum = fsEvent.getByteNum();
        System.out.println("fsEvent = " + fsEvent);
        System.out.println("START_FILE_LIST_ACK byteNum = " + byteNum);
        assertEquals(byteNum, 40);

        // FILE_ENTRIES
        fsEvent.setID(CMFileSyncEvent.FILE_ENTRIES);
        fsEvent.setUserName("ccslab");  // 2+6
        fsEvent.setNumFilesCompleted(1);    // 4
        fsEvent.setNumFiles(5); // 4
        fsEvent.setFileEntryList(new ArrayList<>()); // empty, only number of entries: 4
        byteNum = fsEvent.getByteNum();
        System.out.println("fsEvent = " + fsEvent);
        System.out.println("FILE_ENTRIES byteNum = " + byteNum);
        assertEquals(byteNum, 44);

        // FILE_ENTRIES_ACK
        fsEvent.setID(CMFileSyncEvent.FILE_ENTRIES_ACK);
        fsEvent.setReturnCode(0);   // 4
        byteNum = fsEvent.getByteNum();
        System.out.println("fsEvent = " + fsEvent);
        System.out.println("FILE_ENTRIES_ACK byteNum = " + byteNum);
        assertEquals(byteNum, 48);

        // END_FILE_LIST
        fsEvent.setID(CMFileSyncEvent.END_FILE_LIST);
        fsEvent.setUserName("ccslab");  // 2+6
        fsEvent.setNumFilesCompleted(15);   // 4
        byteNum = fsEvent.getByteNum();
        System.out.println("fsEvent = " + fsEvent);
        System.out.println("END_FILE_LIST byteNum = " + byteNum);
        assertEquals(byteNum, 36);

        // END_FILE_LIST_ACK
        fsEvent.setID(CMFileSyncEvent.END_FILE_LIST_ACK);
        fsEvent.setReturnCode(3);   // 4
        byteNum = fsEvent.getByteNum();
        System.out.println("fsEvent = " + fsEvent);
        System.out.println("END_FILE_LIST_ACK byteNum = " + byteNum);
        assertEquals(byteNum, 40);
    }

    @Test
    public void testMarshallUnmarshall_START_FILE_LIST() {
        System.out.println("===== called testMarshallUnmarshall_START_FILE_LIST()");
        CMFileSyncEvent fsEvent = new CMFileSyncEvent();
        fsEvent.setID(CMFileSyncEvent.START_FILE_LIST);
        fsEvent.setUserName("ccslab");
        fsEvent.setNumTotalFiles(50);
        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        String userName = unmarshallEvent.getUserName();
        System.out.println("userName = " + userName);
        assertEquals(userName, "ccslab");
        int numTotalFiles = unmarshallEvent.getNumTotalFiles();
        System.out.println("numTotalFiles = " + numTotalFiles);
        assertEquals(numTotalFiles, 50);
    }

    @Test
    public void testMarshallUnmarshall_START_FILE_LIST_ACK() {
        System.out.println("===== called testMarshallUnmarshall_START_FILE_LIST_ACK()");
        CMFileSyncEvent fsEvent = new CMFileSyncEvent();
        fsEvent.setID(CMFileSyncEvent.START_FILE_LIST_ACK);
        fsEvent.setUserName("ccslab");
        fsEvent.setNumTotalFiles(50);
        fsEvent.setReturnCode(1);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        System.out.println("userName = " + userName);
        assertEquals(userName, "ccslab");
        int numTotalFiles = unmarshallEvent.getNumTotalFiles();
        System.out.println("numTotalFiles = " + numTotalFiles);
        assertEquals(numTotalFiles, 50);
        int returnCode = unmarshallEvent.getReturnCode();
        System.out.println("returnCode = " + returnCode);
        assertEquals(returnCode, 1);
    }

    @Test
    public void testMarshallUnmarshall_FILE_ENTRIES() {
        System.out.println("===== called testMarshallUnmarshall_FILE_ENTRIES()");
        CMFileSyncEvent fsEvent = new CMFileSyncEvent();
        fsEvent.setID(CMFileSyncEvent.FILE_ENTRIES);
        fsEvent.setUserName("ccslab");
        fsEvent.setNumFilesCompleted(5);
        fsEvent.setNumFiles(11);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        String userName = unmarshallEvent.getUserName();
        System.out.println("userName = " + userName);
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        System.out.println("numFilesCompleted = " + numFilesCompleted);
        assertEquals(numFilesCompleted, 5);
        int numFiles = unmarshallEvent.getNumFiles();
        System.out.println("numFiles = " + numFiles);
        assertEquals(numFiles, 11);
        // List<CMFileSyncEntry> is null
        List<CMFileSyncEntry> entryList = unmarshallEvent.getFileEntryList();
        assertNull(entryList);

        // add an empty List<CMFileSyncEntry>
        entryList = new ArrayList<>();
        fsEvent.setFileEntryList(entryList);
        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        List<CMFileSyncEntry> unmarshallEntryList = unmarshallEvent.getFileEntryList();
        System.out.println("unmarshallEntryList = " + unmarshallEntryList);
        assertNull(unmarshallEntryList);

        // add an List<CMFileSyncEntry> with 2 items
        CMFileSyncEntry entry1 = new CMFileSyncEntry();
        entry1.setPathRelativeToHome(Paths.get("testFile1"));
        entry1.setSize(100);
        entry1.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
        entryList.add(entry1);

        CMFileSyncEntry entry2 = new CMFileSyncEntry();
        entry2.setPathRelativeToHome(Paths.get("testFile2"));
        entry2.setSize(1000000);
        entry2.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
        entryList.add(entry2);

        fsEvent.setFileEntryList(entryList);
        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        unmarshallEntryList = unmarshallEvent.getFileEntryList();
        System.out.println("unmarshallEntryList = " + unmarshallEntryList);

        CMFileSyncEntry unmarshallEntry1 = unmarshallEntryList.get(0);
        Path pathRelativeToHome = unmarshallEntry1.getPathRelativeToHome();
        System.out.println("pathRelativeToHome = " + pathRelativeToHome);
        assertEquals(unmarshallEntry1.getPathRelativeToHome(), entry1.getPathRelativeToHome());
        long size = unmarshallEntry1.getSize();
        System.out.println("size = " + size);
        assertEquals(unmarshallEntry1.getSize(), entry1.getSize());
        FileTime lastModifiedTime = unmarshallEntry1.getLastModifiedTime();
        System.out.println("lastModifiedTime = " + lastModifiedTime);
        assertEquals(unmarshallEntry1.getLastModifiedTime(), entry1.getLastModifiedTime());

        CMFileSyncEntry unmarshallEntry2 = unmarshallEntryList.get(1);
        pathRelativeToHome = unmarshallEntry2.getPathRelativeToHome();
        System.out.println("pathRelativeToHome = " + pathRelativeToHome);
        assertEquals(unmarshallEntry2.getPathRelativeToHome(), entry2.getPathRelativeToHome());
        size = unmarshallEntry2.getSize();
        System.out.println("size = " + size);
        assertEquals(unmarshallEntry2.getSize(), entry2.getSize());
        lastModifiedTime = unmarshallEntry2.getLastModifiedTime();
        System.out.println("lastModifiedTime = " + lastModifiedTime);
        assertEquals(unmarshallEntry2.getLastModifiedTime(), entry2.getLastModifiedTime());
    }

    @Test
    public void testMarshallUnmarshall_FILE_ENTRIES_ACK() {
        System.out.println("===== called testMarshallUnmarshall_FILE_ENTRIES_ACK()");
        CMFileSyncEvent fsEvent = new CMFileSyncEvent();
        fsEvent.setID(CMFileSyncEvent.FILE_ENTRIES_ACK);
        fsEvent.setUserName("ccslab");
        fsEvent.setNumFilesCompleted(5);
        fsEvent.setNumFiles(11);
        fsEvent.setReturnCode(1);

        List<CMFileSyncEntry> entryList = new ArrayList<>();
        // add an List<CMFileSyncEntry> with 2 items
        CMFileSyncEntry entry1 = new CMFileSyncEntry();
        entry1.setPathRelativeToHome(Paths.get("testFile1"));
        entry1.setSize(100);
        entry1.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
        entryList.add(entry1);

        CMFileSyncEntry entry2 = new CMFileSyncEntry();
        entry2.setPathRelativeToHome(Paths.get("testFile2"));
        entry2.setSize(1000000);
        entry2.setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));
        entryList.add(entry2);

        fsEvent.setFileEntryList(entryList);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        String userName = unmarshallEvent.getUserName();
        System.out.println("userName = " + userName);
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        System.out.println("numFilesCompleted = " + numFilesCompleted);
        assertEquals(numFilesCompleted, 5);
        int numFiles = unmarshallEvent.getNumFiles();
        System.out.println("numFiles = " + numFiles);
        assertEquals(numFiles, 11);

        List<CMFileSyncEntry> unmarshallEntryList = unmarshallEvent.getFileEntryList();
        System.out.println("unmarshallEntryList = " + unmarshallEntryList);
        assertNotNull(unmarshallEntryList);

        CMFileSyncEntry unmarshallEntry1 = unmarshallEntryList.get(0);
        Path pathRelativeToHome = unmarshallEntry1.getPathRelativeToHome();
        System.out.println("pathRelativeToHome = " + pathRelativeToHome);
        assertEquals(unmarshallEntry1.getPathRelativeToHome(), entry1.getPathRelativeToHome());
        long size = unmarshallEntry1.getSize();
        System.out.println("size = " + size);
        assertEquals(unmarshallEntry1.getSize(), entry1.getSize());
        FileTime lastModifiedTime = unmarshallEntry1.getLastModifiedTime();
        System.out.println("lastModifiedTime = " + lastModifiedTime);
        assertEquals(unmarshallEntry1.getLastModifiedTime(), entry1.getLastModifiedTime());

        CMFileSyncEntry unmarshallEntry2 = unmarshallEntryList.get(1);
        pathRelativeToHome = unmarshallEntry2.getPathRelativeToHome();
        System.out.println("pathRelativeToHome = " + pathRelativeToHome);
        assertEquals(unmarshallEntry2.getPathRelativeToHome(), entry2.getPathRelativeToHome());
        size = unmarshallEntry2.getSize();
        System.out.println("size = " + size);
        assertEquals(unmarshallEntry2.getSize(), entry2.getSize());
        lastModifiedTime = unmarshallEntry2.getLastModifiedTime();
        System.out.println("lastModifiedTime = " + lastModifiedTime);
        assertEquals(unmarshallEntry2.getLastModifiedTime(), entry2.getLastModifiedTime());

        int returnCode = unmarshallEvent.getReturnCode();
        System.out.println("returnCode = " + returnCode);
        assertEquals(returnCode, 1);
    }
}