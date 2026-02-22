# PROJECT KNOWLEDGE BASE

**Updated:** 2026-02-22
**Project:** MOCCA (Mobile OpenCode Companion App)
**Stack:** Kotlin Multiplatform (Android-only) + Compose Multiplatform + Koin + Voyager + SQLDelight

## OVERVIEW
Android client for OpenCode AI agent. Pitch Black OLED UI, offline-first architecture, single-server HTTP Basic Auth connection.

## STRUCTURE
```
MOCCA/
├── androidApp/           # Entry (MainActivity, Manifest)
├── composeApp/           # 90% of code
│   └── src/commonMain/kotlin/com/mocca/app/
│       ├── api/          # Ktor, SSE, ApiExecutor
│       ├── data/         # Repositories + SQLDelight
│       ├── domain/       # Models (immutable)
│       └── ui/           # Screens, Components
├── maestro-workspace/    # E2E UI tests (YAML)
└── .opencode/skills/     # Agent skills
```

## WHERE TO LOOK
| Task | Location |
|------|----------|
| Entry Point | `androidApp/.../MoccaApp.kt` |
| Root UI | `composeApp/.../ui/App.kt` |
| Connection | `composeApp/.../data/repository/ConnectionManager.kt` |
| API Client | `composeApp/.../api/MoccaApiClient.kt` |
| Git Operations | `composeApp/.../data/repository/GitRepository.kt` |
| DB Schema | `composeApp/.../data/db/*.sq` |
| E2E Tests | `maestro-workspace/flows/` |

## SKILLS
| Skill | Use When |
|-------|----------|
| `kotlin-best-practices` | KMP architecture, MVI, DI patterns |
| `taste-skill-compose` | UI/UX design, animations, theming, liquid glass |
| `android-mcp` | Device automation, ADB commands |

**See `.opencode/skills/` for detailed skill documentation.**

## CONNECTION ARCHITECTURE
- **Server**: OpenCode (`opencode serve --port 4096`)
- **Auth**: HTTP Basic Auth
- **Flow**: `ConnectionManager` owns `HttpClient` → Consumers use `ApiExecutor.execute {}`
- **Status**: `NotConfigured`, `Disconnected`, `Connecting`, `Reconnecting`, `Connected`, `Error`

## CONVENTIONS
- **Architecture**: MVI (ScreenModel → StateFlow → UI)
- **Offline-First**: Repositories return `Flow<Resource<T>>`
- **Paths**: ALWAYS absolute paths
- **Theme**: Pitch Black (`#000000`), Mint Green (`#00D9A5`), rounded corners
- **Glass Effects**: Use [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (`io.github.kyant0:backdrop`) for liquid glass UI

## ANTI-PATTERNS (STRICT)
- NEVER use `RectangleShape` for interactive elements
- NEVER hold `HttpClient` references — use `ApiExecutor.execute {}`
- NEVER use relative paths
- NEVER block main thread
- DO NOT add iOS/Desktop targets
- DO NOT use physical device for `android-mcp` tasks

## COMMANDS
```bash
# Build
.\gradlew.bat :androidApp:assembleDebug

# E2E Tests
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml

# Logcat
adb logcat -c && adb logcat *:W | findstr "mocca|Exception"
```
