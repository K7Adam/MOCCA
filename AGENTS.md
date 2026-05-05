# PROJECT KNOWLEDGE BASE

**Updated:** 2026-05-05
**Commit:** 05faecd
**Project:** MOCCA (Mobile OpenCode Companion App)
**Stack:** Kotlin Multiplatform (Android-only) + Compose Multiplatform + Koin + Voyager + SQLDelight + Material 3 Expressive

## OVERVIEW
Android client for OpenCode AI agent. Server-first state pipeline, Android bootstrap shell, Compose Multiplatform UI, and emulator-driven Maestro verification.

## STRUCTURE
```
MOCCA/
├── androidApp/           # Android bootstrap, Manifest, services, notification wiring
├── composeApp/           # Shared app code + androidMain implementations
│   └── src/commonMain/kotlin/com/mocca/app/
│       ├── api/          # Ktor clients, retry, SSE support
│       ├── bridge/       # CLI bridge client, connection, protocol
│       ├── data/         # LocalCache, repositories, state stores
│       ├── di/           # Koin module graph (ordering matters)
│       ├── discovery/    # Reserved for cross-platform server discovery (empty)
│       ├── domain/       # Models, events, sync contracts
│       ├── ui/           # App shell, screens, components, theme
│       └── util/         # Shared formatters, lifecycle/network abstractions
├── maestro-workspace/    # Emulator-only E2E flows, subflows, test plans
├── .agent/skills/        # Android Studio skill compatibility adapters
├── .agents/skills/       # Cross-agent skills shared by Codex/OpenCode/Gemini/Windsurf-capable clients
└── .opencode/skills/     # Project skills
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Android bootstrap | `androidApp/src/main/java/com/mocca/app/MoccaApp.kt` | Koin startup + lifecycle observer |
| Android launcher | `androidApp/src/main/java/com/mocca/app/MainActivity.kt` | Edge-to-edge + deep links + `App()` |
| Root Compose shell | `composeApp/src/commonMain/kotlin/com/mocca/app/ui/App.kt` | Splash, theme, navigator start screen |
| DI graph | `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` | Declaration order matters |
| Connection lifecycle | `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ConnectionManager.kt` | Owns legacy OpenCode `HttpClient` + health checks |
| Event fanout | `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/StateCoordinator.kt` | SSE -> stores -> UI backbone |
| Chat event reducer | `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/ChatTurnReducer.kt` | Canonical session/message/part state from OpenCode events |
| Global UI state | `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/AppStateStore.kt` | Non-chat app state |
| Chat UI state | `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ChatStateStore.kt` | Messages, streaming, permissions |
| AI runtime config | `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/AiRuntimeConfigRepository.kt` | Bridge-first provider/model/agent/mode selection |
| Silent polling sync | `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/RealtimeSyncService.kt` | Non-SSE refresh loop |
| Bridge lifecycle | `composeApp/src/commonMain/kotlin/com/mocca/app/bridge/connection/BridgeConnectionManager.kt` | CLI bridge client + pairing |
| Bridge bootstrap | `composeApp/src/commonMain/kotlin/com/mocca/app/bridge/opencode/BridgeRuntimeBootstrapper.kt` | Bridge init flow |
| Theme tokens | `composeApp/src/commonMain/kotlin/com/mocca/app/ui/theme/` | Dedicated AGENTS.md there |
| Settings subtree | `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/` | Dedicated AGENTS.md there |
| DB schema | `composeApp/src/commonMain/sqldelight/com/mocca/app/db/*.sq` | 8 SQLDelight tables + 2 migrations; `2.sqm` drops retired `RecentModel` |
| E2E tests | `maestro-workspace/` | Dedicated AGENTS.md there |
| Test suite | `composeApp/src/commonTest/kotlin/com/mocca/app/` | 22 KMP commonTest files |
| Scripts | `scripts/mocca-serve-emulator.ps1` | Emulator-specific OpenCode server |

## STARTUP CHAIN
- `AndroidManifest.xml` -> `MoccaApp` -> `startKoin(...)`
- `MainActivity` -> `setContent { App() }`
- `App()` waits for `ServerConfigRepository.isLoaded`
- Start screen: configured server -> `MainScreen`, otherwise `ProgressiveOnboardingScreen`
- `ConnectionManager.onConnectionEstablished` wires into `StateCoordinator.start()` + `syncFromServer()`

## CROSS-CUTTING ARCHITECTURE
- **Event pipeline**: `EventStreamRepository` -> `StateCoordinator` -> `BroadcastEvent` -> `AppStateStore` / `ChatStateStore` -> ScreenModels -> UI
- **Bridge pipeline**: `BridgeConnectionManager` -> `BridgeRuntimeBootstrapper` -> `OpenCodeBridgeRepository` -> bridge events -> `StateCoordinator` -> state stores
- **Chat turn state**: `ChatTurnReducer` owns idempotent OpenCode event state keyed by `sessionID`, `messageID`, and `partID`. Bridge and fallback SSE events must converge here before UI-specific compatibility state is derived.
- **Bridge-first runtime**: the MOCCA CLI bridge is the preferred provider/model/agent configuration source. Direct OpenCode HTTP/SSE remains a legacy fallback path until parity is complete.
- **Dual sync model**: SSE for sessions/messages + `RealtimeSyncService` for MCP/providers/git/tools/commands/agents. The Server-First pattern (Cache → Network → Update → Emit) is mandatory for primary user-facing data (sessions, messages) and optional for infrequently-changing secondary data (agent list, providers, diffs).
- **AI runtime persistence**: `AiSelection` and project-scoped `AiRecentModel` lists live in `AppSettings` under `ai.selection.*` and `ai.recents.*`. Do not reintroduce the removed global `RecentModel` table or `SessionRepository` recents API.
- **Streaming persistence**: `LocalCache.updateMessagePart(...)` is part-addressable. Pass `messageId`, `partId`, and `partType`; do not append token deltas through multiple repositories.
- **Platform abstractions**: `NetworkObserver`, `AppLifecycleObserver`, `SecureTokenStorage`, `NotificationTracker`
- **Global helpers**: `PreferencesManager`, `GlobalActivityManager`, `UpdateNotifier`, `DatabasePruner`, `TestTags`

## SKILLS
| Skill | Use When |
|-------|----------|
| `kotlin-best-practices` | KMP architecture, repositories, domain/state contracts |
| `taste-skill-compose` | UI/UX composition, hierarchy, interaction polish |
| `material3-expressive-compose` | Material 3 Expressive token work, theming, component refactors |
| `android-mcp` | Emulator startup, install, Maestro, log capture |
| `mocca-android-agent-workflow` | Android CLI, Android skills, Android Knowledge Base, and cross-tool agent workflow |

## ANDROID AGENT TOOLING
- Shared agent entrypoint: `.agents/skills/mocca-android-agent-workflow/SKILL.md`
- Android Studio compatibility adapter: `.agent/skills/mocca-android-agent-workflow/SKILL.md` points back to the shared entrypoint.
- Android CLI wrapper: `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\scripts\android-cli.ps1` delegates to `android` on `PATH` or the installed bundle in `C:\Users\ruzaq\.android\cli`.
- Setup/check script: `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\scripts\setup-android-agent-tooling.ps1`
- Use Android CLI when available for `android update`, `android init`, `android skills add --all`, `android describe --project_dir=...`, and `android docs search` / `android docs fetch`.
- If `android` is not visible in the active shell, use `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\scripts\android-cli.ps1" ...`.
- On Windows, do not rely on `android emulator`; Android CLI 0.7 documents that command as disabled. Use `maestro-workspace/start-emulator.ps1` and `maestro-workspace/run-emulator-tests.ps1` for MOCCA emulator workflows.
- Before changing Android platform APIs, AGP/Gradle behavior, Compose, Navigation, R8, Play Billing, edge-to-edge, emulator automation, or performance guidance, query the Android Knowledge Base with `android docs search` and fetch the relevant `kb://` result.

## CONVENTIONS
- **Architecture**: MVI (`ScreenModel -> StateFlow -> UI`), no logic in composables
- **Network**: Consumers use `ApiExecutor.execute {}`; `ConnectionManager` is the only `HttpClient` owner
- **Persistence**: Repositories depend on `LocalCache`, not raw DB drivers
- **Theme**: `AppTheme`, `AppColors`, `AppTypography`, `AppShapes`; Material 3 defaults are not the source of truth
- **Elevation**: Prefer `tonalElevation` for depth. `shadowElevation` is acceptable for modals (dialogs, modal sheets) that need extra visual separation
- **Motion**: For motion/animation, `MaterialTheme.motionScheme` is the standard API. Do not create custom animation constant objects
- **Testing**: Local UI QA uses visible emulator first; CI uses headless emulator via GitHub Actions
- **Paths**: Use absolute paths in scripts and agent docs

## ANTI-PATTERNS (STRICT)
- NEVER hold `HttpClient` references outside `ConnectionManager` (exception: `GitHubApiClient` holds its own `HttpClient` for external GitHub API calls, not the OpenCode server)
- NEVER put validation/network/state mutation directly in composables (simple UI-only transforms like date formatting or derived display strings are acceptable)
- NEVER use `RectangleShape` for interactive elements
- NEVER mix `MaterialTheme.colorScheme` / `MaterialTheme.shapes` into app primitives in feature/UI code — use `AppColors`, `AppShapes`, `AppTypography` instead. Theme bridge code (`AppTheme.kt`) legitimately uses `MaterialTheme.colorScheme` to provision the M3 shell.
- NEVER block the main thread; use `Dispatchers.IO` for blocking DB/file/network I/O (not required for non-blocking suspend calls)
- NEVER store model picker recents through `SessionRepository` or a global SQLDelight table; use project-scoped `AiRecentModel` persistence in `AppSettings`
- NEVER create another global `streamingText`/`thinkingContent` source of truth. Compatibility flows may mirror reducer state, but canonical chat-turn state is part-keyed.
- NEVER use relative paths in scripts/automation docs
- DO NOT add iOS/Desktop targets
- DO NOT use physical devices for `android-mcp` or Maestro host-connectivity checks

## COMMANDS
```bash
# Start visible emulator
.\maestro-workspace\start-emulator.ps1

# Build debug APK
.\gradlew.bat :androidApp:assembleDebug

# Run smoke plan on running emulator
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml

# Generate screenshot catalog
.\maestro-workspace\capture-screenshot-catalog.ps1

# Start emulator-specific OpenCode server
.\scripts\mocca-serve-emulator.ps1

# Filter logcat for app warnings/errors
adb logcat -c && adb logcat *:W | findstr "mocca|Exception"
```

## NOTES
- `gradle.properties` is tuned for constrained RAM (`workers.max=1`, configuration cache on)
- `Modules.kt` ordering is not cosmetic; moving coordinator/state-store registrations can break bootstrap
- Root README has newer theme wording than the old root AGENTS did; keep AGENTS aligned with current Material 3 Expressive setup
- `discovery/` package exists but is currently empty — server discovery is bridge-first via QR pairing
- `UpdateRepository.kt` holds a standalone `HttpClient` for APK redirect downloads (line 157) — documented deviation from the `ApiExecutor` pattern
- `ServerConfigRepository.kt:50` uses `runBlocking` — known anti-pattern violation for synchronous server-load
