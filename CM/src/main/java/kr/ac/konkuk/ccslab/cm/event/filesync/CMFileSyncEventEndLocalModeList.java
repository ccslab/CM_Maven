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
    private String requester;
    private int numLocalModeFiles;

    public CMFileSyncEventEndLocalModeList() {
        m_nID = CMFileSyncEvent.END_LOCAL_MODE_LIST;
        requester = null;   // must not be null
        numLocalModeFiles = 0;
    }

    public CMFileSyncEventEndLocalModeList(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // requester
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + requester.getBytes().length;
        // numLocalModeFiles
        byteNum += Integer.BYTES;

        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // requester
        putStringToByteBuffer(requester);
        // numLocalModeFiles
        m_bytes.putInt(numLocalModeFiles);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // requester
        requester = getStringFromByteBuffer(msg);
        // numLocalModeFiles
        numLocalModeFiles = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndLocalModeList{" +
                "requester='" + requester + '\'' +
                ", numLocalModeFiles=" + numLocalModeFiles +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndLocalModeList that = (CMFileSyncEventEndLocalModeList) o;
        return numLocalModeFiles == that.numLocalModeFiles && requester.equals(that.requester);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requester, numLocalModeFiles);
    }

    /**
     * gets the requester (client) name.
     * @return requester (client) name
     */
    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
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
