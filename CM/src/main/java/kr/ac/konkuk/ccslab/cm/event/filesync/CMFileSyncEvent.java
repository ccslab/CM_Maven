package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * This class represents CM events that are used for the file-sync task.
 *
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEvent extends CMEvent {

    // Fields: userName, numTotalFiles
    public static final int START_FILE_LIST = 1;
    // Fields: userName, numTotalFiles, returnCode
    public static final int START_FILE_LIST_ACK = 2;
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList
    public static final int FILE_ENTRIES = 3;
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList, returnCode
    public static final int FILE_ENTRIES_ACK = 4;
    // Fields: userName, numFilesCompleted
    public static final int END_FILE_LIST = 5;
    // Fields: userName, numFilesCompleted, returnCode
    public static final int END_FILE_LIST_ACK = 6;

    private String userName;    // user name
    private int numTotalFiles;  // number of total files
    private int returnCode;     // return code
    private int numFilesCompleted;  // number of files completed
    private int numFiles;       // number of current files
    private List<CMFileSyncEntry> fileEntryList;    // list of CMFileSyncEntry

    public CMFileSyncEvent() {
        m_nType = CMInfo.CM_FILE_SYNC_EVENT;
        m_nID = -1;

        userName = null;
        numTotalFiles = -1;
        returnCode = -1;
        numFilesCompleted = -1;
        numFiles = -1;
        fileEntryList = null;
    }

    public CMFileSyncEvent(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getNumTotalFiles() {
        return numTotalFiles;
    }

    public void setNumTotalFiles(int numTotalFiles) {
        this.numTotalFiles = numTotalFiles;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }

    public List<CMFileSyncEntry> getFileEntryList() {
        return fileEntryList;
    }

    public void setFileEntryList(List<CMFileSyncEntry> fileEntryList) {
        this.fileEntryList = fileEntryList;
    }

    @Override
    protected int getByteNum() {
        int byteNum = 0;
        byteNum = super.getByteNum();

        // from here

        return byteNum;
    }

    @Override
    protected void marshallBody() {

    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {

    }
}
