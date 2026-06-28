# Project Tracks

Active architecture and workflow notes live in the root README and root AGENTS.md.

Historical tracks are kept under `conductor/archive/` for audit context. They are not current implementation instructions unless a task explicitly says to resume that archived track. The CLI bridge refactor masterplan (`docs/superpowers/plans/2026-04-20-mocca-cli-refactor-masterplan.md`) is retained for reference; the bridge-first migration it describes is largely complete as of the v1.0.1 alpha.

Recent cleanup notes:

- 2026-04-24: removed the legacy global `RecentModel` path and moved documentation to the bridge-first, project-scoped `AiRecentModel` contract.
