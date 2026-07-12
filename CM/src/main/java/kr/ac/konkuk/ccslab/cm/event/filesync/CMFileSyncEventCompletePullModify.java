package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client notifies the server, during
 * pull synchronization, that it has finished updating a single file.
 * <br>One event is sent per completed file update.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompletePullModify extends CMFileSyncEvent {
    // Fields: modifiedPath
    private String modifiedPath;    // modified file path relative to sync home

    public CMFileSyncEventCompletePullModify() {
        m_nID = CMFileSyncEvent.COMPLETE_PULL_MODIFY;
        modifiedPath = null;
    }

    public CMFileSyncEventCompletePullModify(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // modifiedPath
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + modifiedPath.getBytes().length;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // modifiedPath
        putStringToByteBuffer(modifiedPath);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // modifiedPath
        modifiedPath = getStringFromByteBuffer(msg);
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompletePullModify{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", modifiedPath=" + modifiedPath +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventCompletePullModify that = (CMFileSyncEventCompletePullModify) o;
        return getInitiatorName().equals(that.getInitiatorName()) &&
                Objects.equals(modifiedPath, that.modifiedPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), modifiedPath);
    }

    /**
     * gets the modified file path relative to the sync home.
     * @return modified file path
     */
    public String getModifiedPath() {
        return modifiedPath;
    }

    public void setModifiedPath(String modifiedPath) {
        this.modifiedPath = modifiedPath;
    }
}
