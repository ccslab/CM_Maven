package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server acknowledges the reception of
 * the start of sending a list of file entries for synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventStartFileListAck extends CMFileSyncEvent {
    private String userName;    // user name
    private int numTotalFiles;  // number of total files
    private int returnCode;     // return code

    public CMFileSyncEventStartFileListAck() {
        m_nID = CMFileSyncEvent.START_FILE_LIST_ACK;
        userName = null;
        numTotalFiles = 0;
        returnCode = -1;
    }

    public CMFileSyncEventStartFileListAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // userName
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
        // numTotalFiles
        byteNum += Integer.BYTES;
        // returnCode
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // userName
        putStringToByteBuffer(userName);
        // numTotalFiles
        m_bytes.putInt(numTotalFiles);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // userName
        userName = getStringFromByteBuffer(msg);
        // numTotalFiles
        numTotalFiles = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartFileListAck{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nByteNum=" + m_nByteNum +
                ", userName='" + userName + '\'' +
                ", numTotalFiles=" + numTotalFiles +
                ", returnCode=" + returnCode +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventStartFileListAck fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getNumTotalFiles() == numTotalFiles &&
                fse.getReturnCode() == returnCode;
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventStartFileListAck that = (CMFileSyncEventStartFileListAck) o;
        return numTotalFiles == that.numTotalFiles && returnCode == that.returnCode && userName.equals(that.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, numTotalFiles, returnCode);
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
     * gets the total number of file entries to be synchronized.
     * @return total number of file entries
     */
    public int getNumTotalFiles() {
        return numTotalFiles;
    }

    public void setNumTotalFiles(int numTotalFiles) {
        this.numTotalFiles = numTotalFiles;
    }

    /**
     * gets the return code.
     * @return return code
     * <br>1 if successful; 0 otherwise.
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
