# MOCCA UI Modernization Notes

**Status:** Superseded by the current Material 3 Expressive implementation.

This file used to describe a broad UI redesign with OLED-only colors, glass effects, and speculative animation libraries. The active app has moved to a more maintainable Material 3 Expressive setup with app-owned tokens.

## Current Direction

- Use `AppTheme`, `AppColors`, `AppTypography`, and `AppShapes` as the feature UI source of truth.
- Use Material 3 Expressive components and `MaterialTheme.motionScheme` for motion.
- Keep developer-tool surfaces dense, readable, and action-oriented.
- Prefer state-holder improvements over decorative UI layers.
- Validate user-facing visual changes with emulator screenshots or Maestro flows.

## Avoid Reintroducing

- Liquid/glass dependency plans that are not present in `gradle/libs.versions.toml`.
- One-off animation constants outside the Material motion scheme.
- Marketing-style layouts for operational app screens.
- UI comments or docs that claim completed work without pointing to the owning code.

## Verification

```powershell
.\gradlew.bat :composeApp:allTests
.\gradlew.bat :androidApp:assembleDebug
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml
```

Run the Maestro command when the change affects visible flows.
