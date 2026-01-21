---
id: viz-cons-004
title: "Refactor SkillsEngineModule for visual consistency"
status: Done
priority: Medium
project: project
created: 2026-01-21
updated: 2026-01-21
links:
  - url: ../linear_ticket_parent.md
    title: Parent Ticket
labels: [ui, polish]
assignee: Pickle Rick
---

# Description

## Problem to solve
\`SkillsEngineModule\` uses 8dp dots for active skills, which is inconsistent with the new minimalist goal.

## Solution
Reduce skill dot size to 4dp-6dp or replace with a subtle underline/accent if active. Align with \`ModuleRowItem\` changes.
