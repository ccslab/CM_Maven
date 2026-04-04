package kr.ac.konkuk.ccslab.cm.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CMFileSyncChangeLogEntry implements Comparable<CMFileSyncChangeLogEntry> {

    private long changeId;              // 순차 변경 식별자
    private String userName;            // 변경을 발생시킨 사용자명
    private UUID originDeviceUuid;      // 변경 발생 디바이스 UUID
    private CMFileSyncOp op;            // 작업 종류 (CREATE/MODIFY/DELETE/UNKNOWN)
    private String path;                // sync home 기준 상대 경로 ('/' 구분자)
    private boolean isDirectory;        // 디렉토리 여부
    private String contentHash;         // MD5 해시 (DELETE 시 null)
    private long mtime;                 // 수정 시각 (epoch seconds)
    private long size;                  // 파일 크기 (bytes)
    private boolean tombstone;          // 삭제 여부 (op==DELETE일 때 true)
    private OffsetDateTime ts;          // 변경 로그 기록 시각 (ISO 8601)

    public CMFileSyncChangeLogEntry() {
        changeId = 0;
        userName = null;
        originDeviceUuid = null;
        op = CMFileSyncOp.UNKNOWN;
        path = null;
        isDirectory = false;
        contentHash = null;
        mtime = 0;
        size = 0;
        tombstone = false;
        ts = null;
    }

    public long getChangeId() {
        return changeId;
    }

    public CMFileSyncChangeLogEntry setChangeId(long changeId) {
        this.changeId = changeId;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public CMFileSyncChangeLogEntry setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public UUID getOriginDeviceUuid() {
        return originDeviceUuid;
    }

    public CMFileSyncChangeLogEntry setOriginDeviceUuid(UUID originDeviceUuid) {
        this.originDeviceUuid = originDeviceUuid;
        return this;
    }

    public CMFileSyncOp getOp() {
        return op;
    }

    public CMFileSyncChangeLogEntry setOp(CMFileSyncOp op) {
        this.op = op;
        return this;
    }

    public String getPath() {
        return path;
    }

    public CMFileSyncChangeLogEntry setPath(String path) {
        this.path = path;
        return this;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public CMFileSyncChangeLogEntry setDirectory(boolean directory) {
        this.isDirectory = directory;
        return this;
    }

    public String getContentHash() {
        return contentHash;
    }

    public CMFileSyncChangeLogEntry setContentHash(String contentHash) {
        this.contentHash = contentHash;
        return this;
    }

    public long getMtime() {
        return mtime;
    }

    public CMFileSyncChangeLogEntry setMtime(long mtime) {
        this.mtime = mtime;
        return this;
    }

    public long getSize() {
        return size;
    }

    public CMFileSyncChangeLogEntry setSize(long size) {
        this.size = size;
        return this;
    }

    public boolean isTombstone() {
        return tombstone;
    }

    public CMFileSyncChangeLogEntry setTombstone(boolean tombstone) {
        this.tombstone = tombstone;
        return this;
    }

    public OffsetDateTime getTs() {
        return ts;
    }

    public CMFileSyncChangeLogEntry setTs(OffsetDateTime ts) {
        this.ts = ts;
        return this;
    }

    public String toJsonString() throws JsonProcessingException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("changeId", changeId);
        map.put("userName", userName);
        map.put("originDeviceUuid", originDeviceUuid);
        map.put("op", op != null ? op.name() : null);
        map.put("path", path);
        map.put("isDirectory", isDirectory);
        map.put("contentHash", contentHash);
        map.put("mtime", mtime);
        map.put("size", size);
        map.put("tombstone", tombstone);
        map.put("ts", ts != null ? ts.toString() : null);
        return new ObjectMapper().writeValueAsString(map);
    }

    @SuppressWarnings("unchecked")
    public static CMFileSyncChangeLogEntry fromJsonString(String json) throws JsonProcessingException {
        Map<String, Object> map = new ObjectMapper().readValue(json, LinkedHashMap.class);
        CMFileSyncChangeLogEntry entry = new CMFileSyncChangeLogEntry();
        entry.setChangeId(((Number) map.get("changeId")).longValue());
        entry.setUserName((String) map.get("userName"));
        Object uuidObj = map.get("originDeviceUuid");
        if (uuidObj != null) {
            entry.setOriginDeviceUuid(UUID.fromString(uuidObj.toString()));
        }
        String opStr = (String) map.get("op");
        if (opStr != null) {
            entry.setOp(CMFileSyncOp.valueOf(opStr));
        }
        entry.setPath((String) map.get("path"));
        entry.setDirectory(Boolean.TRUE.equals(map.get("isDirectory")));
        entry.setContentHash((String) map.get("contentHash"));
        Object mtimeObj = map.get("mtime");
        if (mtimeObj != null) {
            entry.setMtime(((Number) mtimeObj).longValue());
        }
        Object sizeObj = map.get("size");
        if (sizeObj != null) {
            entry.setSize(((Number) sizeObj).longValue());
        }
        entry.setTombstone(Boolean.TRUE.equals(map.get("tombstone")));
        String tsStr = (String) map.get("ts");
        if (tsStr != null) {
            entry.setTs(OffsetDateTime.parse(tsStr));
        }
        return entry;
    }

    @Override
    public int compareTo(CMFileSyncChangeLogEntry o) {
        return Long.compare(this.changeId, o.getChangeId());
    }

    @Override
    public String toString() {
        return "CMFileSyncChangeLogEntry{" +
                "changeId=" + changeId +
                ", userName='" + userName + '\'' +
                ", originDeviceUuid=" + originDeviceUuid +
                ", op=" + op +
                ", path='" + path + '\'' +
                ", isDirectory=" + isDirectory +
                ", contentHash='" + contentHash + '\'' +
                ", mtime=" + mtime +
                ", size=" + size +
                ", tombstone=" + tombstone +
                ", ts=" + ts +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof CMFileSyncChangeLogEntry entry)) return false;
        return entry.getChangeId() == changeId &&
                Objects.equals(entry.getUserName(), userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(changeId, userName);
    }
}
