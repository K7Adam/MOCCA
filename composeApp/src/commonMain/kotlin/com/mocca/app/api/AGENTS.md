# NETWORK LAYER KNOWLEDGE BASE

**Generated:** 2026-01-14
**Scope:** `composeApp/src/commonMain/kotlin/com/mocca/app/api`

## OVERVIEW
Ktor-based networking layer providing robust REST API access and resilient Server-Sent Events (SSE) streaming for real-time agent control.

## STRUCTURE
- **MoccaApiClient.kt**: Primary REST client handling sessions, files, git, terminal, and MCP operations.
- **MoccaSseClient.kt**: Manages SSE connection (`/event`) with automatic reconnection and event parsing.
- **RetryPolicy.kt**: Implements exponential backoff strategy with jitter for network resilience.
- **Platform.kt**: Platform-specific networking utilities (if applicable).

## WHERE TO LOOK
| Feature | Location | Key Function |
|---------|----------|--------------|
| REST Calls | `MoccaApiClient.kt` | `safeCall` / `safeCallNoRetry` |
| SSE Logic | `MoccaSseClient.kt` | `subscribeToEvents()` |
| Timeout Config | `MoccaApiClient.kt` | `createHttpClient` (install `HttpTimeout`) |
| Retry Logic | `RetryPolicy.kt` | `withRetry()` |
| Error Mapping | `RetryPolicy.kt` | `NetworkError.from()` |

## CONVENTIONS
- **120s Timeout**: Configured for request, connection, and socket to handle long-running LLM tasks.
- **Dynamic Base URL**: `serverConfigProvider()` is called per request to support runtime server switching.
- **Retry Strategy**:
    - GET requests use `safeCall` (3 retries via `RetryPolicy.Default`).
    - POST/DELETE mutations use `safeCallNoRetry` to prevent duplicate side effects.
- **SSE Resilience**: Automatic reconnection with exponential backoff (1s → 30s) and connection state synthetic events.
- **JSON Config**: `ignoreUnknownKeys = true` and `isLenient = true` enabled globally for API compatibility.

## ANTI-PATTERNS
- **Blocking I/O**: NEVER use blocking calls; all network operations must be `suspend`.
- **Hardcoded URLs**: NEVER hardcode IPs/domains; strictly use `serverConfigProvider`.
- **Swallowed Errors**: DO NOT ignore `Result.failure`; propagate `NetworkError` to UI/Domain layers.
- **Raw JSON**: Avoid manual parsing; let Ktor ContentNegotiation handle serialization where possible.
