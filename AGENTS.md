# PROJECT KNOWLEDGE BASE

**Updated:** 2026-02-15
**Project:** MOCCA (Mobile OpenCode Companion App)
**Stack:** Kotlin Multiplatform (Android-only) + Compose Multiplatform + Koin + Voyager + SQLDelight

## OVERVIEW
Android client for OpenCode AI agent. Features edge-to-edge "Pitch Black" Modern UI, offline-first architecture, and a unified single-server connection to OpenCode with HTTP Basic Auth.

## STRUCTURE
```
MOCCA/
├── androidApp/           # [Platform] Entry (MainActivity), Manifest, Res
├── composeApp/           # [Shared] 90% of code. UI, Logic, Data.
│   └── src/commonMain/kotlin/com/mocca/app/
│       ├── api/          # Network (Ktor, SSE, ApiExecutor)
│       ├── data/         # Repositories (Offline-first) + SQLDelight
│       ├── domain/       # Contracts, Models (Immutable)
│       └── ui/           # Voyager Screens, TerminalTheme
├── maestro-workspace/    # [Testing] External E2E UI tests (YAML)
└── gradle/               # Version Catalog (libs.versions.toml)
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| **Entry Point** | `androidApp/.../MoccaApp.kt` | Koin init, Napier logging |
| **Root UI** | `composeApp/.../ui/App.kt` | `AppTheme`, Navigator |
| **Connection** | `composeApp/.../data/repository/ConnectionManager.kt` | Unified connection lifecycle, health checks, reconnection |
| **API Executor** | `composeApp/.../api/ApiExecutor.kt` | Interface so consumers never hold HttpClient references |
| **API Client** | `composeApp/.../api/MoccaApiClient.kt` | All OpenCode REST endpoints |
| **Git Operations** | `composeApp/.../data/repository/GitRepository.kt` | Uses OpenCode VCS endpoints + `executeShell()` |
| **DB Schema** | `composeApp/.../data/db/*.sq` | SQLDelight definitions |
| **E2E Tests** | `maestro-workspace/flows/` | YAML-based UI tests |

## CONNECTION ARCHITECTURE
MOCCA connects to a single OpenCode server instance. No secondary servers are needed.

- **Server**: OpenCode (`opencode serve --port 4096`)
- **Auth**: HTTP Basic Auth (`OPENCODE_SERVER_USERNAME` / `OPENCODE_SERVER_PASSWORD`)
- **Config**: `ServerConfig` stores `host`, `port`, `username`, `password` per server profile
- **Connection Flow**: `ConnectionManager` owns the `HttpClient` lifecycle. Consumers call `ApiExecutor.execute {}` — they never hold an `HttpClient` reference.
- **Git Operations**: Handled via OpenCode's built-in `/vcs` and `/session/:id/diff` endpoints, plus `executeShell()` for write operations. No separate Git server needed.
- **Connection Status**: `ConnectionStatus` sealed class — `NotConfigured`, `Disconnected(reason)`, `Connecting`, `WaitingForNetwork`, `Reconnecting(attempt, maxAttempts)`, `Connected(serverInfo, latencyMs)`, `Error(message)`.

## CONVENTIONS
- **Architecture**: MVI (ScreenModel -> StateFlow -> UI).
- **Offline-First**: Repositories return `Flow<Resource<T>>` (Cache -> Loading -> Network -> Cache).
- **Paths**: **ALWAYS** use absolute paths for file operations.
- **Theme**: "Compact Modern Glassmorphic" aesthetic.
    - **Background**: Pitch Black (`#000000`) for OLED power saving.
    - **Corners**: Rounded corners (12dp-32dp) for interactive elements.
    - **Buttons**: Pill-shaped (`CircleShape`).
    - **Typography**: Space Grotesk (sans-serif) with variable weights.
    - **Accents**: Mint Green (`#00D9A5`) and White.
    - **Compactness**: Reduced paddings and font sizes for high information density.

## ANTI-PATTERNS (STRICT)
- **NEVER** use `RectangleShape` for interactive elements (buttons, inputs, cards). Use `AppShapes.card`, `AppShapes.pill`, etc.
- **NEVER** use `ToolConfirmation` (Deprecated) -> Use `PermissionRequest`.
- **NEVER** use relative paths.
- **NEVER** block main thread (use `sendMessageAsync`).
- **NEVER** hold `HttpClient` references in consumers — use `ApiExecutor.execute {}`.
- **DO NOT** add `iosMain` or `desktopMain` (Android-only).
- **DO NOT** use physical device for `android-mcp` tasks (Emulator required).
- **DO NOT** ignore Detekt rules (`maxIssues: 0`).

## COMMANDS
```bash
# Build Debug APK
.\gradlew.bat :androidApp:assembleDebug

# Run E2E Tests (Requires Emulator)
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml

# Logcat Monitoring
adb logcat -c && adb logcat *:W | findstr "mocca|Exception"
```
