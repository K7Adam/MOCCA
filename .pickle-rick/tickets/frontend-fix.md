---
id: frontend-fix
title: [Frontend] Harden MoccaApiClient against JSON Errors
status: Done
priority: High
project: mocca-rescue
created: 2026-01-17
updated: 2026-01-17
links:
  - url: ../../../linear_ticket_parent.md
    title: Parent Ticket
labels: [android, frontend, resilience]
assignee: Pickle Rick
---

# Description

## Problem to solve
The Android app crashes with `JsonConvertException` when the API returns an error object (JSON) instead of the expected success response (JSON List).

## Solution
1. Modify `MoccaApiClient.kt` (and `RetryPolicy.kt` if needed) to handle serialization errors gracefully.
2. Implement a wrapper or try-catch block to detect if the response is an error object.
3. Display a user-friendly error message (e.g., "Server Error: [Error Name]") instead of crashing.

# Comments
- Implemented `ServerErrorResponse` model in `Models.kt`.
- Implemented `safeRequest` helper in `MoccaApiClient.kt` that checks response status and parses error bodies before attempting to parse the success model.
- Updated `listSessions`, `getAgents`, `getToolIds`, `getCommands`, `getFormatters`, `getLspStatus`, `getMcpStatus` to use `safeRequest`.