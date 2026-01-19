# UI COMPONENTS KNOWLEDGE BASE

**Scope:** Shared UI components & Terminal Design System (Design System)

## OVERVIEW
Shared UI library implementing the **Terminal Design System** for the MOCCA application. This system focuses on a low-latency, high-contrast "Pitch Black" terminal aesthetic, designed for developers who prefer a CLI-like experience on mobile.

## THEME RULES
The application MUST strictly adhere to the `TerminalTheme`. **Do NOT use Material 3 defaults** (rounded corners, pastel colors, elevation shadows).

- **Theme Composable**: Use `TerminalTheme { ... }` as the root of all screens.
- **Background**: **Pitch Black** (`#000000`) is mandatory for all primary backgrounds.
- **Corners**: **0dp corners** (RectangleShape) for all buttons, cards, and input fields.
- **Typography**: **Monospace** only (using `TerminalTypography`). 
    - Use `TerminalTypography.bodyMedium` for standard text.
    - Use `TerminalTypography.displayLarge` for terminal-style ASCII indicators.
- **Borders**: 1dp or 2dp solid white borders for active elements or containers.

## KEY COMPONENTS
### 1. Terminal Primitives (`terminal/`)
- `TerminalButton`: Blocky button with #D9D9D9 background and black text.
- `TerminalInput`: Solid black background with white outline and cursor.
- `TerminalCard`: Rectangular container with `surfaceVariant` background and 0dp corners.
- `TerminalBadge`: High-contrast "USER" or "AGENT" tags.
- `ConnectionStatus`: Real-time visualization of OpenCode and Git Server health.

### 2. Common Layouts (`CommonComponents.kt`)
- `LoadingScreen`: Center-aligned progress with uppercase terminal status text.
- `ErrorScreen`: High-visibility error display with `[!]` indicator.
- `PermissionRequestDialog`: **MANDATORY** for tool approval requests. Replaces the legacy `ToolConfirmation` system.

### 3. Specialized Widgets
- `QuestionDialog.kt`: For interactive `confirm` or `input` requests from the AI agent.
- `ToolCards.kt`: Specialized rendering for `ToolUse` and `ToolResult` data structures.

## ANTI-PATTERNS
- **Material 3 Mixing**: NEVER use `MaterialTheme.colorScheme` or `MaterialTheme.shapes`. Use `TerminalTheme.extendedColors` and `TerminalTheme.shapes` (which are all 0dp).
- **Rounded Corners**: Avoid any `RoundedCornerShape`. Use `RectangleShape` or `RoundedCornerShape(0.dp)`.
- **Legacy Components**: **DEPRECATED**: `ToolConfirmationDialog.kt` is removed. Use `PermissionRequestDialog` in `CommonComponents.kt`.
- **Color Literals**: Avoid `Color(0xFF...)`. Always use `TerminalColors` or `TerminalTheme.extendedColors`.
- **Main Thread Logic**: Components must remain stateless. Pass click events and state updates up to the `ScreenModel`.
