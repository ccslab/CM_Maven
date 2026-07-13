package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client sends the server
 * a list of online mode file paths that will be changed to the local mode.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventLocalModeList extends CMFileSyncEvent {
    private List<String> relativePathList; // forward-slash separated relative paths

    public CMFileSyncEventLocalModeList() {
        m_nID = CMFileSyncEvent.LOCAL_MODE_LIST;
        relativePathList = null;    // must not be null
    }

    public CMFileSyncEventLocalModeList(ByteBuffer msg) {
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
    public int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // size of list
        byteNum += Integer.BYTES;
        // relativePathList (can be null by calling the method before setting a list)
        if(relativePathList != null) {
            for (String pathStr : relativePathList) {
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + pathStr.getBytes().length;
            }
        }

        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numCurrentFiles
        m_bytes.putInt(relativePathList.size());
        // relativePathList (already normalized to forward slashes)
        for (String pathStr : relativePathList) {
            putStringToByteBuffer(pathStr);
        }
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        int listSize;
        // list size
        listSize = msg.getInt();
        // relativePathList
        relativePathList = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            relativePathList.add(getStringFromByteBuffer(msg));
        }
    }

    @Override
    public String toString() {
        return "CMFileSyncEventLocalModeList{" +
                "initiatorName='" + getInitiatorName() + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", relativePathList=" + relativePathList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventLocalModeList that = (CMFileSyncEventLocalModeList) o;
        return getInitiatorName().equals(that.getInitiatorName()) && relativePathList.equals(that.relativePathList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), relativePathList);
    }


    /**
     * gets the list of online mode file paths that will be changed to the local mode.
     * @return a list of online mode file paths
     * <br> The path is a relative path from the synchronization home directory.
     */
    public List<String> getRelativePathList() {
        return relativePathList;
    }

    public void setRelativePathList(List<String> relativePathList) {
        this.relativePathList = relativePathList;
    }
}
