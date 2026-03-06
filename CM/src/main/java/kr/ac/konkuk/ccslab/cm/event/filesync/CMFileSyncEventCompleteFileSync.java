package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * the completion of file sync operation.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompleteFileSync extends CMFileSyncEvent {
    // Fields: userName, numFilesCompleted, cursor
    private int numFilesCompleted;  // number of files completed
    private long cursor;            // updated lastChangeId to be applied on client side

    public CMFileSyncEventCompleteFileSync() {
        m_nID = CMFileSyncEvent.COMPLETE_FILE_SYNC;
        numFilesCompleted = 0;
        cursor = -1;
    }

    public CMFileSyncEventCompleteFileSync(ByteBuffer msg) {
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
        // numFilesCompleted
        byteNum += Integer.BYTES;
        // cursor
        byteNum += Long.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
        // cursor
        m_bytes.putLong(cursor);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
        // cursor
        cursor = msg.getLong();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompleteFileSync{" +
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
                ", cursor=" + cursor +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventCompleteFileSync fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getNumFilesCompleted() == numFilesCompleted;
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventCompleteFileSync that = (CMFileSyncEventCompleteFileSync) o;
        return numFilesCompleted == that.numFilesCompleted &&
                cursor == that.cursor &&
                getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numFilesCompleted, cursor);
    }


    /**
     * gets the number of files that completed synchronization.
     * <p>
     *     The number is the same as the number of files in the synchronization home directory.
     * </p>
     * @return number of files that completed synchronization
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
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
