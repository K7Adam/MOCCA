# AI Slop Cleanup Notes

**Updated:** 2026-04-24

## Working Definition

For MOCCA, "AI slop" is not "AI-assisted work." It is any code, documentation, or workflow artifact that appears complete but lacks enough precision, ownership, verification, or current context to help the next maintainer.

External grounding:

- Merriam-Webster's 2025 Word of the Year entry defines slop as low-quality digital content usually produced in quantity by AI: <https://www.merriam-webster.com/wordplay/word-of-the-year>
- Built In describes AI slop as high-volume, low-quality generated content with little concern for quality or originality: <https://builtin.com/artificial-intelligence/ai-slop>
- The workplace variant, "workslop," is useful for codebases: work that looks polished but lacks substance and forces others to spend time deciphering or repairing it. See the Harvard Business Review summary quoted by GovTech: <https://www.govtech.com/question-of-the-day/what-is-ai-generated-workslop-and-is-it-costing-businesses-money>

## Local Cleanup Standard

Remove or rewrite artifacts that match these patterns:

- Dead compatibility paths that no current screen, repository, or service should call.
- Generic comments that restate syntax or preserve old migration history without current value.
- Docs that describe planned architecture as if it already exists.
- Duplicate state ownership across repositories, stores, and UI state holders.
- Stale names that point maintainers toward removed contracts.
- Placeholders copied from non-Android workflows or non-MOCCA templates.

Keep artifacts that are intentionally historical, but make sure active entrypoints point to current behavior.

## 2026-04-24 RecentModel Cleanup

Removed active global model-recents storage:

- `LocalCache.getRecentModels(...)`
- `LocalCache.insertRecentModel(...)`
- `SessionRepository.getRecentModels(...)`
- `SessionRepository.addRecentModel(...)`
- `RecentModel` domain model
- `RecentModel.sq`
- legacy RecentModel merge inside `AiRuntimeConfigRepository`

Current contract:

- AI selections are persisted as `AiSelection` JSON in `AppSettings` under `ai.selection.<projectKey>`.
- Model picker recents are persisted as `AiRecentModel` JSON in `AppSettings` under `ai.recents.<projectKey>`.
- `AiRuntimeConfigRepository` reads project-scoped recents only.
- SQLDelight migration `2.sqm` drops the old `RecentModel` table for existing installs.
- `AiRecentModelsTest` guards project scoping, de-duplication, ordering, and picker limit.

## 2026-04-24 Agent Chat Event Cleanup

Removed or reduced active chat slop:

- stale `OpenChamber` and forensic-audit comments in OpenCode API/domain contracts
- obsolete Priority/TODO section labels that described old audit waves as current architecture
- duplicate `message.part.delta` persistence between `EventStreamRepository` and `StateCoordinator`
- UI-only token footers that hid reasoning-only or cache-only usage

Current contract:

- `ChatTurnReducer` is the canonical OpenCode event reducer for `message.updated`, `message.part.updated`, `message.part.delta`, session status, permission, and question events.
- Deltas are keyed by `messageID + partID`; `LocalCache.updateMessagePart(...)` must receive the part id and type instead of rewriting every text part.
- OpenCode `reasoning` parts render as MOCCA reasoning UI. `thinking` remains only as a legacy alias for older cached/imported payloads.
- Bridge protocol defaults to v2 and advertises explicit `events`, `eventReplay`, `permissions`, `questions`, `sessionStatus`, and `usage` capabilities.
- Android promoted Live Updates are reserved for active user-initiated agent runs; unsupported or passive states use standard notifications.

## Verification Commands

```powershell
.\gradlew.bat :composeApp:testAndroidHostTest --tests "com.mocca.app.domain.model.ChatTurnReducerTest"
.\gradlew.bat :composeApp:testAndroidHostTest --tests "com.mocca.app.bridge.*"
.\gradlew.bat :composeApp:allTests
.\gradlew.bat :androidApp:assembleDebug
```

Use emulator/Maestro only when user-facing runtime flows change.
