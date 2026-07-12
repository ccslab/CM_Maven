package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client requests the server to start
 * a pull synchronization, carrying the client's current cursor (last applied change id).
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventStartPullSync extends CMFileSyncEvent {
    private long cursor;    // client cursor value (0 if none)

    public CMFileSyncEventStartPullSync() {
        m_nID = CMFileSyncEvent.START_PULL_SYNC;
        cursor = 0;
    }

    public CMFileSyncEventStartPullSync(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // cursor
        byteNum += Long.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // cursor
        m_bytes.putLong(cursor);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // cursor
        cursor = msg.getLong();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartPullSync{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", cursor=" + cursor +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventStartPullSync that = (CMFileSyncEventStartPullSync) o;
        return cursor == that.cursor && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), cursor);
    }

    /**
     * gets the client cursor value.
     * @return client cursor value (0 if none)
     */
    public long getCursor() {
        return cursor;
    }

    public void setCursor(long cursor) {
        this.cursor = cursor;
    }
}