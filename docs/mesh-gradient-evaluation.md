# MeshGradient evaluation for MOCCA

## Date
2026-05-07

## Source reviewed
- https://composables.com/jetpack-compose/androidx.compose.ui/ui/modifiers/meshGradient/examples
- Android API references for `MeshGradientRenderer` (Compose UI)

## Recommendation
Mesh gradients can be integrated in MOCCA **selectively** for decorative surfaces, but should not become a default background primitive across all screens yet.

## Where it makes sense
1. **Onboarding / empty states / splash transitions** where visual identity matters and there is little content density.
2. **Hero headers** in Settings/About/Project intro areas with static or slow animation.
3. **Modal moments** (confirmation or success states) where we already allow stronger depth separation.

## Where it should be avoided
1. **Primary chat surfaces** and long reading surfaces where color noise hurts readability.
2. **High-frequency recomposition areas** (message streaming rows, live tokens) to avoid GPU/CPU pressure.
3. **Any place that competes with state signaling** (errors, warning badges, diff highlights).

## Technical guardrails for MOCCA
- Keep mesh gradients inside `ui/theme` or narrowly scoped UI components, not business/domain layers.
- Default to **static mesh** first; animate only when profiling confirms acceptable frame stability.
- Provide fallback to existing gradient/solid tokens for low-end devices or reduced-motion settings.
- Keep contrast compliant for text and controls drawn over mesh surfaces.

## Rollout strategy
1. Build one reusable `MeshSurface` component in `composeApp` UI layer.
2. A/B test on a non-critical screen (e.g., onboarding hero).
3. Validate with emulator performance pass + screenshot review.
4. Expand only if no readability regressions and no jank.

## Overall verdict
**Yes, it can be meaningful and beneficial** for branding and perceived polish in MOCCA, as long as usage remains constrained to decorative/low-density UI zones with strict readability and performance checks.
