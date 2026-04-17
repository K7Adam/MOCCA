---
name: mocca-android-agent-workflow
description: Use when developing MOCCA with Android CLI, Android Knowledge Base, Android skills, Gradle, Compose Multiplatform, Maestro, emulator automation, or AI coding agents such as Codex, OpenCode, Gemini CLI, or Windsurf.
license: MIT
compatibility: Windows 11 MOCCA workspace. Android CLI 0.7+ is optional but preferred.
metadata:
  project: MOCCA
  android-cli: "0.7+"
---

# MOCCA Android Agent Workflow

## Purpose

This skill makes the new Android CLI, Android skills, and Android Knowledge Base usable from any agent working on MOCCA without copying the same instructions into every tool-specific config.

## Source Of Truth

- Repo root: `C:\Users\ruzaq\AndroidStudioProjects\MOCCA`
- Always read `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\AGENTS.md` first.
- Also read the nearest nested `AGENTS.md` before changing files under `androidApp`, `composeApp`, or `maestro-workspace`.
- Keep Android CLI and official Android skills external and updateable. Do not vendor Google-provided Android skills into this repo unless the user explicitly asks for an offline copy.

## Bootstrap Or Check Tooling

Run this from the repo root before substantial Android work:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\scripts\setup-android-agent-tooling.ps1" -CheckOnly
```

When Android CLI is installed and available on `PATH`, run the full bootstrap:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\scripts\setup-android-agent-tooling.ps1"
```

The full bootstrap updates Android CLI, runs `android init`, installs all official Android skills for detected agents, describes the MOCCA project, and performs a Knowledge Base search smoke check.

Use the repo-local wrapper when `android` is not visible in the current terminal:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\scripts\android-cli.ps1" --version
```

## Android CLI Usage Policy

Use native Android CLI commands when available. If `android` is missing from `PATH`, replace `android` with:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\scripts\android-cli.ps1"
```

```powershell
android update
android init
android describe --project_dir="C:\Users\ruzaq\AndroidStudioProjects\MOCCA"
android skills list --long
android skills add --all
android docs search "Jetpack Compose state management"
android docs fetch kb://android/topic/performance/overview
```

Known Windows 11 limitation in this MOCCA workspace: `android describe` can fail because Android CLI invokes extensionless `gradlew` instead of `gradlew.bat`. Treat that as an Android CLI Windows issue, not as a MOCCA Gradle failure. Use the repo-native Gradle commands below for build verification.

Use `android docs search` and then `android docs fetch` before changing modern Android APIs, Gradle/AGP behavior, Compose, Navigation, edge-to-edge, R8, Play Billing, emulator/device automation, or performance guidance. Prefer fetched Android Knowledge Base content over model memory.

Use `android skills find <query>` before major migrations, then install the matching official skill if it is not already installed. Official skills that may become relevant to MOCCA include edge-to-edge, Navigation 3, AGP 9 upgrade, XML-to-Compose migration, R8 analyzer, and Play Billing upgrade.

## Windows 11 Caveat

Android CLI 0.7 documents `android emulator` as disabled on Windows. For MOCCA local UI QA, use the existing emulator and Maestro scripts:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\start-emulator.ps1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\run-emulator-tests.ps1" "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\testplans\smoke.yaml"
```

After the app is running on an emulator, Android CLI can still be useful for device-facing inspection:

```powershell
android layout --pretty --output="C:\Users\ruzaq\AndroidStudioProjects\MOCCA\.agents\cache\android-layout.json"
android screen capture --output="C:\Users\ruzaq\AndroidStudioProjects\MOCCA\.agents\cache\android-screen.png" --annotate
```

## MOCCA Build And Verification

Use these repo-native commands for implementation verification:

```powershell
powershell.exe -NoProfile -Command "& 'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\gradlew.bat' :androidApp:assembleDebug"
powershell.exe -NoProfile -Command "& 'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\gradlew.bat' :composeApp:allTests"
```

Only run emulator/Maestro checks when the task affects user-facing flows or Android runtime behavior.

## Agent-Specific Notes

- Codex App and Codex CLI: follow `AGENTS.md`; use this skill when the task mentions Android CLI, Android skills, Knowledge Base grounding, Gradle, Compose, emulator, or Maestro.
- OpenCode: this skill is discoverable from `.agents/skills/`; existing `.opencode/skills/` remain available for project-specific Kotlin, UI, theme, and emulator workflows.
- Gemini CLI: `GEMINI.md` imports `AGENTS.md` and this skill.
- Windsurf: root `AGENTS.md` is discovered automatically; `.windsurf/rules/mocca-agent-tooling.md` points Cascade to this skill.

## Anti-Patterns

- Do not run `android create` inside the MOCCA repository. MOCCA is an existing KMP Android-only project, not a new Android CLI scaffold.
- Do not replace existing Gradle, emulator, or Maestro scripts with Android CLI wrappers until Android CLI supports the needed Windows workflow.
- Do not copy full official Android skill content into multiple tool-specific directories. Install or update official skills through Android CLI instead.
