# MOCCA OpenCode v2 Migration & Feature Parity Plan

**Created:** 2026-07-12
**Status:** Planning
**OpenCode Target Version:** v1.17.11+ (v2 API)
**Android minSdk Target:** 36 (Android 16+)

## Decisions Summary

| # | Decision | Choice |
|---|----------|--------|
| 1 | Bridge vs Direct HTTP | Keep bridge as primary connection layer |
| 2 | Missing part types | Implement all 8 at once |
| 3 | Streaming protocol | Migrate to `message.part.updated` with `delta` field (drop `message.part.delta`) |
| 4 | Permission reply format | New format only: `reply: once/always/reject` via `POST /session/:id/permissions/:permissionID` |
| 5 | Notification architecture | Tiered: Live Updates (progress) + Standard (actionable) + Silent (metadata) |
| 6 | Todo system | Full bidirectional: display + write + Live Update notifications |
| 7 | Server discovery | QR code pairing (MOCCA CLI generates QR) |
| 8 | V2 API migration | Migrate all endpoints to v2 `/api/` prefix |
| 9 | Question API | Full integration + inline notification replies |
| 10 | Session snapshots/revert | Full snapshot + revert UI with diff visualization |
| 11 | Skills + Custom Tools | Full UI for both (browser + inspector in Settings) |
| 12 | Competitive differentiator | Native Android depth (Live Notifications, inline replies, foreground service, widgets) |
| 13 | Structured Output | Full: schema definition + formatted JSON view |
| 14 | TUI Control | Optional remote control feature |
| 15 | Connection reliability | Enhanced: SSE buffering, version detection, quality indicator |
| 16 | Implementation priority | All workstreams in parallel |
| 17 | QR code source | MOCCA CLI bridge server generates QR in terminal |
| 18 | Android minSdk | Raise to API 36 (Android 16+) for Live Updates |
| 19 | Bridge v2 migration | Bridge uses v2 API endpoints internally too |
| 20 | Plugin UI | Read-only status screen in Settings |

---

## Workstream A: Protocol Alignment (V2 API + Format Migrations)

### A1: V2 API Endpoint Migration
**Scope:** Migrate all 78 REST endpoints from v1 to v2 `/api/` prefix with flattened parameters.

**Files to modify:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — rewrite all endpoint paths
- `composeApp/src/commonMain/kotlin/com/mocca/app/bridge/opencode/OpenCodeBridgeRepository.kt` — bridge uses v2 internally
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ConnectionManager.kt` — update base URL handling

**Key changes:**
- All endpoint paths get `/api/` prefix (e.g., `/session` → `/api/session`)
- Flattened parameters instead of nested `{ path, body, query }`
- Remove 2 deprecated endpoints: `GET /app` (getAppInfo), `GET /tool` (getTools)
- Add new endpoints: `POST /api/session/:id/todo` (write todos), `POST /api/question`, `GET /api/question/:requestID/wait`, `POST /api/question/ask`

**Acceptance criteria:**
- All existing unit tests pass with v2 endpoints
- Bridge protocol uses v2 endpoints internally
- Deprecated endpoints removed

### A2: Streaming Protocol Migration
**Scope:** Migrate from `message.part.delta` to `message.part.updated` with `delta` field.

**Files to modify:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaSseClient.kt` — remove `message.part.delta` handler, update `message.part.updated` handler to extract `delta` field
- `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/ServerEvents.kt` — update `EventMessagePartUpdated` to include `delta?: String` field, remove `EventMessagePartDelta`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/EventStreamRepository.kt` — update delta handling
- `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/ChatTurnReducer.kt` — update delta reduction

**Acceptance criteria:**
- Streaming text renders correctly with v1.17.11 server
- No `message.part.delta` event handler remains
- Delta field in `message.part.updated` correctly appended to part text

### A3: Permission Format Migration
**Scope:** Migrate permission reply from `status: ask/deny/allow` to `reply: once/always/reject`.

**Files to modify:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — change `replyToPermission()` endpoint from `POST /permission/{id}/reply` to `POST /api/session/:id/permissions/:permissionID`, change body field from `status` to `reply`, change values from `ask/deny/allow` to `once/always/reject`
- `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/ServerEvents.kt` — update `PermissionRequest` model if needed
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/PermissionActionBus.kt` — update reply format
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/` — update permission UI to show `once/always/reject` instead of `ask/deny/allow`

**Acceptance criteria:**
- Permission approvals work with v1.17.11 server
- UI shows "Once / Always / Reject" buttons
- `always` option correctly whitelists future matching permissions

### A4: Part Type Implementation (8 New Types)
**Scope:** Implement domain models, UI rendering, and persistence for all 8 missing part types.

**New part types to implement:**

| Part Type | Domain Model | UI Rendering | Persistence |
|-----------|-------------|-------------|-------------|
| `subtask` | `SubtaskPart` | Agent delegation card with prompt, description, agent name | Store as message part with type field |
| `step-start` | `StepStartPart` | Step boundary marker with optional snapshot | Store as message part |
| `step-finish` | `StepFinishPart` | Step completion with reason, cost, tokens breakdown | Store as message part |
| `snapshot` | `SnapshotPart` | Session state snapshot marker (visual indicator in timeline) | Store snapshot reference |
| `patch` | `PatchPart` | File patch card with hash and affected files list | Store patch metadata |
| `agent` | `AgentPart` | Agent mention/reference chip with name and source | Store as message part |
| `retry` | `RetryPart` | Retry attempt card with attempt number, error details, timestamp | Store as message part |
| `compaction` | `CompactionPart` | Context compaction marker (visual indicator) | Store as message part |

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — add 8 new part type data classes, update `MessagePart` sealed class
- `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/ServerEvents.kt` — update `EventMessagePartUpdated` to handle all part types
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/` — create renderers for each new part type
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatContent.kt` — render new part types in message list
- `composeApp/src/commonMain/sqldelight/com/mocca/app/db/Message.sq` — ensure schema supports all part types (may need migration)

**Acceptance criteria:**
- All 12 part types render correctly in chat UI
- No content is silently dropped from messages
- Step boundaries show cost/token breakdown
- Subtask delegations show agent name and prompt
- Retry attempts show error details
- Compaction markers show in timeline

---

## Workstream B: Notification System (Native Android Depth)

### B1: Android 17 Live Updates Integration
**Scope:** Implement Live Updates for real-time agent progress visibility.

**Prerequisite:** Raise minSdk from 31 to 36.

**Files to modify:**
- `androidApp/build.gradle.kts` — change `minSdk = 31` to `minSdk = 36`
- `androidApp/src/main/AndroidManifest.xml` — ensure `POST_PROMOTED_NOTIFICATIONS` permission
- `androidApp/src/main/java/com/mocca/app/service/ActiveSessionService.kt` — implement Live Updates for agent progress
- `androidApp/src/main/java/com/mocca/app/service/NotificationManager.kt` (new) — centralized notification management

**Live Update event mapping:**

| OpenCode Event | Notification Behavior |
|----------------|----------------------|
| `session.status` (busy) | Live Update: "Agent working..." with current todo item |
| `todo.updated` | Live Update: update progress checklist in notification |
| `step-start` | Live Update: "Step N started" |
| `step-finish` | Live Update: "Step N completed" with cost/tokens |
| `session.idle` | Standard notification: "Agent finished" with summary |
| `session.error` | Standard notification: "Agent error" with error message |
| `permission.asked` (or `permission.updated`) | Standard notification: "Permission required" with inline reply |
| `question.asked` | Standard notification: "Question from agent" with inline reply |
| `message.part.updated` | Silent (streaming text, no notification) |
| `file.edited` | Silent (too frequent) |
| `session.created` | Silent |
| `session.updated` | Silent |
| `session.diff` | Silent |
| `session.compacted` | Silent |
| `installation.update-available` | Standard notification: "OpenCode update available" |
| `vcs.branch.updated` | Silent |
| `mcp.tools.changed` | Silent |
| All other events | Silent |

**Acceptance criteria:**
- Live Updates show real-time agent progress on Android 16+ devices
- Todo items update in real-time in the notification
- Step progress shows in notification
- Foreground service maintains connection when app is backgrounded
- Notification correctly clears when session goes idle and user opens the session

### B2: Inline Notification Replies (Permissions + Questions)
**Scope:** Allow users to respond to permissions and questions directly from the notification without opening the app.

**Files to modify/create:**
- `androidApp/src/main/java/com/mocca/app/service/PermissionActionReceiver.kt` — handle inline permission replies (once/always/reject)
- `androidApp/src/main/java/com/mocca/app/service/QuestionActionReceiver.kt` (new) — handle inline question replies
- `androidApp/src/main/java/com/mocca/app/service/NotificationManager.kt` — build notifications with `RemoteInput` for inline replies

**Permission notification actions:**
- "Once" (approve just this request)
- "Always" (approve future matching requests)
- "Reject" (deny the request)
- Inline text input for custom responses (if applicable)

**Question notification actions:**
- Inline text input for the answer
- "Reject" button to reject the question

**Acceptance criteria:**
- User can approve/deny permissions from notification without opening app
- User can answer questions from notification with text input
- Replies are sent to OpenCode server via v2 API
- Notification dismisses after reply

### B3: Bidirectional Todo System
**Scope:** Display todos in chat UI, write todos via API, and surface todos in Live Updates.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — add `writeTodos()` via `POST /api/session/:id/todo`
- `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — add `Todo` model with `{id, content, status, priority}`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/SessionRepository.kt` (or new `TodoRepository.kt`) — todo CRUD
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/` — todo checklist component in chat UI
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/TodoList.kt` (new) — reusable todo list component
- `androidApp/src/main/java/com/mocca/app/service/NotificationManager.kt` — show current active todo in Live Update

**Todo model:**
```kotlin
data class Todo(
    val id: String,
    val content: String,
    val status: TodoStatus,    // pending, in_progress, completed, cancelled
    val priority: TodoPriority  // high, medium, low
)
```

**Acceptance criteria:**
- Todos display as a live-updating checklist in the chat UI
- `todo.updated` events update the checklist in real-time
- User can add/edit/complete todos from the app
- Current active todo (status=in_progress) shows in Live Update notification
- Todo priority is visually distinguished (high=red, medium=yellow, low=gray)

### B4: Question API Full Integration
**Scope:** Integrate the new Question API for creating questions and waiting for answers.

**Files to modify:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — add `createQuestion()` via `POST /api/question`, `waitForQuestionAnswer()` via `GET /api/question/:requestID/wait`, `askQuestion()` via `POST /api/question/ask`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/` — wire question API into the repository layer
- Inline notification reply integration (see B2)

**Acceptance criteria:**
- MOCCA can create question requests via the API
- MOCCA can wait for question answers with timeout
- Inline notification replies work for questions
- Questions from `question.asked` SSE events still work (backward compat)

---

## Workstream C: New Features

### C1: Session Snapshot + Revert UI
**Scope:** Render snapshot/patch parts in chat timeline and provide revert controls.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/SnapshotMarker.kt` (new) — visual snapshot marker in chat timeline
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/PatchCard.kt` (new) — file patch card with hash and file list
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/RevertDialog.kt` (new) — revert confirmation dialog with diff preview
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatContent.kt` — integrate snapshot markers and revert actions
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — ensure `revertSession()` and `unrevertSession()` use v2 endpoints

**Acceptance criteria:**
- Snapshot markers appear in chat timeline at appropriate positions
- Patch cards show affected files and hash
- "Revert to here" action on each snapshot/message
- Diff preview before reverting
- Unrevert restores session state
- Revert/unrevert works via v2 API

### C2: Agent Skills Browser
**Scope:** Browse and manage OpenCode agent skills from the app.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — add endpoint to list skills (if available via API, or via bridge)
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SkillsScreen.kt` (new) — skills browser screen
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SkillsScreenModel.kt` (new) — skills screen model
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/SkillsRepository.kt` (new) — skills data

**Features:**
- List all discovered skills (name, description, source location)
- View full SKILL.md content
- Toggle skill permissions (allow/deny/ask) — if API supports it
- Search/filter skills by name

**Acceptance criteria:**
- All skills from `.opencode/skills/`, `.claude/skills/`, `.agents/skills/` are listed
- SKILL.md content is viewable
- Skill permissions are configurable (if server supports it)

### C3: Custom Tools Inspector
**Scope:** Inspect custom tools registered with OpenCode.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/CustomToolsScreen.kt` (new) — tools inspector screen
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/CustomToolsScreenModel.kt` (new) — tools screen model
- Use existing `GET /api/experimental/tool/ids` and `GET /api/experimental/tool` endpoints

**Features:**
- List all tool IDs (built-in + custom)
- View tool JSON schema (arguments, description)
- See which tools are enabled per agent
- Search/filter tools by name

**Acceptance criteria:**
- All tools (built-in and custom) are listed
- Tool schemas are viewable
- Tool list updates when MCP servers connect/disconnect

### C4: Plugin Status Screen
**Scope:** Read-only display of loaded plugins.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/PluginStatusScreen.kt` (new) — plugin status screen
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/PluginStatusScreenModel.kt` (new) — plugin status model
- May require a new bridge method or API endpoint to list loaded plugins

**Features:**
- List loaded plugins (name, source: npm/local, load order)
- Show plugin event hooks
- Show custom tools registered by plugins
- Read-only — no management from app

**Acceptance criteria:**
- All loaded plugins are displayed
- Plugin hooks and tools are visible
- No write operations (read-only)

### C5: Structured Output Support
**Scope:** Allow users to request structured JSON output from the agent.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — add `format` field to message send body
- `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — add `StructuredOutputFormat` model
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/StructuredOutputDialog.kt` (new) — schema definition dialog
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/StructuredOutputView.kt` (new) — formatted JSON view
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatContent.kt` — integrate structured output mode

**Features:**
- "Structured Output" toggle in chat input area
- Schema templates (e.g., "Code Review", "Data Extraction", "Issue List")
- Custom JSON schema editor
- Formatted view for structured output (table, cards, tree)
- Raw JSON fallback view

**Acceptance criteria:**
- User can define a JSON schema before sending a message
- Agent returns validated JSON matching the schema
- Structured output is displayed in a formatted view
- Schema templates are available for common use cases

### C6: TUI Remote Control (Optional)
**Scope:** Optional feature for controlling OpenCode TUI from the phone.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — add TUI control endpoints (`POST /api/tui/append-prompt`, `POST /api/tui/submit-prompt`, `POST /api/tui/execute-command`, `GET /api/tui/control/next`, `POST /api/tui/control/response`)
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/RemoteControlScreen.kt` (new) — optional remote control screen
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/RemoteControlScreenModel.kt` (new) — remote control model

**Features:**
- Submit prompt to TUI from phone
- Execute commands
- View TUI control requests and respond
- Optional — disabled by default, enabled in Settings

**Acceptance criteria:**
- Prompts submitted from phone appear in TUI
- Commands execute in TUI
- Control requests are handled
- Feature is opt-in (disabled by default)

---

## Workstream D: Connection & Discovery

### D1: QR Code Pairing
**Scope:** Implement QR code-based server discovery and connection setup.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/discovery/QRPairingManager.kt` (new) — QR code parsing and connection setup
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/onboarding/QRScannerScreen.kt` (new) — camera QR scanner screen
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/onboarding/ProgressiveOnboardingScreen.kt` — add QR scan option
- `androidApp/src/main/AndroidManifest.xml` — add camera permission for QR scanning
- MOCCA CLI server (separate repo) — generate QR code in terminal on startup

**QR code payload format:**
```json
{
  "v": 2,
  "url": "http://192.168.1.100:4096",
  "bridgeUrl": "ws://192.168.1.100:4097",
  "username": "opencode",
  "password": "secret",
  "serverVersion": "1.17.11"
}
```

**Acceptance criteria:**
- MOCCA CLI server displays QR code in terminal on startup
- Phone camera scans QR code
- Connection is automatically configured from QR payload
- Auth credentials are stored securely
- Manual IP:port entry remains as fallback

### D2: Enhanced Connection Reliability
**Scope:** Make the phone-to-PC connection robust against network instability.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ConnectionManager.kt` — enhance with SSE buffering, version detection, quality indicator
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/SSEEventBuffer.kt` (new) — buffer SSE events during disconnection
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ConnectionQualityMonitor.kt` (new) — connection quality measurement
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/ConnectionQualityIndicator.kt` (new) — UI indicator

**Features:**
- SSE event buffering: events received during brief disconnections are replayed on reconnect (using server-side event ID or timestamp)
- Server version detection: `/api/global/health` returns version, MOCCA adapts behavior
- Connection quality indicator: excellent (<50ms), good (<200ms), poor (>200ms) based on ping latency
- Network state awareness: pause SSE on WiFi loss, resume on reconnect, re-sync missed events
- Foreground service: maintains connection when app is backgrounded

**Acceptance criteria:**
- Brief disconnections (5-30s) don't lose events
- Connection quality is visible in UI
- App correctly handles WiFi switches
- Foreground service keeps connection alive in background
- Server version is detected and logged on connect

### D3: mDNS Discovery (Optional Enhancement)
**Scope:** Optional mDNS auto-discovery as a complement to QR pairing.

**Note:** This is a secondary discovery method. QR pairing is primary.

**Files to modify/create:**
- `composeApp/src/commonMain/kotlin/com/mocca/app/discovery/MdnsDiscoveryService.kt` (new) — mDNS service discovery
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/onboarding/ServerDiscoveryScreen.kt` (new) — discovered servers list

**Acceptance criteria:**
- App discovers `opencode serve --mdns` instances on local network
- Discovered servers are listed in onboarding
- User can select a discovered server and connect
- Falls back to QR pairing / manual entry if no servers found

---

## Workstream E: Android Platform Integration

### E1: Home Screen Widget
**Scope:** Show active session status on the Android home screen.

**Files to modify/create:**
- `androidApp/src/main/java/com/mocca/app/widget/SessionStatusWidget.kt` (new) — home screen widget
- `androidApp/src/main/java/com/mocca/app/widget/SessionStatusWidgetReceiver.kt` (new) — widget receiver
- `androidApp/src/main/res/xml/session_status_widget_info.xml` (new) — widget metadata
- `androidApp/src/main/res/layout/session_status_widget.xml` (new) — widget layout

**Features:**
- Shows current active session title and status (idle/busy/error)
- Shows current todo item (if any)
- Quick action: open session in app
- Updates via foreground service push

**Acceptance criteria:**
- Widget appears on home screen
- Widget updates in real-time when session status changes
- Tapping widget opens the session in the app

### E2: Share Sheet Integration
**Scope:** Allow sharing text/code from other apps to MOCCA as a prompt.

**Files to modify:**
- `androidApp/src/main/AndroidManifest.xml` — add intent filter for `android.intent.action.SEND`
- `androidApp/src/main/java/com/mocca/app/MainActivity.kt` — handle shared text
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatScreenModel.kt` — accept shared text as prompt prefill

**Acceptance criteria:**
- User can share text from any app to MOCCA
- Shared text appears as a pre-filled prompt in the active session
- User can edit the prompt before sending

### E3: Foreground Service Enhancement
**Scope:** Enhance the existing foreground service for persistent connection and notifications.

**Files to modify:**
- `androidApp/src/main/java/com/mocca/app/service/ActiveSessionService.kt` — enhance to manage SSE connection, Live Updates, and notification routing
- `androidApp/src/main/AndroidManifest.xml` — ensure service declarations are correct

**Acceptance criteria:**
- Service maintains SSE connection when app is backgrounded
- Service routes events to Live Updates and notifications
- Service correctly stops when no active sessions exist
- Service restarts on device boot if user has enabled auto-start

---

## Dependency Graph

```
A1 (V2 API) ──┬── A2 (Streaming) ── A4 (Part Types)
              ├── A3 (Permissions) ── B2 (Inline Replies)
              ├── B1 (Live Updates) ── B3 (Todo) ── B4 (Question API)
              ├── C1 (Snapshots) ── A4 (Part Types)
              ├── C2 (Skills) ── C3 (Custom Tools) ── C4 (Plugins)
              ├── C5 (Structured Output)
              ├── C6 (TUI Control)
              ├── D1 (QR Pairing)
              └── D2 (Connection) ─── E3 (Foreground Service) ── E1 (Widget)

E2 (Share Sheet) — independent
D3 (mDNS) — independent, optional
```

## Open Questions for Future Resolution

1. **SSE event buffering mechanism**: OpenCode server doesn't currently support `Last-Event-ID` header for SSE resumption. May need to use `GET /api/session/:id/message` to re-fetch messages after reconnection. Need to verify if v2 API supports event IDs.

2. **Skills listing API**: OpenCode doesn't have a dedicated `/api/skills` endpoint. Skills are discovered by the `skill` tool at runtime. May need a bridge method to list skills, or parse the skill tool's description to extract available skills.

3. **Plugin listing API**: Similarly, no dedicated `/api/plugins` endpoint exists. May need a bridge method or parse server config to list loaded plugins.

4. **MOCCA CLI QR generation**: The MOCCA CLI server (separate repo) needs to generate and display QR codes in the terminal. Need to determine which QR library to use and the exact terminal display format.

5. **Live Updates API specifics**: Need to verify the exact Android 17 Live Updates API surface, including `ProgressStyle`, `setOngoing(true)`, and promoted notification requirements. The `compose-material3-expressive` library may provide abstractions.

6. **V2 API endpoint specifics**: The exact v2 endpoint paths and parameter structures need to be verified from the OpenAPI spec at `http://<server>/api/doc` or the v2 spec at `specs/v2/api.html` in the OpenCode repo.
