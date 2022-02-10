package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

public class CMFileSyncEventEndOnlineMode extends CMFileSyncEvent {
    private String requester;
    private int numOnlineModeFiles;

    public CMFileSyncEventEndOnlineMode() {
        m_nID = CMFileSyncEvent.END_ONLINE_MODE_LIST;
        requester = null;   // must not be null
        numOnlineModeFiles = 0;
    }

    public CMFileSyncEventEndOnlineMode(ByteBuffer msg) {
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
        CMFileSyncEventEndOnlineMode that = (CMFileSyncEventEndOnlineMode) o;
        return numOnlineModeFiles == that.numOnlineModeFiles && requester.equals(that.requester);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requester, numOnlineModeFiles);
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public int getNumOnlineModeFiles() {
        return numOnlineModeFiles;
    }

    public void setNumOnlineModeFiles(int numOnlineModeFiles) {
        this.numOnlineModeFiles = numOnlineModeFiles;
    }
}
