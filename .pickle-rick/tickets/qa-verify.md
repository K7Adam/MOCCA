---
id: qa-verify
title: [QA] Build, Install, and Verify Connectivity
status: Blocked
priority: High
project: mocca-rescue
created: 2026-01-17
updated: 2026-01-17
links:
  - url: ../../../linear_ticket_parent.md
    title: Parent Ticket
labels: [qa, build, verify]
assignee: Pickle Rick
---

# Description

## Problem to solve
The app handles errors gracefully, but the backend server (Main OpenCode Server on port 4096) continues to return `BunInstallFailedError`.

## Analysis
1. **Frontend:** Fixed. App no longer crashes (`JsonConvertException` is gone). It correctly reports `ServerError`.
2. **Environment:** Fixed. `opencode-supermemory` is installed in both project root and `.opencode` directory.
3. **Backend State:** The running `opencode serve` process has likely cached the "missing dependency" state or hasn't reloaded `node_modules`. It ignores the new installation.
4. **Git Server:** Manually started and verified running on port 4097.

## Required Action
The user MUST restart the `opencode serve` process to pick up the dependency changes.

## Verification Steps (After Restart)
1. Stop `opencode serve`.
2. Run `opencode serve --port 4096`.
3. Relaunch MOCCA app.
4. Connection should succeed.