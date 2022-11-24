package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server notifies the client of
 * the completion of sending block checksums of a file.
 * @author CCSLab, Konkuk University
 * */
public class CMFileSyncEventEndFileBlockChecksum extends CMFileSyncEvent {
    private int fileEntryIndex; // client file entry index
    private int totalNumBlocks; // total number of blocks of this file
    private int blockSize;      // block size

    public CMFileSyncEventEndFileBlockChecksum() {
        m_nID = CMFileSyncEvent.END_FILE_BLOCK_CHECKSUM;
        fileEntryIndex = 0;
        totalNumBlocks = 0;
        blockSize = 0;
    }

    public CMFileSyncEventEndFileBlockChecksum(ByteBuffer msg) {
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
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // fileEntryIndex
        m_bytes.putInt(fileEntryIndex);
        // totalNumBlocks
        m_bytes.putInt(totalNumBlocks);
        // blocksSize
        m_bytes.putInt(blockSize);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // fileEntryIndex
        fileEntryIndex = msg.getInt();
        // totalNumBlocks
        totalNumBlocks = msg.getInt();
        // blocksSize
        blockSize = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventEndFileBlockChecksum{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nByteNum=" + m_nByteNum +
                ", fileEntryIndex=" + fileEntryIndex +
                ", totalNumBlocks=" + totalNumBlocks +
                ", blockSize=" + blockSize +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventEndFileBlockChecksum fse)) return false;
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
        CMFileSyncEventEndFileBlockChecksum that = (CMFileSyncEventEndFileBlockChecksum) o;
        return fileEntryIndex == that.fileEntryIndex &&
                totalNumBlocks == that.totalNumBlocks && blockSize == that.blockSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileEntryIndex, totalNumBlocks, blockSize);
    }

    /**
     * gets an index of a file entry for synchronization.
     * @return index of a file entry for synchronization
     */
    public int getFileEntryIndex() {
        return fileEntryIndex;
    }

    public void setFileEntryIndex(int fileEntryIndex) {
        this.fileEntryIndex = fileEntryIndex;
    }

    /**
     * gets the total number of blocks of the file.
     * @return total number of blocks of the file
     */
    public int getTotalNumBlocks() {
        return totalNumBlocks;
    }

    public void setTotalNumBlocks(int totalNumBlocks) {
        this.totalNumBlocks = totalNumBlocks;
    }

    /**
     * gets the block size of the file.
     * @return block size of the file
     */
    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }
}
