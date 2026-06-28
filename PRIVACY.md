# MOCCA Privacy & Security

MOCCA is a local-first Android app. It talks to an OpenCode server you control, keeps your data on your device, and ships without analytics, tracking, or third-party SDKs. This page describes what the app stores, what it sends, and what it doesn't do.

If something below changes in a way that affects your privacy, we'll call it out in the release notes for that version.

---

## TL;DR

- **No cloud, no telemetry, no third parties.** MOCCA sends data only to the OpenCode server you configure (and to GitHub for optional update checks).
- **All app data lives on your device.** Sessions, messages, settings, and tokens are stored locally in a SQLDelight database and Android Keystore.
- **Tokens are encrypted at rest.** GitHub PATs and server passwords use AES-256-GCM via the Android Keystore.
- **No analytics, no crash reporting, no fingerprinting.** If the app crashes, you see a logcat line. We don't see anything.

---

## What Data Is Stored Locally

Everything below lives in the app's private storage on your device. None of it leaves your phone unless the app explicitly sends it somewhere, and the only "somewheres" are the servers you configured.

### In the SQLDelight Database (`AppDatabase`)

The app uses SQLDelight to persist state across launches. The schema lives in `composeApp/src/commonMain/sqldelight/com/mocca/app/db/`.

| Table | What's in it |
|---|---|
| `Session` | Session IDs, titles, parent session (for forks), creation/update timestamps, project association |
| `Message` | Chat messages, role (user/assistant), timestamps, completion state, session association |
| `MessagePart` | Streamed text deltas, reasoning parts, file parts, tool call metadata, all keyed by `messageId + partId` |
| `Agent` | Cached agent configurations (prompts, mode, model settings) from the bridge or server |
| `Command` | Cached slash commands and shell tools |
| `FileInfo` | Cached workspace file browser hierarchy to cut down on repeat fetches |
| `SessionTodo` | Session-specific todo lists |
| `ServerConfig` | Server profiles: name, host, port, username, **password (encrypted)**, HTTPS toggle, active flag |
| `AppSettings` | Key-value bag: AI selection, project-scoped model recents, bridge target, app preferences |

The database file is stored in the app's private data directory. It's not world-readable, not exported, and not backed up to a cloud service unless your device backup configuration explicitly includes app data.

### In Android Keystore

- **AES-256-GCM key** for token encryption, stored under the alias `mocca_auth_token_key`. Hardware-backed on devices with a TEE or StrongBox, software-backed on older devices.

### In `AppSettings` (as Encrypted Strings)

- **GitHub personal access token** (if you provide one for update checks). Stored as `ENC:<base64>` ciphertext.
- **OpenCode server password** (if you use direct server auth). Stored as `ENC:<base64>` ciphertext in the `ServerConfig` table.

Plaintext values from older builds auto-migrate to the encrypted format on first read. There's no manual migration step.

### In SharedPreferences (via `PreferencesManager`)

- UI preferences (theme, density, last active session, etc.). No secrets.

---

## What Data Is Sent Off-Device

MOCCA makes network calls to two categories of servers, and **only** these two. Both are servers you choose.

### 1. Your OpenCode Server

Configured in Settings → Servers. This is the server you started, on a machine you control (locally, on your LAN, or via Tailscale).

**What's sent:**

- Standard OpenCode REST requests: session CRUD, message history, file operations, VCS status, shell commands, provider/agent metadata
- Streaming SSE event subscription for live chat updates (text deltas, reasoning, tool calls, session status)
- HTTP Basic Auth credentials (username + password) on every request, using whatever you saved in the server profile
- The contents of your chat messages, file paths you ask the agent to touch, and any tool input the agent requests

**What's NOT sent to your server:**

- The contents of other apps on your device
- Your device contacts, location, calendar, or media
- Android system identifiers (we don't read them)
- Anything from any other MOCCA user

Your server can do whatever it wants with what you send it, just like any other OpenCode client. If you run an OpenCode server on the public internet, that's your call. We default the docs to Tailscale pairing for a reason.

### 2. GitHub (Optional, Only If You Configure It)

If you paste a GitHub personal access token in Settings → Updates, the app uses it to call the GitHub Releases API for the MOCCA repository. The token and the API calls are used **only** for:

- Listing recent MOCCA release tags
- Checking if a newer build is available
- Downloading a newer APK to your device's `Download/` directory (which you then install manually)

**What GitHub sees:**

- The authenticated API call from your IP
- Your GitHub username (returned by the API)
- The standard rate-limit counter for your token

**What's NOT sent to GitHub:**

- Chat content, session data, file paths, or anything from the SQLDelight database
- Crash reports, analytics, or telemetry
- The contents of any other app

If you don't supply a token, MOCCA still polls the GitHub Releases API without authentication. You'll get rate-limited faster, and the app surfaces that as a friendly status message instead of failing silently.

### 3. Nothing Else

There is no third endpoint. No crash reporting service, no analytics SDK, no attribution provider, no ad network, no remote config service, no push notification backend. If the app crashes, the logcat stays on your device. We do not have a server collecting your data.

---

## Permissions

MOCCA declares the minimum permissions needed to function. We do not request optional permissions upfront.

| Permission | Why | When |
|---|---|---|
| `INTERNET` | Talk to your OpenCode server and GitHub | Always (install-time) |
| `ACCESS_NETWORK_STATE` | Detect connectivity drops for the reconnect flow | Always (install-time) |
| `ACCESS_WIFI_STATE` | Detect Wi-Fi connectivity for multicast and bridge discovery | Always (install-time) |
| `CHANGE_WIFI_MULTICAST_STATE` | Enable multicast for bridge discovery on local networks | Runtime, when bridge discovery is active |
| `POST_NOTIFICATIONS` | Active session foreground service | Runtime, Android 13+ |
| `POST_PROMOTED_NOTIFICATIONS` | Android 16 promoted Live Updates for active agent runs | Always (install-time), Android 16+ |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Keep an active agent session alive | Always (install-time) |
| `REQUEST_INSTALL_PACKAGES` | Install downloaded APK updates in-app | Always (install-time), used when you tap "Install" on a downloaded update |
| `RECORD_AUDIO` | Voice input for chat | Runtime, only when you tap the mic button |

QR code scanning for bridge pairing uses Google Play Services code scanner (`play-services-code-scanner`), which handles camera access internally without MOCCA declaring a `CAMERA` permission. If you don't use the mic feature, that permission never gets requested. There is no location, contacts, storage, or media permission requested by the app. We do not scan for other apps or read your clipboard.

---

## Token Storage Details

For testers who want to verify the security model:

- **Algorithm**: AES-256-GCM
- **Key size**: 256 bits
- **Key storage**: Android Keystore (`AndroidKeyStore` provider), under alias `mocca_auth_token_key`
- **Key generation**: On first use, via `KeyGenerator` with `KeyGenParameterSpec`. Hardware-backed (TEE/StrongBox) when the device supports it, software-backed otherwise. The key never leaves the Keystore boundary.
- **IV handling**: Fresh 12-byte IV per encryption, generated by the cipher. No IV reuse.
- **Auth tag**: 128-bit GCM tag, verified on decrypt. Tampered ciphertext fails closed.
- **Stored format**: `ENC:<base64(IV || ciphertext || tag)>`. The `ENC:` prefix lets the storage layer distinguish encrypted values from legacy plaintext during migration.
- **Key destruction**: `SecureTokenStorage.clearKey()` deletes the keystore entry and regenerates. Used by reset flows.

The Keystore is keyed to the app's signing certificate. If MOCCA is reinstalled with a different key, the Keystore entry is gone and any encrypted values become unreadable. This is by design.

---

## Data Deletion

Uninstalling MOCCA deletes the app's private data, including the SQLDelight database, `AppSettings`, and the Keystore entry (the Keystore entry is scoped to the app UID and dies with it).

For a clean reset without uninstall:

1. Settings → Servers → delete each server profile
2. Settings → Updates → clear the GitHub token
3. Settings → About → "Clear local cache" (where available) or clear app data from Android Settings

There is no cloud backup to wipe, because there is no cloud backup.

---

## Open Source and Auditability

MOCCA's code is open source under the MIT license. The encryption implementation lives in `composeApp/src/androidMain/kotlin/com/mocca/app/data/security/SecureTokenStorage.kt`. The token storage interface is in `composeApp/src/commonMain/kotlin/com/mocca/app/data/security/SecureTokenStorage.kt`. If you find a problem, file an issue or a PR.

The build artifact distributed for this alpha is signed with the standard Android SDK debug key. That's fine for testers but means anyone can re-sign a modified build. Don't install MOCCA APKs from sources you don't trust. When we cut a real release build, we'll publish the SHA-256 of the signing certificate so you can verify what you installed.

---

## Contact

If you have a privacy question that isn't answered here, open an issue on the GitHub repository. We don't have a privacy@ email set up because this is an alpha project run by one person, and a public GitHub issue keeps the answer in the open where the next tester can find it.
