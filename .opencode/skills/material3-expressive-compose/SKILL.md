---
name: material3-expressive-compose
description: Build and refactor Compose Multiplatform UI with Material 3 Expressive tokens, MOCCA dark theme constraints, and production-safe interaction patterns.
license: MIT
compatibility: opencode
metadata:
  platform: android-kmp
  focus: ui-ux
---

## What this skill does

- Designs and implements Jetpack/Compose Multiplatform UI using Material 3 Expressive primitives.
- Enforces MOCCA visual constraints: soft dark surfaces, neutral monochrome palette, subtle cool accent, rounded interactive shapes.
- Keeps screen architecture aligned with existing MVI flow (`ScreenModel -> StateFlow -> UI`).

## When to use

Use this skill for:

- New screens or section redesigns in `composeApp/src/commonMain/.../ui/`
- Component extraction for reusable cards, toolbars, lists, dialogs, and panels
- Visual consistency passes (spacing, typography hierarchy, contrast, elevation)

## MOCCA-specific rules

1. Never use `RectangleShape` for interactive elements.
2. Keep dark-first readability and avoid high-saturation accents.
3. Prefer `Surface` + `tonalElevation` for depth cues. `shadowElevation` is acceptable for modals (dialogs, modal sheets) needing extra visual separation.
4. Keep motion subtle and purposeful; no decorative animation. Use `MaterialTheme.motionScheme` as the standard motion API — do not create custom animation constant objects.
5. Match existing naming and state patterns in nearby UI modules.
6. In feature/UI code, use `AppColors`, `AppShapes`, `AppTypography`, `AppSpacing` — not `MaterialTheme.colorScheme` or `MaterialTheme.shapes`. Theme bridge code in `AppTheme.kt` legitimately uses MaterialTheme APIs to provision the M3 shell.

## Implementation playbook

1. Inspect nearby screens/components before editing.
2. Reuse existing theme tokens (`AppColors`, `AppShapes`, `AppTypography`, `AppSpacing`) and shape definitions.
3. Apply semantic typography (headline/title/body/label) over ad-hoc font sizes.
4. For motion, use `MaterialTheme.motionScheme` tokens (defaultSpatialSpec, defaultEffectsSpec, etc.) with `AppPerformance` toggle awareness.
5. Validate touch targets, contrast, and loading/empty/error states.
6. Run diagnostics and relevant build/test checks after edits.

## Done criteria

- UI composes without diagnostics errors.
- Interaction states are present (enabled, disabled, pressed, loading where relevant).
- Layout respects existing spacing rhythm and shape language.
- No architecture violations (UI stays declarative; business logic remains in ScreenModel/repository layers).

## Reference files

Adapted M3 Expressive reference material with MOCCA token mappings. All files are in `references/`:

| File | Content |
|------|---------|
| `references/token-reference.md` | Typography scale (15 baseline + extended styles with Space Grotesk values), shape tokens (M3 scale → AppShapes mapping), elevation levels, motion scheme reference (spring-based, `motionScheme` API) |
| `references/component-patterns.md` | Jetpack Compose component patterns — buttons, cards, text fields, dialogs, navigation, list items. Each with MOCCA token mapping (`AppShapes`, `AppColors`, `AppTypography`) |
| `references/color-system.md` | M3 color roles with MOCCA's fixed dark palette hex values, surface container hierarchy, color pairing rules, and MOCCA extended tokens (status colors, syntax highlighting, diff colors, utility colors) |
