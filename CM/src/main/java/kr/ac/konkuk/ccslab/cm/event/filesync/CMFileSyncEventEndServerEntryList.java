package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * the completion of sending all server entry lists for pull synchronization.
 * <br>This is the reverse-direction counterpart of {@link CMFileSyncEventEndFileList},
 * which the client sends to the server in the full push synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndServerEntryList extends CMFileSyncEvent {
    private int numFilesCompleted;  // number of files completed

    public CMFileSyncEventEndServerEntryList() {
        m_nID = CMFileSyncEvent.END_SERVER_ENTRY_LIST;
        numFilesCompleted = 0;
    }

    public CMFileSyncEventEndServerEntryList(ByteBuffer msg) {
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
        return "CMFileSyncEventEndServerEntryList{" +
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
        CMFileSyncEventEndServerEntryList that = (CMFileSyncEventEndServerEntryList) o;
        return numFilesCompleted == that.numFilesCompleted && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numFilesCompleted);
    }

    /**
     * gets the number of server entries completed.
     * @return number of server entries completed
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }
}
