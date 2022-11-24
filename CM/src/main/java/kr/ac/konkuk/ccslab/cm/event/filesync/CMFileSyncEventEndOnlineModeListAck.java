package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server replies to the client about
 * the completion of receiving the list of requested files to be changed to the online mode.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndOnlineModeListAck extends CMFileSyncEvent {
    private String requester;
    private int numOnlineModeFiles;
    private int returnCode;

    public CMFileSyncEventEndOnlineModeListAck() {
        m_nID = CMFileSyncEvent.END_ONLINE_MODE_LIST_ACK;
        requester = null;   // must not be null
        numOnlineModeFiles = 0;
        returnCode = -1;
    }

    public CMFileSyncEventEndOnlineModeListAck(ByteBuffer msg) {
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
        // returnCode
        byteNum += Integer.BYTES;

        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // requester
        putStringToByteBuffer(requester);
        // numOnlineModeFiles
        m_bytes.putInt(numOnlineModeFiles);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // requester
        requester = getStringFromByteBuffer(msg);
        // numOnlineModeFiles
        numOnlineModeFiles = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndOnlineModeAck{" +
                "requester='" + requester + '\'' +
                ", numOnlineModeFiles=" + numOnlineModeFiles +
                ", returnCode=" + returnCode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndOnlineModeListAck that = (CMFileSyncEventEndOnlineModeListAck) o;
        return numOnlineModeFiles == that.numOnlineModeFiles && returnCode == that.returnCode && requester.equals(that.requester);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requester, numOnlineModeFiles, returnCode);
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
     * gets the number of requested files to be changed to the online mode
     * @return number of requested local mode files
     */
    public int getNumOnlineModeFiles() {
        return numOnlineModeFiles;
    }

    public void setNumOnlineModeFiles(int numOnlineModeFiles) {
        this.numOnlineModeFiles = numOnlineModeFiles;
    }

    /**
     * gets the return code.
     * @return return code
     * <br> 1 if successful; 0 otherwise
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
