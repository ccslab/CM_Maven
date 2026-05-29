package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncClientEntry;
import kr.ac.konkuk.ccslab.cm.event.filesync.CMFileSyncEventStartFileBlockChecksum;
import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileSyncManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

// Client-side pull-sync Runnable. Mirrors CMFileSyncGenerator (server-side push) but with
// client as the processing subject (서버↔클라이언트 역할 반전).
// run() computes block checksums of local basis files and sends START_FILE_BLOCK_CHECKSUM to
// the server. FILE_BLOCK_CHECKSUM and END_FILE_BLOCK_CHECKSUM are sent from the
// START_FILE_BLOCK_CHECKSUM_ACK handler (PULL branch). UPDATE_EXISTING_FILE events from the
// server are applied via this generator's channels in the event handler.
// Lifecycle ends when all END_FILE_BLOCK_CHECKSUM_ACK events are processed.
public class CMFileSyncPullGenerator implements Runnable {
    private String initiatorName;
    private UUID initiatorUuid;
    private UUID initiatorDeviceUuid;
    private String serverName;

    // pullModifyMap.values() snapshotted as an indexed list; index == fileEntryIndex
    private List<CMFileSyncClientEntry> pullModifyEntryList;

    // key: fileEntryIndex (== basisFileIndex in PULL — no separate basisFileIndexMap needed)
    private Map<Integer, CMFileSyncBlockChecksum[]> blockChecksumArrayMap;
    private Map<Integer, Integer> blockSizeOfBasisFileMap;

    // opened lazily by UPDATE_EXISTING_FILE handler via CMFileSyncInfo.getPullGenerator()
    private Map<Integer, SeekableByteChannel> basisFileChannelForReadMap;
    private Map<Integer, SeekableByteChannel> tempFileChannelForWriteMap;

    // key: relative path string (same convention as pullModifyMap keys)
    private Map<String, Boolean> isUpdateFileCompletedMap;
    private int numUpdateFilesCompleted;

    public CMFileSyncPullGenerator(String initiatorName, UUID initiatorUuid,
                                   UUID initiatorDeviceUuid, String serverName,
                                   List<CMFileSyncClientEntry> pullModifyEntryList) {
        this.initiatorName = initiatorName;
        this.initiatorUuid = initiatorUuid;
        this.initiatorDeviceUuid = initiatorDeviceUuid;
        this.serverName = serverName;
        this.pullModifyEntryList = pullModifyEntryList;

        blockChecksumArrayMap = new Hashtable<>();
        blockSizeOfBasisFileMap = new Hashtable<>();
        basisFileChannelForReadMap = new Hashtable<>();
        tempFileChannelForWriteMap = new Hashtable<>();
        isUpdateFileCompletedMap = new Hashtable<>();
        numUpdateFilesCompleted = 0;
    }

    @Override
    public void run() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPullGenerator.run() called..");

        if (pullModifyEntryList.isEmpty()) return;

        // pre-register all entries as not-yet-completed before sending any checksum event
        for (CMFileSyncClientEntry entry : pullModifyEntryList) {
            isUpdateFileCompletedMap.put(entry.getPath(), false);
        }

        boolean result = compareAndSendBlockChecksums();
        if (!result)
            System.err.println("CMFileSyncPullGenerator.run(), compareAndSendBlockChecksums() failed!");
    }

    // For each entry in pullModifyEntryList: computes block checksum of the local basis file
    // and sends START_FILE_BLOCK_CHECKSUM to the server.
    // FILE_BLOCK_CHECKSUM and END events are sent from the START_ACK handler (PULL branch).
    // Mirrors CMFileSyncGenerator.compareBasisAndInitiatorFileList(); skip logic not needed
    // because pullModifyMap entries are already confirmed as needing update.
    private boolean compareAndSendBlockChecksums() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPullGenerator.compareAndSendBlockChecksums() called..");

        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Path clientSyncHome = syncManager.getClientSyncHome();

        for (int fileEntryIndex = 0; fileEntryIndex < pullModifyEntryList.size(); fileEntryIndex++) {
            CMFileSyncClientEntry entry = pullModifyEntryList.get(fileEntryIndex);
            Path basisFile = clientSyncHome.resolve(entry.getPath()).normalize();

            if (!Files.exists(basisFile)) {
                System.err.println("CMFileSyncPullGenerator.compareAndSendBlockChecksums(), "
                        + "basis file not found: " + basisFile);
                continue;
            }

            CMFileSyncBlockChecksum[] checksumArray = createBlockChecksum(fileEntryIndex, basisFile);
            if (checksumArray == null) continue;

            blockChecksumArrayMap.put(fileEntryIndex, checksumArray);

            boolean sendResult = sendBlockChecksum(fileEntryIndex, checksumArray);
            if (!sendResult) {
                System.err.println("CMFileSyncPullGenerator.compareAndSendBlockChecksums(), "
                        + "send error at fileEntryIndex=" + fileEntryIndex);
                return false;
            }
        }

        return true;
    }

    // Mirrors CMFileSyncGenerator.createBlockChecksum(); basisFileIndex == fileEntryIndex here.
    private CMFileSyncBlockChecksum[] createBlockChecksum(int fileEntryIndex, Path basisFile) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncPullGenerator.createBlockChecksum() called..");
            System.out.println("fileEntryIndex=" + fileEntryIndex + ", basisFile=" + basisFile);
        }

        long fileSize;
        int blockSize;
        try {
            fileSize = Files.size(basisFile);
            blockSize = calculateBlockSize(fileSize);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        blockSizeOfBasisFileMap.put(fileEntryIndex, blockSize);

        int numBlocks = (int) (fileSize / blockSize);
        if (fileSize % blockSize > 0) numBlocks++;
        if (CMInfo._CM_DEBUG)
            System.out.println("numBlocks=" + numBlocks);

        CMFileSyncBlockChecksum[] checksumArray = new CMFileSyncBlockChecksum[numBlocks];
        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = Objects.requireNonNull(
                cmInfo.getServiceManager(CMFileSyncManager.class));

        ByteBuffer buffer = ByteBuffer.allocate(blockSize);
        SeekableByteChannel channel = null;
        try {
            channel = Files.newByteChannel(basisFile, StandardOpenOption.READ);
            for (int i = 0; i < numBlocks; i++) {
                checksumArray[i] = new CMFileSyncBlockChecksum();
                buffer.clear();
                channel.read(buffer);
                buffer.flip();
                checksumArray[i].setBlockIndex(i);
                checksumArray[i].setWeakChecksum(syncManager.calculateWeakChecksum(buffer));
                buffer.rewind();
                checksumArray[i].setStrongChecksum(syncManager.calculateStrongChecksum(buffer));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (channel != null) {
                try { channel.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }

        return checksumArray;
    }

    // Sends START_FILE_BLOCK_CHECKSUM to the server.
    // Mirrors CMFileSyncGenerator.sendBlockChecksum(); target is server instead of initiator.
    private boolean sendBlockChecksum(int fileEntryIndex, CMFileSyncBlockChecksum[] checksumArray) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPullGenerator.sendBlockChecksum() called..");

        CMFileSyncEventStartFileBlockChecksum fse = new CMFileSyncEventStartFileBlockChecksum();
        fse.setInitiatorName(initiatorName);
        fse.setInitiatorUuid(initiatorUuid);
        fse.setInitiatorDeviceUuid(initiatorDeviceUuid);
        fse.setFileEntryIndex(fileEntryIndex);
        fse.setTotalNumBlocks(checksumArray.length);
        fse.setBlockSize(blockSizeOfBasisFileMap.get(fileEntryIndex));
        // pull sync: carry the relative path so the server can map fileEntryIndex to a path
        fse.setRelativePath(pullModifyEntryList.get(fileEntryIndex).getPath());

        boolean ret = CMEventManager.unicastEvent(fse, serverName, null);
        if (!ret)
            System.err.println("CMFileSyncPullGenerator.sendBlockChecksum(), send error: " + fse);
        return ret;
    }

    // adapted from CMFileSyncGenerator.calculateBlockSize()
    private int calculateBlockSize(long fileSize) {
        int blength;
        if (fileSize < CMFileSyncInfo.BLOCK_SIZE * CMFileSyncInfo.BLOCK_SIZE) {
            blength = CMFileSyncInfo.BLOCK_SIZE;
        } else {
            int c;
            long l;
            for (c = 1, l = fileSize; (l >>= 2) > 0; c <<= 1) { }
            if (c < 0 || c >= CMFileSyncInfo.MAX_BLOCK_SIZE) {
                blength = CMFileSyncInfo.MAX_BLOCK_SIZE;
            } else {
                blength = 0;
                do {
                    blength |= c;
                    if (fileSize < (long) blength * blength) blength &= ~c;
                    c >>= 1;
                } while (c >= 8);
                blength = Math.max(blength, CMFileSyncInfo.BLOCK_SIZE);
            }
        }
        return blength;
    }

    public String getInitiatorName() { return initiatorName; }
    public UUID getInitiatorUuid() { return initiatorUuid; }
    public UUID getInitiatorDeviceUuid() { return initiatorDeviceUuid; }
    public String getServerName() { return serverName; }
    public List<CMFileSyncClientEntry> getPullModifyEntryList() { return pullModifyEntryList; }
    public Map<Integer, CMFileSyncBlockChecksum[]> getBlockChecksumArrayMap() { return blockChecksumArrayMap; }
    public Map<Integer, Integer> getBlockSizeOfBasisFileMap() { return blockSizeOfBasisFileMap; }
    public Map<Integer, SeekableByteChannel> getBasisFileChannelForReadMap() { return basisFileChannelForReadMap; }
    public Map<Integer, SeekableByteChannel> getTempFileChannelForWriteMap() { return tempFileChannelForWriteMap; }
    public Map<String, Boolean> getIsUpdateFileCompletedMap() { return isUpdateFileCompletedMap; }
    public int getNumUpdateFilesCompleted() { return numUpdateFilesCompleted; }
    public void setNumUpdateFilesCompleted(int n) { this.numUpdateFilesCompleted = n; }
}
