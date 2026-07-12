package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * the start of sending block checksums of a file.
 * <br>In pull sync the direction is reversed (CLIENT -&gt; SERVER); the client's
 * CMFileSyncPullGenerator fills {@code relativePath} so the server can map fileEntryIndex
 * to a path. It stays empty in the full push direction.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventStartFileBlockChecksum extends CMFileSyncEvent {
    private int fileEntryIndex; // client file entry index
    private int totalNumBlocks; // total number of blocks of this file
    private int blockSize;      // block size
    private String relativePath;    // sync-home-relative path ('/' separated); empty in full push

    public CMFileSyncEventStartFileBlockChecksum() {
        m_nID = CMFileSyncEvent.START_FILE_BLOCK_CHECKSUM;
        fileEntryIndex = 0;
        totalNumBlocks = 0;
        blockSize = 0;
        relativePath = "";
    }

    public CMFileSyncEventStartFileBlockChecksum(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // fileEntryIndex
        byteNum += Integer.BYTES;
        // totalNumBlocks
        byteNum += Integer.BYTES;
        // blockSize
        byteNum += Integer.BYTES;
        // relativePath
        byteNum += CMInfo.STRING_LEN_BYTES_LEN + relativePath.getBytes().length;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // fileEntryIndex
        m_bytes.putInt(fileEntryIndex);
        // totalNumBlocks
        m_bytes.putInt(totalNumBlocks);
        // blocksSize
        m_bytes.putInt(blockSize);
        // relativePath
        putStringToByteBuffer(relativePath);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // fileEntryIndex
        fileEntryIndex = msg.getInt();
        // totalNumBlocks
        totalNumBlocks = msg.getInt();
        // blocksSize
        blockSize = msg.getInt();
        // relativePath
        relativePath = getStringFromByteBuffer(msg);
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartFileBlockChecksum{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nByteNum=" + m_nByteNum +
                ", fileEntryIndex=" + fileEntryIndex +
                ", totalNumBlocks=" + totalNumBlocks +
                ", blockSize=" + blockSize +
                ", relativePath='" + relativePath + '\'' +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventStartFileBlockChecksum fse)) return false;
        return fse.getBlockSize() == blockSize &&
                fse.getFileEntryIndex() == fileEntryIndex &&
                fse.getTotalNumBlocks() == totalNumBlocks;
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventStartFileBlockChecksum that = (CMFileSyncEventStartFileBlockChecksum) o;
        return fileEntryIndex == that.fileEntryIndex &&
                totalNumBlocks == that.totalNumBlocks &&
                blockSize == that.blockSize &&
                Objects.equals(relativePath, that.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileEntryIndex, totalNumBlocks, blockSize, relativePath);
    }

    /**
     * gets the index of the target file entry.
     * @return file entry index
     */
    public int getFileEntryIndex() {
        return fileEntryIndex;
    }

    public void setFileEntryIndex(int fileEntryIndex) {
        this.fileEntryIndex = fileEntryIndex;
    }

    /**
     * gets the total number of target file blocks.
     * @return total number of blocks of the file
     */
    public int getTotalNumBlocks() {
        return totalNumBlocks;
    }

    public void setTotalNumBlocks(int totalNumBlocks) {
        this.totalNumBlocks = totalNumBlocks;
    }

    /**
     * gets the block size of the target file.
     * @return block size of the target file
     */
    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    /**
     * gets the sync-home-relative path of the target file ('/' separated).
     * @return relative path; empty string in the full push direction
     */
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = (relativePath == null ? "" : relativePath);
    }
}
