package kr.ac.konkuk.ccslab.cm.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class CMDeviceUuidManagerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path baseDir;
    // UUID 파일의 예상 경로: baseDir/.cm-settings/file-sync/client/device_uuid
    private Path uuidFile;

    @Before
    public void setUp() throws IOException {
        baseDir = tempFolder.newFolder("cm-home").toPath();
        uuidFile = baseDir
                .resolve(".cm-settings")
                .resolve("file-sync")
                .resolve("client")
                .resolve("device_uuid");
    }

    // ===== getOrCreateDeviceUuid =====

    @Test
    public void getOrCreate_파일없을때_UUID생성() {
        assertFalse("사전 조건: 파일이 없어야 함", Files.exists(uuidFile));

        UUID result = CMDeviceUuidManager.getOrCreateDeviceUuid(baseDir);

        assertNotNull(result);
        assertTrue("파일이 생성되어야 함", Files.isRegularFile(uuidFile));
    }

    @Test
    public void getOrCreate_두번_호출시_같은UUID반환() {
        UUID first  = CMDeviceUuidManager.getOrCreateDeviceUuid(baseDir);
        UUID second = CMDeviceUuidManager.getOrCreateDeviceUuid(baseDir);

        assertEquals("두 번 호출해도 동일한 UUID를 반환해야 함", first, second);
    }

    @Test
    public void getOrCreate_중간디렉토리_자동생성() {
        // 사전에 디렉토리가 존재하지 않더라도 정상 동작해야 함
        assertFalse(Files.exists(uuidFile.getParent()));

        UUID result = CMDeviceUuidManager.getOrCreateDeviceUuid(baseDir);

        assertNotNull(result);
        assertTrue("중간 디렉토리가 생성되어야 함", Files.isDirectory(uuidFile.getParent()));
    }

    @Test
    public void getOrCreate_기존파일이_유효한UUID_읽기() throws IOException {
        UUID expected = UUID.randomUUID();
        Files.createDirectories(uuidFile.getParent());
        Files.writeString(uuidFile, expected + "\n", StandardCharsets.UTF_8);

        UUID result = CMDeviceUuidManager.getOrCreateDeviceUuid(baseDir);

        assertEquals("기존 파일의 UUID를 반환해야 함", expected, result);
    }

    @Test
    public void getOrCreate_파일이_손상된경우_새UUID생성() throws IOException {
        Files.createDirectories(uuidFile.getParent());
        Files.writeString(uuidFile, "not-a-valid-uuid\n", StandardCharsets.UTF_8);

        UUID result = CMDeviceUuidManager.getOrCreateDeviceUuid(baseDir);

        assertNotNull("손상된 파일일 때도 UUID를 반환해야 함", result);
        // 덮어쓴 파일 내용이 유효한 UUID여야 함
        UUID stored = CMDeviceUuidManager.readDeviceUuid(baseDir);
        assertNotNull(stored);
        assertEquals(result, stored);
    }

    @Test(expected = NullPointerException.class)
    public void getOrCreate_baseDir_null이면_NPE() {
        CMDeviceUuidManager.getOrCreateDeviceUuid(null);
    }

    // ===== readDeviceUuid =====

    @Test
    public void read_파일없으면_null반환() {
        assertFalse(Files.exists(uuidFile));

        UUID result = CMDeviceUuidManager.readDeviceUuid(baseDir);

        assertNull(result);
    }

    @Test
    public void read_유효한UUID_파일이면_반환() throws IOException {
        UUID expected = UUID.randomUUID();
        Files.createDirectories(uuidFile.getParent());
        Files.writeString(uuidFile, expected + "\n", StandardCharsets.UTF_8);

        UUID result = CMDeviceUuidManager.readDeviceUuid(baseDir);

        assertEquals(expected, result);
    }

    @Test
    public void read_UUID앞뒤공백무시() throws IOException {
        UUID expected = UUID.randomUUID();
        Files.createDirectories(uuidFile.getParent());
        Files.writeString(uuidFile, "  " + expected + "  \n", StandardCharsets.UTF_8);

        UUID result = CMDeviceUuidManager.readDeviceUuid(baseDir);

        assertEquals(expected, result);
    }

    @Test
    public void read_손상된파일이면_null반환() throws IOException {
        Files.createDirectories(uuidFile.getParent());
        Files.writeString(uuidFile, "garbage-data", StandardCharsets.UTF_8);

        UUID result = CMDeviceUuidManager.readDeviceUuid(baseDir);

        assertNull(result);
    }

    @Test
    public void read_빈파일이면_null반환() throws IOException {
        Files.createDirectories(uuidFile.getParent());
        Files.createFile(uuidFile);

        UUID result = CMDeviceUuidManager.readDeviceUuid(baseDir);

        assertNull(result);
    }

    // ===== 동시성 =====

    @Test
    public void getOrCreate_멀티스레드_동시호출시_모두_같은UUID반환() throws InterruptedException {
        final int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Future<UUID>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                barrier.await(); // 모든 스레드 동시 시작
                return CMDeviceUuidManager.getOrCreateDeviceUuid(baseDir);
            }));
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Set<UUID> distinct = new java.util.HashSet<>();
        for (Future<UUID> f : futures) {
            try {
                UUID uuid = f.get();
                assertNotNull(uuid);
                distinct.add(uuid);
            } catch (ExecutionException e) {
                fail("스레드 실행 중 예외 발생: " + e.getCause());
            }
        }

        assertEquals("멀티스레드 환경에서 모두 동일한 UUID를 반환해야 함", 1, distinct.size());
    }
}
