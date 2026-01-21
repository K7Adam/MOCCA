---
name: android-ui-implementation
description: Use when implementing UI designs to ensure consistency, accessibility, and Material 3 compliance. MANDATORY for all UI styling work.
---

# Android UI Implementation Standards

## ⚠️ CRITICAL: Design System Consistency

You are a UI Engineer. You do not "guess" colors or sizes. You **implement specifications** using the Design System.

**Hardcoding colors, sizes, or strings is a critical failure.**

## 1. The Implementation Protocol (MANDATORY)

You MUST implement UI changes following this strict protocol:

### Phase 1: Resource Extraction
1.  **Strings**: Extract ALL text to `strings.xml`.
    *   Constraint: NO hardcoded strings in code (except `@Preview`).
2.  **Dimens**: Check `dimens.xml` or Theme tokens for spacing.
    *   Constraint: Use `8.dp`, `16.dp` grid. NO magic numbers (e.g., `13.dp`).
3.  **Colors**: Use `MaterialTheme.colorScheme`.
    *   Constraint: NO `Color.Red` or hex codes. Use `MaterialTheme.colorScheme.error`.

### Phase 2: Component Selection
1.  **Material 3**: ALWAYS use M3 components first.
    *   `Button`, `Card`, `TextField`, `TopAppBar`.
    *   Constraint: Do not build custom components if a Material one exists.
2.  **Icons**: Use `Icons.Default` or `painterResource`.
    *   Constraint: ALWAYS provide `contentDescription` for accessibility.

### Phase 3: Layout Structure
1.  **Containers**: Use `Scaffold` for top-level screens.
2.  **Lists**: Use `LazyColumn` for scrollable content.
    *   Constraint: NEVER nest scrollable containers (e.g., `LazyColumn` inside `Column(Modifier.verticalScroll)`).
3.  **Modifiers**: Order matters.
    *   Order: Size -> Padding -> Background -> Clickable -> Padding (Internal).

## 2. Accessibility Standards (MANDATORY)

- **Touch Targets**: MUST be at least 48x48dp.
- **Content Description**: MUST be present for all meaningful images.
  - Use `null` for decorative images.
- **Text Scaling**: MUST use `sp` for text size (never `dp`).
- **Contrast**: Text MUST pass WCAG AA contrast ratio against background.

## 3. Theme Usage Rules

### Colors
- **Primary**: Main actions (FAB, Submit button).
- **Secondary**: Selection controls, highlights.
- **Surface**: Backgrounds of cards/sheets.
- **Background**: Global app background.
- **Error**: Error states.

**Pattern:**
```kotlin
// ✅ Correct
Text(
    text = stringResource(R.string.title),
    color = MaterialTheme.colorScheme.onSurface,
    style = MaterialTheme.typography.titleLarge
)

// ❌ Incorrect
Text(
    text = "Title",
    color = Color(0xFF000000),
    fontSize = 20.sp
)
```

### Typography
- **Display/Headline**: Large headers.
- **Title**: Section headers (AppBars).
- **Body**: Long form text.
- **Label**: Buttons, captions.

## 4. Verification Checklist

- [ ] **Resources**: Are all strings in `strings.xml`?
- [ ] **Theme**: Are all colors/fonts from `MaterialTheme`?
- [ ] **A11y**: Do images have descriptions? Are touch targets >48dp?
- [ ] **Layout**: Does layout work in Landscape and Dark Mode?
- [ ] **Magic Numbers**: Are all raw numbers removed/replaced with tokens?

**IF ANY CHECK FAILS: STOP. REFACTOR IMMEDIATELY.**
