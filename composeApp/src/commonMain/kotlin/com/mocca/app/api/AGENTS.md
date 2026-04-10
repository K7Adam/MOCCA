# NETWORK LAYER KNOWLEDGE BASE (api)

**Updated:** 2026-02-15
**Scope:** `com.mocca.app.api`

**Relevant Skills:** `kotlin-best-practices` (Ktor, coroutines)

## OVERVIEW
Ktor-based networking layer providing REST and SSE connectivity to a single OpenCode server instance. Authentication uses HTTP Basic Auth. The `ApiExecutor` interface ensures consumers never hold `HttpClient` references directly.

## STRUCTURE
- **ApiExecutor.kt**: Interface with `suspend fun <T> execute(block: suspend HttpClient.() -> T): T`. Consumers call `execute {}` to make HTTP requests — the `ConnectionManager` provides the properly-configured `HttpClient`.
- **MoccaApiClient.kt**: Primary client for all OpenCode REST endpoints (Sessions, Messages, Files, VCS, Shell commands). Uses `ApiExecutor` for all requests.
- **MoccaSseClient.kt**: Resilient SSE handler for real-time events and streaming.
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
- **Git Operations**: All Git operations go through `MoccaApiClient` using OpenCode's `/vcs` endpoint (read) and `executeShell()` (write). No separate Git server or port is involved.
