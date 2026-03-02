# Battery/Thermal Optimization — Remove Animated Background

## TL;DR

> **Quick Summary**: Remove the always-on animated background (ASCII shader at 60fps + scanline overlay) that causes heat and battery drain. Replace with a static premium backdrop composable that preserves the Kyant0 liquid-glass aesthetic with zero per-frame cost.
> 
> **Deliverables**:
> - New `StaticPremiumBackdrop.kt` — static Canvas with radial gradient + optional noise grain
> - Updated `MainScreen.kt` — swap 2 animated component calls for 1 static call
> - Deleted: `FullScreenAsciiBackground.kt` (expect), `FullScreenAsciiBackground.android.kt` (actual), `ModernEffects.kt`
> 
> **Estimated Effort**: Short
> **Parallel Execution**: NO — sequential (T1 → T2 → T3 → F1)
> **Critical Path**: Task 1 → Task 2 → Task 3 → F1

---

## Context

### Original Request
The app runs a continuous full-screen ASCII shader (AGSL at 60fps on API 33+, CPU fallback at 30fps on older) plus a `ScanlineOverlay` infinite transition. Combined with the Kyant0 liquid glass blur, this causes significant battery drain and device heating when idling on MainScreen. Option A was selected: remove animation entirely, replace with a static premium backdrop.

### Research Findings
- **`FullScreenAsciiBackground.android.kt`**: Contains TWO `while(true) { withFrameNanos {} }` loops — one for GPU shader path (AGSL, 60fps), one for CPU fallback (30fps). Both run in `LaunchedEffect(Unit)` continuously.
- **`ModernEffects.kt` `ScanlineOverlay`**: Uses `rememberInfiniteTransition` with a 4-second tween loop, causing continuous recomposition.
- **`CRTNoiseOverlay`** in same file: dead code (empty body, zero callers).
- **Usage is isolated**: Both components called ONLY from `MainScreen.kt` lines 168 and 171, via wildcard import `com.mocca.app.ui.components.modern.*`.
- **expect/actual pair**: `FullScreenAsciiBackground.kt` (commonMain, 14 lines) is `expect`, `FullScreenAsciiBackground.android.kt` (androidMain, 183 lines) is `actual`. Both must be deleted together.
- **Glass integration**: `liquidBackdropSource(backdrop)` is on the parent `Box` (MainScreen line 165), so the new static backdrop is automatically captured for glass blur — no extra wiring.
- **Existing pattern**: `Brush.radialGradient` already used in `GlassModifier.kt:433` in commonMain — new component needs no expect/actual.
- **Color references**: Shader uses `half3(0.0, 0.08, 0.07)` ≈ `Color(0xFF001413)` for dark teal, `half3(0.0, 1.0, 0.80)` ≈ mint. `AppColors.accentGreen = Color(0xFF00D9A5)`.

### Metis Review
**Identified Gaps** (all addressed):
- Dark teal color `Color(0xFF001413)` should be inline, NOT added to `AppColors.kt` (single-use, avoids scope creep).
- Optional noise must be computed once in `remember {}`, NOT per-frame.
- Existing `rememberLiquidGlassState()` on line 135 is legacy/unrelated — must not be touched.
- Empty `androidMain/.../modern/` directory after deletion is fine (Gradle doesn't care, git doesn't track empty dirs).
- Glass blur quality will change (simpler background = less complex refraction) — conscious trade-off accepted.

---

## Work Objectives

### Core Objective
Eliminate all infinite rendering loops from the MainScreen background layer, reducing per-frame GPU/CPU cost to zero while maintaining enough visual contrast for the Kyant0 liquid glass blur to produce visible refraction.

### Concrete Deliverables
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/StaticPremiumBackdrop.kt` (NEW)
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt` (MODIFIED — lines 167-171 only)
- 3 files DELETED

### Definition of Done
- [x] No `while(true)` infinite loops running when app is idle on MainScreen
- [x] No `rememberInfiniteTransition` in background layer
- [x] Liquid glass effect (blur + vibrancy) still clearly visible due to subtle static background contrast
- [x] `./gradlew :androidApp:assembleDebug` exits with code 0

### Must Have
- Pitch-black OLED baseline (`AppColors.background`)
- Subtle mint/teal radial gradient so the glass refracts *something*
- Zero animation state in new backdrop component
- Single `modifier: Modifier` parameter (matching replaced component signature)

### Must NOT Have (Guardrails)
- NO animation APIs: `rememberInfiniteTransition`, `animateFloat`, `LaunchedEffect` with loops, `withFrameNanos`, `infiniteRepeatable`
- NO `RuntimeShader`, `AGSL`, or platform-specific APIs (commonMain only)
- NO expect/actual declarations in the new file
- NO modifications to `AppColors.kt`, `ModernGlassmorphism.kt`, or any file besides MainScreen.kt
- NO additional parameters beyond `modifier` (no `color`, `noiseEnabled`, `gradientCenter`)
- NO changes to MainScreen imports (wildcard handles everything)
- NO touching MainScreen lines outside the 167-171 range
- Max 50 lines for `StaticPremiumBackdrop.kt`
- If noise: max 200 dots, computed ONCE in `remember {}`, NOT inside Canvas draw scope

---

## Verification Strategy (MANDATORY)

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: NO
- **Automated tests**: None
- **Framework**: none
- **Agent-Executed QA**: Build verification + static analysis (grep for forbidden patterns)

### QA Policy
Every task MUST include agent-executed QA scenarios.
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately):
├── Task 1: Create StaticPremiumBackdrop.kt [visual-engineering]

Wave 2 (After Wave 1):
├── Task 2: Swap components in MainScreen.kt [quick]
├── Task 3: Delete dead files [quick]

Wave FINAL (After Wave 2):
├── Task F1: Build verification + code quality review [unspecified-high]

Critical Path: Task 1 → Task 2 → Task 3 → F1
```

Note: Tasks 2 and 3 CAN run in parallel (T2 removes usages, T3 deletes files — but since wildcard import means no import errors, and T2 just replaces calls, the files can be deleted simultaneously). However, for maximum safety, T2 before T3 is recommended.

### Dependency Matrix

| Task | Depends On | Blocks | Wave |
|------|-----------|--------|------|
| T1   | —         | T2, T3 | 1    |
| T2   | T1        | F1     | 2    |
| T3   | T1, T2    | F1     | 2    |
| F1   | T2, T3    | —      | FINAL|

### Agent Dispatch Summary

- **Wave 1** (1 task): T1 → `visual-engineering` + `taste-skill-compose`
- **Wave 2** (2 tasks): T2 → `quick`, T3 → `quick`
- **Wave FINAL** (1 task): F1 → `unspecified-high`

---

## TODOs

- [x] 1. Create StaticPremiumBackdrop

  **What to do**:
  - Create `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/StaticPremiumBackdrop.kt`
  - Implement a `@Composable fun StaticPremiumBackdrop(modifier: Modifier = Modifier)` using `Canvas` and `Brush.radialGradient`
  - Use a multi-stop radial gradient from dark teal center (`Color(0xFF001413)`) fading to `AppColors.background` (`#000000`) at edges
  - Optionally add a static noise/grain layer: pre-compute ~100-200 random `Offset` points in `remember {}`, draw them with `drawPoints(PointMode.Points)` at very low alpha (`0.03f-0.05f`) using `AppColors.accentGreen`
  - Add KDoc comment explaining purpose: "Static premium backdrop for MainScreen. Provides subtle visual contrast for Kyant0 liquid glass blur without any animation cost."
  - ZERO animation APIs — no `rememberInfiniteTransition`, no `LaunchedEffect`, no `withFrameNanos`, no `animateFloat`

  **Must NOT do**:
  - Add parameters beyond `modifier: Modifier = Modifier`
  - Use platform-specific APIs (no RuntimeShader, no AGSL)
  - Create expect/actual — this is commonMain only
  - Add colors to `AppColors.kt` — inline the dark teal literal
  - Use `Random` inside the `Canvas` draw block (must be in `remember`)
  - Exceed 50 lines total

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Canvas drawing, gradient design, visual composition — this is UI/visual work.
  - **Skills**: [`taste-skill-compose`]
    - `taste-skill-compose`: Compose UI patterns, Canvas API, theming conventions, glass effects guidance.
  - **Skills Evaluated but Omitted**:
    - `playwright`: No browser testing needed (Kotlin/Android)
    - `frontend-ui-ux`: Web-focused, not applicable to Compose/Kotlin

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1 (solo)
  - **Blocks**: Tasks 2, 3
  - **Blocked By**: None (can start immediately)

  **References** (CRITICAL):

  **Pattern References** (existing code to follow):
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/GlassModifier.kt:433` — Existing usage of `Brush.radialGradient` in commonMain Canvas context. Follow this pattern for gradient creation.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModernEffects.kt:19-61` — Structure reference for a simple composable in this package (package declaration, imports, KDoc, single @Composable fun with modifier param). Follow this file structure but WITHOUT any animation code.

  **API/Type References** (contracts to implement against):
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/FullScreenAsciiBackground.kt:14` — The function signature being replaced: `@Composable fun FullScreenAsciiBackground(modifier: Modifier = Modifier)`. Match this signature pattern.

  **External References** (libraries and frameworks):
  - Compose Canvas API: `androidx.compose.foundation.Canvas`, `Brush.radialGradient`, `drawRect`, `drawPoints`

  **WHY Each Reference Matters**:
  - `GlassModifier.kt:433`: Shows how to use `Brush.radialGradient` in this exact codebase — copy the pattern for correct import resolution
  - `ModernEffects.kt`: Shows the package structure, import style, and composable conventions in this directory
  - `FullScreenAsciiBackground.kt`: The exact API surface we're replacing — new component must be a drop-in substitute

  **Acceptance Criteria**:
  - [x] File exists at `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/StaticPremiumBackdrop.kt`
  - [x] File is ≤50 lines
  - [x] File contains `@Composable fun StaticPremiumBackdrop(modifier: Modifier = Modifier)`
  - [x] File contains `Canvas` and `Brush.radialGradient`
  - [x] File contains `package com.mocca.app.ui.components.modern`

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: No animation APIs present
    Tool: Bash (grep)
    Preconditions: File created
    Steps:
      1. grep -cE "rememberInfiniteTransition|animateFloat|LaunchedEffect|withFrameNanos|while\s*\(true\)|infiniteRepeatable" composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/StaticPremiumBackdrop.kt
    Expected Result: Output is "0"
    Failure Indicators: Output > 0 means animation APIs leaked in
    Evidence: .sisyphus/evidence/task-1-no-animation.txt

  Scenario: File size check
    Tool: Bash (wc)
    Preconditions: File created
    Steps:
      1. wc -l composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/StaticPremiumBackdrop.kt
    Expected Result: Line count ≤ 50
    Failure Indicators: Line count > 50 means over-engineering
    Evidence: .sisyphus/evidence/task-1-size-check.txt

  Scenario: Build succeeds with new file
    Tool: Bash
    Preconditions: File created (MainScreen not yet modified)
    Steps:
      1. ./gradlew :androidApp:assembleDebug 2>&1 | tail -5
    Expected Result: "BUILD SUCCESSFUL" in output, exit code 0
    Failure Indicators: Compilation errors mentioning StaticPremiumBackdrop
    Evidence: .sisyphus/evidence/task-1-build.txt
  ```

  **Commit**: YES
  - Message: `feat(ui): add StaticPremiumBackdrop for battery-friendly background`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/StaticPremiumBackdrop.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [x] 2. Replace animated backgrounds in MainScreen.kt

  **What to do**:
  - Edit `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt`
  - Replace lines 167-171 (the comment + `FullScreenAsciiBackground` call + empty line + comment + `ScanlineOverlay` call) with a single line: `StaticPremiumBackdrop(modifier = Modifier.fillMaxSize())`
  - Keep surrounding code (line 166 opening brace, line 172+ onwards) exactly as-is
  - Do NOT touch imports — the wildcard `import com.mocca.app.ui.components.modern.*` resolves `StaticPremiumBackdrop` automatically

  **Must NOT do**:
  - Modify ANY imports
  - Touch ANY lines outside the 167-171 range
  - Add new parameters to the `StaticPremiumBackdrop` call
  - Touch `rememberLiquidGlassState()`, `liquidBackdropSource`, or any glass-related code

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple 5-line replacement in a single file. Pure text substitution.
  - **Skills**: []
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: Not needed for line replacement

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 3, after Task 1 completes)
  - **Parallel Group**: Wave 2 (with Task 3)
  - **Blocks**: Task F1
  - **Blocked By**: Task 1

  **References** (CRITICAL):

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt:160-175` — The exact code region to modify. Lines 167-171 are the target. Line 166 (`}) {`) and line 172+ are the anchors that must NOT change.

  **WHY Each Reference Matters**:
  - `MainScreen.kt:160-175`: The executor needs to see the exact lines being replaced and the surrounding context to make a precise edit without collateral damage.

  **Acceptance Criteria**:
  - [x] `FullScreenAsciiBackground` no longer appears in `MainScreen.kt`
  - [x] `ScanlineOverlay` no longer appears in `MainScreen.kt`
  - [x] `StaticPremiumBackdrop` appears exactly once in `MainScreen.kt`
  - [x] No imports were added or removed

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Old components removed
    Tool: Bash (grep)
    Preconditions: MainScreen.kt edited
    Steps:
      1. grep -c "FullScreenAsciiBackground\|ScanlineOverlay" composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt
    Expected Result: Output is "0"
    Failure Indicators: Any match means old components weren't fully removed
    Evidence: .sisyphus/evidence/task-2-old-removed.txt

  Scenario: New component present
    Tool: Bash (grep)
    Preconditions: MainScreen.kt edited
    Steps:
      1. grep -c "StaticPremiumBackdrop" composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt
    Expected Result: Output is "1"
    Failure Indicators: Output is 0 (not added) or >1 (duplicated)
    Evidence: .sisyphus/evidence/task-2-new-present.txt

  Scenario: Build succeeds after swap
    Tool: Bash
    Preconditions: Task 1 complete + MainScreen edited
    Steps:
      1. ./gradlew :androidApp:assembleDebug 2>&1 | tail -5
    Expected Result: "BUILD SUCCESSFUL" in output, exit code 0
    Failure Indicators: Unresolved reference errors
    Evidence: .sisyphus/evidence/task-2-build.txt
  ```

  **Commit**: NO (groups with Task 3)

- [x] 3. Delete dead animated background files

  **What to do**:
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/FullScreenAsciiBackground.kt` (the `expect` declaration, 14 lines)
  - Delete `composeApp/src/androidMain/kotlin/com/mocca/app/ui/components/modern/FullScreenAsciiBackground.android.kt` (the `actual` implementation, 183 lines)
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModernEffects.kt` (contains only `ScanlineOverlay` + dead `CRTNoiseOverlay`, 66 lines)

  **Must NOT do**:
  - Delete any other files
  - Delete the `androidMain/.../modern/` directory itself (empty dir is fine)
  - Modify any remaining files

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: 3 file deletions, no logic involved.
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 2, after Task 1 completes)
  - **Parallel Group**: Wave 2 (with Task 2)
  - **Blocks**: Task F1
  - **Blocked By**: Task 1 (need replacement to exist), Task 2 (need usages removed first — safer order)

  **References** (CRITICAL):

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/FullScreenAsciiBackground.kt` — expect declaration to delete (14 lines)
  - `composeApp/src/androidMain/kotlin/com/mocca/app/ui/components/modern/FullScreenAsciiBackground.android.kt` — actual implementation to delete (183 lines)
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModernEffects.kt` — ScanlineOverlay + dead CRTNoiseOverlay to delete (66 lines)

  **WHY Each Reference Matters**:
  - All 3 files contain the exact animated code being removed. The expect+actual MUST be deleted together or the build fails.

  **Acceptance Criteria**:
  - [x] `FullScreenAsciiBackground.kt` does not exist
  - [x] `FullScreenAsciiBackground.android.kt` does not exist
  - [x] `ModernEffects.kt` does not exist
  - [x] No remaining `.kt` files reference `FullScreenAsciiBackground`, `ScanlineOverlay`, or `CRTNoiseOverlay`

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: All 3 files deleted
    Tool: Bash
    Preconditions: Files removed
    Steps:
      1. test ! -f composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/FullScreenAsciiBackground.kt && echo "PASS" || echo "FAIL"
      2. test ! -f composeApp/src/androidMain/kotlin/com/mocca/app/ui/components/modern/FullScreenAsciiBackground.android.kt && echo "PASS" || echo "FAIL"
      3. test ! -f composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModernEffects.kt && echo "PASS" || echo "FAIL"
    Expected Result: All 3 output "PASS"
    Failure Indicators: Any "FAIL" means a file wasn't deleted
    Evidence: .sisyphus/evidence/task-3-files-deleted.txt

  Scenario: No dangling references
    Tool: Bash (grep)
    Preconditions: All 3 files deleted + MainScreen updated (Task 2)
    Steps:
      1. grep -r "FullScreenAsciiBackground\|ScanlineOverlay\|CRTNoiseOverlay" composeApp/src/ --include="*.kt" | wc -l
    Expected Result: Output is "0"
    Failure Indicators: Any match means dangling references remain
    Evidence: .sisyphus/evidence/task-3-no-references.txt

  Scenario: Build succeeds after deletion
    Tool: Bash
    Preconditions: Tasks 1 and 2 complete + files deleted
    Steps:
      1. ./gradlew :androidApp:assembleDebug 2>&1 | tail -5
    Expected Result: "BUILD SUCCESSFUL" in output, exit code 0
    Failure Indicators: "Unresolved reference" or "expect declaration has no actual" errors
    Evidence: .sisyphus/evidence/task-3-build.txt
  ```

  **Commit**: YES (combined with Task 2)
  - Message: `refactor(ui): replace animated background with static premium backdrop for battery optimization`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt`, deleted files
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 4 review agents run in PARALLEL. ALL must APPROVE. Rejection → fix → re-run.

- [x] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, grep patterns). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in `.sisyphus/evidence/`. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [x] F2. **Code Quality Review** — `unspecified-high`
  Run `./gradlew :androidApp:assembleDebug`. Review the new `StaticPremiumBackdrop.kt` for: animation API leaks, excessive complexity, proper `remember` usage for noise, correct package declaration, proper imports. Verify total line count ≤ 50.
  Output: `Build [PASS/FAIL] | New File [CLEAN/N issues] | VERDICT`

- [x] F3. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff (`git diff`). Verify 1:1 — everything in spec was built (no missing), nothing beyond spec was built (no creep). Check "Must NOT do" compliance. Detect cross-task contamination: verify ONLY `MainScreen.kt` was modified and ONLY 3 files were deleted + 1 created. Flag unaccounted changes.
  Output: `Tasks [N/N compliant] | Files Modified [expected/actual] | Unaccounted [CLEAN/N files] | VERDICT`

- [x] F4. **Dead Code Sweep** — `unspecified-high`
  Run comprehensive grep for ALL symbols that were in the deleted files: `FullScreenAsciiBackground`, `AgslAsciiBackground`, `CpuAsciiBackground`, `ScanlineOverlay`, `CRTNoiseOverlay`, `ASCII_CHARS`, `CPU_COLS`, `CPU_ROWS`, `BOTTOM_ROWS`, `buildAsciiFrame`, `AGSL_SOURCE`. Zero hits expected across entire `composeApp/src/`. Also verify no new animation APIs were introduced in `StaticPremiumBackdrop.kt`.
  Output: `Symbols [N/N clean] | Animation APIs [CLEAN/N found] | VERDICT`

---

## Commit Strategy

- **Commit 1** (after Task 1): `feat(ui): add StaticPremiumBackdrop for battery-friendly background` — `StaticPremiumBackdrop.kt`
- **Commit 2** (after Tasks 2+3): `refactor(ui): replace animated background with static premium backdrop for battery optimization` — `MainScreen.kt` + 3 deleted files

---

## Success Criteria

### Verification Commands
```bash
# Build check
./gradlew :androidApp:assembleDebug
# Expected: BUILD SUCCESSFUL

# No animation APIs in new file
grep -cE "rememberInfiniteTransition|animateFloat|LaunchedEffect|withFrameNanos|while\s*\(true\)|infiniteRepeatable" composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/StaticPremiumBackdrop.kt
# Expected: 0

# No dangling references
grep -r "FullScreenAsciiBackground\|ScanlineOverlay\|CRTNoiseOverlay" composeApp/src/ --include="*.kt" | wc -l
# Expected: 0

# New component in MainScreen
grep -c "StaticPremiumBackdrop" composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt
# Expected: 1
```

### Final Checklist
- [x] All "Must Have" present (pitch-black base, mint gradient, zero animation, single modifier param)
- [x] All "Must NOT Have" absent (no animation APIs, no platform-specific code, no AppColors changes)
- [x] Build passes: `./gradlew :androidApp:assembleDebug` → exit code 0
- [x] Exactly 1 file created, 1 file modified, 3 files deleted
