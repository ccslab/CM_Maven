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

    // 같은 JVM 내 동시 생성을 직렬화하기 위한 락.
    // 현재 호출부(CMClientStub.startCM)는 단일 스레드 1회 호출이라 실제 경쟁은 없으나,
    // public static 유틸의 thread-safety 보장을 위한 대비용.
    private static final Object CREATE_LOCK = new Object();

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

            // 1) 파일이 존재하면 읽기 시도 (락 밖 fast path)
            UUID existing = tryReadUuid(file);
            if (existing != null) return existing;

            // 2) 같은 JVM 내 동시 생성을 직렬화 (멀티스레드 경쟁 시 1개만 생성)
            synchronized (CREATE_LOCK) {
                // 락 획득 후 재확인 (다른 스레드가 이미 생성했을 수 있음)
                existing = tryReadUuid(file);
                if (existing != null) return existing;

                // 배타적 생성 (프로세스 간 경쟁에서도 딱 1개만 성공)
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
                    // 다른 프로세스가 먼저 생성한 경우 → 재-읽기 (내용 flush 지연 대비 짧게 재시도)
                    UUID after = readWithRetry(file);
                    if (after != null) return after;

                    // 끝내 읽히지 않으면(손상 등) 덮어쓰기
                    System.err.println("[CMDeviceUuidManager] device_uuid unreadable after race, overwriting: " + file);
                    Files.writeString(file, fresh + "\n", StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);
                    return fresh;
                }
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

    // device_uuid 파일을 짧게 재시도하며 읽는다 (다른 프로세스의 동시 생성 직후 내용 flush 지연 대비).
    private static UUID readWithRetry(Path file) {
        for (int i = 0; i < 50; i++) {
            UUID u = tryReadUuid(file);
            if (u != null) return u;
            try {
                Thread.sleep(2);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
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
