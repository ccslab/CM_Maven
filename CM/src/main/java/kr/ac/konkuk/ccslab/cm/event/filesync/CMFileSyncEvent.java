package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.UUID;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.util.CMUUIDConverter;

/**
 * This class represents CM events that are used for the file synchronization task.
 *
 * @author CCSLab, Konkuk University
 */
public abstract class CMFileSyncEvent extends CMEvent {

    /**
     * event ID of the CMFileSyncEventStartFileList class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, numTotalFiles
    public static final int START_FILE_LIST = 1;

    /**
     * event ID of the CMFileSyncEventStartFileListAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, numTotalFiles, returnCode
    public static final int START_FILE_LIST_ACK = 2;

    /**
     * event ID of the CMFileSyncEventFileEntries class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList
    public static final int FILE_ENTRIES = 3;

    /**
     * event ID of the CMFileSyncEventFileEntriesAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, numFilesCompleted, numFiles, fileEntryList, returnCode
    public static final int FILE_ENTRIES_ACK = 4;

    /**
     * event ID of the CMFileSyncEventEndFileList class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, numFilesCompleted
    public static final int END_FILE_LIST = 5;

    /**
     * event ID of the CMFileSyncEventEndFileListAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, numFilesCompleted, returnCode
    public static final int END_FILE_LIST_ACK = 6;

    /**
     * event ID of the CMFileSyncEventRequestNewFiles class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String requesterName, int numRequestedFiles, List<Path> requestedFileList
    public static final int REQUEST_NEW_FILES = 7;

    /**
     * event ID of the CMFileSyncEventStartFileBlockChecksum class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize, String relativePath
    public static final int START_FILE_BLOCK_CHECKSUM = 8;

    /**
     * event ID of the CMFileSyncEventStartFileBlockChecksumAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize, int returnCode
    public static final int START_FILE_BLOCK_CHECKSUM_ACK = 9;

    /**
     * event ID of the CMFileSyncEventFileBlockChecksum class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int fileEntryIndex, int totalNumBlocks, int startBlocksIndex, int numCurrentBlocks
    // Fields: CMFileSyncBlockChecksum[] checksumArray
    public static final int FILE_BLOCK_CHECKSUM = 10;

    /**
     * event ID of the CMFileSyncEventEndFileBlockChecksum class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize
    public static final int END_FILE_BLOCK_CHECKSUM = 11;

    /**
     * event ID of the CMFileSyncEventEndFileBlockChecksumAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int fileEntryIndex, int totalNumBlocks, int blockSize, int fileChecksum, int returnCode
    public static final int END_FILE_BLOCK_CHECKSUM_ACK = 12;

    /**
     * event ID of the CMFileSyncEventUpdateExistingFile class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int fileEntryIndex, int numNonMatchBytes, byte[] nonMatchBytes, int matchBlockIndex
    public static final int UPDATE_EXISTING_FILE = 13;

    /**
     * event ID of the CMFileSyncEventCompleteNewFile class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, completedPath
    public static final int COMPLETE_NEW_FILE = 14;

    /**
     * event ID of the CMFileSyncEventCompleteUpdateFile class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, completedPath
    public static final int COMPLETE_UPDATE_FILE = 15;

    /**
     * event ID of the CMFileSyncEventSkipUpdateFile class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, skippedPath
    public static final int SKIP_UPDATE_FILE = 16;

    /**
     * event ID of the CMFileSyncEventCompleteFileSync class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: userName, numFilesCompleted
    public static final int COMPLETE_FILE_SYNC = 17;

    /**
     * event ID of the CMFileSyncEventCompleteDeleteFiles class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: deletedPathList, cursor
    public static final int COMPLETE_DELETE_FILES = 18;

    /**
     * event ID of the CMFileSyncEventOnlineModeList class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String requester, List<Path> relativePathList
    public static final int ONLINE_MODE_LIST = 20;

    /**
     * event ID of the CMFileSyncEventOnlineModeListAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String requester, List<Path> relativePathList, int returnCode
    public static final int ONLINE_MODE_LIST_ACK = 21;

    /**
     * event ID of the CMFileSyncEventEndOnlineModeList class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String requester, int numOnlineModeFiles
    public static final int END_ONLINE_MODE_LIST = 22;

    /**
     * event ID of the CMFileSyncEventEndOnlineModeListAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String requester, int numOnlineModeFiles, int returnCode
    public static final int END_ONLINE_MODE_LIST_ACK = 23;

    /**
     * event ID of the CMFileSyncEventLocalModeList class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String requester, List<Path> relativePathList
    public static final int LOCAL_MODE_LIST = 24;

    /**
     * event ID of the CMFileSyncEventLocalModeListAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String requester, List,Path> relativePathList, int returnCode
    public static final int LOCAL_MODE_LIST_ACK = 25;

    /**
     * event ID of the CMFileSyncEventEndLocalModeList class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String requester, int numLocalModeFiles
    public static final int END_LOCAL_MODE_LIST = 26;

    /**
     * event ID of the CMFileSyncEventEndLocalModeListAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String requester, int numLocalModeFiles, int returnCode
    public static final int END_LOCAL_MODE_LIST_ACK = 27;

    /**
     * event ID of the CMFileSyncEventStartPullSync class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: long cursor
    public static final int START_PULL_SYNC = 28;

    /**
     * event ID of the CMFileSyncEventStartPullSyncAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int returnCode, long serverCursor
    public static final int START_PULL_SYNC_ACK = 29;

    /**
     * event ID of the CMFileSyncEventStartServerEntryList class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int numTotalFiles
    public static final int START_SERVER_ENTRY_LIST = 30;

    /**
     * event ID of the CMFileSyncEventStartServerEntryListAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int numTotalFiles, int returnCode
    public static final int START_SERVER_ENTRY_LIST_ACK = 31;

    /**
     * event ID of the CMFileSyncEventServerEntries class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int numFilesCompleted, int numFiles, List<CMFileSyncChangeLogEntry> serverEntryList
    public static final int SERVER_ENTRIES = 32;

    /**
     * event ID of the CMFileSyncEventServerEntriesAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int numFilesCompleted, int numFiles, int returnCode
    public static final int SERVER_ENTRIES_ACK = 33;

    /**
     * event ID of the CMFileSyncEventEndServerEntryList class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int numFilesCompleted
    public static final int END_SERVER_ENTRY_LIST = 34;

    /**
     * event ID of the CMFileSyncEventEndServerEntryListAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int numFilesCompleted, int returnCode
    public static final int END_SERVER_ENTRY_LIST_ACK = 35;

    /**
     * event ID of the CMFileSyncEventCompletePullDelete class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: List<String> deletedPathList
    public static final int COMPLETE_PULL_DELETE = 36;

    /**
     * event ID of the CMFileSyncEventCompletePullCreate class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String createdPath
    public static final int COMPLETE_PULL_CREATE = 37;

    /**
     * event ID of the CMFileSyncEventCompletePullModify class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: String modifiedPath
    public static final int COMPLETE_PULL_MODIFY = 38;

    /**
     * event ID of the CMFileSyncEventCompletePullSync class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int numFilesCompleted
    public static final int COMPLETE_PULL_SYNC = 39;

    /**
     * event ID of the CMFileSyncEventCompletePullSyncAck class.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int numFilesCompleted, int returnCode
    public static final int COMPLETE_PULL_SYNC_ACK = 40;

    /**
     * event ID of the CMFileSyncEventRequestPullCreates class.
     * 클라이언트가 서버에 pull-sync CREATE 대상 파일들의 push 를 요청.
     */
    // CommonHeader: initiatorName, initiatorUuid, initiatorDeviceUuid
    // Fields: int numRequestedFiles, List<Path> requestedFileList (relative to FileSyncHome)
    public static final int REQUEST_PULL_CREATES = 41;

    // ----------------------------------------------------------------
    // [NEW] FileSync common header fields (event initiator identity)
    // Used by both client-initiated and server-initiated events
    // ----------------------------------------------------------------
    private String m_initiatorName       = "";
    private UUID   m_initiatorUuid       = null;   // login-instance UUID
    private UUID   m_initiatorDeviceUuid = null;   // device UUID

    public CMFileSyncEvent() {
        m_nType = CMInfo.CM_FILE_SYNC_EVENT;
    }

    // ----------------------------------------------------------------
    // [NEW] Common header getter/setter
    // ----------------------------------------------------------------
    public String getInitiatorName() { return m_initiatorName; }
    public void setInitiatorName(String v) { m_initiatorName = (v == null ? "" : v); }

    public UUID getInitiatorUuid() { return m_initiatorUuid; }
    public void setInitiatorUuid(UUID v) { m_initiatorUuid = v; }

    public UUID getInitiatorDeviceUuid() { return m_initiatorDeviceUuid; }
    public void setInitiatorDeviceUuid(UUID v) { m_initiatorDeviceUuid = v; }

    // ----------------------------------------------------------------
    // [NEW] getByteNum() — adds common header byte count on top of CMEvent header.
    // All subclasses call super.getByteNum(), so this must account for
    // version(1B) + initiatorName(str) + initiatorUuid(str) + initiatorDeviceUuid(str).
    // UUID fields are serialized as canonical strings via CMUUIDConverter (same as CMEvent).
    // ----------------------------------------------------------------
    @Override
    protected int getByteNum() {
        int byteNum = super.getByteNum();
        // version marker
        byteNum += Byte.BYTES;
        // initiatorName
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + m_initiatorName.getBytes().length;
        // initiatorUuid (serialized as string, null -> "")
        byteNum += CMInfo.STRING_LEN_BYTES_LEN
                + CMUUIDConverter.uuidToString(m_initiatorUuid).getBytes().length;
        // initiatorDeviceUuid (serialized as string, null -> "")
        byteNum += CMInfo.STRING_LEN_BYTES_LEN
                + CMUUIDConverter.uuidToString(m_initiatorDeviceUuid).getBytes().length;
        return byteNum;
    }

    // ----------------------------------------------------------------
    // [NEW] Template method pattern:
    // marshallBody()/unmarshallBody() are sealed here to process the
    // common FS header first, then delegate to subclasses via
    // marshallBodyCore()/unmarshallBodyCore().
    // Subclasses MUST override marshallBodyCore/unmarshallBodyCore
    // instead of marshallBody/unmarshallBody.
    // ----------------------------------------------------------------
    @Override
    protected final void marshallBody() {
        marshallCommonFSHeader();
        marshallBodyCore();
    }

    @Override
    protected final void unmarshallBody(ByteBuffer msg) {
        unmarshallCommonFSHeader(msg);
        unmarshallBodyCore(msg);
    }

    // [NEW] Subclass hook methods
    protected abstract void marshallBodyCore();
    protected abstract void unmarshallBodyCore(ByteBuffer msg);

    // ----------------------------------------------------------------
    // [NEW] Common FS header serialization/deserialization.
    // Wire format: version(1B) | initiatorName(str) | initiatorUuid(str) | initiatorDeviceUuid(str)
    // UUID is converted to/from canonical string via CMUUIDConverter (consistent with CMEvent).
    // ----------------------------------------------------------------
    protected void marshallCommonFSHeader() {
        m_bytes.put((byte) 1);                                              // version = 1
        putStringToByteBuffer(m_initiatorName);
        putStringToByteBuffer(CMUUIDConverter.uuidToString(m_initiatorUuid));
        putStringToByteBuffer(CMUUIDConverter.uuidToString(m_initiatorDeviceUuid));
    }

    protected void unmarshallCommonFSHeader(ByteBuffer msg) {
        byte ver = msg.get();                                               // version (currently 1)
        m_initiatorName       = getStringFromByteBuffer(msg);
        m_initiatorUuid       = CMUUIDConverter.stringToUuid(getStringFromByteBuffer(msg));
        m_initiatorDeviceUuid = CMUUIDConverter.stringToUuid(getStringFromByteBuffer(msg));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", initiatorName='" + m_initiatorName + '\'' +
                ", initiatorUuid=" + m_initiatorUuid +
                ", initiatorDeviceUuid=" + m_initiatorDeviceUuid +
                '}';
    }
}
