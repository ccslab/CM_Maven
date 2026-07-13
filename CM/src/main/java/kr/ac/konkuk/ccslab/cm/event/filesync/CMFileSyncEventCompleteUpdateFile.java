package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * the completion of update of a modified file for synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompleteUpdateFile extends CMFileSyncEvent {
    // Fields: userName, completedPath, cursor
    private String completedPath;     // completed path
    private long cursor;            // updated lastChangeId to be applied on client side

    public CMFileSyncEventCompleteUpdateFile() {
        m_nID = CMFileSyncEvent.COMPLETE_UPDATE_FILE;
        completedPath = null;
        cursor = -1;
    }

    public CMFileSyncEventCompleteUpdateFile(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    /** @deprecated Use {@link #getInitiatorName()} instead. */
    @Deprecated
    public String getUserName() { return getInitiatorName(); }

    /** @deprecated Use {@link #setInitiatorName(String)} instead. */
    @Deprecated
    public void setUserName(String name) { setInitiatorName(name); }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // completedPath
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + completedPath.getBytes().length;
        // cursor
        byteNum += Long.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // completedPath
        putStringToByteBuffer(completedPath);
        // cursor
        m_bytes.putLong(cursor);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // completedPath
        completedPath = getStringFromByteBuffer(msg);
        // cursor
        cursor = msg.getLong();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompleteUpdateFile{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", completedPath=" + completedPath +
                ", cursor=" + cursor +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventCompleteUpdateFile fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getCompletedPath().equals(completedPath);
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventCompleteUpdateFile that = (CMFileSyncEventCompleteUpdateFile) o;
        return cursor == that.cursor &&
                getInitiatorName().equals(that.getInitiatorName()) &&
                completedPath.equals(that.completedPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), completedPath, cursor);
    }


    /**
     * gets the path of the update file.
     * @return path of the updated file
     */
    public String getCompletedPath() {
        return completedPath;
    }

    public void setCompletedPath(String completedPath) {
        this.completedPath = completedPath;
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
