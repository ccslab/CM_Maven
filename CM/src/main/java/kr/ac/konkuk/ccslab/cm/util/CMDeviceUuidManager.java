package kr.ac.konkuk.ccslab.cm.util;

import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

/**
 * 클라이언트 디바이스 UUID를 파일로 영속 관리하는 유틸리티 클래스.
 * <p>
 * 저장 경로: CM home/.cm-settings/file-sync/client/device_uuid
 * </p>
 * 인스턴스화 불가.
 */
public final class CMDeviceUuidManager {

    private static final String[] REL_DIR = {".cm-settings", "file-sync", "client"};
    private static final String FILE_NAME = "device_uuid";

    private CMDeviceUuidManager() {}

    /**
     * 편의 메소드: CM 설정 홈 기준으로 device UUID를 생성하거나 기존 값을 반환한다.
     * CMClientStub.startCM()에서 CM 홈 경로가 확정된 이후에 호출해야 한다.
     */
    public static UUID getOrCreateDeviceUuid() {
        Path home = CMConfigurationInfo.getInstance().getConfFileHome();
        Objects.requireNonNull(home, "CM 홈 경로를 얻지 못했습니다.");
        return getOrCreateDeviceUuid(home);
    }

    /**
     * 지정 baseDir 기준으로 device UUID를 생성하거나 기존 값을 반환한다.
     * (테스트 및 확장 용도)
     */
    public static UUID getOrCreateDeviceUuid(Path baseDir) {
        Objects.requireNonNull(baseDir, "baseDir");
        final Path file = resolve(baseDir);
        try {
            Files.createDirectories(file.getParent());

            // 1) 파일이 존재하면 읽기 시도
            UUID existing = tryReadUuid(file);
            if (existing != null) return existing;

            // 2) 없으면 배타적 생성 (동시 실행 시 딱 1개만 성공)
            final UUID fresh = UUID.randomUUID();
            try {
                Files.writeString(file, fresh + "\n", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                if (CMInfo._CM_DEBUG) {
                    System.out.println("[CMDeviceUuidManager] new device_uuid created: "
                            + fresh.toString().substring(0, 8) + "...");
                }
                return fresh;

            } catch (FileAlreadyExistsException raced) {
                // 동시에 다른 스레드가 먼저 생성한 경우 → 재-읽기
                UUID after = tryReadUuid(file);
                if (after != null) return after;

                // 여전히 읽히지 않으면(손상 등) 덮어쓰기
                System.err.println("[CMDeviceUuidManager] device_uuid unreadable after race, overwriting: " + file);
                Files.writeString(file, fresh + "\n", StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
                return fresh;
            }

        } catch (IOException ioe) {
            throw new UncheckedIOException("device_uuid 준비 실패: " + file, ioe);
        }
    }

    /**
     * device UUID 파일을 읽어 반환한다. 파일이 없거나 손상된 경우 null을 반환한다.
     */
    public static UUID readDeviceUuid(Path baseDir) {
        return tryReadUuid(resolve(baseDir));
    }

    // ===== private 유틸 =====

    private static Path resolve(Path baseDir) {
        Path dir = baseDir;
        for (String s : REL_DIR) dir = dir.resolve(s);
        return dir.resolve(FILE_NAME);
    }

    private static UUID tryReadUuid(Path file) {
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return null;
            String raw = Files.readString(file, StandardCharsets.UTF_8).trim();
            return CMUUIDConverter.stringToUuid(raw);
        } catch (Exception ignore) {
            return null;
        }
    }
}
