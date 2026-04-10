# DOMAIN MODELS KNOWLEDGE BASE

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

### ServerDiscovery.kt
- **`DiscoveredServer`**: Has `username`, `password`, `useHttps`, and `source: DiscoverySource` fields (not `authToken`).

### AppConfig.kt
- **`GlobalAppConfig`**, **`AppConfigUpdate`**, **`FeatureFlags`**: Global application configuration models.

## CONVENTIONS
- **Strict Immutability**: All domain models MUST be defined as `data class` using `val` properties. Mutable `var` properties are strictly prohibited to ensure thread safety and predictable state management within the MVI architecture.
- **Sealed Hierarchies for State**: Use `sealed class` or `sealed interface` to represent mutually exclusive states and resources:
    - **UI State**: For representing complex screen states.
    - **Resource<T>**: Using `Success`, `Loading`, and `Error` patterns for data fetching (see `Config.kt`).
    - **Polymorphic Payloads**: Handling SSE events (`ServerEvent`) and Message parts (`MessagePart`) exhaustively.
- **Zero Business Logic**: Domain models must remain pure data containers. Business logic, complex state transitions, and side effects are forbidden. Simple property mappers or non-calculating convenience getters (e.g., `val isConnected: Boolean get() = this is Connected`) are permitted.
- **Serialization Patterns**: All models intended for network transport or persistence must be annotated with `@Serializable`. Use `@SerialName` to decouple Kotlin property names from API keys.
- **Defensive Nullability**: Always treat API fields as nullable (`?`) unless the protocol specification explicitly guarantees their presence, preventing runtime crashes from unexpected server responses.
- **ConnectionStatus.Disconnected**: This is a `data class`, NOT an `object`. Always construct with `Disconnected()` or `Disconnected("reason")`, never bare `Disconnected`.
