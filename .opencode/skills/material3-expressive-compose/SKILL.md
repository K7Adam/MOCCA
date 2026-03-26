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
3. Prefer Material 3 `Surface` + tonal elevation for depth.
4. Keep motion subtle and purposeful; no decorative animation.
5. Match existing naming and state patterns in nearby UI modules.

## Implementation playbook

1. Inspect nearby screens/components before editing.
2. Reuse existing theme tokens and shape definitions.
3. Apply semantic typography (headline/title/body/label) over ad-hoc font sizes.
4. Validate touch targets, contrast, and loading/empty/error states.
5. Run diagnostics and relevant build/test checks after edits.

## Done criteria

- UI composes without diagnostics errors.
- Interaction states are present (enabled, disabled, pressed, loading where relevant).
- Layout respects existing spacing rhythm and shape language.
- No architecture violations (UI stays declarative; business logic remains in ScreenModel/repository layers).
