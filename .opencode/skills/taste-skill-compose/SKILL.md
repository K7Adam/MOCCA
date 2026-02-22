---
name: taste-skill-compose
description: Senior UI/UX Engineer for Kotlin/Compose. Architect digital interfaces overriding default LLM biases. Enforces metric-based rules, strict component architecture, hardware-accelerated animations, and balanced design engineering.
related_skills:
  - kotlin-best-practices
  - taste-skill
  - android-mcp
---

# High-Agency Compose UI Skill

## RELATED SKILLS

**This skill focuses on UI/UX design decisions. For architecture patterns or device testing, see:**

| Skill | Purpose | When to Use |
|-------|---------|-------------|
| **kotlin-best-practices** | KMP architecture, MVI, DI, Navigation, Testing, SQLDelight | When implementing ViewModels, repositories, navigation, or data layer |
| **taste-skill** | Original React/Next.js + Tailwind version | When building web interfaces alongside this mobile app |
| **android-mcp** | Android device automation via ADB/MCP | When testing UI on emulators, automating UI interactions, capturing screenshots, or running ADB commands |

**Architecture Concerns Delegated to kotlin-best-practices:**
- `expect`/`actual` patterns for platform-specific code
- Dispatcher selection (`Dispatchers.IO`, `Dispatchers.Default`)
- Koin dependency injection module setup
- Voyager navigation screen definitions
- SQLDelight schema and repository patterns
- Unit test and UI test patterns
- `@Stable` annotation for Compose optimization
- Napier logging

## 1. ACTIVE BASELINE CONFIGURATION
* DESIGN_VARIANCE: 8 (1=Perfect Symmetry, 10=Artsy Chaos)
* MOTION_INTENSITY: 6 (1=Static/No movement, 10=Cinematic/Magic Physics)
* VISUAL_DENSITY: 4 (1=Art Gallery/Airy, 10=Pilot Cockpit/Packed Data)

**AI Instruction:** The standard baseline for all generations is strictly set to these values (8, 6, 4). Do not ask the user to edit this file. Otherwise, ALWAYS listen to the user: adapt these values dynamically based on what they explicitly request in their chat prompts. Use these baseline (or user-overridden) values as your global variables to drive the specific logic in Sections 3 through 7.

## 2. DEFAULT ARCHITECTURE & CONVENTIONS

* **DEPENDENCY VERIFICATION [MANDATORY]:** Before importing ANY library, check `libs.versions.toml` and existing imports. If missing, specify the Gradle dependency addition BEFORE providing code.
* **Framework:** Compose Multiplatform with Material3 as base, but CUSTOM theme tokens.
* **State Management:** Use `StateFlow`/`MutableStateFlow` in ViewModels. UI observes via `collectAsState()`.
* **Styling Policy:** Use `AppColors`, `AppShapes`, `AppSpacing`, `AppTypography` from theme package. NEVER use `MaterialTheme.colorScheme` directly.
* **ANTI-EMOJI POLICY [CRITICAL]:** NEVER use emojis in UI text. Use icons from Material Icons or custom vector drawables.
* **LIQUID GLASS [REQUIRED]:** Use [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (`io.github.kyant0:backdrop`) for glassmorphic effects.
  * **Docs:** https://kyant.gitbook.io/backdrop
  * **Components:** `Backdrop`, `LiquidButton`, `LiquidToggle`, `LiquidSlider`, `LiquidBottomTabs`
* **Layout Components:**
  * `Column` for vertical stacking
  * `Row` for horizontal stacking  
  * `Box` for overlay/positioning
  * `LazyColumn`/`LazyRow` for scrollable lists
  * `LazyVerticalGrid` for grid layouts
* **Responsiveness:**
  * Use `BoxWithConstraints` for responsive layouts
  * Use `weight()` modifiers for flexible sizing
  * Standard screen padding: `AppSpacing.screenPaddingHorizontal`
* **Shapes:** ALWAYS use `AppShapes.pill` for buttons, `AppShapes.card` for cards. NEVER use `RectangleShape` for interactive elements.
* **Icons:** Use `androidx.compose.material.icons.Icons.Default.*` or custom vector resources. Standardize icon sizes via `AppSpacing.iconSize*`.

## 3. DESIGN ENGINEERING DIRECTIVES (Bias Correction)

**Rule 1: Deterministic Typography**
* **Display/Headlines:** Use `AppTypography.displayLarge` or custom `TextStyle(fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp)`.
    * **ANTI-SLOP:** Avoid generic system fonts for "premium" feel. Use `FontFamily.SansSerif` with variable weights or load custom fonts.
    * **TECHNICAL UI RULE:** Serif fonts are BANNED for Dashboard/Software UIs. Use Sans-Serif pairings.
* **Body/Paragraphs:** `AppTypography.bodyMedium` or `TextStyle(fontSize = 14.sp, lineHeight = 20.sp)`.

**Rule 2: Color Calibration**
* **Constraint:** Max 1 Accent Color. Avoid oversaturation.
* **THE LILA BAN:** The "AI Purple/Blue" aesthetic is strictly BANNED. No purple button glows, no neon gradients. Use neutral bases with high-contrast singular accents (e.g., Mint Green `#00D9A5`, Electric Blue, Deep Rose).
* **COLOR CONSISTENCY:** Stick to one palette for the entire output. Use `AppColors.accentGreen` as primary accent.

**Rule 3: Layout Diversification**
* **ANTI-CENTER BIAS:** Centered Hero sections are BANNED when `DESIGN_VARIANCE > 4`. Force "Split Screen", "Left Aligned content/Right Aligned asset", or asymmetric structures using `Row` with `weight()` modifiers.

**Rule 4: Materiality, Shadows, and "Anti-Card Overuse"**
* **DASHBOARD HARDENING:** For `VISUAL_DENSITY > 7`, generic card containers are BANNED. Use `Divider` or `HorizontalDivider()` with `Modifier.padding()`.
* **Execution:** Use cards ONLY when elevation communicates hierarchy via `AppColors.surfaceElevated`.

**Rule 5: Interactive UI States**
* **Mandatory Generation:** LLMs naturally generate "static" successful states. You MUST implement full interaction cycles:
  * **Loading:** Skeleton loaders matching layout sizes (use `shimmer` effect or placeholder `Box`).
  * **Empty States:** Beautifully composed empty states with icon and guidance text.
  * **Error States:** Clear, inline error reporting with retry action.
  * **Tactile Feedback:** On press, use `scale(0.98f)` via `animateFloatAsState`.

**Rule 6: Data & Form Patterns**
* **Forms:** Label MUST sit above input. Use `Column` with `gap = AppSpacing.sm`. Helper text optional, error text below input.

## 4. CREATIVE PROACTIVITY (Anti-Slop Implementation)

* **"Liquid Glass" Refraction:** When glassmorphism is needed, use `AppColors.glassBackground` with `AppColors.glassBorder`. Add inner highlight via `drawBehind` or `background` with gradient brush.
* **Perpetual Micro-Interactions (If MOTION_INTENSITY > 5):** Use `rememberInfiniteTransition()` with `animateFloat` for continuous subtle animations (pulse, float, shimmer).
* **Spring Physics:** Apply `spring(stiffness = 300f, dampingRatio = 0.8f)` to all meaningful animations. NO linear easing.
* **Staggered Orchestration:** Use `AnimatedVisibility` with `enter = fadeIn(tween(delayMillis = index * 100))` for staggered list reveals.
* **Shared Element Transitions:** Use `SharedTransitionLayout` and `Modifier.sharedElement()` for hero transitions between screens.

## 5. PERFORMANCE GUARDRAILS

* **Recomposition:** Use `remember`, `derivedStateOf`, and `key()` in LazyColumn to minimize recomposition.
* **Hardware Acceleration:** Animate ONLY `Modifier.graphicsLayer { alpha, scaleX, scaleY, translationX, translationY }`. NEVER animate `width`, `height`, `padding` directly.
* **Z-Index:** Use `Modifier.zIndex()` sparingly, only for systemic layers (Modals, Overlays, Sticky Headers).

## 6. TECHNICAL REFERENCE (Dial Definitions)

### DESIGN_VARIANCE (Level 1-10)
* **1-3 (Predictable):** `Column`/`Row` with `Arrangement.Center`, equal weights, symmetric spacing.
* **4-7 (Offset):** Use `Modifier.offset()` for overlapping, varied image aspect ratios, left-aligned headers.
* **8-10 (Asymmetric):** `LazyVerticalGrid(GridCells.Adaptive())` with fractional units, `Box` with absolute positioning, massive empty zones via `Spacer(Modifier.weight(1f))`.
* **MOBILE OVERRIDE:** For levels 4-10, asymmetric layouts MUST fall back to single-column on narrow screens via `BoxWithConstraints`.

### MOTION_INTENSITY (Level 1-10)
* **1-3 (Static):** No automatic animations. Only `Modifier.clickable` ripple.
* **4-7 (Fluid):** Use `animate*AsState(tween(300, easing = FastOutSlowInEasing))`. Use `AnimatedVisibility` with `fadeIn`/`slideIn`.
* **8-10 (Advanced):** Complex scroll-triggered animations via `LaunchedEffect` + scroll state. Use `Transition` API for orchestrated multi-state animations.

### VISUAL_DENSITY (Level 1-10)
* **1-3 (Art Gallery Mode):** Large `Spacer`, generous padding (`AppSpacing.xxl`), lots of whitespace.
* **4-7 (Daily App Mode):** Normal `AppSpacing` values.
* **8-10 (Cockpit Mode):** Minimal padding (`AppSpacing.xs`), no card boxes, use `Divider` for separation. **Mandatory:** Use `FontFamily.Monospace` for all numbers.

## 7. THE 100 AI TELLS (Forbidden Patterns)

### Visual & Modifier
* **NO Neon/Outer Glows:** Avoid `Modifier.shadow(elevation = X.dp)` with high values. Use subtle `Modifier.border()` instead.
* **NO Oversaturated Accents:** Desaturate accent colors to blend elegantly.
* **NO Excessive Gradients:** Avoid complex `Brush.linearGradient` for large areas.

### Typography
* **NO Inter Font (conceptually):** Avoid generic-looking typography. Use distinct font weights and tracking.
* **NO Oversized H1s:** Control hierarchy with weight and color, not just massive font size.
* **Serif Constraints:** Serif fonts ONLY for creative/editorial designs. NEVER on dashboards.

### Layout & Spacing
* **Align Perfectly:** Ensure consistent `Modifier.padding()` and `Spacer` usage.
* **NO 3-Column Card Layouts:** The generic "3 equal cards in a Row" is BANNED. Use 2-column zigzag, asymmetric grid, or horizontal `LazyRow`.

### Content & Data
* **NO Generic Names:** "John Doe", "Test User" are banned. Use realistic, contextual placeholders.
* **NO Fake Numbers:** Avoid `99.99%`, `50%`. Use organic data (`47.2%`, `1,847`).
* **NO Startup Slop Names:** "Acme", "Nexus", "SmartFlow" are banned. Use contextually relevant names.

### Compose-Specific Anti-Patterns
* **NEVER** use `RectangleShape` for interactive elements.
* **NEVER** hardcode colors like `Color(0xFF...)`. Use `AppColors`.
* **NEVER** skip `contentDescription` for accessibility-critical icons.
* **NEVER** use `Modifier.pointerInteropFilter` for basic clicks (use `Modifier.clickable`).
* **NEVER** nest `LazyColumn` inside `LazyColumn`.
* **NEVER** use `MaterialTheme.colorScheme` directly - use custom `AppColors`.

## 8. ANIMATION CODE PATTERNS

### Spring Animation (Natural Motion)
```kotlin
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioMediumBouncy
    ),
    label = "buttonScale"
)
Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
```

### Staggered List Reveal
```kotlin
LazyColumn {
    items(items = data, key = { it.id }) { item ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(delayMillis = index * 100)) +
                    slideInVertically(tween(delayMillis = index * 100))
        ) {
            ItemRow(item)
        }
    }
}
```

### Perpetual Pulse (MOTION_INTENSITY > 5)
```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "pulse")
val alpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
)
```

### Glassmorphic Card
```kotlin
Card(
    shape = AppShapes.card,
    colors = CardDefaults.cardColors(
        containerColor = AppColors.glassBackground
    ),
    border = BorderStroke(AppSpacing.borderThin, AppColors.glassBorder)
) { /* content */ }
```

### Tactile Button Press
```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = tween(100),
    label = "scale"
)

Button(
    onClick = { },
    interactionSource = interactionSource,
    modifier = Modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(AppShapes.pill)
) { Text("Action") }
```

### Responsive Layout with BoxWithConstraints
```kotlin
BoxWithConstraints {
    if (maxWidth < 600.dp) {
        // Single column for narrow screens
        Column { /* ... */ }
    } else {
        // Two column for wider screens
        Row {
            Box(modifier = Modifier.weight(1f)) { /* Left */ }
            Box(modifier = Modifier.weight(1f)) { /* Right */ }
        }
    }
}
```

## 9. FINAL PRE-FLIGHT CHECK

Before generating ANY Compose UI code, verify:

- [ ] Is state hoisted appropriately (ViewModel vs local `remember`)?
- [ ] Is single-column layout guaranteed on narrow screens for high-variance designs?
- [ ] Are `LaunchedEffect` animations cleaned up properly (no memory leaks)?
- [ ] Are empty, loading, and error states provided?
- [ ] Are cards omitted in favor of `Divider` where appropriate for VISUAL_DENSITY?
- [ ] Are CPU-heavy animations isolated in separate Composables?
- [ ] Is `key()` used in `LazyColumn`/`LazyRow` items?
- [ ] Is `derivedStateOf` used for computed/filtered state?
- [ ] Are `AppColors`/`AppShapes`/`AppSpacing` used instead of hardcoded values?
- [ ] Is `Modifier.graphicsLayer` used for animations (not width/height/padding)?
- [ ] Is `contentDescription` provided for meaningful icons?
- [ ] Is the accent color singular and not purple/blue "AI aesthetic"?
