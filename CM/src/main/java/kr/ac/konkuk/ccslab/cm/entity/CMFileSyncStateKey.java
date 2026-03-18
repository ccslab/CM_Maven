package kr.ac.konkuk.ccslab.cm.entity;

import java.util.UUID;

public record CMFileSyncStateKey(String initiatorName, UUID initiatorDeviceUuid) {
    @Override
    public String toString() {
        return initiatorName + "/" + initiatorDeviceUuid;
    }
}
