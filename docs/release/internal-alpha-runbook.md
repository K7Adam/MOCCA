# MOCCA Internal Alpha Runbook

This runbook is for MOCCA team members and trusted testers running the internal alpha build. It walks through what MOCCA is, what you need on your machine, how to install and pair it, the limitations you should know about up front, and how to report what you find.

If you only have ten minutes, read the "Prerequisites", "Quick Start", and "Known Issues" sections, then come back for the rest.

## What MOCCA Is Right Now

MOCCA is an Android-only companion app for the OpenCode AI agent. It is a Kotlin Multiplatform project with a Compose Multiplatform UI, Material 3 Expressive theming, and a dark-first visual system. The app connects to OpenCode through a CLI bridge (the preferred path) and falls back to direct OpenCode HTTP/SSE if the bridge is not available.

The current build is an internal alpha. It is debug-signed. It is distributed through GitHub APK releases, not the Play Store. It is not feature complete and it is not production-signed yet.

## Prerequisites

You need all of the following on the machine that will build or run MOCCA:

- **JDK 17 or newer.** Anything in the 17+ line works. AGP 9 in this project requires modern JDK.
- **Android SDK with API 36 installed.** The build targets API 36. Older platforms compile but are not the test target.
- **Android command-line tools or Android Studio.** Either is fine. The repo's Gradle build handles the rest.
- **An Android emulator or a physical device running API 31 or newer.** Emulator is the standard target. See "Why Emulator Only" below.
- **OpenCode CLI** on your `PATH`. The legacy HTTP path is still in the code, but the bridge path is what you should test.
- **MOCCA CLI bridge** on your `PATH`. The alpha runbook assumes the bridge is running locally and reachable.
- **A GitHub personal access token** if you want to test in-app APK updates without hitting rate limits. The token is optional for the alpha, but a no-token path runs against unauthenticated GitHub API limits.

On Windows 11 specifically:

- Use `gradlew.bat` from the repo root, not `gradlew`.
- Do not rely on `android emulator` from the Android CLI. It is documented as disabled on Windows in Android CLI 0.7. Use the repo's emulator and Maestro scripts instead.
- If the Android CLI is not on your `PATH`, run it through the repo wrapper at `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\scripts\android-cli.ps1`.

## Why Emulator Only

MOCCA's automated checks assume an Android emulator. The host machine is reachable from the emulator at `10.0.2.2`. The smoke and regression flows in `maestro-workspace/` are written against that assumption. Do not run connectivity tests against a physical device for the alpha; the bridge and OpenCode server URLs in the runbook point at the emulator host loopback and will not work the same way on hardware.

## Quick Start

These commands assume the repo root is `C:\Users\ruzaq\AndroidStudioProjects\MOCCA`. Use absolute paths in any scripts you save.

### 1. Build the debug APK

```powershell
powershell.exe -NoProfile -Command "& 'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\gradlew.bat' :androidApp:assembleDebug"
```

The artifact lands in `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\androidApp\build\outputs\apk\debug\`.

### 2. Start the MOCCA CLI bridge

```bash
mocca-cli
```

If you are pairing over Tailscale:

```bash
mocca-cli tailscale
# or
mocca-cli --tailscale
```

If port `17653` is taken:

```bash
mocca-cli tailscale --port 17654
```

Leave this running in a terminal. The bridge prints a QR code that the app scans during pairing.

### 3. Start an emulator and install the APK

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\start-emulator.ps1"
```

After the emulator is up, install the debug APK:

```powershell
adb install -r C:\Users\ruzaq\AndroidStudioProjects\MOCCA\androidApp\build\outputs\apk\debug\app-debug.apk
```

### 4. Pair MOCCA with the bridge

Open MOCCA on the emulator. The first launch lands on the Progressive Onboarding screen. Tap the QR code option and scan the QR code from the running `mocca-cli` terminal. Pairing should complete within a few seconds and drop you onto the main chat shell.

If you are testing the legacy HTTP/SSE fallback instead, configure a server profile manually under Settings. Use `http://10.0.2.2:4096` as the host, with the username and password you exported when starting the OpenCode server.

### 5. Send a test prompt

Type something short into the chat input and send. You should see one user message and one assistant reply. If the model has reasoning, you should see the reasoning block appear before the final text. Permissions, tool calls, and questions all surface inline.

### 6. Optional: run the smoke plan

If you have Maestro installed, you can replay the canned smoke flow:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\run-emulator-tests.ps1" "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\testplans\smoke.yaml"
```

The smoke flow covers launch, settings reachability, and a basic session create. It does not exercise the bridge or send prompts. Use it as a baseline, not a full check.

## Optional: Legacy OpenCode Server

Use the legacy direct-OpenCode path only for fallback verification, not as the primary test target.

```bash
export OPENCODE_SERVER_USERNAME=opencode
export OPENCODE_SERVER_PASSWORD=your_password
opencode serve --port 4096
opencode serve --port 4096 --hostname 0.0.0.0
```

A helper script lives at `C:\Users\ruzaq\AndroidStudioProjects\MOCCA\scripts\mocca-serve-emulator.ps1` for the emulator-specific OpenCode bring-up.

## What Works (Confirmed in Alpha)

- Bridge-first pairing by QR code with `mocca-cli` running locally or over Tailscale.
- Streaming chat with reasoning parts, tool calls, permission prompts, and question replies.
- Session history, fork, revert, and summary actions.
- Files, git, terminal, MCP, commands, and agents panels sourced from bridge where supported, with HTTP fallback.
- Server-first data: cached data appears immediately, then refreshes from the network/bridge, then updates the cache, then emits fresh state.
- Material 3 Expressive dark theme with the app-owned `AppColors`, `AppTypography`, and `AppShapes` token set.
- In-app APK update check and download through the GitHub Releases API.
- Periodic silent sync of MCP, providers, tools, commands, agents, and git state while connected.

## Current Limitations

Read this section before testing. Most of these are intentional for the alpha and not regressions.

- **Android only.** There is no iOS build. There is no desktop build. The shared `composeApp` module is set up Android-only by policy.
- **Debug-signed APK.** The shipped artifact is debug-signed. It will install on most emulators and dev devices, but it is not a Play Store ready build.
- **No Play Store distribution.** MOCCA is not published to Google Play in the alpha. The only distribution channel is the GitHub Releases APK.
- **Bridge is the primary path.** Direct OpenCode HTTP/SSE is preserved as a legacy fallback. If the bridge is down and you do not have a legacy server profile configured, MOCCA will not connect.
- **Bridge required for full runtime config.** Provider, model, variant, agent, and mode selection are sourced from the bridge when connected. Without the bridge, the model picker shows whatever HTTP fallback can find, and some capability checks are skipped.
- **Server-owned OpenCode config.** MOCCA does not write `opencode.json`. Provider defaults and model defaults come from OpenCode itself, not from MOCCA preferences. Settings that look like they should change OpenCode behavior but actually cannot have been removed.
- **English-only UI strings.** Localization work has not started. Do not file translation bugs in the alpha.
- **Emulator is the canonical target.** Physical device testing is fine for ad-hoc checks, but the smoke and regression suites are written for the emulator. Host connectivity tests against hardware are not supported in the runbook.
- **No iCloud, no Google Drive, no cross-device sync.** Sessions, settings, and recents live in the local SQLDelight store. They do not sync anywhere.

## Known Issues

These are real, observed, and not yet fixed. If you hit one, the workarounds below are the recommended paths.

- **Settings "show thinking" toggle was removed.** The control is gone in this build. Reasoning parts still render in chat from the canonical `reasoning` event source. The legacy `thinking` alias is only used for older cached or imported payloads.
- **Compact mode, font scale, data saver, screen security, clear cache on exit, and similar toggles were removed.** They were dead controls that did not change real behavior. Re-adding them is not on the alpha roadmap.
- **Provider auth controls do not store keys in MOCCA.** OpenCode keeps provider credentials on the server side. MOCCA's provider auth section explains this and links to the server-owned auth flow. Do not paste provider keys into MOCCA expecting them to be used.
- **GitHub token is update-only.** The token field under App Updates exists so MOCCA can call the GitHub Releases API for in-app updates. It is not used for anything else. The token is stored in Android Keystore-backed encrypted storage. If you do not enter a token, MOCCA falls back to unauthenticated GitHub API calls and shows a rate-limit warning when the limit is hit.
- **First launch on the emulator can show the onboarding screen longer than expected on cold cache.** A short delay is normal. If the screen does not change in 30 seconds, check the bridge process and the network from the emulator to the host.
- **Reasoning text may briefly flicker on rapid model switches.** We are part-addressing reasoning through `LocalCache.updateMessagePart`, so the content lands in the right slot, but cross-part transitions can still blink. Treat it as visual noise, not data loss.
- **Patch-only diffs are the only diff format OpenCode sends in current versions.** MOCCA renders them, but if a session somehow has cached old-style full diffs, the older fields are ignored. The visible diff still works.
- **The `RealtimeSyncService` periodic loop runs while connected.** If you kill the bridge and bring it back quickly, the loop can race with the manual sync triggered by reconnect. The guard prevents double-fire, but the logs may show a single deferred attempt.
- **APK install from a downloaded update requires "Install unknown apps" permission for MOCCA on the device.** The system will prompt you. This is normal and not a bug.
- **No notification for permission requests while the app is killed.** If the agent triggers a permission request when MOCCA is not in the foreground, the request still appears when you reopen the app, but no heads-up notification fires. Active agent runs do use Live Updates; passive permission state does not.
- **Auto-update over Tailscale only works if the emulator can reach the GitHub Releases API.** The emulator usually has internet, but locked-down corporate networks may block it. Run update tests on a network that can reach `api.github.com`.

## Settings That Are Honest Now

The Settings screen is short on purpose. Every visible control does something real. The current sections are:

- **App Config** (read-only): shows the OpenCode server config and provider defaults. Comes from the server, not MOCCA.
- **Appearance**: code font, show timestamps, show token counts. All local, all working.
- **App Updates**: GitHub token, check for updates, update channel, install behavior.
- **Experimental**: feature flags sourced from the server config.
- **Notifications**: notification toggles for the cases that actually have a notification path.
- **Privacy & Security**: bridge pairing, secure token storage info, app lock if enabled.
- **Project**: project path display and edit.
- **Provider Auth**: server-side provider auth status. Keys are not stored in MOCCA.

If you see a control that does not do what its label says, that is a bug. Report it under "Reporting Bugs" below.

## Reporting Bugs and Giving Feedback

Use the issue tracker you were onboarded with. The minimum useful report includes:

1. **Build identifier.** Pull it from Settings > App Updates or from the APK file name. The format is `app-debug.apk` for the alpha.
2. **Device and OS version.** Emulator API level plus the AVD name is enough. For physical devices, include the model and Android version.
3. **Connection mode.** Were you on the bridge, the legacy OpenCode server, or offline?
4. **Steps to reproduce.** The shortest path that triggers the bug.
5. **What you expected.**
6. **What actually happened.** Screenshots help. Screen recordings help more.
7. **Logcat slice** if the bug looks like a crash or a hang. Use:

   ```powershell
   adb logcat -c
   adb logcat *:W | findstr "mocca|Exception"
   ```

   The filter keeps warnings and exceptions and drops the noise.
8. **Whether the build was installed fresh or upgraded over a previous alpha.** Fresh installs and upgrades can behave differently, especially around Settings persistence and the `RecentModel` migration.

If the bug is in chat streaming, reasoning, or model behavior, also include:

- The provider and model you were using.
- The variant, if you changed it.
- The exact prompt or a redacted version of it.

For UI bugs, prefer a screenshot over prose. For crashes, prefer a logcat capture over a description.

## GitHub Token Setup for In-App Updates

The token is optional but recommended for testers running repeated update checks.

1. Create a fine-grained personal access token at `https://github.com/settings/tokens?type=beta`. The minimum scope is public repositories read-only. MOCCA only calls the public Releases API.
2. Open MOCCA, go to Settings > App Updates.
3. Paste the token into the GitHub Token field. The input is masked. Tap Save.
4. Tap Check Updates. If the token is valid, MOCCA shows the latest available version and offers to download it.
5. After download completes, the system install prompt opens. Approve it to install the update.

The token is stored in the Android Keystore-backed encrypted storage in `SecureTokenStorage`. It is not written to plain SharedPreferences and it is not printed to logs. If you remove the token, MOCCA falls back to unauthenticated API calls and shows a rate-limit warning when the GitHub limit is hit.

If you previously had a token stored in the old plaintext path, MOCCA migrates it to encrypted storage on first read after upgrade. You do not need to re-enter it.

## Build and Test Commands (Reference)

These commands use absolute paths. Paste them as-is.

Build the debug APK:

```powershell
powershell.exe -NoProfile -Command "& 'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\gradlew.bat' :androidApp:assembleDebug"
```

Run shared host tests:

```powershell
powershell.exe -NoProfile -Command "& 'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\gradlew.bat' :composeApp:allTests"
```

Start a visible emulator:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\start-emulator.ps1"
```

Run the smoke plan on a running emulator:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\run-emulator-tests.ps1" "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\testplans\smoke.yaml"
```

Generate a screenshot catalog (optional, used for release assets):

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\ruzaq\AndroidStudioProjects\MOCCA\maestro-workspace\capture-screenshot-catalog.ps1"
```

## Rollback

If the alpha build is broken on your machine:

1. Uninstall MOCCA: `adb uninstall com.mocca.app` (use the actual application id from your build if it differs).
2. Reinstall a known-good APK from the GitHub Releases history.
3. If the data is suspect, clear app storage from the device settings before reinstalling. This drops the SQLDelight store and the encrypted token.
4. If the bridge is wedged, kill the `mocca-cli` process and restart it.

Do not roll back to a build before the secure token migration if you have an existing GitHub token. Old builds wrote plaintext; new builds will not. Treat the migration as one-way and test new work on the current build.

## What Is Not In Scope for Alpha

These are explicit non-goals for this release, so do not file them as bugs:

- Play Store packaging and signing.
- iOS, desktop, or web builds.
- Cloud sync of sessions, settings, or recents.
- Multi-account support.
- Production key management beyond Android Keystore.
- Localization beyond English.
- New runtime configuration sources beyond the bridge and OpenCode HTTP.
- A second source of truth for streaming or reasoning state.

## Pointers for the Curious

If you want to read the actual code while you test:

- Bridge-first connection: `composeApp/src/commonMain/kotlin/com/mocca/app/bridge/connection/BridgeConnectionManager.kt`
- Chat event reducer: `composeApp/src/commonMain/kotlin/com/mocca/app/domain/model/ChatTurnReducer.kt`
- Server-first data flow: `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/`
- Settings sections: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/sections/`
- Theme tokens: `composeApp/src/commonMain/kotlin/com/mocca/app/ui/theme/`
- Maestro flows: `maestro-workspace/flows/` and `maestro-workspace/testplans/`
- Update download path: `composeApp/src/androidMain/kotlin/com/mocca/app/domain/manager/AndroidUpdateManager.kt`
- Token storage: `composeApp/src/androidMain/kotlin/com/mocca/app/data/security/SecureTokenStorage.kt`

## Questions

If something here is wrong, missing, or out of date, that is a runbook bug. Treat it the same as a code bug and file it through the same channel.
