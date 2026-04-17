# M3 Expressive Token Reference (MOCCA-Adapted)

Quick-reference for typography, shape, elevation, and motion tokens as used in MOCCA's dark theme.

## Typography Scale

MOCCA uses **Space Grotesk** for display/headline/title/label styles and system default for body. Code blocks use a user-selected monospace font (JetBrains Mono by default) via `LocalCodeFontFamily`.

### Baseline Scale (AppTypography)

| Style | Font | Weight | Size (sp) | Line Height (sp) | Tracking (sp) |
|-------|------|--------|-----------|-------------------|---------------|
| Display Large | Space Grotesk | W800 | 57 | 64 | -0.5 |
| Display Medium | Space Grotesk | W700 | 45 | 52 | -0.25 |
| Display Small | Space Grotesk | W600 | 36 | 44 | -0.25 |
| Headline Large | Space Grotesk | W700 | 32 | 40 | 0 |
| Headline Medium | Space Grotesk | W600 | 28 | 36 | 0 |
| Headline Small | Space Grotesk | W500 | 24 | 32 | 0 |
| Title Large | Space Grotesk | W600 | 22 | 28 | 0 |
| Title Medium | Space Grotesk | W500 | 16 | 24 | 0.15 |
| Title Small | Space Grotesk | W500 | 14 | 20 | 0.1 |
| Body Large | System Default | Normal | 16 | 24 | 0.5 |
| Body Medium | System Default | Normal | 14 | 20 | 0.25 |
| Body Small | System Default | Normal | 12 | 16 | 0.4 |
| Label Large | Space Grotesk | W600 | 14 | 20 | 0.1 |
| Label Medium | Space Grotesk | W500 | 12 | 16 | 0.5 |
| Label Small | Space Grotesk | W500 | 11 | 16 | 0.5 |

### Emphasized & Extended Styles

| Style | Weight Override | Notes |
|-------|----------------|-------|
| `displayLargeEmphasized` | W900, tracking -1.5sp | Highest emphasis display |
| `code` | Normal, 14sp/20sp | Uses `LocalCodeFontFamily.current` |
| `codeSmall` | Normal, 13sp/18sp | Compact code blocks |
| `status` | W500, 10sp/12sp | Status indicators |
| `labelExtraSmall` | W500, 9sp/12sp | Micro labels |
| `monoLabel` | Normal, 10sp/14sp | Monospace labels |

### Component Typography Mapping

| Component | Type Style |
|-----------|-----------|
| Button label | `labelLarge` |
| Card title | `titleMedium` |
| Card body | `bodyMedium` |
| Top app bar title | `titleLarge` |
| Navigation label | `labelMedium` |
| Dialog headline | `headlineSmall` |
| Dialog body | `bodyMedium` |
| Text field input | `bodyLarge` |
| List headline | `bodyLarge` |
| Tab label | `titleSmall` |

## Shape Tokens

### M3 Corner Radius Scale vs AppShapes

| M3 Token | Value (dp) | AppShapes Equivalent | Default Components |
|----------|-----------|---------------------|--------------------|
| `none` | 0 | `AppShapes.none` | — |
| `extra-small` | 4 | `AppShapes.extraSmall` | Snackbar, badges |
| `small` | 8 | `AppShapes.small` | Text fields, menus, chips |
| `medium` | 12 | `AppShapes.medium` / `codeBlock` | Cards (M3 default) |
| `large` | 16 | `AppShapes.large` / `card` | FAB, nav drawer, top/bottom rounded |
| `large-increased` | 20 | — | (Expressive) |
| `extra-large` | 28 | `AppShapes.extraLarge` / `moduleCard` | Bottom sheets, dialogs |
| `extra-extra-large` | 32 | `AppShapes.extraExtraLarge` / `input` | (Expressive) |
| `full` | 9999 | `AppShapes.pill` / `tabPill` / `tag` | Buttons, pills, sliders |

### MOCCA-Specific Shape Tokens

| Token | Shape | Usage |
|-------|-------|-------|
| `pill` | 9999dp | Buttons, tab indicators, tags |
| `circle` | 50% | Avatars, FABs |
| `bottomSheet` | 24dp top corners | Modal bottom sheets |
| `messageBubbleUser` | Asymmetric (2dp top-end) | User chat bubbles |
| `messageBubbleAgent` | Asymmetric (2dp top-start) | Agent chat bubbles |
| `squircle` | Custom squircle | Hero elements |
| `dialog` | 24dp uniform | Dialog surfaces |
| `sessionCard` | 24dp | Session list items |

### Expressive MaterialShapes (M3 Expressive)

Available via `MaterialShapes.*` + `.toShape()` — `flower`, `sunny`, `gem`, `puffy`, `bun`, `heart`, `boom`, `slanted`, `arch`, `arrow`, `fan`. Use sparingly for decorative/fun elements only.

## Elevation

### Elevation Levels

| Level | DP | Use |
|-------|----|-----|
| 0 | 0dp | Most resting components (cards, buttons, lists) |
| 1 | 1dp | Elevated cards, modal bottom sheets |
| 2 | 3dp | Menus, nav bar, scrolled app bar |
| 3 | 6dp | FAB, dialogs, search |
| 4 | 8dp | Hover/focus increase only |
| 5 | 12dp | Hover/focus increase only |

### MOCCA Elevation Rules

- **Prefer `tonalElevation`** for depth — maps to surface container hierarchy (surfaceContainerLowest → surfaceContainerHighest).
- **`shadowElevation`** acceptable only for modals (dialogs, modal sheets) needing extra visual separation from background content.
- Surface container hierarchy provides implicit elevation:

| Container Level | Hex | Tone |
|----------------|-----|------|
| `surfaceContainerLowest` | #121212 | 8 |
| `surfaceContainerLow` | #1A1A1A | 10 |
| `surfaceContainer` | #202020 | 12 |
| `surfaceContainerHigh` | #272727 | 17 |
| `surfaceContainerHighest` | #303030 | 22 |

## Motion

### Spring-Based Motion (M3 Expressive)

MOCCA uses `MaterialTheme.motionScheme` as the standard motion API — no custom animation constant objects.

Key APIs on `motionScheme`:
- `defaultSpatialSpec()` — standard spatial transitions
- `fastSpatialSpec()` — quick interactions (used in `moccaClickable`)
- `defaultEffectsSpec()` — fade/crossfade
- `staggeredSpatialSpec()` — staggered container animations

### Performance-Aware Motion

`AppPerformance.useExpressiveMotion` controls whether expressive spring physics are active. On LOW tier or low-power mode, motion degrades gracefully to simpler specs. Check via `LocalAppPerformance.current`.

### Easing Reference (for transition-based animation)

| Easing | Use |
|--------|-----|
| Emphasized Decelerate | Enter screen (400ms) |
| Emphasized Accelerate | Exit permanently (200ms) |
| Emphasized | Stay on screen (500ms) |
| Standard | Utility transitions (300ms) |

### Component Elevation + Motion

| Component | Rest Elevation | Hover Elevation |
|-----------|---------------|-----------------|
| Filled/tonal/outlined button | 0 | +1 |
| Elevated card | 1 | +1 |
| FAB | 3 | 4 |
| Dialog | 3 | — |
| Nav bar | 2 | — |
