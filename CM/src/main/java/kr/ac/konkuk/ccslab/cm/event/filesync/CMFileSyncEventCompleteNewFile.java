package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * the completion of transmission of a new file for synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompleteNewFile extends CMFileSyncEvent {
    // Fields: userName, completedPath
    private Path completedPath;     // completed path

    public CMFileSyncEventCompleteNewFile() {
        m_nID = CMFileSyncEvent.COMPLETE_NEW_FILE;
        completedPath = null;
    }

    public CMFileSyncEventCompleteNewFile(ByteBuffer msg) {
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
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + completedPath.toString().getBytes().length;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // completedPath
        putStringToByteBuffer(completedPath.toString());
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // completedPath
        completedPath = Paths.get(getStringFromByteBuffer(msg));
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompleteNewFile{" +
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
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventCompleteNewFile fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getCompletedPath().equals(completedPath);
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventCompleteNewFile that = (CMFileSyncEventCompleteNewFile) o;
        return getInitiatorName().equals(that.getInitiatorName()) && completedPath.equals(that.completedPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), completedPath);
    }


    /**
     * gets the new file path.
     * @return new file path
     */
    public Path getCompletedPath() {
        return completedPath;
    }

    public void setCompletedPath(Path completedPath) {
        this.completedPath = completedPath;
    }
}
