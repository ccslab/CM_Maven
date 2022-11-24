package kr.ac.konkuk.ccslab.cm.event.filesync;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client replies to the server about
 * the completion of receiving block checksums of a file.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventEndFileBlockChecksumAck extends CMFileSyncEvent {
    private int fileEntryIndex; // client file entry index
    private int totalNumBlocks; // total number of blocks of this file
    private int blockSize;      // block size
    private byte[] fileChecksum;   // file checksum (MD5)
    private int returnCode;

    public CMFileSyncEventEndFileBlockChecksumAck() {
        m_nID = CMFileSyncEvent.END_FILE_BLOCK_CHECKSUM_ACK;
        fileEntryIndex = 0;
        totalNumBlocks = 0;
        blockSize = 0;
        fileChecksum = null;
        returnCode = -1;
    }

    public CMFileSyncEventEndFileBlockChecksumAck(ByteBuffer msg) {
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
        // numFileChecksumBytes
        byteNum += Integer.BYTES;
        // fileChecksum
        if(fileChecksum != null)
            byteNum += fileChecksum.length;
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
        // numFileChecksumBytes, fileChecksum
        if(fileChecksum != null) {
            m_bytes.putInt(fileChecksum.length);
            m_bytes.put(fileChecksum);
        }
        else {
            m_bytes.putInt(0);
        }
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        int numFileChecksumBytes = 0;

        // fileEntryIndex
        fileEntryIndex = msg.getInt();
        // totalNumBlocks
        totalNumBlocks = msg.getInt();
        // blocksSize
        blockSize = msg.getInt();
        // numFileChecksumBytes
        numFileChecksumBytes = msg.getInt();
        // fileChecksum
        if(numFileChecksumBytes > 0) {
            fileChecksum = new byte[numFileChecksumBytes];
            msg.get(fileChecksum);
        }
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        String checksum = null;
        if(fileChecksum != null)
            checksum = DatatypeConverter.printHexBinary(fileChecksum).toUpperCase();

        return "CMFileSyncEventEndFileBlockChecksumAck{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nByteNum=" + m_nByteNum +
                ", fileEntryIndex=" + fileEntryIndex +
                ", totalNumBlocks=" + totalNumBlocks +
                ", blockSize=" + blockSize +
                ", fileChecksum=" + checksum +
                ", returnCode=" + returnCode +
                '}';
    }

/*
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventEndFileBlockChecksumAck fse)) return false;
        return fse.getBlockSize() == blockSize &&
                Arrays.equals(fse.getFileChecksum(), fileChecksum) &&
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
        CMFileSyncEventEndFileBlockChecksumAck that = (CMFileSyncEventEndFileBlockChecksumAck) o;
        return fileEntryIndex == that.fileEntryIndex &&
                totalNumBlocks == that.totalNumBlocks &&
                blockSize == that.blockSize &&
                returnCode == that.returnCode &&
                Arrays.equals(fileChecksum, that.fileChecksum);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileEntryIndex, totalNumBlocks, blockSize, returnCode);
        result = 31 * result + Arrays.hashCode(fileChecksum);
        return result;
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

    /**
     * gets the file checksum.
     * @return file checksum
     */
    public byte[] getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(byte[] fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    /**
     * gets the return code.
     * @return return code
     * <br> 1 if successful; 0 otherwise
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
