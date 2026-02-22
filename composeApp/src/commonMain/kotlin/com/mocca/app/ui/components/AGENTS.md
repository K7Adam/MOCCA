# UI COMPONENTS KNOWLEDGE BASE

**Scope:** Shared UI components & Modern Glassmorphic Design System

## RELEVANT SKILLS
- **taste-skill-compose** — UI/UX design rules, animations, theming, glass effects

## OVERVIEW
Shared UI library implementing the **Modern Glassmorphic Design System** for the MOCCA application. This system focuses on a low-latency, high-contrast, compact "Pitch Black" aesthetic.

## LIQUID GLASS EFFECTS
**REQUIRED**: Use [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) for glassmorphic UI effects.

**Dependency**: `io.github.kyant0:backdrop`
**Docs**: https://kyant.gitbook.io/backdrop

**Example Components:**
- `LiquidButton` — Glass button with refractive effects
- `LiquidToggle` — Glass toggle switch
- `LiquidSlider` — Glass slider control
- `LiquidBottomTabs` — Glass bottom navigation

**Integration:**
```kotlin
// In build.gradle.kts
implementation("io.github.kyant0:backdrop:1.0.6")

// Usage
Backdrop(
    specular = 0.5f,
    refract = 0.3f,
    // ... glass parameters
) {
    // Content
}
```

## THEME RULES
The application MUST strictly adhere to the `AppTheme`. **Do NOT use Material 3 defaults** (pastel colors, elevation shadows).

- **Theme Composable**: Use `AppTheme { ... }` as the root of all screens.
- **Background**: **Pitch Black** (`#000000`) is mandatory for all primary backgrounds.
- **Corners**: Rounded corners (12dp-32dp) for interactive elements (buttons, inputs, cards).
- **Typography**: Use `AppTypography` for all text styles.
    - Use `AppTypography.bodyMedium` for standard text.
    - Use `AppTypography.labelSmall` for status indicators.
- **Borders**: 1dp or 1.5dp solid borders for active elements or containers.

## KEY COMPONENTS
### 1. Modern Primitives (`modern/`)
- `MoccaButton`: Pill-shaped button with `accentGreen` background and black text.
- `MoccaInput`: Solid black background with rounded corners and subtle border.
- `MoccaCard`: Rounded container with `surfaceContainer` background and 16dp corners.
- `ModernBadge`: Glassmorphic "USER" or "AGENT" tags.
- `ModernTopBar`: Compact top bar with connection quality indicator. Observes `ConnectionStatus` from `ConnectionManager`.

### 2. Common Layouts (`CommonComponents.kt`)
- `LoadingScreen`: Center-aligned progress with compact status text.
- `ErrorScreen`: High-visibility error display.
- `PermissionRequestDialog`: **MANDATORY** for tool approval requests.

### 3. Specialized Widgets
- `QuestionDialog.kt`: For interactive `confirm` or `input` requests from the AI agent.
- `ToolCards.kt`: Specialized rendering for `ToolUse` and `ToolResult` data structures.

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
- **Material 3 Mixing**: NEVER use `MaterialTheme.colorScheme` or `MaterialTheme.shapes`. Use `AppColors` and `AppShapes`.
- **RectangleShape for Interactive Elements**: NEVER use `RectangleShape` for buttons, cards, or inputs. Use `AppShapes.pill`, `AppShapes.card`, or `AppShapes.medium`.
- **Legacy Components**: Use `PermissionRequestDialog` instead of legacy confirmation systems.
- **Color Literals**: Avoid `Color(0xFF...)`. Always use `AppColors`.
- **Main Thread Logic**: Components must remain stateless. Pass click events and state updates up to the `ScreenModel`.
