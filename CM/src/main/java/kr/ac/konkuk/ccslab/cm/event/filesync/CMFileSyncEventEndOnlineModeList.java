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
    private String requester;
    private int numOnlineModeFiles;

    public CMFileSyncEventEndOnlineModeList() {
        m_nID = CMFileSyncEvent.END_ONLINE_MODE_LIST;
        requester = null;   // must not be null
        numOnlineModeFiles = 0;
    }

    public CMFileSyncEventEndOnlineModeList(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // requester
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + requester.getBytes().length;
        // numOnlineModeFiles
        byteNum += Integer.BYTES;

        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // requester
        putStringToByteBuffer(requester);
        // numOnlineModeFiles
        m_bytes.putInt(numOnlineModeFiles);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // requester
        requester = getStringFromByteBuffer(msg);
        // numOnlineModeFiles
        numOnlineModeFiles = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndOnlineMode{" +
                "requester='" + requester + '\'' +
                ", numOnlineModeFiles=" + numOnlineModeFiles +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndOnlineModeList that = (CMFileSyncEventEndOnlineModeList) o;
        return numOnlineModeFiles == that.numOnlineModeFiles && requester.equals(that.requester);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requester, numOnlineModeFiles);
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
