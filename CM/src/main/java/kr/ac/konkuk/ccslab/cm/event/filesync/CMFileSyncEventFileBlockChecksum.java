package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncBlockChecksum;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CMFileSyncEventFileBlockChecksum extends CMFileSyncEvent {
    private int fileEntryIndex;     // client file entry index
    private int totalNumBlocks;     // total number of blocks
    private int startBlockIndex;    // index of the starting block checksum of all blocks
    private int numCurrentBlocks;   // number of block checksum in this event
    private CMFileSyncBlockChecksum[] checksumArray;    // array of block checksums

    public CMFileSyncEventFileBlockChecksum() {
        m_nID = CMFileSyncEvent.FILE_BLOCK_CHECKSUM;
        fileEntryIndex = 0;
        totalNumBlocks = 0;
        startBlockIndex = 0;
        numCurrentBlocks = 0;
        checksumArray = null;
    }

    public CMFileSyncEventFileBlockChecksum(ByteBuffer msg) {
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
        // startBlockIndex
        byteNum += Integer.BYTES;
        // numCurrentBlocks
        byteNum += Integer.BYTES;
        // checksumArray
        if(checksumArray != null) {
            // CMFileSyncBlockChecksum (int blockIndex, int weakChecksum, byte[] strongChecksum)
            for(CMFileSyncBlockChecksum blockChecksum : checksumArray) {
                byteNum += Integer.BYTES;   // int blockIndex
                byteNum += Integer.BYTES;   // int weakChecksum
                byteNum += Integer.BYTES;   // size of the byte array
                if(blockChecksum.getStrongChecksum() != null)
                    byteNum += blockChecksum.getStrongChecksum().length;    // byte[] strongChecksum
            }
        }
        return byteNum;
    }

    @Override
    protected void marshallBody() {
        // fileEntryIndex
        m_bytes.putInt(fileEntryIndex);
        // totalNumBlocks
        m_bytes.putInt(totalNumBlocks);
        // startBlockIndex
        m_bytes.putInt(startBlockIndex);
        // numCurrentBlocks
        m_bytes.putInt(numCurrentBlocks);
        // checksumArray
        if(checksumArray != null) {
            for(CMFileSyncBlockChecksum blockChecksum : checksumArray) {
                // int blockIndex
                m_bytes.putInt(blockChecksum.getBlockIndex());
                // int weakChecksum
                m_bytes.putInt(blockChecksum.getWeakChecksum());
                if(blockChecksum.getStrongChecksum() != null) {
                    // size of byte[]
                    m_bytes.putInt(blockChecksum.getStrongChecksum().length);
                    // byte[] strongChecksum
                    m_bytes.put(blockChecksum.getStrongChecksum());
                }
                else {
                    // size of byte[]
                    m_bytes.putInt(0);
                }
            }
        }
    }

    @Override
    protected void unmarshallBody(ByteBuffer msg) {
        int strongChecksumLength = 0;

        // fileEntryIndex
        fileEntryIndex = msg.getInt();
        // totalNumBlocks
        totalNumBlocks = msg.getInt();
        // startBlockIndex
        startBlockIndex = msg.getInt();
        // numCurrentBlocks
        numCurrentBlocks = msg.getInt();
        if(numCurrentBlocks > 0) {
            // checksumArray
            checksumArray = new CMFileSyncBlockChecksum[numCurrentBlocks];
            for(int i = 0; i < numCurrentBlocks; i++) {
                CMFileSyncBlockChecksum blockChecksum = new CMFileSyncBlockChecksum();
                // int blockIndex
                blockChecksum.setBlockIndex(msg.getInt());
                // int weakChecksum
                blockChecksum.setWeakChecksum(msg.getInt());
                // byte[] strongChecksum
                strongChecksumLength = msg.getInt();
                if(strongChecksumLength > 0) {
                    byte[] strongChecksum = new byte[strongChecksumLength];
                    msg.get(strongChecksum);
                    blockChecksum.setStrongChecksum(strongChecksum);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "CMFileSyncEventFileBlockChecksum{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", fileEntryIndex=" + fileEntryIndex +
                ", totalNumBlocks=" + totalNumBlocks +
                ", startBlockIndex=" + startBlockIndex +
                ", numCurrentBlocks=" + numCurrentBlocks +
                ", checksumArray=" + Arrays.toString(checksumArray) +
                '}';
    }

    public int getFileEntryIndex() {
        return fileEntryIndex;
    }

    public void setFileEntryIndex(int fileEntryIndex) {
        this.fileEntryIndex = fileEntryIndex;
    }

    public int getTotalNumBlocks() {
        return totalNumBlocks;
    }

    public void setTotalNumBlocks(int totalNumBlocks) {
        this.totalNumBlocks = totalNumBlocks;
    }

    public int getStartBlockIndex() {
        return startBlockIndex;
    }

    public void setStartBlockIndex(int startBlockIndex) {
        this.startBlockIndex = startBlockIndex;
    }

    public int getNumCurrentBlocks() {
        return numCurrentBlocks;
    }

    public void setNumCurrentBlocks(int numCurrentBlocks) {
        this.numCurrentBlocks = numCurrentBlocks;
    }

    public CMFileSyncBlockChecksum[] getChecksumArray() {
        return checksumArray;
    }

    public void setChecksumArray(CMFileSyncBlockChecksum[] checksumArray) {
        this.checksumArray = checksumArray;
    }
}