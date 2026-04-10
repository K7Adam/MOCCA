# DATA REPOSITORY LAYER

**Scope:** Server-First Repositories, ConnectionManager

**Relevant Skills:** `kotlin-best-practices` (repository patterns, Flow)

## OVERVIEW
The Repository layer implements a **server-first** architecture, mediating between the `MoccaApiClient` (Network) and `LocalCache` (SQLDelight Persistence). Its primary goal is to provide immediate UI updates using cached data while fetching fresh data from the server in the background.

## KEY REPOSITORIES

### ConnectionManager
Unified connection lifecycle manager. Replaces the old `HttpClientProvider` and `AppConnectionManager`. Implements `ApiExecutor` so consumers never hold `HttpClient` references.

- **Owns**: `HttpClient` lifecycle, HTTP Basic Auth configuration, health checks, reconnection logic
- **Exposes**: `connectionStatus: StateFlow<ConnectionStatus>` for UI observation
- **Pattern**: Consumers depend on `ApiExecutor` interface, call `execute {}` to make HTTP requests

### ServerConfigRepository
Manages server profiles (CRUD). Each profile stores `host`, `port`, `username`, `password`.

### GitRepository
All Git operations go through OpenCode's built-in endpoints:
- **Read operations**: `/vcs` endpoint for status, branch info
- **Write operations**: `executeShell()` for git commands (commit, push, pull, etc.)
- **Diff**: `/session/:id/diff` for session diffs
- Write operations require a `sessionId: String` parameter

### StateCoordinator
Central coordinator for all application state.
- **Single Source of Truth**: Tracks active session ID and running sessions.
- **Event Flow**: SSE Events -> EventStreamRepository -> StateCoordinator -> State Stores -> UI

### ChatStateStore
Specialized state store for chat screen state.
- Provides reactive chat state, pending permissions, and thinking state tracking.

### AppStateStore
Global application state store.
- Tracks global configuration and connection state.

## PATTERNS

### 1. Server-First Flow<Resource<T>>
Most data fetching operations return a `Flow<Resource<T>>`. The implementation follows a strict emission order to ensure low latency and data consistency.

**Required Execution Order:**
1.  **Emit Cache**: Check `LocalCache` first. If data is present, emit `Resource.Loading(cachedData)` immediately. This allows the UI to show partial/stale data while refreshing.
2.  **Fetch Network**: Perform the network request via `MoccaApiClient`.
3.  **Update Cache**: Upon a successful network response, update the `LocalCache` with the fresh data.
4.  **Emit New Cache**: Emit `Resource.Success(freshData)`. If the network fails, emit `Resource.Error(message, cachedData)` to keep the UI functional with existing data.

**Standard Template:**
```kotlin
fun getItems(): Flow<Resource<List<Item>>> = flow {
    emit(Resource.Loading()) // Initial loading state
    
    // 1. Emit Cache
    val cached = localCache.getAllItems()
    if (cached.isNotEmpty()) {
        emit(Resource.Loading(cached))
    }

    // 2. Fetch Network
    apiClient.getItems().fold(
        onSuccess = { items ->
            // 3. Update Cache
            items.forEach { localCache.insertItem(it) }
            // 4. Emit New Cache
            emit(Resource.Success(items))
        },
        onFailure = { error ->
            emit(Resource.Error(error.message ?: "Network error", cached))
        }
    )
}.flowOn(Dispatchers.IO) // ALWAYS use IO dispatcher
```

### 2. ApiExecutor Pattern
Repositories that need HTTP access depend on `ApiExecutor` (interface), NOT on `HttpClient` directly. `ConnectionManager` implements `ApiExecutor` and manages the underlying `HttpClient` with proper auth headers and lifecycle.

```kotlin
class MyRepository(private val apiExecutor: ApiExecutor) {
    suspend fun fetchData() = apiExecutor.execute {
        get("http://server/endpoint").body<MyResponse>()
    }
}
```

### 3. Exceptions: Suspend Resource
Some specific lookups return a `suspend Resource<T>` instead of a Flow. These still follow the server-first logic but execute sequentially without multiple emissions.

*   **SessionRepository.getSession(sessionId: String)**: Returns `Resource<Session>`. It attempts to fetch from the network and updates the cache if found, falling back to the local cache if the network request fails or the item isn't in the network response.

### 4. Threading Guidelines
**NEVER block the main thread.**
- Repositories must be thread-safe and offload heavy I/O to `Dispatchers.IO`.
- Use `.flowOn(Dispatchers.IO)` for all `Flow` builders.
- Use `withContext(Dispatchers.IO)` for all `suspend` functions that interact with the network or database.

## ANTI-PATTERNS
- **Blocking Calls**: Using `runBlocking` or calling network/DB methods directly on the main thread.
- **Cache-Last**: Fetching from network before checking the local cache.
- **Stale Cache**: Forgetting to update `LocalCache` after a successful network fetch.
- **Direct API Leakage**: UI calling `MoccaApiClient` directly, bypassing the repository's caching logic.
- **In-Memory Only**: Storing critical business state in repository variables instead of persisting to `LocalCache`.
- **Holding HttpClient**: Consumers must NEVER hold an `HttpClient` reference. Always use `ApiExecutor.execute {}`.
