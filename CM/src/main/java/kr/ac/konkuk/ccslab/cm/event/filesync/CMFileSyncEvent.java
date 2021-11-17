package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncEntry;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents CM events that are used for the file-sync task.
 *
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEvent extends CMEvent {

    // Fields: userName, numTotalFiles
    public static final int START_FILE_LIST = 1;
    // Fields: userName, numTotalFiles, returnCode
    public static final int START_FILE_LIST_ACK = 2;
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList
    public static final int FILE_ENTRIES = 3;
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList, returnCode
    public static final int FILE_ENTRIES_ACK = 4;
    // Fields: userName, numFilesCompleted
    public static final int END_FILE_LIST = 5;
    // Fields: userName, numFilesCompleted, returnCode
    public static final int END_FILE_LIST_ACK = 6;
    // Fields: String requesterName, int numRequestedFiles, List<Path> requestedFileList
    public static final int REQUEST_NEW_FILES = 7;

    // Fields: userName, completedPath
    public static final int COMPLETE_NEW_FILE = 14;
    // Fields: userName, completedPath
    public static final int COMPLETE_UPDATE_FILE = 15;
    // Fields: userName, numFilesCompleted
    public static final int COMPLETE_FILE_SYNC = 16;

    private String userName;    // user name
    private int numTotalFiles;  // number of total files
    private int returnCode;     // return code
    private int numFilesCompleted;  // number of files completed
    private int numFiles;       // number of current files
    private List<CMFileSyncEntry> fileEntryList;    // list of CMFileSyncEntry
    private String requesterName;   // requester name
    private int numRequestedFiles;  // number of requested files
    private List<Path> requestedFileList;   // list of requested files
    private Path completedPath;     // completed path

    public CMFileSyncEvent() {
        m_nType = CMInfo.CM_FILE_SYNC_EVENT;
        m_nID = -1;

        userName = null;
        numTotalFiles = 0;
        returnCode = -1;
        numFilesCompleted = 0;
        numFiles = 0;
        fileEntryList = null;
        requesterName = null;
        numRequestedFiles = 0;
        requestedFileList = null;
        completedPath = null;
    }

    public CMFileSyncEvent(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    public String getUserName() {
        return userName;
    }

    public CMFileSyncEvent setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public int getNumTotalFiles() {
        return numTotalFiles;
    }

    public CMFileSyncEvent setNumTotalFiles(int numTotalFiles) {
        this.numTotalFiles = numTotalFiles;
        return this;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public CMFileSyncEvent setReturnCode(int returnCode) {
        this.returnCode = returnCode;
        return this;
    }

    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public CMFileSyncEvent setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
        return this;
    }

    public int getNumFiles() {
        return numFiles;
    }

    public CMFileSyncEvent setNumFiles(int numFiles) {
        this.numFiles = numFiles;
        return this;
    }

    public List<CMFileSyncEntry> getFileEntryList() {
        return fileEntryList;
    }

    public CMFileSyncEvent setFileEntryList(List<CMFileSyncEntry> fileEntryList) {
        this.fileEntryList = fileEntryList;
        return this;
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

    public Path getCompletedPath() {
        return completedPath;
    }

    public void setCompletedPath(Path completedPath) {
        this.completedPath = completedPath;
    }

    @Override
    public int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();

        switch(m_nID) {
            case START_FILE_LIST:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numTotalFiles
                byteNum += Integer.BYTES;
                break;
            case START_FILE_LIST_ACK:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numTotalFiles
                byteNum += Integer.BYTES;
                // returnCode
                byteNum += Integer.BYTES;
                break;
            case FILE_ENTRIES:
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
                break;
            case FILE_ENTRIES_ACK:
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
                break;
            case END_FILE_LIST:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numFilesCompleted
                byteNum += Integer.BYTES;
                break;
            case END_FILE_LIST_ACK:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numFilesCompleted
                byteNum += Integer.BYTES;
                // returnCode
                byteNum += Integer.BYTES;
                break;
            case REQUEST_NEW_FILES:
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
                break;
            case COMPLETE_NEW_FILE:
            case COMPLETE_UPDATE_FILE:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // completedPath
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + completedPath.toString().getBytes().length;
                break;
            case COMPLETE_FILE_SYNC:
                // userName
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + userName.getBytes().length;
                // numFilesCompleted
                byteNum += Integer.BYTES;
                break;
            default:
                byteNum = -1;
                break;
        }

        return byteNum;
    }

    @Override
    protected void marshallBody() {

        switch(m_nID) {
            case START_FILE_LIST:
                // userName
                putStringToByteBuffer(userName);
                // numTotalFiles
                m_bytes.putInt(numTotalFiles);
                break;
            case START_FILE_LIST_ACK:
                // userName
                putStringToByteBuffer(userName);
                // numTotalFiles
                m_bytes.putInt(numTotalFiles);
                // returnCode
                m_bytes.putInt(returnCode);
                break;
            case FILE_ENTRIES:
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
                break;
            case FILE_ENTRIES_ACK:
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
                break;
            case END_FILE_LIST:
                // userName
                putStringToByteBuffer(userName);
                // numFilesCompleted
                m_bytes.putInt(numFilesCompleted);
                break;
            case END_FILE_LIST_ACK:
                // userName
                putStringToByteBuffer(userName);
                // numFilesCompleted
                m_bytes.putInt(numFilesCompleted);
                // returnCode
                m_bytes.putInt(returnCode);
                break;
            case REQUEST_NEW_FILES:
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
                break;
            case COMPLETE_NEW_FILE:
            case COMPLETE_UPDATE_FILE:
                // userName
                putStringToByteBuffer(userName);
                // completedPath
                putStringToByteBuffer(completedPath.toString());
                break;
            case COMPLETE_FILE_SYNC:
                // userName
                putStringToByteBuffer(userName);
                // numFilesCompleted
                m_bytes.putInt(numFilesCompleted);
                break;
            default:
                System.err.println("CMFileSyncEvent.marshallBody(), unknown event Id("+m_nID+").");
                m_bytes = null;
                break;
        }

    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {

        int numFileEntries;
        int numElementsOfRequestedFileList;

        switch(m_nID) {
            case START_FILE_LIST:
                // userName
                userName = getStringFromByteBuffer(msg);
                // numTotalFiles
                numTotalFiles = msg.getInt();
                break;
            case START_FILE_LIST_ACK:
                // userName
                userName = getStringFromByteBuffer(msg);
                // numTotalFiles
                numTotalFiles = msg.getInt();
                // returnCode
                returnCode = msg.getInt();
                break;
            case FILE_ENTRIES:
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
                break;
            case FILE_ENTRIES_ACK:
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
                break;
            case END_FILE_LIST:
                // userName
                userName = getStringFromByteBuffer(msg);
                // numFilesCompleted
                numFilesCompleted = msg.getInt();
                break;
            case END_FILE_LIST_ACK:
                // userName
                userName = getStringFromByteBuffer(msg);
                // numFilesCompleted
                numFilesCompleted = msg.getInt();
                // returnCode
                returnCode = msg.getInt();
                break;
            case REQUEST_NEW_FILES:
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
                break;
            case COMPLETE_NEW_FILE:
            case COMPLETE_UPDATE_FILE:
                // userName
                userName = getStringFromByteBuffer(msg);
                // completedPath
                completedPath = Paths.get(getStringFromByteBuffer(msg));
                break;
            case COMPLETE_FILE_SYNC:
                // userName
                userName = getStringFromByteBuffer(msg);
                // numFilesCompleted
                numFilesCompleted = msg.getInt();
                break;
            default:
                System.err.println("CMFileSyncEvent.unmarshallBody(), unknown event Id("+m_nID+").");
                break;
        }
    }

    @Override
    public String toString() {

        switch(m_nID) {
            case START_FILE_LIST:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(START_FILE_LIST)" +
                        ", userName='" + userName + '\'' +
                        ", numTotalFiles=" + numTotalFiles +
                        '}';
            case START_FILE_LIST_ACK:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(START_FILE_LIST_ACK)" +
                        ", userName='" + userName + '\'' +
                        ", numTotalFiles=" + numTotalFiles +
                        ", returnCode=" + returnCode +
                        '}';
            case FILE_ENTRIES:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(FILE_ENTRIES)" +
                        ", userName='" + userName + '\'' +
                        ", numFilesCompleted=" + numFilesCompleted +
                        ", numFiles=" + numFiles +
                        ", fileEntryList=" + fileEntryList +
                        '}';
            case FILE_ENTRIES_ACK:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(FILE_ENTRIES_ACK)" +
                        ", userName='" + userName + '\'' +
                        ", numFilesCompleted=" + numFilesCompleted +
                        ", numFiles=" + numFiles +
                        ", fileEntryList=" + fileEntryList +
                        ", returnCode=" + returnCode +
                        '}';
            case END_FILE_LIST:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(END_FILE_LIST)" +
                        ", userName='" + userName + '\'' +
                        ", numFilesCompleted=" + numFilesCompleted +
                        '}';
            case END_FILE_LIST_ACK:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(END_FILE_LIST_ACK)" +
                        ", userName='" + userName + '\'' +
                        ", numFilesCompleted=" + numFilesCompleted +
                        ", returnCode=" + returnCode +
                        '}';
            case REQUEST_NEW_FILES:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(REQUEST_NEW_FILES)" +
                        ", requesterName='" + requesterName + '\'' +
                        ", numRequestedFiles=" + numRequestedFiles +
                        ", requestedFileList=" + requestedFileList +
                        '}';
            case COMPLETE_NEW_FILE:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(COMPLETE_NEW_FILE)" +
                        ", userName='" + userName + '\'' +
                        ", completedPath='" + completedPath + '\'' +
                        '}';
            case COMPLETE_UPDATE_FILE:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(COMPLETE_UPDATE_FILE)" +
                        ", userName='" + userName + '\'' +
                        ", completedPath='" + completedPath + '\'' +
                        '}';
            case COMPLETE_FILE_SYNC:
                return "CMFileSyncEvent {" +
                        "m_nID=" + m_nID + "(COMPLETE_FILE_SYNC)" +
                        ", userName='" + userName + '\'' +
                        ", numFilesCompleted=" + numFilesCompleted +
                        '}';
            default:
                return "CMFileSyncEvent {" +
                        "m_nID="+ m_nID + "(unknown)" +
                        '}';
        }
    }
}
