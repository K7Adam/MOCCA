---
id: viz-cons-001
title: "Fix InlineConnectionStatus logic bug"
status: Done
priority: Urgent
project: project
created: 2026-01-21
updated: 2026-01-21
links:
  - url: ../linear_ticket_parent.md
    title: Parent Ticket
labels: [bug, ui]
assignee: Pickle Rick
---

# Description

## Problem to solve
\`InlineConnectionStatus\` currently shows 'NO SIGNAL' when \`isConnected\` is true, which is backwards and confusing.

## Solution
Flip the logic: \`isConnected == true\` -> 'CONNECTED', \`isConnected == false\` -> 'NO SIGNAL'.
Also ensure it uses overhaul colors.
