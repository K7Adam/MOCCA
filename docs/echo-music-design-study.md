# Echo Music Design Study for MOCCA

## Source Snapshot

- Repository: `https://github.com/EchoMusicApp/Echo-Music`
- Local research clone: `C:\Users\ruzaq\AppData\Local\Temp\mocca-echo-music`
- Inspected commit: `4c8a2dcb`
- Primary files reviewed:
  - `app/src/main/kotlin/com/music/echo/ui/theme/Theme.kt`
  - `app/src/main/kotlin/com/music/echo/ui/theme/Type.kt`
  - `app/src/main/kotlin/com/music/echo/constants/Dimensions.kt`
  - `app/src/main/kotlin/com/music/echo/ui/component/AppNavigation.kt`
  - `app/src/main/kotlin/com/music/echo/ui/component/Items.kt`
  - `app/src/main/kotlin/com/music/echo/ui/component/Preference.kt`
  - `Screenshots/sc_1.png` through `Screenshots/sc_4.png`

## Echo Design Language

Echo is not visually defined by one color. It is defined by a dark tonal Material 3 system with strong container hierarchy, rounded grouped surfaces, and floating controls.

- Color: `rememberDynamicColorScheme` seeded with `0xFFED5564`, `ColorSpec.SPEC_2025`, `PaletteStyle.TonalSpot`, optional system dynamic color, and optional pure black mode.
- Canvas: black or near-black background with tonal surface containers. Primary color is used for selection, progress, and focused states rather than as a full-page wash.
- Shapes: connected grouped items use 24dp outer corners and 4dp inner corners. Individual cards, nav shells, and bottom controls are large pills or soft rounded rectangles.
- Navigation: bottom navigation is a floating 72dp pill. Selected items sit inside their own pill rather than relying only on an underline or icon tint.
- Density: content is compact and scannable. List items use 64dp rows, 48dp thumbnails/icons, 16dp horizontal padding, and sparse dividers.
- Typography: mostly Material scale, with hierarchy from weight, size, and spacing rather than ornate custom type. Titles are confident; supporting text is quiet.
- Motion: shape and color transitions are short and tactile. The UI avoids decorative motion on persistent backgrounds.

## MOCCA Translation

MOCCA should copy the language, not the music-app metaphor. The AI companion interface keeps its three-panel IA, terminal/chat semantics, and productivity density.

- Replace the flat blue-gray palette with a tonal dark system: near-black base, layered surface containers, blue primary, teal secondary, and pink tertiary.
- Make the persistent bottom navigation an Echo-style floating toolbar: 72dp height, tonal container, selected pill, no heavy border.
- Use connected group shapes as first-class app tokens so sessions, settings, and tool lists can read as structured groups.
- Prefer depth through container color and tonal elevation. Keep outlines for focus, warnings, and explicit state only.
- Keep MOCCA's display font and monospace code font, but remove negative letter spacing and use stronger title weights sparingly.
- Replace animated decorative blob backgrounds with a static tonal ambient background that does not compete with chat or terminal content.

## Current Implementation Scope

The first implementation pass updates shared tokens and wrappers:

- `AppColors`: Echo-inspired dark tonal palette and semantic accent roles.
- `AppTheme`: complete tertiary token bridge into Material 3.
- `AppShapes` and `AppSpacing`: connected groups and 72dp floating toolbar tokens.
- `PersistentNavRow` and `MainScreen`: floating tonal bottom nav with selected pill state.
- `MoccaCard`, `MoccaListItem`, and `ModuleCard`: grouped shapes, tonal containers, and pill header actions.
- `DynamicExpressiveBackground`: static dark tonal wash instead of animated radial blobs.

Follow-up screen work should apply connected top/middle/bottom shapes inside settings, sessions, provider/model pickers, and terminal preference groups where the UI currently renders separated cards.
