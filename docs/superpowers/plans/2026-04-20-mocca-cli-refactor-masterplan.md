# MOCCA CLI Bridge Refactor Masterplan

> Status: research-backed architecture and migration plan
> Date: 2026-04-20
> Workspace: `C:\Users\ruzaq\AndroidStudioProjects\MOCCA`
> Lunel clone analyzed at: `C:\Users\ruzaq\AppData\Local\Temp\mocca-lunel-research\lunel`
> Lunel revision analyzed: `ba88d5f`

## Goal

Refactor MOCCA from an Android app that talks directly to one OpenCode HTTP/SSE
server into a robust Material 3 Expressive Android control client backed by a
local `mocca-cli` bridge that the user can run from a project terminal:

```bash
npx mocca-cli
```

The CLI should own the local developer environment, OpenCode runtime, project
filesystem, git, terminal, process, port, monitor, and provider/MCP capability
surface. The Android app should become a reliable, fast, elegant client that
pairs to the CLI and renders typed state from it.

The target product should be simple to explain:

1. Start `npx mocca-cli` in a project.
2. Scan or enter the pairing code in MOCCA.
3. Use OpenCode, files, git, terminal, MCP/tools, provider auth, and project
   status from the phone with durable reconnect and local-first performance.

## Research Inputs

### Lunel

- Repository: `https://github.com/lunel-dev/lunel`
- Website: `https://lunel.dev`
- Main packages analyzed:
  - `cli/`: Node CLI published as `lunel-cli`, runnable through `npx lunel-cli`.
  - `app/`: Expo/React Native mobile app.
  - `manager/`: Bun manager for pairing, proxy assignment, reattach sessions,
    audit, health, and gateway coordination.
  - `proxy/`: Bun gateway that relays encrypted app/CLI sessions and raw TCP
    tunnel data.
  - `pty/`: Rust PTY process using `portable-pty` and `wezterm-term`.

### MOCCA

Primary files inspected:

- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaApiClient.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaSseClient.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/ConnectionManager.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/EventStreamRepository.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/StateCoordinator.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/RealtimeSyncService.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/SessionRepository.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/GitRepository.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/SystemMonitorRepository.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/terminal/TerminalScreenModel.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/App.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt`
- `composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt`

### Android Knowledge Base Checks

Queried and fetched before planning:

- `kb://android/develop/ui/compose/performance/bestpractices`
- `kb://android/develop/ui/compose/state-hoisting`
- `kb://android/develop/ui/compose/designsystems/material3`
- `kb://android/develop/background-work`

Planning implications:

- Avoid expensive work in composables; precompute in state holders.
- Use stable lazy list keys and content types.
- Use `derivedStateOf` and deferred state reads for high-frequency scroll,
  terminal, and streaming updates.
- Hoist business state to screen/state holders; keep simple UI element state
  local.
- Use Material 3/Expressive theming through the MOCCA app token system.
- Long-lived background network behavior must respect Android lifecycle and
  power constraints. A foreground connection can be rich; background work must
  be constrained and explicit.

## Key Lunel Lessons To Adopt

### 1. Mobile App As Thin Rendering Client

Lunel's strongest product decision is that the mobile app is not the source of
truth. The CLI owns project-side capability and emits typed state. MOCCA should
adopt this model.

Apply to MOCCA:

- The Android app should not need to know the OpenCode server URL, Basic auth,
  OpenCode endpoint quirks, shell command formats, or PTY implementation.
- App repositories should talk to a MOCCA bridge protocol.
- OpenCode REST/SSE should become one CLI backend implementation, not the app's
  primary contract.

### 2. Unified Request/Response/Event Protocol

Lunel uses one typed protocol with namespaces, request IDs, responses, events,
and system messages.

Apply to MOCCA:

- Define `Mocca Bridge Protocol v1`.
- Use namespaces such as `ai`, `fs`, `git`, `terminal`, `process`, `ports`,
  `monitor`, `mcp`, `providers`, `commands`, `project`, `system`, and
  `diagnostics`.
- Make every capability explicit and discoverable through a `system.capabilities`
  response.
- Use sequence numbers and snapshots for critical state so the app can recover
  from dropped events.

### 3. Secure Pairing And Reattach

Lunel's V2 transport pairs app and CLI through a code/password flow, then
encrypts frames end-to-end and supports reconnect/reattach.

Apply to MOCCA:

- Pair once with QR/manual code.
- Store paired CLI sessions securely on Android.
- Use short-lived pairing codes and durable session credentials.
- Support reconnect after app background, Wi-Fi change, CLI restart, and relay
  failover.
- Treat the bridge as untrusted transport; encrypt content after handshake.

### 4. Native Project Capabilities Beat Shell Wrappers

Lunel implements filesystem, git, terminal, process, port, HTTP, monitor, and
OpenCode/Codex bridge capabilities in the CLI.

Apply to MOCCA:

- Replace app-side shell strings with typed CLI actions.
- Keep dangerous operations behind capability policy and app confirmations.
- Use direct OS/process APIs where possible.
- Use shell only as an internal CLI implementation detail, never as a mobile app
  protocol surface.

### 5. Real PTY Requires A Real PTY Model

Lunel's Rust PTY sidecar maintains a terminal grid and sends dirty row updates
at a capped frame rate.

Apply to MOCCA:

- Replace terminal text concatenation with a terminal grid model.
- Send dirty rows, cursor, title, modes, and scrollback metadata.
- Cap render/update frequency.
- Keep terminal rendering independent from chat message streaming.

## Lunel Parts Not To Copy Directly

1. Do not copy the monolithic CLI router style. Lunel's `cli/src/index.ts` is a
   large god file. MOCCA should use small capability modules from the start.
2. Do not make a public gateway mandatory. MOCCA should prefer direct local LAN
   pairing first, with relay as optional.
3. Do not auto-install global AI runtimes without clear user consent. The MOCCA
   CLI should detect, explain, and offer commands, but not surprise-mutate the
   machine.
4. Do not expose destructive git/file/process actions without policy. Mobile
   approval must be explicit and logged.
5. Do not rely on event buffer overflow for correctness. Critical state needs
   sequence numbers and resync.

## Current MOCCA Constraints And Hotspots

### Current Strengths

- `ConnectionManager` is already the single `HttpClient` owner and implements
  `ApiExecutor`.
- `StateCoordinator` and stores already centralize server events enough to be
  replaced by a bridge coordinator.
- SQLDelight `LocalCache` enables cache-first UI.
- Theme system already has app tokens, Material 3 shell, performance tiers, and
  `MaterialTheme.motionScheme` usage.
- Voyager/Koin structure is workable for phased migration.

### Current Weaknesses

1. **OpenCode server is the app contract.**
   `MoccaApiClient` exposes OpenCode REST, SSE, terminal, provider, config, and
   shell endpoints directly to repositories.

2. **Git operations are shell-string based.**
   `GitRepository` builds commands like `git add "$path"` and sends them through
   `/session/:id/shell`. This couples git to an active OpenCode session and
   increases quoting/security risk.

3. **System monitoring is shell-string based.**
   `SystemMonitorRepository` uses platform command strings through OpenCode
   shell. This is fragile and slow compared with a local CLI capability.

4. **Terminal output is a growing string.**
   `TerminalScreenModel` appends chunks to `TerminalTab.output` and trims to
   50,000 chars. It cannot represent terminal semantics robustly and causes
   avoidable allocation/recomposition under load.

5. **RealtimeSyncService does not implement its documented periodic loop.**
   The service documents 30-second periodic polling but currently only syncs on
   connection, foreground, active-session changes, manual triggers, and partial
   repo triggers.

6. **SSE correctness depends on in-memory flows.**
   `EventStreamRepository` uses bounded `MutableSharedFlow` with `DROP_OLDEST`
   for events. This is acceptable for non-critical telemetry but risky for
   message/session correctness unless paired with durable sequence resync.

7. **StateCoordinator has duplicate connection/sync entry points.**
   `onConnectionEstablished`, `start()`, connection observer, and active session
   switching can all trigger SSE connect/sync flows. It works as a patchwork but
   is not an ideal base for robust reconnect.

8. **Startup is config-file oriented, not pairing oriented.**
   `App()` starts `MainScreen` whenever an active server config exists. The new
   model needs paired CLI sessions, direct/relay transport health, and clear
   pairing recovery states.

## Target Architecture

```text
User terminal
  |
  | npx mocca-cli
  v
@mocca/cli
  |-- OpenCode runtime manager
  |-- Project sandbox and filesystem index
  |-- Git provider
  |-- Terminal PTY sidecar
  |-- Process/ports/monitor provider
  |-- MCP/tools/providers/config bridge
  |-- Secure transport server
  |-- Optional relay client
  v
Mocca Bridge Protocol v1
  | direct LAN WebSocket or optional relay
  | encrypted after pairing
  v
MOCCA Android app
  |-- BridgeConnectionManager
  |-- BridgeStateCoordinator
  |-- SQLDelight cache
  |-- ScreenModels / state stores
  |-- Material 3 Expressive UI
```

The app should render state and send commands. The CLI should own project-side
truth and adapt external systems such as OpenCode.

## New `mocca-cli` Design

### Package Layout

Create a new CLI workspace without adding Android/iOS/Desktop targets:

```text
cli/
  package.json
  tsconfig.json
  src/
    index.ts
    config/
      paths.ts
      project-config.ts
      paired-devices.ts
    protocol/
      schema.ts
      message.ts
      codecs.ts
      errors.ts
      capabilities.ts
    transport/
      direct-server.ts
      pairing.ts
      secure-session.ts
      reattach.ts
      heartbeat.ts
      backpressure.ts
    ai/
      opencode-runtime.ts
      opencode-provider.ts
      event-normalizer.ts
      message-normalizer.ts
      reconciliation.ts
    capabilities/
      fs.ts
      git.ts
      terminal.ts
      process.ts
      ports.ts
      monitor.ts
      mcp.ts
      providers.ts
      commands.ts
      project.ts
      diagnostics.ts
    pty/
      install.ts
      protocol.ts
      binaries.ts
    security/
      sandbox.ts
      policy.ts
      approvals.ts
      audit-log.ts
    observability/
      logger.ts
      metrics.ts
      trace.ts
    test-support/
      fake-app.ts
      fake-opencode.ts
```

### CLI Commands

Initial public command:

```bash
npx mocca-cli
```

Supported flags:

```bash
npx mocca-cli --new
npx mocca-cli --host 0.0.0.0 --port 17653
npx mocca-cli --project C:\path\to\repo
npx mocca-cli --no-opencode-start
npx mocca-cli --relay https://relay.example.com
npx mocca-cli --extra-port 3000 --extra-port 5173
npx mocca-cli --debug
npx mocca-cli doctor
npx mocca-cli pair
npx mocca-cli revoke
```

MVP behavior:

1. Resolve project root from `process.cwd()` unless `--project` is provided.
2. Start or attach to OpenCode on `127.0.0.1` with a random available port.
3. Start a local WebSocket bridge server.
4. Print QR and manual pairing code.
5. Keep running until interrupted.

### Runtime Management

The CLI should:

- Detect `opencode` and version.
- Start OpenCode server in-process or as a managed child if supported by the
  installed OpenCode API.
- Bind OpenCode to localhost only.
- Generate per-run credentials.
- Normalize OpenCode events into MOCCA protocol events.
- Reconcile sessions/messages after reconnect by fetching snapshots.
- Never require the Android app to call OpenCode directly.

### Capability Namespaces

#### `system`

Actions:

- `system.hello`
- `system.capabilities`
- `system.ping`
- `system.disconnect`
- `system.endSession`
- `system.version`
- `system.health`

Events:

- `system.ready`
- `system.peerConnected`
- `system.peerDisconnected`
- `system.cliReconnecting`
- `system.capabilitiesChanged`
- `system.error`

#### `ai`

Actions:

- `ai.backends`
- `ai.sessions.list`
- `ai.sessions.create`
- `ai.sessions.get`
- `ai.sessions.rename`
- `ai.sessions.delete`
- `ai.messages.list`
- `ai.prompt`
- `ai.abort`
- `ai.permissions.list`
- `ai.permissions.reply`
- `ai.questions.list`
- `ai.questions.reply`
- `ai.questions.reject`
- `ai.providers.list`
- `ai.providers.authMethods`
- `ai.providers.setAuth`
- `ai.agents.list`
- `ai.commands.list`
- `ai.commands.execute`
- `ai.revert`
- `ai.unrevert`

Events:

- `ai.session.created`
- `ai.session.updated`
- `ai.session.deleted`
- `ai.session.status`
- `ai.message.updated`
- `ai.message.part.updated`
- `ai.message.part.delta`
- `ai.permission.asked`
- `ai.permission.updated`
- `ai.question.asked`
- `ai.tool.started`
- `ai.tool.updated`
- `ai.tool.finished`
- `ai.error`

#### `fs`

Actions:

- `fs.list`
- `fs.stat`
- `fs.read`
- `fs.write`
- `fs.create`
- `fs.mkdir`
- `fs.move`
- `fs.delete`
- `fs.grep`
- `fs.find`
- `fs.watch.open`
- `fs.watch.close`

Events:

- `fs.changed`
- `fs.deleted`
- `fs.renamed`

Rules:

- All paths are relative to project root.
- Resolve and verify canonical path before every filesystem operation.
- Binary reads return metadata plus base64 only when explicitly requested.
- Large files require ranges/chunks.

#### `git`

Actions:

- `git.status`
- `git.diff`
- `git.branches`
- `git.log`
- `git.commitDetails`
- `git.stage`
- `git.unstage`
- `git.discard`
- `git.commit`
- `git.pull`
- `git.push`
- `git.fetch`
- `git.checkout`
- `git.stash`
- `git.tags`
- `git.remotes`

Events:

- `git.statusChanged`
- `git.branchChanged`
- `git.operationStarted`
- `git.operationFinished`

Rules:

- Use `spawn("git", args)` rather than shell strings.
- Destructive operations require `policy.confirmationRequired = true`.
- File paths are arrays of path args, never interpolated command strings.

#### `terminal`

Actions:

- `terminal.spawn`
- `terminal.write`
- `terminal.resize`
- `terminal.kill`
- `terminal.scroll`
- `terminal.snapshot`

Events:

- `terminal.spawned`
- `terminal.state`
- `terminal.exited`
- `terminal.error`

Rules:

- Use Rust PTY sidecar for real terminal semantics.
- Emit dirty row updates at a capped rate.
- Store scrollback in CLI memory with bounded limits.
- App renders grid state, not one giant output string.

#### `process`, `ports`, `monitor`

Actions:

- `process.list`
- `process.spawn`
- `process.kill`
- `process.output`
- `ports.list`
- `ports.track`
- `ports.untrack`
- `ports.tunnel`
- `monitor.system`
- `monitor.cpu`
- `monitor.memory`
- `monitor.disk`

Rules:

- Prefer OS APIs or non-shell process args.
- Keep process output buffers bounded.
- Port tunneling is optional and isolated from the critical AI chat path.

## Protocol Contract

### Message Shapes

```ts
type MoccaRequest = {
  v: 1
  id: string
  ns: string
  action: string
  payload?: unknown
}

type MoccaResponse = {
  v: 1
  id: string
  ns: string
  action: string
  ok: boolean
  payload?: unknown
  error?: MoccaError
}

type MoccaEvent = {
  v: 1
  ns: string
  event: string
  seq?: number
  payload?: unknown
}
```

### Critical-State Sequencing

Critical namespaces:

- `ai`
- `fs`
- `git`
- `terminal`

Each critical event should include:

- `seq`: monotonically increasing per namespace or stream.
- `snapshotVersion`: optional state version.
- `entityId`: session ID, terminal ID, file path, or git worktree ID.

The app should track last processed sequence per stream. On gap:

1. Stop applying partial events for that stream.
2. Request `*.snapshot`.
3. Replace local stream state atomically.
4. Resume event application.

### Backpressure Policy

Never use the same overflow behavior for every stream.

- Critical session/message/git/fs/terminal state: no silent drop. Use sequence
  gap detection and snapshot recovery.
- Telemetry/monitor/process sampling: drop old samples when UI is behind.
- Logs: bounded ring buffer.
- Terminal: coalesce dirty rows and cap frame rate.

## Transport And Pairing

### Direct Local Mode First

Default mode:

- CLI starts WebSocket server on local network.
- App discovers via QR/manual code. Optional LAN discovery can follow later.
- QR encodes host, port, pairing code, CLI public key, session ID, and protocol
  version.

### Optional Relay Mode Later

Relay mode should be optional:

- Useful across networks and CGNAT.
- Relay should never decrypt content.
- Relay should support app/CLI reattach generations.
- Relay should be self-hostable.

Do not make MOCCA depend on a public gateway to work on a local network.

### Security Requirements

- Pairing code TTL: short, e.g. 5-10 minutes.
- Paired session credentials: stored securely on Android and in OS config dir on
  the computer.
- Content encryption after handshake.
- Per-device revocation.
- Capability policies sent from CLI to app.
- Destructive operations require explicit app confirmation.
- Audit log in CLI for file/git/process/terminal actions.

## Android App Refactor

### New App Modules

Add a bridge layer parallel to current `api/`:

```text
composeApp/src/commonMain/kotlin/com/mocca/app/bridge/
  protocol/
    BridgeMessage.kt
    BridgeResponse.kt
    BridgeEvent.kt
    BridgeError.kt
    BridgeCapability.kt
  transport/
    BridgeTransport.kt
    DirectBridgeTransport.kt
    RelayBridgeTransport.kt
    SecureBridgeSession.kt
    BridgeReconnectPolicy.kt
  client/
    MoccaBridgeClient.kt
    BridgeRequestExecutor.kt
    BridgeEventStream.kt
```

Add data repositories that depend on the bridge client:

```text
composeApp/src/commonMain/kotlin/com/mocca/app/data/bridge/
  BridgeConnectionManager.kt
  BridgeStateCoordinator.kt
  BridgeSessionRepository.kt
  BridgeFileRepository.kt
  BridgeGitRepository.kt
  BridgeTerminalRepository.kt
  BridgeSystemMonitorRepository.kt
  BridgeProviderRepository.kt
  BridgeMcpRepository.kt
```

Keep legacy OpenCode classes during migration:

- `ConnectionManager`
- `MoccaApiClient`
- `MoccaSseClient`
- current repositories

Introduce a runtime mode:

```kotlin
enum class BackendMode {
    LegacyOpenCodeServer,
    MoccaCliBridge
}
```

The app should boot into bridge mode for new pairings but allow legacy mode
until parity is verified.

### App Startup

Replace "active server config exists" with "valid connection target exists":

Current:

- `App()` checks `ServerConfigRepository.activeServer`.
- Non-empty host starts `MainScreen`.

Target:

- `App()` checks `ConnectionTargetRepository`.
- Target types:
  - `PairedCliDirect`
  - `PairedCliRelay`
  - `LegacyOpenCodeServer`
- If target exists, start `MainScreen`.
- If no target, start pairing onboarding.
- If target exists but unreachable, show reconnect/pairing recovery state.

### Pairing UI

New onboarding flow:

1. "Run npx mocca-cli" instruction.
2. QR scan or manual code.
3. Secure handshake.
4. Capability readout.
5. Project confirmation.
6. Main screen.

Screens:

- `ProgressiveOnboardingScreen` should be reworked around pairing.
- Server host/port/password form should move under "Advanced legacy server".
- Add paired devices/session management in settings.

### Data And Cache

Add SQLDelight tables:

```text
paired_cli_session
bridge_connection_target
bridge_capability_snapshot
bridge_event_cursor
project_snapshot
terminal_snapshot
git_snapshot
```

Existing sessions/messages tables can stay, but the source of writes changes:

- Legacy mode writes from OpenCode REST/SSE.
- Bridge mode writes from `ai.*` snapshots/events.

### Coordinator Refactor

Current:

- `StateCoordinator` consumes `EventStreamRepository`.
- `RealtimeSyncService` polls secondary data.

Target:

- `BridgeStateCoordinator` consumes `BridgeEventStream`.
- Critical state flows through typed reducers:
  - `AiReducer`
  - `GitReducer`
  - `FsReducer`
  - `TerminalReducer`
  - `SystemReducer`
- Reducers write to `LocalCache` in batched transactions.
- Reconnect triggers snapshot reconciliation, not multiple ad-hoc refreshes.

### Repository Migration Mapping

| Current Feature | Current Source | Target Source |
| --- | --- | --- |
| Sessions | OpenCode `/session` + SSE | CLI `ai.sessions.*` + sequenced events |
| Messages | OpenCode `/session/:id/message` + SSE | CLI `ai.messages.*` + deltas/snapshots |
| Permissions | OpenCode `permission/*` | CLI `ai.permissions.*` |
| Questions | OpenCode `question/*` | CLI `ai.questions.*` |
| Providers | OpenCode config/provider endpoints | CLI `ai.providers.*` |
| Agents | OpenCode config | CLI `ai.agents.*` |
| Commands | OpenCode commands | CLI `ai.commands.*` |
| Git | OpenCode `/vcs` + shell | CLI `git.*` with typed args |
| Files | OpenCode file/search endpoints | CLI `fs.*` sandboxed project API |
| Terminal | OpenCode terminal websocket | CLI Rust PTY sidecar |
| System monitor | shell through OpenCode | CLI `monitor.*` direct/local |
| Ports | shell/OpenCode-side checks | CLI `ports.*` direct/local |
| MCP/tools | OpenCode endpoints/polling | CLI `mcp.*` and `ai.tools.*` |

## UI/UX Direction

### Product Shape

The first viewport should be the tool, not marketing:

- Chat is primary.
- Connection state is visible but quiet.
- Secondary project features are available through clear panels/sheets:
  - Sessions
  - Files
  - Git
  - Terminal
  - Tools/MCP
  - System

### Material 3 Expressive Rules

Keep MOCCA's existing project rules:

- Feature UI uses `AppColors`, `AppTypography`, `AppShapes`, and `AppSpacing`.
- `MaterialTheme.colorScheme` and `MaterialTheme.shapes` stay in theme bridge
  code only.
- Motion uses `MaterialTheme.motionScheme`.
- Prefer tonal elevation for depth.
- Do not use `RectangleShape` for interactive elements.

### Simplification

Current main screen is feature-rich but cognitively dense. Target:

- Center: chat.
- Top: project/session title, compact connection indicator, search.
- Bottom: input and 4-5 icon actions.
- Panels: context/session/files/git/terminal as focused tasks, not permanent
  dashboards.
- Settings: paired CLI sessions, legacy server mode, performance mode, provider
  auth, diagnostics.

### Performance-Oriented UI Rules

- Chat `LazyColumn` must use stable keys and `contentType`.
- Avoid sorting/filtering large lists in composables.
- Search result derivation should move to state holders or use `remember` and
  `derivedStateOf`.
- Terminal should render a grid/snapshot model, not one large `String`.
- Streaming message deltas should be coalesced before UI emission.
- High-frequency scroll/read states should be read in lower scopes or layout /
  draw phases where possible.

## Performance Plan

### Transport

- Use one long-lived encrypted WebSocket for control/events.
- Use binary frames for large payloads and terminal deltas when useful.
- Compress only large text/diff/file responses; avoid compressing tiny events.
- Heartbeat interval should adapt to app foreground/background.
- Reconnect with exponential backoff and jitter.
- Reattach should keep session identity stable.

### App State

- Replace full refresh after common events with event reducers.
- Snapshot only on first connect, reconnect gaps, or explicit refresh.
- Batch database writes for streaming message parts.
- Use `StateFlow` with immutable state objects and stable sub-state.
- Do not broadcast one global event stream to everything for every update.

### CLI State

- Maintain project-side caches:
  - session metadata
  - recent messages
  - git status
  - filesystem stat/index
  - terminal grids
  - process output rings
- Emit diffs where possible.
- Use worker queues for slow operations.
- Keep OpenCode event reconciliation independent from WebSocket relay.

### Terminal

- Implement Rust PTY sidecar after MVP AI parity.
- Cap terminal frame rate around 24-30 fps.
- Coalesce dirty rows.
- Bound scrollback.
- App renders visible rows with stable keys.

### Git

- Replace shell commands with non-shell `git` process args.
- Parse porcelain v1/v2 and status data in CLI.
- Cache status and emit only changes.
- Use operation queue so `stage`, `commit`, `pull`, and `push` cannot corrupt
  each other.

## Reliability Plan

### Reconnect Cases To Support

- App background -> foreground.
- Wi-Fi changes.
- CLI process restarts.
- OpenCode server inside CLI restarts.
- Phone loses network temporarily.
- Relay unavailable.
- App misses critical event sequence.

### Recovery Behavior

- App never assumes connection is permanently dead until backoff deadline.
- CLI keeps paired session alive during app offline grace.
- On reconnect, app sends last known cursors.
- CLI replies with either "resume from seq" or "snapshot required".
- UI shows stale cached state with connection status, not blank screens.

### Observability

Add diagnostics surfaces:

- Transport state.
- Last heartbeat.
- Last event seq per namespace.
- Snapshot counts.
- OpenCode runtime state.
- CLI version and app version.
- Recent errors.

## Security Plan

### Filesystem

- Project root sandbox is mandatory.
- Resolve symlinks/canonical paths.
- New paths use lexical checks plus parent canonical checks.
- Reject path traversal.
- Large/binary file reads require explicit flags.

### Git/File Destructive Actions

Require confirmation for:

- `git.discard`
- `git.checkout --force`
- `git.reset`
- file delete
- directory delete
- process kill
- terminal kill when process is active

### Secrets

- Android uses `SecureTokenStorage` for paired credentials.
- CLI stores secrets under OS config dir.
- Pairing secrets are never printed after pairing.
- Logs redact tokens, auth headers, and provider keys.

## Migration Phases

### Phase 0: Spec And Test Harness

- [ ] Add this plan to the repo.
- [ ] Create protocol schemas under `cli/src/protocol/`.
- [ ] Create matching Kotlin protocol models under `composeApp/.../bridge/protocol/`.
- [ ] Add JSON fixture tests shared conceptually between TS and Kotlin.
- [ ] Add a fake bridge server for Android repository tests.
- [ ] Add a fake app client for CLI tests.

Verification:

```bash
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
npm test --workspace cli
```

### Phase 1: CLI MVP Pairing And AI Bridge

- [ ] Add `cli/package.json` with `bin` entry for `mocca-cli`.
- [ ] Implement `mocca-cli` startup, project root resolution, config dir, and
      QR/manual pairing.
- [ ] Implement direct local WebSocket transport.
- [ ] Implement secure session handshake.
- [ ] Implement `system.capabilities`.
- [ ] Implement OpenCode runtime startup/attach.
- [ ] Implement AI actions for sessions, messages, prompt, abort, permissions,
      questions, providers, agents, and commands.
- [ ] Implement AI event normalization and snapshot reconciliation.

MVP exit criteria:

- `npx mocca-cli` starts in a project and prints a pairing code.
- A test client can pair, list sessions, create a session, send a prompt, and
  receive streaming message events.
- CLI survives OpenCode event reconnect and can resync sessions/messages.

### Phase 2: Android Bridge Connection

- [ ] Add `BridgeConnectionManager`.
- [ ] Add `MoccaBridgeClient`.
- [ ] Add `BridgeEventStream`.
- [ ] Add secure credential storage for paired CLI sessions.
- [ ] Add pairing onboarding.
- [ ] Add `BackendMode` and keep legacy OpenCode mode.
- [ ] Wire app startup to `ConnectionTargetRepository`.

Exit criteria:

- App can pair to local CLI.
- App can reconnect after background/foreground.
- App can show CLI capabilities.
- Legacy OpenCode server setup still works.

### Phase 3: Chat And Session Migration

- [ ] Add `BridgeSessionRepository`.
- [ ] Add `BridgeStateCoordinator` with AI reducers.
- [ ] Feed existing `ChatStateStore` from bridge-backed repository.
- [ ] Replace OpenCode direct message refresh with AI snapshots/events in bridge
      mode.
- [ ] Implement event cursor persistence.
- [ ] Keep `SessionRepository` legacy path until parity is proven.

Exit criteria:

- Chat works end-to-end through CLI.
- Permissions and questions work.
- Reconnect does not duplicate or lose messages.
- App does not full-refresh history on every common event.

### Phase 4: Files, Git, Providers, MCP, Commands

- [ ] Implement CLI `fs.*` sandboxed capability.
- [ ] Implement CLI `git.*` with typed non-shell args.
- [ ] Implement CLI `mcp.*`, `providers.*`, and `commands.*` bridge actions.
- [ ] Add bridge repositories in Android.
- [ ] Migrate Git screen to bridge mode.
- [ ] Migrate Files screen to bridge mode.
- [ ] Migrate Settings/provider auth surfaces to bridge mode.

Exit criteria:

- File browsing/read/write/search works without OpenCode file endpoints.
- Git status/stage/unstage/commit/pull/push works without OpenCode shell.
- Destructive operations require confirmation.
- Provider/MCP/tool metadata works from CLI.

### Phase 5: Terminal, Process, Ports, Monitor

- [ ] Add Rust PTY sidecar workspace under `cli/pty/` or `pty/`.
- [ ] Implement terminal JSON protocol compatible with CLI transport.
- [ ] Implement `terminal.*` bridge namespace.
- [ ] Replace `TerminalScreenModel` output string with terminal grid state.
- [ ] Implement `process.*`, `ports.*`, and `monitor.*`.
- [ ] Add bounded output buffers and sampling policies.

Exit criteria:

- Terminal renders correctly under high output.
- Resize, scroll, input, exit, and reconnect work.
- System monitor and process/port screens no longer require an OpenCode session.

### Phase 6: Relay And Remote Robustness

- [ ] Design optional `mocca-relay` manager/gateway.
- [ ] Implement relay assignment and validation.
- [ ] Implement app/CLI reattach generations.
- [ ] Add offline grace windows.
- [ ] Keep direct local mode as default.

Exit criteria:

- Same app protocol works over direct and relay transport.
- Relay cannot decrypt content.
- App and CLI recover from relay reconnect.

### Phase 7: Material 3 Expressive UX Simplification

- [ ] Rework onboarding around pairing.
- [ ] Simplify main screen hierarchy around chat-first workflow.
- [ ] Move dense dashboards into task-focused panels/sheets.
- [ ] Audit feature UI for raw `MaterialTheme.colorScheme` and shape mixing.
- [ ] Audit lists for stable keys and `contentType`.
- [ ] Replace terminal text view with grid renderer.
- [ ] Add connection diagnostics sheet.

Exit criteria:

- First-run path is clear.
- Main screen has one obvious primary action: talk to OpenCode.
- Secondary features are reachable without clutter.
- UI remains smooth during streaming and terminal output.

### Phase 8: Verification And Release

- [ ] Add CLI unit tests for protocol, path sandbox, git args, pairing, and
      OpenCode normalization.
- [ ] Add Android unit tests for bridge reducers and repositories.
- [ ] Add integration test with fake CLI bridge.
- [ ] Add emulator Maestro smoke: pair, create session, prompt, permission,
      git status, file read, terminal spawn.
- [ ] Add network chaos tests: app background, CLI restart, network drop.
- [ ] Add performance tests for long chat, high terminal output, and large file
      search.
- [ ] Add release docs for `npx mocca-cli`.

Exit criteria:

- Debug APK builds.
- CLI package runs from clean checkout.
- Maestro smoke passes on emulator.
- No critical state loss under reconnect tests.

## First Implementation Slice Recommendation

Start with the smallest slice that proves the new architecture:

1. CLI direct transport + pairing + `system.capabilities`.
2. CLI OpenCode startup + `ai.sessions.list` + `ai.prompt`.
3. Android bridge connection manager + pairing screen.
4. Chat screen in bridge mode for one active session.

Do not start with terminal or relay. Those are important, but they will slow
down the architecture proof. The first milestone should prove that MOCCA can
stop depending on OpenCode HTTP/SSE directly for chat.

## Major Risks

1. **Protocol drift between TS and Kotlin.**
   Mitigation: keep schemas explicit, add fixtures, and generate models later
   if drift appears.

2. **OpenCode API instability.**
   Mitigation: isolate it inside CLI `ai/opencode-*` modules and normalize into
   MOCCA-owned events.

3. **Android lifecycle and long-lived sockets.**
   Mitigation: app foreground gets rich live transport; background transport is
   conservative and reconnects/resyncs on foreground.

4. **Terminal complexity.**
   Mitigation: delay PTY until AI/files/git bridge is stable, then use a sidecar
   grid model instead of app-side text concatenation.

5. **Security of local machine control from phone.**
   Mitigation: sandbox paths, typed args, confirmation policies, audit logs, and
   per-device revocation.

## Definition Of Done For The Refactor

MOCCA is considered refactored when:

- `npx mocca-cli` is the recommended connection path.
- The app can pair, reconnect, and resume without manual host/port/password
  setup.
- Chat/session/message/permission/question flows use the CLI bridge by default.
- Files, git, terminal, system monitor, process, ports, providers, MCP, and
  commands no longer require app-side OpenCode endpoint knowledge.
- Legacy OpenCode server mode is optional or removed after parity.
- Critical state streams have sequence/snapshot recovery.
- Terminal uses grid/delta rendering.
- Git/file/process destructive actions require confirmation.
- Emulator smoke tests cover pairing, chat, git, files, terminal, and reconnect.
- UI follows MOCCA Material 3 Expressive token rules and remains smooth during
  streaming.
