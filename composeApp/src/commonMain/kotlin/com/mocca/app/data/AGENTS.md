# DATA LAYER KNOWLEDGE BASE

## OVERVIEW
Clean data layer separating SQLDelight persistence from business logic via offline-first repositories.

- **Purpose**: Provides a robust, offline-first data management system for MOCCA.
- **Architecture**: Separates persistence (SQLDelight) from domain logic using the Repository pattern.
- **Offline-First**: Uses `Flow<Resource<T>>` to deliver cached data instantly while fetching updates from the server.
- **Encryption**: **NONE**. Data is internal and unencrypted.

## DB SCHEMA
Schema definitions are located in `composeApp/src/commonMain/sqldelight/com/mocca/app/db/`.

| File | Description |
|------|-------------|
| `Agent.sq` | Caches agent configurations (prompts, modes, model settings). |
| `Command.sq` | Caches available slash commands and terminal tools. |
| `FileInfo.sq` | Caches file browser hierarchy to reduce network overhead. |
| `Message.sq` | Stores chat history, message parts, and execution status. |
| `RecentModel.sq` | Tracks recently used LLM models and providers. |
| `ServerConfig.sq` | Manages server profiles. Schema: `id, name, host, port, username, password, isActive`. |
| `Session.sq` | Manages conversation sessions, metadata, and sync status. |

## CONVENTIONS
- **LocalCache Interface**: Primary abstraction for all persistence. Repositories MUST depend on `LocalCache`, never direct DB drivers.
- **ServerConfig Schema**: Fields are `id`, `name`, `host`, `port`, `username`, `password`, `isActive`. The `baseUrl` is a computed property (`http://$host:$port`) on the domain model, not stored in the DB. There is no `connectionType`, `authType`, or `authToken` field.
- **Offline-First Flow**:
    1. Emit `Resource.Loading(cached)` immediately.
    2. Fetch from API via `safeCall`.
    3. Update `LocalCache` on success.
    4. Emit `Resource.Success(fresh)`.
- **Git Cache**: `GitStatusResponse` is stored as an in-memory cache in `LocalCache` (not persisted to DB).
- **Error Handling**: Always return `Resource.Error(message, cached)` to prevent UI flickering during transient network failures.
