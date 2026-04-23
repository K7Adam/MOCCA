# R8 Configuration Analysis

Date: 2026-04-23

## Current configuration

- AGP version: `9.0.0-rc03` in [`C:\Users\ruzaq\AndroidStudioProjects\MOCCA\gradle\libs.versions.toml`](C:/Users/ruzaq/AndroidStudioProjects/MOCCA/gradle/libs.versions.toml:20)
- Release minification: enabled in [`C:\Users\ruzaq\AndroidStudioProjects\MOCCA\androidApp\build.gradle.kts`](C:/Users/ruzaq/AndroidStudioProjects/MOCCA/androidApp/build.gradle.kts:40)
- Default optimized ProGuard baseline: `proguard-android-optimize.txt`
- `android.enableR8.fullMode=false` is not present in [`C:\Users\ruzaq\AndroidStudioProjects\MOCCA\gradle.properties`](C:/Users/ruzaq/AndroidStudioProjects/MOCCA/gradle.properties:1)

## Findings

- [`C:\Users\ruzaq\AndroidStudioProjects\MOCCA\androidApp\proguard-rules.pro`](C:/Users/ruzaq/AndroidStudioProjects/MOCCA/androidApp/proguard-rules.pro:8) keeps the entire `com.mocca.app` package graph. That blocks most meaningful shrinking and obfuscation.
- [`C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\proguard-rules.pro`](C:/Users/ruzaq/AndroidStudioProjects/MOCCA/composeApp/proguard-rules.pro:24) keeps broad package groups for Ktor, Koin, Voyager, SQLDelight and Compose. Several of these libraries already ship consumer rules or do not need package-wide keeps.
- Domain-model rules such as [`Session`, `Message`, `ServerConfig`, `FileInfo`, `GitStatusResponse`](C:/Users/ruzaq/AndroidStudioProjects/MOCCA/composeApp/proguard-rules.pro:88) are still wider than needed and should eventually be narrowed to serialization entry points only.

## Recommended next pass

1. Remove the package-wide `-keep class com.mocca.app.** { *; }` rule and replace it with targeted keeps for the specific reflection or serialization entry points that fail in a release build.
2. Re-audit the Compose/Koin/Ktor/Voyager rules against actual reflection usage before keeping any package-wide library namespaces.
3. Keep serializer companions and generated `$$serializer` classes, then tighten the rest incrementally with release smoke tests.
4. Run release verification with UI automation focused on onboarding, settings, chat send, files, git and terminal flows after each narrowing step.

## Notes

- AGP 9 already enables the modern optimized shrinking pipeline when resource shrinking is enabled.
- This report documents the remaining R8 work; the app-side architecture and UI cleanup in this refactor intentionally avoided speculative keep-rule edits without release-only validation.
