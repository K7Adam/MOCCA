# Liquid Glass QA Checklist

## Visual Correctness

### Dark Mode (Primary Theme)
- [ ] Glass surfaces render with pitch black (#000000) background
- [ ] Mint green accent (#00D9A5) appears correctly on glass edges
- [ ] Specular highlight visible at top edge of glass surfaces
- [ ] Inner shadow visible at bottom edge of glass surfaces
- [ ] Border stroke is subtle but visible (White 15% alpha)
- [ ] Glass tint provides adequate contrast for content
- [ ] Text on glass is readable with WCAG AA contrast (4.5:1)

### Light Mode (Secondary Theme)
- [ ] Glass surfaces render with appropriate light background
- [ ] Specular highlight more prominent (White 55% alpha)
- [ ] Border stroke visible (White 30% alpha)
- [ ] Dark overlay provides adequate depth
- [ ] Text on glass is readable with WCAG AA contrast

### Visual Effects
- [ ] Refraction effect creates subtle lens distortion (API 33+)
- [ ] Chromatic aberration creates prismatic fringing at edges
- [ ] Blur effect creates frosted glass appearance
- [ ] Gradient transitions are smooth, not banded
- [ ] No visual artifacts or tearing during animations

## API Level Compatibility

### API 33+ (Full AGSL Effect)
- [ ] AGSL shader compiles and runs without errors
- [ ] RuntimeShader applies all glass effects
- [ ] Refraction is visible when enabled
- [ ] Chromatic aberration is visible when enabled
- [ ] Performance is acceptable (60fps target)

### API 31-32 (RenderEffect Fallback)
- [ ] RenderEffect blur applies correctly
- [ ] Glass surfaces appear frosted
- [ ] Specular and shadow effects visible
- [ ] No crashes or visual artifacts

### API < 31 (Not Applicable - minSdk = 31)
- [ ] N/A - This app has minSdk = 31

## Performance

### Frame Rate
- [ ] 60 fps maintained on mid-range device (Snapdragon 680 or equivalent)
- [ ] No frame drops during glass surface transitions
- [ ] Scroll performance not affected by glass surfaces
- [ ] Animation duration respects user's animation scale settings

### Memory & Overdraw
- [ ] No glass-on-glass stacking (max 1 glass layer per depth level)
- [ ] Blur applied at layer level, not per-composable
- [ ] No memory leaks from shader allocations
- [ ] Overdraw within acceptable limits (max 2x in glass regions)

### Shader Performance
- [ ] BLUR_SAMPLES default (4) provides good quality/performance balance
- [ ] Heavy blur samples (5) used only for prominent surfaces
- [ ] Light blur samples (3) used for performance-sensitive areas

## Accessibility

### System Settings Integration
- [ ] Reduced-transparency mode: glass becomes solid surface
- [ ] Reduced-motion mode: refraction/parallax animation disabled
- [ ] High contrast mode: text contrast auto-adjusted

### Touch Targets
- [ ] All interactive glass elements have ≥ 48×48.dp touch targets
- [ ] Touch targets are not obscured by glass overlays
- [ ] Focus indicators visible on glass surfaces

### Screen Readers
- [ ] Glass surfaces have appropriate content descriptions
- [ ] Screen reader can navigate glass content
- [ ] Focus traversal order is logical

### Text Contrast
- [ ] Text on glass meets WCAG AA (4.5:1) in dark mode
- [ ] Text on glass meets WCAG AA (4.5:1) in light mode
- [ ] Tint opacity auto-adjusts when background brightness shifts

## Component-Specific Tests

### GlassAppBar
- [ ] Renders full-width with no corner rounding
- [ ] Navigation icon is accessible
- [ ] Title is centered or start-aligned appropriately
- [ ] Action buttons are accessible
- [ ] StatusBar padding applied correctly

### GlassBottomBar
- [ ] Renders with rounded corners (32dp default)
- [ ] Navigation items are evenly spaced
- [ ] NavigationBar padding applied correctly
- [ ] Floating variant centers on screen
- [ ] Height animations are smooth

### GlassSheet
- [ ] Renders with rounded top corners
- [ ] Drag handle is visible and functional
- [ ] Swipe-to-dismiss works correctly
- [ ] Content scrolls independently
- [ ] NavigationBar padding applied when needed

### GlassSurface
- [ ] Applies glass effect correctly
- [ ] Shape clipping works for all shape types
- [ ] Content is not clipped unexpectedly
- [ ] Modifier.glass() works on existing components

## Edge Cases

### Content Scenarios
- [ ] Glass renders correctly over images
- [ ] Glass renders correctly over gradients
- [ ] Glass renders correctly over other glass (should show warning)
- [ ] Glass renders correctly in RTL layouts

### Rotation & Configuration
- [ ] Glass surfaces survive configuration changes
- [ ] Glass surfaces work in split-screen mode
- [ ] Glass surfaces work in picture-in-picture mode (if applicable)

### Error Handling
- [ ] Shader compilation failures fall back gracefully
- [ ] Invalid parameters are clamped to valid ranges
- [ ] No crashes on low-memory devices

## Regression Tests

### Existing Screens
- [ ] No regressions on non-glass screens
- [ ] Existing theme still applies correctly
- [ ] Existing components render as expected
- [ ] Navigation still works correctly

## Debug Tools

### LiquidGlassDebugOverlay
- [ ] Shows correct shader uniform values
- [ ] Shows correct accessibility state
- [ ] Shows correct performance metrics
- [ ] Overdraw visualization works correctly

### Validation
- [ ] GlassDebugValidator catches common configuration issues
- [ ] Warnings shown for problematic parameter combinations
- [ ] Debug panel allows real-time token adjustment

---

## Sign-Off

| Tester | Date | Device | API Level | Result |
|--------|------|--------|-----------|--------|
| ______ | ____ | _______ | _________ | ______ |
| ______ | ____ | _______ | _________ | ______ |
| ______ | ____ | _______ | _________ | ______ |

---

## Notes

- Record any visual differences between device models
- Note performance characteristics on different API levels
- Document any workarounds applied
- Track any issues found for follow-up
