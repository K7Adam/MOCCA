# MOCCA

Android companion app for OpenCode, built with Kotlin Multiplatform, Compose Multiplatform, Koin, Voyager, SQLDelight, and Material 3 Expressive.

MOCCA is Android-only. Shared code lives in `composeApp`; `androidApp` stays as the platform bootstrap.

## Current Product Shape

- **Bridge-first connection**: pair with the MOCCA CLI bridge by QR code. Direct OpenCode HTTP/SSE remains available as an advanced legacy path.
- **AI sessions**: streaming chat, session history, tool permissions, question replies, forks, reverts, sharing, and summaries.
- **Runtime config**: provider, model, variant, agent, and mode selection come from the CLI bridge when available. Model picker recents are project-scoped `AiRecentModel` entries stored in `AppSettings`.
- **Project tools**: files, git, terminal, MCP, commands, agents, and update checks are exposed through repositories and state stores.
- **Server-first data**: primary user-facing data emits cache first, refreshes from network/bridge, updates `LocalCache`, then emits fresh state.
- **UI system**: Material 3 Expressive shell with app-owned `AppColors`, `AppTypography`, `AppShapes`, and motion from `MaterialTheme.motionScheme`.

## Screenshots

UI screenshots are produced by deterministic Maestro flows against a visible Android emulator and committed to the catalog for review. The catalog is regenerated from the flows rather than hand-edited, so the on-disk PNGs always reflect the current Compose surface area.

### Catalog Generation

- Test plan: `maestro-workspace/testplans/screenshot-catalog.yaml`
- Flow set: `maestro-workspace/flows/catalog/*.yaml`
- Runner: `maestro-workspace/capture-screenshot-catalog.ps1`
- Output: `screenshots/catalog_*.png`
- Per-screenshot index: `screenshots/README.md`

Regenerate locally after the emulator is running:

```powershell
.\maestro-workspace\capture-screenshot-catalog.ps1
```

In CI, run the **Screenshot Catalog** workflow to refresh the artifact bundle.

### Catalog Flows

| Flow | Coverage |
|---|---|
| `capture_panels.yaml` | Three-panel shell: Sessions, Chat, Tools (top and scrolled) |
| `capture_sessions_chat.yaml` | Sessions list before and after creating a session, chat with new session |
| `capture_files_git_terminal.yaml` | Git Status, Branches, Log, Remotes, Tags, Terminal |
| `capture_settings_skills_flags.yaml` | Settings scroll positions, Skills, Feature Flags |
| `capture_onboarding_connection.yaml` | First-run and chat-empty states |
| `capture_mcp.yaml` | MCP dashboard module, JSON config, resources |

`capture_workspace_explorer.yaml` covers the workspace explorer, dashboard, git, and chat tabs. It exists in the flow set but is not currently wired into `screenshot-catalog.yaml`.

### What The Catalog Reflects

The catalog captures the Material 3 Expressive dark theme surface area: the three-panel `SwipePanelLayout` shell, the modern chat primitives (streaming, reasoning, file, sub-task, tool parts), the dashboard modules, the Git, Terminal, Files, Settings, MCP, Skills, and Feature Flags screens, and the first-run onboarding state. The `screenshots/README.md` index maps each PNG to the surface it documents and lists surfaces that the current runtime profile cannot reach (for example, the manual server form on a fully cleared install).

## Architecture

```text
Compose UI
  -> Voyager ScreenModels / state holders
  -> Repositories
      -> LocalCache (SQLDelight + AppSettings)
      -> ChatTurnReducer (session/message/part event state)
      -> MOCCA CLI bridge
      -> legacy OpenCode HTTP/SSE fallback
```

Key boundaries:

- `ConnectionManager` owns the legacy OpenCode `HttpClient`; consumers use `ApiExecutor.execute {}`.
- `BridgeConnectionManager` owns the CLI bridge client and v2 capability checks.
- `EventStreamRepository` ingests OpenCode events, reduces them through `ChatTurnReducer`, and persists streaming deltas by `messageId + partId`.
- `StateCoordinator` fans canonical events into `AppStateStore` and `ChatStateStore`; it does not append stream tokens a second time.
- `AiRuntimeConfigRepository` owns provider/model/agent/mode selection and project-scoped AI recents.
- Repositories depend on `LocalCache`, not raw SQLDelight drivers.

## Persistence

SQLDelight schema lives in `composeApp/src/commonMain/sqldelight/com/mocca/app/db/`.

Current durable tables:

- `Agent.sq`
- `AppSettings.sq` (`ai.selection.*`, `ai.recents.*`, bridge target settings, app settings)
- `Command.sq`
- `FileInfo.sq`
- `Message.sq`
- `ServerConfig.sq`
- `Session.sq`
- `SessionTodo.sq`

Migration `2.sqm` removes the retired global `RecentModel` table. Do not add a new global model-recents table; use project-scoped `AiRecentModel` persistence through `LocalCache`.

Streaming message persistence is part-addressable: text, reasoning, and legacy thinking deltas update only the affected message part. Avoid full-message JSON rewrites in token loops.

## Quick Start

### Prerequisites

- JDK 17+
- Android SDK API 36
- OpenCode CLI
- MOCCA CLI bridge available on `PATH`

### Start the MOCCA CLI Bridge

```bash
mocca-cli
```

For Tailscale pairing:

```bash
mocca-cli tailscale
# or
mocca-cli --tailscale
```

If port `17653` is already in use:

```bash
mocca-cli tailscale --port 17654
```

### Legacy Direct OpenCode Server

Use this only for direct server testing or fallback verification:

```bash
export OPENCODE_SERVER_USERNAME=opencode
export OPENCODE_SERVER_PASSWORD=your_password
opencode serve --port 4096
opencode serve --port 4096 --hostname 0.0.0.0
```

## Build And Test

```powershell
# Build debug APK
.\gradlew.bat :androidApp:assembleDebug

# Run shared host tests
.\gradlew.bat :composeApp:allTests

# Start visible emulator
.\maestro-workspace\start-emulator.ps1

# Run smoke plan
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml
```

## Tech Stack

| Component | Version |
|---|---|
| Kotlin | 2.3.0 |
| Compose Multiplatform | 1.9.3 |
| Material 3 Expressive | 1.11.0-alpha04 |
| Ktor | 3.0.3 |
| SQLDelight | 2.2.1 |
| Koin | 4.1.1 |
| Voyager | 1.1.0-beta03 |
| AGP | 9.0.0-rc03 |

## Engineering Rules

- Keep business logic out of composables; use ScreenModels, repositories, and state stores.
- Use `ApiExecutor` for legacy HTTP access and bridge repositories for CLI-backed capabilities.
- Keep model picker recents project-scoped through `AiRecentModel`; `SessionRepository` must not persist model recents.
- Route OpenCode `message.part.delta`, permission, question, status, and usage events through `ChatTurnReducer` before exposing chat UI state.
- Treat `reasoning` as the canonical OpenCode part type; `thinking` is a legacy import/display alias only.
- Prefer deleting obsolete compatibility paths over layering new adapters on top of them.
- Use absolute paths in scripts and agent docs.
- Do not add iOS or desktop targets.

## AI Slop Cleanup Standard

For this repository, "AI slop" means code or docs that look complete but add low-quality maintenance burden: stale architecture claims, unused compatibility paths, generic placeholder docs, vague comments, duplicate state ownership, and old names that obscure the current contract.

Cleanup work should leave a test or build signal behind, update the docs that future agents will read, and remove dead paths instead of hiding them behind new wrappers.

See `docs/ai-slop-cleanup.md` for the current cleanup notes and sources.

## License

MIT
