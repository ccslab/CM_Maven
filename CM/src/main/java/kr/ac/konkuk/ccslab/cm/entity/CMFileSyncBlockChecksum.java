package kr.ac.konkuk.ccslab.cm.entity;

import java.util.Arrays;

public class CMFileSyncBlockChecksum {
    private int blockIndex;     // 0-base block index
    private int weakChecksum;   // rolling checksum (32bit)
    private byte[] strongChecksum;  // MD5 (128bit)

    public CMFileSyncBlockChecksum() {
        blockIndex = 0;
        weakChecksum = -1;
        strongChecksum = null;
    }

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

    @Override
    public String toString() {
        return "CMFileSyncBlockChecksum{" +
                "blockIndex=" + blockIndex +
                ", weakChecksum=" + weakChecksum +
                ", strongChecksum=" + Arrays.toString(strongChecksum) +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(!(obj instanceof CMFileSyncBlockChecksum blockChecksum)) return false;
        return blockChecksum.getBlockIndex() == blockIndex &&
                blockChecksum.getWeakChecksum() == weakChecksum &&
                Arrays.equals(blockChecksum.getStrongChecksum(), strongChecksum);
    }
}
