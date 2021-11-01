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

    public CMFileSyncEvent setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public int getNumTotalFiles() {
        return numTotalFiles;
    }

    public CMFileSyncEvent setNumTotalFiles(int numTotalFiles) {
        this.numTotalFiles = numTotalFiles;
        return this;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public CMFileSyncEvent setReturnCode(int returnCode) {
        this.returnCode = returnCode;
        return this;
    }

    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public CMFileSyncEvent setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
        return this;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public CMFileSyncEvent setNumFiles(int numFiles) {
        this.numFiles = numFiles;
        return this;
    }

    public List<CMFileSyncEntry> getFileEntryList() {
        return fileEntryList;
    }

    public CMFileSyncEvent setFileEntryList(List<CMFileSyncEntry> fileEntryList) {
        this.fileEntryList = fileEntryList;
        return this;
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();

        switch(m_nID) {
            case START_FILE_LIST:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numTotalFiles
                byteNum += Integer.BYTES;
                break;
            case START_FILE_LIST_ACK:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numTotalFiles
                byteNum += Integer.BYTES;
                // returnCode
                byteNum += Integer.BYTES;
                break;
            case FILE_ENTRIES:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numFilesCompleted
                byteNum += Integer.BYTES;
                // numFiles
                byteNum += Integer.BYTES;
                // number of elements of fileEntryList
                byteNum += Integer.BYTES;
                // fileEntryList (Path pathRelativeToHome, long size, FileTime lastModifiedTime)
                for(CMFileSyncEntry entry : fileEntryList) {
                    // Path pathRelativeToHome
                    byteNum += CMInfo.STRING_LEN_BYTES_LEN +
                            entry.getPathRelativeToHome().toString().getBytes().length;
                    // long size
                    byteNum += Long.BYTES;
                    // FileTime lastModifiedTime -> long type of milliseconds
                    byteNum += Long.BYTES;
                }
                break;
            case FILE_ENTRIES_ACK:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numFilesCompleted
                byteNum += Integer.BYTES;
                // numFiles
                byteNum += Integer.BYTES;
                // number of elements of fileEntryList
                byteNum += Integer.BYTES;
                // fileEntryList (Path pathRelativeToHome, long size, FileTime lastModifiedTime)
                for(CMFileSyncEntry entry : fileEntryList) {
                    // Path pathRelativeToHome
                    byteNum += CMInfo.STRING_LEN_BYTES_LEN +
                            entry.getPathRelativeToHome().toString().getBytes().length;
                    // long size
                    byteNum += Long.BYTES;
                    // FileTime lastModifiedTime -> long type of milliseconds
                    byteNum += Long.BYTES;
                }
                // returnCode
                byteNum += Integer.BYTES;
                break;
            case END_FILE_LIST:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numFilesCompleted
                byteNum += Integer.BYTES;
                break;
            case END_FILE_LIST_ACK:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numFilesCompleted
                byteNum += Integer.BYTES;
                // returnCode
                byteNum += Integer.BYTES;
                break;
            default:
                byteNum = -1;
                break;
        }

        return byteNum;
    }

    @Override
    protected void marshallBody() {

        switch(m_nID) {
            case START_FILE_LIST:
                // userName
                putStringToByteBuffer(userName);
                // numTotalFiles
                m_bytes.putInt(numTotalFiles);
                break;
            case START_FILE_LIST_ACK:
                // userName
                putStringToByteBuffer(userName);
                // numTotalFiles
                m_bytes.putInt(numTotalFiles);
                // returnCode
                m_bytes.putInt(returnCode);
                break;
            case FILE_ENTRIES:
                // userName
                putStringToByteBuffer(userName);
                // numFilesCompleted
                m_bytes.putInt(numFilesCompleted);
                // numFiles
                m_bytes.putInt(numFiles);
                // number of elements of fileEntryList
                m_bytes.putInt(fileEntryList.size());
                // fileEntryList
                for(CMFileSyncEntry entry : fileEntryList) {
                    // Path relativePathToHome
                    putStringToByteBuffer(entry.getPathRelativeToHome().toString());
                    // long size
                    m_bytes.putLong(entry.getSize());
                    // FileTime lastModifiedTime (changed to long milliseconds)
                    m_bytes.putLong(entry.getLastModifiedTime().toMillis());
                }
                break;
            case FILE_ENTRIES_ACK:
                // userName
                putStringToByteBuffer(userName);
                // numFilesCompleted
                m_bytes.putInt(numFilesCompleted);
                // numFiles
                m_bytes.putInt(numFiles);
                // number of elements of fileEntryList
                m_bytes.putInt(fileEntryList.size());
                // fileEntryList
                for(CMFileSyncEntry entry : fileEntryList) {
                    // Path relativePathToHome
                    putStringToByteBuffer(entry.getPathRelativeToHome().toString());
                    // long size
                    m_bytes.putLong(entry.getSize());
                    // FileTime lastModifiedTime (changed to long milliseconds)
                    m_bytes.putLong(entry.getLastModifiedTime().toMillis());
                }
                // returnCode
                m_bytes.putInt(returnCode);
                break;
            case END_FILE_LIST:
                // userName
                putStringToByteBuffer(userName);
                // numFilesCompleted
                m_bytes.putInt(numFilesCompleted);
                break;
            case END_FILE_LIST_ACK:
                // userName
                putStringToByteBuffer(userName);
                // numFilesCompleted
                m_bytes.putInt(numFilesCompleted);
                // returnCode
                m_bytes.putInt(returnCode);
                break;
            default:
                System.err.println("CMFileSyncEvent.marshallBody(), unknown event Id("+m_nID+").");
                m_bytes = null;
                break;
        }

    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {

        switch(m_nID) {

            // from here

        }
    }
}
