package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileType;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client sends the server
 * a list of file entries that will be synchronized.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventFileEntries extends CMFileSyncEvent {
    private String userName;    // user name
    private int numFilesCompleted;  // number of files completed
    private int numFiles;       // number of current files
    private List<CMFileSyncEntry> clientPathEntryList;    // list of CMFileSyncEntry

    public CMFileSyncEventFileEntries() {
        m_nID = CMFileSyncEvent.FILE_ENTRIES;
        userName = null;
        numFilesCompleted = 0;
        numFiles = 0;
        clientPathEntryList = null;
    }

    public CMFileSyncEventFileEntries(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    public int getByteNum() {
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
        if(clientPathEntryList != null) {
            for (CMFileSyncEntry entry : clientPathEntryList) {
                // Path pathRelativeToHome
                byteNum += CMInfo.STRING_LEN_BYTES_LEN +
                        entry.getPathRelativeToHome().toString().getBytes().length;
                // long size
                byteNum += Long.BYTES;
                // FileTime lastModifiedTime -> long type of milliseconds
                byteNum += Long.BYTES;
                // CMFileType -> int
                byteNum += Integer.BYTES;
            }
        }
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
        if(clientPathEntryList != null) {
            // number of elements of fileEntryList
            m_bytes.putInt(clientPathEntryList.size());
            // fileEntryList
            for (CMFileSyncEntry entry : clientPathEntryList) {
                // Path relativePathToHome
                putStringToByteBuffer(entry.getPathRelativeToHome().toString());
                // long size
                m_bytes.putLong(entry.getSize());
                // FileTime lastModifiedTime (changed to long milliseconds)
                m_bytes.putLong(entry.getLastModifiedTime().toMillis());
                // CMFileType (enum -> int)
                m_bytes.putInt(entry.getType().ordinal());
            }
        }
        else
            m_bytes.putInt(0);
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
            clientPathEntryList = new ArrayList<>();
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
                // CMFileType (int -> enum)
                CMFileType type = CMFileType.values()[msg.getInt()];
                entry.setType(type);
                // add to the entry list
                clientPathEntryList.add(entry);
            }
        }
    }

    @Override
    public String toString() {
        return "CMFileSyncEventFileEntries{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nByteNum=" + m_nByteNum +
                ", userName='" + userName + '\'' +
                ", numFilesCompleted=" + numFilesCompleted +
                ", numFiles=" + numFiles +
                ", fileEntryList=" + clientPathEntryList +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventFileEntries fse)) return false;
        return fse.getUserName().equals(userName) &&
                fse.getNumFilesCompleted() == numFilesCompleted &&
                fse.getNumFiles() == numFiles &&
                fse.getClientPathEntryList().equals(clientPathEntryList);
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventFileEntries that = (CMFileSyncEventFileEntries) o;
        return numFilesCompleted == that.numFilesCompleted &&
                numFiles == that.numFiles &&
                userName.equals(that.userName) &&
                clientPathEntryList.equals(that.clientPathEntryList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, numFilesCompleted, numFiles, clientPathEntryList);
    }

    /**
     * gets user (client) name.
     * @return user (client) name
     */
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * gets the number of file entries that has been already transferred.
     * @return number of file entries that has been already transferred
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }

    /**
     * gets the number of file entries in this event.
     * @return number of file entries in this event
     */
    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }

    /**
     * gets the list of file entries in this event.
     * @return a list of file entries
     */
    public List<CMFileSyncEntry> getClientPathEntryList() {
        return clientPathEntryList;
    }

    public void setClientPathEntryList(List<CMFileSyncEntry> clientPathEntryList) {
        this.clientPathEntryList = clientPathEntryList;
    }
}
