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
    private String userName;    // user name
    private int numTotalFiles;  // number of total files

    public CMFileSyncEventStartFileList() {
        m_nID = CMFileSyncEvent.START_FILE_LIST;
        userName = null;
        numTotalFiles = 0;
    }

    public CMFileSyncEventStartFileList(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    public int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // userName
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
        // numTotalFiles
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // userName
        putStringToByteBuffer(userName);
        // numTotalFiles
        m_bytes.putInt(numTotalFiles);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // userName
        userName = getStringFromByteBuffer(msg);
        // numTotalFiles
        numTotalFiles = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartFileList{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", userName='" + userName + '\'' +
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
        return numTotalFiles == that.numTotalFiles && userName.equals(that.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, numTotalFiles);
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
