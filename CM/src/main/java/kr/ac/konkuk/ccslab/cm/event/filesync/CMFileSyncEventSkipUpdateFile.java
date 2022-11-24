package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * skipping update of a file for synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventSkipUpdateFile extends CMFileSyncEvent {
    // Fields: userName, skippedPath
    private String userName;    // user name
    private Path skippedPath;     // skipped path

    public CMFileSyncEventSkipUpdateFile() {
        m_nID = CMFileSyncEvent.SKIP_UPDATE_FILE;
        userName = null;
        skippedPath = null;
    }

    public CMFileSyncEventSkipUpdateFile(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // userName
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
        // skippedPath
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + skippedPath.toString().getBytes().length;
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // userName
        putStringToByteBuffer(userName);
        // completedPath
        putStringToByteBuffer(skippedPath.toString());
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // userName
        userName = getStringFromByteBuffer(msg);
        // completedPath
        skippedPath = Paths.get(getStringFromByteBuffer(msg));
    }

    @Override
    public String toString() {
        return "CMFileSyncEventSkipUpdateFile{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", userName='" + userName + '\'' +
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
        return userName.equals(that.userName) && skippedPath.equals(that.skippedPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, skippedPath);
    }

    /**
     * gets the user (client) name.
     * @return user (client) name
     */
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * gets the file path that does not need to be synchronized.
     * @return a skipped file path
     * <br>The path is a relative path from the synchronization home directory.
     */
    public Path getSkippedPath() {
        return skippedPath;
    }

    public void setSkippedPath(Path skippedPath) {
        this.skippedPath = skippedPath;
    }
}
