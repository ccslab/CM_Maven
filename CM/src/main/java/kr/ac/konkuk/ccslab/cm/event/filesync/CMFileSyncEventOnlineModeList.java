package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client sends the server
 * a list of local mode file paths that will be changed to the online mode.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventOnlineModeList extends CMFileSyncEvent {
    private List<String> relativePathList; // forward-slash separated relative paths

    public CMFileSyncEventOnlineModeList() {
        m_nID = CMFileSyncEvent.ONLINE_MODE_LIST;
        relativePathList = null;    // must not be null
    }

    public CMFileSyncEventOnlineModeList(ByteBuffer msg) {
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
        return "CMFileSyncEventOnlineModeList{" +
                "initiatorName='" + getInitiatorName() + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", relativePathList=" + relativePathList +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventOnlineModeList fse)) return false;
        return fse.getRequester().equals(requester) &&
                fse.getRelativePathList().equals(relativePathList);
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventOnlineModeList that = (CMFileSyncEventOnlineModeList) o;
        return getInitiatorName().equals(that.getInitiatorName()) && relativePathList.equals(that.relativePathList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), relativePathList);
    }


    /**
     * gets the list of local mode file paths that will be changed to the online mode.
     * @return a list of local mode file paths
     * <br>The path is a relative path from the synchronization home directory.
     */
    public List<String> getRelativePathList() {
        return relativePathList;
    }

    public void setRelativePathList(List<String> relativePathList) {
        this.relativePathList = relativePathList;
    }
}
