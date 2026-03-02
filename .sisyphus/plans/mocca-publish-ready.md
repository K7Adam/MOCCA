# MOCCA Publish-Ready: Full Feature Parity & Polish

## TL;DR

> **Quick Summary**: Bring the MOCCA KMP Compose Android app to publish-ready quality by removing dead code, fixing broken stubs, hardening error handling, adding missing OpenCode feature parity, and polishing the entire UX. No TODOs, no placeholders, no broken flows.
> 
> **Deliverables**:
> - All dead/orphaned code removed (ChatComponents.kt, WorktreeScreen, SessionsScreen+WorkspaceScreen chain, duplicate SkillsScreen, CrossProjectSessionsScreen)
> - All user-visible stubs fixed (Stashes tab, GlassBottomBar TODOs)
> - Error handling hardened across all repositories (no silent catch-null patterns)
> - Missing OpenCode parity features added (Command Palette completion, Session Sharing UI, Project switching, Provider/Model management, Session summarization)
> - ProGuard rules tightened for release build
> - Maestro E2E tests expanded for all critical flows
> - Complete UI/UX polish pass (animations, empty states, error states, loading states)
> 
> **Estimated Effort**: XL (40+ tasks across 4 phases)
> **Parallel Execution**: YES - 5 waves + final verification
> **Critical Path**: Dead code removal → Error hardening → Feature additions → Polish → QA

---

## Context

### Original Request
The user is building MOCCA as a reliable, robust, professional, performant, and fun-to-use Android client for the open-source agentic coding tool OpenCode. The app uses the OpenCode server (same backend powering CLI/TUI/Web/Desktop). The goal is publish-ready quality with full feature parity against the OpenCode web experience — no half-baked features, no broken flows, no TODOs in production code.

### Interview Summary
**Key Discussions**:
- MOCCA must match OpenCode web/TUI/desktop feature set while leveraging mobile-native advantages (offline cache, liquid glass UI, swipe panels, QR onboarding)
- Architecture: KMP Compose + Koin DI + Voyager nav + SQLDelight cache + MVI pattern
- Theme: Pitch Black OLED + Mint Green accents + Space Grotesk font + liquid glassmorphism
- 3-panel swipe layout in MainScreen is a core differentiator — preserve it

**Research Findings**:
- OpenCode server exposes 50+ endpoints; MOCCA API client covers most but UI doesn't expose all
- 6/8 screens initially flagged as "orphaned" were actually ACTIVE — only WorktreeScreen, SessionsScreen, CrossProjectSessionsScreen, and WorkspaceScreen are truly dead
- Stashes feature is FULLY IMPLEMENTED in GitRepository + GitScreenModel + Status tab, but the dedicated Stashes tab shows a "Not Fully Implemented" stub
- ChatComponents.kt has ZERO imports — confirmed dead code
- Duplicate SkillsScreen exists in two locations (skills/ and settings/)
- SettingsScreen navigates to `com.mocca.app.ui.screens.skills.SkillsScreen` (the correct one)
- GlassBottomBar TODOs are cosmetic polish items with working fallbacks
- ProGuard rules disable all minification (`-keep class com.mocca.app.** { *; }`)
- Only 6 Maestro E2E flows exist covering basic navigation

### Metis Review
**Identified Gaps** (addressed):
- Explore agent incorrectly classified 6/8 screens as orphaned — CORRECTED via direct verification
- Stash tab misdiagnosed as missing feature — it's just a stub hiding already-complete functionality
- WorkspaceScreen contains hardcoded prototype data, not real data
- ProGuard over-permissive for release builds
- Silent catch blocks in GitRepository (4 instances) swallow errors
- EventStreamRepository lacks per-event error isolation
- Duplicate SkillsScreen in two locations creates compilation ambiguity
- Component proliferation: multiple card/button variants need consolidation audit

---

## Work Objectives

### Core Objective
Transform MOCCA from a feature-rich but rough prototype into a polished, publish-ready Android app with zero broken flows, no dead code, robust error handling, and complete OpenCode feature parity.

### Concrete Deliverables
- Clean codebase with zero dead/orphaned files
- All screens functional end-to-end (no stubs, no "Not Implemented")
- Robust error handling (no silent catches, proper user feedback)
- Feature parity: Command Palette, Session Sharing, Project Switching, Provider Management, Session Summarization
- Tightened ProGuard rules for smaller release APK
- Expanded Maestro E2E test suite (20+ flows)
- Polished UI/UX with proper empty states, loading states, error states, and animations

### Definition of Done
- [ ] `./gradlew :androidApp:assembleDebug` succeeds with 0 errors
- [ ] `./gradlew :androidApp:assembleRelease` succeeds with 0 errors
- [ ] `grep -r "Not Fully Implemented\|Not Yet Implemented\|Coming Soon" composeApp/src/` returns 0 matches
- [ ] `grep -r "// TODO" composeApp/src/` returns 0 matches (all TODOs resolved)
- [ ] Zero orphaned screens reachable via DI but not navigated to
- [ ] All Maestro E2E smoke tests pass
- [ ] No silent `catch { null }` patterns in any repository

### Must Have
- All dead code removed and DI registrations cleaned
- Stashes tab shows actual stash list (not stub)
- Error handling hardened in all repositories
- Command Palette functional with slash commands
- Session sharing/unsharing UI
- ProGuard rules tightened for release
- All existing screens work end-to-end without crashes
- Proper loading, empty, and error states on every screen

### Must NOT Have (Guardrails)
- NO iOS/Desktop targets added
- NO changes to 3-panel swipe layout architecture
- NO new navigation framework (keep Voyager)
- NO new DI framework (keep Koin)
- NO theme changes (keep Pitch Black OLED + Mint Green)
- NO over-abstraction or premature generalization
- NO excessive JSDoc/KDoc on internal implementation details
- NO placeholder or stub UI in any user-facing screen
- NO `RectangleShape` for interactive elements
- NO direct `HttpClient` references — always use `ApiExecutor.execute {}`
- NO relative file paths
- NO blocking main thread operations

---

## Verification Strategy (MANDATORY)

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: YES — Maestro E2E in `maestro-workspace/`
- **Automated tests**: Tests-after (expand Maestro flows after implementation)
- **Framework**: Maestro E2E + Gradle build verification
- **Unit tests**: Not currently in project; adding unit test infrastructure is out of scope for this plan

### QA Policy
Every task MUST include agent-executed QA scenarios.
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Build verification**: `./gradlew :androidApp:assembleDebug` (every task)
- **Dead code verification**: `grep -r` for removed symbols
- **UI verification**: Maestro flows or android-mcp screenshots where applicable
- **Error handling**: Code review via `ast_grep_search` for catch patterns

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately — Dead Code Removal & Stub Fixes):
├── Task 1: Remove ChatComponents.kt dead code [quick]
├── Task 2: Remove duplicate SkillsScreen (settings/) [quick]
├── Task 3: Remove orphaned WorktreeScreen + WorktreeScreenModel [quick]
├── Task 4: Remove orphaned SessionsScreen → WorkspaceScreen dead chain [quick]
├── Task 5: Remove orphaned CrossProjectSessionsScreen [quick]
├── Task 6: Fix Stashes tab stub → show real stash list [quick]
├── Task 7: Resolve GlassBottomBar TODO comments [quick]
├── Task 8: Clean DI module registrations for removed screens [quick]

Wave 2 (After Wave 1 — Error Hardening & Robustness):
├── Task 9: Harden GitRepository silent catch blocks [unspecified-high]
├── Task 10: Add per-event error isolation in EventStreamRepository [deep]
├── Task 11: Harden ConnectionManager reconnection & error recovery [deep]
├── Task 12: Audit & fix ChatStateStore TODO sections [unspecified-high]
├── Task 13: Tighten ProGuard rules for release builds [quick]
├── Task 14: Remove hardcoded prototype data from Models.kt TODO comment [quick]

Wave 3 (After Wave 2 — Feature Parity: Core):
├── Task 15: Complete Command Palette with slash commands [deep]
├── Task 16: Add Session Sharing/Unsharing UI [unspecified-high]
├── Task 17: Add Session Summarization trigger [quick]
├── Task 18: Add Session Statistics display [quick]
├── Task 19: Add Agent Selection/Management UI to DashboardPanel [unspecified-high]
├── Task 20: Wire Provider OAuth flow in Settings [unspecified-high]

Wave 4 (After Wave 3 — Feature Parity: Enhanced + Polish):
├── Task 21: Add Project Switching UI [unspecified-high]
├── Task 22: Enhance Terminal screen from read-only to interactive [deep]
├── Task 23: Add proper empty states for all screens [visual-engineering]
├── Task 24: Add proper loading states (skeletons/shimmers) for all screens [visual-engineering]
├── Task 25: Add proper error states with retry for all screens [visual-engineering]
├── Task 26: Audit & consolidate duplicate UI components [unspecified-high]
├── Task 27: Polish animations & transitions across all screens [visual-engineering]

Wave 5 (After Wave 4 — QA & Release Prep):
├── Task 28: Expand Maestro E2E: Chat flow tests [unspecified-high]
├── Task 29: Expand Maestro E2E: Git operations tests [unspecified-high]
├── Task 30: Expand Maestro E2E: Settings & connection tests [unspecified-high]
├── Task 31: Expand Maestro E2E: Session lifecycle tests [unspecified-high]
├── Task 32: Expand Maestro E2E: Error state & edge case tests [unspecified-high]
├── Task 33: Final build verification (debug + release) [quick]

Wave FINAL (After ALL tasks — independent review, 4 parallel):
├── Task F1: Plan compliance audit (oracle)
├── Task F2: Code quality review (unspecified-high)
├── Task F3: Real manual QA (unspecified-high)
├── Task F4: Scope fidelity check (deep)

Critical Path: Task 1-8 → Task 9-14 → Task 15-20 → Task 21-27 → Task 28-33 → F1-F4
Parallel Speedup: ~65% faster than sequential
Max Concurrent: 8 (Wave 1)
```

### Dependency Matrix

| Task | Depends On | Blocks | Wave |
|------|-----------|--------|------|
| 1-7 | — | 8 | 1 |
| 8 | 1-7 | 9-14 | 1 |
| 9-14 | 8 | 15-20 | 2 |
| 15-20 | 9-14 | 21-27 | 3 |
| 21-27 | 15-20 | 28-33 | 4 |
| 28-33 | 21-27 | F1-F4 | 5 |
| F1-F4 | 28-33 | — | FINAL |

### Agent Dispatch Summary

- **Wave 1**: **8 tasks** — T1-T7 → `quick`, T8 → `quick`
- **Wave 2**: **6 tasks** — T9 → `unspecified-high`, T10-T11 → `deep`, T12 → `unspecified-high`, T13-T14 → `quick`
- **Wave 3**: **6 tasks** — T15 → `deep`, T16 → `unspecified-high`, T17-T18 → `quick`, T19-T20 → `unspecified-high`
- **Wave 4**: **7 tasks** — T21 → `unspecified-high`, T22 → `deep`, T23-T25 → `visual-engineering`, T26 → `unspecified-high`, T27 → `visual-engineering`
- **Wave 5**: **6 tasks** — T28-T32 → `unspecified-high`, T33 → `quick`
- **FINAL**: **4 tasks** — F1 → `oracle`, F2-F3 → `unspecified-high`, F4 → `deep`

---

## TODOs

> Implementation + Test = ONE Task. Never separate.
> EVERY task MUST have: Recommended Agent Profile + Parallelization info + QA Scenarios.

### Wave 1: Dead Code Removal & Stub Fixes

- [ ] 1. Remove ChatComponents.kt dead code

  **What to do**:
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatComponents.kt` entirely
  - Verify zero imports/references exist via `grep -r "ChatComponents" composeApp/src/`
  - Verify build compiles after removal

  **Must NOT do**:
  - Do NOT touch `ChatContent.kt` — that's the active chat rendering file
  - Do NOT modify any other chat-related files

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single file deletion with verification — trivial task
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Ensures proper cleanup of Kotlin file and any stale imports
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI work involved — pure deletion

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3, 4, 5, 6, 7)
  - **Blocks**: Task 8 (DI cleanup needs all deletions done first)
  - **Blocked By**: None (can start immediately)

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatComponents.kt` — The file to delete. Contains `MessageBubble`, `ReasoningBlock`, `ToolResultBlock` with old 2-param signatures
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatContent.kt` — The ACTIVE chat rendering. Uses `MessageBubble` from `ui/components/modern/MessageBubble.kt` with 12+ params — NOT from ChatComponents.kt

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/MessageBubble.kt` — The actual MessageBubble implementation used by ChatContent.kt

  **WHY Each Reference Matters**:
  - ChatComponents.kt: Confirms this file has zero imports (dead code). The executor should verify with grep before deleting.
  - ChatContent.kt: Shows the active code path — executor must NOT confuse the two files.
  - MessageBubble.kt: The real component. Executor should verify ChatContent.kt imports from HERE, not from ChatComponents.kt.

  **Acceptance Criteria**:
  - [ ] `grep -r "ChatComponents" composeApp/src/` returns 0 matches
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds
  - [ ] File `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatComponents.kt` no longer exists

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: ChatComponents.kt successfully removed with zero dangling references
    Tool: Bash
    Preconditions: File exists before task
    Steps:
      1. Run `grep -r "ChatComponents" composeApp/src/` — assert 0 matches
      2. Run `grep -r "import.*chat\.ChatComponents" composeApp/src/` — assert 0 matches
      3. Run `ls composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatComponents.kt` — assert file not found
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: File deleted, zero references, build passes
    Failure Indicators: Any grep match, build failure, file still exists
    Evidence: .sisyphus/evidence/task-1-chatcomponents-removal.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `chore: remove dead ChatComponents.kt`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatComponents.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 2. Remove duplicate SkillsScreen from settings/

  **What to do**:
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SkillsScreen.kt` (the DUPLICATE)
  - Keep `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/skills/SkillsScreen.kt` (the ACTIVE one referenced by SettingsScreen line 885)
  - Verify `SettingsScreen.kt` imports from `com.mocca.app.ui.screens.skills.SkillsScreen` (already confirmed)
  - Verify build compiles

  **Must NOT do**:
  - Do NOT delete the skills/ version — that's the active one
  - Do NOT modify SettingsScreen.kt navigation reference

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single file deletion after confirming which is active
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Ensures Kotlin package/import resolution is correct after deletion
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI work — pure cleanup

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3, 4, 5, 6, 7)
  - **Blocks**: Task 8 (DI cleanup)
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SkillsScreen.kt` — The DUPLICATE to delete. Has identical structure to skills/ version.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/skills/SkillsScreen.kt` — The ACTIVE version to keep. Referenced by SettingsScreen.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SettingsScreen.kt:885` — `navigator.push(com.mocca.app.ui.screens.skills.SkillsScreen)` — uses fully-qualified path to skills/ version
  - `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt:29` — `import com.mocca.app.ui.screens.skills.SkillsScreenModel` — DI imports from skills/ package

  **WHY Each Reference Matters**:
  - settings/SkillsScreen.kt: This is the duplicate to delete. Executor must verify it's truly unused.
  - skills/SkillsScreen.kt: This is the one to keep. SettingsScreen uses FQN to reference it.
  - SettingsScreen.kt:885: Confirms the active reference uses `com.mocca.app.ui.screens.skills.SkillsScreen`
  - Modules.kt:29: DI already imports from the correct `skills` package

  **Acceptance Criteria**:
  - [ ] `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SkillsScreen.kt` no longer exists
  - [ ] `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/skills/SkillsScreen.kt` still exists
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Duplicate SkillsScreen removed, correct one preserved
    Tool: Bash
    Preconditions: Both SkillsScreen files exist
    Steps:
      1. Run `ls composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SkillsScreen.kt` — assert file not found
      2. Run `ls composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/skills/SkillsScreen.kt` — assert file exists
      3. Run `grep -n "com.mocca.app.ui.screens.skills.SkillsScreen" composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SettingsScreen.kt` — assert match on line ~885
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Duplicate deleted, active preserved, navigation intact, build passes
    Failure Indicators: Build fails, wrong file deleted, navigation reference broken
    Evidence: .sisyphus/evidence/task-2-duplicate-skills-removal.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `chore: remove duplicate SkillsScreen from settings/`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SkillsScreen.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 3. Remove orphaned WorktreeScreen + WorktreeScreenModel

  **What to do**:
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/worktree/WorktreeScreen.kt`
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/worktree/WorktreeScreenModel.kt`
  - Remove the entire `worktree/` directory if empty after deletions
  - Verify no `navigator.push(WorktreeScreen` references exist (already confirmed: 0 matches)
  - Note: DI registration cleanup handled in Task 8

  **Must NOT do**:
  - Do NOT remove WorktreeRepository — it may be used by other features or could be wired to the worktree screen later
  - Do NOT touch FeatureFlagsScreen (it's ACTIVE — navigated from SettingsScreen)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Two file deletions in one directory
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Verify no stale references after deletion
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI changes

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 4, 5, 6, 7)
  - **Blocks**: Task 8
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/worktree/WorktreeScreen.kt` — The orphaned screen. Never referenced by any `navigator.push()` call.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/worktree/WorktreeScreenModel.kt` — The orphaned ScreenModel. Only referenced by its own screen and DI.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` — Has factory registration for WorktreeScreenModel (to be cleaned in Task 8)

  **WHY Each Reference Matters**:
  - WorktreeScreen.kt: Confirmed orphaned — `grep -r "navigator.push(WorktreeScreen" composeApp/src/` returns 0 matches. Safe to delete.
  - WorktreeScreenModel.kt: Only instantiated via DI for WorktreeScreen. No other consumers.
  - Modules.kt: Shows DI registration that becomes stale after deletion — handled by Task 8.

  **Acceptance Criteria**:
  - [ ] `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/worktree/` directory no longer exists
  - [ ] `grep -r "WorktreeScreen" composeApp/src/` returns only Modules.kt reference (cleaned in Task 8)
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds (DI references may need Task 8 first — verify)

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: WorktreeScreen and WorktreeScreenModel removed
    Tool: Bash
    Preconditions: Both files exist
    Steps:
      1. Run `ls composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/worktree/` — assert directory not found
      2. Run `grep -r "WorktreeScreen" composeApp/src/ --include="*.kt"` — assert only Modules.kt references remain (or zero if Task 8 ran)
      3. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Worktree directory deleted, no navigation references, build passes
    Failure Indicators: Directory still exists, unexpected references found, build failure
    Evidence: .sisyphus/evidence/task-3-worktree-removal.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `chore: remove orphaned WorktreeScreen`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/worktree/`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 4. Remove orphaned SessionsScreen → WorkspaceScreen dead navigation chain

  **What to do**:
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreen.kt`
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreenModel.kt`
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/workspace/WorkspaceScreen.kt` (and its directory if no other files)
  - Verify: `SessionsScreen` is never pushed to from ANY screen (confirmed: `grep -r "navigator.push(SessionsScreen" composeApp/src/` = 0 matches)
  - Verify: `WorkspaceScreen` is only pushed from `SessionsScreen` (which is itself unreachable)
  - WorkspaceScreen contains entirely hardcoded prototype data ("MCP SERVER Online 12ms latency", "feat: modular grid", etc.)
  - Note: ContextHistoryPanel in MainScreen's left swipe replaced SessionsScreen's session selection functionality
  - Note: DI cleanup in Task 8

  **Must NOT do**:
  - Do NOT delete `SessionRepository` — it's the active data layer used by ChatScreenModel and others
  - Do NOT delete `ContextHistoryPanel` — it replaced SessionsScreen's functionality
  - Do NOT delete ANY files from `sessions/` directory other than SessionsScreen.kt and SessionsScreenModel.kt (CrossProjectSessionsScreen handled in Task 5)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Delete 3 files in a dead navigation chain. All confirmed unreachable.
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Verify no stale references across codebase
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI changes

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3, 5, 6, 7)
  - **Blocks**: Task 8
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreen.kt` — Orphaned screen. `grep -r "navigator.push(SessionsScreen" composeApp/src/` returns 0 matches. Contains `navigator.push(WorkspaceScreen(sessionId))` on lines 67, 237.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreenModel.kt` — ScreenModel for the orphaned SessionsScreen.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/workspace/WorkspaceScreen.kt` — Only reachable from SessionsScreen (which is itself unreachable). Contains hardcoded prototype data, not real API data. Has `// TODO: Add SVG-like path drawing for git graph` comment.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/panels/ContextHistoryPanel.kt` — The REPLACEMENT for SessionsScreen. Provides session selection in MainScreen's left swipe panel.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` — Has factory registrations for SessionsScreenModel and WorkspaceScreenModel (if exists) — cleaned in Task 8.

  **WHY Each Reference Matters**:
  - SessionsScreen.kt: Orphaned. Its functionality lives in ContextHistoryPanel now. Safe to delete.
  - WorkspaceScreen.kt: Only entry point is the orphaned SessionsScreen. Contains entirely hardcoded data. Safe to delete.
  - ContextHistoryPanel.kt: The executor must understand this is the active replacement — do NOT confuse it with the deleted screens.
  - Modules.kt: DI registrations become stale. Cleaned in Task 8.

  **Acceptance Criteria**:
  - [ ] `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreen.kt` no longer exists
  - [ ] `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreenModel.kt` no longer exists
  - [ ] `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/workspace/` directory no longer exists
  - [ ] `grep -r "WorkspaceScreen" composeApp/src/ --include="*.kt"` returns only Modules.kt (cleaned in Task 8)
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Dead SessionsScreen → WorkspaceScreen chain removed
    Tool: Bash
    Preconditions: Files exist
    Steps:
      1. Run `ls composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreen.kt` — assert not found
      2. Run `ls composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreenModel.kt` — assert not found
      3. Run `ls composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/workspace/` — assert not found
      4. Run `grep -r "navigator.push(WorkspaceScreen" composeApp/src/` — assert 0 matches
      5. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: All 3 files deleted, no navigation references, build passes
    Failure Indicators: Files still exist, dangling references, build failure
    Evidence: .sisyphus/evidence/task-4-sessions-workspace-removal.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `chore: remove orphaned SessionsScreen and WorkspaceScreen`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreen.kt`, `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/SessionsScreenModel.kt`, `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/workspace/WorkspaceScreen.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 5. Remove orphaned CrossProjectSessionsScreen

  **What to do**:
  - Delete `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/CrossProjectSessionsScreen.kt`
  - Verify: `grep -r "navigator.push(CrossProject" composeApp/src/` = 0 matches (already confirmed)
  - Note: DI cleanup in Task 8 (Modules.kt:324 `factoryOf(::CrossProjectSessionsScreenModel)`)

  **Must NOT do**:
  - Do NOT touch other files in sessions/ directory (they may be needed)
  - Do NOT remove ProjectRepository — it's unrelated

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single file deletion, confirmed orphaned
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Verify clean removal
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI work

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3, 4, 6, 7)
  - **Blocks**: Task 8
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/CrossProjectSessionsScreen.kt` — Orphaned. Contains both `CrossProjectSessionsScreenModel` (class) and `CrossProjectSessionsScreen` (object Screen). Never referenced by any `navigator.push()` call.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt:28,324` — Import and factory registration for `CrossProjectSessionsScreenModel` — to be cleaned in Task 8.

  **WHY Each Reference Matters**:
  - CrossProjectSessionsScreen.kt: Confirmed zero push references. Self-contained file with model and screen. Safe to delete.
  - Modules.kt: DI registration for the ScreenModel that becomes stale.

  **Acceptance Criteria**:
  - [ ] `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/CrossProjectSessionsScreen.kt` no longer exists
  - [ ] `grep -r "CrossProjectSessionsScreen" composeApp/src/` returns only Modules.kt (cleaned in Task 8)
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: CrossProjectSessionsScreen removed
    Tool: Bash
    Preconditions: File exists
    Steps:
      1. Run `ls composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/CrossProjectSessionsScreen.kt` — assert not found
      2. Run `grep -r "CrossProjectSessionsScreen" composeApp/src/ --include="*.kt"` — assert only Modules.kt
      3. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: File deleted, only DI reference remains (cleaned in T8)
    Failure Indicators: File exists, unexpected references, build failure
    Evidence: .sisyphus/evidence/task-5-crossproject-removal.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `chore: remove orphaned CrossProjectSessionsScreen`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/sessions/CrossProjectSessionsScreen.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 6. Fix Stashes tab stub → show real stash list

  **What to do**:
  - Replace the placeholder `StashesTab` composable in `GitScreen.kt` (lines 805-814) with a proper stash list UI
  - The stash data and operations are ALREADY FULLY IMPLEMENTED:
    - `GitRepository.kt`: `listStashes()`, `createStash()`, `popStash()`, `applyStash()`, `dropStash()`
    - `GitScreenModel.kt`: All stash actions already wired (create, pop, apply, drop)
    - `GitScreen.kt` Status tab (lines ~426-471): Already shows stash list with create/pop/drop UI
  - The new StashesTab should show:
    - Full stash list from `uiState.stashes`
    - Create stash button (with optional message input)
    - Per-stash actions: Apply, Pop, Drop
    - Empty state when no stashes exist
  - Pattern: Follow the existing Status tab stash UI as reference, but make it the dedicated full stash management view
  - Remove the "Stashes Not Fully Implemented" text entirely

  **Must NOT do**:
  - Do NOT modify the Status tab's stash display — it can keep its summary view
  - Do NOT change GitRepository or GitScreenModel — they're already complete
  - Do NOT add new stash operations that don't exist in the repository

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Replacing a stub with UI that mirrors existing patterns already in the same file
  - **Skills**: [`taste-skill-compose`, `kotlin-best-practices`]
    - `taste-skill-compose`: Building proper Compose UI for the stash list with glassmorphic styling
    - `kotlin-best-practices`: Following MVI pattern — UI reads from uiState, calls screenModel actions
  - **Skills Evaluated but Omitted**:
    - `android-mcp`: Could verify on emulator but not needed for this scope

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3, 4, 5, 7)
  - **Blocks**: Task 8
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt:805-814` — The stub `StashesTab` to replace. Currently shows only an Icon + "Not Fully Implemented" text + stash count.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt:426-471` — The Status tab's stash section. Use this as the pattern for the new StashesTab — it shows stash list with create/pop/drop actions.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt:817-830` — The `TagsTab` composable. Shows LazyColumn pattern with item cards — good structural reference for StashesTab.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/GitRepository.kt` — `listStashes()`, `createStash(message?)`, `popStash(stashRef)`, `applyStash(stashRef)`, `dropStash(stashRef)` — ALL already implemented.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreenModel.kt` — All stash actions wired: `createStash()`, `popStash()`, `applyStash()`, `dropStash()`, stashes loaded in `loadGitData()`.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — `GitStash` data class with `index`, `message`, `branch` fields.

  **WHY Each Reference Matters**:
  - GitScreen.kt:805-814: The exact lines to replace. Shows the stub.
  - GitScreen.kt:426-471: The pattern to follow — existing stash UI in Status tab.
  - GitScreen.kt:817-830: TagsTab shows the LazyColumn card pattern for reference.
  - GitRepository: Confirms all operations are available — no new API work needed.
  - GitScreenModel: Confirms all actions are wired — no new ScreenModel work needed.
  - Models.kt: Shows the `GitStash` data class shape for UI rendering.

  **Acceptance Criteria**:
  - [ ] `grep -r "Not Fully Implemented" composeApp/src/` returns 0 matches
  - [ ] StashesTab shows a LazyColumn with stash items from `uiState.stashes`
  - [ ] Each stash item has Apply, Pop, Drop action buttons
  - [ ] Empty state shows appropriate message when no stashes exist
  - [ ] Create stash button exists and calls `screenModel.createStash()`
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Stashes tab shows proper stash list UI
    Tool: Bash
    Preconditions: GitScreen.kt compiled successfully
    Steps:
      1. Run `grep -r "Not Fully Implemented" composeApp/src/` — assert 0 matches
      2. Run `grep -n "StashesTab" composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt` — assert function exists
      3. Run `grep -n "LazyColumn\|stashes\|createStash\|popStash\|dropStash\|applyStash" composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt` — assert StashesTab contains stash list + actions
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Stashes tab renders real stash data with full CRUD actions
    Failure Indicators: "Not Fully Implemented" text remains, missing actions, build failure
    Evidence: .sisyphus/evidence/task-6-stashes-tab-fix.txt

  Scenario: Stashes tab handles empty state
    Tool: Bash (code inspection)
    Preconditions: Task implementation complete
    Steps:
      1. Read StashesTab function and verify it handles `uiState.stashes.isEmpty()` case
      2. Verify empty state shows meaningful message (not blank screen)
    Expected Result: Empty state UI exists with appropriate messaging
    Failure Indicators: No empty state handling, blank screen on empty list
    Evidence: .sisyphus/evidence/task-6-stashes-empty-state.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `fix: replace Stashes tab stub with real stash list UI`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 7. Resolve GlassBottomBar TODO comments

  **What to do**:
  - Replace the 3 `// TODO` comments in `GlassBottomBar.kt` (lines 283, 300, 317) with:
    - Either proper implementation of liquid glass provider integration (if feasible within existing AndroidLiquidGlass API)
    - Or clear documentation comments explaining why the fallback is used and that full integration requires upstream library changes
  - The goal is ZERO `// TODO` comments in production code
  - The fallback already works — GlassBottomBar renders correctly without the provider. These are cosmetic, not functional.

  **Must NOT do**:
  - Do NOT break the existing glass rendering — the fallback works fine
  - Do NOT add new dependencies for glass effects
  - Do NOT spend excessive time on perfect glass integration — this is a polish item

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Replace 3 TODO comments with either implementation or documentation
  - **Skills**: [`taste-skill-compose`]
    - `taste-skill-compose`: Understanding liquid glass API for potential implementation
  - **Skills Evaluated but Omitted**:
    - `kotlin-best-practices`: Not needed for comment resolution

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3, 4, 5, 6)
  - **Blocks**: Task 8
  - **Blocked By**: None

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/GlassBottomBar.kt:283` — `// TODO: Implement full liquid glass provider integration`
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/GlassBottomBar.kt:300` — `// TODO: Implement state sharing for true liquid glass effect`
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/GlassBottomBar.kt:317` — `// TODO: Implement source marking for liquid glass sampling`

  **External References**:
  - AndroidLiquidGlass library: `io.github.kyant0:backdrop` — Check if API supports the operations described in the TODOs
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/LiquidBackdrop.kt` — The backdrop implementation that GlassBottomBar integrates with
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/GlassSurface.kt` — Glass surface that handles the rendering

  **WHY Each Reference Matters**:
  - GlassBottomBar.kt TODO lines: The exact comments to resolve.
  - LiquidBackdrop.kt: Shows what glass API is available — helps decide if TODOs can be implemented or should be documented as known limitations.
  - GlassSurface.kt: The glass rendering pipeline — shows how glass effects work today.

  **Acceptance Criteria**:
  - [ ] `grep -n "// TODO" composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/GlassBottomBar.kt` returns 0 matches
  - [ ] GlassBottomBar still renders correctly (no visual regression)
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: All TODOs resolved in GlassBottomBar
    Tool: Bash
    Preconditions: File has 3 TODO comments
    Steps:
      1. Run `grep -c "// TODO" composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/GlassBottomBar.kt` — assert 0
      2. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Zero TODO comments, build passes
    Failure Indicators: TODO comments remain, build failure
    Evidence: .sisyphus/evidence/task-7-glassbottombar-todos.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `chore: resolve GlassBottomBar TODO comments`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/GlassBottomBar.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 8. Clean DI module registrations for all removed screens

  **What to do**:
  - Update `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` to remove:
    - Import and factory for `WorktreeScreenModel` (from Task 3)
    - Import and factory for `SessionsScreenModel` (from Task 4)
    - Import and factory for `CrossProjectSessionsScreenModel` (from Task 5, line 28 import, line 324 factory)
    - Any WorkspaceScreenModel reference if it exists (from Task 4)
  - Verify no stale imports remain for deleted screen packages
  - Run full build to confirm DI graph is still valid

  **Must NOT do**:
  - Do NOT remove ANY DI registrations for active screens
  - Do NOT remove repository registrations (SessionRepository, GitRepository, etc. are still active)
  - Do NOT modify DI registration patterns — keep the existing `factoryOf` / `singleOf` style

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Surgical edits to one file (Modules.kt) — remove specific lines
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Understanding Koin DI module structure and factory registration
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI work

  **Parallelization**:
  - **Can Run In Parallel**: NO — must run AFTER Tasks 1-7
  - **Parallel Group**: Sequential (after Wave 1 parallel group)
  - **Blocks**: Tasks 9-14 (Wave 2 depends on clean DI)
  - **Blocked By**: Tasks 1, 2, 3, 4, 5 (needs all deletions done first)

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` — The DI registration file. Contains all `factoryOf()` and `singleOf()` calls for ScreenModels, Repositories, and API clients.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt:28` — `import com.mocca.app.ui.screens.sessions.CrossProjectSessionsScreenModel` — stale after Task 5
  - `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt:324` — `factoryOf(::CrossProjectSessionsScreenModel)` — stale after Task 5

  **WHY Each Reference Matters**:
  - Modules.kt: The single DI configuration file. All stale registrations must be removed here to avoid runtime crashes from missing class references.
  - Line numbers are approximate — executor should search for the specific class names to find exact lines.

  **Acceptance Criteria**:
  - [ ] `grep -r "WorktreeScreenModel\|SessionsScreenModel\|CrossProjectSessionsScreenModel\|WorkspaceScreenModel" composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` returns 0 matches
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds
  - [ ] No runtime crash on app launch (DI graph resolves correctly)

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: All stale DI registrations removed
    Tool: Bash
    Preconditions: Tasks 1-7 complete, deleted screens exist
    Steps:
      1. Run `grep -c "WorktreeScreenModel\|SessionsScreenModel\|CrossProjectSessionsScreenModel\|WorkspaceScreenModel" composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` — assert 0
      2. Run `grep -c "import.*worktree\|import.*workspace" composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` — assert 0
      3. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Zero stale registrations, zero stale imports, build passes
    Failure Indicators: Stale references found, build failure, runtime DI crash
    Evidence: .sisyphus/evidence/task-8-di-cleanup.txt
  ```

  **Commit**: YES (groups with Wave 1)
  - Message: `chore: clean DI registrations for removed screens`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

---

### Wave 2: Error Hardening & Robustness

- [ ] 9. Harden GitRepository silent catch blocks

  **What to do**:
  - Find all `catch (e: Exception) { null }` patterns in `GitRepository.kt` (approximately 4 instances at lines ~315, 352, 387, 451)
  - Replace silent null returns with:
    - Logging the exception with context (which operation failed, what input)
    - Returning a meaningful error state that callers can surface to UI
  - Pattern: Use `Resource.Error(exception)` or log + return empty collection with warning
  - Review all catch blocks in the file — any that silently swallow exceptions should at minimum log

  **Must NOT do**:
  - Do NOT change the function signatures or return types
  - Do NOT add new dependencies for logging (use existing logging pattern in the codebase)
  - Do NOT surface raw exception messages to users — use friendly error messages
  - Do NOT break existing callers of GitRepository

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Requires careful analysis of error propagation paths through repository → ScreenModel → UI
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Proper Kotlin error handling patterns, Result type usage, Flow error handling
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI changes in this task

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 10, 11, 12, 13, 14)
  - **Blocks**: Tasks 15-20 (Wave 3 depends on robust data layer)
  - **Blocked By**: Task 8 (Wave 1 must be complete)

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/GitRepository.kt:315,352,387,451` — The 4 `catch { null }` blocks. Each is in a git operation parsing function that silently swallows errors.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/FileRepository.kt` — Check for comparison: how does FileRepository handle errors? Follow same pattern.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/SessionRepository.kt` — Largest repository. Check its error handling pattern for consistency.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — `Resource<T>` sealed class with `Success`, `Error`, `Loading` variants — use for error propagation

  **WHY Each Reference Matters**:
  - GitRepository catch blocks: The exact code to fix. Each returns `null` on error, causing empty data with no user feedback.
  - FileRepository/SessionRepository: Reference for consistent error handling patterns across repositories.
  - Resource<T>: The established pattern for propagating errors to UI layer.

  **Acceptance Criteria**:
  - [ ] `grep -c "catch.*null" composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/GitRepository.kt` returns 0 (no silent null catches)
  - [ ] All catch blocks either log the error or propagate it via Resource.Error
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: No silent catch-null patterns in GitRepository
    Tool: Bash
    Preconditions: GitRepository.kt has ~4 silent catch blocks
    Steps:
      1. Run `grep -n "catch" composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/GitRepository.kt` — list all catch blocks
      2. For each catch block, verify it either logs or returns Resource.Error (not silent null)
      3. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: All catch blocks have proper error handling
    Failure Indicators: Any catch block returns null without logging, build failure
    Evidence: .sisyphus/evidence/task-9-git-error-hardening.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: `fix: harden GitRepository error handling - no silent catches`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/GitRepository.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 10. Add per-event error isolation in EventStreamRepository

  **What to do**:
  - In `EventStreamRepository.kt` (1026 lines), find the main event processing loop in `handleEvent()` or the SSE collection flow
  - Wrap each individual event handler in a try-catch so a malformed event doesn't crash the entire SSE stream
  - Log malformed events with their raw content for debugging
  - Ensure the SSE collection continues after a single event processing failure
  - Audit the reconnection logic — ensure SSE reconnects gracefully after network drops

  **Must NOT do**:
  - Do NOT change the SSE protocol or event format
  - Do NOT suppress all errors — only isolate per-event failures from stream-level failures
  - Do NOT modify the event types or their data classes
  - Do NOT add retry logic for individual events (they're ephemeral)

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: 1026-line file with complex SSE streaming, state management, and concurrent event processing. Requires deep understanding of the streaming pipeline.
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Kotlin coroutine error handling, Flow exception handling, structured concurrency
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI changes

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 9, 11, 12, 13, 14)
  - **Blocks**: Tasks 15-20 (Wave 3 depends on robust event stream)
  - **Blocked By**: Task 8

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/EventStreamRepository.kt` — The entire file. Focus on `handleEvent()` method and the SSE collection flow. This is the most complex repository at 1026 lines.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaSseClient.kt` — The SSE client that feeds events into EventStreamRepository. Understand the event delivery mechanism.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — SSE event types and their data classes — understand what can be malformed
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ConnectionManager.kt` — Connection lifecycle — understand how stream failures affect connection state

  **WHY Each Reference Matters**:
  - EventStreamRepository.kt: The core file to modify. The event processing loop must be isolated per-event.
  - MoccaSseClient.kt: Understand the SSE delivery to know what error conditions are possible.
  - Models.kt: Event type definitions — helps identify which events could be malformed.
  - ConnectionManager.kt: Understand how stream errors propagate to connection state.

  **Acceptance Criteria**:
  - [ ] Each event type handler in EventStreamRepository is wrapped in try-catch
  - [ ] Malformed events are logged with context (event type, raw data)
  - [ ] SSE stream continues after individual event failures
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Event processing is isolated per-event
    Tool: Bash (code inspection)
    Preconditions: EventStreamRepository.kt exists
    Steps:
      1. Read the event handling method(s) in EventStreamRepository.kt
      2. Verify each event type branch has individual error handling
      3. Verify error logging includes event type and context
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Per-event isolation confirmed, build passes
    Failure Indicators: Shared catch block for all events, missing logging, build failure
    Evidence: .sisyphus/evidence/task-10-event-isolation.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: `fix: isolate per-event error handling in EventStreamRepository`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/EventStreamRepository.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 11. Harden ConnectionManager reconnection & error recovery

  **What to do**:
  - Audit `ConnectionManager.kt` for edge cases in reconnection logic:
    - Does it handle rapid connect/disconnect cycles? (debouncing)
    - Does it handle auth failures distinctly from network failures?
    - Does it have exponential backoff for reconnection attempts?
    - Does it properly clean up previous connections before reconnecting?
  - Ensure connection state transitions are correct and don't leave orphaned states
  - Verify health check failure doesn't crash the app
  - Ensure `Reconnecting` state is properly shown in UI during retries

  **Must NOT do**:
  - Do NOT change the ConnectionStatus enum values
  - Do NOT change the HttpClient creation pattern (ApiExecutor.execute pattern)
  - Do NOT add aggressive polling (respect server resources)

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: Connection lifecycle is critical infrastructure. Requires understanding of coroutine cancellation, state machines, and network edge cases.
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Coroutine lifecycle, state machine patterns, Ktor client configuration
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI changes (ConnectionStatus UI already exists)

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 9, 10, 12, 13, 14)
  - **Blocks**: Tasks 15-20
  - **Blocked By**: Task 8

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ConnectionManager.kt` — Primary file. Connection lifecycle, health checks, reconnection logic, HttpClient creation.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/ApiExecutor.kt` — The execution layer. Understand how callers interact with connections.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ConnectionStatus.kt` — UI component showing connection state — verify it handles all states properly.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ConnectionTroubleshooting.kt` — Troubleshooting UI — verify it appears on connection failures.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — `ConnectionStatus` enum: `NotConfigured`, `Disconnected`, `Connecting`, `Reconnecting`, `Connected`, `Error`

  **WHY Each Reference Matters**:
  - ConnectionManager.kt: The core file to harden.
  - ApiExecutor.kt: Shows how consumers use connections — any changes must be backward compatible.
  - ConnectionStatus.kt: Shows what states the UI handles — new states or transitions must be renderable.
  - ConnectionTroubleshooting.kt: The error recovery UI — should show actionable troubleshooting steps.

  **Acceptance Criteria**:
  - [ ] Reconnection uses exponential backoff (not fixed interval)
  - [ ] Auth failures (401) are handled distinctly from network failures (timeout/connection refused)
  - [ ] Rapid connect/disconnect cycles don't cause state corruption
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: ConnectionManager handles reconnection robustly
    Tool: Bash (code inspection)
    Preconditions: ConnectionManager.kt exists
    Steps:
      1. Read ConnectionManager.kt reconnection logic
      2. Verify exponential backoff implementation (increasing delays between retries)
      3. Verify auth failure (401) triggers different state than network failure
      4. Verify previous connection cleanup before new connection
      5. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Robust reconnection with backoff, distinct error types, clean teardown
    Failure Indicators: Fixed retry interval, no auth/network distinction, leaked connections
    Evidence: .sisyphus/evidence/task-11-connection-hardening.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: `fix: harden ConnectionManager reconnection with exponential backoff`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ConnectionManager.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 12. Audit & fix ChatStateStore TODO sections

  **What to do**:
  - `ChatStateStore.kt` has 2 TODO comments (lines 131, 444): `// TODO STATE` and `// TODO OPERATIONS`
  - Audit what these sections are supposed to contain
  - If they're section headers for existing code, replace with proper KDoc section comments
  - If they indicate missing functionality, implement it or document why it's deferred
  - Ensure ChatStateStore fully manages chat state without gaps

  **Must NOT do**:
  - Do NOT refactor ChatStateStore's architecture — just resolve the TODOs
  - Do NOT change its public API

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Requires understanding the ChatStateStore's role in the MVI architecture and what the TODO sections indicate
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Kotlin state management, StateFlow patterns
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI changes

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 9, 10, 11, 13, 14)
  - **Blocks**: Tasks 15-20
  - **Blocked By**: Task 8

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ChatStateStore.kt:131` — `// TODO STATE` — section marker or missing state?
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ChatStateStore.kt:444` — `// TODO OPERATIONS` — section marker or missing operations?
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatScreenModel.kt` — Primary consumer of ChatStateStore — shows what state/operations are actually used

  **WHY Each Reference Matters**:
  - ChatStateStore.kt:131,444: The exact TODO lines to resolve.
  - ChatScreenModel.kt: Shows what the store's consumers need — helps determine if TODOs indicate missing functionality.

  **Acceptance Criteria**:
  - [ ] `grep -c "// TODO" composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ChatStateStore.kt` returns 0
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: ChatStateStore TODOs resolved
    Tool: Bash
    Preconditions: File has 2 TODO comments
    Steps:
      1. Run `grep -c "// TODO" composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ChatStateStore.kt` — assert 0
      2. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Zero TODO comments, build passes
    Failure Indicators: TODO comments remain, build failure
    Evidence: .sisyphus/evidence/task-12-chatstate-todos.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: `chore: resolve ChatStateStore TODO comments`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ChatStateStore.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 13. Tighten ProGuard rules for release builds

  **What to do**:
  - Current `androidApp/proguard-rules.pro` has `-keep class com.mocca.app.** { *; }` which disables ALL minification
  - Replace with targeted keep rules:
    - Keep Koin module references (reflection-based DI)
    - Keep Ktor serialization models (JSON serialization uses reflection)
    - Keep SQLDelight generated code
    - Keep domain model classes used in API responses (JSON deserialization)
    - Keep Voyager Screen classes (navigation uses class references)
  - Verify release build succeeds with tightened rules
  - Compare APK sizes before/after

  **Must NOT do**:
  - Do NOT remove ALL ProGuard rules — some keeps are necessary for reflection
  - Do NOT enable obfuscation initially — focus on shrinking first
  - Do NOT break serialization or DI

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single config file change, but requires understanding of what needs reflection
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Android ProGuard/R8 rules for KMP + Koin + Ktor + Voyager
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI work

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 9, 10, 11, 12, 14)
  - **Blocks**: Tasks 15-20
  - **Blocked By**: Task 8

  **References**:

  **Pattern References**:
  - `androidApp/proguard-rules.pro` — The ProGuard rules file to modify. Currently has blanket keep rule.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` — Shows all Koin registrations — these classes need reflection keep rules
  - `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — Domain models used in JSON deserialization — must be kept
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — API client — shows which model classes are used in serialization

  **External References**:
  - Koin ProGuard rules: https://insert-koin.io/docs/reference/koin-android/instrumented-testing#keep-koin-classes
  - Ktor ProGuard rules: https://ktor.io/docs/client-serialization.html

  **WHY Each Reference Matters**:
  - proguard-rules.pro: The file to modify.
  - Modules.kt: Every class registered with Koin needs reflection access.
  - Models.kt: Every data class used in API responses must survive minification.
  - MoccaApiClient.kt: Shows the serialization entry points.

  **Acceptance Criteria**:
  - [ ] `./gradlew :androidApp:assembleRelease` succeeds with tightened rules
  - [ ] No `ClassNotFoundException` or `SerializationException` at runtime
  - [ ] APK size decreased compared to blanket keep rule

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Release build succeeds with tightened ProGuard rules
    Tool: Bash
    Preconditions: ProGuard rules updated
    Steps:
      1. Run `./gradlew :androidApp:assembleRelease` — assert BUILD SUCCESSFUL
      2. Check APK exists at `androidApp/build/outputs/apk/release/`
      3. Compare file size with previous release APK (if available)
    Expected Result: Release build succeeds, APK is smaller
    Failure Indicators: Build failure, missing APK, ProGuard errors in build log
    Evidence: .sisyphus/evidence/task-13-proguard-release.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: `build: tighten ProGuard rules for smaller release APK`
  - Files: `androidApp/proguard-rules.pro`
  - Pre-commit: `./gradlew :androidApp:assembleRelease`

- [ ] 14. Remove Models.kt TODO comment

  **What to do**:
  - Remove the `// TODO LIST MODELS` comment at line 809 of `Models.kt`
  - If this indicates missing model definitions, audit what's needed and either implement or document
  - Check if there's a corresponding TODO list model referenced elsewhere

  **Must NOT do**:
  - Do NOT refactor Models.kt — just resolve the TODO
  - Do NOT change existing model definitions

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single line TODO resolution
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Kotlin data class conventions
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: No UI work

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 9, 10, 11, 12, 13)
  - **Blocks**: Tasks 15-20
  - **Blocked By**: Task 8

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt:809` — `// TODO LIST MODELS` — section header or missing models?
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/chat/TodoListPanel.kt` — Todo list UI component — check what models it uses

  **WHY Each Reference Matters**:
  - Models.kt:809: The TODO to resolve. May be a section header for todo list models that already exist, or may indicate missing model definitions.
  - TodoListPanel.kt: Shows what todo models are actually consumed by UI.

  **Acceptance Criteria**:
  - [ ] `grep -c "// TODO" composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` returns 0
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Models.kt TODO resolved
    Tool: Bash
    Preconditions: File has TODO comment
    Steps:
      1. Run `grep -c "// TODO" composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — assert 0
      2. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Zero TODO comments, build passes
    Failure Indicators: TODO remains, build failure
    Evidence: .sisyphus/evidence/task-14-models-todo.txt
  ```

  **Commit**: YES (groups with Wave 2)
  - Message: `chore: resolve Models.kt TODO comment`
  - Files: `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

---

### Wave 3: Feature Parity — Core

- [ ] 15. Complete Command Palette with slash commands

  **What to do**:
  - `CommandPaletteOverlay.kt` already exists at `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/CommandPaletteOverlay.kt`
  - `CommandRepository.kt` already exists at `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/CommandRepository.kt`
  - MoccaApiClient has `listCommands()` endpoint
  - Audit the current implementation:
    - Does CommandPaletteOverlay fetch and display all server commands?
    - Does it support search/filter?
    - Does it execute selected commands?
    - Is it accessible from the chat input (e.g., typing `/` triggers it)?
  - Ensure full functionality:
    - Fetch commands from server via `listCommands()`
    - Display searchable/filterable list
    - Execute selected command via `executeCommand()` or `sendCommand()`
    - Trigger from chat input when user types `/`
    - Show command descriptions and parameter hints
    - Dismiss on backdrop tap or command execution

  **Must NOT do**:
  - Do NOT hardcode command list — fetch from server dynamically
  - Do NOT change the chat input TextField — add command palette as an overlay/sheet
  - Do NOT implement commands that don't exist on the server

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: Requires understanding chat input flow, command API, and overlay interaction patterns
  - **Skills**: [`taste-skill-compose`, `kotlin-best-practices`]
    - `taste-skill-compose`: Building the overlay/sheet UI with proper animations, search, and list rendering
    - `kotlin-best-practices`: MVI state management for command palette state, API integration
  - **Skills Evaluated but Omitted**:
    - `android-mcp`: Could verify on emulator but not in scope for this task

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 16, 17, 18, 19, 20)
  - **Blocks**: Tasks 21-27 (Wave 4)
  - **Blocked By**: Tasks 9-14 (Wave 2 must be complete)

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/CommandPaletteOverlay.kt` — Existing command palette component. Audit its current state and complete it.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/navigation/ChatInputBar.kt` — The chat input bar. Check if `/` trigger for command palette exists.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/SuggestionPopup.kt` — Suggestion popup pattern — could be used as reference for command completion UI.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/CommandRepository.kt` — Command data layer — `listCommands()` fetches from server
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — `listCommands()` and `executeCommand()` endpoints

  **WHY Each Reference Matters**:
  - CommandPaletteOverlay.kt: The starting point — audit what exists and complete what's missing.
  - ChatInputBar.kt: The integration point — `/` trigger must connect here.
  - CommandRepository.kt: The data source — server commands must flow through here.
  - MoccaApiClient: The API endpoints for commands.

  **Acceptance Criteria**:
  - [ ] Command palette displays all server commands fetched from `listCommands()`
  - [ ] Search/filter works to narrow command list
  - [ ] Selecting a command executes it
  - [ ] Typing `/` in chat input triggers command palette
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Command palette displays and filters commands
    Tool: Bash (code inspection)
    Preconditions: CommandPaletteOverlay.kt updated
    Steps:
      1. Verify CommandPaletteOverlay calls `listCommands()` or receives commands from ScreenModel
      2. Verify search/filter TextField exists in the overlay
      3. Verify command selection triggers execution
      4. Verify ChatInputBar has `/` detection for palette trigger
      5. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Full command palette functionality
    Failure Indicators: Hardcoded commands, no search, no execution, no `/` trigger
    Evidence: .sisyphus/evidence/task-15-command-palette.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: `feat: complete command palette with slash command support`
  - Files: `CommandPaletteOverlay.kt`, `ChatInputBar.kt`, `CommandRepository.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 16. Add Session Sharing/Unsharing UI

  **What to do**:
  - OpenCode server supports `shareSession(sessionId)` and `unshareSession(sessionId)` endpoints
  - MoccaApiClient already has these methods implemented
  - Add UI in the chat screen or session context menu to:
    - Share current session (generates a share link/ID)
    - Unshare a previously shared session
    - Show share status indicator on shared sessions
    - Copy share link to clipboard
  - Check if `ForkSessionDialog.kt` exists — if so, follow its pattern for the share dialog

  **Must NOT do**:
  - Do NOT implement server-side sharing — just the client UI calling existing API
  - Do NOT add social sharing (Twitter, etc.) — just clipboard copy of share link

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: New UI dialog + state management + API integration
  - **Skills**: [`taste-skill-compose`, `kotlin-best-practices`]
    - `taste-skill-compose`: Share dialog UI with glassmorphic styling
    - `kotlin-best-practices`: MVI state for share status, API calls in ScreenModel
  - **Skills Evaluated but Omitted**:
    - `android-mcp`: Not needed for implementation

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 15, 17, 18, 19, 20)
  - **Blocks**: Tasks 21-27
  - **Blocked By**: Tasks 9-14

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ForkSessionDialog.kt` — Existing session action dialog. Follow this pattern for share dialog.
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatContent.kt` — Chat content with existing share dialog integration — check current state.

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — `shareSession(sessionId)`, `unshareSession(sessionId)` endpoints
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/SessionRepository.kt` — Session operations — check if share/unshare methods exist

  **WHY Each Reference Matters**:
  - ForkSessionDialog.kt: Pattern for session action dialogs.
  - ChatContent.kt: Integration point for share actions.
  - MoccaApiClient: The share/unshare endpoints.
  - SessionRepository: May already have share methods — or needs them added.

  **Acceptance Criteria**:
  - [ ] Share session action is accessible from chat screen or session context menu
  - [ ] Sharing calls server API and shows success/error feedback
  - [ ] Unshare action available for previously shared sessions
  - [ ] Share link can be copied to clipboard
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Session sharing UI is accessible and functional
    Tool: Bash (code inspection)
    Preconditions: Share UI implemented
    Steps:
      1. Verify share action button/menu exists in chat screen or session context
      2. Verify share dialog calls `shareSession()` API
      3. Verify clipboard copy functionality exists
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Full share/unshare UI with clipboard support
    Failure Indicators: No share button, no API call, no clipboard copy
    Evidence: .sisyphus/evidence/task-16-session-sharing.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: `feat: add session sharing/unsharing UI`
  - Files: `ChatContent.kt` or new dialog file, `SessionRepository.kt`, `ChatScreenModel.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 17. Add Session Summarization trigger

  **What to do**:
  - OpenCode server supports `summarizeSession(sessionId)` endpoint
  - MoccaApiClient has (or should have) this method
  - Add a "Summarize" action in the session context menu or chat screen
  - Show the generated summary in a dialog or inline in the session

  **Must NOT do**:
  - Do NOT implement summarization logic client-side — call server API
  - Do NOT auto-summarize sessions — make it user-triggered only

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple API call + UI button + result display
  - **Skills**: [`taste-skill-compose`, `kotlin-best-practices`]
    - `taste-skill-compose`: Summary display UI
    - `kotlin-best-practices`: MVI action handling

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 15, 16, 18, 19, 20)
  - **Blocks**: Tasks 21-27
  - **Blocked By**: Tasks 9-14

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatContent.kt` — Integration point for session actions
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ForkSessionDialog.kt` — Pattern for session action dialog

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — `summarizeSession()` endpoint (verify it exists)
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/SessionRepository.kt` — Repository method for summarization

  **WHY Each Reference Matters**:
  - ChatContent.kt: Where the summarize button lives.
  - MoccaApiClient: Must verify the endpoint exists or add it.
  - SessionRepository: Data layer for the summarize action.

  **Acceptance Criteria**:
  - [ ] "Summarize" action accessible from session context
  - [ ] Calls server API `summarizeSession()`
  - [ ] Shows summary result to user
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Session summarization trigger exists
    Tool: Bash (code inspection)
    Preconditions: Summarize action implemented
    Steps:
      1. Verify summarize action exists in UI
      2. Verify it calls server API
      3. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Summarize action triggers server call and displays result
    Failure Indicators: No action, no API call, build failure
    Evidence: .sisyphus/evidence/task-17-session-summarize.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: `feat: add session summarization trigger`
  - Files: `ChatContent.kt`, `SessionRepository.kt`, `MoccaApiClient.kt` (if endpoint missing)
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 18. Add Session Statistics display

  **What to do**:
  - OpenCode server has `/session/status` endpoint returning session statistics (token usage, cost, timing)
  - Add a session stats display — either in the session header, context panel, or a dedicated info sheet
  - Show: token count, model used, duration, message count

  **Must NOT do**:
  - Do NOT add complex analytics — just display what the server provides
  - Do NOT add cost tracking if server doesn't provide it

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple API call + display component
  - **Skills**: [`taste-skill-compose`, `kotlin-best-practices`]
    - `taste-skill-compose`: Stats display UI
    - `kotlin-best-practices`: API integration

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 15, 16, 17, 19, 20)
  - **Blocks**: Tasks 21-27
  - **Blocked By**: Tasks 9-14

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/panels/ContextHistoryPanel.kt` — The left panel showing session info — good location for session stats
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/panels/DashboardPanel.kt` — Dashboard with module cards — alternative location

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — Check for `getSessionStatus()` endpoint
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/SessionRepository.kt` — Repository method for session status

  **WHY Each Reference Matters**:
  - ContextHistoryPanel: Natural location for session stats alongside session list.
  - MoccaApiClient: Must verify/add the session status endpoint.

  **Acceptance Criteria**:
  - [ ] Session stats are displayed in the UI (token count, model, duration)
  - [ ] Stats fetched from server API
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Session statistics displayed
    Tool: Bash (code inspection)
    Preconditions: Stats UI implemented
    Steps:
      1. Verify stats component exists in UI
      2. Verify it fetches from server API
      3. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Stats visible, data from server
    Failure Indicators: No stats UI, hardcoded data, build failure
    Evidence: .sisyphus/evidence/task-18-session-stats.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: `feat: add session statistics display`
  - Files: `ContextHistoryPanel.kt` or new component, `SessionRepository.kt`, `MoccaApiClient.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 19. Add Agent Selection/Management UI to DashboardPanel

  **What to do**:
  - OpenCode server has `listAgents()` endpoint returning available AI agents
  - MOCCA's DashboardPanel shows agents in a module card but no dedicated selection/management
  - Add:
    - Agent list showing all available agents with their capabilities
    - Agent selection — ability to choose which agent handles a session
    - Show current active agent prominently
  - Check `AgentRepository.kt` for existing data layer support

  **Must NOT do**:
  - Do NOT create a separate screen for agents — integrate into DashboardPanel or chat header
  - Do NOT implement agent capabilities that the server doesn't support

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: New UI section in DashboardPanel + state management + API integration
  - **Skills**: [`taste-skill-compose`, `kotlin-best-practices`]
    - `taste-skill-compose`: Agent list/selector UI with glassmorphic cards
    - `kotlin-best-practices`: MVI state management, API integration

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 15, 16, 17, 18, 20)
  - **Blocks**: Tasks 21-27
  - **Blocked By**: Tasks 9-14

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/panels/DashboardPanel.kt` — The dashboard panel — integration point for agent management UI
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModelSelectorDialog.kt` — Model selection pattern — follow this for agent selection
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModuleCard.kt` — Card component for dashboard modules

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/AgentRepository.kt` — Agent data layer — check existing methods
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — `listAgents()` endpoint
  - `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/Models.kt` — Agent model class

  **WHY Each Reference Matters**:
  - DashboardPanel: The integration point where agents section lives.
  - ModelSelectorDialog: Pattern for selection dialogs to follow.
  - AgentRepository: Existing data layer to build upon.

  **Acceptance Criteria**:
  - [ ] Agent list visible in DashboardPanel
  - [ ] Agent selection is functional (calls server to set active agent)
  - [ ] Current active agent is prominently displayed
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Agent management UI in DashboardPanel
    Tool: Bash (code inspection)
    Preconditions: Agent UI implemented
    Steps:
      1. Verify agent list section in DashboardPanel
      2. Verify agent selection calls AgentRepository/API
      3. Verify active agent indicator exists
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Functional agent management UI
    Failure Indicators: No agent section, no selection, build failure
    Evidence: .sisyphus/evidence/task-19-agent-management.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: `feat: add agent selection and management UI`
  - Files: `DashboardPanel.kt`, `DashboardScreenModel.kt`, `AgentRepository.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 20. Wire Provider OAuth flow in Settings

  **What to do**:
  - OpenCode server supports provider authentication including OAuth flows
  - `MoccaApiClient` has provider-related endpoints (`listProviders()`, auth methods)
  - `ProviderRepository.kt` exists for provider management
  - Ensure Settings screen has:
    - Provider list showing all configured providers
    - Authentication status per provider
    - OAuth flow trigger (opens browser for OAuth, handles callback)
    - Manual credential entry for non-OAuth providers
  - Check current SettingsScreen for existing provider UI

  **Must NOT do**:
  - Do NOT implement OAuth server-side — just trigger the flow via API and handle redirect
  - Do NOT store OAuth tokens client-side — the server manages tokens
  - Do NOT add providers that the server doesn't support

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: OAuth flow requires browser intent, callback handling, and provider management UI
  - **Skills**: [`kotlin-best-practices`]
    - `kotlin-best-practices`: Android OAuth integration patterns, Intent handling, Provider management
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: Provider list UI is straightforward

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 15, 16, 17, 18, 19)
  - **Blocks**: Tasks 21-27
  - **Blocked By**: Tasks 9-14

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SettingsScreen.kt` — Settings screen — integration point for provider management
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/mcp/McpOAuthDialog.kt` — Existing OAuth dialog for MCP — follow this pattern for provider OAuth

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ProviderRepository.kt` — Provider data layer
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — Provider endpoints: `listProviders()`, auth methods, OAuth

  **WHY Each Reference Matters**:
  - SettingsScreen: Where provider management UI lives.
  - McpOAuthDialog: Pattern for OAuth dialogs already in the codebase.
  - ProviderRepository: Existing data layer for provider operations.

  **Acceptance Criteria**:
  - [ ] Provider list visible in Settings
  - [ ] Auth status shown per provider
  - [ ] OAuth flow can be triggered for OAuth-supporting providers
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Provider management in Settings
    Tool: Bash (code inspection)
    Preconditions: Provider UI implemented
    Steps:
      1. Verify provider list section in SettingsScreen
      2. Verify auth status indicators per provider
      3. Verify OAuth trigger mechanism exists
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Provider management with auth flows
    Failure Indicators: No provider section, no auth status, no OAuth, build failure
    Evidence: .sisyphus/evidence/task-20-provider-oauth.txt
  ```

  **Commit**: YES (groups with Wave 3)
  - Message: `feat: wire provider OAuth flow and management UI`
  - Files: `SettingsScreen.kt`, `SettingsScreenModel.kt`, `ProviderRepository.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

---

### Wave 4: Feature Parity Enhanced + Polish

- [ ] 21. Add Project Switching UI

  **What to do**:
  - OpenCode server supports `listProjects()` and `getCurrentProject()` endpoints
  - MoccaApiClient has these methods; ProjectRepository exists
  - Currently no UI for switching between projects
  - Add project switching capability:
    - Project selector accessible from main screen header or dashboard
    - Show current project name prominently
    - List available projects with switch action
    - On project switch: refresh all data (sessions, files, git, etc.)
  - Pattern: Could be a dropdown in the top bar or a dialog like ModelSelectorDialog

  **Must NOT do**:
  - Do NOT implement project creation/deletion client-side
  - Do NOT cache project-specific data across project switches (clear and refresh)

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Requires understanding data refresh cascade on project switch
  - **Skills**: [`taste-skill-compose`, `kotlin-best-practices`]
    - `taste-skill-compose`: Project selector UI
    - `kotlin-best-practices`: State management for project switching cascade

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 22, 23, 24, 25, 26, 27)
  - **Blocks**: Tasks 28-33
  - **Blocked By**: Tasks 15-20

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModelSelectorDialog.kt` — Dialog pattern for selecting from a list
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt` — Main screen header where project name could be shown

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ProjectRepository.kt` — Project CRUD operations
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — `listProjects()`, `getCurrentProject()` endpoints

  **Acceptance Criteria**:
  - [ ] Current project name visible in UI
  - [ ] Project selector shows available projects
  - [ ] Switching project refreshes all data
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Project switching UI exists and is functional
    Tool: Bash (code inspection)
    Steps:
      1. Verify project name display in main screen
      2. Verify project selector fetches from ProjectRepository
      3. Verify project switch triggers data refresh
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Functional project switching
    Evidence: .sisyphus/evidence/task-21-project-switching.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Message: `feat: add project switching UI`
  - Files: `MainScreen.kt`, `MainScreenModel.kt`, `ProjectRepository.kt`, new dialog file
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 22. Enhance Terminal screen from read-only to interactive

  **What to do**:
  - Current `TerminalScreen.kt` is a read-only terminal viewer
  - OpenCode server supports `executeShell(sessionId, command)` for command execution
  - Enhance to support:
    - Command input field at the bottom
    - Command execution via server API
    - Real-time output display (streaming if possible)
    - Command history (up/down arrow or swipe)
    - Basic terminal styling (monospace font, ANSI color support if feasible)
  - Check WebSocket support in MoccaSseClient for real-time terminal output

  **Must NOT do**:
  - Do NOT implement a full terminal emulator with PTY — use server's shell execution API
  - Do NOT support interactive commands requiring stdin (use simple command-response model)
  - Do NOT add SSH/WebSocket terminal unless server already supports it

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: Terminal interaction requires streaming output handling, command history, and careful UX for input/output flow
  - **Skills**: [`taste-skill-compose`, `kotlin-best-practices`]
    - `taste-skill-compose`: Terminal UI with monospace font, input field, output scrolling
    - `kotlin-best-practices`: Streaming response handling, coroutine-based output collection

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 21, 23, 24, 25, 26, 27)
  - **Blocks**: Tasks 28-33
  - **Blocked By**: Tasks 15-20

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/terminal/TerminalScreen.kt` — Current terminal screen — enhance this
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/terminal/TerminalScreenModel.kt` — Terminal ScreenModel — add command execution here

  **API/Type References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/SessionRepository.kt` — `executeShell()` method for server-side command execution
  - `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt` — Shell execution endpoint

  **Acceptance Criteria**:
  - [ ] Command input field exists at bottom of terminal screen
  - [ ] Commands execute via server API and display output
  - [ ] Command history is navigable
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Terminal supports command input and execution
    Tool: Bash (code inspection)
    Steps:
      1. Verify command input TextField in TerminalScreen
      2. Verify command execution calls ScreenModel which calls Repository
      3. Verify output display in scrollable list
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Interactive terminal with command execution
    Evidence: .sisyphus/evidence/task-22-interactive-terminal.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Message: `feat: enhance terminal screen with interactive command execution`
  - Files: `TerminalScreen.kt`, `TerminalScreenModel.kt`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 23. Add proper empty states for all screens

  **What to do**:
  - Audit every screen for empty state handling:
    - GitScreen: no commits, no branches, no stashes, no tags
    - FilesScreen: empty project directory
    - McpScreen: no MCP servers configured
    - TerminalScreen: no terminal output
    - ChatScreen: new session with no messages
    - DashboardPanel: no data loaded
  - For each empty state:
    - Show relevant icon/illustration
    - Show descriptive message explaining the empty state
    - Show action button where applicable (e.g., "Add MCP Server" on empty MCP screen)
  - Follow existing empty state patterns in the codebase (if any)

  **Must NOT do**:
  - Do NOT show blank screens — every list/grid must have an empty state
  - Do NOT use generic "No data" messages — be specific to the context
  - Do NOT add animated illustrations (keep it simple with icons + text)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Pure UI work across multiple screens — consistent empty state design
  - **Skills**: [`taste-skill-compose`]
    - `taste-skill-compose`: Compose UI design for empty states with proper theming
  - **Skills Evaluated but Omitted**:
    - `kotlin-best-practices`: No business logic changes

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 21, 22, 24, 25, 26, 27)
  - **Blocks**: Tasks 28-33
  - **Blocked By**: Tasks 15-20

  **References**:

  **Pattern References**:
  - All screen files listed in Execution Strategy — each needs empty state audit
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt` — Check if branches/tags tabs have empty states
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/files/FilesScreen.kt` — Check empty directory state
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/mcp/McpScreen.kt` — Check empty servers state

  **Acceptance Criteria**:
  - [ ] Every LazyColumn/LazyGrid has an empty state when list is empty
  - [ ] Empty states show context-specific messages
  - [ ] Empty states show action buttons where applicable
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: All screens handle empty data gracefully
    Tool: Bash (code inspection)
    Steps:
      1. For each screen with a list, search for `.isEmpty()` or `items.size == 0` checks
      2. Verify each has an alternative UI branch for empty state
      3. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: All lists have empty state handling
    Evidence: .sisyphus/evidence/task-23-empty-states.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Message: `feat: add proper empty states for all screens`
  - Files: Multiple screen files
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 24. Add proper loading states (skeletons/shimmers) for all screens

  **What to do**:
  - Audit every screen for loading state handling:
    - When data is being fetched, show skeletons/shimmers instead of blank screens
    - `ShimmerModifier.kt` already exists — use it for loading placeholders
    - `MessageSkeleton.kt` already exists for chat — follow this pattern for other screens
  - Ensure `Resource.Loading` state shows appropriate loading UI on every screen

  **Must NOT do**:
  - Do NOT use plain CircularProgressIndicator as sole loading state — use skeletons/shimmers
  - Do NOT create new loading patterns — reuse `ShimmerModifier` and skeleton components

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Pure UI work — creating skeleton layouts for each screen
  - **Skills**: [`taste-skill-compose`]
    - `taste-skill-compose`: Shimmer/skeleton component creation

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 21, 22, 23, 25, 26, 27)
  - **Blocks**: Tasks 28-33
  - **Blocked By**: Tasks 15-20

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ShimmerModifier.kt` — Existing shimmer effect — use for loading placeholders
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/MessageSkeleton.kt` — Chat message skeleton — pattern for other screen skeletons

  **Acceptance Criteria**:
  - [ ] Every screen shows shimmer/skeleton during `Resource.Loading`
  - [ ] Skeletons match the layout of actual content
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Loading states use skeletons/shimmers
    Tool: Bash (code inspection)
    Steps:
      1. For each screen, search for `Resource.Loading` handling
      2. Verify skeleton/shimmer UI is shown, not blank screen
      3. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: All screens show proper loading states
    Evidence: .sisyphus/evidence/task-24-loading-states.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Message: `feat: add shimmer loading states for all screens`
  - Files: Multiple screen files
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 25. Add proper error states with retry for all screens

  **What to do**:
  - Audit every screen for error state handling:
    - When API calls fail, show user-friendly error with retry button
    - Show specific error messages (not generic "Something went wrong")
    - Distinguish between network errors, auth errors, and server errors
  - Use existing error handling patterns (ConnectionTroubleshooting as reference)

  **Must NOT do**:
  - Do NOT show raw exception messages to users
  - Do NOT hide errors silently — always show feedback
  - Do NOT auto-retry aggressively (respect server resources)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Error state UI design across multiple screens
  - **Skills**: [`taste-skill-compose`, `kotlin-best-practices`]
    - `taste-skill-compose`: Error state UI with retry buttons
    - `kotlin-best-practices`: Error propagation and Resource.Error handling

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 21, 22, 23, 24, 26, 27)
  - **Blocks**: Tasks 28-33
  - **Blocked By**: Tasks 15-20

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ConnectionTroubleshooting.kt` — Error/troubleshooting UI pattern
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ConnectionStatus.kt` — Connection error UI

  **Acceptance Criteria**:
  - [ ] Every screen handles `Resource.Error` with user-friendly message
  - [ ] Retry button exists on all error states
  - [ ] Error messages are contextual (not generic)
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Error states with retry on all screens
    Tool: Bash (code inspection)
    Steps:
      1. For each screen, verify `Resource.Error` handling
      2. Verify retry button triggers data reload
      3. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: All screens have error states with retry
    Evidence: .sisyphus/evidence/task-25-error-states.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Message: `feat: add error states with retry for all screens`
  - Files: Multiple screen files
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 26. Audit & consolidate duplicate UI components

  **What to do**:
  - Audit the 63 UI component files for duplicates/overlaps:
    - Card variants: `MoccaCard`, `GlassmorphicCard`, `ModuleCard` — which are actually used?
    - Button variants: `MoccaButton`, any `GodButton` references — which are standard?
    - Navigation: `GlassBottomBar`, `CompactNavBar`, `PersistentNavRow`, `MoccaBottomNavigation`, `UnifiedFloatingBottomBar` — which is the active nav?
  - For each duplicate cluster:
    - Identify which variant is actively used (via `grep` for imports)
    - Remove unused variants
    - Ensure consistent usage of the chosen variant
  - Focus on identifying and removing truly unused components, not on refactoring working code

  **Must NOT do**:
  - Do NOT merge components that serve different purposes
  - Do NOT change the visual appearance of active components
  - Do NOT refactor the glass component system — it's the design system

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Requires systematic audit of 63 files with import/usage analysis
  - **Skills**: [`kotlin-best-practices`, `taste-skill-compose`]
    - `kotlin-best-practices`: Import analysis, dead code detection
    - `taste-skill-compose`: Understanding which components serve distinct UI purposes

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 21, 22, 23, 24, 25, 27)
  - **Blocks**: Tasks 28-33
  - **Blocked By**: Tasks 15-20

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/` — All 63 component files — the audit scope
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/MoccaCard.kt` — Card variant 1
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/glass/GlassmorphicCard.kt` — Card variant 2
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModuleCard.kt` — Card variant 3

  **Acceptance Criteria**:
  - [ ] All unused component files identified and removed
  - [ ] No broken imports after removal
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Unused components removed
    Tool: Bash
    Steps:
      1. For each removed component, verify zero imports remain
      2. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Clean component directory, build passes
    Evidence: .sisyphus/evidence/task-26-component-audit.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Message: `chore: consolidate UI components, remove unused variants`
  - Files: Multiple component files
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

- [ ] 27. Polish animations & transitions across all screens

  **What to do**:
  - Audit screen transitions for smoothness:
    - Navigation transitions (push/pop animations)
    - 3-panel swipe transitions in MainScreen
    - Dialog open/close animations
    - List item appearance animations
  - Ensure consistent animation patterns:
    - Use same duration and easing curves throughout
    - Add enter/exit transitions to lazy list items
    - Smooth panel transitions in swipe layout
  - Check `ModernEffects.kt` for existing animation utilities
  - Add haptic feedback for key interactions (button taps, swipe completions, pull-to-refresh)

  **Must NOT do**:
  - Do NOT add animations that slow down perceived performance
  - Do NOT add excessive motion (respect reduced motion accessibility setting)
  - Do NOT change the swipe panel gesture behavior

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Pure animation and transition polish work
  - **Skills**: [`taste-skill-compose`]
    - `taste-skill-compose`: Compose animation APIs, transition specs, haptic feedback

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 21, 22, 23, 24, 25, 26)
  - **Blocks**: Tasks 28-33
  - **Blocked By**: Tasks 15-20

  **References**:

  **Pattern References**:
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModernEffects.kt` — Existing animation utilities
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt` — Swipe panel transitions
  - `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/navigation/SharedNavIndicator.kt` — Navigation indicator animations

  **Acceptance Criteria**:
  - [ ] Screen transitions are smooth and consistent
  - [ ] List item animations on appearance
  - [ ] Haptic feedback on key interactions
  - [ ] Respects reduced motion accessibility setting
  - [ ] `./gradlew :androidApp:assembleDebug` succeeds

  **QA Scenarios (MANDATORY):**
  ```
  Scenario: Animations are consistent and performant
    Tool: Bash (code inspection)
    Steps:
      1. Verify animation duration constants are centralized
      2. Verify reduced motion check exists
      3. Verify haptic feedback calls exist for buttons and swipe completions
      4. Run `./gradlew :androidApp:assembleDebug` — assert BUILD SUCCESSFUL
    Expected Result: Polished animations throughout
    Evidence: .sisyphus/evidence/task-27-animation-polish.txt
  ```

  **Commit**: YES (groups with Wave 4)
  - Message: `feat: polish animations, transitions, and haptic feedback`
  - Files: Multiple screen and component files
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

---

### Wave 5: QA & Release Prep


- [ ] 28. Maestro E2E: Chat Flow Tests

  **What to do**:
  - Create `maestro-workspace/flows/chat/send_message.yaml` — launch app, swipe to chat panel, tap input field, type a test message, tap send button, verify message appears in chat list, take screenshot
  - Create `maestro-workspace/flows/chat/message_streaming.yaml` — send a message (or mock via existing session), verify streaming indicator appears (shimmer/skeleton), wait for response to complete, verify response bubble renders with content, take screenshot
  - Create `maestro-workspace/flows/chat/abort_generation.yaml` — send a message, while streaming is active tap the abort/stop button, verify generation stops and partial response is preserved, take screenshot
  - Create `maestro-workspace/flows/chat/chat_input_interactions.yaml` — test multiline input (long press enter), test paste into input field, test input field auto-focus when navigating to chat, take screenshot of each state
  - Add all new flows to `maestro-workspace/testplans/regression.yaml`
  - Create `maestro-workspace/testplans/chat.yaml` test plan grouping all chat flows

  **Must NOT do**:
  - Do NOT modify any existing Maestro flows — only add new ones
  - Do NOT require a live OpenCode server connection for basic UI tests (test UI elements exist and respond, not server responses)
  - Do NOT use hardcoded sleep durations — use `extendedWaitUntil` with timeouts

  **Recommended Agent Profile**:
  > Maestro YAML flows following existing patterns, device automation focus.
  - **Category**: `unspecified-high`
    - Reason: E2E test authoring requires understanding both Maestro syntax and MOCCA's UI structure
  - **Skills**: [`android-mcp`]
    - `android-mcp`: ADB device automation, emulator interaction, screenshot capture
  - **Skills Evaluated but Omitted**:
    - `playwright`: Not applicable — Maestro is the E2E framework, not Playwright
    - `taste-skill-compose`: Not modifying UI code, only testing it

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 29, 30, 31, 32)
  - **Blocks**: Task 33 (final build verification)
  - **Blocked By**: Tasks 15-27 (all feature implementation must be complete before E2E testing)

  **References**:

  **Pattern References** (existing Maestro flows to follow):
  - `maestro-workspace/flows/sessions/create_new_session.yaml` — Session interaction pattern: launch → swipe → tap → screenshot. Follow this exact YAML structure
  - `maestro-workspace/flows/navigation/navigate_to_git.yaml` — Navigation flow pattern: swipe direction, `waitForAnimationToEnd`, optional taps with `id` regex
  - `maestro-workspace/subflows/common/launch_app.yaml` — Reusable launch subflow. Use `runFlow` to include this instead of duplicating launch logic
  - `maestro-workspace/testplans/regression.yaml` — Test plan format: `appId` header + `runFlow` entries. Add new flows here

  **API/Type References** (UI elements to target):
  - `composeApp/.../ui/components/navigation/ChatInputBar.kt` — Chat input bar composable. Look for `testTag` or semantic descriptions to use as Maestro selectors
  - `composeApp/.../ui/screens/chat/ChatScreen.kt` — Chat screen structure. Identify key UI landmarks for assertions
  - `composeApp/.../ui/components/modern/MessageBubble.kt` — Message rendering. Look for text content patterns to assert against

  **WHY Each Reference Matters**:
  - Existing flows establish the canonical Maestro YAML pattern — copy structure exactly
  - UI source files reveal `testTag` values and semantic descriptions usable as Maestro selectors
  - Test plan files show how to register new flows for CI execution

  **Acceptance Criteria**:
  - [ ] 4 new YAML flow files created in `maestro-workspace/flows/chat/`
  - [ ] Chat test plan created: `maestro-workspace/testplans/chat.yaml`
  - [ ] All 4 flows added to `maestro-workspace/testplans/regression.yaml`
  - [ ] Each flow follows existing pattern: `appId` header, tags, launch, actions, screenshot

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Chat flows parse and validate
    Tool: Bash
    Preconditions: Maestro CLI installed, MOCCA app installed on emulator
    Steps:
      1. Run `python -c "import yaml; yaml.safe_load(open('maestro-workspace/flows/chat/send_message.yaml'))"` for each file
      2. Verify each YAML file has required `appId: com.mocca.app` header
      3. Verify each YAML file has `tags` section with appropriate tags
      4. Verify each file ends with a `takeScreenshot` step
    Expected Result: All 4 YAML files are valid and parseable, all have required structure
    Failure Indicators: YAML parse error, missing appId, missing screenshot step
    Evidence: .sisyphus/evidence/task-28-chat-flows-validation.txt

  Scenario: Regression test plan includes all chat flows
    Tool: Bash
    Preconditions: Task 28 flows written
    Steps:
      1. Read `maestro-workspace/testplans/regression.yaml` and verify it contains `runFlow` entries for all 4 chat flows
      2. Read `maestro-workspace/testplans/chat.yaml` and verify it lists all 4 chat flows
    Expected Result: Both test plans reference all 4 new chat flow files
    Failure Indicators: Missing flow reference, wrong relative path
    Evidence: .sisyphus/evidence/task-28-testplan-check.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Message: `test: expand Maestro E2E test coverage`
  - Files: `maestro-workspace/flows/chat/*.yaml`, `maestro-workspace/testplans/chat.yaml`, `maestro-workspace/testplans/regression.yaml`
  - Pre-commit: YAML syntax validation

---

- [ ] 29. Maestro E2E: Git Operations Tests

  **What to do**:
  - Create `maestro-workspace/flows/git/view_git_status.yaml` — launch app, swipe left to dashboard, tap Git, verify Status tab is visible with file list or empty state message, take screenshot
  - Create `maestro-workspace/flows/git/view_git_diff.yaml` — navigate to Git screen, tap on a file entry (if available) or verify diff view components exist, take screenshot
  - Create `maestro-workspace/flows/git/navigate_git_tabs.yaml` — navigate to Git screen, verify all tabs exist (Status, Log, Stashes, Branches), tap each tab sequentially, verify each tab renders content (not "Not Implemented"), take screenshot of each tab
  - Create `maestro-workspace/flows/git/stash_operations.yaml` — navigate to Git → Stashes tab, verify stash list or empty state renders (not stub text), verify stash action buttons are present, take screenshot
  - Add all new flows to `maestro-workspace/testplans/regression.yaml`
  - Create `maestro-workspace/testplans/git.yaml` test plan grouping all git flows

  **Must NOT do**:
  - Do NOT modify existing `navigate_to_git.yaml` flow
  - Do NOT perform destructive git operations (no commit, no stash pop) in E2E tests — read-only verification only
  - Do NOT require specific git repository state — tests should handle empty state gracefully

  **Recommended Agent Profile**:
  > Maestro YAML flows for git screen verification.
  - **Category**: `unspecified-high`
    - Reason: Requires understanding Git screen tab structure and Maestro assertion patterns
  - **Skills**: [`android-mcp`]
    - `android-mcp`: Emulator interaction and screenshot capture
  - **Skills Evaluated but Omitted**:
    - `kotlin-best-practices`: Not writing Kotlin code, only Maestro YAML

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 28, 30, 31, 32)
  - **Blocks**: Task 33 (final build verification)
  - **Blocked By**: Tasks 6, 9 (Git stubs fixed and error handling hardened)

  **References**:

  **Pattern References**:
  - `maestro-workspace/flows/navigation/navigate_to_git.yaml` — Existing git navigation flow. New flows should extend this pattern (swipe left → tap Git)
  - `maestro-workspace/flows/sessions/create_new_session.yaml` — Interaction pattern with swipe + tap + optional selector

  **API/Type References**:
  - `composeApp/.../ui/screens/git/GitScreen.kt` — Git screen with tab layout. Lines ~60-90 define tab names (Status, Log, Stashes, Branches). Lines 805-814 contain the Stashes tab (will be fixed by Task 6). Use these tab names as Maestro text selectors
  - `composeApp/.../ui/screens/git/GitScreenModel.kt` — Git screen model with state. Shows what data each tab expects — helps design assertions

  **WHY Each Reference Matters**:
  - Existing git navigation flow shows the swipe+tap pattern to reach the Git screen
  - GitScreen.kt reveals tab names and structure for assertion targeting
  - GitScreenModel.kt shows what states are possible (loading, empty, populated) for each tab

  **Acceptance Criteria**:
  - [ ] 4 new YAML flow files created in `maestro-workspace/flows/git/`
  - [ ] Git test plan created: `maestro-workspace/testplans/git.yaml`
  - [ ] All 4 flows added to `maestro-workspace/testplans/regression.yaml`
  - [ ] Stashes tab flow verifies NO "Not Fully Implemented" text is visible (regression test for Task 6)

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Git flows are valid YAML with correct structure
    Tool: Bash
    Preconditions: Flow files written
    Steps:
      1. Validate YAML syntax for each of the 4 files in `maestro-workspace/flows/git/`
      2. Verify each has `appId: com.mocca.app` and `tags` section
      3. Verify each ends with `takeScreenshot` step
    Expected Result: All 4 files valid, properly structured
    Failure Indicators: Parse error, missing required fields
    Evidence: .sisyphus/evidence/task-29-git-flows-validation.txt

  Scenario: Stashes tab regression assertion exists
    Tool: Bash
    Preconditions: stash_operations.yaml written
    Steps:
      1. Read `maestro-workspace/flows/git/stash_operations.yaml`
      2. Verify it contains an assertion that "Not Fully Implemented" text is NOT visible (e.g., `assertNotVisible` or equivalent)
    Expected Result: Flow explicitly checks that stub text is absent
    Failure Indicators: No negative assertion for stub text
    Evidence: .sisyphus/evidence/task-29-stash-regression.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Message: `test: expand Maestro E2E test coverage`
  - Files: `maestro-workspace/flows/git/*.yaml`, `maestro-workspace/testplans/git.yaml`, `maestro-workspace/testplans/regression.yaml`
  - Pre-commit: YAML syntax validation

---

- [ ] 30. Maestro E2E: Settings & Connection Tests

  **What to do**:
  - Create `maestro-workspace/flows/settings/configure_connection.yaml` — launch app, navigate to settings (swipe left → tap Settings), find host/port/username fields, type test values (`10.0.2.2`, `4096`, `opencode`), take screenshot of filled form
  - Create `maestro-workspace/flows/settings/connection_indicator.yaml` — launch app, verify connection indicator is visible in top bar area, verify it shows a status (connected/disconnected/not configured), take screenshot
  - Create `maestro-workspace/flows/settings/navigate_settings_sections.yaml` — navigate to settings, verify all settings sections are visible and tappable (Connection, Theme/Display, Skills, About), tap into Skills subsection, verify skills list renders, take screenshot of each section
  - Create `maestro-workspace/flows/settings/theme_verification.yaml` — navigate to settings, verify dark/OLED theme elements are present, verify mint accent color elements exist, take screenshot for visual verification
  - Add all new flows to `maestro-workspace/testplans/regression.yaml`
  - Create `maestro-workspace/testplans/settings.yaml` test plan

  **Must NOT do**:
  - Do NOT actually connect to a server during tests — just verify UI elements exist and accept input
  - Do NOT modify existing `navigate_to_settings.yaml` flow
  - Do NOT store real credentials in test files

  **Recommended Agent Profile**:
  > Settings UI verification with form interaction patterns.
  - **Category**: `unspecified-high`
    - Reason: Form interaction testing requires careful selector identification
  - **Skills**: [`android-mcp`]
    - `android-mcp`: Device interaction, form filling, screenshot capture
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: Not modifying UI, only verifying it

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 28, 29, 31, 32)
  - **Blocks**: Task 33 (final build verification)
  - **Blocked By**: Tasks 1-27 (all implementation waves complete)

  **References**:

  **Pattern References**:
  - `maestro-workspace/flows/navigation/navigate_to_settings.yaml` — Existing settings navigation. Extend this pattern
  - `maestro-workspace/subflows/common/launch_app.yaml` — Reusable launch subflow

  **API/Type References**:
  - `composeApp/.../ui/screens/settings/SettingsScreen.kt` — Settings screen layout. Shows all sections, field names, and navigation targets. Line ~885 shows Skills navigation
  - `composeApp/.../data/repository/ConnectionManager.kt` — Connection states (NotConfigured, Disconnected, Connecting, Connected, Error) — these map to UI indicators to assert against

  **WHY Each Reference Matters**:
  - SettingsScreen.kt reveals field labels and section names for Maestro text-based selectors
  - ConnectionManager states map to UI indicator colors/text that tests can assert

  **Acceptance Criteria**:
  - [ ] 4 new YAML flow files created in `maestro-workspace/flows/settings/`
  - [ ] Settings test plan created: `maestro-workspace/testplans/settings.yaml`
  - [ ] All 4 flows added to `maestro-workspace/testplans/regression.yaml`
  - [ ] Connection config flow verifies input fields accept text

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Settings flows are valid and structured
    Tool: Bash
    Preconditions: Flow files written
    Steps:
      1. Validate YAML syntax for all 4 files in `maestro-workspace/flows/settings/`
      2. Verify `appId` and `tags` present in each
      3. Verify `takeScreenshot` step in each
    Expected Result: All valid with correct structure
    Failure Indicators: Parse error, missing fields
    Evidence: .sisyphus/evidence/task-30-settings-flows-validation.txt

  Scenario: Connection config flow includes form interaction
    Tool: Bash
    Preconditions: configure_connection.yaml written
    Steps:
      1. Read `maestro-workspace/flows/settings/configure_connection.yaml`
      2. Verify it contains `inputText` or `tapOn` + type actions for host, port, username fields
      3. Verify it does NOT contain real passwords or sensitive data
    Expected Result: Flow interacts with form fields using test data only
    Failure Indicators: No input actions, real credentials present
    Evidence: .sisyphus/evidence/task-30-connection-form-check.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Message: `test: expand Maestro E2E test coverage`
  - Files: `maestro-workspace/flows/settings/*.yaml`, `maestro-workspace/testplans/settings.yaml`, `maestro-workspace/testplans/regression.yaml`
  - Pre-commit: YAML syntax validation

---

- [ ] 31. Maestro E2E: Session Lifecycle Tests

  **What to do**:
  - Create `maestro-workspace/flows/sessions/switch_session.yaml` — launch app, swipe right to context panel, verify session list is visible, tap on a different session (if multiple exist) or verify single session is selected, take screenshot
  - Create `maestro-workspace/flows/sessions/session_list_display.yaml` — launch app, swipe right to context panel, verify session entries show session ID/title and timestamp, verify list scrolls if multiple sessions, take screenshot
  - Create `maestro-workspace/flows/sessions/fork_session.yaml` — navigate to context panel, long-press or find fork action on a session entry, verify fork dialog appears (if implemented by Task 17), take screenshot
  - Create `maestro-workspace/flows/sessions/delete_session.yaml` — navigate to context panel, long-press or find delete action on a session entry, verify confirmation dialog appears, take screenshot (do NOT confirm deletion to keep test idempotent)
  - Add all new flows to `maestro-workspace/testplans/regression.yaml`
  - Create `maestro-workspace/testplans/sessions.yaml` test plan

  **Must NOT do**:
  - Do NOT actually delete sessions — only verify the deletion UI flow up to confirmation
  - Do NOT modify existing `create_new_session.yaml` flow
  - Do NOT assume specific session data exists — handle empty state gracefully

  **Recommended Agent Profile**:
  > Session management UI testing with list and dialog interactions.
  - **Category**: `unspecified-high`
    - Reason: Complex UI interactions (long-press, dialogs, list navigation)
  - **Skills**: [`android-mcp`]
    - `android-mcp`: Gesture simulation, dialog interaction
  - **Skills Evaluated but Omitted**:
    - `kotlin-best-practices`: Not writing Kotlin

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 28, 29, 30, 32)
  - **Blocks**: Task 33 (final build verification)
  - **Blocked By**: Tasks 16, 17 (session sharing and fork features implemented)

  **References**:

  **Pattern References**:
  - `maestro-workspace/flows/sessions/create_new_session.yaml` — Existing session flow. Shows swipe-right-to-context-panel pattern and session interaction selectors

  **API/Type References**:
  - `composeApp/.../ui/screens/panels/ContextHistoryPanel.kt` — Context panel with session list. Shows session entry layout, action buttons, and dialog triggers
  - `composeApp/.../data/repository/SessionRepository.kt` — Session data model. Shows what fields are available (id, title, timestamp, model) for UI assertions
  - `composeApp/.../ui/components/modern/ForkSessionDialog.kt` — Fork dialog component. Shows dialog content and buttons for assertion targeting

  **WHY Each Reference Matters**:
  - Existing create_new_session flow provides the canonical swipe+tap pattern for context panel
  - ContextHistoryPanel reveals session entry layout and action button selectors
  - ForkSessionDialog shows what dialog content to assert when forking

  **Acceptance Criteria**:
  - [ ] 4 new YAML flow files created in `maestro-workspace/flows/sessions/`
  - [ ] Sessions test plan created: `maestro-workspace/testplans/sessions.yaml`
  - [ ] All 4 flows added to `maestro-workspace/testplans/regression.yaml`
  - [ ] Delete session flow stops at confirmation dialog (non-destructive)

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Session flows are valid and non-destructive
    Tool: Bash
    Preconditions: Flow files written
    Steps:
      1. Validate YAML syntax for all 4 files in `maestro-workspace/flows/sessions/`
      2. Verify `delete_session.yaml` does NOT contain a confirmation tap (only shows dialog)
      3. Verify all files have `appId`, `tags`, and `takeScreenshot`
    Expected Result: All valid, delete flow is non-destructive
    Failure Indicators: Parse error, delete flow confirms deletion
    Evidence: .sisyphus/evidence/task-31-session-flows-validation.txt

  Scenario: Session test plan is complete
    Tool: Bash
    Preconditions: Test plan written
    Steps:
      1. Read `maestro-workspace/testplans/sessions.yaml` and verify all 4 new flows plus existing `create_new_session.yaml` are referenced
    Expected Result: 5 total flows in sessions test plan
    Failure Indicators: Missing flow references
    Evidence: .sisyphus/evidence/task-31-sessions-testplan.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Message: `test: expand Maestro E2E test coverage`
  - Files: `maestro-workspace/flows/sessions/*.yaml`, `maestro-workspace/testplans/sessions.yaml`, `maestro-workspace/testplans/regression.yaml`
  - Pre-commit: YAML syntax validation

---

- [ ] 32. Maestro E2E: Error State & Edge Case Tests

  **What to do**:
  - Create `maestro-workspace/flows/edge-cases/disconnected_state.yaml` — launch app with `clearState: true` (no saved connection), verify app shows "Not Configured" or disconnected indicator, verify chat input is disabled or shows connection prompt, navigate around app and verify no crashes, take screenshot of disconnected state on each screen
  - Create `maestro-workspace/flows/edge-cases/empty_session_state.yaml` — launch app fresh, verify empty session state renders correctly (no blank screen, shows placeholder or welcome message), take screenshot
  - Create `maestro-workspace/flows/edge-cases/rapid_navigation.yaml` — launch app, rapidly swipe left-right-left-right (4 times in quick succession), verify app doesn't crash, verify UI settles to a valid state, take screenshot
  - Create `maestro-workspace/flows/edge-cases/back_navigation.yaml` — launch app, navigate to Settings, navigate to Skills subsection, press back, verify returns to Settings, press back again, verify returns to main screen, take screenshot at each step
  - Create `maestro-workspace/flows/edge-cases/rotate_screen.yaml` — launch app, take screenshot in portrait, trigger rotation (if supported by Maestro), verify app doesn't crash, take screenshot in landscape
  - Add all new flows to `maestro-workspace/testplans/regression.yaml`
  - Create `maestro-workspace/testplans/edge-cases.yaml` test plan

  **Must NOT do**:
  - Do NOT create flows that depend on server connectivity — all edge case tests must work in disconnected state
  - Do NOT use hardcoded sleep — use `extendedWaitUntil` with appropriate timeouts
  - Do NOT assume any specific data in the app state

  **Recommended Agent Profile**:
  > Edge case and resilience testing, needs creative scenario design.
  - **Category**: `unspecified-high`
    - Reason: Edge case testing requires understanding failure modes and UI resilience
  - **Skills**: [`android-mcp`]
    - `android-mcp`: Device automation for gestures, rotation, rapid interaction
  - **Skills Evaluated but Omitted**:
    - `taste-skill-compose`: Not modifying UI, testing resilience only

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (with Tasks 28, 29, 30, 31)
  - **Blocks**: Task 33 (final build verification)
  - **Blocked By**: Tasks 1-27 (all implementation complete — edge cases test the final product)

  **References**:

  **Pattern References**:
  - `maestro-workspace/flows/onboarding/app_launch.yaml` — Launch with `clearState: true` pattern. Edge case tests should always clear state for reproducibility
  - `maestro-workspace/flows/navigation/navigate_to_settings.yaml` — Navigation pattern for back-navigation testing

  **API/Type References**:
  - `composeApp/.../data/repository/ConnectionManager.kt` — Connection status enum (NotConfigured, Disconnected, Error). Tests should verify UI reflects these states correctly
  - `composeApp/.../ui/screens/main/MainScreen.kt` — Main screen 3-panel layout. Rapid navigation tests target this swipe behavior

  **WHY Each Reference Matters**:
  - ConnectionManager states tell us what disconnected UI should look like
  - MainScreen layout reveals the swipe gesture targets for rapid navigation testing

  **Acceptance Criteria**:
  - [ ] 5 new YAML flow files created in `maestro-workspace/flows/edge-cases/`
  - [ ] Edge cases test plan created: `maestro-workspace/testplans/edge-cases.yaml`
  - [ ] All 5 flows added to `maestro-workspace/testplans/regression.yaml`
  - [ ] All flows use `clearState: true` for reproducibility
  - [ ] No flow depends on server connectivity

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Edge case flows are valid and self-contained
    Tool: Bash
    Preconditions: Flow files written
    Steps:
      1. Validate YAML syntax for all 5 files in `maestro-workspace/flows/edge-cases/`
      2. Verify each starts with `launchApp: clearState: true`
      3. Verify none contain server-dependent assertions (no `assertVisible` for server data)
      4. Verify all have `appId`, `tags`, and `takeScreenshot`
    Expected Result: All 5 files valid, self-contained, no server dependency
    Failure Indicators: Parse error, server-dependent assertion, missing clearState
    Evidence: .sisyphus/evidence/task-32-edge-case-validation.txt

  Scenario: Rapid navigation flow has multiple swipes
    Tool: Bash
    Preconditions: rapid_navigation.yaml written
    Steps:
      1. Read `maestro-workspace/flows/edge-cases/rapid_navigation.yaml`
      2. Count `swipe` actions — verify at least 4 rapid swipes
      3. Verify a final `takeScreenshot` captures the settled state
    Expected Result: Flow has 4+ rapid swipes followed by stability assertion
    Failure Indicators: Fewer than 4 swipes, no final screenshot
    Evidence: .sisyphus/evidence/task-32-rapid-nav-check.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Message: `test: expand Maestro E2E test coverage`
  - Files: `maestro-workspace/flows/edge-cases/*.yaml`, `maestro-workspace/testplans/edge-cases.yaml`, `maestro-workspace/testplans/regression.yaml`
  - Pre-commit: YAML syntax validation

---

- [ ] 33. Final Build Verification & Test Plan Update

  **What to do**:
  - Run `./gradlew :androidApp:assembleDebug` — verify clean build with zero errors
  - Run `./gradlew :androidApp:assembleRelease` — verify release build with ProGuard succeeds (depends on Task 13 ProGuard tightening)
  - Verify APK sizes are reasonable (debug < 50MB, release < 30MB)
  - Update `maestro-workspace/testplans/regression.yaml` to include ALL new flows from Tasks 28-32 (final verification that nothing was missed)
  - Update `maestro-workspace/testplans/smoke.yaml` to add 2-3 critical new flows (e.g., chat send_message, git tab navigation, disconnected state)
  - Verify total Maestro flow count: should be 6 existing + ~21 new = ~27 total flows
  - Run `maestro test maestro-workspace/testplans/smoke.yaml` on emulator — verify all smoke tests pass
  - Document final flow inventory in a comment at top of `regression.yaml`

  **Must NOT do**:
  - Do NOT modify any source code — this is verification only
  - Do NOT change ProGuard rules (that's Task 13)
  - Do NOT skip the release build check

  **Recommended Agent Profile**:
  > Build verification and test plan consolidation. Mostly running commands and checking output.
  - **Category**: `unspecified-high`
    - Reason: Build verification + test plan consolidation requires careful checking
  - **Skills**: [`android-mcp`]
    - `android-mcp`: Emulator interaction for running Maestro smoke tests
  - **Skills Evaluated but Omitted**:
    - `kotlin-best-practices`: No Kotlin code changes

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Sequential (after all Wave 5 tasks)
  - **Blocks**: Final Verification Wave (F1-F4)
  - **Blocked By**: Tasks 28, 29, 30, 31, 32 (all E2E test authoring must be complete)

  **References**:

  **Pattern References**:
  - `maestro-workspace/testplans/smoke.yaml` — Current smoke plan (3 flows). Will be expanded to include 5-6 critical flows
  - `maestro-workspace/testplans/regression.yaml` — Current regression plan (6 flows). Will be expanded to include all ~27 flows

  **API/Type References**:
  - `androidApp/proguard-rules.pro` — ProGuard config. Release build depends on this being correctly configured (Task 13)
  - `androidApp/build.gradle.kts` — Build configuration. Check for signing config and minification settings

  **WHY Each Reference Matters**:
  - Test plan files need to be consolidated with all new flows from Tasks 28-32
  - Build config files determine whether debug/release builds will succeed

  **Acceptance Criteria**:
  - [ ] `./gradlew :androidApp:assembleDebug` → BUILD SUCCESSFUL
  - [ ] `./gradlew :androidApp:assembleRelease` → BUILD SUCCESSFUL
  - [ ] `regression.yaml` contains all ~27 flow references
  - [ ] `smoke.yaml` contains 5-6 critical flows
  - [ ] Maestro smoke tests pass on emulator

  **QA Scenarios (MANDATORY):**

  ```
  Scenario: Debug build succeeds
    Tool: Bash
    Preconditions: All Wave 1-4 implementation complete
    Steps:
      1. Run `./gradlew :androidApp:assembleDebug`
      2. Verify exit code is 0
      3. Verify APK exists at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
      4. Check APK size: `ls -la androidApp/build/outputs/apk/debug/androidApp-debug.apk`
    Expected Result: BUILD SUCCESSFUL, APK exists, size < 50MB
    Failure Indicators: Build error, missing APK, APK > 50MB
    Evidence: .sisyphus/evidence/task-33-debug-build.txt

  Scenario: Release build succeeds with ProGuard
    Tool: Bash
    Preconditions: Task 13 ProGuard rules updated
    Steps:
      1. Run `./gradlew :androidApp:assembleRelease`
      2. Verify exit code is 0
      3. Verify APK exists at `androidApp/build/outputs/apk/release/androidApp-release.apk` (or unsigned variant)
      4. Check APK size is smaller than debug (ProGuard minification working)
    Expected Result: BUILD SUCCESSFUL, release APK exists, size < debug APK size
    Failure Indicators: ProGuard error, missing APK, release larger than debug
    Evidence: .sisyphus/evidence/task-33-release-build.txt

  Scenario: Smoke tests pass on emulator
    Tool: Bash
    Preconditions: Emulator running, APK installed, smoke.yaml updated
    Steps:
      1. Run `maestro --device emulator-5554 test maestro-workspace/testplans/smoke.yaml`
      2. Verify all flows pass
    Expected Result: All smoke test flows PASS
    Failure Indicators: Any flow FAIL, timeout, crash
    Evidence: .sisyphus/evidence/task-33-smoke-tests.txt
  ```

  **Commit**: YES (groups with Wave 5)
  - Message: `test: expand Maestro E2E test coverage`
  - Files: `maestro-workspace/testplans/smoke.yaml`, `maestro-workspace/testplans/regression.yaml`
  - Pre-commit: `./gradlew :androidApp:assembleDebug`

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 4 review agents run in PARALLEL. ALL must APPROVE. Rejection → fix → re-run.

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, grep for patterns, run build). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in `.sisyphus/evidence/`. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run `./gradlew :androidApp:assembleDebug` + `./gradlew :androidApp:assembleRelease`. Review all changed files for: `as Any`, `@Suppress`, empty catches, `println` in prod, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic names (data/result/item/temp). Verify no `// TODO` comments remain.
  Output: `Build [PASS/FAIL] | Debug [PASS/FAIL] | Release [PASS/FAIL] | Files [N clean/N issues] | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high` (+ `android-mcp` skill)
  Start from clean state. Run Maestro smoke tests. Use android-mcp to screenshot every screen and verify: no "Not Implemented" text, no placeholder data, no broken layouts, proper theming. Test edge cases: disconnected state, empty sessions, rapid navigation. Save to `.sisyphus/evidence/final-qa/`.
  Output: `Screens [N/N pass] | Maestro [N/N] | Edge Cases [N tested] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff (git log/diff). Verify 1:1 — everything in spec was built (no missing), nothing beyond spec was built (no creep). Check "Must NOT do" compliance. Detect cross-task contamination. Flag unaccounted changes.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

| Wave | Commit | Message | Verify |
|------|--------|---------|--------|
| 1 | After all Wave 1 tasks | `chore: remove dead code and fix stubs` | `./gradlew :androidApp:assembleDebug` |
| 2 | After all Wave 2 tasks | `fix: harden error handling and release config` | `./gradlew :androidApp:assembleDebug && ./gradlew :androidApp:assembleRelease` |
| 3 | After all Wave 3 tasks | `feat: add command palette, session sharing, agents, and provider management` | `./gradlew :androidApp:assembleDebug` |
| 4 | After all Wave 4 tasks | `feat: project switching, interactive terminal, UI polish` | `./gradlew :androidApp:assembleDebug` |
| 5 | After all Wave 5 tasks | `test: expand Maestro E2E test coverage` | Maestro smoke tests pass |

---

## Success Criteria

### Verification Commands
```bash
# Debug build succeeds
./gradlew :androidApp:assembleDebug
# Expected: BUILD SUCCESSFUL

# Release build succeeds
./gradlew :androidApp:assembleRelease
# Expected: BUILD SUCCESSFUL

# No stubs or TODOs in user-facing code
grep -r "Not Fully Implemented\|Not Yet Implemented\|Coming Soon" composeApp/src/
# Expected: 0 matches

grep -r "// TODO" composeApp/src/
# Expected: 0 matches

# No dead code references
grep -r "ChatComponents" composeApp/src/
# Expected: 0 matches

grep -r "WorkspaceScreen\b" composeApp/src/
# Expected: 0 matches (after removal)

grep -r "WorktreeScreen\b" composeApp/src/
# Expected: 0 matches (after removal)

# No silent catch-null patterns
grep -rn "catch.*{" composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/GitRepository.kt | grep -c "null"
# Expected: 0
```

### Final Checklist
- [ ] All "Must Have" items present and verified
- [ ] All "Must NOT Have" items absent and verified
- [ ] Debug build succeeds
- [ ] Release build succeeds
- [ ] All Maestro E2E tests pass
- [ ] Zero `// TODO` comments in production code
- [ ] Zero "Not Implemented" stubs in user-facing UI
- [ ] Zero orphaned screens in DI registrations
- [ ] Zero silent catch-null patterns in repositories
