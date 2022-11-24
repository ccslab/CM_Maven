package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client acknowledges the reception of
 * the start of sending block checksums of a file from the server.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventStartFileBlockChecksumAck extends CMFileSyncEvent {
    private int fileEntryIndex; // client file entry index
    private int totalNumBlocks; // total number of blocks of this file
    private int blockSize;      // block size
    private int returnCode;     // return code

    public CMFileSyncEventStartFileBlockChecksumAck() {
        m_nID = CMFileSyncEvent.START_FILE_BLOCK_CHECKSUM_ACK;
        fileEntryIndex = 0;
        totalNumBlocks = 0;
        blockSize = 0;
        returnCode = -1;
    }

    public CMFileSyncEventStartFileBlockChecksumAck(ByteBuffer msg) {
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
        // returnCode
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
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        // fileEntryIndex
        fileEntryIndex = msg.getInt();
        // totalNumBlocks
        totalNumBlocks = msg.getInt();
        // blocksSize
        blockSize = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartFileBlockChecksumAck{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", fileEntryIndex=" + fileEntryIndex +
                ", totalNumBlocks=" + totalNumBlocks +
                ", blockSize=" + blockSize +
                ", returnCode=" + returnCode +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventStartFileBlockChecksumAck fse)) return false;
        return fse.getBlockSize() == blockSize &&
                fse.getFileEntryIndex() == fileEntryIndex &&
                fse.getTotalNumBlocks() == totalNumBlocks &&
                fse.getReturnCode() == returnCode;
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventStartFileBlockChecksumAck that = (CMFileSyncEventStartFileBlockChecksumAck) o;
        return fileEntryIndex == that.fileEntryIndex &&
                totalNumBlocks == that.totalNumBlocks &&
                blockSize == that.blockSize &&
                returnCode == that.returnCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileEntryIndex, totalNumBlocks, blockSize, returnCode);
    }

    /**
     * gets the index of target file entry
     * @return the index of target file entry
     */
    public int getFileEntryIndex() {
        return fileEntryIndex;
    }

    public void setFileEntryIndex(int fileEntryIndex) {
        this.fileEntryIndex = fileEntryIndex;
    }

    /**
     * gets the total number of blocks of the target file.
     * @return total number of target file blocks
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
     * gets the return code.
     * @return return code
     * <br>1 if successful; 0 otherwise.
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
