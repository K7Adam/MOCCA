---
id: backend-fix
title: [Backend] Fix BunInstallFailedError on opencode Server
status: Done
priority: Urgent
project: mocca-rescue
created: 2026-01-17
updated: 2026-01-17
links:
  - url: ../../../linear_ticket_parent.md
    title: Parent Ticket
labels: [backend, bug]
assignee: Pickle Rick
---

# Description

## Problem to solve
The local `opencode` server (port 4096) returns a `BunInstallFailedError` when the app tries to connect. This persists even after attempting to install `opencode-supermemory` in the project root. The server is likely running from a different context or failing to detect the package. The Git server (port 4097) is also failing to connect, likely a cascade failure.

## Solution
1. Located `.opencode/package.json` in the project directory.
2. Discovered `opencode-supermemory` was missing from `dependencies`.
3. Added `opencode-supermemory` to `.opencode/package.json`.
4. Ran `bun install` inside `.opencode`.

# Comments
- **Success**: `opencode-supermemory` is now installed in the correct location (`.opencode/node_modules`).
- **Note**: The user may need to restart `opencode serve` for the changes to take effect if hot-reloading is not enabled for package manifest changes.