package kr.ac.konkuk.ccslab.cm.entity;

import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncOp;

import java.util.Objects;

public class CMFileSyncClientEntry implements Comparable<CMFileSyncClientEntry> {

    private String path;            // sync home 기준 상대 경로 ('/' 구분자)
    private long size;              // 파일 크기 (bytes)
    private long curMtime;          // 클라이언트 현재 mtime (epoch seconds, -1이면 없음)
    private long baseMtime;         // 마지막 동기화 시점 mtime (epoch seconds, -1이면 없음)
    private CMFileSyncOp opHint;    // CREATE/MODIFY/DELETE/UNKNOWN
    private boolean isCompleted;    // 동기화 완료 여부
    private long serverMtime;       // 서버측 mtime (epoch seconds, -1이면 미설정; PULL 전용, 전송 제외)

    public CMFileSyncClientEntry() {
        this.path = null;
        size = 0;
        curMtime = -1;
        baseMtime = -1;
        opHint = CMFileSyncOp.UNKNOWN;
        isCompleted = false;
        serverMtime = -1;
    }

    public String getPath() {
        return path;
    }

    public CMFileSyncClientEntry setPath(String path) {
        this.path = path;
        return this;
    }

    public long getSize() {
        return size;
    }

    public CMFileSyncClientEntry setSize(long size) {
        this.size = size;
        return this;
    }

    public long getCurMtime() {
        return curMtime;
    }

    public CMFileSyncClientEntry setCurMtime(long curMtime) {
        this.curMtime = curMtime;
        return this;
    }

    public long getBaseMtime() {
        return baseMtime;
    }

    public CMFileSyncClientEntry setBaseMtime(long baseMtime) {
        this.baseMtime = baseMtime;
        return this;
    }

    public CMFileSyncOp getOpHint() {
        return opHint;
    }

    public CMFileSyncClientEntry setOpHint(CMFileSyncOp opHint) {
        this.opHint = opHint;
        return this;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public CMFileSyncClientEntry setCompleted(boolean completed) {
        this.isCompleted = completed;
        return this;
    }

    public long getServerMtime() {
        return serverMtime;
    }

    public CMFileSyncClientEntry setServerMtime(long serverMtime) {
        this.serverMtime = serverMtime;
        return this;
    }

    @Override
    public int compareTo(CMFileSyncClientEntry o) {
        return this.path.compareTo(o.getPath());
    }

    @Override
    public String toString() {
        return "CMFileSyncClientEntry{" +
                "path=" + path +
                ", size=" + size +
                ", curMtime=" + curMtime +
                ", baseMtime=" + baseMtime +
                ", opHint=" + opHint +
                ", isCompleted=" + isCompleted +
                ", serverMtime=" + serverMtime +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(obj == this) return true;
        if(!(obj instanceof CMFileSyncClientEntry entry)) return false;
        return Objects.equals(entry.getPath(), path) &&
                entry.getSize() == size &&
                entry.getCurMtime() == curMtime &&
                entry.getBaseMtime() == baseMtime &&
                entry.getOpHint() == opHint &&
                entry.isCompleted() == isCompleted &&
                entry.getServerMtime() == serverMtime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, size, curMtime, baseMtime, opHint, isCompleted, serverMtime);
    }
}
