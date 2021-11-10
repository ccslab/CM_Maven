package kr.ac.konkuk.ccslab.cm.entity;

public class CMFileSyncBlockChecksum {
    private int blockIndex = 0;     // 0-base block index
    private int weakChecksum = 0;   // rolling checksum (32bit)
    private byte[] strongChecksum = null;  // MD5 (128bit)

    public int getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public int getWeakChecksum() {
        return weakChecksum;
    }

    public void setWeakChecksum(int weakChecksum) {
        this.weakChecksum = weakChecksum;
    }

    public byte[] getStrongChecksum() {
        return strongChecksum;
    }

    public void setStrongChecksum(byte[] strongChecksum) {
        this.strongChecksum = strongChecksum;
    }
}
