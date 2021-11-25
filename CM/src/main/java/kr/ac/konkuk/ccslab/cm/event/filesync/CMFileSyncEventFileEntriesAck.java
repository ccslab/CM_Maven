package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

public class CMFileSyncEventFileEntriesAck extends CMFileSyncEvent {
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList, returnCode
    private String userName;    // user name
    private int numFilesCompleted;  // number of files completed
    private int numFiles;       // number of current files
    private List<CMFileSyncEntry> fileEntryList;    // list of CMFileSyncEntry
    private int returnCode;     // return code

    public CMFileSyncEventFileEntriesAck() {
        m_nID = CMFileSyncEvent.FILE_ENTRIES_ACK;
        userName = null;
        numFilesCompleted = 0;
        numFiles = 0;
        fileEntryList = null;
        returnCode = -1;
    }

    public CMFileSyncEventFileEntriesAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // userName
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
        // numFilesCompleted
        byteNum += Integer.BYTES;
        // numFiles
        byteNum += Integer.BYTES;
        // number of elements of fileEntryList
        byteNum += Integer.BYTES;
        // fileEntryList (Path pathRelativeToHome, long size, FileTime lastModifiedTime)
        if(fileEntryList != null) {
            for (CMFileSyncEntry entry : fileEntryList) {
                // Path pathRelativeToHome
                byteNum += CMInfo.STRING_LEN_BYTES_LEN +
                        entry.getPathRelativeToHome().toString().getBytes().length;
                // long size
                byteNum += Long.BYTES;
                // FileTime lastModifiedTime -> long type of milliseconds
                byteNum += Long.BYTES;
            }
        }
        // returnCode
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // userName
        putStringToByteBuffer(userName);
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
        // numFiles
        m_bytes.putInt(numFiles);
        if(fileEntryList != null) {
            // number of elements of fileEntryList
            m_bytes.putInt(fileEntryList.size());
            // fileEntryList
            for (CMFileSyncEntry entry : fileEntryList) {
                // Path relativePathToHome
                putStringToByteBuffer(entry.getPathRelativeToHome().toString());
                // long size
                m_bytes.putLong(entry.getSize());
                // FileTime lastModifiedTime (changed to long milliseconds)
                m_bytes.putLong(entry.getLastModifiedTime().toMillis());
            }
        }
        else
            m_bytes.putInt(0);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        int numFileEntries;

        // userName
        userName = getStringFromByteBuffer(msg);
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
        // numFiles
        numFiles = msg.getInt();
        // fileEntryList
        numFileEntries = msg.getInt();
        if(numFileEntries > 0){
            // create a new entry list
            fileEntryList = new ArrayList<>();
            for (int i = 0; i < numFileEntries; i++) {
                CMFileSyncEntry entry = new CMFileSyncEntry();
                // Path relativePathToHome
                Path relativePath = Paths.get(getStringFromByteBuffer(msg));
                entry.setPathRelativeToHome(relativePath);
                // long size
                entry.setSize(msg.getLong());
                // FileTime lastModifiedTime
                FileTime lastModifiedTime = FileTime.fromMillis(msg.getLong());
                entry.setLastModifiedTime(lastModifiedTime);
                // add to the entry list
                fileEntryList.add(entry);
            }
        }
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventFileEntriesAck{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nByteNum=" + m_nByteNum +
                ", userName='" + userName + '\'' +
                ", numFilesCompleted=" + numFilesCompleted +
                ", numFiles=" + numFiles +
                ", fileEntryList=" + fileEntryList +
                ", returnCode=" + returnCode +
                '}';
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }

    public List<CMFileSyncEntry> getFileEntryList() {
        return fileEntryList;
    }

    public void setFileEntryList(List<CMFileSyncEntry> fileEntryList) {
        this.fileEntryList = fileEntryList;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
