# Learnings — battery-optimization

## [2026-03-02] Session ses_3512895c8ffeXYPA7XA7aGE8DJ

### Project conventions
- Kotlin Multiplatform, Android-only target
- Build command: `./gradlew :androidApp:assembleDebug` (run from repo root)
- worktree: `/home/opencode/mocca-project-battery-opt`
- All work done inside worktree path
- Compose Multiplatform — commonMain Canvas APIs work fine (no expect/actual needed)
- `Brush.radialGradient` used in `GlassModifier.kt:433` — reference pattern for gradient in commonMain

### Task scope
- T1: CREATE `StaticPremiumBackdrop.kt` in `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/`
- T2: EDIT `MainScreen.kt` lines 167-171 only (replace 2 animated calls with 1 static call)
- T3: DELETE 3 files — FullScreenAsciiBackground.kt (expect), FullScreenAsciiBackground.android.kt (actual), ModernEffects.kt
- T1 → T2 → T3 sequence (strict)

### Color palette
- `AppColors.background = Color(0xFF000000)` — pitch black
- `AppColors.accentGreen = Color(0xFF00D9A5)` — mint green
- Dark teal inline literal: `Color(0xFF001413)` — NOT in AppColors, keep inline

### Verification (F1)
- Verified Must Have (4/4) constraints met perfectly.
- Verified Must NOT Have (8/8) guardrails strictly respected.
- Git diff confirms exactly 1 file created, 1 modified, and 3 deleted.
