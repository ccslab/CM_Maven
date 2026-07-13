package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client, during
 * incremental push synchronization, that it has finished receiving a single new file.
 * <br>One event is sent per completed file reception.
 * <br>This is the reverse-direction counterpart of {@link CMFileSyncEventCompletePullCreate},
 * which the client sends to the server in the pull synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompletePushCreate extends CMFileSyncEvent {
    // Fields: createdPath
    private String createdPath;     // created file path relative to sync home

    public CMFileSyncEventCompletePushCreate() {
        m_nID = CMFileSyncEvent.COMPLETE_PUSH_CREATE;
        createdPath = null;
    }

    public CMFileSyncEventCompletePushCreate(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // createdPath
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + createdPath.getBytes().length;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // createdPath
        putStringToByteBuffer(createdPath);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // createdPath
        createdPath = getStringFromByteBuffer(msg);
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompletePushCreate{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", createdPath=" + createdPath +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventCompletePushCreate that = (CMFileSyncEventCompletePushCreate) o;
        return getInitiatorName().equals(that.getInitiatorName()) &&
                Objects.equals(createdPath, that.createdPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), createdPath);
    }

    /**
     * gets the created file path relative to the sync home.
     * @return created file path
     */
    public String getCreatedPath() {
        return createdPath;
    }

    public void setCreatedPath(String createdPath) {
        this.createdPath = createdPath;
    }
}
