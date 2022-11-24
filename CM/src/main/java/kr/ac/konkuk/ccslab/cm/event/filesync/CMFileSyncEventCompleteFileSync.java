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
    private String userName;    // user name
    private int numFilesCompleted;  // number of files completed

    public CMFileSyncEventCompleteFileSync() {
        m_nID = CMFileSyncEvent.COMPLETE_FILE_SYNC;
        userName = null;
        numFilesCompleted = 0;
    }

    public CMFileSyncEventCompleteFileSync(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // userName
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
        // numFilesCompleted
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // userName
        putStringToByteBuffer(userName);
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // userName
        userName = getStringFromByteBuffer(msg);
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompleteFileSync{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", userName='" + userName + '\'' +
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
        return numFilesCompleted == that.numFilesCompleted && userName.equals(that.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, numFilesCompleted);
    }

    /**
     * gets the target user (client) name.
     *
     * @return user (client) name
     */
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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
