<#
.SYNOPSIS
    Starts the OpenCode Git Server (Embedded in git-plugin.js).
.DESCRIPTION
    Launches the git-plugin.js in standalone server mode using Bun.
    This listens on port 4097 for Git operations from the MOCCA app.
#>

$ErrorActionPreference = "Stop"

# Configuration
$WorkDir = $PSScriptRoot
$PluginScript = "$WorkDir\.opencode\plugins\git-plugin.js"
$LogFileOut = "$WorkDir\git-server.out.log"
$LogFileErr = "$WorkDir\git-server.err.log"

# Find Bun executable
$BunPath = Get-Command "bun" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
if (-not $BunPath) {
    # Try common location
    $PotentialPath = "$env:APPDATA\npm\bun.exe"
    if (Test-Path $PotentialPath) {
        $BunPath = $PotentialPath
    } else {
        $PotentialPath = "$env:USERPROFILE\.bun\bin\bun.exe"
        if (Test-Path $PotentialPath) {
            $BunPath = $PotentialPath
        }
    }
}

if (-not $BunPath) {
    Write-Error "Bun not found! Please install Bun (https://bun.sh) to run the git server."
    exit 1
}

# Validation
if (-not (Test-Path $PluginScript)) {
    Write-Error "Git plugin script not found at: $PluginScript"
    exit 1
}

# Stop existing processes
Write-Host "Checking for existing git server processes..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -like "*bun*" -and $_.MainWindowTitle -like "*git-plugin*"} | Stop-Process -Force -ErrorAction SilentlyContinue

# Start Server
Write-Host "Starting Git Server..." -ForegroundColor Cyan
Write-Host "Runtime: $BunPath"
Write-Host "Script: $PluginScript"
Write-Host "WorkDir: $WorkDir"

try {
    # Start process in background
    $Process = Start-Process -FilePath $BunPath `
        -ArgumentList "run", """$PluginScript""", "start-server" `
        -WorkingDirectory $WorkDir `
        -RedirectStandardOutput $LogFileOut `
        -RedirectStandardError $LogFileErr `
        -PassThru `
        -WindowStyle Hidden

    Write-Host "SUCCESS: Git Server started with PID $($Process.Id)" -ForegroundColor Green
    Write-Host "Logs available at:"
    Write-Host "  OUT: $LogFileOut"
    Write-Host "  ERR: $LogFileErr"
    
    # Wait a moment to check for immediate failure
    Start-Sleep -Seconds 1
    if ($Process.HasExited) {
        Write-Error "Server process exited immediately. Check logs."
        Get-Content $LogFileErr -Tail 10 | Write-Host -ForegroundColor Red
        exit 1
    }
}
catch {
    Write-Error "Failed to start server: $_"
    exit 1
}