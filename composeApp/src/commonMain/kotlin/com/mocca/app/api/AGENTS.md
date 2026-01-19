# NETWORK LAYER KNOWLEDGE BASE (api)

**Generated:** 2026-01-18
**Scope:** `com.mocca.app.api`

## OVERVIEW
Ktor-based networking layer providing REST and SSE connectivity. MOCCA uses a dual-server architecture to separate agent logic from heavy file/git operations:
1. **OpenCode Server (Port 4096)**: Primary agent logic, sessions, and configuration.
2. **Git HTTP Server (Port 4097)**: Specialized high-performance server for Git operations, powered by `simple-git`.

## STRUCTURE
- **MoccaApiClient.kt**: Primary client for OpenCode REST endpoints (Sessions, Messages, Files).
- **GitApiClient.kt**: Specialized client for Git operations on port 4097.
- **MoccaSseClient.kt**: Resilient SSE handler for real-time events and streaming.
- **HttpClientProvider.kt**: Manages Ktor `HttpClient` lifecycle, auth tokens, and engine configuration.
- **GitServerChecker.kt**: Utility for fast server availability detection (500ms-1s).
- **ServerConfigRepository.kt**: Manages environment detection (Emulator `10.0.2.2` vs LAN/Tailscale).

## GIT PROTOCOL
MOCCA requires a secondary Git HTTP server for high-performance git operations to avoid blocking the primary agent loop.

- **Port Mapping**:
    - OpenCode (Agent): `4096`
    - Git Server (Target): `4097`
- **Auto-Start Logic**:
    - **Trigger**: `GitApiClient.requestStartGitServer()`
    - **Method**: `POST /command`
    - **Body**: `{"command": "start-git-server"}`
    - **Host Action**: OpenCode executes `start-git-server.ps1` upon receiving the command.
- **Connectivity**:
    - Local connections **MUST** allow cleartext (HTTP) as the local git server does not support TLS.
    - `GitApiClient` forces `http://` regardless of the primary server's scheme.
- **Availability Check**:
    - `ensureServerRunning()` performs a quick port check (500ms-1s) before any Git operation.
    - Returns `GitServerNotRunningException` immediately if port 4097 is unreachable.

## CONVENTIONS
- **Timeouts**: Standard timeout is **120s** (`requestTimeout`, `socketTimeout`) to accommodate long-running LLM tasks and heavy git operations.
- **Environment Detection**:
    - **Android Emulator**: Automatically detects and uses `10.0.2.2` to reach the host machine.
    - **Physical Device**: Uses LAN IP or Tailscale hostname.
- **Resilience Strategy**:
    - **Read Operations (GET)**: Use `safeCall` with exponential backoff retry.
    - **Write Operations (POST/DELETE)**: Use `safeCallNoRetry` to prevent duplicate side effects on network instability.
- **Dynamic URL Resolution**: `serverConfigProvider` is invoked per-request, enabling seamless runtime switching between servers without app restart.
- **JSON Configuration**: `ignoreUnknownKeys = true` and `isLenient = true` are mandatory for forward compatibility with OpenCode server updates.
