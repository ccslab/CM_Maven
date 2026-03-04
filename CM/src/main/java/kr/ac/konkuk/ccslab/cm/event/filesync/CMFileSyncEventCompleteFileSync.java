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
    // Fields: userName, numFilesCompleted
    private int numFilesCompleted;  // number of files completed

    public CMFileSyncEventCompleteFileSync() {
        m_nID = CMFileSyncEvent.COMPLETE_FILE_SYNC;
        numFilesCompleted = 0;
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
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
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
        return numFilesCompleted == that.numFilesCompleted && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numFilesCompleted);
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
}
