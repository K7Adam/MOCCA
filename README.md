# MOCCA

## 1. Project Overview
MOCCA is a **Kotlin Multiplatform** Android client for the **OpenCode** AI agent. It delivers a production-ready mobile interface enabling developers to remotely manage coding sessions, files, and terminal operations via secure HTTP and Server-Sent Events (SSE).

## 2. Key Features
- **Full Session Management**: Real-time chat with streaming responses and optimistic UI updates.
- **File & Git Operations**: Browse, search, and edit files; complete Git version control interface via OpenCode's built-in VCS endpoints.
- **Terminal Access**: WebSocket-based terminal emulation for remote command execution.
- **Offline-First**: SQLDelight-backed caching ensures instant access to sessions and logs.
- **Secure Control**: HTTP Basic Auth authentication and mobile permission approval for tool execution.
- **Auto-Update**: Checks GitHub Releases for new versions and installs updates automatically.

## 3. Architecture
The app follows a clean architecture with unidirectional data flow:

```
UI (Compose Multiplatform)
  ↓
ScreenModels (Voyager / State Management)
  ↓
Repositories (Data Layer)
  ├── Local Cache (SQLDelight)
  └── Remote Data (ApiExecutor → ConnectionManager → HttpClient)
        ↓
OpenCode Server (single server, HTTP Basic Auth)
```

**Connection Architecture:**
- `ConnectionManager` owns the `HttpClient` lifecycle, auth, health checks, and reconnection
- `ApiExecutor` interface: consumers call `execute {}` — they never hold `HttpClient` references
- `ConnectionStatus` sealed class provides real-time connection state to the UI
- Git operations use OpenCode's `/vcs` and `/session/:id/diff` endpoints (no separate Git server)

## 4. Setup Instructions
1.  **Prerequisites**: JDK 17+, Android SDK (API 36), and OpenCode server running.
2.  **Start OpenCode Server**:
    ```bash
    # Set credentials (optional — defaults to username "opencode" with no password)
    export OPENCODE_SERVER_USERNAME=opencode
    export OPENCODE_SERVER_PASSWORD=your_password

    # Start server
    opencode serve --port 4096
    ```
3.  **Build Debug APK**:
    ```bash
    # Windows
    .\gradlew.bat :androidApp:assembleDebug
    
    # macOS/Linux
    ./gradlew :androidApp:assembleDebug
    ```
4.  **Install**:
    ```bash
    adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
    ```
5.  **Connect**:
    - **Emulator**: Auto-connects to host via `10.0.2.2:4096`.
    - **LAN Device**: Enter your machine's LAN IP (e.g., `192.168.1.100`) and port `4096` in Settings.
    - **Tailscale**: Enter your Tailscale hostname/IP and port `4096` in Settings.
    - **Credentials**: Enter the same `username` and `password` set via `OPENCODE_SERVER_USERNAME` / `OPENCODE_SERVER_PASSWORD` on the server.

## 5. Development Workflow
- **Tech Stack**: Kotlin 2.3.0, Compose 1.9.3, Koin 4.1.1, Voyager 1.1.0-beta03, AGP 9.0.0-rc03.
- **Patterns**: MVI architecture; Repositories return `Flow<Resource<T>>`.
- **Guidelines**:
  - **Automated Tests**: CI builds debug APKs on every push.
  - **Linting**: Detekt static analysis is enforced via GitHub Actions.
  - **Absolute Paths**: Use absolute paths for all file operations.

## License
MIT

## Auto-Update Feature
The app supports checking for updates via GitHub Releases. The update dialog will automatically appear when a new version is available.

**Note:** For private repositories, GitHub authentication is required. The current implementation only works with public repositories. If your repository is private, you'll need to configure a GitHub Personal Access Token in the app (feature coming soon) or make the repository public.

### Current Implementation:
- Checks GitHub Releases API on app startup and manual trigger
- Compares versions including `-build.X` format
- Downloads and installs APK updates automatically
- Update dialog appears in MainScreen when an update is available

### Known Limitations:
- Private repositories require GitHub authentication (not yet implemented)
- Repository must be public for auto-updates to work without authentication
- Network permissions must be granted in Android Manifest
