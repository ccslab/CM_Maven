package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncClientEntry;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client sends the server
 * a list of push (client) entries for incremental push synchronization.
 * <br>If multiple lists exist, the next list is sent upon receiving the ack of this event.
 * <br>This is the reverse-direction counterpart of {@link CMFileSyncEventServerEntries},
 * which the server sends to the client in the pull synchronization. Unlike that event,
 * the entry type here is {@link CMFileSyncClientEntry} so that the op-classification
 * metadata (curMtime/baseMtime/opHint) travels with each entry.
 * <br>The {@code serverMtime} field of CMFileSyncClientEntry is PULL-only and is NOT
 * transmitted.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventPushEntries extends CMFileSyncEvent {
    private int numFilesCompleted;  // number of files completed
    private int numFiles;           // number of current files
    private List<CMFileSyncClientEntry> pushEntryList;  // list of CMFileSyncClientEntry

    // Enum values cached to map opHint ordinal <-> enum during (un)marshalling.
    private static final CMFileSyncOp[] OP_VALUES = CMFileSyncOp.values();

    public CMFileSyncEventPushEntries() {
        m_nID = CMFileSyncEvent.PUSH_ENTRIES;
        numFilesCompleted = 0;
        numFiles = 0;
        pushEntryList = null;
    }

    public CMFileSyncEventPushEntries(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    public int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numFilesCompleted
        byteNum += Integer.BYTES;
        // numFiles
        byteNum += Integer.BYTES;
        // number of elements of pushEntryList
        byteNum += Integer.BYTES;
        // pushEntryList (field-by-field; serverMtime excluded)
        if (pushEntryList != null) {
            for (CMFileSyncClientEntry entry : pushEntryList) {
                // path
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + entry.getPath().getBytes().length;
                // size
                byteNum += Long.BYTES;
                // curMtime
                byteNum += Long.BYTES;
                // baseMtime
                byteNum += Long.BYTES;
                // opHint (ordinal as int)
                byteNum += Integer.BYTES;
                // isCompleted (as byte)
                byteNum += Byte.BYTES;
            }
        }
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
        // numFiles
        m_bytes.putInt(numFiles);
        if (pushEntryList != null) {
            // number of elements of pushEntryList
            m_bytes.putInt(pushEntryList.size());
            // pushEntryList (serverMtime excluded — PULL-only field)
            for (CMFileSyncClientEntry entry : pushEntryList) {
                putStringToByteBuffer(entry.getPath());
                m_bytes.putLong(entry.getSize());
                m_bytes.putLong(entry.getCurMtime());
                m_bytes.putLong(entry.getBaseMtime());
                CMFileSyncOp op = entry.getOpHint();
                m_bytes.putInt(op == null ? CMFileSyncOp.UNKNOWN.ordinal() : op.ordinal());
                m_bytes.put((byte) (entry.isCompleted() ? 1 : 0));
            }
        } else {
            m_bytes.putInt(0);
        }
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
        // numFiles
        numFiles = msg.getInt();
        // pushEntryList
        int numEntries = msg.getInt();
        if (numEntries > 0) {
            pushEntryList = new ArrayList<>();
            for (int i = 0; i < numEntries; i++) {
                CMFileSyncClientEntry entry = new CMFileSyncClientEntry()
                        .setPath(getStringFromByteBuffer(msg))
                        .setSize(msg.getLong())
                        .setCurMtime(msg.getLong())
                        .setBaseMtime(msg.getLong())
                        .setOpHint(ordinalToOp(msg.getInt()))
                        .setCompleted(msg.get() != 0);
                // serverMtime stays at its default (-1): PULL-only, not transmitted
                pushEntryList.add(entry);
            }
        }
    }

    private static CMFileSyncOp ordinalToOp(int ordinal) {
        if (ordinal < 0 || ordinal >= OP_VALUES.length) return CMFileSyncOp.UNKNOWN;
        return OP_VALUES[ordinal];
    }

    @Override
    public String toString() {
        return "CMFileSyncEventPushEntries{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", numFilesCompleted=" + numFilesCompleted +
                ", numFiles=" + numFiles +
                ", pushEntryList=" + pushEntryList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventPushEntries that = (CMFileSyncEventPushEntries) o;
        return numFilesCompleted == that.numFilesCompleted &&
                numFiles == that.numFiles &&
                getInitiatorName().equals(that.getInitiatorName()) &&
                Objects.equals(pushEntryList, that.pushEntryList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numFilesCompleted, numFiles, pushEntryList);
    }

    /**
     * gets the number of push entries that have been already transferred.
     * @return number of push entries that have been already transferred
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }

    /**
     * gets the number of push entries in this event.
     * @return number of push entries in this event
     */
    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }

    /**
     * gets the list of push entries in this event.
     * @return a list of push entries
     */
    public List<CMFileSyncClientEntry> getPushEntryList() {
        return pushEntryList;
    }

    public void setPushEntryList(List<CMFileSyncClientEntry> pushEntryList) {
        this.pushEntryList = pushEntryList;
    }
}
