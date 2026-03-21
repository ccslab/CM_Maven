package kr.ac.konkuk.ccslab.cm.entity;

import java.io.IOException;
import java.nio.file.Path;

public interface CMFileSyncIndexSnapshotStore {
    CMFileSyncIndexSnapshot loadOrEmpty(Path dir) throws IOException;
    void saveAtomically(Path dir, CMFileSyncIndexSnapshot snap) throws IOException;
}
