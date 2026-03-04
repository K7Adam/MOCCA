# UI & SCREENS KNOWLEDGE BASE

**Scope:** UI Screens, MVI Architecture, Navigation

**Relevant Skills:** `taste-skill-compose` (UI/UX), `kotlin-best-practices` (MVI architecture)

## OVERVIEW
The UI layer is built with **Compose Multiplatform** using a strict **MVI (Model-View-Intent)** architecture. Navigation is managed by **Voyager**, and state is held in **ScreenModels** which are injected via Koin. All screens follow a **Compact Modern M3 Surface-based** design aesthetic with neutral monochrome palette.

## NAVIGATION
- **Voyager Stack**: The app uses `Navigator` for screen transitions. Access it via `LocalNavigator.currentOrThrow`.
- **Panel System**: `MainScreen` implements a `SwipePanelLayout` for a 3-panel experience:
    - **Left Swipe (Context)**: Session list, model info, and token usage.
    - **Center (Focus)**: Active chat interface and message history.
    - **Right Swipe (Dashboard)**: Modular tools dashboard (Git status, MCP servers, Tools).
- **Settings Access**: The **Settings** screen is reachable exclusively via the **Dashboard swipe** (Right panel) by clicking the `[SETTINGS]` button.

## SCREEN LIST
| Screen | Logic Highlight | Description |
|--------|-----------------|-------------|
| `MainScreen` | Panel Management | Host scaffold for the 3-panel UI. Manages global session state and `ConnectionStatus` observation. |
| `GitScreen` | VCS Operations | Comprehensive Git UI. Uses OpenCode's `/vcs` endpoint for status and `executeShell()` for git commands. `GitDiffScreenModel` uses `getSessionDiffs(sessionId)` + `SessionRepository`. |
| `SettingsScreen` | Server Config | Configuration for OpenCode server host, port, username, and password. |
| `McpScreen` | Tool Inspection | UI for managing MCP servers and viewing available tools. |
| `ConsoleScreen` | SSH/WebSocket | Real-time terminal emulator for direct host access. |
| `FilesScreen` | Explorer | Browsing and basic editing of the project workspace. |

## MVI ARCHITECTURE (STRICT)
- **State**: Every `ScreenModel` exposes a single, immutable `StateFlow<State>`. 
- **Observation**: UI observes state via `val state by screenModel.state.collectAsState()`.
- **ScreenModel**: All business logic, repository calls, and state transitions MUST happen here.
- **DI**: Use `koinScreenModel<T>()` inside the Composable `Content()` function to retrieve the model.

## ANTI-PATTERNS
- **NEVER put logic in Composables**: No validation, no network calls, no state manipulation in `@Composable` functions.
- **NEVER expose MutableStateFlow**: Keep the mutable state private in the `ScreenModel`; only expose the read-only `StateFlow`.
- **AVOID complex navigation params**: Pass IDs or simple strings in `Screen` constructors. Let the `ScreenModel` load the data.
- **DO NOT block the main thread**: Always use `screenModelScope.launch` for asynchronous operations.
