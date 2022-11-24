package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server replies to the client about
 * the completion of receiving the list of requested files to be changed to the local mode.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndLocalModeListAck extends CMFileSyncEvent {
    private String requester;
    private int numLocalModeFiles;
    private int returnCode;

    public CMFileSyncEventEndLocalModeListAck() {
        m_nID = CMFileSyncEvent.END_LOCAL_MODE_LIST_ACK;
        requester = null;   // must not be null
        numLocalModeFiles = 0;
        returnCode = -1;
    }

    public CMFileSyncEventEndLocalModeListAck(ByteBuffer msg) {
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
        // returnCode
        byteNum += Integer.BYTES;

        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // requester
        putStringToByteBuffer(requester);
        // numLocalModeFiles
        m_bytes.putInt(numLocalModeFiles);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // requester
        requester = getStringFromByteBuffer(msg);
        // numLocalModeFiles
        numLocalModeFiles = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndLocalModeListAck{" +
                "requester='" + requester + '\'' +
                ", numLocalModeFiles=" + numLocalModeFiles +
                ", returnCode=" + returnCode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventEndLocalModeListAck that = (CMFileSyncEventEndLocalModeListAck) o;
        return numLocalModeFiles == that.numLocalModeFiles && returnCode == that.returnCode && requester.equals(that.requester);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requester, numLocalModeFiles, returnCode);
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
