package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client acknowledges the start of
 * receiving a list of server entries for pull synchronization.
 * <br>This is the reverse-direction counterpart of {@link CMFileSyncEventStartFileListAck},
 * which the server sends to the client in the full push synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventStartServerEntryListAck extends CMFileSyncEvent {
    private int numTotalFiles;  // number of total files
    private int returnCode;     // return code

    public CMFileSyncEventStartServerEntryListAck() {
        m_nID = CMFileSyncEvent.START_SERVER_ENTRY_LIST_ACK;
        numTotalFiles = 0;
        returnCode = -1;
    }

    public CMFileSyncEventStartServerEntryListAck(ByteBuffer msg) {
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
        return "CMFileSyncEventStartServerEntryListAck{" +
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
        CMFileSyncEventStartServerEntryListAck that = (CMFileSyncEventStartServerEntryListAck) o;
        return numTotalFiles == that.numTotalFiles &&
                returnCode == that.returnCode &&
                getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numTotalFiles, returnCode);
    }

    /**
     * gets the total number of server entries to be synchronized.
     * @return total number of server entries
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