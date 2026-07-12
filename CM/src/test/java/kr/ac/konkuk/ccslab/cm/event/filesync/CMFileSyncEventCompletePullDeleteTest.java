package kr.ac.konkuk.ccslab.cm.event.filesync;

import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class CMFileSyncEventCompletePullDeleteTest {
    @Test
    public void marshallUnmarshall() {
        System.out.println("===== CMFileSyncEventCompletePullDeleteTest.marshallUnmarshall() called..");
        CMFileSyncEventCompletePullDelete fsEvent = new CMFileSyncEventCompletePullDelete();
        fsEvent.setInitiatorName("ccslab");
        fsEvent.setInitiatorUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        fsEvent.setInitiatorDeviceUuid(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        System.out.println("fsEvent = " + fsEvent);

        // null list -> null after roundtrip
        ByteBuffer byteBuffer = CMEventManager.marshallEvent(fsEvent);
        CMFileSyncEventCompletePullDelete unmarshallEvent =
                (CMFileSyncEventCompletePullDelete) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        assertNull(unmarshallEvent.getDeletedPathList());

        // with a list of 2 paths
        List<String> deletedPathList = new ArrayList<>();
        deletedPathList.add("dir/file1.txt");
        deletedPathList.add("dir/sub/file2.txt");
        fsEvent.setDeletedPathList(deletedPathList);
        System.out.println("fsEvent = " + fsEvent);

        byteBuffer = CMEventManager.marshallEvent(fsEvent);
        unmarshallEvent = (CMFileSyncEventCompletePullDelete) CMEventManager.unmarshallEvent(byteBuffer);
        assertNotNull(unmarshallEvent);
        System.out.println("unmarshallEvent = " + unmarshallEvent);

        assertEquals("ccslab", unmarshallEvent.getInitiatorName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), unmarshallEvent.getInitiatorUuid());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), unmarshallEvent.getInitiatorDeviceUuid());
        assertEquals(deletedPathList, unmarshallEvent.getDeletedPathList());

        assertEquals(fsEvent, unmarshallEvent);
    }
}
