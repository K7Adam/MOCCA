# MOCCA

## 1. Project Overview
MOCCA is a **Kotlin Multiplatform** Android client for the **OpenCode** AI agent. It delivers a production-ready mobile interface matching the OpenChamber web UI, enabling developers to remotely manage coding sessions, files, and terminal operations via secure HTTP and Server-Sent Events (SSE).

## 2. Key Features
- **Full Session Management**: Real-time chat with streaming responses and optimistic UI updates.
- **File & Git Operations**: Browse, search, and edit files; complete Git version control interface.
- **Automatic Git Server Management**: Mobile app can start git HTTP server when unavailable via REST API command.
- **Terminal Access**: WebSocket-based terminal emulation for remote command execution.
- **Offline-First**: SQLDelight-backed caching ensures instant access to sessions and logs.
- **Secure Control**: Token-based authentication and mobile permission approval for tool execution.

## 3. Architecture
The app follows a clean architecture with unidirectional data flow:

```
UI (Compose Multiplatform)
  ↓
ScreenModels (Voyager / State Management)
  ↓
Repositories (Data Layer)
  ├── Local Cache (SQLDelight)
  └── Remote Data (Ktor Client / SSE)
        ↓
OpenCode Server (API)
  └── Git HTTP Server (Port 4097)
```

**Git Server Auto-Startup Flow:**
```
GitServerNotRunningDialog
    ↓ sends command
OpenCode /command endpoint
    ↓ triggers PowerShell
Git HTTP Server (Port 4097)
```

## 4. Setup Instructions
1.  **Prerequisites**: JDK 17+, Android SDK (API 36), and OpenCode server running (`opencode --port 4096`).
2.  **Build Debug APK**:
    ```bash
    # Windows
    .\gradlew.bat :androidApp:assembleDebug
    
    # macOS/Linux
    ./gradlew :androidApp:assembleDebug
    ```
3.  **Install**:
    ```bash
    adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
    ```
4.  **Connect**:
    - **Emulator**: Auto-connects to host via `10.0.2.2:4096`.
    - **Device**: Configure LAN or Tailscale IP in Settings.

### ⚠️ CRITICAL: Android Emulator Network Setup

**MANDATORY for Emulator Git Operations:**

The Android Emulator cannot reach the Git Server directly due to network limitations. You **MUST** set up ADB reverse port forwarding before launching the app:

```bash
# 1. Start Emulator
# 2. Set up ADB reverse for Git Server (port 4097)
adb reverse tcp:4097 tcp:4097

# 3. Verify setup
adb reverse --list
# Expected output should include: tcp:4097 tcp:4097
```

**Why this is needed:**
- Android Emulator's `10.0.2.2` mapping doesn't work for all ports
- Port 4097 (Git Server) is blocked from emulator
- ADB reverse maps host's port 4097 to emulator's localhost:4097
- Without this, ALL Git operations will fail with "Git server is not running"

**Troubleshooting:**
- If Git operations fail, verify: `adb reverse --list` includes `tcp:4097`
- If not, re-run: `adb reverse tcp:4097 tcp:4097`
- Restart emulator after setting up ADB reverse if issues persist

## 5. Development Workflow
- **Tech Stack**: Kotlin 2.3.0, Compose 1.9.3, Koin 4.1.1, Voyager 1.1.0-beta03, AGP 9.0.0-rc03.
- **Patterns**: MVI architecture; Repositories return `Flow<Resource<T>>`.
- **Guidelines**:
  - **Automated Tests**: CI builds debug APKs on every push.
  - **Linting**: Detekt static analysis is enforced via GitHub Actions.
  - **Absolute Paths**: Use absolute paths for all file operations.

## License
MIT

## Note
Auto-update feature enabled - app checks GitHub releases for updates.
