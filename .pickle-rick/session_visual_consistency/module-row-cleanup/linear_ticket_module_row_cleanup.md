---
id: viz-cons-003
title: "Update ModuleRowItem to hide dots in normal state"
status: Done
priority: High
project: project
created: 2026-01-21
updated: 2026-01-21
links:
  - url: ../linear_ticket_parent.md
    title: Parent Ticket
labels: [ui, ux]
assignee: Pickle Rick
---

# Description

## Problem to solve
\`ModuleRowItem\` shows a status dot for every item. If an item is enabled and connected, it shouldn't distract the user.

## Solution
Only show the \`StatusDot\` if \`!isEnabled\` or \`!isConnected\`. Alternatively, make the 'Connected' dot extremely small (4dp) or dimmed to reduce visual weight.
