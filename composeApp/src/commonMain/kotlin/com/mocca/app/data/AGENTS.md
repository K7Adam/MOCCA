# DATA LAYER KNOWLEDGE BASE

## OVERVIEW
Clean data layer separating SQLDelight persistence from business logic via offline-first repositories.

## STRUCTURE
- **local/**: Platform-agnostic persistence abstraction.
    - `LocalCache`: Interface defining CRUD for Sessions, Messages, ServerConfigs, and Git status cache.
    - `LocalCacheFactory`: `expect` class for platform-specific cache instantiation.
- **repository/**: Domain-specific data orchestrators.
    - `SessionRepository`: Chat history, session lifecycle, permissions, questions, fork/revert.
    - `ServerConfigRepository`: Multiple server profiles and active target.
    - `EventStreamRepository`: SSE connection and event routing.
    - `FileRepository`: Remote file system access (ls, cat, search).
    - `GitRepository`: Full Git operations (status, branch, commit, push/pull).
    - `TerminalRepository`: WebSocket-based terminal sessions.
    - `AppConnectionManager`: Orchestrates server connection lifecycle.

## WHERE TO LOOK
- **SessionRepository**:
    - `getSessions()` / `getMessages()`: Primary offline-first entry points using `Flow<Resource<T>>`.
    - `sendMessageAsync()`: Non-blocking chat via `/prompt_async` + SSE.
    - `forkFromMessage()` / `revertToMessage()` / `unrevertSession()`: Session branching and undo.
    - `replyToPermission()` / `replyToQuestion()`: Handle interactive prompts.
    - `loadDefaultConfig()`: Syncs model/provider IDs from `/config/providers`.
- **ServerConfigRepository**:
    - `getActiveServerConfig()`: Singleton source of truth for current API target.
    - Defaults to Tailscale template (`100.x.x.x:4096`) if no config exists.
- **EventStreamRepository**:
    - Single SSE connection to `/event` shared across all session observers.
    - Implements exponential backoff (1s to 30s) and network-state awareness.
    - Handles `PermissionAsked`, `QuestionAsked`, `MessagePartUpdated` events.
- **GitRepository**:
    - `getStatus()` / `getBranches()` / `getLog()` / `getDiff()`: Read operations.
    - `stage()` / `unstage()` / `discard()` / `commit()` / `push()` / `pull()`: Write operations.
    - All mutations auto-refresh Git status after completion.
- **FileRepository**:
    - Stateless execution - does NOT use `LocalCache` (file contents are not persisted).
    - Returns `Flow<Resource<T>>` for unified UI handling.
- **TerminalRepository**:
    - `createTerminal()` / `connectToTerminal()`: WebSocket-based terminal.

## CONVENTIONS
- **Offline-First Flow**:
    1. `emit(Resource.Loading())` - Start progress indicator.
    2. `emit(Resource.Loading(cached))` - Show stale data immediately if available.
    3. `apiClient.call()` - Fetch fresh data.
    4. `localCache.insert()` - Update persistence on success.
    5. `emit(Resource.Success(fresh))` - Update UI with latest data.
- **Cache Dependency**: Repositories MUST only depend on `LocalCache` interface, never direct DB drivers.
- **Error Handling**: 
    - Always include `cached` data in `Resource.Error(message, cached)` to preserve UI state.
    - Use `Napier.e()` for data-layer logging to track sync failures.
- **API Call Safety**:
    - Use `safeCall` for idempotent queries (auto-retry enabled).
    - Use `safeCallNoRetry` for mutations (POST/DELETE) to prevent duplicate side effects.
- **Model Mapping**: Repositories are responsible for converting API responses to domain models.
