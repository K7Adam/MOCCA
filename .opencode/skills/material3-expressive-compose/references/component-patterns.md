# M3 Expressive Component Patterns (MOCCA-Adapted)

Jetpack Compose component patterns with MOCCA token mappings. All examples assume `AppTheme` is in scope.

## Buttons

5 types ordered by emphasis: Filled > Filled Tonal > Elevated > Outlined > Text.

**MOCCA mapping**: Use `AppShapes.pill` for button shape. Label uses `AppTypography.labelLarge`.

### Filled Button — Primary action

```kotlin
Button(
    onClick = { /* action */ },
    shape = AppShapes.pill,
    colors = ButtonDefaults.buttonColors(
        containerColor = AppColors.primary,
        contentColor = AppColors.onPrimary
    )
) { Text("Get started", style = AppTypography.labelLarge) }
```

### Filled Tonal Button — Secondary action

```kotlin
FilledTonalButton(
    onClick = { /* action */ },
    shape = AppShapes.pill
) { Text("Save draft", style = AppTypography.labelLarge) }
```

### Elevated Button — Medium emphasis on colored backgrounds

```kotlin
ElevatedButton(
    onClick = { /* action */ },
    shape = AppShapes.pill
) { Text("Add to cart", style = AppTypography.labelLarge) }
```

### Outlined Button — Medium emphasis, neutral

```kotlin
OutlinedButton(
    onClick = { /* action */ },
    shape = AppShapes.pill,
    border = BorderStroke(AppSpacing.borderThin, AppColors.outline)
) { Text("Cancel", style = AppTypography.labelLarge) }
```

### Text Button — Lowest emphasis

```kotlin
TextButton(onClick = { /* action */ }) {
    Text("Learn more", style = AppTypography.labelLarge)
}
```

### MoccaButton (MOCCA custom)

The project has `MoccaButton` in `ui/components/modern/` — pill-shaped with `primary` background and black text. Prefer it for standard primary actions.

## Cards

**MOCCA mapping**: Use `AppShapes.card` (16dp) for standard cards, `AppShapes.moduleCard` (28dp) for dashboard modules. Background uses `AppColors.surfaceContainer`.

### Outlined Card

```kotlin
Card(
    shape = AppShapes.card,
    colors = CardDefaults.cardColors(
        containerColor = AppColors.surface,
        contentColor = AppColors.onSurface
    ),
    border = BorderStroke(AppSpacing.borderThin, AppColors.outlineVariant)
) {
    // Content with AppSpacing.cardPadding internal padding
}
```

### Filled Card

```kotlin
Card(
    shape = AppShapes.card,
    colors = CardDefaults.cardColors(
        containerColor = AppColors.surfaceContainerHighest
    )
) { /* content */ }
```

### Elevated Card

```kotlin
ElevatedCard(
    shape = AppShapes.card,
    colors = CardDefaults.cardColors(
        containerColor = AppColors.surfaceContainerLow
    ),
    elevation = CardDefaults.elevatedCardElevation(
        defaultElevation = 1.dp
    )
) { /* content */ }
```

## Text Fields

**MOCCA mapping**: Use `AppShapes.input` (32dp) or `AppShapes.extraSmall` (4dp) for text field shape. `MoccaInput` in `ui/components/modern/` is the project-standard input.

### OutlinedTextField

```kotlin
var text by remember { mutableStateOf("") }

OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Email", style = AppTypography.bodySmall) },
    isError = false,
    supportingText = { Text("We'll never share your email", style = AppTypography.bodySmall) },
    shape = AppShapes.small,
    modifier = Modifier.fillMaxWidth()
)
```

### State-Based API (current M3)

```kotlin
val state = rememberTextFieldState("")

OutlinedTextField(
    state = state,
    label = { Text("Search") },
    modifier = Modifier.fillMaxWidth()
)
```

## Dialogs

**MOCCA mapping**: Use `AppShapes.dialog` (24dp). Title uses `AppTypography.headlineSmall`, body uses `AppTypography.bodyMedium`. Use `tonalElevation` for depth; `shadowElevation` is acceptable here.

### Standard Dialog

```kotlin
AlertDialog(
    onDismissRequest = { /* dismiss */ },
    title = {
        Text("Confirm action", style = AppTypography.headlineSmall)
    },
    text = {
        Text("Are you sure you want to proceed?", style = AppTypography.bodyMedium)
    },
    confirmButton = {
        TextButton(onClick = { /* confirm */ }) {
            Text("Confirm", style = AppTypography.labelLarge)
        }
    },
    dismissButton = {
        TextButton(onClick = { /* dismiss */ }) {
            Text("Cancel", style = AppTypography.labelLarge)
        }
    },
    shape = AppShapes.dialog,
    containerColor = AppColors.surfaceContainerHigh
)
```

### PermissionRequestDialog (MOCCA custom)

The project has `PermissionRequestDialog` — mandatory for tool approval requests. Always use it instead of building custom confirmation dialogs for agent permission flows.

## Navigation Components

### NavigationBar (Bottom)

For 3–5 primary destinations on mobile.

```kotlin
NavigationBar(
    containerColor = AppColors.surfaceContainer,
    contentColor = AppColors.onSurface
) {
    NavigationBarItem(
        selected = true,
        onClick = { /* navigate */ },
        icon = { Icon(Icons.Default.Home, "Home") },
        label = { Text("Home", style = AppTypography.labelMedium) }
    )
    // ... more items
}
```

### NavigationRail (Side)

For 3–7 destinations on medium screens (600–839dp).

```kotlin
NavigationRail(
    containerColor = AppColors.surface
) {
    NavigationRailItem(
        selected = true,
        onClick = { /* navigate */ },
        icon = { Icon(Icons.Default.Home, "Home") },
        label = { Text("Home", style = AppTypography.labelMedium) }
    )
}
```

### TopAppBar Variants

```kotlin
// Small (standard)
TopAppBar(
    title = { Text("Page Title", style = AppTypography.titleLarge) },
    navigationIcon = { /* nav icon */ },
    actions = { /* action icons */ },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = AppColors.surface
    )
)

// Center-aligned
CenterAlignedTopAppBar(
    title = { Text("Title", style = AppTypography.titleLarge) }
)

// Medium/Large (collapsible)
MediumTopAppBar(title = { Text("Title") })
LargeTopAppBar(title = { Text("Title") })
```

Expressive variants (e.g. `LargeFlexibleTopAppBar`) may require `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.

## List Items

```kotlin
// One-line
ListItem(
    headlineContent = { Text("Single line", style = AppTypography.bodyLarge) }
)

// Two-line with icon
ListItem(
    headlineContent = { Text("Jane Smith", style = AppTypography.bodyLarge) },
    supportingContent = { Text("Senior Developer", style = AppTypography.bodyMedium) },
    leadingContent = { Icon(Icons.Default.Person, null) }
)

// Three-line with trailing metadata
ListItem(
    headlineContent = { Text("Meeting notes", style = AppTypography.bodyLarge) },
    supportingContent = { Text("Please review the attached notes...", style = AppTypography.bodyMedium) },
    trailingContent = { Text("3 min ago", style = AppTypography.labelSmall) },
    leadingContent = { Icon(Icons.Default.Mail, null) }
)
```

## Chips

| Variant | Use |
|---------|-----|
| Assist | Smart suggestions, shortcuts |
| Filter | Multi-select filtering |
| Input | User input tokens (e.g., email recipients) |
| Suggestion | Suggested responses |

```kotlin
FilterChip(
    selected = true,
    onClick = { /* toggle */ },
    label = { Text("Active", style = AppTypography.labelLarge) }
)
```

## Other Components Quick Reference

| Component | Compose API | MOCCA Notes |
|-----------|-------------|-------------|
| FAB | `FloatingActionButton` | Use `AppShapes.fab`, `AppColors.primary` |
| Switch | `Switch` | Standard M3; `AppColors.primary` for track |
| Slider | `Slider` | Full shape handle |
| Divider | `HorizontalDivider` | Use `AppColors.outlineVariant` color |
| Badge | Custom `ModernBadge` | Surface-based with tonal elevation |
| Progress | `LinearProgressIndicator` / `CircularProgressIndicator` | Use `AppColors.primary` for indicator |
| Snackbar | `Snackbar` | `AppColors.inverseSurface` background |
| Tooltip | `PlainTooltip` / `RichTooltip` | Standard M3 |
| Tabs | `TabRow` + `Tab` | Use `AppShapes.tabPill` for indicator |
