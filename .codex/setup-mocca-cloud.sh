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

ensure_package_tools() {
  local missing=()
  for tool in curl unzip; do
    if ! command -v "$tool" >/dev/null 2>&1; then
      missing+=("$tool")
    fi
  done

  if ((${#missing[@]} == 0)); then
    return
  fi

  if command -v apt-get >/dev/null 2>&1; then
    log "Install OS packages"
    if command -v sudo >/dev/null 2>&1; then
      sudo apt-get update
      sudo apt-get install -y ca-certificates curl unzip
    else
      apt-get update
      apt-get install -y ca-certificates curl unzip
    fi
    return
  fi

  warn "Missing tools: ${missing[*]}. Install them in the Codex image or setup script."
  return 1
}

make_writable_dir() {
  local target="$1"

  if mkdir -p "$target" 2>/dev/null; then
    return
  fi

  if command -v sudo >/dev/null 2>&1; then
    sudo mkdir -p "$target"
    sudo chown -R "$(id -u):$(id -g)" "$(dirname "$target")"
    return
  fi

  return 1
}

configure_android_env() {
  local requested="${MOCCA_ANDROID_SDK_ROOT:-/home/opencode/android-sdk}"
  if make_writable_dir "$requested"; then
    ANDROID_HOME="$requested"
  else
    ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
    make_writable_dir "$ANDROID_HOME"
  fi

  export ANDROID_HOME
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
  export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$HOME/.android}"
  export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

  mkdir -p "$ANDROID_USER_HOME" "$GRADLE_USER_HOME"
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$ROOT_DIR/local.properties"

  local bashrc="$HOME/.bashrc"
  local tmp
  tmp="$(mktemp)"
  if [[ -f "$bashrc" ]]; then
    awk '
      /# >>> MOCCA Codex Android/ { skip = 1; next }
      /# <<< MOCCA Codex Android/ { skip = 0; next }
      !skip { print }
    ' "$bashrc" > "$tmp"
  fi
  cat "$tmp" > "$bashrc"
  rm -f "$tmp"
  cat >> "$bashrc" <<EOF
# >>> MOCCA Codex Android
export ANDROID_HOME="$ANDROID_HOME"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME="$ANDROID_USER_HOME"
export GRADLE_USER_HOME="$GRADLE_USER_HOME"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:\$PATH"
# <<< MOCCA Codex Android
EOF
}

install_android_sdk() {
  local sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
  local tools_url="${ANDROID_CMDLINE_TOOLS_URL:-https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip}"

  if [[ ! -x "$sdkmanager" ]]; then
    log "Install Android command-line tools"
    local tmpdir
    tmpdir="$(mktemp -d)"
    curl -fsSL "$tools_url" -o "$tmpdir/cmdline-tools.zip"
    unzip -q "$tmpdir/cmdline-tools.zip" -d "$tmpdir"
    rm -rf "$ANDROID_HOME/cmdline-tools/latest"
    mkdir -p "$ANDROID_HOME/cmdline-tools/latest"
    mv "$tmpdir/cmdline-tools/"* "$ANDROID_HOME/cmdline-tools/latest/"
    rm -rf "$tmpdir"
  fi

  log "Install Android SDK packages"
  local packages=(
    "cmdline-tools;latest"
    "platform-tools"
    "platforms;android-36"
    "build-tools;36.0.0"
  )

  local install_status
  set +e +o pipefail
  yes | "$sdkmanager" --sdk_root="$ANDROID_HOME" "${packages[@]}"
  install_status="${PIPESTATUS[1]}"
  set -e -o pipefail
  if [[ "$install_status" -ne 0 ]]; then
    return "$install_status"
  fi

  set +e +o pipefail
  yes | "$sdkmanager" --sdk_root="$ANDROID_HOME" --licenses >/dev/null
  local licenses_status="${PIPESTATUS[1]}"
  set -e -o pipefail
  if [[ "$licenses_status" -ne 0 ]]; then
    warn "sdkmanager --licenses exited with status $licenses_status; continuing because packages were installed."
  fi
}

configure_android_cli_if_available() {
  if ! command -v android >/dev/null 2>&1; then
    warn "Android CLI is not installed in this container; Gradle/SDK setup is still usable."
    return
  fi

  log "Configure Android CLI"
  printf -- '--sdk=%s\n' "$ANDROID_HOME" > "$HOME/.androidrc"
  android update || warn "android update failed"
  android init || warn "android init failed"
  android --sdk="$ANDROID_HOME" skills add --all || warn "android skills add --all failed"
  android --sdk="$ANDROID_HOME" info || true
}

install_node_workspace() {
  if [[ ! -f "$ROOT_DIR/package-lock.json" ]]; then
    return
  fi

  log "Install Node workspace dependencies"
  npm ci --no-audit --no-fund
}

warm_gradle() {
  log "Prepare Gradle wrapper"
  chmod +x "$ROOT_DIR/gradlew"
  "$ROOT_DIR/gradlew" --no-daemon --version

  if [[ "${MOCCA_CODEX_WARM_BUILD:-1}" != "1" ]]; then
    return
  fi

  log "Warm Android build cache"
  read -r -a gradle_tasks <<< "${MOCCA_CODEX_BUILD_TASKS:-:androidApp:assembleDebug}"
  if ! "$ROOT_DIR/gradlew" --no-daemon "${gradle_tasks[@]}"; then
    warn "Warm Gradle build failed; Codex tasks can still start and fix project-level failures."
  fi
}

warm_cli() {
  if [[ "${MOCCA_CODEX_WARM_CLI:-1}" != "1" || ! -f "$ROOT_DIR/cli/package.json" ]]; then
    return
  fi

  log "Warm MOCCA CLI build"
  if ! npm run cli:build; then
    warn "MOCCA CLI warm build failed; Android app work can continue."
  fi
}

log "MOCCA Codex cloud setup"
ensure_package_tools
configure_android_env
install_android_sdk
configure_android_cli_if_available
install_node_workspace
warm_gradle
warm_cli

log "MOCCA Codex cloud setup complete"
printf 'ANDROID_HOME=%s\n' "$ANDROID_HOME"
