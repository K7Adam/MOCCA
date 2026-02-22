# COMPREHENSIVE MOCCA CODEBASE ANALYSIS & REFACTORING PROMPT

## MISSION

You are tasked with deeply analyzing the MOCCA Android application codebase, identifying ALL inconsistencies, violations of best practices, and duplications against the documented skills and AGENTS.md files, and then AUTONOMOUSLY FIXING every single issue found. Your goal is to achieve 100% consistency with the project's defined architecture, design system, and best practices.

**This is NOT a request to discuss or plan - you MUST immediately begin analysis and fix all issues.**

---

## CRITICAL SELF-ANALYSIS REQUIREMENT

### THIS SUMMARY IS NOT GUARANTEED COMPLETE

The issues listed in this prompt are a **STARTING POINT** discovered through preliminary analysis. They are NOT exhaustive. You MUST conduct your OWN comprehensive analysis of the codebase and challenge every assumption made here.

**YOUR MANDATORY SELF-CRITIQUE PROTOCOL:**

1. **Question Every Statement**: Before fixing anything, verify it actually exists as described
2. **Search Beyond Listed Patterns**: The grep patterns provided are examples - search for RELATED anti-patterns
3. **Explore Unlisted Areas**: Don't just fix what's mentioned - explore the ENTIRE codebase for issues
4. **Challenge The Summary**: If something seems wrong in this prompt, investigate before following blindly
5. **Find Issues This Prompt Missed**: Your job is to find problems THIS PROMPT didn't list

### Additional Areas You MUST Investigate (Not Listed Above)

Since this summary is incomplete, you MUST also search for:

- [ ] **Naming inconsistencies**: snake_case vs camelCase, Hungarian notation
- [ ] **Magic numbers/strings**: Hardcoded values that should be constants
- [ ] **Code duplication**: Repeated logic across different files
- [ ] **Circular dependencies**: Import cycles between modules
- [ ] **Memory leaks**: Uncollected flows, unbounded caches
- [ ] **Threading issues**: Main thread blocking, incorrect dispatchers
- [ ] **Missing null checks**: Potential NPEs
- [ ] **Inconsistent error messages**: Varying error handling approaches
- [ ] **Unused code**: Dead code, unreachable branches
- [ ] **Test coverage gaps**: Missing unit tests
- [ ] **Logging inconsistencies**: Mixed logging approaches
- [ ] **Configuration scattered**: Hardcoded configs instead of centralized
- [ ] **API coupling**: Tight coupling to specific API structures
- [ ] **State management**: Inconsistent state approaches across screens

**If you find issues NOT in this prompt, you MUST fix them too.**

---

## PROJECT REFERENCE FILES (MANDATORY CONTEXT)

You MUST internalize these files as the source of truth:

### 1. Root Knowledge Base
- **File**: `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\AGENTS.md`
- **Key Rules**:
  - Theme: Pitch Black (`#000000`), Mint Green (`#00D9A5`), rounded corners
  - Architecture: MVI (ScreenModel → StateFlow → UI)
  - Offline-First: Repositories return `Flow<Resource<T>>`
  - Paths: ALWAYS absolute paths
  - **ANTI-PATTERNS**: 
    - NEVER use `RectangleShape` for interactive elements
    - NEVER hold `HttpClient` references — use `ApiExecutor.execute {}`
    - NEVER use relative paths
    - NEVER block main thread

### 2. Kotlin Best Practices Skill
- **File**: `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\.opencode\skills\kotlin-best-practices\SKILL.md`
- **Key Rules**:
  - MVI Pattern: State (immutable data class) → Events (sealed interface) → Effects (sealed interface)
  - StateFlow/MutableStateFlow in ViewModels
  - Koin DI: `singleOf()`, `viewModelOf()`, `factoryOf()`
  - SQLDelight for persistence
  - Voyager for navigation
  - Napier for logging (NOT android.util.Log)
  - `@Stable` annotation for Compose optimization
  - expect/actual for platform-specific code

### 3. UI/UX Compose Skill
- **File**: `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\.opencode\skills\taste-skill-compose\SKILL.md`
- **Key Rules**:
  - **THE LILA BAN**: "AI Purple/Blue" aesthetic is strictly BANNED. No purple button glows, no neon gradients. Use neutral bases with high-contrast singular accents (Mint Green `#00D9A5`)
  - Use `AppColors`, `AppShapes`, `AppSpacing`, `AppTypography` from theme package
  - **NEVER** use `MaterialTheme.colorScheme` directly
  - **NEVER** hardcode colors like `Color(0xFF...)` - use `AppColors`
  - **NEVER** use `RectangleShape` for interactive elements
  - Always use `AppShapes.pill` for buttons, `AppShapes.card` for cards
  - Glassmorphic effects: Use `AppColors.glassBackground` with `AppColors.glassBorder`
  - AndroidLiquidGlass: Use `io.github.kyant0:backdrop` for liquid glass UI components
  - State hoisting: ViewModel vs local `remember`
  - Empty, loading, error states MUST be implemented
  - `contentDescription` for accessibility-critical icons

### 4. Domain Models Knowledge Base
- **File**: `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\domain\model\AGENTS.md`
- **Key Rules**:
  - Strict Immutability: All domain models `data class` with `val` properties (NO `var`)
  - Sealed hierarchies for state: `Resource<T>` with `Success`, `Loading`, `Error`
  - Zero Business Logic: Domain models must remain pure data containers
  - `@Serializable` for network/serialization
  - `ConnectionStatus.Disconnected` is a `data class`, NOT an `object`

---

## ANALYSIS CHECKLIST (EXHAUSTIVE)

You MUST verify EACH of the following categories:

### CATEGORY 1: THEME SYSTEM CONSISTENCY

#### Issue 1.1: Theme File Duplication & Conflicts
**Expected**: Single unified theme system using Pitch Black + Mint Green
**Current Problems Found**:
- `Theme.kt` - Uses "OpenChamber Palette" (Warm Sand/Golden) - **VIOLATES PITCH BLACK SPEC**
- `ModernTheme.kt` - Uses ModernColors with Electric Violet/Indigo - **VIOLATES "LILA BAN"**
- `AppTheme.kt` - Correctly uses AppColors with Pitch Black + Mint Green
- `Color.kt` - Duplicate warm sand palette (conflicts with AppColors)
- `ModernColors.kt` - Duplicate palette with violet/indigo - **VIOLATES "LILA BAN"**
- `AppColors.kt` - Correct Pitch Black + Mint Green palette
- `Typography.kt` vs `AppTypography.kt` - Duplicate typography systems
- `Shapes.kt` vs `AppShapes.kt` - Duplicate shape systems

**FIX REQUIRED**:
1. Remove ALL theme files except `AppTheme.kt`, `AppColors.kt`, `AppTypography.kt`, `AppShapes.kt`, `AppSpacing.kt`
2. Delete: `Theme.kt`, `Color.kt`, `ModernTheme.kt`, `ModernColors.kt`, `Typography.kt`, `Shapes.kt`
3. Update ALL imports referencing deleted files
4. Ensure AppTheme.kt uses only AppColors, AppTypography, AppShapes

#### Issue 1.2: Hardcoded Colors in Components
**Search Pattern**: `Color(0xFF...)` appearing in component files (not in AppColors.kt)
**Files to Fix**: Any .kt file using direct Color() that should use AppColors

**FIX REQUIRED**:
1. Search entire codebase for `Color(0x`
2. For each occurrence NOT in AppColors.kt, ModernColors.kt (to be deleted), or theme files:
   - Either add the color to AppColors.kt if it's a design system color
   - Or replace with existing AppColors reference

#### Issue 1.3: MaterialTheme.colorScheme Direct Usage
**Search Pattern**: `MaterialTheme.colorScheme` in component files
**Expected**: Use custom AppColors via `AppTheme.extendedColors` or direct AppColors object

**FIX REQUIRED**:
1. Search for `MaterialTheme.colorScheme` in all .kt files
2. Replace with appropriate AppColors references
3. Exception: Internal MaterialTheme configuration in AppTheme.kt is acceptable

### CATEGORY 2: ARCHITECTURE CONSISTENCY

#### Issue 2.1: MVI Pattern Compliance
**Expected**: 
- State: Immutable data class with val properties
- Events: Sealed interface
- Effects: Sealed interface for side effects
- ViewModel: StateFlow + SharedFlow for effects

**FIX REQUIRED**:
1. Review all ScreenModel implementations:
   - `SettingsScreenModel.kt`
   - `OnboardingWizardModel.kt`
   - `GitScreenModel.kt`
   - `McpScreenModel.kt`
   - `SessionsScreenModel.kt`
   - `DashboardScreenModel.kt`
   - `ChatScreenModel.kt`
   - `MainScreenModel.kt`
2. Ensure each has:
   - Proper immutable state data class
   - Sealed interface for events/intents
   - Sealed interface for effects (navigation, toasts, etc.)
   - Private MutableStateFlow exposed as StateFlow
   - Private MutableSharedFlow exposed as SharedFlow

#### Issue 2.2: Repository Pattern Consistency
**Expected**: 
- All repositories return `Flow<Resource<T>>` for offline-first
- Single source of truth pattern
- Proper error handling

**FIX REQUIRED**:
1. Review all Repository classes in `data/repository/`
2. Ensure consistency in:
   - Return types (Flow<Resource<T>> preferred)
   - Error handling patterns
   - Refresh/cache invalidation patterns

#### Issue 2.3: Dependency Injection Consistency
**Expected**: 
- Koin modules in `di/Modules.kt`
- Consistent scoping (single vs factory)
- No direct HttpClient references in consumers

**FIX REQUIRED**:
1. Verify all repositories use `ApiExecutor.execute {}` not direct HttpClient
2. Verify Koin module consistency

### CATEGORY 3: UI/COMPOSE CONSISTENCY

#### Issue 3.1: RectangleShape Usage
**Search Pattern**: `RectangleShape` in component files
**Expected**: `AppShapes.pill`, `AppShapes.card`, or other AppShapes values

**FIX REQUIRED**:
1. Search for `RectangleShape` in all .kt files
2. Replace with appropriate AppShapes

#### Issue 3.2: State Hoisting
**Expected**:
- Stateless composables that receive state via parameters
- State held in ViewModel, not in composables
- Proper use of `remember`, `LaunchedEffect`, `SideEffect`

**FIX REQUIRED**:
1. Review composable functions in `ui/components/` and `ui/screens/`
2. Ensure complex state is in ViewModels
3. Ensure composables are properly stateless

#### Issue 3.3: Empty/Loading/Error States
**Expected**: All list screens implement loading, empty, and error states

**FIX REQUIRED**:
1. Review screens with data lists
2. Ensure proper loading/empty/error UI states exist

#### Issue 3.4: Accessibility
**Expected**: All meaningful icons have contentDescription

**FIX REQUIRED**:
1. Search for Icon composables without contentDescription
2. Add meaningful descriptions

### CATEGORY 4: CODE QUALITY

#### Issue 4.1: Empty Catch Blocks
**Search Pattern**: `catch(e: Exception) {}` or similar
**Expected**: Proper error handling with logging

**FIX REQUIRED**:
1. Find all empty catch blocks
2. Add proper error handling (Napier logging or re-throw)

#### Issue 4.2: Unnecessary Suppress Annotations
**Search Pattern**: `@Suppress` annotations
**Expected**: Only necessary suppressions

**FIX REQUIRED**:
1. Review all @Suppress annotations
2. Remove unnecessary ones
3. Fix underlying issues where possible

#### Issue 4.3: Unused Imports
**Expected**: No unused imports

**FIX REQUIRED**:
1. Run IDE inspection to find unused imports
2. Remove them

### CATEGORY 5: DOMAIN MODEL COMPLIANCE

#### Issue 5.1: Mutable Properties
**Search Pattern**: `var` properties in data classes in domain/model/
**Expected**: All `val`

**FIX REQUIRED**:
1. Review all data classes in domain/model/
2. Change any `var` to `val`

#### Issue 5.2: ConnectionStatus.Disconnected
**Expected**: `Disconnected(reason: String?)` as data class, NOT object

**FIX REQUIRED**:
1. Verify Config.kt has `data class Disconnected(...)` not `object Disconnected`

---

## EXECUTION STRATEGY

### Phase 1: Analysis (PARALLEL EXECUTION)
Execute these searches SIMULTANEOUSLY to gather all issues:
1. `grep -rn "RectangleShape" --include="*.kt"` → Report all occurrences
2. `grep -rn "MaterialTheme.colorScheme" --include="*.kt"` → Report all occurrences  
3. `grep -rn "Color(0x" --include="*.kt" | grep -v "AppColors\|ModernColors\|Color.kt\|Theme.kt\|AnsiParser"` → Hardcoded colors
4. `grep -rn "catch.*{.*}" --include="*.kt"` → Empty catch blocks
5. `grep -rn "var " --include="*.kt" | grep "data class"` → Mutable data class props
6. List all theme files → Identify duplicates
7. `grep -rn "Theme\|Color\|Typography\|Shapes" --include="*.kt" | grep "import.*theme"` → Theme import analysis

### Phase 2: Fix Implementation
For each issue category:
1. Delete duplicate/unused files (theme files)
2. Fix imports
3. Replace hardcoded values
4. Fix architecture patterns
5. Add missing states

### Phase 3: Verification
After fixes:
1. Run build: `.\gradlew.bat :androidApp:assembleDebug`
2. Verify no new errors
3. Confirm all skill requirements met

---

## SUCCESS CRITERIA

Your work is COMPLETE when:

- [ ] Build succeeds with exit code 0
- [ ] No `RectangleShape` in interactive elements
- [ ] No hardcoded `Color(0xFF...)` outside theme files
- [ ] No `MaterialTheme.colorScheme` direct usage in components
- [ ] Theme files consolidated to: AppTheme.kt, AppColors.kt, AppTypography.kt, AppShapes.kt, AppSpacing.kt
- [ ] Pitch Black (#000000) + Mint Green (#00D9A5) theme exclusively used
- [ ] No "LILA BAN" violations (no violet/purple/indigo in design)
- [ ] All ScreenModels follow MVI pattern
- [ ] All repositories return Flow<Resource<T>> pattern
- [ ] Empty catch blocks resolved
- [ ] Domain models use val only

---

## MANDATORY SELF-REVIEW BEFORE DECLARING COMPLETE

Before you declare your work done, you MUST answer these questions honestly:

### Self-Critique Checklist

1. **Did I verify each issue actually exists before fixing it?**
   - [ ] Yes, I searched and confirmed
   - [ ] No, I blindly followed this prompt

2. **Did I search for additional issues beyond this prompt?**
   - [ ] Yes, I explored thoroughly and found X additional issues
   - [ ] No, I only fixed what was listed

3. **Did I challenge the assumptions in this prompt?**
   - [ ] Yes, I found Y items that were incorrectly described
   - [ ] No, I accepted everything at face value

4. **Is there any remaining technical debt I noticed but didn't fix?**
   - [ ] No, I fixed everything I found
   - [ ] Yes: (list the issues)

5. **Would a senior engineer review approve my changes?**
   - [ ] Yes, my changes are production-ready
   - [ ] No, there are concerns: (explain)

6. **Did I break any existing functionality?**
   - [ ] No, build passes and app works
   - [ ] Yes, I need to fix: (explain)

**If you cannot honestly check all boxes, you are NOT done.**

---

## FILE INVENTORY (STARTING POINT)

### Theme Files (CONSOLIDATE TO 5):
```
EXISTING:
- Theme.kt (DELETE - warm sand palette)
- Color.kt (DELETE - duplicate)
- ModernTheme.kt (DELETE - violet/indigo violates spec)
- ModernColors.kt (DELETE - violet/indigo violates spec)
- AppTheme.kt (KEEP)
- AppColors.kt (KEEP)
- AppTypography.kt (KEEP)
- AppShapes.kt (KEEP)
- AppSpacing.kt (KEEP)
- Typography.kt (DELETE - duplicate)
- Shapes.kt (DELETE - duplicate)
```

### Key Source Directories:
```
composeApp/src/commonMain/kotlin/com/mocca/app/
├── api/                    # Ktor clients, ApiExecutor
├── data/
│   ├── local/              # SQLDelight
│   ├── repository/        # 20+ repositories
│   └── security/          # Token storage
├── di/                    # Koin modules
├── domain/
│   ├── manager/           # Platform managers
│   └── model/             # Domain models (20+ .kt files)
├── ui/
│   ├── components/        # Reusable composables
│   │   ├── chat/
│   │   ├── glass/
│   │   ├── modern/
│   │   └── navigation/
│   ├── navigation/
│   ├── screens/           # Screen composables + ViewModels
│   │   ├── chat/
│   │   ├── files/
│   │   ├── git/
│   │   ├── main/
│   │   ├── mcp/
│   │   ├── onboarding/
│   │   ├── panels/
│   │   ├── sessions/
│   │   └── settings/
│   └── theme/             # (TO BE CONSOLIDATED)
└── util/                  # Utilities
```

---

## CRITICAL REMINDERS

1. **NO PINK/PURPLE/INDIGO**: ModernColors uses Electric Violet (0xFF8B5CF6) and Deep Indigo (0xFF6366F1) - THIS VIOLATES THE "LILA BAN" in taste-skill-compose. Delete ModernColors.kt entirely.

2. **WARM SAND vs PITCH BLACK**: Theme.kt uses warm sand colors (DarkBackground = 0xFF151313) - THIS VIOLATES the Pitch Black spec. Delete Theme.kt.

3. **DUPLICATE SYSTEMS**: There are 2 of everything in theme - consolidate to single App* system.

4. **ABSOLUTE PATHS**: All file references must be absolute paths (C:\Users\ruzaq\AndroidStudioProjects\MOCCA\...)

---

## DOCUMENT YOUR FINDINGS

As you analyze and fix, you MUST document:

### Additional Issues Found (Beyond This Prompt)

Create a list of issues you discovered that were NOT mentioned in this prompt:

```
## My Additional Findings

1. [Category] - [Issue Description]
   - File: [path]
   - Fix: [what you did]

2. [Category] - [Issue Description]
   - File: [path]
   - Fix: [what you did]
```

### Corrections to This Prompt

If you found anything in this prompt that was incorrect:

```
## Prompt Corrections

1. [Issue mentioned here] - Actually: [real situation]
2. [File claimed to exist] - Actually: [it doesn't / has different content]
```

---

## BEGIN NOW

Start with parallel grep searches to identify all issues, then systematically fix each category. Document your progress. The build must pass at the end.
