package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server acknowledges the reception of
 * the list of local mode file paths that will be changed to the online mode.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventOnlineModeListAck extends CMFileSyncEvent {
    private List<Path> relativePathList;
    private int returnCode;

    public CMFileSyncEventOnlineModeListAck() {
        m_nID = CMFileSyncEvent.ONLINE_MODE_LIST_ACK;
        relativePathList = null;    // must not be null
        returnCode = -1;            // initial value
    }

    public CMFileSyncEventOnlineModeListAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    /** @deprecated Use {@link #getInitiatorName()} instead. */
    @Deprecated
    public String getRequester() { return getInitiatorName(); }

    /** @deprecated Use {@link #setInitiatorName(String)} instead. */
    @Deprecated
    public void setRequester(String name) { setInitiatorName(name); }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // size of list
        byteNum += Integer.BYTES;
        // relativePathList (must not null)
        for (Path path : relativePathList) {
            byteNum += CMInfo.STRING_LEN_BYTES_LEN + path.toString().getBytes().length;
        }
        // returnCode
        byteNum += Integer.BYTES;

        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numCurrentFiles
        m_bytes.putInt(relativePathList.size());
        // relativePathList
        for (Path path : relativePathList) {
            putStringToByteBuffer(path.toString());
        }
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        int listSize;
        // list size
        listSize = msg.getInt();
        // relativePathList
        relativePathList = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            Path relativePath = Paths.get(getStringFromByteBuffer(msg));
            relativePathList.add(relativePath);
        }
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventOnlineModeListAck{" +
                "initiatorName='" + getInitiatorName() + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", relativePathList=" + relativePathList +
                ", returnCode=" + returnCode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventOnlineModeListAck that = (CMFileSyncEventOnlineModeListAck) o;
        return returnCode == that.returnCode && getInitiatorName().equals(that.getInitiatorName()) && relativePathList.equals(that.relativePathList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), relativePathList, returnCode);
    }


    /**
     * gets the list of local mode file paths that will be changed to the online mode.
     * @return a list of local mode file paths
     * <br>The path is a relative path from the synchronization home directory.
     */
    public List<Path> getRelativePathList() {
        return relativePathList;
    }

    public void setRelativePathList(List<Path> relativePathList) {
        this.relativePathList = relativePathList;
    }

    /**
     * gets the return code.
     * @return return code
     * <br>1 if successful; 0 otherwise.
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
