# DOMAIN MODELS KNOWLEDGE BASE

**Updated:** 2026-06-28
**Scope:** Domain Models & Contracts

**Relevant Skills:** `kotlin-best-practices` (data classes, sealed classes)

## OVERVIEW
Core immutable data structures and sealed class hierarchies that define the MOCCA domain language. These models represent the source of truth for the application state and are used across the UI, Data, and Domain layers to ensure consistency and type safety.

## KEY TYPES

### Config.kt
- **`ServerConfig`**: Server profile — `id, name, host, port, username, password, isActive, useHttps: Boolean`. `baseUrl` is a computed property (`protocol = if (useHttps) "https" else "http"`, omits port 443 if standard). Computed properties: `displayType`, `hasCredentials`. No `connectionType`, `authType`, or `authToken`.
- **`ConnectionStatus`** (sealed class): `NotConfigured`, `Disconnected(reason: String? = null)` (data class, NOT object), `Connecting`, `WaitingForNetwork`, `Reconnecting(attempt, maxAttempts)`, `Connected(serverInfo: AppInfo, latencyMs)`, `Error(message)`.
- **`ConnectionQuality`** (enum): `EXCELLENT`, `GOOD`, `DEGRADED`, `POOR`, `OFFLINE`, `UNKNOWN`.
- **`Resource<T>`** (sealed class): `Success(data)`, `Loading(data?)`, `Error(message, data?, cause: Throwable?)`.

### VcsTypes.kt
- **`VcsInfo`**: `branch: String = ""` — NON-nullable with default. No `?:` elvis needed.

### GitTypes.kt
- **`GitStatusResponse`**: Constructor params include `branch, upstream, ahead, behind, staged, unstaged, untracked, conflicted, stashes, clean`. `hasChanges` and `totalChanges` are computed properties (not constructor params).

### Config.kt (continued)
- **`GlobalAppConfig`**, **`AppConfigUpdate`**, **`FeatureFlags`**: Global application configuration models.
- **`DiscoveredServer`**: Has `username`, `password`, `useHttps`, and `source: DiscoverySource` fields (not `authToken`). Bridge-first QR pairing is the active discovery mechanism; no `discovery/` package exists.

### AiRuntimeConfig.kt
- **`AiRuntimeConfigSnapshot`**: Provider/model/agent/mode runtime projection from the CLI bridge or legacy HTTP fallback.
- **`AiSelection`**: Persisted per-project explicit selection. The fingerprint must match the active snapshot before reuse.
- **`AiRecentModel`**: Project-scoped model picker history. Use `selectAiRecentModelsForSnapshot(...)` for ordering, de-duplication, project filtering, and the current picker limit.

### Other Key Types
- **`ServerEvents.kt`**: SSE event types (`ServerEvent` sealed hierarchy).
- **`Models.kt`**: Core message/session models (`Message`, `Session`, `MessagePart`).
- **`BroadcastEvent.kt`**: Event fanout types for `StateCoordinator` → stores.
- **`AgentTypes.kt`**, **`CommandTypes.kt`**, **`ProviderTypes.kt`**, **`ToolTypes.kt`**: Typed enums and sealed classes for respective domains.
- **`McpModels.kt`**, **`DiffTypes.kt`**, **`SearchTypes.kt`**, **`SyncState.kt`**, **`SystemInfo.kt`**, **`UpdateModels.kt`**, **`AttachedFile.kt`**, **`TerminalGrid.kt`**, **`VoiceInput.kt`**, **`UserPreferences.kt`**, **`AiSelectionPresentation.kt`**, **`MessageExtensions.kt`**: Specialized domain types.

### ChatTurnReducer.kt
- **`ChatTurnState`**: Canonical OpenCode turn projection keyed by session, message, and part id.
- **`AgentActivity`**: Compact activity state for queued/running/reasoning/tool/writing/idle/error UI and notifications.
- **`ChatTurnReducer.reduce(...)`**: Single ingestion point for OpenCode `message.updated`, `message.part.updated`, `message.part.delta`, session status, permissions, and questions.
- **Part naming**: `reasoning` is canonical. `thinking` is retained as a legacy alias only.

## CONVENTIONS
- **Strict Immutability**: All domain models MUST be defined as `data class` using `val` properties. Mutable `var` properties are strictly prohibited to ensure thread safety and predictable state management within the MVI architecture.
- **Sealed Hierarchies for State**: Use `sealed class` or `sealed interface` to represent mutually exclusive states and resources:
    - **UI State**: For representing complex screen states.
    - **Resource<T>**: Using `Success`, `Loading`, and `Error` patterns for data fetching (see `Config.kt`).
    - **Polymorphic Payloads**: Handling SSE events (`ServerEvent`) and Message parts (`MessagePart`) exhaustively.
- **Zero Side Effects**: Domain models must remain pure data containers. Business logic, complex state transitions, and side effects are forbidden. Simple property mappers, non-calculating convenience getters, and deterministic list selectors such as AI recent-model normalization are permitted.
- **Reducers Are Pure**: Event reducers may transform immutable state but must not call repositories, clocks, databases, network clients, or notification managers.
- **Serialization Patterns**: All models intended for network transport or persistence must be annotated with `@Serializable`. Use `@SerialName` to decouple Kotlin property names from API keys.
- **Defensive Nullability**: Always treat API fields as nullable (`?`) unless the protocol specification explicitly guarantees their presence, preventing runtime crashes from unexpected server responses.
- **ConnectionStatus.Disconnected**: This is a `data class`, NOT an `object`. Always construct with `Disconnected()` or `Disconnected("reason")`, never bare `Disconnected`.
