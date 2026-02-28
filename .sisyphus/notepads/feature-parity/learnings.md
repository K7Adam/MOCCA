# Feature Parity — Learnings & Conventions

## [2026-02-27] Session ses_36114fe8affe4Zhpi8hp712Qo6 — Project Setup

### MOCCA Architecture Conventions (from AGENTS.md)
- **MVI pattern**: ScreenModel → StateFlow → UI (Compose)
- **DI**: Koin — all deps registered in `di/Modules.kt`
- **Navigation**: Voyager
- **HTTP**: NEVER hold HttpClient — use `ApiExecutor.execute {}`
- **Theme**: Pitch Black `#000000` OLED, Mint Green `#00D9A5` accents
- **THE LILA BAN**: No AI Purple/Blue aesthetic
- **Offline-first**: Repositories return `Flow<Resource<T>>`
- **Shapes**: NEVER use `RectangleShape` for interactive elements
- **Paths**: ALWAYS absolute paths
- **Glass effects**: Use AndroidLiquidGlass (`io.github.kyant0:backdrop`)

### Project Root
- `/home/opencode/mocca-project/`
- Source: `composeApp/src/commonMain/kotlin/com/mocca/app/`
- API: `api/MoccaApiClient.kt` (73 functions), `api/MoccaSseClient.kt` (21 events)
- Models: `domain/model/` (ServerEvents.kt, Config.kt, McpModels.kt, etc.)
- DI: `di/Modules.kt`

### OpenCode Server Details
- TypeScript/Hono/Bun on port 4096
- HTTP Basic Auth
- SSE format: `{payload:{type,properties}}`
- WebSocket only for PTY (not chat)
- 86 applicable REST routes, 43 SSE events

### Gap Summary
- 21 missing API endpoints (see FEATURE_PARITY_PLAN.md §1)
- 22 missing SSE event handlers (see FEATURE_PARITY_PLAN.md §2)
- 28 missing UI features across 8 categories (see FEATURE_PARITY_PLAN.md §3)

## [2026-02-27] W0-T1 COMPLETE: Domain Models Added
- Created: SkillTypes.kt (SkillInfo)
- Created: WorktreeTypes.kt (WorktreeInfo, WorktreeStatus, WorktreeCreateRequest, WorktreeResetRequest)
- Extended: McpModels.kt (+McpAuthRequest, McpAuthCallbackRequest, McpOAuthState, McpAddServerRequest)
- Extended: ServerEvents.kt (+22 new event types and properties — SessionCreated, SessionStatus, SessionDiff, SessionCompacted, TodoUpdated, QuestionRejected, MessagePartDelta, PtyCreated/Updated/Exited/Deleted, ProjectUpdated, VcsBranchUpdated, FileUpdated, LspUpdated, McpToolsChanged, McpBrowserOpenFailed, WorktreeReady/Failed, InstallationUpdateAvailable, ServerInstanceDisposed, GlobalDisposed)
- Extended: Config.kt (+GlobalAppConfig, AppConfigUpdate, FeatureFlags)
- Extended: Models.kt (+PtyUpdateRequest, ProjectUpdateRequest, CrossProjectSession)
- Note: Kotlin LSP not available in env; no build verification possible but all models follow existing patterns
- Pattern: `@Serializable @Immutable` on data classes, `@SerialName` on enum variants and sealed subclasses

## [2026-02-27] W0-T2 COMPLETE: 21 API Endpoints Added to MoccaApiClient.kt
- Added global config, skills, auth removal, message management, project update
- Added terminal update/delete, MCP OAuth x4, worktrees x4
- Added cross-project sessions, MCP resources x2
- All type names verified against domain model files before insertion

## [2026-02-27] W0-T2 COMPLETE: 21 API Endpoints Added to MoccaApiClient.kt
- Added: getGlobalConfig, updateGlobalConfig (global/config)
- Added: listSkills (skill)
- Added: deleteProviderAuth (provider/:id/auth DELETE)
- Added: deleteMessage, deleteMessagePart, patchMessagePart (session message management)
- Added: updateProject (project/:id PATCH)
- Added: updateTerminal, deleteTerminal (terminal/:id PATCH/DELETE)
- Added: startMcpAuth, handleMcpAuthCallback, deleteMcpServer, addMcpServerConfig (MCP OAuth)
- Added: listWorktrees, createWorktree, deleteWorktree, resetWorktree (experimental/worktree)
- Added: listCrossProjectSessions (experimental/session)
- Added: listMcpResources, readMcpResource (mcp/:name/resource)
- Also added McpResourceContent data class to McpModels.kt (was missing)
- MoccaApiClient.kt grew from 859 → 1057 lines

## [2026-02-27] W0-T3 COMPLETE: 22 SSE Event Handlers Added to MoccaSseClient.kt
- Added branches: session.created, session.status, session.diff, session.compacted
- Added branches: todo.updated, question.rejected, message.part.delta
- Added branches: pty.created, pty.updated, pty.exited, pty.deleted
- Added branches: project.updated, vcs.branch.updated
- Added branches: file.updated, lsp.updated
- Added branches: mcp.tools.changed, mcp.browser.open.failed
- Added branches: worktree.ready, worktree.failed
- Added branches: installation.update.available, server.instance.disposed, global.disposed
- MoccaSseClient.kt grew from 168 → 210 lines
- All 22 ServerEvent subclasses verified to exist in ServerEvents.kt before adding
