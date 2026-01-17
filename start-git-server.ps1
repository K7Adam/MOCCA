<#
.SYNOPSIS
    Fallback script to start the OpenCode Git Server manually.
.DESCRIPTION
    Use this script if the automated plugin fails to start the server.
    It launches the git-server.js located in your .opencode configuration directory
    and logs output to git-server-fallback.log.
#>

$ErrorActionPreference = "Stop"

# Configuration
$OpenCodeDir = "$env:USERPROFILE\.opencode"
$ServerScript = "$OpenCodeDir\git-server.js"
$LogFileOut = "$env:USERPROFILE\git-server.out.log"
$LogFileErr = "$env:USERPROFILE\git-server.err.log"
# Use the script's own directory (Project Root) as the WorkDir
$WorkDir = $PSScriptRoot

# Validation
if (-not (Test-Path $ServerScript)) {
    Write-Error "Git server script not found at: $ServerScript"
    exit 1
}

if (-not (Test-Path $WorkDir)) {
    Write-Warning "Working directory not found: $WorkDir"
    exit 1
}

# Start Server
Write-Host "Starting Git Server..." -ForegroundColor Cyan
Write-Host "Script: $ServerScript"
Write-Host "WorkDir: $WorkDir"

try {
    # Start process in background
    $Process = Start-Process -FilePath "node" `
        -ArgumentList """$ServerScript""", """$WorkDir""" `
        -WorkingDirectory $OpenCodeDir `
        -RedirectStandardOutput $LogFileOut `
        -RedirectStandardError $LogFileErr `
        -PassThru `
        -WindowStyle Hidden

    Write-Host "SUCCESS: Git Server started with PID $($Process.Id)" -ForegroundColor Green
    Write-Host "Logs available at:"
    Write-Host "  OUT: $LogFileOut"
    Write-Host "  ERR: $LogFileErr"
    Write-Host "You can close this window."
}
catch {
    Write-Error "Failed to start server: $_"
    exit 1
}
