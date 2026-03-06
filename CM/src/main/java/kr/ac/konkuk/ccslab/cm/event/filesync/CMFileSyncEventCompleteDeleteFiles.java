package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * the completion of file deletion(s) for synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompleteDeleteFiles extends CMFileSyncEvent {
    // Fields: deletedPathList, cursor
    private List<Path> deletedPathList; // list of deleted file paths
    private long cursor;                // updated lastChangeId to be applied on client side

    public CMFileSyncEventCompleteDeleteFiles() {
        m_nID = CMFileSyncEvent.COMPLETE_DELETE_FILES;
        deletedPathList = null;
        cursor = -1;
    }

    public CMFileSyncEventCompleteDeleteFiles(ByteBuffer msg) {
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
            for (Path path : deletedPathList) {
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + path.toString().getBytes().length;
            }
        }
        // cursor
        byteNum += Long.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // number of elements of deletedPathList
        if (deletedPathList != null) {
            m_bytes.putInt(deletedPathList.size());
            // deletedPathList
            for (Path path : deletedPathList) {
                putStringToByteBuffer(path.toString());
            }
        } else {
            m_bytes.putInt(0);
        }
        // cursor
        m_bytes.putLong(cursor);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // number of elements of deletedPathList
        int numElements = msg.getInt();
        if (numElements > 0) {
            deletedPathList = new ArrayList<>();
            for (int i = 0; i < numElements; i++) {
                deletedPathList.add(Paths.get(getStringFromByteBuffer(msg)));
            }
        }
        // cursor
        cursor = msg.getLong();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompleteDeleteFiles{" +
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
                ", cursor=" + cursor +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventCompleteDeleteFiles that = (CMFileSyncEventCompleteDeleteFiles) o;
        return cursor == that.cursor &&
                getInitiatorName().equals(that.getInitiatorName()) &&
                Objects.equals(deletedPathList, that.deletedPathList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), deletedPathList, cursor);
    }

    /**
     * gets the list of deleted file paths.
     * @return list of deleted file paths
     */
    public List<Path> getDeletedPathList() {
        return deletedPathList;
    }

    public void setDeletedPathList(List<Path> deletedPathList) {
        this.deletedPathList = deletedPathList;
    }

    /**
     * gets the updated lastChangeId to be applied on the client side.
     * @return updated lastChangeId (cursor)
     */
    public long getCursor() {
        return cursor;
    }

    public void setCursor(long cursor) {
        this.cursor = cursor;
    }
}
