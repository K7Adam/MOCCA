# MOCCA ↔ OpenCode Feature Parity Plan

> **Generated**: 2026-02-27
> **Goal**: Achieve 100% feature parity between MOCCA Android app and OpenCode Web/Desktop client
> **Scope**: All OpenCode server endpoints, SSE events, and web UI features — including experimental

---

## Table of Contents

1. [Gap Analysis: API Endpoints](#1-gap-analysis-api-endpoints)
2. [Gap Analysis: SSE Events](#2-gap-analysis-sse-events)
3. [Gap Analysis: UI Features & Screens](#3-gap-analysis-ui-features--screens)
4. [Gap Analysis: Domain Models](#4-gap-analysis-domain-models)
5. [Implementation Plan](#5-implementation-plan)
6. [Wave Execution Order](#6-wave-execution-order)
7. [File Change Manifest](#7-file-change-manifest)

---

## 1. Gap Analysis: API Endpoints

### Legend
- ✅ Implemented and working
- ⚠️ Partial implementation (missing fields, incomplete)
- ❌ Not implemented
- 🚫 Not applicable for mobile

### 1.1 Global Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/global/health` | GET | ✅ | `getHealthCheck()` |
| `/global/event` | GET (SSE) | ✅ | `MoccaSseClient` global stream |
| `/global/config` | GET | ❌ | Global config separate from instance config |
| `/global/config` | PATCH | ❌ | Global config update |
| `/global/dispose` | POST | ❌ | Dispose all instances globally |

### 1.2 Instance / Root Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/doc` | GET | ❌ | OpenAPI spec — low priority for mobile |
| `/path` | GET | ✅ | `getCurrentPath()` |
| `/vcs` | GET | ✅ | `getVcsInfo()` |
| `/command` | GET | ✅ | `getCommands()` |
| `/agent` | GET | ✅ | `getAgents()` |
| `/skill` | GET | ❌ | **NEW** — List available skills |
| `/lsp` | GET | ✅ | `getLspStatus()` |
| `/formatter` | GET | ✅ | `getFormatters()` |
| `/log` | POST | ✅ | `writeLog()` |
| `/instance/dispose` | POST | ✅ | `disposeInstance()` |
| `/auth/:providerID` | PUT | ✅ | `setProviderAuth()` |
| `/auth/:providerID` | DELETE | ❌ | **NEW** — Remove provider auth |
| `/event` | GET (SSE) | ✅ | `MoccaSseClient` instance stream |

### 1.3 Session Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/session/` | GET | ✅ | `listSessions()` |
| `/session/status` | GET | ✅ | `getSessionStatus()` |
| `/session/:id` | GET | ✅ | `getSession()` |
| `/session/:id/children` | GET | ✅ | `getSessionChildren()` |
| `/session/:id/todo` | GET | ✅ | `getSessionTodos()` |
| `/session/:id/diff` | GET | ✅ | `getSessionDiff()` |
| `/session/:id/message` | GET | ✅ | `getMessages()` |
| `/session/:id/message/:messageID` | GET | ✅ | `getMessage()` |
| `/session/` | POST | ✅ | `createSession()` |
| `/session/:id/init` | POST | ✅ | `initSession()` |
| `/session/:id/fork` | POST | ✅ | `forkSession()` |
| `/session/:id/abort` | POST | ✅ | `abortSession()` |
| `/session/:id/share` | POST | ✅ | `shareSession()` |
| `/session/:id/summarize` | POST | ✅ | `summarizeSession()` |
| `/session/:id/message` | POST | ✅ | `sendMessage()` |
| `/session/:id/prompt_async` | POST | ✅ | `promptAsync()` |
| `/session/:id/command` | POST | ✅ | `executeCommand()` |
| `/session/:id/shell` | POST | ✅ | `executeShell()` |
| `/session/:id/revert` | POST | ✅ | `revertSession()` |
| `/session/:id/unrevert` | POST | ✅ | `unrevertSession()` |
| `/session/:id/permissions/:permissionID` | POST | ✅ | `replyToPermission()` (deprecated) |
| `/session/:id` | PATCH | ✅ | `updateSession()` |
| `/session/:id` | DELETE | ✅ | `deleteSession()` |
| `/session/:id/share` | DELETE | ✅ | `unshareSession()` |
| `/session/:id/message/:messageID` | DELETE | ❌ | **NEW** — Delete specific message |
| `/session/:id/message/:messageID/part/:partID` | DELETE | ❌ | **NEW** — Delete message part |
| `/session/:id/message/:messageID/part/:partID` | PATCH | ❌ | **NEW** — Update message part |

### 1.4 Project Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/project/` | GET | ✅ | `listProjects()` |
| `/project/current` | GET | ✅ | `getCurrentProject()` |
| `/project/:projectID` | PATCH | ❌ | **NEW** — Update project settings |

### 1.5 File & Search Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/find` | GET | ✅ | `findText()` |
| `/find/file` | GET | ✅ | `findFile()` |
| `/find/symbol` | GET | ✅ | `findSymbol()` |
| `/file` | GET | ✅ | `listFiles()` |
| `/file/content` | GET | ✅ | `getFileContent()` |
| `/file/status` | GET | ✅ | `getFileStatus()` |

### 1.6 PTY / Terminal Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/pty/` | GET | ✅ | `listTerminals()` (uses `/terminal/`) |
| `/pty/:ptyID` | GET | ✅ | `getTerminal()` |
| `/pty/:ptyID/connect` | WS | ✅ | `connectTerminalSocket()` |
| `/pty/` | POST | ✅ | `createTerminal()` |
| `/pty/:ptyID` | PUT | ❌ | **NEW** — Update terminal (resize, etc.) |
| `/pty/:ptyID` | DELETE | ❌ | **NEW** — Delete/close terminal |

### 1.7 Config Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/config/` | GET | ✅ | `getConfig()` |
| `/config/providers` | GET | ✅ | `getConfigProviders()` |
| `/config/` | PATCH | ✅ | `patchConfig()` |

### 1.8 Provider Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/provider/` | GET | ✅ | `getProviders()` |
| `/provider/auth` | GET | ✅ | `getProviderAuth()` |
| `/provider/:id/oauth/authorize` | POST | ✅ | `authorizeOAuth()` |
| `/provider/:id/oauth/callback` | POST | ✅ | `oauthCallback()` |

### 1.9 Permission Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/permission/` | GET | ✅ | `getPermissions()` |
| `/permission/:requestID/reply` | POST | ✅ | `replyToPermissionRequest()` |

### 1.10 Question Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/question/` | GET | ✅ | `getQuestions()` |
| `/question/:requestID/reply` | POST | ✅ | `replyToQuestion()` |
| `/question/:requestID/reject` | POST | ✅ | `rejectQuestion()` |

### 1.11 MCP Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/mcp/` | GET | ✅ | `getMcpServers()` |
| `/mcp/` | POST | ✅ | `addMcpServer()` |
| `/mcp/:name/connect` | POST | ✅ | `connectMcpServer()` |
| `/mcp/:name/disconnect` | POST | ✅ | `disconnectMcpServer()` |
| `/mcp/:name/auth` | POST | ❌ | **NEW** — Start MCP OAuth flow |
| `/mcp/:name/auth/callback` | POST | ❌ | **NEW** — MCP OAuth callback |
| `/mcp/:name/auth/authenticate` | POST | ❌ | **NEW** — MCP OAuth authenticate |
| `/mcp/:name/auth` | DELETE | ❌ | **NEW** — Remove MCP auth |

### 1.12 Experimental Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/experimental/tool/ids` | GET | ✅ | `getToolIds()` |
| `/experimental/tool` | GET | ✅ | `getTools()` |
| `/experimental/worktree` | GET | ❌ | **NEW** — List worktrees |
| `/experimental/worktree` | POST | ❌ | **NEW** — Create worktree |
| `/experimental/worktree/reset` | POST | ❌ | **NEW** — Reset worktree |
| `/experimental/worktree` | DELETE | ❌ | **NEW** — Delete worktree |
| `/experimental/session` | GET | ❌ | **NEW** — Cross-project session listing |
| `/experimental/resource` | GET | ❌ | **NEW** — List MCP resources |

### 1.13 TUI Routes

| Endpoint | Method | MOCCA Status | Notes |
|----------|--------|-------------|-------|
| `/tui/*` (13 routes) | Various | 🚫 | Not applicable — TUI-specific control |

### API Gap Summary

| Category | Total Routes | ✅ Implemented | ❌ Missing | 🚫 N/A |
|----------|-------------|---------------|-----------|---------|
| Global | 5 | 2 | 3 | 0 |
| Root/Instance | 13 | 10 | 2 | 1 |
| Session | 27 | 24 | 3 | 0 |
| Project | 3 | 2 | 1 | 0 |
| File/Search | 6 | 6 | 0 | 0 |
| PTY/Terminal | 5 | 3 | 2 | 0 |
| Config | 3 | 3 | 0 | 0 |
| Provider | 4 | 4 | 0 | 0 |
| Permission | 2 | 2 | 0 | 0 |
| Question | 3 | 3 | 0 | 0 |
| MCP | 8 | 4 | 4 | 0 |
| Experimental | 8 | 2 | 6 | 0 |
| TUI | 13 | 0 | 0 | 13 |
| **TOTAL** | **100** | **65** | **21** | **14** |

**Coverage: 65/86 applicable routes = 75.6%** → Target: 86/86 = 100%

---

## 2. Gap Analysis: SSE Events

### 2.1 Currently Handled Events (21)

| Event Type | MOCCA Handler | Status |
|------------|--------------|--------|
| `server.connected` | ✅ | Marks connected |
| `server.heartbeat` | ✅ | Keeps alive |
| `session.updated` | ✅ | Updates session cache |
| `session.deleted` | ✅ | Removes from cache |
| `session.idle` | ✅ | Updates status |
| `session.error` | ✅ | Shows error |
| `message.updated` | ✅ | Updates message in store |
| `message.removed` | ✅ | Removes message from store |
| `message.part.updated` | ✅ | Updates message part |
| `message.part.removed` | ✅ | Removes message part |
| `permission.updated` | ✅ | Legacy handler |
| `permission.asked` | ✅ | Shows permission dialog |
| `permission.replied` | ✅ | Dismisses dialog |
| `question.asked` | ✅ | Shows question dialog |
| `question.replied` | ✅ | Dismisses dialog |
| `file.edited` | ✅ | Refreshes file tree |
| `file.watcher.updated` | ✅ | Updates watcher |
| `installation.updated` | ✅ | Handles install state |
| `lsp.client.diagnostics` | ✅ | Updates diagnostics |
| `log` | ✅ | Writes to log |
| `agent.status` | ✅ | Updates agent state |

### 2.2 Missing Events (~19)

| Event Type | Priority | Description | Impact |
|------------|----------|-------------|--------|
| `session.created` | P0 | New session created (e.g., by another client) | Multi-client sync |
| `session.status` | P0 | Session status transitions (running→idle→error) | Real-time status |
| `session.diff` | P1 | Session diff data changed | Diff viewer updates |
| `session.compacted` | P2 | Session was summarized/compacted | UI notification |
| `todo.updated` | P0 | Todo item added/changed/completed | Live todo tracking |
| `question.rejected` | P1 | Question was rejected | UI cleanup |
| `message.part.delta` | P0 | Incremental text streaming delta | **Critical for streaming UX** |
| `pty.created` | P1 | Terminal created | Terminal tab management |
| `pty.updated` | P1 | Terminal state changed | Terminal UI sync |
| `pty.exited` | P1 | Terminal process exited | Terminal cleanup |
| `pty.deleted` | P1 | Terminal removed | Terminal tab removal |
| `project.updated` | P1 | Project settings changed | Project state sync |
| `vcs.branch.updated` | P1 | Git branch changed | Branch indicator |
| `file.updated` | P1 | File content changed (watcher) | File tree refresh |
| `lsp.updated` | P2 | LSP server status changed | Status indicator |
| `command.executed` | P2 | A command was executed | Notification |
| `mcp.tools.changed` | P1 | MCP tools list changed | MCP UI refresh |
| `mcp.browser.open.failed` | P2 | MCP browser auth failed | Error handling |
| `worktree.ready` | P2 | Worktree created/ready | Worktree UI |
| `worktree.failed` | P2 | Worktree creation failed | Error handling |
| `installation.update.available` | P2 | App update available | Update notification |
| `server.instance.disposed` | P1 | Instance disposed | Reconnection flow |
| `global.disposed` | P1 | Global server disposed | Connection lost |

### SSE Gap Summary

| | Count |
|---|---|
| Currently handled | 21 |
| Missing (P0 critical) | 4 |
| Missing (P1 important) | 11 |
| Missing (P2 nice-to-have) | 7 |
| **Total missing** | **~22** |
| **Target** | **43 events** |

---

## 3. Gap Analysis: UI Features & Screens

### 3.1 Chat & Messaging

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| Markdown rendering | marked + DOMPurify + morphdom | ✅ Basic markdown | ⚠️ Need DOM-diffing equivalent for smooth updates |
| Typewriter streaming | 30-250ms variable speed, blinking cursor | ❌ | Full implementation needed |
| `message.part.delta` streaming | Real-time character-by-character | ❌ | Requires SSE event + UI |
| Code blocks + syntax highlighting | highlight.js + copy button | ✅ Basic | ⚠️ Verify copy button, language detection |
| Tool call rendering (PART_MAPPING) | Registry pattern, per-tool components | ✅ `ToolCards.kt` | ⚠️ Verify all tool types covered |
| ContextToolGroup | Grouped context tools, collapsible | ❌ | New component needed |
| TextShimmer loading animation | Animated shimmer on pending text | ❌ | New animation component |
| ImagePreview modal | Full-screen + keyboard dismiss | ❌ | New component needed |
| ReasoningPart display | Throttled, cached, collapsible | ⚠️ Partial | Verify collapsible + caching |
| Error cards | Styled error display | ✅ | Verify styling match |
| `@` mentions (file/agent) | Fuzzy search popover | ❌ | **Major feature** — new component |
| `/` slash commands | Autocomplete popover, fuzzy search, badges | ❌ | **Major feature** — new component |
| Prompt history cycling | ArrowUp/ArrowDown through history | ❌ | New feature in ChatInputBar |
| Shell mode (`!` prefix) | Toggle shell mode | ⚠️ Partial | `executeShell()` exists, UI toggle missing |
| Image attachments | Drag & drop, paste, preview | ❌ | New feature — camera + gallery picker |
| Highlighted file references | Clickable file paths in messages | ❌ | New rendering logic |

### 3.2 Session Management

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| Session list (grouped by date) | Date-grouped timeline | ✅ | Verify grouping |
| Fork dialog | Searchable message list, preview, timestamps | ⚠️ Partial | Need searchable selector UI |
| Share dialog | URL copy + toast | ✅ | Verify UX |
| Revert/unrevert | Visual markers + undo | ✅ | Verify visual markers |
| Session status indicators | Running, idle, error, aborting | ✅ | Verify all states |
| Auto-summarize | Automatic on threshold | ✅ | — |
| "Open in IDE" button | VS Code, Cursor, Zed deep links | ❌ | Low priority for mobile |
| Session delete message | Delete individual messages | ❌ | Needs API + UI |
| Session edit message part | Edit/delete parts | ❌ | Needs API + UI |

### 3.3 Navigation & Layout

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| Sidebar with projects | Project list + session timeline | ⚠️ | MOCCA uses bottom nav instead — different paradigm |
| Command palette (Ctrl+K) | Global search + actions | ❌ | **Major feature** — Android search/action overlay |
| Tab-based multi-panel | Files, terminals, review tabs | ⚠️ | MOCCA has separate screens, not tabs |
| Mobile responsive layout | 768px breakpoint adaptation | ✅ | MOCCA is mobile-native |
| Mobile tabs | Tab bar for mobile | ✅ | Bottom navigation |

### 3.4 Settings & Configuration

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| Model selector | Search, provider grouping, tags (Free/Latest) | ✅ `ModelSelectorDialog` | ⚠️ Verify tags/search |
| Provider connection | OAuth dialog | ✅ | — |
| Theme switching | OpenCode, Zed, Dark, Light | ❌ | MOCCA is OLED-only by design (intentional) |
| Keybind customization | Custom keyboard shortcuts | 🚫 | Not applicable for mobile |
| Permission auto-accept settings | TTL-based auto-respond | ⚠️ | Verify full implementation |
| Global config management | GET/PATCH /global/config | ❌ | Needs API + Settings UI |

### 3.5 File Management

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| File tree with Git indicators | Status icons + language icons | ✅ `FilesScreen` | ⚠️ Verify language icons |
| Tabbed editor (multi-file) | Multiple open files in tabs | ❌ | Not core for mobile viewer |
| Diff viewer (unified/split) | @pierre/diffs + virtualized | ⚠️ `GitDiffScreen` | Verify unified/split toggle |
| DiffChanges bar visualization | Visual bar showing change distribution | ❌ | New component |
| File content viewer | Syntax-highlighted read-only | ✅ | — |

### 3.6 Git / VCS

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| Visual diffs | Side-by-side or unified | ⚠️ | Verify both modes |
| Worktree management | Create, reset, delete worktrees | ❌ | **Major feature** — full UI + API |
| Branch tracking | Per-workspace branch display | ✅ `GitScreen` | Verify |

### 3.7 MCP (Model Context Protocol)

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| Server management | Add/remove/configure servers | ✅ `McpScreen` | — |
| Status indicators | Connected, failed, disabled, needs auth | ✅ | Verify all states |
| OAuth flow for MCP servers | Browser-based OAuth | ❌ | **Major feature** — needs all 4 auth endpoints |
| Tool listing | Display available tools | ✅ | — |
| Resource browsing | List and view MCP resources | ❌ | Needs API + UI |

### 3.8 Terminal

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| PTY with WebSocket | Real-time terminal | ✅ | — |
| Multiple terminal tabs | Concurrent terminals | ❌ | Tab management UI |
| Terminal lifecycle (update/delete) | Resize, close terminals | ❌ | Needs API + UI |
| ANSI rendering | Full ANSI escape code support | ✅ `AnsiParser` | Verify completeness |

### 3.9 Permission & Question Systems

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| Permission dock | Inline approval UI | ✅ `PermissionBanner` | Verify UX parity |
| Allow once/always/deny | Three-option response | ✅ | — |
| Auto-respond with TTL | Configurable auto-accept | ⚠️ | Verify TTL cleanup |
| Question dock | AI question UI | ✅ `QuestionDialog` | — |
| Multi-step Q&A | Sequential questions | ✅ | Verify |

### 3.10 Experimental Features

| Feature | OpenCode Web | MOCCA | Gap |
|---------|-------------|-------|-----|
| Plan Mode | Multi-step architectural planning | ❌ | New screen/mode |
| Worktrees | Task isolation via git worktrees | ❌ | Full implementation needed |
| Cross-project sessions | Browse sessions across projects | ❌ | Needs API + UI |
| MCP resources | Browse MCP server resources | ❌ | Needs API + UI |
| Feature flags | OPENCODE_EXPERIMENTAL_* | ❌ | Config-based feature flags |
| Skills listing | GET /skill | ❌ | Needs API + UI |

### UI Gap Summary

| Category | Features | ✅ Done | ⚠️ Partial | ❌ Missing | 🚫 N/A |
|----------|----------|---------|-----------|-----------|---------|
| Chat & Messaging | 16 | 3 | 4 | 9 | 0 |
| Session Management | 9 | 5 | 1 | 3 | 0 |
| Navigation/Layout | 5 | 2 | 1 | 2 | 0 |
| Settings/Config | 6 | 2 | 1 | 1 | 2 |
| File Management | 5 | 2 | 1 | 2 | 0 |
| Git/VCS | 3 | 1 | 1 | 1 | 0 |
| MCP | 5 | 3 | 0 | 2 | 0 |
| Terminal | 4 | 2 | 0 | 2 | 0 |
| Permissions/Questions | 5 | 4 | 1 | 0 | 0 |
| Experimental | 6 | 0 | 0 | 6 | 0 |
| **TOTAL** | **64** | **24** | **10** | **28** | **2** |

---

## 4. Gap Analysis: Domain Models

Models that need to be created or extended to support missing features:

| Model | File | Status | Changes Needed |
|-------|------|--------|----------------|
| `SkillInfo` | `domain/model/AgentTypes.kt` | ❌ New | Data class for GET /skill response |
| `WorktreeInfo` | `domain/model/WorktreeTypes.kt` | ❌ New | Data class for worktree CRUD |
| `McpAuthState` | `domain/model/McpModels.kt` | ❌ Extend | OAuth state for MCP servers |
| `McpResource` | `domain/model/McpModels.kt` | ❌ Extend | MCP resource data model |
| `MessagePartDelta` | `domain/model/ServerEvents.kt` | ❌ Extend | Delta streaming event model |
| `GlobalConfig` | `domain/model/Config.kt` | ❌ Extend | Global vs instance config distinction |
| `PtyUpdateRequest` | `domain/model/Models.kt` | ❌ New | Terminal update request body |
| `ProjectUpdateRequest` | `domain/model/Models.kt` | ❌ New | Project PATCH request body |
| `CrossProjectSession` | `domain/model/Models.kt` | ❌ New | Experimental cross-project session |
| `FeatureFlags` | `domain/model/Config.kt` | ❌ New | Feature flag configuration |
| Server event types | `domain/model/ServerEvents.kt` | ❌ Extend | ~22 new SSE event data classes |

---

## 5. Implementation Plan

### Priority Definitions

- **P0 (Critical)**: Core functionality gaps that break user experience or block other features
- **P1 (Important)**: Significant features expected by users, improves UX substantially
- **P2 (Standard)**: Feature completeness items, nice-to-have improvements
- **P3 (Low)**: Experimental features, edge cases, polish items

### Complexity Estimates

- **XS**: < 1 hour — Single function/model addition
- **S**: 1-3 hours — Small component or simple feature
- **M**: 3-8 hours — Full feature with API + UI + state
- **L**: 8-16 hours — Major feature requiring multiple files
- **XL**: 16+ hours — Large feature system with significant architecture

---

### Wave 0: Foundation (Models + API Layer)
> **Goal**: Add all missing API endpoints and domain models. No UI changes.
> **Dependency**: None — this is the base layer everything else builds on.

#### W0-T1: New Domain Models
- **Priority**: P0 | **Complexity**: S
- **Files to create/modify**:
  - `domain/model/SkillTypes.kt` — NEW: `SkillInfo`, `SkillListResponse`
  - `domain/model/WorktreeTypes.kt` — NEW: `WorktreeInfo`, `WorktreeCreateRequest`, `WorktreeListResponse`
  - `domain/model/McpModels.kt` — EXTEND: `McpAuthRequest`, `McpAuthCallbackRequest`, `McpResource`, `McpResourceListResponse`
  - `domain/model/Config.kt` — EXTEND: `GlobalConfig`, `FeatureFlags`
  - `domain/model/Models.kt` — EXTEND: `PtyUpdateRequest`, `ProjectUpdateRequest`, `CrossProjectSession`
  - `domain/model/ServerEvents.kt` — EXTEND: all 22 new SSE event data classes (see §2.2)

#### W0-T2: Missing API Endpoints in MoccaApiClient
- **Priority**: P0 | **Complexity**: M
- **File**: `api/MoccaApiClient.kt`
- **Add functions**:
  ```
  // Global
  suspend fun getGlobalConfig(): Result<GlobalConfig>
  suspend fun patchGlobalConfig(config: GlobalConfig): Result<GlobalConfig>
  suspend fun disposeGlobal(): Result<Unit>

  // Skills
  suspend fun getSkills(): Result<List<SkillInfo>>

  // Auth
  suspend fun removeProviderAuth(providerID: String): Result<Unit>

  // Session message/part management
  suspend fun deleteMessage(sessionId: String, messageId: String): Result<Unit>
  suspend fun deleteMessagePart(sessionId: String, messageId: String, partId: String): Result<Unit>
  suspend fun updateMessagePart(sessionId: String, messageId: String, partId: String, content: Any): Result<Unit>

  // Project
  suspend fun updateProject(projectId: String, update: ProjectUpdateRequest): Result<Project>

  // PTY lifecycle
  suspend fun updateTerminal(ptyId: String, update: PtyUpdateRequest): Result<Terminal>
  suspend fun deleteTerminal(ptyId: String): Result<Unit>

  // MCP OAuth
  suspend fun startMcpAuth(name: String, request: McpAuthRequest): Result<McpAuthResponse>
  suspend fun mcpAuthCallback(name: String, callback: McpAuthCallbackRequest): Result<Unit>
  suspend fun mcpAuthenticate(name: String): Result<Unit>
  suspend fun removeMcpAuth(name: String): Result<Unit>

  // Experimental
  suspend fun listWorktrees(): Result<List<WorktreeInfo>>
  suspend fun createWorktree(request: WorktreeCreateRequest): Result<WorktreeInfo>
  suspend fun resetWorktree(): Result<Unit>
  suspend fun deleteWorktree(): Result<Unit>
  suspend fun listCrossProjectSessions(): Result<List<CrossProjectSession>>
  suspend fun listMcpResources(): Result<List<McpResource>>
  ```

#### W0-T3: Missing SSE Event Handlers
- **Priority**: P0 | **Complexity**: M
- **Files**:
  - `api/MoccaSseClient.kt` — Add 22 new event type handlers in the `when` block
  - `domain/model/ServerEvents.kt` — Already extended in W0-T1
- **Events to add**: All from §2.2

---

### Wave 1: Core UX (Streaming + Chat Enhancements)
> **Goal**: Implement the most impactful user-facing features.
> **Dependency**: Wave 0 (models + API)

#### W1-T1: message.part.delta Streaming
- **Priority**: P0 | **Complexity**: L
- **Description**: Real-time character-by-character message streaming using `message.part.delta` SSE events, replacing the current "update whole message" approach.
- **Files**:
  - `api/MoccaSseClient.kt` — Handle `message.part.delta` event
  - `data/repository/ChatStateStore.kt` — Add delta accumulation logic
  - `ui/screens/chat/ChatContent.kt` — Render streaming deltas incrementally
  - `ui/components/chat/StreamingTextComponent.kt` — NEW: Composable for animated text streaming

#### W1-T2: Typewriter Streaming Effect
- **Priority**: P1 | **Complexity**: M
- **Description**: Animated typewriter effect for streaming messages (30-250ms variable speed, blinking cursor) matching OpenCode Web's `typewriter.tsx`.
- **Files**:
  - `ui/components/chat/TypewriterText.kt` — NEW: Typewriter animation composable
  - `ui/components/chat/StreamingCursor.kt` — NEW: Blinking cursor composable
  - `ui/screens/chat/ChatContent.kt` — Integrate typewriter into message rendering

#### W1-T3: TextShimmer Loading Animation
- **Priority**: P1 | **Complexity**: S
- **Description**: Shimmer animation for "thinking" / loading states, matching `text-shimmer.tsx`.
- **Files**:
  - `ui/components/chat/TextShimmer.kt` — NEW: Shimmer animation composable
  - `ui/screens/chat/ChatContent.kt` — Use shimmer for pending messages

#### W1-T4: @ Mentions (File/Agent Autocomplete)
- **Priority**: P1 | **Complexity**: L
- **Description**: Type `@` in the prompt to fuzzy-search files and agents. Popover with results, keyboard navigation (or touch selection on mobile).
- **Files**:
  - `ui/components/navigation/MentionPopover.kt` — NEW: Autocomplete popover
  - `ui/components/navigation/ChatInputBar.kt` — MODIFY: Detect `@` trigger, show popover
  - `ui/components/navigation/ChatInputContent.kt` — MODIFY: Handle mention insertion
  - `data/repository/FileRepository.kt` — Reuse for file listing
  - `data/repository/AgentRepository.kt` — Reuse for agent listing

#### W1-T5: Slash Command Autocomplete
- **Priority**: P1 | **Complexity**: L
- **Description**: Type `/` at start of prompt to see available commands with fuzzy search, badges (builtin vs custom), and descriptions.
- **Files**:
  - `ui/components/navigation/CommandPopover.kt` — NEW: Slash command popover
  - `ui/components/navigation/ChatInputBar.kt` — MODIFY: Detect `/` trigger
  - `data/repository/CommandRepository.kt` — Reuse for command listing

#### W1-T6: Prompt History
- **Priority**: P1 | **Complexity**: S
- **Description**: Cycle through previous prompts using swipe gesture or button (mobile equivalent of ArrowUp/ArrowDown).
- **Files**:
  - `data/repository/ChatStateStore.kt` — MODIFY: Store prompt history (in-memory ring buffer or SQLDelight)
  - `ui/components/navigation/ChatInputBar.kt` — MODIFY: Add history cycling gesture/button

#### W1-T7: Shell Mode UI Toggle
- **Priority**: P2 | **Complexity**: XS
- **Description**: Visual toggle to prefix prompt with `!` for shell execution mode. API (`executeShell`) already exists.
- **Files**:
  - `ui/components/navigation/ChatInputBar.kt` — MODIFY: Add shell mode toggle button
  - `ui/components/navigation/ChatInputContent.kt` — MODIFY: Handle `!` prefix

---

### Wave 2: Session & Message Management
> **Goal**: Complete session management feature set.
> **Dependency**: Wave 0 (API endpoints)

#### W2-T1: Delete Message / Message Part
- **Priority**: P1 | **Complexity**: S
- **Description**: Allow users to delete individual messages or message parts from a session.
- **Files**:
  - `ui/screens/chat/ChatContent.kt` — MODIFY: Add delete action (long-press menu or swipe)
  - `data/repository/ChatStateStore.kt` — MODIFY: Handle optimistic deletion
  - API already added in W0-T2

#### W2-T2: Edit Message Part
- **Priority**: P2 | **Complexity**: M
- **Description**: Allow editing individual message parts (PATCH endpoint).
- **Files**:
  - `ui/screens/chat/ChatContent.kt` — MODIFY: Add edit action
  - `ui/components/chat/EditMessagePartDialog.kt` — NEW: Edit dialog
  - `data/repository/ChatStateStore.kt` — MODIFY: Handle optimistic update
  - API already added in W0-T2

#### W2-T3: Enhanced Fork Dialog
- **Priority**: P1 | **Complexity**: M
- **Description**: Fork dialog with searchable message list (choose fork point), message preview, timestamps.
- **Files**:
  - `ui/components/chat/ForkSessionDialog.kt` — NEW (or modify existing fork UI)
  - `ui/screens/sessions/SessionsScreenModel.kt` — MODIFY: Fork with message selection

#### W2-T4: ContextToolGroup Component
- **Priority**: P1 | **Complexity**: M
- **Description**: Group related context/tool calls into a collapsible section with summary header.
- **Files**:
  - `ui/components/chat/ContextToolGroup.kt` — NEW: Collapsible tool group component
  - `ui/components/ToolCards.kt` — MODIFY: Detect groupable tools and render via ContextToolGroup

#### W2-T5: Image Attachments in Chat
- **Priority**: P2 | **Complexity**: M
- **Description**: Attach images to chat messages. On mobile: camera capture, gallery picker, preview before send.
- **Files**:
  - `ui/components/navigation/ChatInputBar.kt` — MODIFY: Add attachment button
  - `ui/components/chat/ImageAttachmentPicker.kt` — NEW: Gallery/camera picker
  - `ui/components/chat/ImagePreviewModal.kt` — NEW: Full-screen image preview
  - `ui/screens/chat/delegates/ChatMessageDelegate.kt` — MODIFY: Include attachments in message send
  - `domain/model/AttachedFile.kt` — Already exists, verify it supports images

#### W2-T6: Highlighted File References
- **Priority**: P2 | **Complexity**: S
- **Description**: Detect file paths in messages and make them tappable (navigate to file viewer).
- **Files**:
  - `ui/components/chat/MessageBubble.kt` or equivalent — MODIFY: File path detection + annotation
  - Navigation: Link to `FilesScreen` with file pre-selected

---

### Wave 3: Terminal & MCP Enhancements
> **Goal**: Complete terminal lifecycle and MCP OAuth.
> **Dependency**: Wave 0 (API endpoints)

#### W3-T1: Multiple Terminal Tabs
- **Priority**: P1 | **Complexity**: M
- **Description**: Support multiple concurrent terminal sessions in a tabbed UI.
- **Files**:
  - `ui/screens/chat/ChatScreen.kt` (or new `TerminalTabsScreen.kt`) — MODIFY/NEW: Terminal tab bar
  - `data/repository/` — May need `TerminalRepository.kt` to track multiple terminals
  - Handle SSE events: `pty.created`, `pty.updated`, `pty.exited`, `pty.deleted`

#### W3-T2: Terminal Lifecycle (Update/Delete)
- **Priority**: P1 | **Complexity**: S
- **Description**: Support resizing (PUT) and closing (DELETE) terminals.
- **Files**:
  - Terminal UI component — MODIFY: Add close button, handle resize on orientation change
  - API already added in W0-T2

#### W3-T3: MCP OAuth Flow
- **Priority**: P1 | **Complexity**: L
- **Description**: Full OAuth flow for MCP servers (start auth → browser/webview → callback → authenticate).
- **Files**:
  - `ui/screens/mcp/McpScreen.kt` — MODIFY: Add auth buttons for servers needing auth
  - `ui/screens/mcp/McpAuthDialog.kt` — NEW: OAuth flow dialog
  - `ui/screens/mcp/McpScreenModel.kt` — MODIFY: Handle OAuth state machine
  - `data/repository/McpRepository.kt` — May need extension for auth state
  - API already added in W0-T2
  - SSE: Handle `mcp.browser.open.failed`

#### W3-T4: MCP Resource Browsing
- **Priority**: P2 | **Complexity**: M
- **Description**: Browse and view resources exposed by MCP servers.
- **Files**:
  - `ui/screens/mcp/McpResourcesScreen.kt` — NEW: Resource browser screen
  - `ui/screens/mcp/McpScreenModel.kt` — MODIFY: Load resources
  - API already added in W0-T2

---

### Wave 4: Navigation & Discovery
> **Goal**: Command palette and enhanced navigation.
> **Dependency**: Wave 1 (chat enhancements for fuzzy search reuse)

#### W4-T1: Command Palette
- **Priority**: P1 | **Complexity**: L
- **Description**: Global action/search overlay (Android equivalent of Ctrl+K). Search sessions, files, commands, agents.
- **Files**:
  - `ui/components/CommandPaletteOverlay.kt` — NEW: Full-screen search overlay
  - `ui/screens/main/MainScreen.kt` — MODIFY: Add trigger (FAB or gesture)
  - Reuse fuzzy search from W1-T4/W1-T5

#### W4-T2: DiffChanges Bar Visualization
- **Priority**: P2 | **Complexity**: S
- **Description**: Visual bar showing distribution of additions/deletions in a diff, matching `diff-changes.tsx`.
- **Files**:
  - `ui/components/git/DiffChangesBar.kt` — NEW: Bar visualization composable
  - `ui/screens/git/GitDiffScreen.kt` — MODIFY: Integrate bar

#### W4-T3: Diff Viewer Mode Toggle
- **Priority**: P2 | **Complexity**: M
- **Description**: Toggle between unified and split (side-by-side) diff views.
- **Files**:
  - `ui/screens/git/GitDiffScreen.kt` — MODIFY: Add mode toggle
  - `ui/screens/git/GitDiffScreenModel.kt` — MODIFY: Track view mode state
  - May need split-view composable for side-by-side rendering

---

### Wave 5: Experimental Features
> **Goal**: Implement all experimental features from OpenCode.
> **Dependency**: Waves 0-2

#### W5-T1: Worktree Management
- **Priority**: P2 | **Complexity**: L
- **Description**: Full worktree CRUD with UI. Create isolated workspaces, switch between them, reset, delete.
- **Files**:
  - `ui/screens/workspace/WorktreeScreen.kt` — NEW: Worktree management screen
  - `ui/screens/workspace/WorktreeScreenModel.kt` — NEW: ScreenModel
  - `data/repository/WorktreeRepository.kt` — NEW: Repository for worktree API
  - `di/Modules.kt` — MODIFY: Register new repository + screen model
  - Handle SSE: `worktree.ready`, `worktree.failed`

#### W5-T2: Cross-Project Sessions
- **Priority**: P2 | **Complexity**: M
- **Description**: Browse and open sessions from other projects (experimental endpoint).
- **Files**:
  - `ui/screens/sessions/CrossProjectSessionsScreen.kt` — NEW
  - `ui/screens/sessions/CrossProjectSessionsModel.kt` — NEW
  - API already added in W0-T2

#### W5-T3: Skills Listing & Display
- **Priority**: P2 | **Complexity**: S
- **Description**: Show available skills in the UI (new section in settings or agent info).
- **Files**:
  - `ui/screens/settings/SkillsSection.kt` — NEW: Skills list component
  - `data/repository/SkillRepository.kt` — NEW: Repository for skills
  - `di/Modules.kt` — MODIFY: Register

#### W5-T4: Feature Flags System
- **Priority**: P2 | **Complexity**: M
- **Description**: Read feature flags from server config, gate experimental features behind them.
- **Files**:
  - `domain/model/Config.kt` — MODIFY: Add `FeatureFlags` model
  - `data/repository/ConfigRepository.kt` — MODIFY: Parse and expose feature flags
  - `ui/` — MODIFY: Gate experimental UI behind flags

#### W5-T5: Plan Mode UI
- **Priority**: P3 | **Complexity**: L
- **Description**: Multi-step architectural planning mode. Shows plan steps, progress, and allows editing.
- **Files**:
  - `ui/screens/chat/PlanModeView.kt` — NEW: Plan visualization
  - `ui/screens/chat/ChatScreenModel.kt` — MODIFY: Track plan mode state
  - Integration with session's plan data

---

### Wave 6: Global & Lifecycle
> **Goal**: Complete server lifecycle management.
> **Dependency**: Wave 0

#### W6-T1: Global Config Management
- **Priority**: P1 | **Complexity**: S
- **Description**: Read and update global server configuration (separate from per-instance config).
- **Files**:
  - `data/repository/ConfigRepository.kt` — MODIFY: Add global config methods
  - `ui/screens/settings/SettingsScreen.kt` — MODIFY: Global config section

#### W6-T2: Provider Auth Removal
- **Priority**: P1 | **Complexity**: XS
- **Description**: Add ability to remove/disconnect a provider's authentication (DELETE /auth/:providerID).
- **Files**:
  - `ui/screens/settings/SettingsScreen.kt` — MODIFY: Add disconnect button per provider
  - API already added in W0-T2

#### W6-T3: Project Update
- **Priority**: P2 | **Complexity**: S
- **Description**: Update project settings via PATCH /project/:projectID.
- **Files**:
  - `ui/screens/workspace/WorkspaceScreen.kt` — MODIFY: Project settings editor
  - API already added in W0-T2

#### W6-T4: Instance/Global Disposal Events
- **Priority**: P1 | **Complexity**: S
- **Description**: Handle `server.instance.disposed` and `global.disposed` SSE events with proper reconnection/cleanup flow.
- **Files**:
  - `api/MoccaSseClient.kt` — Already extended in W0-T3
  - `data/repository/ConnectionManager.kt` — MODIFY: Handle disposal → reconnect flow
  - `ui/components/modern/ConnectionStatus.kt` — MODIFY: Show disposal state

---

### Wave 7: Polish & Parity Verification
> **Goal**: Fill remaining gaps and verify full parity.
> **Dependency**: All previous waves

#### W7-T1: ReasoningPart Enhancements
- **Priority**: P2 | **Complexity**: S
- **Description**: Ensure reasoning display is throttled, cached, and collapsible matching OpenCode Web.
- **Files**:
  - `ui/components/chat/` — MODIFY: Reasoning rendering component

#### W7-T2: ImagePreview Modal
- **Priority**: P2 | **Complexity**: S
- **Description**: Full-screen image viewer with pinch-to-zoom, dismissable.
- **Files**:
  - `ui/components/chat/ImagePreviewModal.kt` — NEW (or merged with W2-T5)

#### W7-T3: Verify All Tool Card Types
- **Priority**: P2 | **Complexity**: S
- **Description**: Audit `ToolCards.kt` to ensure every tool type from OpenCode's PART_MAPPING is covered (read, bash, edit, write, glob, grep, list, webfetch, task, look_at, ast_grep_search, ast_grep_replace, lsp_*, etc.).
- **Files**:
  - `ui/components/ToolCards.kt` — MODIFY: Add any missing tool type renderers

#### W7-T4: Permission Auto-Respond TTL
- **Priority**: P2 | **Complexity**: S
- **Description**: Verify and complete TTL-based auto-respond for permissions.
- **Files**:
  - `data/repository/PermissionActionBus.kt` — VERIFY/MODIFY

#### W7-T5: Comprehensive Integration Testing
- **Priority**: P1 | **Complexity**: L
- **Description**: End-to-end verification of all implemented features against a running OpenCode server.
- **Files**: Test files throughout the project

---

## 6. Wave Execution Order

```
Wave 0: Foundation (Models + API)          ← START HERE
  ├── W0-T1: Domain Models                [P0, S]
  ├── W0-T2: API Endpoints                [P0, M]
  └── W0-T3: SSE Event Handlers           [P0, M]
      │
      ├──────────────────────────────────────────────┐
      ▼                                              ▼
Wave 1: Core UX (Streaming + Chat)        Wave 3: Terminal & MCP
  ├── W1-T1: Delta Streaming    [P0, L]     ├── W3-T1: Multi Terminal    [P1, M]
  ├── W1-T2: Typewriter         [P1, M]     ├── W3-T2: Terminal CRUD     [P1, S]
  ├── W1-T3: TextShimmer        [P1, S]     ├── W3-T3: MCP OAuth         [P1, L]
  ├── W1-T4: @ Mentions         [P1, L]     └── W3-T4: MCP Resources     [P2, M]
  ├── W1-T5: Slash Commands     [P1, L]
  ├── W1-T6: Prompt History     [P1, S]   Wave 6: Global & Lifecycle
  └── W1-T7: Shell Toggle       [P2, XS]    ├── W6-T1: Global Config     [P1, S]
      │                                      ├── W6-T2: Provider Removal  [P1, XS]
      ▼                                      ├── W6-T3: Project Update    [P2, S]
Wave 2: Session & Message Mgmt              └── W6-T4: Disposal Events   [P1, S]
  ├── W2-T1: Delete Message     [P1, S]
  ├── W2-T2: Edit Part          [P2, M]
  ├── W2-T3: Fork Dialog        [P1, M]
  ├── W2-T4: ContextToolGroup   [P1, M]
  ├── W2-T5: Image Attachments  [P2, M]
  └── W2-T6: File References    [P2, S]
      │
      ▼
Wave 4: Navigation & Discovery
  ├── W4-T1: Command Palette    [P1, L]
  ├── W4-T2: DiffChanges Bar    [P2, S]
  └── W4-T3: Diff Mode Toggle   [P2, M]
      │
      ▼
Wave 5: Experimental Features
  ├── W5-T1: Worktrees          [P2, L]
  ├── W5-T2: Cross-Project      [P2, M]
  ├── W5-T3: Skills Listing     [P2, S]
  ├── W5-T4: Feature Flags      [P2, M]
  └── W5-T5: Plan Mode          [P3, L]
      │
      ▼
Wave 7: Polish & Verification
  ├── W7-T1: Reasoning Polish   [P2, S]
  ├── W7-T2: Image Preview      [P2, S]
  ├── W7-T3: Tool Card Audit    [P2, S]
  ├── W7-T4: Permission TTL     [P2, S]
  └── W7-T5: Integration Test   [P1, L]
```

### Parallel Execution Notes:
- **Wave 1 and Wave 3 and Wave 6** can run in parallel (independent feature areas)
- **Wave 2** depends on Wave 1 for chat component patterns
- **Wave 4** depends on Wave 1 for fuzzy search components
- **Wave 5** depends on Waves 0-2 for base infrastructure
- **Wave 7** is final — runs after all other waves

---

## 7. File Change Manifest

### New Files to Create (~25)

| File | Wave | Description |
|------|------|-------------|
| `domain/model/SkillTypes.kt` | W0 | Skill data models |
| `domain/model/WorktreeTypes.kt` | W0 | Worktree data models |
| `ui/components/chat/StreamingTextComponent.kt` | W1 | Delta streaming renderer |
| `ui/components/chat/TypewriterText.kt` | W1 | Typewriter animation |
| `ui/components/chat/StreamingCursor.kt` | W1 | Blinking cursor |
| `ui/components/chat/TextShimmer.kt` | W1 | Loading shimmer |
| `ui/components/navigation/MentionPopover.kt` | W1 | @ mention autocomplete |
| `ui/components/navigation/CommandPopover.kt` | W1 | / command autocomplete |
| `ui/components/chat/ForkSessionDialog.kt` | W2 | Enhanced fork dialog |
| `ui/components/chat/ContextToolGroup.kt` | W2 | Grouped tool calls |
| `ui/components/chat/ImageAttachmentPicker.kt` | W2 | Image picker |
| `ui/components/chat/ImagePreviewModal.kt` | W2/W7 | Full-screen image viewer |
| `ui/components/chat/EditMessagePartDialog.kt` | W2 | Edit part dialog |
| `ui/screens/mcp/McpAuthDialog.kt` | W3 | MCP OAuth dialog |
| `ui/screens/mcp/McpResourcesScreen.kt` | W3 | Resource browser |
| `ui/components/CommandPaletteOverlay.kt` | W4 | Command palette |
| `ui/components/git/DiffChangesBar.kt` | W4 | Diff visualization |
| `ui/screens/workspace/WorktreeScreen.kt` | W5 | Worktree management |
| `ui/screens/workspace/WorktreeScreenModel.kt` | W5 | Worktree state |
| `data/repository/WorktreeRepository.kt` | W5 | Worktree API |
| `data/repository/SkillRepository.kt` | W5 | Skills API |
| `ui/screens/sessions/CrossProjectSessionsScreen.kt` | W5 | Cross-project sessions |
| `ui/screens/sessions/CrossProjectSessionsModel.kt` | W5 | Cross-project state |
| `ui/screens/settings/SkillsSection.kt` | W5 | Skills display |
| `ui/screens/chat/PlanModeView.kt` | W5 | Plan mode UI |

### Files to Modify (~20)

| File | Waves | Changes |
|------|-------|---------|
| `api/MoccaApiClient.kt` | W0 | +21 new API functions |
| `api/MoccaSseClient.kt` | W0 | +22 new event handlers |
| `domain/model/ServerEvents.kt` | W0 | +22 event data classes |
| `domain/model/McpModels.kt` | W0 | +McpAuth*, McpResource |
| `domain/model/Config.kt` | W0 | +GlobalConfig, FeatureFlags |
| `domain/model/Models.kt` | W0 | +PtyUpdateRequest, ProjectUpdateRequest, CrossProjectSession |
| `ui/screens/chat/ChatContent.kt` | W1, W2 | Streaming, delete, edit, file refs |
| `ui/components/navigation/ChatInputBar.kt` | W1, W2 | @mentions, /commands, history, shell, attachments |
| `ui/components/navigation/ChatInputContent.kt` | W1 | Mention/command insertion |
| `data/repository/ChatStateStore.kt` | W1, W2 | Delta accumulation, prompt history, optimistic ops |
| `ui/screens/chat/delegates/ChatMessageDelegate.kt` | W2 | Image attachment sending |
| `ui/components/ToolCards.kt` | W2, W7 | ContextToolGroup integration, tool audit |
| `ui/screens/mcp/McpScreen.kt` | W3 | Auth buttons, resource link |
| `ui/screens/mcp/McpScreenModel.kt` | W3 | OAuth state, resources |
| `data/repository/ConnectionManager.kt` | W6 | Disposal → reconnect |
| `ui/components/modern/ConnectionStatus.kt` | W6 | Disposal state display |
| `ui/screens/settings/SettingsScreen.kt` | W6 | Global config, provider removal |
| `data/repository/ConfigRepository.kt` | W5, W6 | Feature flags, global config |
| `ui/screens/main/MainScreen.kt` | W4 | Command palette trigger |
| `ui/screens/git/GitDiffScreen.kt` | W4 | Diff mode toggle, bar viz |
| `di/Modules.kt` | W5 | Register new repos + screen models |

---

## Estimated Total Effort

| Wave | Tasks | Total Complexity | Est. Hours |
|------|-------|-----------------|------------|
| Wave 0: Foundation | 3 | S + M + M | 8-14 |
| Wave 1: Core UX | 7 | L + M + S + L + L + S + XS | 20-35 |
| Wave 2: Session/Message | 6 | S + M + M + M + M + S | 14-24 |
| Wave 3: Terminal/MCP | 4 | M + S + L + M | 12-22 |
| Wave 4: Navigation | 3 | L + S + M | 10-18 |
| Wave 5: Experimental | 5 | L + M + S + M + L | 16-28 |
| Wave 6: Global/Lifecycle | 4 | S + XS + S + S | 4-8 |
| Wave 7: Polish | 5 | S + S + S + S + L | 10-16 |
| **TOTAL** | **37 tasks** | | **94-165 hours** |

---

## Appendix A: MOCCA Conventions Checklist

Every implementation must follow these rules (from AGENTS.md):

- [ ] Pitch Black `#000000` OLED theme, Mint Green `#00D9A5` accents
- [ ] No AI Purple/Blue (THE LILA BAN)
- [ ] MVI architecture: ScreenModel → StateFlow → UI
- [ ] Voyager navigation
- [ ] Koin DI for all dependencies
- [ ] `ApiExecutor.execute {}` — never hold `HttpClient`
- [ ] Absolute paths only
- [ ] Never block main thread
- [ ] `Flow<Resource<T>>` for offline-first repositories
- [ ] No `RectangleShape` for interactive elements
- [ ] No iOS/Desktop targets

## Appendix B: OpenCode Server Connection Details

- **Protocol**: HTTP REST + SSE + WebSocket (PTY only)
- **Default Port**: 4096
- **Auth**: Basic Auth (base64 encoded)
- **SSE Format**: `{payload:{type,properties}}`
- **WebSocket**: Binary frames with meta prefix for PTY
- **Framework**: Hono (TypeScript/Bun)
