# NETWORK LAYER KNOWLEDGE BASE (api)

**Updated:** 2026-05-05
**Scope:** `com.mocca.app.api`

**Relevant Skills:** `kotlin-best-practices` (Ktor, coroutines)

## OVERVIEW
Ktor-based networking layer providing REST and SSE fallback connectivity to a single OpenCode server instance. The MOCCA CLI bridge is the preferred runtime/config path; direct HTTP/SSE remains for compatibility and verification. Authentication uses HTTP Basic Auth. The `ApiExecutor` interface ensures consumers never hold `HttpClient` references directly.

## STRUCTURE
- **ApiExecutor.kt**: Interface with `suspend fun <T> execute(block: suspend HttpClient.() -> T): T`. Consumers call `execute {}` to make HTTP requests — the `ConnectionManager` provides the properly-configured `HttpClient`.
- **MoccaApiClient.kt**: OpenCode REST fallback client for sessions, messages, files, VCS, shell commands, permissions, and questions. Long-running sends use `/session/:id/prompt_async`.
- **MoccaSseClient.kt**: Resilient SSE handler for OpenCode events, including `message.part.delta`.
- **NetworkConfig.kt**: Timeout constants and network configuration.
- **RetryPolicy.kt**: Exponential backoff retry logic for transient failures.
- **GitHubApiClient.kt**: Client for GitHub Releases API (app auto-update feature).
- **Platform.kt**: Platform detection utilities.

## CONVENTIONS
- **ApiExecutor Pattern**: All API consumers depend on `ApiExecutor`, never on `HttpClient` directly. The `ConnectionManager` implements `ApiExecutor` and manages the `HttpClient` lifecycle, auth headers, and reconnection.
- **Timeouts**: Standard timeout is **120s** (`requestTimeout`, `socketTimeout`) to accommodate long-running LLM tasks.
- **Authentication**: HTTP Basic Auth with credentials from `ServerConfig` (`username`/`password`). Configured by `ConnectionManager` on the `HttpClient`.
- **Resilience Strategy**:
    - **Read Operations (GET)**: Use `safeCall` with exponential backoff retry.
    - **Write Operations (POST/DELETE)**: Use `safeCallNoRetry` to prevent duplicate side effects on network instability.
- **JSON Configuration**: `ignoreUnknownKeys = true` and `isLenient = true` are mandatory for forward compatibility with OpenCode server updates.
- **OpenCode Parts**: Treat `reasoning` as canonical. Keep `thinking` only as a legacy alias when parsing older cached/imported payloads.
- **Git Operations**: All Git operations go through `MoccaApiClient` using OpenCode's `/vcs` endpoint (read) and `executeShell()` (write). No separate Git server or port is involved.

## ANTI-PATTERNS
- Do not add new callers to `@Deprecated(ERROR)` methods in `MoccaApiClient.kt` (`getAppInfo` at line 46, `getTools` at line 596). These legacy endpoints return HTML and will be removed.
- Do not bypass `ApiExecutor.execute {}` for direct `HttpClient` ownership. Exception: `GitHubApiClient` for external GitHub API calls.
