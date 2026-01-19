# MOCCA (Mobile OpenCode Companion App)

## Project Overview
MOCCA is a **Kotlin Multiplatform** Android client for the **OpenCode** AI agent. It allows developers to remotely manage coding sessions, files, and terminal operations via secure HTTP and Server-Sent Events (SSE). It is designed with an **offline-first** architecture and a "God Mode" aesthetic (high contrast, terminal-like).

## Architecture
- **Type:** Kotlin Multiplatform (KMP) targeting **Android**.
- **Pattern:** MVI (Model-View-Intent) with Unidirectional Data Flow.
- **Navigation:** Voyager.
- **Dependency Injection:** Koin.
- **Data:** SQLDelight (Local Cache) + Ktor (Remote API).

### Modules
- **`composeApp`**: The core library containing **90% of the code**. Includes:
    - `src/commonMain`: Shared business logic, UI, API client, and Database.
    - `src/androidMain`: Android-specific implementations (Drivers, Ktor engines).
- **`androidApp`**: The Android entry point module. Produces the installable APK.

## Key Directories
- `androidApp/src/main/`: Android Manifest, Resources, `MainActivity`.
- `composeApp/src/commonMain/kotlin/com/mocca/app/`:
    - `api/`: Networking (Ktor, SSE).
    - `data/`: Repositories and SQLDelight definitions.
    - `domain/`: Data models and business rules.
    - `ui/`: Compose screens and components.
    - `di/`: Koin modules.

## Development Workflow

### Building & Running
**Do not** try to run the `composeApp` module directly. Always build the `androidApp`.

**Windows:**
```powershell
.\gradlew.bat :androidApp:assembleDebug
```

**macOS/Linux:**
```bash
./gradlew :androidApp:assembleDebug
```

### Installation
```bash
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Verification
- **Automated Tests:** None.
- **Linting:** None currently enforced.
- **Manual QA:** Strict manual verification is required.
- **Logcat:** **ALWAYS** check logcat after an install/launch.
    ```bash
    adb logcat -c && adb logcat *:W | findstr "mocca\|MOCCA\|Exception\|Error"
    ```

## Agent Guidelines & Conventions

### Core Rules
1.  **Offline-First:** Repositories must return `Flow<Resource<T>>`. Emit cached data immediately, then fetch from network, update DB, and emit new cache.
2.  **Absolute Paths:** ALL file operations must use absolute paths.
3.  **No "Slop":** Avoid boilerplate. Keep code focused and "God Mode" efficient.
4.  **Dependencies:** Always use the latest stable versions. Do not downgrade.

### Environment
- **Emulator:** Host is `10.0.2.2`.
- **Physical Device:** Auto-migrates to Tailscale/LAN IP.
- **Git Server:** Runs on port `4097`. OpenCode runs on `4096`.

### Anti-Patterns
- **NEVER** use relative paths.
- **NEVER** block the main thread.
- **DO NOT** add iOS or Desktop targets without explicit instruction.

## Tech Stack
- **Language:** Kotlin 2.3.0
- **UI:** Compose Multiplatform 1.9.3, Material3
- **Navigation:** Voyager
- **DI:** Koin
- **Network:** Ktor, Server-Sent Events (SSE)
- **Database:** SQLDelight
- **Logging:** Napier
