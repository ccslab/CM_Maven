package kr.ac.konkuk.ccslab.cm.entity;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.*;

/**
 * SERVER-side holder for one client's pull sync MODIFY session state.
 * Keyed by CMFileSyncStateKey in CMFileSyncInfo.pullModifyStateMap.
 * Lifecycle: created lazily on first START_FILE_BLOCK_CHECKSUM, removed on COMPLETE_PULL_SYNC_ACK.
 */
public class CMFileSyncPullModifyState {

    private final String initiatorName;
    private final UUID initiatorUuid;
    private final UUID initiatorDeviceUuid;

    // key: fileEntryIndex; value: checksum array built up over FILE_BLOCK_CHECKSUM events
    private final Map<Integer, CMFileSyncBlockChecksum[]> blockChecksumArrayMap;

    // key: fileEntryIndex; value: (weakChecksum → blockIndex) built on END_FILE_BLOCK_CHECKSUM
    private final Map<Integer, Map<Short, Integer>> fileIndexToHashToBlockIndexMap;

    // key: fileEntryIndex; value: block size for that file
    private final Map<Integer, Integer> blockSizeMap;

    // key: fileEntryIndex; value: relative path string registered from START_FILE_BLOCK_CHECKSUM
    private final Map<Integer, String> fileEntryIndexToRelativePathMap;

    // key: fileEntryIndex; value: read channel on the server's newer source file
    private final Map<Integer, SeekableByteChannel> sourceFileChannelForReadMap;

    // key: relative path string; value: whether server-side processing is done for that file
    private final Map<String, Boolean> isUpdateFileCompletedMap;

    private int numUpdateFilesCompleted;

    public CMFileSyncPullModifyState(String initiatorName, UUID initiatorUuid,
                                     UUID initiatorDeviceUuid) {
        this.initiatorName = initiatorName;
        this.initiatorUuid = initiatorUuid;
        this.initiatorDeviceUuid = initiatorDeviceUuid;

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

    // Called after END_FILE_BLOCK_CHECKSUM_ACK is sent for one file entry.
    public void cleanupForFileEntry(int fileEntryIndex) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPullModifyState.cleanupForFileEntry(), fileEntryIndex=" + fileEntryIndex);

        blockChecksumArrayMap.remove(fileEntryIndex);
        fileIndexToHashToBlockIndexMap.remove(fileEntryIndex);
        blockSizeMap.remove(fileEntryIndex);
        fileEntryIndexToRelativePathMap.remove(fileEntryIndex);

        SeekableByteChannel ch = sourceFileChannelForReadMap.get(fileEntryIndex);
        if (ch != null) {
            try {
                ch.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                sourceFileChannelForReadMap.remove(fileEntryIndex);
            }
        }
    }

    // Returns true when every registered entry has been marked completed.
    public boolean isAllRegisteredEntriesCompleted() {
        if (isUpdateFileCompletedMap.isEmpty()) return false;
        for (Boolean done : isUpdateFileCompletedMap.values()) {
            if (!done) return false;
        }
        return true;
    }

    // Called before pullModifyStateMap.remove(stateKey) to release any residual resources.
    public void cleanupAll() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPullModifyState.cleanupAll() called.");

        for (SeekableByteChannel ch : sourceFileChannelForReadMap.values()) {
            try {
                ch.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        blockChecksumArrayMap.clear();
        fileIndexToHashToBlockIndexMap.clear();
        blockSizeMap.clear();
        fileEntryIndexToRelativePathMap.clear();
        sourceFileChannelForReadMap.clear();
        isUpdateFileCompletedMap.clear();
        numUpdateFilesCompleted = 0;
    }

    @Override
    public String toString() {
        return "CMFileSyncPullModifyState{" +
                "initiatorName='" + initiatorName + '\'' +
                ", initiatorDeviceUuid=" + initiatorDeviceUuid +
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
