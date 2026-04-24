# MOCCA Maestro UI Testing

Maestro-based mobile UI testing framework for the MOCCA Android app.

## Directory Structure

```
maestro-workspace/
├── flows/              # Individual test scenarios
│   ├── onboarding/     # App launch & first-run tests
│   ├── sessions/       # Session management tests
│   └── navigation/     # Screen navigation tests
├── subflows/           # Reusable components
│   └── common/         # Shared actions (launch, wait)
├── testplans/          # Test suite groupings
│   ├── smoke.yaml      # Quick PR validation (3 tests)
│   └── regression.yaml # Full coverage (5 tests)
└── config/             # Environment configurations
```

## Quick Start

### Prerequisites
- [Maestro CLI](https://maestro.mobile.dev/getting-started/installing-maestro) installed
- Android emulator AVD available (default: `Pixel_9_Pro_XL`)
- Debug APK built at `androidApp/build/outputs/apk/debug/androidApp-debug.apk` (or use `-BuildApk`)

### Local Agent Workflow (VISIBLE emulator)

```powershell
# Start visible emulator window for local runs
.\maestro-workspace\start-emulator.ps1

# Execute smoke plan against running emulator
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml
```

`start-emulator.ps1` launches a visible emulator by default and waits until `sys.boot_completed=1` so subsequent test commands are deterministic.

### CI Workflow (HEADLESS emulator)

CI uses `.github/workflows/maestro-tests.yml` with `reactivecircus/android-emulator-runner` and headless flags (`-no-window -no-snapshot`).

To regenerate the entire screenshot catalog via CI:
1. Navigate to **Actions** → **Screenshot Catalog** in GitHub.
2. Click **Run workflow**.
3. Download the generated artifact from the workflow run.

### Run Tests

> [!IMPORTANT]
> To ensure tests run on an emulator even when physical devices are connected, use the provided helper script:

```bash
# Run smoke tests on emulator
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml

# Run full regression on emulator
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/regression.yaml

# Build APK first, then run smoke tests
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml -BuildApk
```

The helper script now performs deterministic preflight checks (emulator boot readiness), installs APK (unless `-SkipInstall`), clears app data (unless `-SkipClearState`), and captures logcat artifacts per run.

Alternatively, manually specify the device:
```bash
# Find emulator serial (e.g., emulator-5554)
adb devices

# Run with --device flag
maestro --device emulator-5554 test maestro-workspace/testplans/smoke.yaml
```

## Flow Tags

| Tag | Description |
|-----|-------------|
| `@smoke` | Quick validation tests |
| `@critical` | Must-pass tests |
| `@navigation` | Screen navigation tests |
| `@sessions` | Session management tests |

## Adding New Tests

1. Create a new `.yaml` file in the appropriate `flows/` subdirectory
2. Add tags for categorization
3. Include the flow in relevant test plans

**Flow template:**
```yaml
appId: com.mocca.app
tags:
  - smoke
---
- launchApp:
    clearState: true
- extendedWaitUntil:
    visible: "Expected Text"
    timeout: 10000
- takeScreenshot: test_name
```

## Screenshots

Screenshots are saved to `maestro-workspace/.maestro/screenshots/` after test runs.

## CI/CD Integration

Maestro tests run automatically on GitHub Actions for every push to `main` and PRs.

The workflow (`.github/workflows/maestro-tests.yml`):
1. Builds debug APK
2. Starts Android emulator (`reactivecircus/android-emulator-runner`)
3. Installs Maestro CLI
4. Runs smoke tests
5. Captures logcat
6. Uploads `.maestro/` artifacts

> [!NOTE]
> Tests run on emulator only (no Maestro Cloud required).
