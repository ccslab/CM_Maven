package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client notifies the server of
 * the completion of sending all push (client) entry lists for incremental push synchronization.
 * <br>On receiving this event, the server verifies the accumulated entry count of its
 * pushStateTable[stateKey] and then starts the per-op processing.
 * <br>This is the reverse-direction counterpart of {@link CMFileSyncEventEndServerEntryList},
 * which the server sends to the client in the pull synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndPushEntryList extends CMFileSyncEvent {
    private int numFilesCompleted;  // number of files completed

    public CMFileSyncEventEndPushEntryList() {
        m_nID = CMFileSyncEvent.END_PUSH_ENTRY_LIST;
        numFilesCompleted = 0;
    }

    public CMFileSyncEventEndPushEntryList(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numFilesCompleted
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndPushEntryList{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", numFilesCompleted=" + numFilesCompleted +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndPushEntryList that = (CMFileSyncEventEndPushEntryList) o;
        return numFilesCompleted == that.numFilesCompleted && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numFilesCompleted);
    }

    /**
     * gets the number of push entries completed.
     * @return number of push entries completed
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }
}
