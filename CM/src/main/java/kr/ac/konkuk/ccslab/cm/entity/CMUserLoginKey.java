package kr.ac.konkuk.ccslab.cm.entity;

import java.util.Objects;
import java.util.UUID;

/**
 * 다중 디바이스 로그인을 지원하기 위한 복합 키 클래스
 * 사용자 이름(userName)과 로그인 식별자(uuid)를 조합하여 유니크한 키를 구성함.
 * SNS 첨부 파일 관리 및 파일 동기화 세션 관리 등에서 Map의 Key로 사용됨.
 */
public class CMUserLoginKey {
    private final String userName;
    private final UUID uuid;

    public CMUserLoginKey(String userName, UUID uuid) {
        this.userName = userName;
        this.uuid = uuid;
    }

    public String getUserName() {
        return userName;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CMUserLoginKey that = (CMUserLoginKey) o;
        return Objects.equals(userName, that.userName) &&
                Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, uuid);
    }

    @Override
    public String toString() {
        return "CMUserLoginKey{" +
                "userName='" + userName + '\'' +
                ", uuid=" + uuid +
                '}';
    }
}