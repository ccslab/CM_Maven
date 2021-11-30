package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;

public class CMFileSyncEventStartFileListAck extends CMFileSyncEvent {
    private String userName;    // user name
    private int numTotalFiles;  // number of total files
    private int returnCode;     // return code

    public CMFileSyncEventStartFileListAck() {
        m_nID = CMFileSyncEvent.START_FILE_LIST_ACK;
        userName = null;
        numTotalFiles = 0;
        returnCode = -1;
    }

    public CMFileSyncEventStartFileListAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // userName
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
        // numTotalFiles
        byteNum += Integer.BYTES;
        // returnCode
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // userName
        putStringToByteBuffer(userName);
        // numTotalFiles
        m_bytes.putInt(numTotalFiles);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // userName
        userName = getStringFromByteBuffer(msg);
        // numTotalFiles
        numTotalFiles = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartFileListAck{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nByteNum=" + m_nByteNum +
                ", userName='" + userName + '\'' +
                ", numTotalFiles=" + numTotalFiles +
                ", returnCode=" + returnCode +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventStartFileListAck fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getNumTotalFiles() == numTotalFiles &&
                fse.getReturnCode() == returnCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getNumTotalFiles() {
        return numTotalFiles;
    }

    public void setNumTotalFiles(int numTotalFiles) {
        this.numTotalFiles = numTotalFiles;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
