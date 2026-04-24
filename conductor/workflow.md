# Project Workflow

## Working Principles

1. Read `AGENTS.md` and the nearest nested `AGENTS.md` before editing.
2. Start with analysis and source checks; do not guess around Android, Gradle, Compose, or runtime contracts.
3. Prefer small, reversible changes with tests or compile/build verification.
4. Keep docs current when code boundaries move.
5. Remove obsolete paths instead of preserving dead compatibility layers.

## Standard Development Flow

1. **Map the scope.** Use `rg`/`rg --files`, read the owning files, and identify nested instructions.
2. **Write or identify a guard.** For bug fixes and behavior changes, add a failing test first. For pure deletions, create a compile or static guard when practical.
3. **Implement narrowly.** Follow existing architecture: ScreenModel -> StateFlow -> UI, repositories over `LocalCache`, `ApiExecutor` for legacy HTTP, bridge repositories for CLI-backed behavior.
4. **Update docs.** Refresh README, AGENTS, and active Conductor docs when architecture or commands change.
5. **Verify freshly.** Run the smallest useful command first, then the broader command that proves the claim.

## MOCCA Commands

```powershell
# Shared host tests
.\gradlew.bat :composeApp:allTests

# Debug APK
.\gradlew.bat :androidApp:assembleDebug

# Visible emulator
.\maestro-workspace\start-emulator.ps1

# Smoke plan on running emulator
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml
```

Run emulator/Maestro checks for user-facing flows, Android runtime behavior, onboarding, settings, or visual regressions. Code-only repository/cache cleanup usually needs `:composeApp:allTests` and `:androidApp:assembleDebug`.

## Documentation Rules

- Root `README.md` is the product-facing entrypoint.
- Root `AGENTS.md` and nested `AGENTS.md` files are agent-facing source of truth.
- `conductor/*.md` documents active product/workflow context.
- `conductor/archive/**` is historical. Update it only when the task explicitly asks to rewrite archival records.
- Do not leave generic template instructions that reference non-MOCCA stacks, physical-device-only validation, or web deployment flows.

## Quality Gates

- Code compiles for Android.
- New behavior has a regression test where practical.
- SQLDelight schema changes include migrations.
- Cache ownership is explicit and documented.
- No direct `HttpClient` ownership outside approved boundaries.
- No new iOS/Desktop targets.
- No relative paths in scripts or agent docs.

## Commit Guidance

Use conventional commits when committing:

```text
refactor(data): remove legacy model recents cache
docs(readme): refresh bridge-first architecture notes
test(domain): guard project-scoped ai recents
```

Do not commit generated build artifacts, screenshots, or local environment files unless the task explicitly requires them.
