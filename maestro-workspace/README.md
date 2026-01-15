# MOCCA Maestro UI Testing

Maestro-based mobile UI testing framework for the MOCCA Android app.

## 📁 Directory Structure

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
│   └── regression.yaml # Full coverage (6 tests)
└── config/             # Environment configurations
```

## 🚀 Quick Start

### Prerequisites
- [Maestro CLI](https://maestro.mobile.dev/getting-started/installing-maestro) installed
- Android emulator running or device connected
- MOCCA app installed: `adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Run Tests

> [!IMPORTANT]
> To ensure tests run on an emulator even when physical devices are connected, use the provided helper script:

```bash
# Run smoke tests on emulator
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml

# Run full regression on emulator
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/regression.yaml
```

Alternatively, manually specify the device:
```bash
# Find emulator serial (e.g., emulator-5554)
adb devices

# Run with --device flag
maestro --device emulator-5554 test maestro-workspace/testplans/smoke.yaml
```

## 🏷️ Flow Tags

| Tag | Description |
|-----|-------------|
| `@smoke` | Quick validation tests |
| `@critical` | Must-pass tests |
| `@navigation` | Screen navigation tests |
| `@sessions` | Session management tests |

## 📝 Adding New Tests

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

## 📸 Screenshots

Screenshots are saved to `maestro-workspace/.maestro/screenshots/` after test runs.

## 🔄 CI/CD Integration

Maestro tests run automatically on GitHub Actions for every push to `main` and PRs.

The workflow (`.github/workflows/maestro-tests.yml`):
1. Builds debug APK
2. Starts Android emulator (`reactivecircus/android-emulator-runner`)
3. Installs Maestro CLI
4. Runs smoke tests
5. Uploads screenshots as artifacts

> [!NOTE]
> Tests run on emulator only (no Maestro Cloud required).
