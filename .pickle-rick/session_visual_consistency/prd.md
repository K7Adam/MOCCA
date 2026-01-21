# Green Dot Reduction & Visual Consistency Audit PRD

## HR Eng

| Green Dot Reduction & Visual Consistency Audit PRD |  | Reduce visual noise by minimizing status indicators and aligning with minimalist overhaul references. |
| :---- | :---- | :---- |
| **Author**: Pickle Rick **Contributors**: Engineering **Intended audience**: Engineering, Design | **Status**: Draft **Created**: 2026-01-21 | **Context**: /ui_overhaul_refactoring/ |

## Introduction

The current MOCCA UI implementation uses `StatusDot` and green accents too liberally, particularly in the modular dashboard. This deviates from the sleek, minimalist reference designs which prioritize content over status noise.

## Problem Statement

**Current Process:** Status dots are shown for every 'connected' or 'enabled' item in the dashboard modules (MCP servers, skills, agents).
**Primary Users:** Power users who want a focused, high-performance interface.
**Pain Points:** Visual clutter, 'Christmas tree' effect in the dashboard, inconsistent use of green accents.
**Importance:** To achieve the 'God Mode' aesthetic, the UI must be clean and only highlight anomalies (errors/offline states).

## Objective & Scope

**Objective:** Reduce the usage of green status dots and ensure visual consistency across all components.
**Ideal Outcome:** A clean, minimalist UI where status is implicit when 'normal' and explicit only when 'abnormal'.

### In-scope or Goals
- Modify `ModuleRowItem` to hide or dim the status dot when 'normal' (Connected & Enabled).
- Fix logic errors in `InlineConnectionStatus`.
- Update `StatusDot` and `StatusSquare` to match overhaul color palette and glow effects.
- Audit `TerminalTopBar` and `ChatTopBar` for unnecessary dots.

### Not-in-scope or Non-Goals
- Complete redesign of the dashboard layout.
- Adding new features to MCP or Skills.

## Product Requirements

### Critical User Journeys (CUJs)
1. **Dashboard Focus**: User opens the dashboard and sees a clean list of modules without being distracted by a wall of green dots.
2. **Anomaly Detection**: User immediately notices if an MCP server goes offline because a red indicator appears in an otherwise clean list.

### Functional Requirements

| Priority | Requirement | User Story |
| :---- | :---- | :---- |
| P0 | Hide 'Connected' dot in `ModuleRowItem` | As a user, I want the UI to be clean when everything is working correctly. |
| P0 | Fix `InlineConnectionStatus` text logic | As a user, I want to see 'CONNECTED' when I am actually connected. |
| P1 | Align status colors with Overhaul Palette | As a designer, I want status colors to match the mint green/alert red theme. |
| P1 | Update `StatusDot` to use subtle glow | As a user, I want status indicators to look modern and premium. |
| P2 | Audit TopBar for consistency | As a user, I want a consistent header experience across screens. |

## Assumptions

- 'Normal' state means Enabled=True and Connected=True (or equivalent).

## Risks & Mitigations

- **Risk**: Users might think something is broken if there's no indicator. -> **Mitigation**: Use subtle text or dimmed indicators, and ensure 'Offline' is highly visible.
