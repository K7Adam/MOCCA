---
name: android-mcp
description: Run emulator-first Android device automation for MOCCA using MCP/ADB workflows with reproducible diagnostics.
license: MIT
compatibility: opencode
metadata:
  platform: android
  focus: automation
---

## What this skill does

- Executes Android automation loops on emulator with deterministic steps.
- Uses ADB and emulator-safe workflows for install, smoke checks, and log capture.
- Supports rapid validation of app behavior after code changes.

## When to use

Use for:

- UI regression checks on emulator
- Connection/setup validation after environment or networking changes
- Capturing runtime evidence (screenshots/logcat) for failures

## Operational rules

1. Emulator-first only; do not use physical devices.
2. Local runs MUST use visible emulator startup script first: `./maestro-workspace/start-emulator.ps1`.
3. Verify ADB target state before actions (`adb devices`).
4. Prefer scripted/repeatable flows over ad-hoc manual tapping.
5. Capture filtered logs for failures (`mocca|Exception`).
6. Report exact command evidence for each validation run.

## Core commands

- Start emulator (visible local): `./maestro-workspace/start-emulator.ps1`
- Build: `./gradlew.bat :androidApp:assembleDebug`
- Install: `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- E2E smoke: `./maestro-workspace/run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml`
- Logs: `adb logcat -c && adb logcat *:W | findstr "mocca|Exception"`

## Local vs CI

- Local agent workflow: visible emulator via `start-emulator.ps1`, then `run-emulator-tests.ps1`.
- CI workflow: headless emulator handled by `.github/workflows/maestro-tests.yml`.

## Done criteria

- Emulator run reproduces expected behavior or captures failure evidence.
- Commands and outputs are included in validation summary.
