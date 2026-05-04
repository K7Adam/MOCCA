#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() {
  printf '\n== %s ==\n' "$*"
}

warn() {
  printf 'Warning: %s\n' "$*" >&2
}

export ANDROID_HOME="${ANDROID_HOME:-${MOCCA_ANDROID_SDK_ROOT:-/home/opencode/android-sdk}}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$HOME/.android}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
  log "Android SDK missing; running full setup"
  exec "$ROOT_DIR/.codex/setup-mocca-cloud.sh"
fi

printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$ROOT_DIR/local.properties"
chmod +x "$ROOT_DIR/gradlew"

if [[ -f "$ROOT_DIR/package-lock.json" ]]; then
  log "Refresh Node workspace dependencies"
  npm ci --prefer-offline --no-audit --no-fund
fi

log "Refresh Gradle metadata"
if ! "$ROOT_DIR/gradlew" --no-daemon help >/dev/null; then
  warn "Gradle metadata refresh failed; continuing so Codex can inspect and fix the project."
fi

if [[ "${MOCCA_CODEX_MAINTENANCE_WARM_BUILD:-0}" == "1" ]]; then
  log "Warm Android build cache"
  read -r -a gradle_tasks <<< "${MOCCA_CODEX_BUILD_TASKS:-:androidApp:assembleDebug}"
  if ! "$ROOT_DIR/gradlew" --no-daemon "${gradle_tasks[@]}"; then
    warn "Maintenance warm build failed; continuing."
  fi
fi

log "MOCCA Codex cloud maintenance complete"
