package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * skipping update of a file for synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventSkipUpdateFile extends CMFileSyncEvent {
    // Fields: userName, skippedPath
    private String skippedPath;     // skipped path

    public CMFileSyncEventSkipUpdateFile() {
        m_nID = CMFileSyncEvent.SKIP_UPDATE_FILE;
        skippedPath = null;
    }

    public CMFileSyncEventSkipUpdateFile(ByteBuffer msg) {
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
        // skippedPath
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + skippedPath.getBytes().length;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // completedPath
        putStringToByteBuffer(skippedPath);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // completedPath
        skippedPath = getStringFromByteBuffer(msg);
    }

    @Override
    public String toString() {
        return "CMFileSyncEventSkipUpdateFile{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", skippedPath=" + skippedPath +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventSkipUpdateFile fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getSkippedPath().equals(skippedPath);
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventSkipUpdateFile that = (CMFileSyncEventSkipUpdateFile) o;
        return getInitiatorName().equals(that.getInitiatorName()) && skippedPath.equals(that.skippedPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), skippedPath);
    }


    /**
     * gets the file path that does not need to be synchronized.
     * @return a skipped file path
     * <br>The path is a relative path from the synchronization home directory.
     */
    public String getSkippedPath() {
        return skippedPath;
    }

    public void setSkippedPath(String skippedPath) {
        this.skippedPath = skippedPath;
    }
}
