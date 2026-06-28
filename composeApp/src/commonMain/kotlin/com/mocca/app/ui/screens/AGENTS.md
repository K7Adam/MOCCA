# UI & SCREENS KNOWLEDGE BASE

**Updated:** 2026-06-28
**Scope:** UI screens, screen models, navigation shell

**Relevant Skills:** `taste-skill-compose`, `material3-expressive-compose`, `kotlin-best-practices`

## OVERVIEW
Compose Multiplatform screen layer built on Voyager + strict MVI. `MainScreen` is the app shell; most other screens are feature leaves hanging off the chat/dashboard/session flow.

## NAVIGATION
- **Voyager**: `Navigator` drives screen transitions; use `LocalNavigator.currentOrThrow`
- **App shell**: `MainScreen` hosts a 3-panel `SwipePanelLayout`
  - Left: context/session history
  - Center: active chat
  - Right: dashboard tools
- **Settings path**: dashboard panel -> `[SETTINGS]` -> `SettingsScreen`
- **Shared transitions**: root `App()` wraps navigation in `SharedTransitionLayout`

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| App shell | `main/MainScreen.kt` | Panel host + bottom bar |
| App shell logic | `main/MainScreenModel.kt` | Aggregates app/chat state |
| Chat UX | `chat/` | Main user workflow |
| Files explorer | `files/` | Viewer/editor flow |
| Git tools | `git/` | Status + diff workflows |
| MCP tooling | `mcp/` | MCP status/resources |
| Onboarding | `onboarding/` | First-run and connection setup |
| Sessions | `sessions/` | Session components |
| Settings subtree | `settings/` | Dedicated AGENTS.md there |
| Terminal | `terminal/` | Interactive terminal session UI: raw/cooked input modes, real-time font size control via 3-dots actions menu, bridge connection state handling, capability checks, tuned row-height rendering |
| Dashboard panels | `panels/` | Context/history + dashboard modules |
| Skills | `skills/` | Placeholder (empty) |
| Workspace | `workspace/` | Placeholder (empty) |
| Worktree | `worktree/` | Placeholder (empty) |

## SCREEN LIST
| Screen | Role |
|--------|------|
| `ProgressiveOnboardingScreen` | First-run connection setup with mesh-gradient hero surface |
| `MainScreen` | 3-panel shell + update/dialog coordination |
| `ChatScreen` | Active session/messages |
| `GitScreen` / `GitDiffScreen` | VCS UI over OpenCode endpoints |
| `FilesScreen` | Browse + edit workspace files |
| `McpScreen` / `McpResourceScreen` | MCP inspection |
| `SettingsScreen` / `FeatureFlagsScreen` | Preferences + server-side config |
| `TerminalScreen` | Terminal session UI |
| `SessionsScreen` | Session management components |

## MVI RULES
- `ScreenModel` owns repository calls, validation, and state transitions
- UI observes immutable `StateFlow` only
- Prefer simple screen constructor params (IDs/keys), then load inside the model
- `koinScreenModel<T>()` is the standard DI entry inside `Content()`

## TESTING NOTES
- New leaf screens should usually get a matching Maestro navigation flow or coverage in an existing plan
- `maestro-workspace/flows/navigation/` is the current screen reachability baseline
- Shared selectors belong in `composeApp/.../ui/TestTags.kt`, not ad-hoc string duplication

## ANTI-PATTERNS
- NEVER put business logic in composables
- NEVER expose `MutableStateFlow` publicly
- AVOID complex navigation params when an ID load will do
- DO NOT block the main thread; use `screenModelScope.launch`
