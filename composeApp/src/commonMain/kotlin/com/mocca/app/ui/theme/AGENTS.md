# UI THEME KNOWLEDGE BASE

**Updated:** 2026-04-12
**Scope:** `com.mocca.app.ui.theme`

## OVERVIEW
Theme/token source of truth for MOCCA: Material 3 Expressive shell, custom dark tonal palette, spacing, shapes, motion, focus, and code-font locals.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Theme root | `AppTheme.kt` | `MaterialExpressiveTheme`, `MoccaTheme`, `LocalCodeFontFamily` |
| Color tokens | `AppColors.kt` | Dark palette + status colors |
| Typography | `AppTypography.kt` | App fonts + code font selection |
| Shapes | `AppShapes.kt` | Squircle/card/pill definitions |
| Spacing | `AppSpacing.kt` | Layout constants used across screens |
| Motion | `AppAnimations.kt`, `MotionUtils.kt` | Expressive vs standard motion |
| Adaptive tuning | `AppPerformance.kt` | Performance-sensitive theme toggles |
| Focus polish | `FocusModifiers.kt` | Accessibility/focus affordances |

## CONVENTIONS
- `AppTheme {}` wraps the app; screens/components consume app tokens rather than inventing their own
- Use `AppColors`, `AppTypography`, `AppShapes`, `AppSpacing` directly for project styling
- `MoccaTheme` exists for unified token access outside plain composable parameter chains
- Code/editor font comes from `LocalCodeFontFamily` via preferences, not hardcoded monospace assumptions
- Expressive motion is allowed, but `AppPerformance` can downgrade to standard motion

## ANTI-PATTERNS
- Never treat raw Material defaults as the source of truth
- Never use `RectangleShape` for interactive surfaces
- Never scatter `Color(0xFF...)` literals through UI code when a token exists
- Never bypass token files for one-off spacing/shape systems in screens

## NOTES
- Root docs and README should describe the current Material 3 Expressive setup, not the older soft-dark wording alone
- This directory is foundational; changes ripple into `ui/components/` and `ui/screens/` immediately
