package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server replies to the client about
 * the completion of receiving the list of requested files to be changed to the online mode.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndOnlineModeListAck extends CMFileSyncEvent {
    private int numOnlineModeFiles;
    private int returnCode;

    public CMFileSyncEventEndOnlineModeListAck() {
        m_nID = CMFileSyncEvent.END_ONLINE_MODE_LIST_ACK;
        numOnlineModeFiles = 0;
        returnCode = -1;
    }

    public CMFileSyncEventEndOnlineModeListAck(ByteBuffer msg) {
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
        // numOnlineModeFiles
        byteNum += Integer.BYTES;
        // returnCode
        byteNum += Integer.BYTES;

        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numOnlineModeFiles
        m_bytes.putInt(numOnlineModeFiles);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numOnlineModeFiles
        numOnlineModeFiles = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndOnlineModeAck{" +
                "initiatorName='" + getInitiatorName() + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", numOnlineModeFiles=" + numOnlineModeFiles +
                ", returnCode=" + returnCode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndOnlineModeListAck that = (CMFileSyncEventEndOnlineModeListAck) o;
        return numOnlineModeFiles == that.numOnlineModeFiles && returnCode == that.returnCode && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numOnlineModeFiles, returnCode);
    }


    /**
     * gets the number of requested files to be changed to the online mode
     * @return number of requested local mode files
     */
    public int getNumOnlineModeFiles() {
        return numOnlineModeFiles;
    }

    public void setNumOnlineModeFiles(int numOnlineModeFiles) {
        this.numOnlineModeFiles = numOnlineModeFiles;
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
