# Tech Stack: MOCCA

## Platform

- **Target:** Android only.
- **Project shape:** Kotlin Multiplatform module with Android target only; do not add iOS, desktop, or web targets.
- **Android SDK:** compile/target SDK 36, min SDK 31.

## Language And Build

| Area | Tooling |
|---|---|
| Language | Kotlin 2.3.0 |
| Build | Gradle with Android Gradle Plugin 9.0.0-rc03 |
| Dependency catalog | `gradle/libs.versions.toml` |
| Local constraints | `gradle.properties` limits Gradle workers for constrained RAM |

## UI

| Area | Tooling |
|---|---|
| UI runtime | Compose Multiplatform 1.9.3 |
| Material shell | Material 3 Expressive 1.11.0-alpha04 |
| Adaptive UI | Material 3 Adaptive 1.2.0 |
| Navigation | Voyager 1.1.0-beta03 |
| Images | Coil 3.3.0 |
| Markdown | multiplatform-markdown-renderer 0.27.0 + highlights 0.9.0 |

Theme code owns the app surface through `AppTheme`, `AppColors`, `AppTypography`, and `AppShapes`. Feature UI should not reach directly for `MaterialTheme.colorScheme` or `MaterialTheme.shapes`.

## Data And Runtime

| Area | Tooling |
|---|---|
| Networking | Ktor 3.0.3 |
| DI | Koin 4.1.1 |
| Persistence | SQLDelight 2.2.1 |
| Serialization | kotlinx-serialization 1.9.0 |
| Concurrency | kotlinx-coroutines 1.10.2 |
| Time | kotlinx-datetime 0.6.0 |
| Logging | Napier 2.7.1 |

The MOCCA CLI bridge is the preferred runtime source. `MoccaApiClient` and SSE remain for legacy direct OpenCode server mode.

## Verification

- Unit/host tests: `.\gradlew.bat :composeApp:allTests`
- Debug APK: `.\gradlew.bat :androidApp:assembleDebug`
- Visible emulator workflow: `.\maestro-workspace\start-emulator.ps1`
- Maestro smoke plan: `.\maestro-workspace\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml`

Use Android Knowledge Base queries before changing Android platform APIs, AGP/Gradle behavior, Compose, Navigation, edge-to-edge, R8, Play Billing, emulator automation, or performance guidance.
