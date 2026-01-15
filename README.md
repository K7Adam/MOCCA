# OpenCode Remote

## 1. Project Overview
OpenCode Remote is a **Kotlin Multiplatform** Android client for the **OpenCode** AI agent. It delivers a production-ready mobile interface matching the OpenChamber web UI, enabling developers to remotely manage coding sessions, files, and terminal operations via secure HTTP and Server-Sent Events (SSE).

## 2. Key Features
- **Full Session Management**: Real-time chat with streaming responses and optimistic UI updates.
- **File & Git Operations**: Browse, search, and edit files; complete Git version control interface.
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

## 5. Development Workflow
- **Tech Stack**: Kotlin 2.3.0, Compose 1.9.3, Koin 4.1.1, Voyager 1.1.0-beta03, AGP 9.0.0-rc03.
- **Patterns**: MVI architecture; Repositories return `Flow<Resource<T>>`.
- **Guidelines**:
  - **No Automated Tests**: Rely on rigorous manual verification.
  - **No Linting**: Manually adhere to standard Kotlin coding conventions.
  - **Absolute Paths**: Use absolute paths for all file operations.

## License
MIT
