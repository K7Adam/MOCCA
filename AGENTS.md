# PROJECT KNOWLEDGE BASE

**Generated:** 2026-01-15
**Project:** MOCCA (Mobile OpenCode Companion App)
**Stack:** Kotlin Multiplatform (Android-only) + Compose Multiplatform + Ktor + SQLDelight + Koin

## OVERVIEW
Android client for OpenCode AI agent, featuring edge-to-edge Terminal UI, offline-first architecture, real-time SSE control, and smart server detection.

## STRUCTURE
```
MOCCA/
├── androidApp/           # [Platform] Entry points (MainActivity, MoccaApp), Manifest, Res
├── composeApp/           # [Shared] Business logic + UI (90% of code)
│   └── src/commonMain/kotlin/com/mocca/app/
│       ├── api/          # Network layer (Ktor, SSE, Auth)
│       ├── data/         # Repositories (Offline-first) + SQLDelight
│       ├── di/           # Koin modules
│       ├── domain/       # Models, Sealed Classes, Contracts
│       └── ui/           # Screens (Voyager) + Theme + Components
├── docs/                 # Project documentation
└── gradle/               # Build logic & libs.versions.toml
```

## MODULE ARCHITECTURE
| Module | Type | Output | Purpose |
|--------|------|--------|----------|
| `composeApp` | **Library** | `composeApp/build/outputs/aar/` | Shared business logic + UI (90% of code) |
| `androidApp` | **Application** | `androidApp/build/outputs/apk/` | Entry point, produces installable APK |

> **CRITICAL**: Always build `:androidApp` module for APK. `:composeApp` only produces a library (AAR).

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| **Entry Point** | `androidApp/.../MainActivity.kt` | Edge-to-edge, `safeDrawingPadding` |
| **App Init** | `androidApp/.../MoccaApp.kt` | Koin `startKoin`, Napier logging |
| **Root UI** | `composeApp/.../ui/App.kt` | `TerminalTheme`, Voyager Navigator |
| **Networking** | `composeApp/.../api/` | `MoccaApiClient` (REST), `MoccaSseClient` (Events), 120s timeout |
| **State** | `composeApp/.../ui/screens/*Model.kt` | `ScreenModel` (ViewModel equivalent) |
| **Persistence** | `composeApp/.../data/local/` | `LocalCache` interface, SQLDelight `.sq` |
| **Build** | `gradle/libs.versions.toml` | Kotlin 2.3.0, Compose 1.9.3, AGP 9.0.0-rc03, minSdk 31, targetSdk 36 |

## CONVENTIONS
- **Offline-First**: Repositories return `Flow<Resource<T>>`. Emit DB cache first → Fetch network → Update DB → Emit new cache.
- **Exceptions**: `SessionRepository.getSession(id)` returns `suspend Resource` (skips Flow pattern).
- **Absolute Paths**: ALL file operations must use absolute paths.
- **UI Architecture**: MVI-style. ScreenModels expose `StateFlow<State>`, UI calls `collectAsState()`.
- **Navigation**: Settings reachable ONLY via DashboardPanel (Right Swipe).
- **Manual QA**: NO automated tests. NO linting. Rely on strict manual verification.
- **Terminal Theme**: 0dp corners, pitch-black background (#000000), monospace fonts.
- **Emulator Default**: `android-mcp` validation MUST occur on Emulator (`10.0.2.2`).
- **Server Detection**: `serverConfigProvider` auto-detects Emulator (`10.0.2.2`) vs Physical Device (LAN/Tailscale).
- **Network Resilience**: 120s timeout configured for all operations. SSE clients handle reconnection automatically.
- **Device Migration**: `ServerConfigRepository` automatically migrates physical devices from `10.0.2.2` to Tailscale/LAN.

## ANTI-PATTERNS (THIS PROJECT)
- **NEVER** use `ToolConfirmation` (Deprecated) → Use `PermissionRequest`.
- **Naming**: `ToolConfirmationDialog` name mismatch (should be `PermissionRequestDialog`).
- **NEVER** use relative paths for file ops.
- **NEVER** block the main thread. Chat messages use `sendMessageAsync`.
- **DO NOT** add `iosMain` or `desktopMain` (Android-only focus currently).
- **DO NOT** use physical device for `android-mcp` tasks. `android-mcp` testing MUST use Emulator (`10.0.2.2`).

## COMMANDS
```bash
# Build Debug APK
.\gradlew.bat :androidApp:assembleDebug

# Install via ADB
adb install androidApp\build\outputs\apk\debug\androidApp-debug.apk

# Build Release APK (unsigned)
.\gradlew.bat :androidApp:assembleRelease

# Clean
.\gradlew.bat clean
```

## NOTES
- **Network**: `network_security_config.xml` allows cleartext for local/LAN dev.
- **Emulator**: Host is `10.0.2.2:4096`. Auto-detected by `isEmulator()`.
- **Physical Devices**: Default to Tailscale/LAN. Migrated from `10.0.2.2` automatically.
- **Git Server**: Runs on port 4097 (auto-started by git-plugin.js when `opencode serve` launches).
- **Terminal**: Clear button unimplemented.

---

## GIT SERVER INTEGRATION

### Architecture
MOCCA's Git functionality requires a dedicated HTTP server running alongside OpenCode:

| Component | Port | Purpose |
|-----------|------|---------|
| OpenCode Server | 4096 | AI agent, SSE events, file operations |
| Git HTTP Server | 4097 | Git operations (status, commit, push, etc.) |

### How It Works
1. User runs `opencode serve 4096`
2. OpenCode loads `~/.config/opencode/plugin/git-plugin.js`
3. Plugin immediately starts embedded HTTP server on port 4097
4. MOCCA's `GitApiClient.kt` connects to `<host>:4097` for Git operations

### Setup (One-Time)
```bash
# Install dependencies for the git plugin
cd ~/.config/opencode
bun install  # or npm install
```

### Plugin Location
`~/.config/opencode/plugin/git-plugin.js` - Embedded HTTP server, no external files needed

### Troubleshooting
- **Git screen shows error**: Ensure `opencode serve` is running - the git server starts automatically
- **Port conflict**: Check if port 4097 is free (`netstat -an | grep 4097`)
- **Dependencies missing**: Run `bun install` in `~/.config/opencode/`

---

## AGENT DIRECTIVES (MANDATORY)

### 🔄 Dependency & Version Policy
> **ALWAYS use the latest stable versions** of all libraries, dependencies, and tools. **NEVER proactively downgrade** any dependency. If a build fails due to version incompatibility, research the fix for the newer version first before considering any downgrade.

### 🌐 Proactive Web Research
> **ALWAYS perform deep web research** before implementing features or making architectural decisions:
> - Search for **latest versions** of all dependencies
> - Research **current best practices** and **do's/don'ts**
> - Check for **new features** and **deprecated APIs**
> - Consult official documentation for **breaking changes**

### 📋 Logcat Monitoring
> **ALWAYS check logcat logs** after every build, install, or app launch:
> ```bash
> adb logcat -c && adb logcat *:W | findstr "mocca\|MOCCA\|Exception\|Error"
> ```
> **Proactively fix ALL errors and warnings** — do not leave any unresolved.

### 📚 Documentation Sync
> **ALWAYS verify and update ALL documentation** after making changes:
> - `README.md` — Setup instructions, tech stack versions
> - `AGENTS.md` — Project knowledge base, commands, conventions
> - `docs/` — Any feature-specific documentation
> 
> Documentation must be **synchronized with the current state** of the codebase at all times.

### 🔧 ADB Fallback Commands
When `android-mcp` is unreachable, use these ADB commands directly:

```bash
# List connected devices
adb devices

# Install APK
adb install -r androidApp\\build\\outputs\\apk\\debug\\androidApp-debug.apk

# Launch app
adb shell am start -n com.mocca.app/.android.MainActivity

# View logs (filtered)
adb logcat -c && adb logcat *:W | findstr "mocca"

# Clear app data
adb shell pm clear com.mocca.app

# Uninstall
adb uninstall com.mocca.app

# Take screenshot
adb exec-out screencap -p > screenshot.png

# Record screen (stop with Ctrl+C)
adb shell screenrecord /sdcard/recording.mp4
```

---

## AVAILABLE SKILLS

Reference these skills from `.agent/skills/` for specialized tasks:

### Android Development (Primary)
| Skill | When to Use |
|-------|-------------|
| `android-dev-standards` | **First read for ANY Android task** — coding standards, architecture patterns, best practices |
| `android-architecture` | MVVM, Clean Architecture, Repository pattern, Hilt DI |
| `android-jetpack-compose` | Building UIs with Compose, state management, declarative patterns |
| `android-ui-compose` | Implementing UI from UX flows, hierarchy, navigation |
| `android-ui-design` | XML layouts, ConstraintLayout, Material Design 3, accessibility |
| `android-platform` | Activities, Fragments, Services, lifecycle management |
| `android-networking` | Retrofit, OkHttp, REST APIs, SSL pinning, error handling |
| `android-data-storage` | Room, DataStore, SharedPreferences, SQLCipher |
| `android-production` | Testing, profiling, security, Play Store deployment |
| `android-code-review` | Code review for naming, memory leaks, UIState patterns, MVI/MVVM |

### Kotlin & Coroutines
| Skill | When to Use |
|-------|-------------|
| `kotlin-fundamentals` | Null safety, scope functions, extensions, OOP, SOLID |
| `kotlin-coroutines` | Async/await, runBlocking, suspending functions, Flow |

### Debugging & Analysis
| Skill | When to Use |
|-------|-------------|
| `bug-diagnosis` | Systematic debugging, hypothesis generation, root cause analysis |
| `deep-analysis` | Complex architectural decisions, trade-off analysis |
| `database-indexes` | SQL query optimization, N+1 pattern detection |
| `erd-documentation` | ERD diagrams, schema documentation, Mermaid |

### Cross-Platform Reference
| Skill | When to Use |
|-------|-------------|
| `mobile-guide` | iOS/Android/React Native/Flutter — general mobile reference |
