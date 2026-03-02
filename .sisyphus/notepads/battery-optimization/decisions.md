# Decisions — battery-optimization

## [2026-03-02] Session ses_3512895c8ffeXYPA7XA7aGE8DJ

### D1: Delete entire ModernEffects.kt (not just ScanlineOverlay)
CRTNoiseOverlay has empty body and zero callers. Entire file is dead after ScanlineOverlay removal.

### D2: No expect/actual for StaticPremiumBackdrop
Brush.radialGradient already available in commonMain (see GlassModifier.kt:433). CommonMain-only file suffices.

### D3: Dark teal Color(0xFF001413) stays inline
Single-use color. Adding to AppColors would be scope creep.

### D4: LiquidBackdrop tuning removed from scope
No chromatic aberration config found in MainScreen.kt. Deliverable N/A.

### D5: Battery > Visual complexity trade-off accepted
Simpler static gradient means less complex backdrop refraction. Accepted as conscious trade-off.
