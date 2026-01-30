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

function Test-AdbServer {
    Write-StatusMessage "🔌 Checking ADB server status..." -Type "Info"
    
    try {
        # Try to start ADB server if not running
        $null = adb start-server 2>&1
        
        # Give it a moment to start
        Start-Sleep -Milliseconds 500
        
        # Check if server is responding
        $devices = adb devices 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            $connectedDevices = $devices | Select-String "device$" | Where-Object { $_ -notmatch "List of devices" }
            if ($connectedDevices) {
                Write-StatusMessage "   ✓ ADB server running with connected devices" -Type "Success"
                return $true
            } else {
                Write-StatusMessage "   ⚠ ADB server running but no devices connected" -Type "Warning"
                return $false
            }
        } else {
            Write-StatusMessage "   ⚠ ADB server not responding properly" -Type "Warning"
            return $false
        }
    }
    catch {
        Write-StatusMessage "   ⚠ ADB check failed: $_" -Type "Warning"
        return $false
    }
}

function Setup-AdbReverse {
    param([int]$Port, [int]$GitPort)
    
    Write-StatusMessage "🔌 Setting up ADB port forwarding..." -Type "Info"
    
    try {
        # Ensure ADB server is running first
        $null = adb start-server 2>&1
        Start-Sleep -Milliseconds 1000
        
        # Check if any emulator/device is connected
        $devicesOutput = adb devices 2>&1
        $devices = $devicesOutput | Select-String "device$" | Where-Object { $_ -notmatch "List of devices" }
        
        if (-not $devices) {
            Write-StatusMessage "   ⚠ No Android emulator or device detected" -Type "Warning"
            Write-StatusMessage "     Skipping ADB port forwarding" -Type "Warning"
            Write-Host ""
            Write-StatusMessage "💡 For physical devices:" -Type "Info"
            Write-Host "     Use the IP address shown below for connection" -ForegroundColor $Colors.Normal
            return $false
        }
        
        # Set up reverse port forwarding for both ports
        Write-StatusMessage "   Setting up reverse port forwarding..." -Type "Info"
        
        $agentResult = adb reverse tcp:$Port tcp:$Port 2>&1
        $agentSuccess = $LASTEXITCODE -eq 0
        
        $gitResult = adb reverse tcp:$GitPort tcp:$GitPort 2>&1
        $gitSuccess = $LASTEXITCODE -eq 0
        
        if ($agentSuccess -and $gitSuccess) {
            Write-StatusMessage "   ✓ Port forwarding configured:" -Type "Success"
            Write-StatusMessage "     • OpenCode Agent: localhost:$Port → host:$Port" -Type "Success"
            Write-StatusMessage "     • Git Server: localhost:$GitPort → host:$GitPort" -Type "Success"
            Write-Host ""
            Write-StatusMessage "💡 Emulator users:" -Type "Info"
            Write-Host "     Use 'localhost' or '127.0.0.1' in the app" -ForegroundColor $Colors.Normal
            return $true
        } else {
            if (-not $agentSuccess) {
                Write-StatusMessage "   ⚠ Failed to forward agent port: $agentResult" -Type "Warning"
            }
            if (-not $gitSuccess) {
                Write-StatusMessage "   ⚠ Failed to forward git port: $gitResult" -Type "Warning"
            }
            Write-Host ""
            Write-StatusMessage "💡 Try running these commands manually:" -Type "Info"
            Write-Host "     adb reverse tcp:$Port tcp:$Port" -ForegroundColor $Colors.Info
            Write-Host "     adb reverse tcp:$GitPort tcp:$GitPort" -ForegroundColor $Colors.Info
            return $false
        }
    }
    catch {
        Write-StatusMessage "   ⚠ ADB setup failed: $_" -Type "Warning"
        Write-Host ""
        Write-StatusMessage "💡 You can skip ADB setup and use IP address instead:" -Type "Info"
        return $false
    }
}

function Generate-QrCode {
    param(
        [string]$ServerHost,
        [int]$Port,
        [string]$Token = "",
        [bool]$AdbConfigured = $false
    )
    
    Write-StatusMessage "📱 Generating connection information..." -Type "Info"
    
    # Build connection URLs for different scenarios
    $protocol = "http"
    
    # Primary URL (IP address - works for physical devices)
    $ipUrl = "$protocol`:${ServerHost}:$Port"
    
    # Localhost URL (for emulator when ADB is configured)
    $localhostUrl = "$protocol`://localhost:$Port"
    $localIpUrl = "$protocol`://127.0.0.1:$Port"
    
    # Create JSON payload for QR code (prioritize IP for broadest compatibility)
    $payload = @{
        url = $ipUrl
        token = $Token
        name = "MOCCA Auto-Setup"
        alt_urls = @($localhostUrl, $localIpUrl)
    } | ConvertTo-Json -Compress
    
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Info
    Write-Host "                    CONNECTION OPTIONS                         " -ForegroundColor $Colors.Info
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Info
    Write-Host ""
    
    # Show all connection options
    Write-StatusMessage "📍 For Physical Device (same WiFi network):" -Type "Success"
    Write-Host "   $ipUrl" -ForegroundColor $Colors.Success
    Write-Host ""
    
    if ($AdbConfigured) {
        Write-StatusMessage "📱 For Android Emulator (via ADB):" -Type "Success"
        Write-Host "   $localhostUrl" -ForegroundColor $Colors.Success
        Write-Host "   or" -ForegroundColor $Colors.Normal
        Write-Host "   $localIpUrl" -ForegroundColor $Colors.Success
        Write-Host ""
    } else {
        Write-StatusMessage "⚠ For Android Emulator:" -Type "Warning"
        Write-Host "   ADB port forwarding not configured" -ForegroundColor $Colors.Warning
        Write-Host "   Run these commands manually:" -ForegroundColor $Colors.Warning
        Write-Host "     adb reverse tcp:$Port tcp:$Port" -ForegroundColor $Colors.Info
        Write-Host "     adb reverse tcp:4097 tcp:4097" -ForegroundColor $Colors.Info
        Write-Host "   Then use: $localhostUrl" -ForegroundColor $Colors.Info
        Write-Host ""
    }
    
    # Try to display QR code in terminal
    try {
        # Check if qrencode is available (via chocolatey or other)
        $qrencode = Get-Command qrencode -ErrorAction SilentlyContinue
        
        if ($qrencode) {
            Write-StatusMessage "📱 Scan this QR code with your MOCCA app:" -Type "Info"
            Write-Host ""
            qrencode -t ANSIUTF8 "$payload" 2>$null
            Write-Host ""
            Write-StatusMessage "   (QR contains: $ipUrl)" -Type "Info"
        } else {
            Write-StatusMessage "💡 Tip: Install qrencode for QR code display:" -Type "Info"
            Write-Host "   choco install qrencode" -ForegroundColor $Colors.Info
            Write-Host "   or" -ForegroundColor $Colors.Normal
            Write-Host "   scoop install qrencode" -ForegroundColor $Colors.Info
        }
    }
    catch {
        # Silently continue if QR generation fails
    }
    
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Info
}

function Test-ServerHealth {
    param(
        [string]$ServerUrl,
        [int]$TimeoutSec = 3
    )
    
    try {
        # Try multiple health endpoints (OpenCode might use different paths)
        $endpoints = @(
            "/health",
            "/api/health",
            "/global/health",
            "/"
        )
        
        foreach ($endpoint in $endpoints) {
            try {
                $response = Invoke-WebRequest -Uri "$ServerUrl$endpoint" -TimeoutSec $TimeoutSec -ErrorAction Stop
                if ($response.StatusCode -eq 200) {
                    return $true
                }
            }
            catch {
                # Try next endpoint
                continue
            }
        }
        
        return $false
    }
    catch {
        return $false
    }
}

function Start-OpenCodeServer {
    param(
        [int]$Port,
        [int]$GitPort
    )
    
    Write-StatusMessage "🚀 Starting OpenCode server..." -Type "Info"
    Write-Host ""
    
    $serverUrl = "http://localhost:$Port"
    
    # Check if server is already running
    Write-StatusMessage "   Checking if server already running..." -Type "Info"
    if (Test-ServerHealth -ServerUrl $serverUrl) {
        Write-StatusMessage "   ✓ OpenCode server is already running on port $Port" -Type "Success"
        return $true
    }
    
    # Start the server
    Write-StatusMessage "   Starting server on port $Port..." -Type "Info"
    Write-StatusMessage "   Git server will run on port $GitPort" -Type "Info"
    Write-Host ""
    
    try {
        # Try different ways to start opencode
        $processStarted = $false
        
        # Method 1: Try 'opencode' directly
        try {
            $opencodetCmd = Get-Command opencode -ErrorAction Stop
            $arguments = "serve --port $Port"
            
            Write-StatusMessage "Launching: opencode $arguments" -Type "Info"
            
            # Start process (hidden window to avoid popup)
            $psi = New-Object System.Diagnostics.ProcessStartInfo
            $psi.FileName = "opencode"
            $psi.Arguments = $arguments
            $psi.UseShellExecute = $true
            $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Normal
            
            [System.Diagnostics.Process]::Start($psi) | Out-Null
            $processStarted = $true
        }
        catch {
            Write-StatusMessage "   Could not start 'opencode' directly, trying alternative methods..." -Type "Warning"
        }
        
        if (-not $processStarted) {
            # Method 2: Check if server binary exists in common locations
            $possiblePaths = @(
                "$env:USERPROFILE\.cargo\bin\opencode.exe",
                "$env:LOCALAPPDATA\Programs\opencode\opencode.exe",
                "$env:USERPROFILE\.local\bin\opencode.exe"
            )
            
            foreach ($path in $possiblePaths) {
                if (Test-Path $path) {
                    $psi = New-Object System.Diagnostics.ProcessStartInfo
                    $psi.FileName = $path
                    $psi.Arguments = "serve --port $Port"
                    $psi.UseShellExecute = $true
                    $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Normal
                    
                    [System.Diagnostics.Process]::Start($psi) | Out-Null
                    $processStarted = $true
                    Write-StatusMessage "   Started from: $path" -Type "Info"
                    break
                }
            }
        }
        
        if (-not $processStarted) {
            Write-StatusMessage "   ✗ Could not start OpenCode server automatically" -Type "Error"
            Write-Host ""
            Write-StatusMessage "💡 Please start it manually:" -Type "Info"
            Write-Host "     opencode serve --port $Port" -ForegroundColor $Colors.Info
            Write-Host ""
            return $false
        }
        
        # Wait for server to start with progress feedback
        Write-StatusMessage "   Waiting for server to start (this may take 10-30 seconds)..." -Type "Info" -NoNewline
        
        $maxAttempts = 45  # Increased from 30
        $attempt = 0
        $started = $false
        $progressChars = @('/', '-', '\', '|')
        
        while ($attempt -lt $maxAttempts -and -not $started) {
            Start-Sleep -Milliseconds 1000
            $char = $progressChars[$attempt % $progressChars.Length]
            Write-Host "`r   Waiting for server to start... $char (attempt $($attempt + 1)/$maxAttempts)" -NoNewline -ForegroundColor $Colors.Info
            
            if (Test-ServerHealth -ServerUrl $serverUrl -TimeoutSec 2) {
                $started = $true
            }
            
            $attempt++
        }
        
        Write-Host ""  # New line after progress
        
        if ($started) {
            Write-StatusMessage "   ✓ Server started successfully!" -Type "Success"
            return $true
        } else {
            Write-StatusMessage "   ⚠ Server did not respond to health checks" -Type "Warning"
            Write-Host ""
            Write-StatusMessage "💡 The server may still be starting. Common issues:" -Type "Info"
            Write-Host "     1. First-time startup takes longer (downloading models, etc.)" -ForegroundColor $Colors.Normal
            Write-Host "     2. Check the OpenCode window that opened for errors" -ForegroundColor $Colors.Normal
            Write-Host "     3. Port $Port might be in use by another application" -ForegroundColor $Colors.Normal
            Write-Host ""
            Write-StatusMessage "💡 You can check if it's running:" -Type "Info"
            Write-Host "     curl http://localhost:$Port/health" -ForegroundColor $Colors.Info
            return $false
        }
    }
    catch {
        Write-StatusMessage "   ✗ Failed to start server: $_" -Type "Error"
        Write-Host ""
        Write-StatusMessage "💡 Please start manually:" -Type "Info"
        Write-Host "     opencode serve --port $Port" -ForegroundColor $Colors.Info
        return $false
    }
}

function Show-Summary {
    param(
        [string]$ServerHost,
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
    Write-Host "  • OpenCode Agent: http://${ServerHost}:$Port" -ForegroundColor $Colors.Normal
    Write-Host "  • Git HTTP Server: http://${ServerHost}:$GitPort" -ForegroundColor $Colors.Normal
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
    # Use the improved ADB check
    if (Test-AdbServer) {
        $isEmulatorHost = $true
        Write-StatusMessage "   Android emulator/device detected on host" -Type "Success"
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
Generate-QrCode -ServerHost $localIp -Port $Port -AdbConfigured $adbConfigured

# Step 6: Show summary
Show-Summary -ServerHost $localIp -Port $Port -GitPort $GitPort -AdbConfigured $adbConfigured
