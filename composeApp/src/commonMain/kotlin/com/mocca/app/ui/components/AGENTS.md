# UI COMPONENTS KNOWLEDGE BASE

**Updated:** 2026-05-05
**Scope:** Shared UI components and Material 3 Expressive primitives

## RELEVANT SKILLS
- **taste-skill-compose** — UI/UX design rules, animations, theming, M3 Surface components

## OVERVIEW
Shared UI library for MOCCA's compact developer-tool interface. Components should be low-latency, high-contrast, and consistent with `AppTheme`, `AppColors`, `AppTypography`, `AppShapes`, and Material 3 Expressive motion.

## SURFACE-BASED COMPONENTS
- Prefer `tonalElevation` for depth. `shadowElevation` acceptable for modals (dialogs, modal sheets) needing extra visual separation.
- Card components use surface container hierarchy (surface, surfaceContainer, surfaceContainerHigh)
- NO blur effects, NO glassmorphism, NO backdrop sampling

## THEME RULES
The application MUST strictly adhere to the `AppTheme`.

- **Theme Composable**: Use `AppTheme { ... }` as the root of all screens.
- **Color Tokens**: Use `AppColors`; do not hard-code a screen-specific dark palette.
- **Corners**: Use `AppShapes`; never use `RectangleShape` for interactive elements.
- **Typography**: Use `AppTypography` for all text styles.
    - Use `AppTypography.bodyMedium` for standard text.
    - Use `AppTypography.labelSmall` for status indicators.
- **Borders**: 1dp or 1.5dp solid borders for active elements or containers.

## KEY COMPONENTS
### 1. Modern Primitives (`modern/`)
- `MoccaButton`: Pill-shaped button with `primary` background and black text.
- `MoccaButtonVariants`: Extended button styles for secondary/ghost states.
- `MoccaInput`: Solid black background with rounded corners and subtle border.
- `MoccaCard`: Rounded container with `surfaceContainer` background and 16dp corners.
- `ModernBadge`: Surface-based "USER" or "AGENT" tags with tonal elevation.
- `ModernTopBar`: Compact top bar with connection quality indicator. Observes `ConnectionStatus` from `ConnectionManager`.
- `ModernActionComponents`, `ModernChatComponents`, `ModernProcessingIndicator`, `ModernThinkingIndicator`: Chat-specific modern primitives.
- `ConnectionStatus`, `ConnectionTroubleshooting`, `SyncStatusIndicator`: Connection-state UI.
- `MessageRow`, `MessageSkeleton`, `GroupedSessionCard`: Message and session rendering.
- `ScrollToBottomButton`, `SuggestionPopup`, `QuoteRotator`: Chat interaction helpers.
- `ModelSelectorDialog`, `VariantSelectorDialog`, `EditMessagePartDialog`, `ForkSessionDialog`: Modal dialogs.
- `UpdateDialog`, `GlobalActivityIndicator`, `UnifiedSystemStatus`: System status surfaces.
- `AdaptiveScaffold`, `DashboardModules`, `ModuleCard`: Layout scaffolding.
- `CommandPaletteOverlay`, `ContextInfo`: Command and context UI.
- `MoccaList`, `ModernText`, `ShimmerModifier`, `SnakeDotsLoader`: Utility primitives.
- `SystemModules`: Modular system UI components.

### 2. Message Parts (`modern/message/`)
- `StreamingPart`, `ReasoningPart`, `FilePart`, `SubTaskPart`, `ToolCallPart`: Part-type renderers for chat messages.

### 3. Common Layouts (`CommonComponents.kt`)
- `LoadingScreen`: Center-aligned progress with compact status text.
- `ErrorScreen`: High-visibility error display.
- `PermissionRequestDialog`: **MANDATORY** for tool approval requests.
- `GodBlocks.kt`: Debug/diagnostic overlay components.
- `EmulatorSetupBanner.kt`: Emulator environment banner.

### 4. Chat Components (`chat/`)
- `InlineDiffViewer`, `FileChangeBlock`, `PermissionBanner`, `TodoListPanel`: Chat inline widgets.

### 5. Editor Components (`editor/`)
- `CodeEditorView`, `EditorJsBridge`: Code editing surface and bridge.

### 6. Navigation Components (`navigation/`)
- `PersistentNavRow`, `BottomNavItems`, `SharedNavIndicator`, `NavConstants`: Navigation chrome.
- `ChatInputActions`, `ChatInputContent`, `ChatInputStatusBar`, `ChatInputTextField`: Chat input composition.
- `AgentSelectorSheet`, `AttachmentPreview`: Chat input sheets and previews.

### 7. Voice Components (`voice/`)
- `RequestVoicePermissionEffect`: Voice permission handling.

### 8. Specialized Widgets
- `QuestionDialog.kt`: For interactive `confirm` or `input` requests from the AI agent.
- `ToolCards.kt`, `ToolCardHelpers.kt`, `ToolCardWidgets.kt`, `UniversalToolCard.kt`: Specialized rendering for `ToolUse` and `ToolResult` data structures.
- Chat activity strips should be compact, state-derived, and tied to `AgentActivity`; avoid separate UI-owned status buffers.

## CONNECTION STATUS
The `ConnectionStatus` sealed class (defined in `Config.kt`) drives the connection indicator in the top bar:
- `NotConfigured` — No server configured
- `Disconnected(reason)`
- `Connecting` — Initial connection attempt
- `WaitingForNetwork` — Network unavailable
- `Reconnecting(attempt, maxAttempts)` — Auto-reconnection in progress
- `Connected(serverInfo, latencyMs)` — Active connection with latency info
- `Error(message)` — Connection error

## ANTI-PATTERNS
- **Material 3 Mixing**: In feature/UI code, use `AppColors`, `AppShapes`, `AppTypography` instead of `MaterialTheme.colorScheme`/`MaterialTheme.shapes`. Theme bridge code (`AppTheme.kt`) may use MaterialTheme APIs for provisioning.
- **RectangleShape for Interactive Elements**: NEVER use `RectangleShape` for buttons, cards, or inputs. Use `AppShapes.pill`, `AppShapes.card`, or `AppShapes.medium`.
- **Legacy Components**: Use `PermissionRequestDialog` instead of legacy confirmation systems.
- **Detached Streaming UI**: Do not add new global streaming/thinking rows unless the active assistant message has no matching live part yet.
- **Color Literals**: Avoid `Color(0xFF...)`. Always use `AppColors`.
- **Main Thread Logic**: Components must remain stateless. Pass click events and state updates up to the `ScreenModel`.
