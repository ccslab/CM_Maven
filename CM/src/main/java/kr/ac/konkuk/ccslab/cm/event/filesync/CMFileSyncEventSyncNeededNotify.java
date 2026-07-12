package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * [NEW 10-3] This class represents a CMFileSyncEvent with which the server notifies an online
 * device that the synchronized files of the same user have changed, so the device should pull.
 * <br>The server sends this after a push session commits (changelog append + changelogHead
 * advance). It is a hint (wake-up): propagation correctness is guaranteed by the receiver's pull
 * (clientCursor vs. changelogHead), so a lost/duplicated notify is safe.
 * <br>Common-header convention: the server creates the event but fills the initiator* fields with
 * the pushing client A's values (initiatorName = A's user name, initiatorUuid = A's session UUID,
 * initiatorDeviceUuid = A's device UUID). The sender is the server, but the semantic cause is A.
 * <br>The origin device is carried by the common header's initiatorDeviceUuid (no separate field);
 * the only event-specific member is {@code changelogHead}.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventSyncNeededNotify extends CMFileSyncEvent {
    // Fields: changelogHead (통지 시점 전역 head; 수신측 빠른 비교용, 권위는 아님)
    private long changelogHead;

    public CMFileSyncEventSyncNeededNotify() {
        m_nID = CMFileSyncEvent.SYNC_NEEDED_NOTIFY;
        changelogHead = -1;
    }

    public CMFileSyncEventSyncNeededNotify(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // changelogHead
        byteNum += Long.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // changelogHead
        m_bytes.putLong(changelogHead);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // changelogHead
        changelogHead = msg.getLong();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventSyncNeededNotify{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", initiatorUuid=" + getInitiatorUuid() +
                ", initiatorDeviceUuid=" + getInitiatorDeviceUuid() +
                ", changelogHead=" + changelogHead +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventSyncNeededNotify that = (CMFileSyncEventSyncNeededNotify) o;
        return changelogHead == that.changelogHead &&
                getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), changelogHead);
    }

    /**
     * gets the global changelog head at the time of this notification.
     * @return changelog head (next-changeId basis / pull comparison basis)
     */
    public long getChangelogHead() {
        return changelogHead;
    }

    public void setChangelogHead(long changelogHead) {
        this.changelogHead = changelogHead;
    }
}
