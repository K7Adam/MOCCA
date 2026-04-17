[CmdletBinding()]
param(
    [int]$Port = 4096,
    [string]$Hostname = "0.0.0.0",
    [string]$Username = "opencode",
    [string]$Password,
    [switch]$SkipInstall,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "== $Title =="
}

$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path

Write-Section "MOCCA Quick-Start: OpenCode Server Helper"
Write-Host "Repo: $RepoRoot"
Write-Host ""

# --- Check for opencode ---
$opencodeCommand = Get-Command opencode -ErrorAction SilentlyContinue

if ($null -eq $opencodeCommand) {
    Write-Section "OpenCode CLI not found"

    if ($SkipInstall) {
        Write-Host "Skipping install (-SkipInstall was set)."
        Write-Host ""
        Write-Host "Install OpenCode manually:"
        Write-Host "  npm install -g @anthropic-ai/opencode"
        Write-Host "  -- or --"
        Write-Host "  See https://github.com/opencode-ai/opencode for installation options."
        exit 1
    }

    Write-Host "OpenCode is required for MOCCA but is not installed."
    Write-Host ""
    Write-Host "Install it with one of:"
    Write-Host "  npm install -g @anthropic-ai/opencode"
    Write-Host "  -- or --"
    Write-Host "  curl -fsSL https://opencode.ai/install | bash   (macOS/Linux)"
    Write-Host ""
    Write-Host "After installing, re-run this script."
    Write-Host ""
    Write-Host "NOTE: MOCCA itself does NOT require Node.js. Only the OpenCode server does."
    exit 1
}

Write-Section "OpenCode CLI found"
Write-Host "Path: $($opencodeCommand.Source)"
$versionOutput = & opencode --version 2>&1
Write-Host "Version: $($versionOutput -join ' ')"
Write-Host ""

# --- Determine credentials ---
if ([string]::IsNullOrWhiteSpace($Password)) {
    $Password = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 16 | ForEach-Object { [char]$_ })
    Write-Section "Generated credentials"
} else {
    Write-Section "Using provided credentials"
}

Write-Host "Username: $Username"
Write-Host "Password: $Password"
Write-Host "Port:     $Port"
Write-Host "Hostname: $Hostname"
Write-Host ""

# --- Environment variables ---
$env:OPENCODE_SERVER_USERNAME = $Username
$env:OPENCODE_SERVER_PASSWORD = $Password

# --- Build the command ---
$serveArgs = @("serve", "--port", $Port, "--hostname", $Hostname)
$commandDisplay = "opencode $($serveArgs -join ' ')"

Write-Section "Server command"
Write-Host "  $commandDisplay"
Write-Host ""

# --- Connection info ---
Write-Section "MOCCA connection details"
Write-Host "After the server starts, configure MOCCA with:"
Write-Host ""
Write-Host "  Host:     localhost (or your machine's LAN IP)"
Write-Host "  Port:     $Port"
Write-Host "  Username: $Username"
Write-Host "  Password: (shown above)"
Write-Host ""
Write-Host "For Android emulator, use host 10.0.2.2"
Write-Host "For LAN/Tailscale, use your machine's IP address"
Write-Host ""

# --- Verify with health check hint ---
Write-Section "Health check (run in another terminal)"
Write-Host "  curl -u ${Username}:${Password} http://localhost:$Port/global/health"
Write-Host ""

if ($DryRun) {
    Write-Section "Dry run -- not starting server"
    Write-Host "Remove -DryRun to start the server."
    exit 0
}

# --- Start the server ---
Write-Section "Starting OpenCode server"
Write-Host "Press Ctrl+C to stop."
Write-Host ""

& opencode @serveArgs
