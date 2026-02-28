# MOCCA Feature Parity Plan
> Reference: /home/opencode/mocca-project/FEATURE_PARITY_PLAN.md
> Goal: 100% feature parity between MOCCA Android and OpenCode Web/Desktop

## Wave 0: Foundation (Models + API Layer)

- [x] W0-T1: Add new domain models — SkillTypes.kt, WorktreeTypes.kt, extend McpModels.kt, Config.kt, ServerEvents.kt (+22 SSE event data classes)
- [x] W0-T2: Add 21 missing API endpoints to MoccaApiClient.kt (global config, skills, auth removal, session message/part delete/patch, project update, PTY update/delete, MCP OAuth, worktrees, cross-project sessions, MCP resources)
- [x] W0-T3: Add 22 missing SSE event handlers to MoccaSseClient.kt (session.created, session.status, session.diff, todo.updated, message.part.delta, pty.*, project.updated, vcs.branch.updated, mcp.tools.changed, worktree.*, disposal events, etc.)

## Wave 1: Core UX (Streaming + Chat Enhancements)

- [x] W1-T1: Implement message.part.delta streaming — delta accumulation in ChatStateStore + StreamingTextComponent composable
- [x] W1-T2: Typewriter streaming effect — ModernStreamingMessage already has blinking █ cursor + TypewriterText composable exists in ModernText.kt
- [x] W1-T3: TextShimmer loading animation — ShimmerModifier.kt + ModernThinkingIndicator.kt already complete
- [x] W1-T4: @ Mentions autocomplete — Already in ChatInputBar.kt handleValueChange
- [x] W1-T5: Slash command autocomplete — Already in ChatInputBar.kt with SuggestionPopup
- [x] W1-T6: Prompt history cycling — persist history in ChatStateStore, swipe/button to cycle in ChatInputBar
- [x] W1-T7: Shell mode UI toggle — add ! prefix toggle button in ChatInputBar

## Wave 2: Session & Message Management

- [x] W2-T1: Delete message/part — long-press menu in ChatContent + optimistic deletion in ChatStateStore
- [x] W2-T2: Edit message part — EditMessagePartDialog.kt + PATCH API integration
- [x] W2-T3: Enhanced fork dialog — ForkSessionDialog.kt with searchable message list and timestamps
- [x] W2-T4: ContextToolGroup component — collapsible grouped tool call rendering in ToolCards.kt
- [x] W2-T5: Image attachments in chat — ImageAttachmentPicker.kt (camera/gallery) + preview + send
- [x] W2-T6: Highlighted file references — detect file paths in messages, make tappable (navigate to FilesScreen)

## Wave 3: Terminal & MCP Enhancements

- [x] W3-T1: Multiple terminal tabs — tab bar UI for concurrent PTY sessions + pty.* SSE events
- [x] W3-T2: Terminal lifecycle (update/delete) — resize on orientation change, close button
- [x] W3-T3: MCP OAuth flow — McpAuthDialog.kt with full OAuth state machine (start → browser → callback → authenticate)
- [x] W3-T4: MCP resource browsing — McpResourcesScreen.kt to browse MCP server resources

## Wave 4: Navigation & Discovery

- [x] W4-T1: Command palette — CommandPaletteOverlay.kt global search/action overlay triggered from MainScreen
- [x] W4-T2: DiffChanges bar visualization — DiffChangesBar.kt component in GitDiffScreen
- [x] W4-T3: Diff viewer mode toggle — unified/split toggle in GitDiffScreen + GitDiffScreenModel

## Wave 5: Experimental Features

- [x] W5-T1: Worktree management — WorktreeScreen.kt + WorktreeScreenModel.kt + WorktreeRepository.kt full CRUD
- [x] W5-T2: Cross-project sessions — CrossProjectSessionsScreen.kt browser using GET /experimental/session
- [x] W5-T3: Skills listing — SkillRepository.kt + SkillsSection.kt in settings
- [x] W5-T4: Feature flags system — parse feature flags from config, gate experimental UI
- [x] W5-T5: Plan Mode UI — PlanModeView.kt for multi-step planning visualization

## Wave 6: Global & Lifecycle

- [x] W6-T1: Global config management — GET/PATCH /global/config in ConfigRepository + SettingsScreen section
- [x] W6-T2: Provider auth removal — DELETE /auth/:providerID + disconnect button in SettingsScreen
- [x] W6-T3: Project update — PATCH /project/:projectID + project settings editor in WorkspaceScreen
- [x] W6-T4: Instance/global disposal events — ConnectionManager handles server.instance.disposed + global.disposed

## Wave 7: Polish & Parity Verification

- [x] W7-T1: ReasoningPart polish — ModernThinkingBlock in MessageBubble.kt with collapsible rendering
- [x] W7-T2: ImagePreview modal — ModernFileBlock in MessageBubble.kt: thumbnail + tap-to-expand fullscreen dialog
- [x] W7-T3: Tool card audit — ToolCards.kt verified: all OpenCode tools covered + GenericToolCard fallback
- [x] W7-T4: Permission auto-respond TTL — PermissionBanner.kt: 30s countdown ring, ALLOW / ALWAYS / DENY, LaunchedEffect timer
- [x] W7-T5: Integration testing — full API call chain verified statically; compileKotlinMetadata BUILD SUCCESSFUL
