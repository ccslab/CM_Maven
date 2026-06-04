# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CM (Communication Middleware) is a Java communication framework for distributed systems, developed at CCSLab, Konkuk University. It supports client-server (`CM_CS`) and peer-server (`CM_PS`) architectures with features including file transfer, file synchronization (rsync-like delta encoding), MQTT protocol, SNS, and multi-device concurrent login.

- **Group ID**: `kr.ac.konkuk.ccslab`, **Artifact**: `CM`, **Version**: `3.0.0-SNAPSHOT`
- **Java**: JDK 18
- **Test Framework**: JUnit 4
- **Dependencies**: commons-math3, mysql-connector-java, jaxb-api

## Build Commands

```bash
mvn clean compile                          # Compile
mvn clean test                             # Run all tests
mvn test -Dtest=CMFileSyncEntryTest        # Run single test class
mvn test -Dtest=CMFileSyncEntryTest#testEquals  # Run single test method
mvn clean package                          # Build JARs (includes uber-JARs for CMWinServer/CMWinClient)
```

## Architecture

### Layered Design

Applications interact with CM through **Stub** classes, which delegate to **Manager** classes for service logic. Managers read/write global state via **Info** singletons. Communication is event-driven: **Event** objects are serialized to `ByteBuffer` and dispatched through event queues.

```
Application Layer:  CMServerStub / CMClientStub  (public API)
        ↓
Service Layer:      Managers (CMCommManager, CMSessionManager, CMEventManager,
                    CMFileTransferManager, CMFileSyncManager, CMMqttManager, ...)
        ↓
State Layer:        Info singletons (CMInfo, CMCommInfo, CMInteractionInfo,
                    CMConfigurationInfo, CMThreadInfo, CMFileTransferInfo, ...)
        ↓
Entity Layer:       CMUser, CMServer, CMSession, CMGroup, CMMember, CMChannelInfo, ...
        ↓
Event Layer:        CMEvent → CMSessionEvent, CMFileEvent, CMUserEvent,
                    CMSNSEvent, CMMqttEvent, CMFileSyncEvent, ...
```

### Key Classes

- **`CMStub`** (abstract) → **`CMServerStub`** / **`CMClientStub`**: Entry points for applications. Handles init, start/stop, send/receive events.
- **`CMInfo`**: Central singleton holding constants, service manager registry, and event handler registry. Access via `CMInfo.getInstance()`.
- **`CMEvent`**: Abstract base for all events. Subclasses implement `marshall()` / `unmarshall()` for `ByteBuffer` serialization. Events carry sender/receiver, session/group info.
- **`CMUserEvent`**: Flexible user-defined events with typed fields (int, long, float, double, string, bytes).
- **`CMUser`**: Represents a connected client. Supports UUID for multi-device login identification.
- **`CMMember`**: Collection of users. In multi-login mode, maps `String → List<CMUser>` (one name, multiple device connections).
- **Event Handlers** (`CMAppEventHandler` interface): Applications implement this to handle incoming events.

### Communication

- NIO-based non-blocking I/O: `SocketChannel` (TCP), `DatagramChannel` (UDP), `MulticastChannel`
- Selector-based event loop with separate send/receive event queues
- File transfer supports push/pull with append or overwrite modes

### Configuration

- `cm-server.conf` / `cm-client.conf`: Runtime configuration (system type, ports, login scheme, file paths, DB settings, etc.)
- `cm-session*.conf`: Session/group definitions
- Key parameters: `SYS_TYPE`, `COMM_ARCH`, `LOGIN_SCHEME`, `MULTI_LOGIN_SCHEME`, `FILE_SYNC_MODE`, `FILE_UPDATE_MODE`

## Code Conventions

- **Package structure**: `kr.ac.konkuk.ccslab.cm.{stub,entity,event,manager,info,thread,util,sns}`
- **Member variable prefix**: `m_` with Hungarian notation — `m_strName` (String), `m_nPort` (int), `m_bConnected` (boolean), `m_lTimestamp` (long)
- **Method naming**: `get`/`set` for accessors, `find` for lookups, `process` for event handlers, `marshall`/`unmarshall` for serialization
- **Thread safety**: `synchronized` methods on shared data structures
- **Error handling**: Returns `boolean` or `null` for failure; exceptions logged via `e.printStackTrace()`

## Active Development

The `feature/concurrent-login/*` branches add multi-device login support using UUIDs to identify individual device connections per user. `CMMember` was refactored to use `Hashtable<String, List<CMUser>>` for this.

### File Sync: implementation divergences from design doc 10-2

The bidirectional file sync (`docs/10-2_...` design doc; `docs/` is gitignored / local-only) is being implemented incrementally. Pull sync is done; push sync is next. During pull debugging, **client-side sync metadata was added that the 10-2 doc does not describe** — account for these when implementing push (push completion must update the base snapshot with mtime **and** size, consistent with the items below):

- **`m_lastSyncedSizeMap`** (`CMFileSyncInfo`): the doc's client base snapshot had only `lastSyncedMtimeMap` (relPath→mtime); implementation adds a size map (relPath→bytes) plus the combined setter `setLastSynced(relPath, mtime, size)`. Strengthens the WatchService self-event filter from mtime-only to mtime+size.
- **`CMFileSyncClientEntry.serverMtime`** field: in the doc `serverMtime` is only a local var in compare logic; implementation adds it as a PULL-only field, excluded from marshall/unmarshall (not transmitted), used for client-local decisions.
- **`m_pendingPullDeletePaths`** (`CMFileSyncInfo`): not in the doc; guards against pull-deleted files bouncing back as phantom DELETE self-events in the WatchService.
- The base-snapshot persistence DTO gained a `fileSizes` field (companion to the size map; nullable for backward compat).

Note: the index/persistence layer (`CMFileSyncIndex*`, `CMFileSyncJacksonSnapshotStore`) belongs to a *separate* design doc, not these divergences. Also, pull CREATE was switched from the doc's `requestPermitForPullFile` (client pulls) approach to a server-push approach (`REQUEST_PULL_CREATES` event + server `pushFile` + client-side `Files.move`) — a protocol change, not a metadata change.
