# PROJECT KNOWLEDGE BASE

**Generated:** 2026-01-18
**Project:** MOCCA (Mobile OpenCode Companion App)
**Stack:** Kotlin Multiplatform (Android-only) + Compose Multiplatform + Koin + Voyager + SQLDelight

## OVERVIEW
Android client for OpenCode AI agent. Features edge-to-edge "Pitch Black" Terminal UI, offline-first architecture, and a unique dual-server connection (AI Agent + Git HTTP Server).

## STRUCTURE
```
MOCCA/
├── androidApp/           # [Platform] Entry (MainActivity), Manifest, Res
├── composeApp/           # [Shared] 90% of code. UI, Logic, Data.
│   └── src/commonMain/kotlin/com/mocca/app/
│       ├── api/          # Network (Ktor, SSE) + Git Server Logic
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
| **Git Server** | `composeApp/.../api/GitApiClient.kt` | Auto-start logic (Port 4097) |
| **DB Schema** | `composeApp/.../data/db/*.sq` | SQLDelight definitions |
| **E2E Tests** | `maestro-workspace/flows/` | YAML-based UI tests |

## CONVENTIONS
- **Architecture**: MVI (ScreenModel -> StateFlow -> UI).
- **Offline-First**: Repositories return `Flow<Resource<T>>` (Cache -> Loading -> Network -> Cache).
- **Paths**: **ALWAYS** use absolute paths for file operations.
- **Theme**: "Glassmorphic Terminal" aesthetic.
    - **Background**: Pitch Black (`#000000`) for OLED power saving.
    - **Corners**: Rounded corners (16dp-32dp) for interactive elements.
    - **Buttons**: Pill-shaped (`CircleShape`).
    - **Typography**: Space Grotesk (sans-serif) with variable weights.
    - **Accents**: Mint Green (`#00D9A5`) and White.
- **Environment**: `serverConfigProvider` auto-detects `10.0.2.2` (Emulator) vs Tailscale/LAN.

## ANTI-PATTERNS (STRICT)
- **NEVER** use `RectangleShape` for interactive elements (buttons, inputs, cards). Use `AppShapes.card`, `AppShapes.pill`, etc.
- **NEVER** use `ToolConfirmation` (Deprecated) -> Use `PermissionRequest`.
- **NEVER** use relative paths.
- **NEVER** block main thread (use `sendMessageAsync`).
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

## GIT SERVER INTEGRATION
MOCCA requires a secondary HTTP server for Git operations.
- **Port**: 4097 (Target), 4096 (OpenCode).
- **Auto-Start**: App can trigger `start-git-server.ps1` on host via OpenCode command.
- **See**: `composeApp/src/commonMain/kotlin/com/mocca/app/api/AGENTS.md` for protocol details.
