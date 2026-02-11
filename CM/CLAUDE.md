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
