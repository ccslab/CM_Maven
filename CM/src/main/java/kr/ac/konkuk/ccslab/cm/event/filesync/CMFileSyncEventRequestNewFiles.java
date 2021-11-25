package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CMFileSyncEventRequestNewFiles extends CMFileSyncEvent {
    // Fields: String requesterName, int numRequestedFiles, List<Path> requestedFileList
    private String requesterName;   // requester name
    private int numRequestedFiles;  // number of requested files
    private List<Path> requestedFileList;   // list of requested files

    public CMFileSyncEventRequestNewFiles() {
        m_nID = CMFileSyncEvent.REQUEST_NEW_FILES;
        requesterName = null;
        numRequestedFiles = 0;
        requestedFileList = null;
    }

    public CMFileSyncEventRequestNewFiles(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    public int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // requesterName
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + requesterName.getBytes().length;
        // numRequestedFiles
        byteNum += Integer.BYTES;
        // number of elements of requestedFileList
        byteNum += Integer.BYTES;
        // requestedFileList
        if(requestedFileList != null) {
            for(Path path : requestedFileList) {
                byteNum += CMInfo.STRING_LEN_BYTES_LEN +
                        path.toString().getBytes().length;
            }
        }
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // requesterName
        putStringToByteBuffer(requesterName);
        // numRequestedFiles
        m_bytes.putInt(numRequestedFiles);
        if(requestedFileList != null) {
            // number of elements of requestedFileList
            m_bytes.putInt(requestedFileList.size());
            // requestedFileList
            for(Path path : requestedFileList) {
                putStringToByteBuffer(path.toString());
            }
        }
        else
            m_bytes.putInt(0);  // number of elements of requestedFileList
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        int numElementsOfRequestedFileList;

        // requesterName
        requesterName = getStringFromByteBuffer(msg);
        // numRequestedFiles
        numRequestedFiles = msg.getInt();
        // number of elements of requestedFileList
        numElementsOfRequestedFileList = msg.getInt();
        if(numElementsOfRequestedFileList > 0) {
            // create a new requestedFileList
            requestedFileList = new ArrayList<>();
            for(int i = 0; i < numElementsOfRequestedFileList; i++) {
                Path path = Paths.get(getStringFromByteBuffer(msg));
                requestedFileList.add(path);
            }
        }
    }

    @Override
    public String toString() {
        return "CMFileSyncEventRequestNewFiles{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", requesterName='" + requesterName + '\'' +
                ", numRequestedFiles=" + numRequestedFiles +
                ", requestedFileList=" + requestedFileList +
                '}';
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
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
