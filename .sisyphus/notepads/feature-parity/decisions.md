# Feature Parity — Decisions

## [2026-02-27] Initial Architecture Decisions

### Wave execution order
- Wave 0 first — all API/model changes are foundation
- Wave 1 + Wave 3 + Wave 6 can run in PARALLEL (independent)
- Wave 2 after Wave 1 (reuses chat components)
- Wave 4 after Wave 1 (reuses fuzzy search)
- Wave 5 after Waves 0-2
- Wave 7 last (polish + verification)

### TUI routes excluded
- 13 /tui/* routes are TUI-specific, not applicable for Android mobile

### Theme: No theme switching
- MOCCA is intentionally OLED-only. OpenCode Web's light/dark/zed themes are NOT implemented.

### Keybinds: Not applicable
- Keyboard shortcut customization has no mobile equivalent.

### "Open in IDE": Low priority
- VS Code/Cursor/Zed deep links not applicable to mobile context.
