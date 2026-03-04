package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client notifies the server of
 * the completion of sending the list of requested files to be changed to the local mode.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndLocalModeList extends CMFileSyncEvent {
    private int numLocalModeFiles;

    public CMFileSyncEventEndLocalModeList() {
        m_nID = CMFileSyncEvent.END_LOCAL_MODE_LIST;
        numLocalModeFiles = 0;
    }

    public CMFileSyncEventEndLocalModeList(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    /** @deprecated Use {@link #getInitiatorName()} instead. */
    @Deprecated
    public String getRequester() { return getInitiatorName(); }

    /** @deprecated Use {@link #setInitiatorName(String)} instead. */
    @Deprecated
    public void setRequester(String name) { setInitiatorName(name); }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numLocalModeFiles
        byteNum += Integer.BYTES;

        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numLocalModeFiles
        m_bytes.putInt(numLocalModeFiles);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numLocalModeFiles
        numLocalModeFiles = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndLocalModeList{" +
                "initiatorName='" + getInitiatorName() + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", numLocalModeFiles=" + numLocalModeFiles +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndLocalModeList that = (CMFileSyncEventEndLocalModeList) o;
        return numLocalModeFiles == that.numLocalModeFiles && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numLocalModeFiles);
    }


    /**
     * gets the number of requested files to be changed to the local mode.
     * @return number of requested online mode files
     */
    public int getNumLocalModeFiles() {
        return numLocalModeFiles;
    }

    public void setNumLocalModeFiles(int numLocalModeFiles) {
        this.numLocalModeFiles = numLocalModeFiles;
    }
}
