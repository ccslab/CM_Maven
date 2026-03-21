package kr.ac.konkuk.ccslab.cm.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Map;

public class CMFileSyncJacksonSnapshotStore implements CMFileSyncIndexSnapshotStore {

    private final ObjectMapper mapper;

    public CMFileSyncJacksonSnapshotStore() {
        mapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public CMFileSyncIndexSnapshot loadOrEmpty(Path dir) throws IOException {
        Path file = dir.resolve("index.json");
        if (!Files.exists(file)) {
            return new CMFileSyncIndexSnapshot(0L, Map.of(), nowString());
        }
        return mapper.readValue(file.toFile(), CMFileSyncIndexSnapshot.class);
    }

    @Override
    public void saveAtomically(Path dir, CMFileSyncIndexSnapshot snap) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path tmp = dir.resolve("index.json.tmp");
        Path target = dir.resolve("index.json");

        mapper.writeValue(tmp.toFile(), snap);

        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.READ)) {
            channel.force(true);
        }

        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        try (FileChannel dirChannel = FileChannel.open(dir, StandardOpenOption.READ)) {
            dirChannel.force(true);
        }
    }

    private String nowString() {
        return java.time.OffsetDateTime.now().toString();
    }
}
