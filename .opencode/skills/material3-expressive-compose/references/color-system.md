# M3 Color System (MOCCA-Adapted)

Color role reference for MOCCA's fixed dark palette. **Dynamic color is NOT used** — the app uses a static dark scheme seeded from `#8B9DC3` (AnchorPrimary) with `#1A1A1A` neutral seed.

## Accent Color Roles

Three accent groups, each with 4 roles. In MOCCA, primary and secondary share the same hue for a monochrome feel.

| Role | Hex | Tone | Purpose |
|------|-----|------|---------|
| `primary` | #8B9DC3 | 80 | High-emphasis fills, active states |
| `onPrimary` | #1A2C4D | 20 | Text/icons on primary |
| `primaryContainer` | #324465 | 30 | Container fills (FAB, key components) |
| `onPrimaryContainer` | #C2D1F0 | 90 | Text/icons on primary container |
| `secondary` | #8B9DC3 | 80 | Less prominent fills |
| `onSecondary` | #1A2C4D | 20 | Text/icons on secondary |
| `secondaryContainer` | #272727 | 20 | Recessive containers (tonal buttons) |
| `onSecondaryContainer` | #A0A0A0 | 70 | Text/icons on secondary container |
| `inversePrimary` | #3F517E | 40 | Actionable text on inverse surfaces |

> **Note**: MOCCA does not currently define tertiary roles separately. The `accent` / `accentBright` tokens serve as complementary accents.

## Error Color Roles

| Role | Hex | Purpose |
|------|-----|---------|
| `error` | #EF5350 | Urgent/attention elements |
| `onError` | #690005 | Text/icons on error |
| `errorContainer` | #93000A | Error container fill |
| `onErrorContainer` | #FFDAD6 | Text/icons on error container |
| `errorDim` | #EF5350 @ 10% | Subtle error backgrounds |

## Surface Colors

### Core Surfaces

| Role | Hex | Tone | Purpose |
|------|-----|------|---------|
| `surface` | #1A1A1A | 10 | Default background |
| `onSurface` | #E8E8E8 | 90 | Text/icons on surface |
| `onSurfaceVariant` | #A0A0A0 | 70 | Lower-emphasis text/icons |
| `surfaceVariant` | #272727 | 20 | Variant surface |
| `surfaceTint` | #8B9DC3 | — | Brand tint for surface tinting |
| `surfaceDim` | #0F0F0F | 6 | Dimmest surface |
| `surfaceBright` | #383838 | 24 | Brightest surface |

### Surface Container Hierarchy (low → high)

| Role | Hex | Tone | Typical Use |
|------|-----|------|-------------|
| `surfaceContainerLowest` | #121212 | 8 | Deepest recessed elements |
| `surfaceContainerLow` | #1A1A1A | 10 | Slightly recessed |
| `surfaceContainer` | #202020 | 12 | Default containers, navigation areas |
| `surfaceContainerHigh` | #272727 | 17 | Elevated containers |
| `surfaceContainerHighest` | #303030 | 22 | Highest-emphasis containers |

### Inverse Colors

| Role | Hex | Purpose |
|------|-----|---------|
| `inverseSurface` | #E8E8E8 | Background for contrasting elements (snackbar) |
| `inverseOnSurface` | #1A1A1A | Text on inverse surface |

## Outline Colors

| Role | Hex | Purpose |
|------|-----|---------|
| `outline` | #707070 | Important boundaries (text field borders, 3:1 contrast) |
| `outlineVariant` | #444444 | Decorative (dividers, card borders) |

**Rule**: Use `outline` for interactive boundaries needing contrast. Use `outlineVariant` for decorative separators. Never substitute one for the other.

## Fixed Accent Colors (M3 Standard)

Fixed colors maintain the same value in both light and dark themes. MOCCA defines `accent` / `accentBright` for similar cross-theme consistency.

| M3 Role | MOCCA Equivalent |
|---------|-----------------|
| `primaryFixed` | Not defined — use `accent` (#8B9DC3) |
| `primaryFixedDim` | Not defined |
| `onPrimaryFixed` | Not defined — use `onPrimary` |

> Fixed colors are rarely needed in MOCCA since the app is dark-only.

## Color Pairing Rules

Never pair colors outside their intended pairs — this breaks contrast guarantees.

| Fill/Container | Text/Icon |
|---------------|-----------|
| `primary` | `onPrimary` |
| `primaryContainer` | `onPrimaryContainer` |
| `secondaryContainer` | `onSecondaryContainer` |
| `error` | `onError` |
| `errorContainer` | `onErrorContainer` |
| `surface` | `onSurface` or `onSurfaceVariant` |
| `surfaceContainer*` | `onSurface` or `onSurfaceVariant` |
| `inverseSurface` | `inverseOnSurface` |

## MOCCA Extended Tokens

### Status Colors

| Token | Hex | Use |
|-------|-----|-----|
| `statusOnline` / `statusSuccess` | #4CAF50 | Connected, success states |
| `statusOffline` / `statusError` | #EF5350 | Disconnected, error states |
| `statusWaiting` / `statusWarning` | #FFB74D | Waiting, warning states |
| `statusThinking` / `statusInfo` | #8B9DC3 | Thinking, info states |
| `statusProcessing` | #8B9DC3 | Processing indicator |
| `success` | #4CAF50 | Generic success |
| `warning` | #FFB74D | Generic warning |

### Syntax Highlighting

| Token | Hex | Use |
|-------|-----|-----|
| `syntaxKeyword` | #C586C0 | Language keywords |
| `syntaxFunction` | #61DAFB | Function names |
| `syntaxString` | #CE9178 | String literals |
| `syntaxType` | #9E9E9E | Type annotations |
| `syntaxComment` | #6A9955 | Comments |
| `syntaxPunctuation` | #D4D4D4 | Punctuation/brackets |
| `lineNumbers` | #666666 | Line number gutter |

### Diff Colors

| Token | Hex | Use |
|-------|-----|-----|
| `diffAddition` | #4CAF50 @ 10% | Added line background |
| `diffDeletion` | #EF5350 @ 10% | Deleted line background |
| `diffAdditionText` | #4CAF50 | Added line text/gutter |
| `diffDeletionText` | #EF5350 | Deleted line text/gutter |

### File Type Colors

| Token | Hex | File Type |
|-------|-----|-----------|
| `fileTsx` | #61DAFB | TSX/React files |
| `fileCss` | #9E9E9E | CSS files |
| `fileJson` | #EF5350 | JSON files |

### Utility Colors

| Token | Hex | Use |
|-------|-----|-----|
| `scrim` | #000000 @ 80% | Modal scrims |
| `inputGlow` | #8B9DC3 @ 25% | Focused input border glow |
| `shimmerBase` | #202020 | Loading shimmer base |
| `shimmerHighlight` | #383838 | Loading shimmer sweep |
| `shimmerAccent` | #8B9DC3 @ 8% | Accent shimmer sweep |
| `badgeBackground` | #303030 | Badge/chip background |
| `badgeText` | #E8E8E8 | Badge/chip text |
