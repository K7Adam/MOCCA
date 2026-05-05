# MAESTRO E2E TESTING

**Updated:** 2026-05-05
**Project:** MOCCA Maestro Workspace
**Target:** Android Emulator (`10.0.2.2` host access)
**Format:** Maestro YAML flows + PowerShell runners

**Relevant Skills:** `android-mcp`

## OVERVIEW
Emulator-only UI verification layer for MOCCA. Covers smoke, regression, and screenshot-catalog flows; unit tests do not live here.

## STRUCTURE
```
maestro-workspace/
├── flows/
│   ├── onboarding/     # Launch / first-run checks
│   ├── navigation/     # Reachability of dashboard-driven screens
│   ├── sessions/       # Session creation / context flows
│   ├── catalog/        # Screenshot capture flows
│   └── debug-connection.yaml
├── subflows/common/    # Shared launch + wait primitives
├── testplans/
│   ├── smoke.yaml
│   ├── regression.yaml
│   └── screenshot-catalog.yaml
├── config/
└── *.ps1               # Emulator, runner, screenshot automation
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Smoke path | `testplans/smoke.yaml` | App launch + settings + session create |
| Regression path | `testplans/regression.yaml` | Adds Git + Terminal reachability |
| Screenshot catalog | `testplans/screenshot-catalog.yaml` | Visual artifact generation |
| Shared launch logic | `subflows/common/launch_app.yaml` | Standard app boot sequence |
| Stable wait | `subflows/common/wait_for_main_screen.yaml` | Main-screen landmark sync |
| Files navigation | `flows/navigation/navigate_to_files.yaml` | Files screen reachability |
| Terminal navigation | `flows/navigation/navigate_to_terminal.yaml` | Terminal screen reachability |
| Runner script | `run-emulator-tests.ps1` | Install, clear state, logcat capture |
| Emulator startup | `start-emulator.ps1` | Visible by default |
| Screenshot script | `capture-screenshot-catalog.ps1` | Catalog batch capture |

## RUNNING TESTS
```powershell
# Start visible emulator
.\maestro-workspace\start-emulator.ps1

# Run smoke plan on running emulator
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml

# Build first, then run
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml -BuildApk

# Generate screenshot catalog assets
.\maestro-workspace\capture-screenshot-catalog.ps1
```

## CONVENTIONS
- Flows are `snake_case.yaml`, grouped by feature area
- Navigation flows use `navigate_to_<screen>.yaml`
- Catalog flows use `capture_<surface>.yaml`
- Shared setup belongs in `subflows/common/`, not duplicated per flow
- CI uses headless emulator; local agent runs use visible emulator first

## TAGS / PLANS
- `@smoke` -> quick validation
- `@critical` -> must-pass app launch
- `@navigation` -> screen reachability
- `@sessions` -> session management
- `@catalog` -> screenshot capture only

## ANTI-PATTERNS
- Do not use physical devices for host-connectivity tests
- Do not put unit/integration logic tests here; use `composeApp/src/commonTest`
- Do not rely on relative paths outside the workspace in scripts
- Do not add orphaned catalog flows without wiring them into `screenshot-catalog.yaml`

## NOTES
- `flows/debug-connection.yaml` is a debug helper, not a normal plan member
- `flows/catalog/capture_workspace_explorer.yaml` exists but is not currently wired into `testplans/screenshot-catalog.yaml`
- `flows/sessions/create_new_session.yaml` exercises session creation from the onboarding flow
