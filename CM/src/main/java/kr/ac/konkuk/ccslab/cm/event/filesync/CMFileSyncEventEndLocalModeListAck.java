package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server replies to the client about
 * the completion of receiving the list of requested files to be changed to the local mode.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndLocalModeListAck extends CMFileSyncEvent {
    private int numLocalModeFiles;
    private int returnCode;

    public CMFileSyncEventEndLocalModeListAck() {
        m_nID = CMFileSyncEvent.END_LOCAL_MODE_LIST_ACK;
        numLocalModeFiles = 0;
        returnCode = -1;
    }

    public CMFileSyncEventEndLocalModeListAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    /** @deprecated Use {@link #getInitiatorName()} instead. */
    @Deprecated
    public String getRequester() { return getInitiatorName(); }

    /** @deprecated Use {@link #setInitiatorName(String)} instead. */
    @Deprecated
    public void setRequester(String name) { setInitiatorName(name); }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numLocalModeFiles
        byteNum += Integer.BYTES;
        // returnCode
        byteNum += Integer.BYTES;

        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numLocalModeFiles
        m_bytes.putInt(numLocalModeFiles);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numLocalModeFiles
        numLocalModeFiles = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndLocalModeListAck{" +
                "initiatorName='" + getInitiatorName() + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", numLocalModeFiles=" + numLocalModeFiles +
                ", returnCode=" + returnCode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndLocalModeListAck that = (CMFileSyncEventEndLocalModeListAck) o;
        return numLocalModeFiles == that.numLocalModeFiles && returnCode == that.returnCode && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numLocalModeFiles, returnCode);
    }


    /**
     * gets the number of requested files to be changed to the local mode.
     * @return number of requested online mode files
     */
    public int getNumLocalModeFiles() {
        return numLocalModeFiles;
    }

    public void setNumLocalModeFiles(int numLocalModeFiles) {
        this.numLocalModeFiles = numLocalModeFiles;
    }

    /**
     * gets the return code.
     * @return return code
     * <br> 1 if successful; 0 otherwise
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
