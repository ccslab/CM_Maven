package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client notifies the server, during
 * pull synchronization, of the list of files it has finished deleting.
 * <br>If the list is long, it can be split across multiple events.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompletePullDelete extends CMFileSyncEvent {
    // Fields: deletedPathList
    private List<String> deletedPathList; // list of deleted file paths relative to sync home (forward-slash separated)

    public CMFileSyncEventCompletePullDelete() {
        m_nID = CMFileSyncEvent.COMPLETE_PULL_DELETE;
        deletedPathList = null;
    }

    public CMFileSyncEventCompletePullDelete(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // number of elements of deletedPathList
        byteNum += Integer.BYTES;
        // deletedPathList
        if (deletedPathList != null) {
            for (String pathStr : deletedPathList) {
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + pathStr.getBytes().length;
            }
        }
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // number of elements of deletedPathList
        if (deletedPathList != null) {
            m_bytes.putInt(deletedPathList.size());
            // deletedPathList (already normalized to forward slashes)
            for (String pathStr : deletedPathList) {
                putStringToByteBuffer(pathStr);
            }
        } else {
            m_bytes.putInt(0);
        }
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // number of elements of deletedPathList
        int numElements = msg.getInt();
        if (numElements > 0) {
            deletedPathList = new ArrayList<>();
            for (int i = 0; i < numElements; i++) {
                deletedPathList.add(getStringFromByteBuffer(msg));
            }
        }
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompletePullDelete{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", deletedPathList=" + deletedPathList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventCompletePullDelete that = (CMFileSyncEventCompletePullDelete) o;
        return getInitiatorName().equals(that.getInitiatorName()) &&
                Objects.equals(deletedPathList, that.deletedPathList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), deletedPathList);
    }

    /**
     * gets the list of deleted file paths.
     * @return list of deleted file paths
     */
    public List<String> getDeletedPathList() {
        return deletedPathList;
    }

    public void setDeletedPathList(List<String> deletedPathList) {
        this.deletedPathList = deletedPathList;
    }
}
