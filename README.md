# MOCCA

Android client for the **OpenCode** AI agent. Kotlin Multiplatform + Compose Multiplatform.

## Features

- **Session Management** — Real-time chat with streaming responses
- **File & Git Operations** — Browse, edit, version control via OpenCode VCS endpoints
- **Terminal Access** — WebSocket-based remote command execution
- **Offline-First** — SQLDelight-backed caching for instant access
- **Auto-Update** — GitHub Releases integration

## Architecture

```
UI (Compose Multiplatform)
  ↓
ScreenModels (Voyager / MVI)
  ↓
Repositories (Data Layer)
  ├── Local Cache (SQLDelight)
  └── Remote Data (ApiExecutor → ConnectionManager → HttpClient)
        ↓
OpenCode Server (HTTP Basic Auth)
```

**Key Components:**
- `ConnectionManager` — owns HttpClient lifecycle, auth, health checks
- `ApiExecutor` — interface for HTTP requests (consumers never hold HttpClient)
- `ConnectionStatus` — real-time connection state for UI

**UI Theme:** Neutral monochrome soft dark with clean Material 3 Surface components

## Quick Start

### 1. Prerequisites

- JDK 17+
- Android SDK (API 36)
- OpenCode CLI installed

### 2. Start OpenCode Server

```bash
# Set credentials (optional — default: username "opencode", no password)
export OPENCODE_SERVER_USERNAME=opencode
export OPENCODE_SERVER_PASSWORD=your_password

# Start server
opencode serve --port 4096

# For LAN/Tailscale access, bind to all interfaces:
opencode serve --port 4096 --hostname 0.0.0.0
```

### 3. Build & Install

```bash
# Windows
.\gradlew.bat :androidApp:assembleDebug

# macOS/Linux
./gradlew :androidApp:assembleDebug

# Install
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### 4. Configure Connection

| Method | Host | Notes |
|--------|------|-------|
| **Emulator** | `10.0.2.2` | Auto-detected, no config needed |
| **LAN** | Your machine's IP | Requires `--hostname 0.0.0.0` |
| **Tailscale** | Tailscale hostname/IP | Both devices must be on Tailscale |

**Settings** → Dashboard (right swipe) → `[SETTINGS]` → Enter host, port, username, password.

### 5. Verify Connection

Connection indicator in top bar:

| Color | Status |
|-------|--------|
| 🟢 Green | Connected (good latency) |
| 🟡 Yellow | Connected (high latency) |
| 🔴 Red | Disconnected / Error |
| ⚪ Gray | Not configured |

**Health check from any machine:**
```bash
curl -u opencode:your_password http://<host>:4096/global/health
# Expected: {"healthy":true,"version":"..."}
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Not Configured" | Go to Settings and enter server details |
| Connection refused | Verify OpenCode server is running |
| 401 Unauthorized | Check username/password match server config |
| Timeout on LAN | Ensure `--hostname 0.0.0.0` is set |
| Emulator can't connect | Server must listen on `127.0.0.1` or `0.0.0.0` |

## Development

### Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.0 |
| Compose Multiplatform | 1.9.3 |
| Koin | 4.1.1 |
| Voyager | 1.1.0-beta03 |
| AGP | 9.0.0-rc03 |

### Project Structure

```
MOCCA/
├── androidApp/           # Entry point (MainActivity, Manifest)
├── composeApp/           # 90% of code
│   └── src/commonMain/kotlin/com/mocca/app/
│       ├── api/          # Network (Ktor, SSE)
│       ├── data/         # Repositories + SQLDelight
│       ├── domain/       # Models (immutable)
│       └── ui/           # Screens + Components
├── maestro-workspace/    # E2E UI tests
└── gradle/               # Version catalog
```

### Commands

```bash
# Build
.\gradlew.bat :androidApp:assembleDebug

# E2E Tests (requires emulator)
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml

# Logcat
adb logcat -c && adb logcat *:W | findstr "mocca|Exception"
```

### Conventions

- **Architecture**: MVI (ScreenModel → StateFlow → UI)
- **Offline-First**: Repositories return `Flow<Resource<T>>`
- **Paths**: ALWAYS use absolute paths
- **Theme**: Soft Dark (`#1A1A1A`) with neutral palette, rounded corners, subtle cool accent

### Anti-Patterns (STRICT)

- NEVER use `RectangleShape` for interactive elements
- NEVER hold `HttpClient` references — use `ApiExecutor.execute {}`
- NEVER use relative paths
- NEVER block main thread
- DO NOT add iOS/Desktop targets

## License

MIT
