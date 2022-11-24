package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server acknowledges
 * the reception of a list of file entries that will be synchronized.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventFileEntriesAck extends CMFileSyncEvent {
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList, returnCode
    private String userName;    // user name
    private int numFilesCompleted;  // number of files completed
    private int numFiles;       // number of current files
    private int returnCode;     // return code

    public CMFileSyncEventFileEntriesAck() {
        m_nID = CMFileSyncEvent.FILE_ENTRIES_ACK;
        userName = null;
        numFilesCompleted = 0;
        numFiles = 0;
        returnCode = -1;
    }

    public CMFileSyncEventFileEntriesAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // userName
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
        // numFilesCompleted
        byteNum += Integer.BYTES;
        // numFiles
        byteNum += Integer.BYTES;
        // returnCode
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // userName
        putStringToByteBuffer(userName);
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
        // numFiles
        m_bytes.putInt(numFiles);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        int numFileEntries;

        // userName
        userName = getStringFromByteBuffer(msg);
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
        // numFiles
        numFiles = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventFileEntriesAck{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nByteNum=" + m_nByteNum +
                ", userName='" + userName + '\'' +
                ", numFilesCompleted=" + numFilesCompleted +
                ", numFiles=" + numFiles +
                ", returnCode=" + returnCode +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventFileEntriesAck fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getNumFilesCompleted() == numFilesCompleted &&
                fse.getNumFiles() == numFiles &&
                fse.getReturnCode() == returnCode;
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventFileEntriesAck that = (CMFileSyncEventFileEntriesAck) o;
        return numFilesCompleted == that.numFilesCompleted &&
                numFiles == that.numFiles &&
                returnCode == that.returnCode &&
                userName.equals(that.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, numFilesCompleted, numFiles, returnCode);
    }

    /**
     * gets the user (client) name.
     * @return user (client) name
     */
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * gets the number of file entries that has been already transferred.
     * @return number of file entries that has been already transferred
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }

    /**
     * gets the number of file entries in this event.
     * @return number of file entries in this event
     */
    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }

    /**
     * gets the return code.
     * @return return code
     * <br> 1 if successful; 0 otherwise.
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
