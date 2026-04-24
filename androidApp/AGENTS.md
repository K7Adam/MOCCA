# ANDROID BOOTSTRAP KNOWLEDGE BASE

**Updated:** 2026-04-24
**Scope:** `androidApp/`

## OVERVIEW
Android-only bootstrap layer: `Application`, launcher `Activity`, Manifest wiring, foreground service, notification action receiver.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| App process startup | `src/main/java/com/mocca/app/MoccaApp.kt` | Starts Koin, Napier, lifecycle observer |
| Launcher entry | `src/main/java/com/mocca/app/MainActivity.kt` | Edge-to-edge, deep links, permission prompt, `App()` |
| Android wiring | `src/main/AndroidManifest.xml` | `MoccaApp`, `MainActivity`, service, receiver, deep link |
| Active session service | `src/main/java/com/mocca/app/service/ActiveSessionService.kt` | Foreground `dataSync` service |
| Notification actions | `src/main/java/com/mocca/app/service/PermissionActionReceiver.kt` | Approve/deny actions |
| Build config | `build.gradle.kts` | Android target packaging |

## STARTUP FLOW
- Manifest declares `MoccaApp` as `android:name`
- `MoccaApp` starts Koin with `appModules + androidModule + appAndroidModule`
- `MainActivity` enables edge-to-edge and handles `mocca://oauth`
- `setContent { Surface { App() } }` hands off to shared Compose code

## CONVENTIONS
- Android layer stays thin; business logic belongs in shared `composeApp`
- Android-only services/receivers bridge to shared repositories or buses instead of duplicating logic
- Notification permission prompt only for Android 13+
- Android 16 promoted Live Updates are only for active, user-initiated, time-sensitive agent runs. Use standard `NotificationCompat` states for passive messages, completed work, and unsupported devices.
- Promotable ongoing notifications must avoid custom RemoteViews, group summaries, colorized styling, and low-importance channels.
- Keep host connectivity features emulator-friendly (`10.0.2.2`, visible emulator workflow)

## ANTI-PATTERNS
- Do not move shared state logic into `MainActivity`
- Do not bypass Koin bootstrapping in `MoccaApp`
- Do not add physical-device-only testing assumptions
- Do not break the `mocca://oauth` deep link contract without updating `MainActivity` and server auth flow together

## NOTES
- `POST_PROMOTED_NOTIFICATIONS` and multicast permissions are intentional
- `largeHeap="true"` and `networkSecurityConfig` are part of current Android bootstrap assumptions
