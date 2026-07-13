package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client notifies the server of
 * the completion of sending the list of requested files to be changed to the online mode.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndOnlineModeList extends CMFileSyncEvent {
    private int numOnlineModeFiles;

    public CMFileSyncEventEndOnlineModeList() {
        m_nID = CMFileSyncEvent.END_ONLINE_MODE_LIST;
        numOnlineModeFiles = 0;
    }

    public CMFileSyncEventEndOnlineModeList(ByteBuffer msg) {
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
        // numOnlineModeFiles
        byteNum += Integer.BYTES;

        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numOnlineModeFiles
        m_bytes.putInt(numOnlineModeFiles);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numOnlineModeFiles
        numOnlineModeFiles = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndOnlineMode{" +
                "initiatorName='" + getInitiatorName() + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", numOnlineModeFiles=" + numOnlineModeFiles +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndOnlineModeList that = (CMFileSyncEventEndOnlineModeList) o;
        return numOnlineModeFiles == that.numOnlineModeFiles && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numOnlineModeFiles);
    }


    /**
     * gets the number of requested files to be changed to the online mode.
     * @return number of requested local mode files
     */
    public int getNumOnlineModeFiles() {
        return numOnlineModeFiles;
    }

    public void setNumOnlineModeFiles(int numOnlineModeFiles) {
        this.numOnlineModeFiles = numOnlineModeFiles;
    }
}
