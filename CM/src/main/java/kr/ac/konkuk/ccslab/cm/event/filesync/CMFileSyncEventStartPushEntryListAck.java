package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server acknowledges the start of
 * receiving a list of push (client) entries for incremental push synchronization.
 * <br>This is the reverse-direction counterpart of {@link CMFileSyncEventStartServerEntryListAck},
 * which the client sends to the server in the pull synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventStartPushEntryListAck extends CMFileSyncEvent {
    private int numTotalFiles;  // number of total files
    private int returnCode;     // return code

    public CMFileSyncEventStartPushEntryListAck() {
        m_nID = CMFileSyncEvent.START_PUSH_ENTRY_LIST_ACK;
        numTotalFiles = 0;
        returnCode = -1;
    }

    public CMFileSyncEventStartPushEntryListAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numTotalFiles
        byteNum += Integer.BYTES;
        // returnCode
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numTotalFiles
        m_bytes.putInt(numTotalFiles);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numTotalFiles
        numTotalFiles = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartPushEntryListAck{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", numTotalFiles=" + numTotalFiles +
                ", returnCode=" + returnCode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventStartPushEntryListAck that = (CMFileSyncEventStartPushEntryListAck) o;
        return numTotalFiles == that.numTotalFiles &&
                returnCode == that.returnCode &&
                getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numTotalFiles, returnCode);
    }

    /**
     * gets the total number of push entries to be synchronized.
     * @return total number of push entries
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
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
