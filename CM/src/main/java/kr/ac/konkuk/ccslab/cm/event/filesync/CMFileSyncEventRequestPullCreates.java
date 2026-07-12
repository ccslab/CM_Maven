package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * pull-sync 의 CREATE 대상 파일들을 서버가 클라이언트로 push 하도록 요청하는 이벤트.
 * 클라이언트가 서버로 보내며, requestedFileList 의 각 path 는 FileSyncHome 기준 상대경로다.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventRequestPullCreates extends CMFileSyncEvent {
    private int numRequestedFiles;
    private List<Path> requestedFileList;

    public CMFileSyncEventRequestPullCreates() {
        m_nID = CMFileSyncEvent.REQUEST_PULL_CREATES;
        numRequestedFiles = 0;
        requestedFileList = null;
    }

    public CMFileSyncEventRequestPullCreates(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    public int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        byteNum += Integer.BYTES;   // numRequestedFiles
        byteNum += Integer.BYTES;   // requestedFileList size
        if (requestedFileList != null) {
            for (Path path : requestedFileList) {
                byteNum += CMInfo.STRING_LEN_BYTES_LEN +
                        path.toString().replace('\\', '/').getBytes().length;
            }
        }
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        m_bytes.putInt(numRequestedFiles);
        if (requestedFileList != null) {
            m_bytes.putInt(requestedFileList.size());
            for (Path path : requestedFileList) {
                putStringToByteBuffer(path.toString().replace('\\', '/'));
            }
        } else {
            m_bytes.putInt(0);
        }
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        numRequestedFiles = msg.getInt();
        int numElements = msg.getInt();
        if (numElements > 0) {
            requestedFileList = new ArrayList<>();
            for (int i = 0; i < numElements; i++) {
                requestedFileList.add(Paths.get(getStringFromByteBuffer(msg)));
            }
        }
    }

    @Override
    public String toString() {
        return "CMFileSyncEventRequestPullCreates{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", initiatorUuid=" + getInitiatorUuid() +
                ", initiatorDeviceUuid=" + getInitiatorDeviceUuid() +
                ", numRequestedFiles=" + numRequestedFiles +
                ", requestedFileList=" + requestedFileList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventRequestPullCreates that = (CMFileSyncEventRequestPullCreates) o;
        return numRequestedFiles == that.numRequestedFiles &&
                Objects.equals(getInitiatorName(), that.getInitiatorName()) &&
                Objects.equals(getInitiatorUuid(), that.getInitiatorUuid()) &&
                Objects.equals(getInitiatorDeviceUuid(), that.getInitiatorDeviceUuid()) &&
                Objects.equals(requestedFileList, that.requestedFileList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), getInitiatorUuid(), getInitiatorDeviceUuid(),
                numRequestedFiles, requestedFileList);
    }

    public int getNumRequestedFiles() {
        return numRequestedFiles;
    }

    public void setNumRequestedFiles(int numRequestedFiles) {
        this.numRequestedFiles = numRequestedFiles;
    }

    public List<Path> getRequestedFileList() {
        return requestedFileList;
    }

    public void setRequestedFileList(List<Path> requestedFileList) {
        this.requestedFileList = requestedFileList;
    }
}
