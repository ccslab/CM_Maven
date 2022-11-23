package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents CM events that are used for the file synchronization task.
 *
 * @author CCSLab, Konkuk University
 */
public abstract class CMFileSyncEvent extends CMEvent {

    /**
     * event ID of the CMFileSyncEventStartFileList class.
     */
    // Fields: userName, numTotalFiles
    public static final int START_FILE_LIST = 1;

    /**
     * event ID of the CMFileSyncEventStartFileListAck class.
     */
    // Fields: userName, numTotalFiles, returnCode
    public static final int START_FILE_LIST_ACK = 2;

    /**
     * event ID of the CMFileSyncEventFileEntries class.
     */
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList
    public static final int FILE_ENTRIES = 3;

    /**
     * event ID of the CMFileSyncEventFileEntriesAck class.
     */
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList, returnCode
    public static final int FILE_ENTRIES_ACK = 4;

    /**
     * event ID of the CMFileSyncEventEndFileList class.
     */
    // Fields: userName, numFilesCompleted
    public static final int END_FILE_LIST = 5;

    /**
     * event ID of the CMFileSyncEventEndFileListAck class.
     */
    // Fields: userName, numFilesCompleted, returnCode
    public static final int END_FILE_LIST_ACK = 6;

    /**
     * event ID of the CMFileSyncEventRequestNewFiles class.
     */
    // Fields: String requesterName, int numRequestedFiles, List<Path> requestedFileList
    public static final int REQUEST_NEW_FILES = 7;

    /**
     * event ID of the CMFileSyncEventStartFileBlockChecksum class.
     */
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize
    public static final int START_FILE_BLOCK_CHECKSUM = 8;

    /**
     * event ID of the CMFileSyncEventStartFileBlockChecksumAck class.
     */
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize, int returnCode
    public static final int START_FILE_BLOCK_CHECKSUM_ACK = 9;

    /**
     * event ID of the CMFileSyncEventFileBlockChecksum class.
     */
    // Fields: int fileEntryIndex, int totalNumBlocks, int startBlocksIndex, int numCurrentBlocks
    // Fields: CMFileSyncBlockChecksum[] checksumArray
    public static final int FILE_BLOCK_CHECKSUM = 10;

    /**
     * event ID of the CMFileSyncEventEndFileBlockChecksum class.
     */
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize
    public static final int END_FILE_BLOCK_CHECKSUM = 11;

    /**
     * event ID of the CMFileSyncEventEndFileBlockChecksumAck class.
     */
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize, int fileChecksum, int returnCode
    public static final int END_FILE_BLOCK_CHECKSUM_ACK = 12;

    /**
     * event ID of the CMFileSyncEventUpdateExistingFile class.
     */
    // Fields: int fileEntryIndex, int numNonMatchBytes, byte[] nonMatchBytes, int matchBlockIndex
    public static final int UPDATE_EXISTING_FILE = 13;

    /**
     * event ID of the CMFileSyncEventCompleteNewFile class.
     */
    // Fields: userName, completedPath
    public static final int COMPLETE_NEW_FILE = 14;

    /**
     * event ID of the CMFileSyncEventCompleteUpdateFile class.
     */
    // Fields: userName, completedPath
    public static final int COMPLETE_UPDATE_FILE = 15;

    /**
     * event ID of the CMFileSyncEventSkipUpdateFile class.
     */
    // Fields: userName, skippedPath
    public static final int SKIP_UPDATE_FILE = 16;

    /**
     * event ID of the CMFileSyncEventCompleteFileSync class.
     */
    // Fields: userName, numFilesCompleted
    public static final int COMPLETE_FILE_SYNC = 17;

    /**
     * event ID of the CMFileSyncEventOnlineModeList class.
     */
    // Fields: String requester, List<Path> relativePathList
    public static final int ONLINE_MODE_LIST = 20;

    /**
     * event ID of the CMFileSyncEventOnlineModeListAck class.
     */
    // Fields: String requester, List<Path> relativePathList, int returnCode
    public static final int ONLINE_MODE_LIST_ACK = 21;

    /**
     * event ID of the CMFileSyncEventEndOnlineModeList class.
     */
    // Fields: String requester, int numOnlineModeFiles
    public static final int END_ONLINE_MODE_LIST = 22;

    /**
     * event ID of the CMFileSyncEventEndOnlineModeListAck class.
     */
    // Fields: String requester, int numOnlineModeFiles, int returnCode
    public static final int END_ONLINE_MODE_LIST_ACK = 23;

    /**
     * event ID of the CMFileSyncEventLocalModeList class.
     */
    // Fields: String requester, List<Path> relativePathList
    public static final int LOCAL_MODE_LIST = 24;

    /**
     * event ID of the CMFileSyncEventLocalModeListAck class.
     */
    // Fields: String requester, List,Path> relativePathList, int returnCode
    public static final int LOCAL_MODE_LIST_ACK = 25;

    /**
     * event ID of the CMFileSyncEventEndLocalModeList class.
     */
    // Fields: String requester, int numLocalModeFiles
    public static final int END_LOCAL_MODE_LIST = 26;

    /**
     * event ID of the CMFileSyncEventEndLocalModeListAck class.
     */
    // Fields: String requester, int numLocalModeFiles, int returnCode
    public static final int END_LOCAL_MODE_LIST_ACK = 27;

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
