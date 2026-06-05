package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the client acknowledges the completion
 * of the incremental push synchronization to the server, after it has finalized its in-memory
 * client-index and adopted the new cursor.
 * <br>Like the pull-sync completion ack, this is sent client->server. On receiving it, the
 * server cleans up pushStateTable[stateKey] and its state holder.
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventCompletePushSyncAck extends CMFileSyncEvent {
    // Fields: numFilesCompleted, returnCode
    private int numFilesCompleted;  // number of completed push-sync entries reported by the server (echo)
    private int returnCode;         // client-side cursor/client-index application result (1: success, 0: failure)

    public CMFileSyncEventCompletePushSyncAck() {
        m_nID = CMFileSyncEvent.COMPLETE_PUSH_SYNC_ACK;
        numFilesCompleted = 0;
        returnCode = -1;
    }

    public CMFileSyncEventCompletePushSyncAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // numFilesCompleted
        byteNum += Integer.BYTES;
        // returnCode
        byteNum += Integer.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // numFilesCompleted
        m_bytes.putInt(numFilesCompleted);
        // returnCode
        m_bytes.putInt(returnCode);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // numFilesCompleted
        numFilesCompleted = msg.getInt();
        // returnCode
        returnCode = msg.getInt();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventCompletePushSyncAck{" +
                "m_nType=" + m_nType +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nID=" + m_nID +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", numFilesCompleted=" + numFilesCompleted +
                ", returnCode=" + returnCode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventCompletePushSyncAck that = (CMFileSyncEventCompletePushSyncAck) o;
        return numFilesCompleted == that.numFilesCompleted &&
                returnCode == that.returnCode &&
                getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), numFilesCompleted, returnCode);
    }

    /**
     * gets the number of completed push-sync entries reported by the server.
     * @return number of completed push-sync entries
     */
    public int getNumFilesCompleted() {
        return numFilesCompleted;
    }

    public void setNumFilesCompleted(int numFilesCompleted) {
        this.numFilesCompleted = numFilesCompleted;
    }

    /**
     * gets the return code.
     * @return return code (1: success, 0: failure)
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
