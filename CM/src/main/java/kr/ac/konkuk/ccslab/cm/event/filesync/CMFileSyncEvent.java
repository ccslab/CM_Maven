package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents CM events that are used for the file-sync task.
 *
 * @author CCSLab, Konkuk University
 */
public abstract class CMFileSyncEvent extends CMEvent {

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

    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize
    public static final int START_FILE_BLOCK_CHECKSUM = 8;
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize, int returnCode
    public static final int START_FILE_BLOCK_CHECKSUM_ACK = 9;
    // Fields: int fileEntryIndex, int totalNumBlocks, int startBlocksIndex, int numCurrentBlocks
    // Fields: CMFileSyncBlockChecksum[] checksumArray
    public static final int FILE_BLOCK_CHECKSUM = 10;
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize
    public static final int END_FILE_BLOCK_CHECKSUM = 11;
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize, int fileChecksum
    public static final int END_FILE_BLOCK_CHECKSUM_ACK = 12;
    // Fields: int fileEntryIndex, int numNonMatchBytes, byte[] nonMatchBytes, int matchBlockIndex
    public static final int UPDATE_EXISTING_FILE = 13;

    // Fields: userName, completedPath
    public static final int COMPLETE_NEW_FILE = 14;
    // Fields: userName, completedPath
    public static final int COMPLETE_UPDATE_FILE = 15;
    // Fields: userName, skippedPath
    public static final int SKIP_UPDATE_FILE = 16;
    // Fields: userName, numFilesCompleted
    public static final int COMPLETE_FILE_SYNC = 17;

    // Fields: String requester, int numCurrentFiles, List<Path> relativePathList
    public static final int ONLINE_MODE_LIST = 20;
    // Fields: String requester, int numCurrentFiles, List<Path> relativePathList, int returnCode
    public static final int ONLINE_MODE_LIST_ACK = 21;
    // Fields: String requester, int numOnlineModeFiles
    public static final int END_ONLINE_MODE_LIST = 22;
    // Fields: String requester, int numOnlineModeFiles, int returnCode
    public static final int END_ONLINE_MODE_LIST_ACK = 23;

    public CMFileSyncEvent() {
        m_nType = CMInfo.CM_FILE_SYNC_EVENT;
    }

/*
    public CMFileSyncEvent(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }
*/
}
