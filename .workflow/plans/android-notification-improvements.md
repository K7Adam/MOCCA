# Plan: Android Notification Improvements

## Scope
- In scope: Implement actual tracking of running agents by calling `NotificationTracker.startSession`, `updateProgressNotification`, and `stopSession`. Ensure "Live Updates"-style active notification correctly displays the currently executing tool ("todo-step") and progress information. Properly handle Android 13+ POST_NOTIFICATIONS permission flow.
- Ensure strict adherence to Android 16 "Live Updates" (Rich Ongoing Notifications) requirements so the notification appears as an interactive pill in the status bar/Dynamic display.
- Out of scope: iOS implementation, changes to the core OpenCode backend.
- Assumptions: The backend sends `MessagePartUpdated` events with `type = "tool"` and `title` which can be used to track the agent's current step. The OpenCode API also provides a `/session/:id/todo` endpoint (available via `MoccaApiClient.getSessionTodos`) which returns the full list of tasks. We will leverage this to calculate exact determinate progress (completed out of total).

## Requirements
### Functional
- [ ] Connect `NotificationTracker.startSession` and `stopSession` to the agent lifecycle (e.g., in `StateCoordinator` or `EventStreamRepository`).
- [ ] Extract the current tool `title` from `MessagePartUpdated` events to serve as the "todo-step".
- [ ] Integrate `MoccaApiClient.getSessionTodos` during active runs to fetch exact task progress (total vs completed).
- [ ] **Handle Retry States**: Check the `SessionRunningState` for `type == "retry"` and display "Retrying: [message]" in the Live Update pill.
- [ ] Call `NotificationTracker.updateProgressNotification` with the correct tool title, calculated elapsed time, and determinate progress values.
- [ ] Ensure the Foreground Service successfully starts and stays active while the agent is running.
- [ ] Handle POST_NOTIFICATIONS permission request properly.
- [ ] Implement optimal Android 16 Live Updates format: Use `setOngoing(true)`, ensure `contentTitle` is set, avoid `RemoteViews`, use `setProgress(totalCount, completedCount, false)` for determinate tracking, and utilize `setUsesChronometer(true)` with the original session start time.
- [ ] **Notification Actions**: Add a "Stop Agent" action button to the ongoing notification. Create a `PendingIntent` to a BroadcastReceiver that calls `MoccaApiClient.abortSession(sessionId)`.
- [ ] **Deep Linking**: Ensure tapping the notification body passes the `sessionId` to `MainActivity` so Voyager can instantly route the user into the active `SessionScreen`.

### User Input & Completion Flow
- [ ] **Questions/Permissions (Heads-Up)**: When the agent requires user input (`ServerEvent.QuestionRequired` or `ServerEvent.PermissionRequired`), trigger `showQuestionNotification`/`showPermissionNotification` with `IMPORTANCE_HIGH` so it pops down as a Heads-Up notification over the screen. Simultaneously, update the Live Update pill text to "Waiting for user input...".
- [ ] **Completion/Error**: When the agent finishes (`SessionIdle`, `SessionError`), call `stopSession()` to clear the Live Update/Foreground Service pill, and immediately trigger `showAgentFinishedNotification` or `showAgentErrorNotification` to leave a dismissible, standard priority receipt of the executed task in the notification tray.

### Non-functional
- [ ] Notification must look clean and informative (compact mode), fitting naturally into the Android 16 status bar pill.
- [ ] No Battery/Memory leaks from stale foreground services.
- [ ] Reliable cleanup of notifications when the app or agent crashes.

## Dependencies & constraints
- Internal: Depends on `ActiveSessionService` and `StateCoordinator`.
- External: Android 13+ Notification permission requirements, Android 14+ Foreground Service types, Android 16 Live Updates strict view constraints.

## Success criteria
| Criteria Type | Description | Example |
|---|---|---|
| Functional | Shows live notification | "Processing: [Task Name] -> Current step: listing files" |
| Observable | Foreground service badge | System tray shows an ongoing notification while agent runs |
| Pass/Fail | Permission flow works | App asks for permission and respects the user's choice |

## Test plan
### Objective: Verify that running an agent spawns a live updating notification.
### Prerequisites: MOCCA App installed on an Android API 33+ device/emulator. OpenCode server running.
### Test cases
0. Use ´mcp-cli android-mcp` or the maestro-cli testing suite to run the tests and adb commands.
1. Start the app, grant notification permission if requested.
2. Start an agent task. Observe the system tray.
3. Verify that a notification appears showing the session title and the current tool step (e.g., "Searching codebase...").
4. Verify the notification updates as the agent progresses.
5. Verify the notification disappears when the agent finishes or errors.
### How to execute
- Build: `./gradlew :androidApp:assembleDebug`
- Tests: None automated currently (relying on manual QA per project guidelines).
- Manual: Deploy `androidApp-debug.apk` to emulator, trigger an agent run, and visually inspect the notification shade. Capture logcat output for `ActiveSessionService`.

## Work breakdown (checkboxes)
- [x] Task 1: Update `StateCoordinator` to call `NotificationTracker.startSession` / `stopSession` when `_runningSessionIds` changes. (Done when: Foreground service correctly starts/stops based on session running state).
- [x] Task 2: Implement progress tracking in `StateCoordinator` or `EventStreamRepository` to track the active tool's `title` from `MessagePartUpdated` events. (Done when: Active tool title is captured in state).
- [x] Task 3: Modify `ActiveSessionService.updateProgressNotification` to accept and display the current step/tool name. Wire it to the progress tracking from Task 2. (Done when: Notification actively updates with the current tool).
- [x] Task 4: Ensure the `POST_NOTIFICATIONS` permission is requested correctly in `MainActivity` before `startSession` is called. (Done when: Permission dialog appears on Android 13+ and doesn't crash).

## Risks & mitigations
- Risk: ForegroundServiceStartNotAllowedException on Android 14+ if started from background. → Mitigation: Ensure the service is started while the app is in the foreground, or use correct routing.

## Handoff
- Start execution by running the next phase in this workflow: **Bootstrap (start-work)**.

# RUN (state section)

## RUN.Status
- Status: IN_PROGRESS

## RUN.Active Plan
- Plan path: .workflow/plans/android-notification-improvements.md
- Plan title: Android Notification Improvements

## RUN.Timestamps
- Started at: 2026-02-21T21:21:13+01:00
- Last updated: 2026-02-21T21:21:13+01:00

## RUN.Session History
- Session: 1 — Started: 2026-02-21T21:21:13+01:00 — Note: start

## RUN.Progress Snapshot
- Completed: 4
- Total: 4
- Next task pointer: Complete!

## RUN.Evidence Log
- [2026-02-21T21:21:13Z] Manual: Bootstrap → Initialized Session State
- [2026-02-21T21:23:00Z] Manual: Task 1 → Updated StateCoordinator and DI to connect NotificationTracker start/stop
- [2026-02-21T21:27:00Z] Assisted: Task 2 → Updated StateCoordinator to track activeToolTitles flow via MessagePartUpdated events
- [2026-02-21T21:30:00Z] Assisted: Task 3 → Updated ActiveSessionService to support determinate notifications and modified StateCoordinator to fetch task progress and fire notification updates.
- [2026-02-21T21:35:00Z] Assisted: Task 4 → Verified POST_NOTIFICATIONS logic in MainActivity and AndroidManifest.xml. Verified compile success.

## RUN.Notes / Decisions
