package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server replies to the client about
 * the completion of receiving file list for synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndFileListAck extends CMFileSyncEvent {
    // Fields: userName, numFilesCompleted, returnCode
    private String userName;    // user name
    private int numFilesCompleted;  // number of files completed
    private int returnCode;     // return code

    public CMFileSyncEventEndFileListAck() {
        m_nID = CMFileSyncEvent.END_FILE_LIST_ACK;
        userName = null;
        returnCode = -1;
        numFilesCompleted = 0;
    }

    public CMFileSyncEventEndFileListAck(ByteBuffer msg) {
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
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // userName
        userName = getStringFromByteBuffer(msg);
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndFileListAck{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", userName='" + userName + '\'' +
                ", numFilesCompleted=" + numFilesCompleted +
                ", returnCode=" + returnCode +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventEndFileListAck fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getNumFilesCompleted() == numFilesCompleted &&
                fse.getReturnCode() == returnCode;
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndFileListAck that = (CMFileSyncEventEndFileListAck) o;
        return numFilesCompleted == that.numFilesCompleted &&
                returnCode == that.returnCode && userName.equals(that.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, numFilesCompleted, returnCode);
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
     * gets the number of files.
     * @return number of files
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }

    /**
     * gets the return code.
     * @return return code
     * <br>1 if successful; 0 otherwise
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
