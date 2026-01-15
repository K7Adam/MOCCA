# UI COMPONENTS KNOWLEDGE BASE

**Generated:** 2026-01-14
**Scope:** Shared UI components & Terminal Design System

## OVERVIEW
Shared UI library implementing the Terminal Design System and atomic widgets for the Mocca application.

## STRUCTURE
```
components/
├── terminal/             # Terminal Design System primitives & widgets
│   ├── Terminal*.kt      # Core atoms (Text, Button, Card, Badge, Input)
│   ├── ConnectionStatus.kt # Server connection state visualization
│   └── QuoteRotator.kt   # Loading screen trivia display
├── CommonComponents.kt   # Shared application dialogs (PermissionDialog)
├── ToolCards.kt          # Agent tool execution visualizations
└── QuestionDialog.kt     # Interactive user prompts from server
```

## WHERE TO LOOK
| Component Type | File Pattern | Usage |
|----------------|--------------|-------|
| **Core Primitives** | `terminal/Terminal*.kt` | Base building blocks (Buttons, Inputs, Text) |
| **Tool Visualization** | `ToolCards.kt` | displaying `ToolUse` and `ToolResult` from agents |
| **Dialogs** | `QuestionDialog.kt`, `CommonComponents.kt` | User interruptions (Questions, Permissions) |
| **Status Indicators** | `terminal/ConnectionStatus.kt` | Network health and retry controls |

## CONVENTIONS
- **Terminal Theme Usage**: EXCLUSIVELY use `TerminalTheme.colors` (background, onBackground, primary) and `TerminalTheme.typography`.
- **Statelessness**: All components must be stateless; hoist state to parent/ScreenModel and pass events via lambdas.
- **Modifier param**: First argument MUST be `modifier: Modifier = Modifier`.
- **Typography**: Prefer `TerminalTextStyle.Mono` for data/code and `TerminalTextStyle.Header` for structural text.
- **Files**: One component per file generally, except for tight groupings (e.g., `ToolCards.kt`).

## ANTI-PATTERNS
- **Hardcoded Colors**: NEVER use `Color(0xFF...)` or `Color.Red`. Use `TerminalTheme.colors.error` instead.
- **Material Theme Mixing**: Avoid `MaterialTheme.typography` or `MaterialTheme.colorScheme` inside Terminal components.
- **Deprecated Components**: `ToolConfirmationDialog.kt` is DEPRECATED. Use `PermissionDialog` inside `CommonComponents.kt`.
- **Business Logic**: Components should never access Repositories or ViewModels directly.
