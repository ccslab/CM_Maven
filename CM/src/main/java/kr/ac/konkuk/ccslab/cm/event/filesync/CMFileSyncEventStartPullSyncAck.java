package kr.ac.konkuk.ccslab.cm.event.filesync;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * This class represents a CMFileSyncEvent with which the server replies to the client's
 * pull-sync start request, indicating how the client should proceed and the server's
 * current cursor value.
 * <br>returnCode:
 * <br>0  - new user (no cursor on server) -&gt; proceed with full push sync
 * <br>1  - up to date (same cursor) -&gt; end sync session
 * <br>-1 - error (server cursor &lt; client cursor) -&gt; recover with full push sync
 * <br>2  - pull sync needed -&gt; proceed with pull sync after receiving serverEntryList
 * @author CCSLab, Konkuk University
 */
public class CMFileSyncEventStartPullSyncAck extends CMFileSyncEvent {
    private int returnCode;     // return code
    private long serverCursor;  // server's current cursor value

    public CMFileSyncEventStartPullSyncAck() {
        m_nID = CMFileSyncEvent.START_PULL_SYNC_ACK;
        returnCode = -1;
        serverCursor = 0;
    }

    public CMFileSyncEventStartPullSyncAck(ByteBuffer msg) {
        this();
        unmarshall(msg);
    }

    @Override
    protected int getByteNum() {
        int byteNum;
        byteNum = super.getByteNum();
        // returnCode
        byteNum += Integer.BYTES;
        // serverCursor
        byteNum += Long.BYTES;
        return byteNum;
    }

    @Override
    protected void marshallBodyCore() {
        // returnCode
        m_bytes.putInt(returnCode);
        // serverCursor
        m_bytes.putLong(serverCursor);
    }

    @Override
    protected void unmarshallBodyCore(ByteBuffer msg) {
        // returnCode
        returnCode = msg.getInt();
        // serverCursor
        serverCursor = msg.getLong();
    }

    @Override
    public String toString() {
        return "CMFileSyncEventStartPullSyncAck{" +
                "m_nType=" + m_nType +
                ", m_nID=" + m_nID +
                ", m_strSender='" + m_strSender + '\'' +
                ", m_senderUuid=" + m_senderUuid +
                ", m_strReceiver='" + m_strReceiver + '\'' +
                ", m_receiverUuid=" + m_receiverUuid +
                ", m_distributionUuid=" + m_distributionUuid +
                ", m_nByteNum=" + m_nByteNum +
                ", initiatorName='" + getInitiatorName() + '\'' +
                ", returnCode=" + returnCode +
                ", serverCursor=" + serverCursor +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CMFileSyncEventStartPullSyncAck that = (CMFileSyncEventStartPullSyncAck) o;
        return returnCode == that.returnCode &&
                serverCursor == that.serverCursor &&
                getInitiatorName().equals(that.getInitiatorName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInitiatorName(), returnCode, serverCursor);
    }

    /**
     * gets the return code.
     * @return return code
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * gets the server's current cursor value.
     * @return server's current cursor value
     */
    public long getServerCursor() {
        return serverCursor;
    }

    public void setServerCursor(long serverCursor) {
        this.serverCursor = serverCursor;
    }
}