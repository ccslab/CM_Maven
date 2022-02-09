package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CMFileSyncEventOnlineModeList extends CMFileSyncEvent {
    private String requester;
    private List<Path> relativePathList;

    public CMFileSyncEventOnlineModeList() {
        m_nID = CMFileSyncEvent.ONLINE_MODE_LIST;
        requester = null;   // must not null
        relativePathList = null;    // must not null
    }

    public CMFileSyncEventOnlineModeList(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // requester
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + requester.getBytes().length;
        // size of list
        byteNum += Integer.BYTES;
        // relativePathList (must not null)
        for (Path path : relativePathList) {
            byteNum += CMInfo.STRING_LEN_BYTES_LEN + path.toString().getBytes().length;
        }

        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // requester
        putStringToByteBuffer(requester);
        // numCurrentFiles
        m_bytes.putInt(relativePathList.size());
        // relativePathList
        for (Path path : relativePathList) {
            putStringToByteBuffer(path.toString());
        }
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        int listSize;
        // requester
        requester = getStringFromByteBuffer(msg);
        // list size
        listSize = msg.getInt();
        // relativePathList
        relativePathList = new ArrayList<>();
        for (int i = 0; i < listSize; i++) {
            Path relativePath = Paths.get(getStringFromByteBuffer(msg));
            relativePathList.add(relativePath);
        }
    }

    @Override
    public String toString() {
        return "CMFileSyncEventOnlineModeList{" +
                "requester='" + requester + '\'' +
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
        return requester.equals(that.requester) && relativePathList.equals(that.relativePathList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requester, relativePathList);
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public List<Path> getRelativePathList() {
        return relativePathList;
    }

    public void setRelativePathList(List<Path> relativePathList) {
        this.relativePathList = relativePathList;
    }
}
