package kr.ac.konkuk.ccslab.cm.entity;

import java.util.Map;

public record CMFileSyncIndexSnapshot(
        long lastChangeId,
        Map<String, CMFileSyncIndexEntry> entries,
        String generatedAt
) {
}
