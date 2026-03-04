package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client notifies the server of
 * the start of sending a list of file entries for synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventStartFileList extends CMFileSyncEvent {
    private int numTotalFiles;  // number of total files

    public CMFileSyncEventStartFileList() {
        m_nID = CMFileSyncEvent.START_FILE_LIST;
        numTotalFiles = 0;
    }

    public CMFileSyncEventStartFileList(ByteBuffer msg) {
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
    public int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numTotalFiles
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numTotalFiles
        m_bytes.putInt(numTotalFiles);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numTotalFiles
        numTotalFiles = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartFileList{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", numTotalFiles=" + numTotalFiles +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventStartFileList fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getNumTotalFiles() == numTotalFiles;
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventStartFileList that = (CMFileSyncEventStartFileList) o;
        return numTotalFiles == that.numTotalFiles && getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numTotalFiles);
    }


    /**
     * gets the total number of file entries to be synchronized.
     * @return total number of file entries
     */
    public int getNumTotalFiles() {
        return numTotalFiles;
    }

    public void setNumTotalFiles(int numTotalFiles) {
        this.numTotalFiles = numTotalFiles;
    }
}
