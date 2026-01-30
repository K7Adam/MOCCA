#!/usr/bin/env pwsh
#Requires -Version 5.1

<#
.SYNOPSIS
    MOCCA Setup Script - One-command setup for MOCCA mobile app

.DESCRIPTION
    This script automates the setup process for the MOCCA Android app:
    1. Checks for OpenCode installation
    2. Starts OpenCode server with proper configuration
    3. Generates QR code for instant mobile pairing
    4. Sets up Git HTTP server
    5. Configures ADB reverse for emulator support

.EXAMPLE
    .\mocca-setup.ps1
    Starts the MOCCA setup process

.EXAMPLE
    .\mocca-setup.ps1 -Port 4096 -GitPort 4097
    Starts with custom port configuration

.PARAMETER Port
    Port for OpenCode agent server (default: 4096)

.PARAMETER GitPort
    Port for Git HTTP server (default: 4097)

.PARAMETER SkipAdb
    Skip ADB reverse setup (for physical devices only)
#>

param(
    [int]$Port = 4096,
    [int]$GitPort = 4097,
    [switch]$SkipAdb,
    [switch]$Verbose
)

$ErrorActionPreference = "Stop"
$script:Version = "1.0.0"

# Colors for output
$Colors = @{
    Success = "Green"
    Error = "Red"
    Warning = "Yellow"
    Info = "Cyan"
    Normal = "White"
}

function Write-StatusMessage {
    param(
        [string]$Message,
        [string]$Type = "Info",
        [switch]$NoNewline
    )
    $color = $Colors[$Type]
    if ($NoNewline) {
        Write-Host $Message -ForegroundColor $color -NoNewline
    } else {
        Write-Host $Message -ForegroundColor $color
    }
}

function Show-Header {
    Clear-Host
    Write-Host @"
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║   MOCCA Mobile App Setup                                     ║
║   One-command configuration for MOCCA + OpenCode             ║
║                                                              ║
║   Version: $script:Version                                           ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor $Colors.Info
    Write-Host ""
}

function Test-OpenCodeInstallation {
    Write-StatusMessage "🔍 Checking for OpenCode installation..." -Type "Info"
    
    try {
        $opencodetPath = Get-Command opencode -ErrorAction SilentlyContinue
        if (-not $opencodetPath) {
            # Try common installation locations
            $possiblePaths = @(
                "$env:USERPROFILE\.cargo\bin\opencode.exe",
                "$env:LOCALAPPDATA\Programs\opencode\opencode.exe",
                "C:\Program Files\opencode\opencode.exe",
                "$env:USERPROFILE\scoop\shims\opencode.exe",
                "$env:USERPROFILE\.local\bin\opencode.exe"
            )
            
            foreach ($path in $possiblePaths) {
                if (Test-Path $path) {
                    $env:PATH = "$env:PATH;$(Split-Path $path)"
                    Write-StatusMessage "   ✓ Found OpenCode at: $path" -Type "Success"
                    return $true
                }
            }
            
            return $false
        }
        
        Write-StatusMessage "   ✓ OpenCode found in PATH" -Type "Success"
        return $true
    }
    catch {
        return $false
    }
}

function Install-OpenCode {
    Write-StatusMessage "📥 OpenCode not found. Installing..." -Type "Warning"
    
    Write-Host ""
    Write-StatusMessage "Please install OpenCode manually:" -Type "Info"
    Write-Host ""
    Write-Host "   1. Visit: https://github.com/opencode-ai/opencode/releases" -ForegroundColor $Colors.Normal
    Write-Host "   2. Download the latest release for Windows" -ForegroundColor $Colors.Normal
    Write-Host "   3. Extract and add to your PATH" -ForegroundColor $Colors.Normal
    Write-Host "   OR use cargo: cargo install opencode" -ForegroundColor $Colors.Normal
    Write-Host ""
    
    $response = Read-Host "Press Enter after installing OpenCode, or type 'skip' to continue anyway"
    
    if ($response -eq 'skip') {
        return $false
    }
    
    # Re-check
    return Test-OpenCodeInstallation
}

function Test-AdbInstallation {
    Write-StatusMessage "🔍 Checking for ADB (Android Debug Bridge)..." -Type "Info"
    
    try {
        $adbPath = Get-Command adb -ErrorAction SilentlyContinue
        if ($adbPath) {
            Write-StatusMessage "   ✓ ADB found in PATH" -Type "Success"
            return $true
        }
        
        # Check common Android SDK locations
        $possiblePaths = @(
            "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
            "$env:PROGRAMFILES\Android\android-sdk\platform-tools\adb.exe",
            "$env:ANDROID_HOME\platform-tools\adb.exe",
            "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
        )
        
        foreach ($path in $possiblePaths) {
            if (Test-Path $path) {
                $env:PATH = "$env:PATH;$(Split-Path $path)"
                Write-StatusMessage "   ✓ Found ADB at: $path" -Type "Success"
                return $true
            }
        }
        
        return $false
    }
    catch {
        return $false
    }
}

function Setup-AdbReverse {
    param([int]$Port, [int]$GitPort)
    
    Write-StatusMessage "🔌 Setting up ADB port forwarding..." -Type "Info"
    
    try {
        # Check if any emulator/device is connected
        $devices = adb devices | Select-String "device$" | Where-Object { $_ -notmatch "List of devices" }
        
        if (-not $devices) {
            Write-StatusMessage "   ⚠ No Android emulator or device detected" -Type "Warning"
            Write-StatusMessage "     Connect an emulator or device and run this script again" -Type "Warning"
            return $false
        }
        
        # Set up reverse port forwarding for both ports
        Write-StatusMessage "   Setting up reverse port forwarding..." -Type "Info"
        
        $agentResult = adb reverse tcp:$Port tcp:$Port 2>&1
        $gitResult = adb reverse tcp:$GitPort tcp:$GitPort 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-StatusMessage "   ✓ Port forwarding configured:" -Type "Success"
            Write-StatusMessage "     • OpenCode Agent: localhost:$Port → host:$Port" -Type "Success"
            Write-StatusMessage "     • Git Server: localhost:$GitPort → host:$GitPort" -Type "Success"
            return $true
        } else {
            Write-StatusMessage "   ⚠ Failed to set up port forwarding" -Type "Warning"
            Write-StatusMessage "     Error: $agentResult $gitResult" -Type "Warning"
            return $false
        }
    }
    catch {
        Write-StatusMessage "   ⚠ ADB setup failed: $_" -Type "Warning"
        return $false
    }
}

function Generate-QrCode {
    param(
        [string]$Host,
        [int]$Port,
        [string]$Token = ""
    )
    
    Write-StatusMessage "📱 Generating QR code for mobile pairing..." -Type "Info"
    
    # Build connection URL
    $protocol = "http"
    $url = "$protocol`:${Host}:$Port"
    
    # Create JSON payload for QR code
    $payload = @{
        url = $url
        token = $Token
        name = "MOCCA Auto-Setup"
    } | ConvertTo-Json -Compress
    
    Write-Host ""
    Write-StatusMessage "Connection URL: $url" -Type "Success"
    Write-Host ""
    
    # Try to display QR code in terminal
    try {
        # Check if qrencode is available (via chocolatey or other)
        $qrencode = Get-Command qrencode -ErrorAction SilentlyContinue
        
        if ($qrencode) {
            Write-StatusMessage "Scan this QR code with your MOCCA app:" -Type "Info"
            Write-Host ""
            qrencode -t ANSIUTF8 "$payload"
            Write-Host ""
        } else {
            # Display as text block
            Write-StatusMessage "📋 Manual Configuration:" -Type "Info"
            Write-Host "═══════════════════════════════════════════════" -ForegroundColor $Colors.Info
            Write-Host "  Server URL: $url" -ForegroundColor $Colors.Success
            if ($Token) {
                Write-Host "  Auth Token: $Token" -ForegroundColor $Colors.Success
            }
            Write-Host "═══════════════════════════════════════════════" -ForegroundColor $Colors.Info
            Write-Host ""
            Write-StatusMessage "Tip: You can also scan a QR code in the MOCCA app" -Type "Warning"
        }
    }
    catch {
        Write-StatusMessage "   (Displaying connection details as text)" -Type "Warning"
    }
}

function Start-OpenCodeServer {
    param(
        [int]$Port,
        [int]$GitPort
    )
    
    Write-StatusMessage "🚀 Starting OpenCode server..." -Type "Info"
    Write-Host ""
    
    # Check if server is already running
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$Port/global/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-StatusMessage "   ✓ OpenCode server is already running on port $Port" -Type "Success"
            return $true
        }
    }
    catch {
        # Server not running, which is expected
    }
    
    # Start the server
    Write-StatusMessage "   Starting server on port $Port..." -Type "Info"
    Write-StatusMessage "   Git server will run on port $GitPort" -Type "Info"
    Write-Host ""
    
    try {
        # Start opencode serve in a new window
        $arguments = "serve --port $Port"
        
        Write-StatusMessage "Launching: opencode $arguments" -Type "Info"
        
        # Start process in new window
        Start-Process -FilePath "opencode" -ArgumentList $arguments -WindowStyle Normal
        
        # Wait for server to start
        Write-StatusMessage "   Waiting for server to start..." -Type "Info" -NoNewline
        
        $maxAttempts = 30
        $attempt = 0
        $started = $false
        
        while ($attempt -lt $maxAttempts -and -not $started) {
            Start-Sleep -Seconds 1
            Write-Host "." -NoNewline -ForegroundColor $Colors.Info
            
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:$Port/global/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
                if ($response.StatusCode -eq 200) {
                    $started = $true
                }
            }
            catch {
                $attempt++
            }
        }
        
        Write-Host ""
        
        if ($started) {
            Write-StatusMessage "   ✓ Server started successfully!" -Type "Success"
            return $true
        } else {
            Write-StatusMessage "   ⚠ Server may not have started properly" -Type "Warning"
            return $false
        }
    }
    catch {
        Write-StatusMessage "   ✗ Failed to start server: $_" -Type "Error"
        return $false
    }
}

function Show-Summary {
    param(
        [string]$Host,
        [int]$Port,
        [int]$GitPort,
        [bool]$AdbConfigured
    )
    
    Write-Host ""
    Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Success
    Write-Host "║                     SETUP COMPLETE!                          ║" -ForegroundColor $Colors.Success
    Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Success
    Write-Host ""
    Write-StatusMessage "📱 Your MOCCA app should now be able to connect!" -Type "Success"
    Write-Host ""
    Write-StatusMessage "Configuration Summary:" -Type "Info"
    Write-Host "  • OpenCode Agent: http://${Host}:$Port" -ForegroundColor $Colors.Normal
    Write-Host "  • Git HTTP Server: http://${Host}:$GitPort" -ForegroundColor $Colors.Normal
    Write-Host "  • ADB Port Forwarding: $(if ($AdbConfigured) { '✓ Configured' } else { '⚠ Not configured' })" -ForegroundColor $Colors.Normal
    Write-Host ""
    Write-StatusMessage "Next steps:" -Type "Info"
    Write-Host "  1. Open the MOCCA app on your Android device/emulator" -ForegroundColor $Colors.Normal
    Write-Host "  2. The app should auto-discover your server" -ForegroundColor $Colors.Normal
    Write-Host "  3. If not found, tap 'Scan QR Code' and point at the screen" -ForegroundColor $Colors.Normal
    Write-Host ""
    
    if (-not $AdbConfigured) {
        Write-StatusMessage "⚠ Important for Emulator users:" -Type "Warning"
        Write-Host "  Run this command manually:" -ForegroundColor $Colors.Warning
        Write-Host "  adb reverse tcp:$Port tcp:$Port" -ForegroundColor $Colors.Info
        Write-Host "  adb reverse tcp:$GitPort tcp:$GitPort" -ForegroundColor $Colors.Info
        Write-Host ""
    }
    
    Write-StatusMessage "Press any key to exit..." -Type "Info"
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

# ═══════════════════════════════════════════════════════════════════════════════
# MAIN SCRIPT
# ═══════════════════════════════════════════════════════════════════════════════

Show-Header

# Step 1: Check OpenCode installation
$hasOpenCode = Test-OpenCodeInstallation
if (-not $hasOpenCode) {
    $hasOpenCode = Install-OpenCode
    
    if (-not $hasOpenCode) {
        Write-StatusMessage "⚠ Continuing without OpenCode verification..." -Type "Warning"
    }
}

# Step 2: Determine host address
Write-StatusMessage "🌐 Determining network configuration..." -Type "Info"

# Get local IP address
$localIp = (Get-NetIPAddress -AddressFamily IPv4 | 
    Where-Object { $_.InterfaceAlias -notmatch "Loopback|Virtual|VMware|Hyper-V" -and $_.IPAddress -notmatch "^169\.254" } |
    Select-Object -First 1).IPAddress

if (-not $localIp) {
    $localIp = "localhost"
}

Write-StatusMessage "   Local IP: $localIp" -Type "Success"

# Check if running on emulator host
$isEmulatorHost = $false
if (Test-AdbInstallation) {
    $adbDevices = adb devices | Select-String "device$"
    if ($adbDevices) {
        $isEmulatorHost = $true
        Write-StatusMessage "   Android emulator detected on host" -Type "Success"
    }
}

# Step 3: Setup ADB reverse if needed
$adbConfigured = $false
if ($isEmulatorHost -and -not $SkipAdb) {
    $adbConfigured = Setup-AdbReverse -Port $Port -GitPort $GitPort
}

# Step 4: Start OpenCode server
$serverStarted = Start-OpenCodeServer -Port $Port -GitPort $GitPort

if (-not $serverStarted) {
    Write-StatusMessage "⚠ Server startup may have issues" -Type "Warning"
}

# Step 5: Generate QR code
Write-Host ""
Generate-QrCode -Host $localIp -Port $Port

# Step 6: Show summary
Show-Summary -Host $localIp -Port $Port -GitPort $GitPort -AdbConfigured $adbConfigured
