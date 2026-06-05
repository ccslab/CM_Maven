package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client that
 * the incremental push synchronization with this client has been completed.
 * <br>Before sending this event, the server must commit the transaction: decide that all
 * per-op entries of pushStateTable[stateKey] are completed, append this push's change batch
 * to the client's change-log, increment the server cursor (lastChangeId) and persist the
 * index. The new cursor value travels with this event via {@code newServerCursor}; the client
 * adopts it and calls saveClientCursor.
 * <br>This is the same-direction counterpart of {@link CMFileSyncEventCompletePullSync}
 * (server notifies session completion in both pull and push).
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompletePushSync extends CMFileSyncEvent {
    // Fields: numFilesCompleted, newServerCursor
    private int numFilesCompleted;  // number of completed push-sync entries (pushStateTable[stateKey] size)
    private long newServerCursor;   // server cursor after change-log append + cursor increment

    public CMFileSyncEventCompletePushSync() {
        m_nID = CMFileSyncEvent.COMPLETE_PUSH_SYNC;
        numFilesCompleted = 0;
        newServerCursor = -1;
    }

    public CMFileSyncEventCompletePushSync(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numFilesCompleted
        byteNum += Integer.BYTES;
        // newServerCursor
        byteNum += Long.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
        // newServerCursor
        m_bytes.putLong(newServerCursor);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
        // newServerCursor
        newServerCursor = msg.getLong();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompletePushSync{" +
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
                ", newServerCursor=" + newServerCursor +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventCompletePushSync that = (CMFileSyncEventCompletePushSync) o;
        return numFilesCompleted == that.numFilesCompleted &&
                newServerCursor == that.newServerCursor &&
                getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numFilesCompleted, newServerCursor);
    }

    /**
     * gets the number of completed push-sync entries.
     * @return number of completed push-sync entries
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }

    /**
     * gets the new server cursor after the push transaction has been committed.
     * @return new server cursor
     */
    public long getNewServerCursor() {
        return newServerCursor;
    }

    public void setNewServerCursor(long newServerCursor) {
        this.newServerCursor = newServerCursor;
    }
}
