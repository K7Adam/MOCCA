# MOCCA UI/UX Redesign Plan

## Overview
Comprehensive UI/UX redesign based on 2025-2026 best practices for Kotlin Multiplatform Compose.

## Research Summary
- **Stack**: Kotlin 2.3.0, Compose 1.9.3, Koin 4.1.1, Voyager 1.1.0-beta03
- **Theme**: Pitch Black OLED (#000000), Mint Green (#00D9A5), Rounded corners
- **Glass**: AndroidLiquidGlass (backdrop 1.0.6), Haze 1.7.2, Liquid 1.1.1
- **Markdown**: multiplatform-markdown-renderer 0.27.0 with syntax highlighting

---

## Phase 1: Theme System Enhancements

### 1.1 AppColors.kt
- Add semantic color tokens (surface, onSurface, etc.)
- Add OLED-optimized elevated blacks
- Add animated color transitions for theme changes

### 1.2 AppTypography.kt  
- Add variable font support
- Add text appearance animations
- Enhance accessibility with dynamic sizing

### 1.3 AppShapes.kt
- Add continuous corners for liquid glass
- Add animated shape transitions

### 1.4 AppSpacing.kt
- Add responsive spacing tokens
- Add animation duration tokens

---

## Phase 2: Modern Animation Integration

### 2.1 Spring Animations
- Add standard spring constants (DampingRatioMediumBouncy, StiffnessLow)
- Apply to all animated components

### 2.2 LazyList Animations
- Add animateItemPlacement() with spring specs
- Add enter/exit animations for items

### 2.3 AnimatedContent
- Add custom transitionSpecs for state changes
- Add SharedTransitionLayout for screen transitions

---

## Phase 3: Glass Effects Enhancement

### 3.1 LiquidBackdrop Improvements
- Add vibrancy + blur + lens effects chain
- Add OLED-optimized color overlays
- Add luminance-based dynamic adaptation

### 3.2 Glass Components
- Refine GlassmorphicCard with proper effects
- Enhance GlassSurface with proper highlights/shadows

---

## Phase 4: Chat UI Enhancements

### 4.1 MessageBubble
- Add streaming text animation
- Add syntax highlighting for code blocks
- Add smooth expand/collapse for tool results
- Add shimmer loading states

### 4.2 ChatContent
- Add smooth scroll animations
- Add skeleton loading states
- Enhance auto-scroll behavior

### 4.3 ChatInputBar
- Add haptic feedback
- Add smooth mode transitions
- Enhance attachment preview animations

---

## Phase 5: Navigation & Transitions

### 5.1 SwipePanelLayout
- Add predictive back gesture support
- Enhance spring animations
- Add shared element transitions

### 5.2 UnifiedFloatingBottomBar
- Add liquid glass morphing animations
- Enhance mode transition smoothness

---

## Phase 6: Screen Refinements

### 6.1 MainScreen
- Enhance panel transitions
- Add connection status animations

### 6.2 DashboardPanel
- Enhance module card animations
- Add stagger animation for modules

### 6.3 SettingsScreen
- Modernize toggle switches
- Add smooth section animations

---

## Implementation Priority

1. Theme tokens (Foundation - affects everything)
2. Animation utilities (Shared across components)
3. Primitive components (Button, Card, Input)
4. Glass components (Used everywhere)
5. Complex components (MessageBubble, ChatContent)
6. Screens (MainScreen, DashboardPanel)
7. Navigation (SwipePanelLayout, BottomBar)

---

## Build Verification

Run after each phase:
```bash
./gradlew :androidApp:assembleDebug
```

Must compile without errors or warnings before proceeding to next phase.
