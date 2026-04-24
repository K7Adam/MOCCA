# Product Guidelines: MOCCA

## Visual Style & Design System
MOCCA uses a restrained **Material 3 Expressive** design system for a dense developer tool. Motion should clarify state changes and preserve spatial context, not decorate routine actions.
- **Adaptive Layouts:** The UI must respond gracefully to different screen sizes and orientations, leveraging Material 3 adaptive components.
- **Expressive Motion:** Use `MaterialTheme.motionScheme` and existing app tokens. Avoid custom animation constant objects.
- **Token Ownership:** Feature UI uses `AppColors`, `AppTypography`, and `AppShapes`; Material bridge code owns direct Material defaults.

## Prose & Voice
Our communication style is **clear, compact, and task-focused**.
- **Clarity:** Use plain language for AI runtime state, connection state, and tool permissions.
- **Specificity:** Prefer concrete status, action, and error text over generic reassurance.
- **No Filler:** Do not add visible text that merely explains the UI layout or repeats button labels.

## Interaction Principles
- **Efficiency-First UX:** Design for repeated developer tasks. Chat, model selection, permissions, git, files, and terminal actions should be reachable without ceremony.
- **Contextual Transitions:** Use animations to bridge the gap between different app states, helping users maintain their mental model of the application's flow.
- **Broad Accessibility:** Ensure all interactive elements are accessible, supporting screen readers, high-contrast modes, and multiple input methods (touch, keyboard).

## Branding & Tone
MOCCA should feel like a serious mobile workbench for AI-assisted development.
- **Information Density:** Prefer scannable, organized panels over marketing-style hero sections or decorative cards.
- **Professional Palette:** Maintain contrast and hierarchy without one-note color themes.
