package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * the start of sending a list of server entries for pull synchronization.
 * <br>This is the reverse-direction counterpart of {@link CMFileSyncEventStartFileList},
 * which the client sends to the server in the full push synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventStartServerEntryList extends CMFileSyncEvent {
    private int numTotalFiles;  // number of total files

    public CMFileSyncEventStartServerEntryList() {
        m_nID = CMFileSyncEvent.START_SERVER_ENTRY_LIST;
        numTotalFiles = 0;
    }

    public CMFileSyncEventStartServerEntryList(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numTotalFiles
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numTotalFiles
        m_bytes.putInt(numTotalFiles);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numTotalFiles
        numTotalFiles = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartServerEntryList{" +
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
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventStartServerEntryList that = (CMFileSyncEventStartServerEntryList) o;
        return numTotalFiles == that.numTotalFiles && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numTotalFiles);
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
}