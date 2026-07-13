package kr.ac.konkuk.ccslab.cm.event.filesync;

import com.fasterxml.jackson.core.JsonProcessingException;
import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncChangeLogEntry;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server sends the client
 * a list of server entries (change-log entries) for pull synchronization.
 * <br>If multiple lists exist, the next list is sent upon receiving the ack of this event.
 * <br>This is the reverse-direction counterpart of {@link CMFileSyncEventFileEntries},
 * which the client sends to the server in the full push synchronization.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventServerEntries extends CMFileSyncEvent {
    private int numFilesCompleted;  // number of files completed
    private int numFiles;           // number of current files
    private List<CMFileSyncChangeLogEntry> serverEntryList;  // list of CMFileSyncChangeLogEntry

    public CMFileSyncEventServerEntries() {
        m_nID = CMFileSyncEvent.SERVER_ENTRIES;
        numFilesCompleted = 0;
        numFiles = 0;
        serverEntryList = null;
    }

    public CMFileSyncEventServerEntries(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    // CMFileSyncChangeLogEntry is serialized as a JSON string to preserve nullable
    // fields (originDeviceUuid, contentHash, ts) without ambiguity.
    private static String entryToJson(CMFileSyncChangeLogEntry entry) {
        try {
            return entry.toJsonString();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static CMFileSyncChangeLogEntry jsonToEntry(String json) {
        try {
            return CMFileSyncChangeLogEntry.fromJsonString(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new CMFileSyncChangeLogEntry();
        }
    }

    @Override
    public int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numFilesCompleted
        byteNum += Integer.BYTES;
        // numFiles
        byteNum += Integer.BYTES;
        // number of elements of serverEntryList
        byteNum += Integer.BYTES;
        // serverEntryList (each entry as a JSON string)
        if (serverEntryList != null) {
            for (CMFileSyncChangeLogEntry entry : serverEntryList) {
                String json = entryToJson(entry);
                byteNum += CMInfo.STRING_LEN_BYTES_LEN + json.getBytes().length;
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
        if (serverEntryList != null) {
            // number of elements of serverEntryList
            m_bytes.putInt(serverEntryList.size());
            // serverEntryList
            for (CMFileSyncChangeLogEntry entry : serverEntryList) {
                putStringToByteBuffer(entryToJson(entry));
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
        // serverEntryList
        int numEntries = msg.getInt();
        if (numEntries > 0) {
            serverEntryList = new ArrayList<>();
            for (int i = 0; i < numEntries; i++) {
                serverEntryList.add(jsonToEntry(getStringFromByteBuffer(msg)));
            }
        }
    }

    @Override
    public String toString() {
        return "CMFileSyncEventServerEntries{" +
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
                ", serverEntryList=" + serverEntryList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventServerEntries that = (CMFileSyncEventServerEntries) o;
        return numFilesCompleted == that.numFilesCompleted &&
                numFiles == that.numFiles &&
                getInitiatorName().equals(that.getInitiatorName()) &&
                Objects.equals(serverEntryList, that.serverEntryList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numFilesCompleted, numFiles, serverEntryList);
    }

    /**
     * gets the number of server entries that have been already transferred.
     * @return number of server entries that have been already transferred
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }

    /**
     * gets the number of server entries in this event.
     * @return number of server entries in this event
     */
    public int getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }

    /**
     * gets the list of server entries in this event.
     * @return a list of server entries
     */
    public List<CMFileSyncChangeLogEntry> getServerEntryList() {
        return serverEntryList;
    }

    public void setServerEntryList(List<CMFileSyncChangeLogEntry> serverEntryList) {
        this.serverEntryList = serverEntryList;
    }
}
