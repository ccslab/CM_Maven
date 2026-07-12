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

// Server-side incremental-push Runnable. Mirror image of CMFileSyncPullGenerator (client-side,
// PULL MODIFY): the processing subject is the server and the send target is the client.
// General rule: "the Generator always lives on the basis-holding side = the side that creates
// block checksums". PULL basis is on the client (PullGenerator on the client); PUSH basis is on
// the server (PushGenerator on the server). The source-holding side is handled by the
// ModifyState holder.
// run() computes block checksums of the server basis files and sends START_FILE_BLOCK_CHECKSUM
// to the client. FILE_BLOCK_CHECKSUM and END_FILE_BLOCK_CHECKSUM are sent from the
// START_FILE_BLOCK_CHECKSUM_ACK handler (PUSH branch, F-2). UPDATE_EXISTING_FILE events from the
// client are applied via this generator's channels in the event handler (F-5). Lifecycle ends
// when all END_FILE_BLOCK_CHECKSUM_ACK events are processed at the server (F-6).
public class CMFileSyncPushGenerator implements Runnable {
    private String initiatorName;           // target client name (event receiver, unicast)
    private UUID initiatorUuid;             // client login UUID (unicast receiver + common field)
    private UUID initiatorDeviceUuid;       // client device UUID (common field; stateKey reconstruction)

    // input: modify entries classified by proceedPushStateMap; index == fileEntryIndex
    private List<CMFileSyncClientEntry> modifyEntries;

    // key: fileEntryIndex (== basisFileIndex in PUSH — no separate basisFileIndexMap needed)
    private Map<Integer, CMFileSyncBlockChecksum[]> blockChecksumArrayMap;
    private Map<Integer, Integer> blockSizeOfBasisFileMap;

    // opened lazily by the UPDATE_EXISTING_FILE handler (F-5) via pushGeneratorMap lookup
    private Map<Integer, SeekableByteChannel> basisFileChannelForReadMap;
    private Map<Integer, SeekableByteChannel> tempFileChannelForWriteMap;

    // key: relative path string (same convention as pushStateMap keys)
    private Map<String, Boolean> isUpdateFileCompletedMap;
    private int numUpdateFilesCompleted;

    public CMFileSyncPushGenerator(String initiatorName, UUID initiatorUuid,
                                   UUID initiatorDeviceUuid,
                                   List<CMFileSyncClientEntry> modifyEntries) {
        this.initiatorName = initiatorName;
        this.initiatorUuid = initiatorUuid;
        this.initiatorDeviceUuid = initiatorDeviceUuid;
        this.modifyEntries = modifyEntries;

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
            System.out.println("=== CMFileSyncPushGenerator.run() called..");

        if (modifyEntries.isEmpty()) return;

        // pre-register all entries as not-yet-completed before sending any checksum event
        for (CMFileSyncClientEntry entry : modifyEntries) {
            isUpdateFileCompletedMap.put(entry.getPath(), false);
        }

        boolean result = compareAndSendBlockChecksums();
        if (!result)
            System.err.println("CMFileSyncPushGenerator.run(), compareAndSendBlockChecksums() failed!");
    }

    // For each entry in modifyEntries: computes block checksum of the server basis file and sends
    // START_FILE_BLOCK_CHECKSUM to the client. FILE_BLOCK_CHECKSUM and END events are sent from
    // the START_ACK handler (PUSH branch, F-2). Mirrors CMFileSyncGenerator.compareBasisAndInitiatorFileList()
    // and CMFileSyncPullGenerator.compareAndSendBlockChecksums(); skip logic not needed because
    // proceedPushStateMap entries are already confirmed as MODIFY.
    private boolean compareAndSendBlockChecksums() {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPushGenerator.compareAndSendBlockChecksums() called..");

        CMInfo cmInfo = CMInfo.getInstance();
        CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
        Path serverSyncHome = syncManager.getServerSyncHome(initiatorName);

        for (int fileEntryIndex = 0; fileEntryIndex < modifyEntries.size(); fileEntryIndex++) {
            CMFileSyncClientEntry entry = modifyEntries.get(fileEntryIndex);
            Path basisFile = serverSyncHome.resolve(entry.getPath()).toAbsolutePath().normalize();

            if (!Files.exists(basisFile) || Files.isDirectory(basisFile)) {
                System.err.println("CMFileSyncPushGenerator.compareAndSendBlockChecksums(), "
                        + "basis file not found or is a directory: " + basisFile);
                continue;
            }

            CMFileSyncBlockChecksum[] checksumArray = createBlockChecksum(fileEntryIndex, basisFile);
            if (checksumArray == null) continue;

            blockChecksumArrayMap.put(fileEntryIndex, checksumArray);

            boolean sendResult = sendBlockChecksum(fileEntryIndex, checksumArray);
            if (!sendResult) {
                System.err.println("CMFileSyncPushGenerator.compareAndSendBlockChecksums(), "
                        + "send error at fileEntryIndex=" + fileEntryIndex);
                return false;
            }
        }

        return true;
    }

    // Mirrors CMFileSyncGenerator.createBlockChecksum(); basisFileIndex == fileEntryIndex here.
    private CMFileSyncBlockChecksum[] createBlockChecksum(int fileEntryIndex, Path basisFile) {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncPushGenerator.createBlockChecksum() called..");
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

    // Sends START_FILE_BLOCK_CHECKSUM to the client.
    // Mirrors CMFileSyncPullGenerator.sendBlockChecksum(); target is the client (login user uuid).
    private boolean sendBlockChecksum(int fileEntryIndex, CMFileSyncBlockChecksum[] checksumArray) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPushGenerator.sendBlockChecksum() called..");

        CMFileSyncEventStartFileBlockChecksum fse = new CMFileSyncEventStartFileBlockChecksum();
        fse.setInitiatorName(initiatorName);
        fse.setInitiatorUuid(initiatorUuid);
        fse.setInitiatorDeviceUuid(initiatorDeviceUuid);
        fse.setFileEntryIndex(fileEntryIndex);
        fse.setTotalNumBlocks(checksumArray.length);
        fse.setBlockSize(blockSizeOfBasisFileMap.get(fileEntryIndex));
        // push sync: carry the relative path so the client can map fileEntryIndex to a path
        fse.setRelativePath(modifyEntries.get(fileEntryIndex).getPath());

        boolean ret = CMEventManager.unicastEvent(fse, initiatorName, initiatorUuid);
        if (!ret)
            System.err.println("CMFileSyncPushGenerator.sendBlockChecksum(), send error: " + fse);
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
    public List<CMFileSyncClientEntry> getModifyEntries() { return modifyEntries; }
    public Map<Integer, CMFileSyncBlockChecksum[]> getBlockChecksumArrayMap() { return blockChecksumArrayMap; }
    public Map<Integer, Integer> getBlockSizeOfBasisFileMap() { return blockSizeOfBasisFileMap; }
    public Map<Integer, SeekableByteChannel> getBasisFileChannelForReadMap() { return basisFileChannelForReadMap; }
    public Map<Integer, SeekableByteChannel> getTempFileChannelForWriteMap() { return tempFileChannelForWriteMap; }
    public Map<String, Boolean> getIsUpdateFileCompletedMap() { return isUpdateFileCompletedMap; }
    public int getNumUpdateFilesCompleted() { return numUpdateFilesCompleted; }
    public void setNumUpdateFilesCompleted(int n) { this.numUpdateFilesCompleted = n; }

    // Called from the per-entry cleanup step of the END_FILE_BLOCK_CHECKSUM_ACK handler (F-6).
    // The fileEntryIndex channels are already closed by F-6 (ii); this method only clears the
    // index-keyed data. isUpdateFileCompletedMap is NOT removed here — it is needed for the
    // all-entries-completed decision and is cleared in bulk by cleanupAll().
    public void cleanupForFileEntry(int fileEntryIndex) {
        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPushGenerator.cleanupForFileEntry() called, fileEntryIndex="
                    + fileEntryIndex);

        // channels already closed in F-6 (ii) — only clear index-keyed data here
        blockChecksumArrayMap.remove(fileEntryIndex);
        blockSizeOfBasisFileMap.remove(fileEntryIndex);

        if (CMInfo._CM_DEBUG)
            System.out.println("after remove: blockChecksumArrayMap.size=" + blockChecksumArrayMap.size()
                    + ", blockSizeOfBasisFileMap.size=" + blockSizeOfBasisFileMap.size());
    }

    // Protective full cleanup at the end of the generator lifecycle. Mirror of
    // CMFileSyncPullGenerator.cleanupAll(). Called from F-6 (PUSH branch) right after the
    // all-entries-completed decision (numUpdateFilesCompleted == modifyEntries.size()) and just
    // before pushGeneratorMap.remove(loginKey). In the normal flow the per-fileEntryIndex channels
    // are already closed + removed by cleanupForFileEntry / F-6; this handles abnormal termination.
    public void cleanupAll() {
        if (CMInfo._CM_DEBUG) {
            System.out.println("=== CMFileSyncPushGenerator.cleanupAll() called..");
            System.out.println("basisFileChannelForReadMap.size=" + basisFileChannelForReadMap.size()
                    + ", tempFileChannelForWriteMap.size=" + tempFileChannelForWriteMap.size()
                    + ", blockChecksumArrayMap.size=" + blockChecksumArrayMap.size()
                    + ", blockSizeOfBasisFileMap.size=" + blockSizeOfBasisFileMap.size()
                    + ", isUpdateFileCompletedMap.size=" + isUpdateFileCompletedMap.size()
                    + ", numUpdateFilesCompleted=" + numUpdateFilesCompleted);
        }

        for (SeekableByteChannel ch : basisFileChannelForReadMap.values()) {
            if (ch != null && ch.isOpen()) {
                try { ch.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
        basisFileChannelForReadMap.clear();

        for (SeekableByteChannel ch : tempFileChannelForWriteMap.values()) {
            if (ch != null && ch.isOpen()) {
                try { ch.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
        tempFileChannelForWriteMap.clear();

        blockChecksumArrayMap.clear();
        blockSizeOfBasisFileMap.clear();
        isUpdateFileCompletedMap.clear();
        numUpdateFilesCompleted = 0;

        // modifyEntries is an external reference (passed in by proceedPushModifyEntries); not
        // cleared here. After this call pushGeneratorMap.remove(loginKey) makes the generator
        // unreachable, so the list reference becomes GC-eligible along with it.
        // Identifiers (initiatorName/Uuid/DeviceUuid) are left as-is for last-state debugging.

        if (CMInfo._CM_DEBUG)
            System.out.println("=== CMFileSyncPushGenerator.cleanupAll() done.");
    }
}
