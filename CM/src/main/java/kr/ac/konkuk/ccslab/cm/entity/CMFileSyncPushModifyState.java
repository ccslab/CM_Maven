package kr.ac.konkuk.ccslab.cm.entity;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.*;

/**
 * CLIENT-side holder for this client's incremental push sync MODIFY session state.
 * Mirror image of {@link CMFileSyncPullModifyState} (which is server-side): here the client is
 * the source-holding side, so it accumulates the basis block checksums sent by the server, builds
 * the (weakChecksum → blockIndex) map on END_FILE_BLOCK_CHECKSUM, scans its own source file and
 * sends UPDATE_EXISTING_FILE events to the server.
 * <br>Stored as a single field in CMFileSyncInfo.pushModifyState (the client runs at most one push
 * session at a time), unlike the server's stateKey-keyed pullModifyStateMap.
 * <br>Lifecycle: created lazily on the first START_FILE_BLOCK_CHECKSUM (PUSH branch), fully cleaned
 * up on COMPLETE_PUSH_SYNC (client side).
 */
public class CMFileSyncPushModifyState {

    private final String initiatorName;         // this client's own name (event common field)
    private final UUID initiatorUuid;           // this client's own login UUID (event common field)
    private final UUID initiatorDeviceUuid;     // this client's own device UUID (event common field)
    private final String serverName;            // send target server name (unicast receiver)

    // key: fileEntryIndex; value: checksum array built up over FILE_BLOCK_CHECKSUM events
    private final Map<Integer, CMFileSyncBlockChecksum[]> blockChecksumArrayMap;

    // key: fileEntryIndex; value: (weakChecksum → blockIndex) built on END_FILE_BLOCK_CHECKSUM
    private final Map<Integer, Map<Short, Integer>> fileIndexToHashToBlockIndexMap;

    // key: fileEntryIndex; value: block size for that file
    private final Map<Integer, Integer> blockSizeMap;

    // key: fileEntryIndex; value: relative path string registered from START_FILE_BLOCK_CHECKSUM
    private final Map<Integer, String> fileEntryIndexToRelativePathMap;

    // key: fileEntryIndex; value: read channel on this client's own (newer) source file
    private final Map<Integer, SeekableByteChannel> sourceFileChannelForReadMap;

    // key: relative path string; value: whether client-side processing is done for that file
    private final Map<String, Boolean> isUpdateFileCompletedMap;

    private int numUpdateFilesCompleted;

    public CMFileSyncPushModifyState(String initiatorName, UUID initiatorUuid,
                                     UUID initiatorDeviceUuid, String serverName) {
        this.initiatorName = initiatorName;
        this.initiatorUuid = initiatorUuid;
        this.initiatorDeviceUuid = initiatorDeviceUuid;
        this.serverName = serverName;

        blockChecksumArrayMap = new Hashtable<>();
        fileIndexToHashToBlockIndexMap = new Hashtable<>();
        blockSizeMap = new Hashtable<>();
        fileEntryIndexToRelativePathMap = new Hashtable<>();
        sourceFileChannelForReadMap = new Hashtable<>();
        isUpdateFileCompletedMap = new Hashtable<>();
        numUpdateFilesCompleted = 0;
    }

    // --- getters (identifiers are immutable; maps are exposed by reference) ---

    public String getInitiatorName() { return initiatorName; }
    public UUID getInitiatorUuid() { return initiatorUuid; }
    public UUID getInitiatorDeviceUuid() { return initiatorDeviceUuid; }
    public String getServerName() { return serverName; }

    public Map<Integer, CMFileSyncBlockChecksum[]> getBlockChecksumArrayMap() {
        return blockChecksumArrayMap;
    }

    public Map<Integer, Map<Short, Integer>> getFileIndexToHashToBlockIndexMap() {
        return fileIndexToHashToBlockIndexMap;
    }

    public Map<Integer, Integer> getBlockSizeMap() { return blockSizeMap; }

    public Map<Integer, String> getFileEntryIndexToRelativePathMap() {
        return fileEntryIndexToRelativePathMap;
    }

    public Map<Integer, SeekableByteChannel> getSourceFileChannelForReadMap() {
        return sourceFileChannelForReadMap;
    }

    public Map<String, Boolean> getIsUpdateFileCompletedMap() { return isUpdateFileCompletedMap; }

    public int getNumUpdateFilesCompleted() { return numUpdateFilesCompleted; }
    public void setNumUpdateFilesCompleted(int n) { this.numUpdateFilesCompleted = n; }
    public void incrementNumUpdateFilesCompleted() { this.numUpdateFilesCompleted++; }

    // --- lifecycle methods ---

    // Called after this client sends its own END_FILE_BLOCK_CHECKSUM_ACK for one file entry.
    // Closes the source read channel and clears the index-keyed data for this entry.
    // isUpdateFileCompletedMap is NOT removed here — it is needed for the all-entries-completed
    // decision and is cleared in bulk by cleanupAll().
    public void cleanupForFileEntry(int fileEntryIndex) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPushModifyState.cleanupForFileEntry(), fileEntryIndex=" + fileEntryIndex);

        SeekableByteChannel ch = sourceFileChannelForReadMap.remove(fileEntryIndex);
        if (ch != null && ch.isOpen()) {
            try { ch.close(); } catch (IOException e) { e.printStackTrace(); }
        }

        blockChecksumArrayMap.remove(fileEntryIndex);
        fileIndexToHashToBlockIndexMap.remove(fileEntryIndex);
        blockSizeMap.remove(fileEntryIndex);
        fileEntryIndexToRelativePathMap.remove(fileEntryIndex);

        if (CMInfo._CM_DEBUG)
            System.out.println("after remove: blockChecksumArrayMap.size=" + blockChecksumArrayMap.size()
                    + ", blockSizeMap.size=" + blockSizeMap.size()
                    + ", sourceFileChannelForReadMap.size=" + sourceFileChannelForReadMap.size());
    }

    // Returns true when every registered entry has been marked completed.
    public boolean isAllRegisteredEntriesCompleted() {
        if (isUpdateFileCompletedMap.isEmpty()) return false;
        for (Boolean done : isUpdateFileCompletedMap.values()) {
            if (!done) return false;
        }
        return true;
    }

    // Protective full cleanup at the end of the holder lifecycle. Called from the cleanup step of
    // processCOMPLETE_PUSH_SYNC (client side) right before setPushModifyState(null). In the normal
    // flow the per-entry channels are already closed + removed by cleanupForFileEntry; this handles
    // abnormal termination. Identifiers are left as-is — after this call setPushModifyState(null)
    // makes the holder unreachable, so reference nulling is unnecessary.
    public void cleanupAll() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncPushModifyState.cleanupAll() called.");
            System.out.println("blockChecksumArrayMap.size=" + blockChecksumArrayMap.size()
                    + ", fileIndexToHashToBlockIndexMap.size=" + fileIndexToHashToBlockIndexMap.size()
                    + ", blockSizeMap.size=" + blockSizeMap.size()
                    + ", fileEntryIndexToRelativePathMap.size=" + fileEntryIndexToRelativePathMap.size()
                    + ", sourceFileChannelForReadMap.size=" + sourceFileChannelForReadMap.size()
                    + ", isUpdateFileCompletedMap.size=" + isUpdateFileCompletedMap.size()
                    + ", numUpdateFilesCompleted=" + numUpdateFilesCompleted);
        }

        for (SeekableByteChannel ch : sourceFileChannelForReadMap.values()) {
            if (ch != null && ch.isOpen()) {
                try { ch.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
        sourceFileChannelForReadMap.clear();

        blockChecksumArrayMap.clear();
        fileIndexToHashToBlockIndexMap.clear();
        blockSizeMap.clear();
        fileEntryIndexToRelativePathMap.clear();
        isUpdateFileCompletedMap.clear();
        numUpdateFilesCompleted = 0;

        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPushModifyState.cleanupAll() done.");
    }

    @Override
    public String toString() {
        return "CMFileSyncPushModifyState{" +
                "initiatorName='" + initiatorName + '\'' +
                ", initiatorDeviceUuid=" + initiatorDeviceUuid +
                ", serverName='" + serverName + '\'' +
                ", blockChecksumArrayMap.size=" + blockChecksumArrayMap.size() +
                ", fileIndexToHashToBlockIndexMap.size=" + fileIndexToHashToBlockIndexMap.size() +
                ", blockSizeMap.size=" + blockSizeMap.size() +
                ", fileEntryIndexToRelativePathMap.size=" + fileEntryIndexToRelativePathMap.size() +
                ", sourceFileChannelForReadMap.size=" + sourceFileChannelForReadMap.size() +
                ", isUpdateFileCompletedMap.size=" + isUpdateFileCompletedMap.size() +
                ", numUpdateFilesCompleted=" + numUpdateFilesCompleted +
                '}';
    }
}
