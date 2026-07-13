package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.info.CMFileSyncInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncGeneratorTest {

    private CMInfo cmInfo;
    private String initiatorName;
    private UUID initiatorUuid;
    private UUID initiatorDeviceUuid;

    @Before
    public void setUp() {
        System.out.println("===== called setUp()..");
        cmInfo = CMInfo.getInstance();
        initiatorName = "ccslab";
        initiatorUuid = UUID.randomUUID();
        initiatorDeviceUuid = UUID.randomUUID();
    }

    @Test
    public void calculateBlockSize() {
        System.out.println("===== called calculateBlockSize()..");
        CMFileSyncGenerator syncGenerator = new CMFileSyncGenerator(initiatorName, initiatorUuid, initiatorDeviceUuid);
        try {
            Method method = syncGenerator.getClass().getDeclaredMethod("calculateBlockSize", long.class);
            method.setAccessible(true);

            int blockSize;
            // fileSize < CMFileSyncInfo.BLOCK_SIZE * CMFileSyncInfo.BLOCK_SIZE
            blockSize = (int) method.invoke(syncGenerator, 400000);
            assertEquals(CMFileSyncInfo.BLOCK_SIZE, blockSize);

            // fileSize > CMFileSyncInfo.BLOCK_SIZE * CMFileSyncInfo.BLOCK_SIZE
            blockSize = (int) method.invoke(syncGenerator, 500000);
            assertNotEquals(CMFileSyncInfo.BLOCK_SIZE, blockSize);  // expected value: 704 ?
            blockSize = (int) method.invoke(syncGenerator, 1000000);
            assertEquals(1000, blockSize);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}