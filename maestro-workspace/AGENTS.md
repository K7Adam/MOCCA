# MAESTRO E2E TESTING

**Project:** MOCCA Maestro Workspace
**Target:** Android Emulator (10.0.2.2)
**Format:** Maestro YAML Flows

## OVERVIEW
This workspace contains the End-to-End (E2E) UI testing suite for MOCCA. It uses Maestro to simulate user interactions on an Android emulator. These tests are strictly external and do not contain any internal unit or integration tests.

## STRUCTURE
```
maestro-workspace/
├── flows/              # Individual test scenarios
│   ├── onboarding/     # App launch & first-run tests
│   ├── sessions/       # Session management tests
│   └── navigation/     # Screen navigation tests
├── subflows/           # Reusable components (e.g., launch, wait)
├── testplans/          # Test suite groupings
│   ├── smoke.yaml      # Critical path validation
│   └── regression.yaml # Full feature coverage
└── config/             # Environment configurations
```

## RUNNING TESTS
Tests MUST be executed on an Android Emulator to ensure the app can reach the host via `10.0.2.2`.

### Recommended Execution
Use the helper script to automatically target the first available emulator:
```powershell
.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml
```

### Manual Execution
```bash
# Find emulator serial
adb devices

# Run specific test
maestro --device <serial> test maestro-workspace/flows/onboarding/start.yaml
```

## CONVENTIONS
- **YAML Flows**: All tests must be defined in `.yaml` files using Maestro syntax.
- **No Unit Tests**: Do not add unit tests here. Use `composeApp/src/commonTest` for logic tests.
- **Emulator Only**: Do not use physical devices for tests requiring host connectivity.
- **Absolute Paths**: When referencing files outside the workspace in scripts, always use absolute paths.
- **Tags**: Use tags (`@smoke`, `@critical`) in YAML headers to categorize flows for test plans.
