package kr.ac.konkuk.ccslab.cm.thread;

import kr.ac.konkuk.ccslab.cm.entity.CMFileSyncClientEntry;

import java.util.List;
import java.util.UUID;

// TODO: 설계 10-2 (라인 1683~) 구현 예정 — pull sync용 block-checksum generator
public class CMFileSyncPullGenerator implements Runnable {
    private final String initiatorName;
    private final UUID initiatorUuid;
    private final UUID initiatorDeviceUuid;
    private final String serverName;
    private final List<CMFileSyncClientEntry> pullModifyEntryList;

    public CMFileSyncPullGenerator(String initiatorName, UUID initiatorUuid,
                                   UUID initiatorDeviceUuid, String serverName,
                                   List<CMFileSyncClientEntry> pullModifyEntryList) {
        this.initiatorName = initiatorName;
        this.initiatorUuid = initiatorUuid;
        this.initiatorDeviceUuid = initiatorDeviceUuid;
        this.serverName = serverName;
        this.pullModifyEntryList = pullModifyEntryList;
    }

    @Override
    public void run() {
        // TODO: block-checksum 생성 후 START/FILE_BLOCK/END_FILE_BLOCK_CHECKSUM 이벤트 전송
        System.out.println("=== CMFileSyncPullGenerator.run() called.. (not yet implemented)");
    }

    public String getInitiatorName() { return initiatorName; }
    public UUID getInitiatorUuid() { return initiatorUuid; }
    public UUID getInitiatorDeviceUuid() { return initiatorDeviceUuid; }
    public String getServerName() { return serverName; }
    public List<CMFileSyncClientEntry> getPullModifyEntryList() { return pullModifyEntryList; }
}
