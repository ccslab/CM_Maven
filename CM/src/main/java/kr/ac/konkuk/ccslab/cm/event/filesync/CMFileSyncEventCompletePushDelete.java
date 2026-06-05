package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client, during
 * incremental push synchronization, of the list of files it has finished deleting from its
 * sync home.
 * <br>If the list is long, it can be split across multiple events.
 * <br>This is the reverse-direction counterpart of {@link CMFileSyncEventCompletePullDelete},
 * which the client sends to the server in the pull synchronization.
 * <br>Note: a pendingPushMap-triggered push never produces DELETE ops, but a file-watcher
 * triggered push can; both triggers share the same protocol.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompletePushDelete extends CMFileSyncEvent {
    // Fields: deletedPathList
    private List<String> deletedPathList; // list of deleted file paths relative to sync home (forward-slash separated)

    public CMFileSyncEventCompletePushDelete() {
        m_nID = CMFileSyncEvent.COMPLETE_PUSH_DELETE;
        deletedPathList = null;
    }

    public CMFileSyncEventCompletePushDelete(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    public int getByteNum() {
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
        return "CMFileSyncEventCompletePushDelete{" +
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
        CMFileSyncEventCompletePushDelete that = (CMFileSyncEventCompletePushDelete) o;
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
