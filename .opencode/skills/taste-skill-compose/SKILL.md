---
name: taste-skill-compose
description: Design and refine Compose UI/UX for MOCCA with coherent spacing, hierarchy, motion, and dark-theme visual quality.
license: MIT
compatibility: opencode
metadata:
  platform: compose-multiplatform
  focus: ui-ux
---

## What this skill does

- Produces clean, production-ready Compose screens/components with consistent visual rhythm.
- Improves information hierarchy using typography, spacing, and elevation.
- Aligns interactions with MOCCA’s dark UI language and rounded shapes.

## When to use

Use this skill for visual/interaction improvements where structure already exists and the task is mostly UX polish.

## Design rules

1. Prefer calm, neutral surfaces and subtle accent usage.
2. Use rounded interactive shapes; never use `RectangleShape` for controls.
3. Keep animations brief and meaningful (state change feedback only).
4. Preserve accessibility basics (tap targets, contrast, readable text hierarchy).
5. Reuse existing component patterns before inventing new visual primitives.

## Pairing guidance

When implementation requires strict Material 3 Expressive tokenization, load `material3-expressive-compose` together with this skill.

## Done criteria

- Visual result matches nearby MOCCA screens in tone and spacing.
- Interaction states are explicit (enabled/disabled/loading when relevant).
- No architecture leakage from UI into data/business layers.
