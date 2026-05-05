# DATA LAYER KNOWLEDGE BASE

**Updated:** 2026-05-05
**Scope:** SQLDelight, LocalCache, Server-First

**Relevant Skills:** `kotlin-best-practices` (SQLDelight, repositories)

## OVERVIEW
Clean data layer separating SQLDelight persistence from business logic via server-first repositories.

- **Purpose**: Provides a robust, server-first data management system for MOCCA.
- **Architecture**: Separates persistence (SQLDelight) from domain logic using the Repository pattern.
- **Server-First**: Uses `Flow<Resource<T>>` to deliver cached data instantly while fetching updates from the server.
- **Encryption**: **NONE**. Data is internal and unencrypted.

## DB SCHEMA
Schema definitions are located in `composeApp/src/commonMain/sqldelight/com/mocca/app/db/`.

| File | Description |
|------|-------------|
| `Agent.sq` | Caches agent configurations (prompts, modes, model settings). |
| `AppSettings.sq` | Caches key-value application settings, including AI selection and project-scoped AI model recents. |
| `Command.sq` | Caches available slash commands and terminal tools. |
| `FileInfo.sq` | Caches file browser hierarchy to reduce network overhead. |
| `Message.sq` | Stores chat history, message parts, and execution status. |
| `ServerConfig.sq` | Manages server profiles. Schema: `id, name, host, port, username, password, isActive, useHttps`. |
| `Session.sq` | Manages conversation sessions, metadata, and sync status. |
| `SessionTodo.sq` | Caches session-specific todo lists. |

Migrations: `1.sqm` (initial), `2.sqm` (drops retired global `RecentModel`).

## CONVENTIONS
- **LocalCache Interface**: Primary abstraction for all persistence. Repositories MUST depend on `LocalCache`, never direct DB drivers.
- **ServerConfig Schema**: Fields are `id`, `name`, `host`, `port`, `username`, `password`, `isActive`, `useHttps`. The `baseUrl` is a computed property (`protocol = if (useHttps) "https" else "http"`, omits port 443 if standard) on the domain model, not stored in the DB. There is no `connectionType`, `authType`, or `authToken` field.
- **AI Runtime Persistence**: `AiSelection` and `AiRecentModel` are stored as JSON in `AppSettings` using `ai.selection.<projectKey>` and `ai.recents.<projectKey>`. The old global `RecentModel` table is retired and dropped by migration `2.sqm`.
- **Part-Addressable Chat Updates**: Streaming text, reasoning, and legacy thinking updates use `LocalCache.updateMessagePart(messageId, partId, partType, ...)`. Do not scan/update every text part when OpenCode provides `partID`.
- **Server-First Flow**:
    1. Emit `Resource.Loading(cached)` immediately.
    2. Fetch from API via `safeCall`.
    3. Update `LocalCache` on success.
    4. Emit `Resource.Success(fresh)`.
- **Git Cache**: `GitStatusResponse` is stored as an in-memory cache in `LocalCache` (not persisted to DB).
- **Error Handling**: Always return `Resource.Error(message, cached)` to prevent UI flickering during transient network failures.
- **ServerDiscovery**: `discovery/` package is reserved for cross-platform server discovery but currently empty; bridge-first QR pairing is the active mechanism.
