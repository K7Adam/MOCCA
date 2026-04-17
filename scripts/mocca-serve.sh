#!/usr/bin/env bash
# mocca-serve.sh — MOCCA Quick-Start: start OpenCode server with correct settings
#
# Usage:
#   ./scripts/mocca-serve.sh [--port PORT] [--hostname HOST] [--username USER] [--password PASS] [--skip-install] [--dry-run]
#
# Options:
#   --port PORT         Server port (default: 4096)
#   --hostname HOST     Bind address (default: 0.0.0.0)
#   --username USER     Auth username (default: opencode)
#   --password PASS     Auth password (default: auto-generated)
#   --skip-install      Do not prompt for OpenCode installation
#   --dry-run           Print configuration and exit without starting the server
#
# Notes:
#   - MOCCA itself does NOT require Node.js. Only the OpenCode server does.
#   - For Android emulator, use host 10.0.2.2 in MOCCA settings.
#   - For LAN/Tailscale, use your machine's IP address.

set -euo pipefail

PORT=4096
HOSTNAME="0.0.0.0"
USERNAME="opencode"
PASSWORD=""
SKIP_INSTALL=false
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --port)       PORT="$2"; shift 2 ;;
        --hostname)   HOSTNAME="$2"; shift 2 ;;
        --username)   USERNAME="$2"; shift 2 ;;
        --password)   PASSWORD="$2"; shift 2 ;;
        --skip-install) SKIP_INSTALL=true; shift ;;
        --dry-run)    DRY_RUN=true; shift ;;
        -h|--help)
            head -20 "$0" | grep '^#' | sed 's/^# \?//'
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--port PORT] [--hostname HOST] [--username USER] [--password PASS] [--skip-install] [--dry-run]"
            exit 1
            ;;
    esac
done

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo ""
echo "== MOCCA Quick-Start: OpenCode Server Helper =="
echo "Repo: $REPO_ROOT"
echo ""

# --- Check for opencode ---
if ! command -v opencode &>/dev/null; then
    echo "== OpenCode CLI not found =="
    echo ""

    if [[ "$SKIP_INSTALL" == "true" ]]; then
        echo "Skipping install (--skip-install was set)."
        echo ""
        echo "Install OpenCode manually:"
        echo "  npm install -g @anthropic-ai/opencode"
        echo "  -- or --"
        echo "  curl -fsSL https://opencode.ai/install | bash"
        exit 1
    fi

    echo "OpenCode is required for MOCCA but is not installed."
    echo ""
    echo "Install it with one of:"
    echo "  npm install -g @anthropic-ai/opencode"
    echo "  -- or --"
    echo "  curl -fsSL https://opencode.ai/install | bash"
    echo ""
    echo "After installing, re-run this script."
    echo ""
    echo "NOTE: MOCCA itself does NOT require Node.js. Only the OpenCode server does."
    exit 1
fi

echo "== OpenCode CLI found =="
echo "Path: $(command -v opencode)"
echo "Version: $(opencode --version 2>&1 || echo 'unknown')"
echo ""

# --- Generate password if not provided ---
if [[ -z "$PASSWORD" ]]; then
    PASSWORD=$(openssl rand -base64 16 2>/dev/null || LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 16)
    echo "== Generated credentials =="
else
    echo "== Using provided credentials =="
fi

echo "Username: $USERNAME"
echo "Password: $PASSWORD"
echo "Port:     $PORT"
echo "Hostname: $HOSTNAME"
echo ""

# --- Export environment variables ---
export OPENCODE_SERVER_USERNAME="$USERNAME"
export OPENCODE_SERVER_PASSWORD="$PASSWORD"

# --- Build the command ---
echo "== Server command =="
echo "  opencode serve --port $PORT --hostname $HOSTNAME"
echo ""

# --- Connection info ---
echo "== MOCCA connection details =="
echo "After the server starts, configure MOCCA with:"
echo ""
echo "  Host:     localhost (or your machine's LAN IP)"
echo "  Port:     $PORT"
echo "  Username: $USERNAME"
echo "  Password: (shown above)"
echo ""
echo "For Android emulator, use host 10.0.2.2"
echo "For LAN/Tailscale, use your machine's IP address"
echo ""

# --- Health check hint ---
echo "== Health check (run in another terminal) =="
echo "  curl -u ${USERNAME}:${PASSWORD} http://localhost:${PORT}/global/health"
echo ""

if [[ "$DRY_RUN" == "true" ]]; then
    echo "== Dry run -- not starting server =="
    echo "Remove --dry-run to start the server."
    exit 0
fi

# --- Start the server ---
echo "== Starting OpenCode server =="
echo "Press Ctrl+C to stop."
echo ""

exec opencode serve --port "$PORT" --hostname "$HOSTNAME"
