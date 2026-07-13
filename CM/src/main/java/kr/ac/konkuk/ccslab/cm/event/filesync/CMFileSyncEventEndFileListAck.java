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
    private int numFilesCompleted;  // number of files completed
    // return code: 1 = OK(파일 수 일치), 0 = 파일 수 불일치,
    //              [10-3] 2 = busy(다른 device 가 per-user push lease 보유 중, full-push 개시 거절, §2.6 (ii))
    private int returnCode;

    public CMFileSyncEventEndFileListAck() {
        m_nID = CMFileSyncEvent.END_FILE_LIST_ACK;
        returnCode = -1;
        numFilesCompleted = 0;
    }

    public CMFileSyncEventEndFileListAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    /** @deprecated Use {@link #getInitiatorName()} instead. */
    @Deprecated
    public String getUserName() { return getInitiatorName(); }

    /** @deprecated Use {@link #setInitiatorName(String)} instead. */
    @Deprecated
    public void setUserName(String name) { setInitiatorName(name); }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numFilesCompleted
        byteNum += Integer.BYTES;
        // returnCode
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
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
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
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
                returnCode == that.returnCode && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numFilesCompleted, returnCode);
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
