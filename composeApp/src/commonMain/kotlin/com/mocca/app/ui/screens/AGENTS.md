# UI & SCREENS KNOWLEDGE BASE

## OVERVIEW
Voyager-based navigation implementing feature-specific screens with MVI architecture and ScreenModel state management.

## STRUCTURE
```
ui/screens/
├── chat/           # Chat interface, message rendering, optimistic updates
├── files/          # File explorer, editor, search
├── git/            # Git controls: status, log, diff, commit
├── main/           # Main container, bottom nav, swipe panels
├── mcp/            # MCP tool management
├── onboarding/     # Initial setup flow
├── panels/         # Dashboard & Context panels for MainScreen
├── sessions/       # Session list, creation, deletion
├── settings/       # App configuration, server management
├── terminal/       # WebSocket terminal emulator
└── workspace/      # Workspace layout container
```

## WHERE TO LOOK
| Component | Location | Notes |
|-----------|----------|-------|
| Main Nav | `main/MainScreen.kt` | Root scaffold, `SwipePanelLayout` integration |
| ScreenModel | `*/XScreenModel.kt` | One per screen, extends Voyager `ScreenModel` |
| UI Content | `*/XScreen.kt` | Composable `Content()`, collects state |
| Dashboard | `panels/DashboardPanel.kt` | Side panel for quick settings/stats |
| Context | `panels/ContextHistoryPanel.kt` | Side panel for chat history/context |

## CONVENTIONS
- **Voyager Navigation**: All screens implement `Screen`. Use `LocalNavigator.currentOrThrow.push(Screen())`.
- **MVI Pattern**: `ScreenModel` exposes single `StateFlow<XState>`. UI observes via `collectAsState()`.
- **DI Injection**: Use `koinScreenModel<XScreenModel>()` in Composable.
- **Events**: One-off events (navigation) use `SharedFlow`.
- **State Immutability**: State classes are `data class` with `val`. Copy to update.
- **Loading/Error**: All states include `isLoading` and `error` fields.

## ANTI-PATTERNS
- **NEVER** put business logic in Composable. Use ScreenModel.
- **NEVER** expose MutableStateFlow to UI. Expose as `StateFlow`.
- **AVOID** passing complex objects in navigation. Pass IDs (e.g., `sessionId`).
- **DO NOT** duplicate scaffold for full-screen dialogs.
