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
    public void testMarshallUnmarshall_START_FILE_LIST() {
        System.out.println("===== called testMarshallUnmarshall_START_FILE_LIST()");
        CMFileSyncEvent fsEvent = new CMFileSyncEvent();
        fsEvent.setID(CMFileSyncEvent.START_FILE_LIST);
        fsEvent.setUserName("ccslab");
        fsEvent.setNumTotalFiles(50);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numTotalFiles = unmarshallEvent.getNumTotalFiles();
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
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numTotalFiles = unmarshallEvent.getNumTotalFiles();
        assertEquals(numTotalFiles, 50);
        int returnCode = unmarshallEvent.getReturnCode();
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
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);
        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 5);
        int numFiles = unmarshallEvent.getNumFiles();
        assertEquals(numFiles, 11);
        // List<CMFileSyncEntry> is null
        List<CMFileSyncEntry> entryList = unmarshallEvent.getFileEntryList();
        assertNull(entryList);

        // add an empty List<CMFileSyncEntry>
        entryList = new ArrayList<>();
        fsEvent.setFileEntryList(entryList);
        System.out.println("fsEvent = " + fsEvent);
        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);
        List<CMFileSyncEntry> unmarshallEntryList = unmarshallEvent.getFileEntryList();
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
        System.out.println("fsEvent = " + fsEvent);

        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);
        unmarshallEntryList = unmarshallEvent.getFileEntryList();

        CMFileSyncEntry unmarshallEntry1 = unmarshallEntryList.get(0);
        Path pathRelativeToHome = unmarshallEntry1.getPathRelativeToHome();
        assertEquals(unmarshallEntry1.getPathRelativeToHome(), entry1.getPathRelativeToHome());
        long size = unmarshallEntry1.getSize();
        assertEquals(unmarshallEntry1.getSize(), entry1.getSize());
        FileTime lastModifiedTime = unmarshallEntry1.getLastModifiedTime();
        assertEquals(unmarshallEntry1.getLastModifiedTime(), entry1.getLastModifiedTime());

        CMFileSyncEntry unmarshallEntry2 = unmarshallEntryList.get(1);
        assertEquals(unmarshallEntry2.getPathRelativeToHome(), entry2.getPathRelativeToHome());
        assertEquals(unmarshallEntry2.getSize(), entry2.getSize());
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
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 5);
        int numFiles = unmarshallEvent.getNumFiles();
        assertEquals(numFiles, 11);

        List<CMFileSyncEntry> unmarshallEntryList = unmarshallEvent.getFileEntryList();
        assertNotNull(unmarshallEntryList);

        CMFileSyncEntry unmarshallEntry1 = unmarshallEntryList.get(0);
        Path pathRelativeToHome = unmarshallEntry1.getPathRelativeToHome();
        assertEquals(unmarshallEntry1.getPathRelativeToHome(), entry1.getPathRelativeToHome());
        long size = unmarshallEntry1.getSize();
        assertEquals(unmarshallEntry1.getSize(), entry1.getSize());
        FileTime lastModifiedTime = unmarshallEntry1.getLastModifiedTime();
        assertEquals(unmarshallEntry1.getLastModifiedTime(), entry1.getLastModifiedTime());

        CMFileSyncEntry unmarshallEntry2 = unmarshallEntryList.get(1);
        assertEquals(unmarshallEntry2.getPathRelativeToHome(), entry2.getPathRelativeToHome());
        assertEquals(unmarshallEntry2.getSize(), entry2.getSize());
        assertEquals(unmarshallEntry2.getLastModifiedTime(), entry2.getLastModifiedTime());

        int returnCode = unmarshallEvent.getReturnCode();
        assertEquals(returnCode, 1);
    }

    @Test
    public void testMarshallUnmarshall_END_FILE_LIST() {
        System.out.println("===== called testMarshallUnmarshall_END_FILE_LIST()");
        CMFileSyncEvent fsEvent = new CMFileSyncEvent();
        fsEvent.setID(CMFileSyncEvent.END_FILE_LIST);
        fsEvent.setUserName("ccslab");
        fsEvent.setNumFilesCompleted(20);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 20);
    }

    @Test
    public void testMarshallUnmarshall_END_FILE_LIST_ACK() {
        System.out.println("===== called testMarshallUnmarshall_END_FILE_LIST_ACK()");
        CMFileSyncEvent fsEvent = new CMFileSyncEvent();
        fsEvent.setID(CMFileSyncEvent.END_FILE_LIST_ACK);
        fsEvent.setUserName("ccslab");
        fsEvent.setNumFilesCompleted(8);
        fsEvent.setReturnCode(1);
        System.out.println("fsEvent = " + fsEvent);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 8);
        int returnCode = unmarshallEvent.getReturnCode();
        assertEquals(returnCode, 1);
    }

    @Test
    public void testMarshallUnmarshall_REQUEST_NEW_FILES() {
        System.out.println("===== called testMarshallUnmarshall_REQUEST_NEW_FILES()");
        CMFileSyncEvent fse = new CMFileSyncEvent();
        fse.setID(CMFileSyncEvent.REQUEST_NEW_FILES);
        fse.setRequesterName("ccslab");
        fse.setNumRequestedFiles(5);
        // requested list is null
        fse.setRequestedFileList(null);
        System.out.println("fse = " + fse);

        // the case that the requested file list is null
        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String requesterName = unmarshallEvent.getRequesterName();
        assertEquals(requesterName, "ccslab");
        int numRequestedFiles = unmarshallEvent.getNumRequestedFiles();
        assertEquals(numRequestedFiles, 5);
        List<Path> unmarshallRequestedFileList;
        assertNull(unmarshallEvent.getRequestedFileList());

        // the case that the requested file list exists
        List<Path> requestedFileList = new ArrayList<>();
        requestedFileList.add(Paths.get("testFile1.txt"));
        requestedFileList.add(Paths.get("subdir/testFile2.txt"));
        fse.setRequestedFileList(requestedFileList);
        System.out.println("fse = " + fse);

        byteBuffer = CMEventManager.marshallEvent(fse);
        unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        requesterName = unmarshallEvent.getRequesterName();
        assertEquals(requesterName, "ccslab");
        numRequestedFiles = unmarshallEvent.getNumRequestedFiles();
        assertEquals(numRequestedFiles, 5);
        unmarshallRequestedFileList = unmarshallEvent.getRequestedFileList();
        assertEquals(unmarshallRequestedFileList.get(0), Paths.get("testFile1.txt"));
        assertEquals(unmarshallRequestedFileList.get(1), Paths.get("subdir/testFile2.txt"));
    }

    @Test
    public void testMarsahllUnmarshall_COMPLETE_NEW_FILE() {
        System.out.println("===== called testMarshallUnmarshall_COMPLETE_NEW_FILE()");
        CMFileSyncEvent fse = new CMFileSyncEvent();
        fse.setID(CMFileSyncEvent.COMPLETE_NEW_FILE);
        fse.setUserName("ccslab");
        fse.setCompletedPath(Paths.get("test1.txt"));
        System.out.println("fse = " + fse);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        Path completedPath = unmarshallEvent.getCompletedPath();
        assertEquals(completedPath, Paths.get("test1.txt"));
    }

    @Test
    public void testMarsahllUnmarshall_COMPLETE_UPDATE_FILE() {
        System.out.println("===== called testMarshallUnmarshall_COMPLETE_UPDATE_FILE()");
        CMFileSyncEvent fse = new CMFileSyncEvent();
        fse.setID(CMFileSyncEvent.COMPLETE_UPDATE_FILE);
        fse.setUserName("ccslab");
        fse.setCompletedPath(Paths.get("test1.txt"));
        System.out.println("fse = " + fse);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        Path completedPath = unmarshallEvent.getCompletedPath();
        assertEquals(completedPath, Paths.get("test1.txt"));
    }

    @Test
    public void testMarshallUnmarshall_COMPLETE_FILE_SYNC() {
        System.out.println("===== called testMarshallUnmarshall_COMPLETE_UPDATE_FILE()");
        CMFileSyncEvent fse = new CMFileSyncEvent();
        fse.setID(CMFileSyncEvent.COMPLETE_FILE_SYNC);
        fse.setUserName("ccslab");
        fse.setNumFilesCompleted(25);
        System.out.println("fse = " + fse);

        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fse);
        CMFileSyncEvent unmarshallEvent = (CMFileSyncEvent) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        String userName = unmarshallEvent.getUserName();
        assertEquals(userName, "ccslab");
        int numFilesCompleted = unmarshallEvent.getNumFilesCompleted();
        assertEquals(numFilesCompleted, 25);
    }
}