# MOCCA Release Notes

## 1.0.1 (Internal Alpha)

This is an internal alpha build of MOCCA, the Android companion app for OpenCode. It's meant for testers and developers who run their own OpenCode server. Expect rough edges, missing polish, and the occasional crash. We need real-world feedback before we widen distribution.

### Version Info

| Field | Value |
|---|---|
| Version | 1.0.1 |
| Application ID | `com.mocca.app` |
| Min Android | 7.0 (API 31) |
| Target Android | 16 (API 36) |
| Signing | Debug keystore (alpha only, see [Known Limitations](#known-limitations)) |

CI-built artifacts follow the pattern `1.0.1-build.{run_number}`. Local debug builds report as `1.0.1`.

---

## What's New

### Bridge-First Architecture

The MOCCA CLI bridge is now the primary path for AI runtime configuration, provider selection, and chat orchestration. Pair by scanning a QR code from the bridge. Direct OpenCode HTTP/SSE still works as a fallback for advanced users running the OpenCode server by hand.

Why this matters: the bridge gives you project-scoped model picker recents, smarter model discovery, and a stable v2 capability surface. Legacy HTTP still works for compatibility, but new work lands on the bridge first.

### Secure Token Storage

GitHub personal access tokens (used for in-app update checks against GitHub Releases) and OpenCode server passwords are now encrypted with AES-256-GCM using the Android Keystore. The key is hardware-backed when the device supports it, software-backed otherwise. Existing plaintext tokens in `AppSettings` and the SQLDelight DB auto-migrate to the encrypted format on first read.

Token input fields now mask your paste with `PasswordVisualTransformation`, so shoulder surfers don't get a free look.

### Honest Settings

The Settings screen no longer ships dead controls. Sections that only displayed server-owned values got reorganized so what's local stays local and what comes from the bridge is labeled as such. We deleted switches that didn't do anything rather than leave them lying around as noise.

### Event Deduplication Hardening

`EventStreamRepository` no longer relies on `hashCode()` for dedup IDs. Dedup keys are now built from deterministic fields like message timestamps, completion times, and `delta?.length`. We also added a session filter at the top of `handleEvent()` so OpenCode's broadcast of every session's events doesn't trigger redundant work for sessions you aren't watching.

The result: fewer duplicate reducer calls, less flicker in long sessions, and a clean foundation for the chat UI's part-based rendering.

### Reconnect Resilience

`ConnectionManager` re-runs health checks and refetches critical data after a dropped connection. The event stream reattaches and `StateCoordinator` rebuilds the active chat state from the network. If the bridge was the source, it re-handshakes. You should see one clear "reconnecting" state during drops, not a half-stuck UI.

---

## Known Limitations

### Debug-Only Signing

Both `debug` and `release` build types sign with the same debug keystore (the standard Android SDK debug key). That is fine for internal alpha testers. It is **not** acceptable for production distribution, and we won't claim otherwise. A proper release keystore with secrets stored outside the repo comes before any wider rollout.

### Bridge Dependency for Full Features

If you don't run the MOCCA CLI bridge, the app falls back to the legacy OpenCode HTTP/SSE path. Most things work. Model picker recents, live provider discovery, and a few v2-only capabilities don't. We document this clearly in app, but please don't file bugs about "missing recents" against a server you started with `opencode serve` by itself. Run `mocca-cli` for the full experience.

### Android Only

MOCCA is Android-only. There is no iOS build, no desktop build, and no web build. The project is a Kotlin Multiplatform codebase structured for shared business logic and an Android UI, not for cross-platform deployment. Don't ask when iOS is coming. The answer is "not planned."

### Other Honest Caveats

- OAuth callbacks use the `mocca://` custom scheme. Make sure your server auth flow respects it.
- The bridge uses Tailscale pairing as the recommended path. Plain LAN works but you'll need port forwarding.
- The GitHub update checker is optional. If you don't supply a token, you get unauthenticated API access and a polite rate-limit message.
- Foreground `dataSync` service is real and runs while a session is active. Battery impact is modest but non-zero.

---

## Installation

### What You Need

- An Android device or emulator running **Android 7.0 (API 31) or newer**
- An OpenCode server you can reach (yours, on your network or via Tailscale)
- The MOCCA CLI bridge installed on the same machine that runs OpenCode

### Install the APK

1. Download the latest `mocca-1.0.1-build.*.apk` from the GitHub Releases page.
2. On your Android device, allow installs from your file manager or browser (Settings → Apps → Special access → Install unknown apps).
3. Open the APK and tap Install.
4. Launch MOCCA. If this is your first run, you'll land on the connection setup screen.
5. Start the bridge on your dev machine: `mocca-cli`. Scan the QR code from the bridge output with MOCCA's connection screen.

### Direct OpenCode Server (Fallback)

If you can't or don't want to use the bridge:

1. Start OpenCode with credentials:
   ```bash
   export OPENCODE_SERVER_USERNAME=opencode
   export OPENCODE_SERVER_PASSWORD=your_password
   opencode serve --port 4096 --hostname 0.0.0.0
   ```
2. In MOCCA's connection screen, choose "Direct server" and enter `http://10.0.2.2:4096` (emulator) or `http://<your-server-ip>:4096` (physical device on the same network).
3. Enter the username and password you set.

### Updating

MOCCA checks GitHub Releases for new builds on a scheduled interval if you supply a GitHub token in Settings → Updates. Without a token, you can manually download the latest APK from the Releases page and install over the existing app. The signing key is the same, so updates install cleanly.

---

## Reporting Issues

When you hit a bug, please include:

- MOCCA version (visible in Settings → About, or the APK filename)
- OpenCode server version and how you started it (bridge vs. direct)
- Device model and Android version
- A logcat capture filtered to the app: `adb logcat *:W | grep mocca`

File issues on the GitHub repository. Don't share your server password or bridge pairing token in public reports.
