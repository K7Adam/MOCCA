# MOCCA Codex Cloud Environment

Use these settings for the Codex web/cloud environment for `K7Adam/MOCCA`.

## Runtime Versions

Set package/runtime versions:

```text
Java: 17
Node.js: 22
Rust: 1.94.0
```

Equivalent `codex-universal` variables:

```text
CODEX_ENV_JAVA_VERSION=17
CODEX_ENV_NODE_VERSION=22
CODEX_ENV_RUST_VERSION=1.94.0
```

## Environment Variables

```text
MOCCA_ANDROID_SDK_ROOT=/home/opencode/android-sdk
ANDROID_HOME=/home/opencode/android-sdk
ANDROID_SDK_ROOT=/home/opencode/android-sdk
MOCCA_CODEX_WARM_BUILD=1
MOCCA_CODEX_BUILD_TASKS=:androidApp:assembleDebug
MOCCA_CODEX_WARM_CLI=1
MOCCA_CODEX_MAINTENANCE_WARM_BUILD=0
```

Do not add local `.env` values, OpenCode passwords, ChatGPT tokens, or Android signing secrets to this environment. The current debug keystore is repo-local and enough for debug APK builds.

## Setup Script

```bash
bash .codex/setup-mocca-cloud.sh
```

The setup script:

- installs Android SDK command-line tools, platform tools, API 36, and build-tools 36.0.0;
- writes ignored `local.properties` with the cloud SDK path;
- installs Node workspace dependencies with `npm ci`;
- warms the Gradle wrapper and debug APK build cache;
- warms the MOCCA CLI build when Rust is available;
- configures Android CLI only if `android` is already present in the image.

It intentionally does not install emulator images. Codex cloud is best used for code changes and host/build checks; emulator and Maestro verification should stay in GitHub Actions or the local MOCCA workspace.

## Maintenance Script

```bash
bash .codex/maintenance-mocca-cloud.sh
```

This is safe for cached containers. It refreshes ignored local SDK config, Node dependencies, and Gradle metadata without forcing a full APK build unless `MOCCA_CODEX_MAINTENANCE_WARM_BUILD=1`.

## Agent Internet Access

Recommended setting:

```text
On, restricted to GET/HEAD/OPTIONS
Preset: Common dependencies
Additional domains:
dl.google.com
maven.google.com
repo.maven.apache.org
plugins.gradle.org
plugins-artifacts.gradle.org
services.gradle.org
registry.npmjs.org
static.rust-lang.org
crates.io
index.crates.io
github.com
raw.githubusercontent.com
developer.android.com
developers.google.com
```

Reason: setup always has internet, but later Codex tasks may need read-only access when Gradle, npm, Rust, Android docs, or dependency metadata changes. Keep write methods blocked unless a task explicitly requires them.

## First Cloud Check

After saving the environment, start a Codex cloud task with:

```text
Check the MOCCA environment. Run `./gradlew --no-daemon :androidApp:assembleDebug` and `npm run cli:build`, then summarize any setup problems without changing code.
```
