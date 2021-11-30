package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;

public class CMFileSyncEventStartFileBlockChecksum extends CMFileSyncEvent {
    private int fileEntryIndex; // client file entry index
    private int totalNumBlocks; // total number of blocks of this file
    private int blockSize;      // block size

    public CMFileSyncEventStartFileBlockChecksum() {
        m_nID = CMFileSyncEvent.START_FILE_BLOCK_CHECKSUM;
        fileEntryIndex = 0;
        totalNumBlocks = 0;
        blockSize = 0;
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
        return "CMFileSyncEventStartFileBlockChecksum{" +
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

    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) return false;
        if(!(obj instanceof CMFileSyncEventStartFileBlockChecksum fse)) return false;
        return fse.getBlockSize() == blockSize &&
                fse.getFileEntryIndex() == fileEntryIndex &&
                fse.getTotalNumBlocks() == totalNumBlocks;
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

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }
}
