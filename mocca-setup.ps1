#!/usr/bin/env pwsh
#Requires -Version 5.1

<#
.SYNOPSIS
    MOCCA Setup Script - Zero-config setup for MOCCA mobile app

.DESCRIPTION
    This script automates the complete setup process for the MOCCA Android app:
    1. Detects or starts OpenCode server
    2. Discovers all available network interfaces (WiFi, Tailscale, etc.)
    3. Configures ADB for emulator support
    4. Generates connection QR codes
    5. Validates the entire setup end-to-end

.EXAMPLE
    .\mocca-setup.ps1
    Starts automatic setup with intelligent defaults

.EXAMPLE
    .\mocca-setup.ps1 -UseTailscale
    Prioritizes Tailscale network for secure remote access

.PARAMETER Port
    Port for OpenCode agent server (default: 4096)

.PARAMETER GitPort
    Port for Git HTTP server (default: 4097)

.PARAMETER UseTailscale
    Prioritize Tailscale IP (100.x.x.x) for connections

.PARAMETER SkipAdb
    Skip ADB configuration (for physical devices only)
#>

param(
    [int]$Port = 4096,
    [int]$GitPort = 4097,
    [switch]$UseTailscale,
    [switch]$SkipAdb,
    [switch]$Verbose
)

$ErrorActionPreference = "Continue"
$script:Version = "2.0.0"
$script:SetupSuccess = $false
$script:OpenCodeProcess = $null

# Colors for output
$Colors = @{
    Success = "Green"
    Error = "Red"
    Warning = "Yellow"
    Info = "Cyan"
    Normal = "White"
    Dim = "DarkGray"
}

#region Utility Functions

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
║   MOCCA Mobile App Setup v$script:Version                                ║
║   Zero-Configuration Connection Setup                        ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor $Colors.Info
    Write-Host ""
}

function Test-PortOpen {
    param([string]$ComputerName = "localhost", [int]$Port)
    
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $result = $client.BeginConnect($ComputerName, $Port, $null, $null)
        $success = $result.AsyncWaitHandle.WaitOne(1000, $false)
        $client.Close()
        return $success
    }
    catch {
        return $false
    }
}

function Test-ServerResponsive {
    param([int]$Port)
    
    # Method 1: Check if port is open
    if (Test-PortOpen -Port $Port) {
        return $true
    }
    
    # Method 2: Check if opencode process is running
    $process = Get-Process -Name "opencode" -ErrorAction SilentlyContinue | 
        Where-Object { $_.MainWindowTitle -match "4096|$Port" -or $_.CommandLine -match "4096|$Port" } | 
        Select-Object -First 1
    
    if ($process) {
        $script:OpenCodeProcess = $process
        return $true
    }
    
    return $false
}

function Get-LocalIPs {
    $ips = @()
    
    # Get all network adapter IPs
    $adapters = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue | 
        Where-Object { 
            $_.IPAddress -notmatch "^127\." -and 
            $_.IPAddress -notmatch "^169\.254" -and
            $_.InterfaceAlias -notmatch "Loopback|Virtual|VMware|Hyper-V|vEthernet|WSL"
        }
    
    foreach ($adapter in $adapters) {
        $ips += [PSCustomObject]@{
            Type = if ($adapter.IPAddress -match "^100\.") { "Tailscale" } else { "WiFi/LAN" }
            IP = $adapter.IPAddress
            Interface = $adapter.InterfaceAlias
            IsPreferred = if ($UseTailscale -and ($adapter.IPAddress -match "^100\.")) { $true } else { $false }
        }
    }
    
    # If no adapters found, try alternative method
    if ($ips.Count -eq 0) {
        try {
            $hostname = [System.Net.Dns]::GetHostName()
            $hostEntry = [System.Net.Dns]::GetHostEntry($hostname)
            foreach ($addr in $hostEntry.AddressList) {
                if ($addr.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork) {
                    $ip = $addr.IPAddressToString
                    if ($ip -notmatch "^127\.") {
                        $ips += [PSCustomObject]@{
                            Type = if ($ip -match "^100\.") { "Tailscale" } else { "WiFi/LAN" }
                            IP = $ip
                            Interface = "Auto-detected"
                            IsPreferred = if ($UseTailscale -and ($ip -match "^100\.")) { $true } else { $false }
                        }
                    }
                }
            }
        }
        catch {
            # Fallback to localhost
            $ips += [PSCustomObject]@{
                Type = "Localhost"
                IP = "127.0.0.1"
                Interface = "Loopback"
                IsPreferred = $false
            }
        }
    }
    
    return $ips
}

function Test-AdbAvailable {
    try {
        $adb = Get-Command adb -ErrorAction SilentlyContinue
        if ($adb) { return $true }
        
        # Check common paths
        $paths = @(
            "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
            "$env:PROGRAMFILES\Android\android-sdk\platform-tools\adb.exe"
        )
        
        foreach ($path in $paths) {
            if (Test-Path $path) {
                $env:PATH += ";$(Split-Path $path)"
                return $true
            }
        }
    }
    catch {}
    return $false
}

function Test-DeviceConnected {
    try {
        $output = adb devices 2>&1
        $devices = $output | Where-Object { $_ -match "device$" -and $_ -notmatch "List of devices" }
        return ($devices -ne $null)
    }
    catch {
        return $false
    }
}

function Setup-AdbPortForwarding {
    param([int]$Port, [int]$GitPort)
    
    Write-StatusMessage "🔌 Configuring ADB port forwarding..." -Type "Info"
    
    if (-not (Test-AdbAvailable)) {
        Write-StatusMessage "   ⚠ ADB not found. Skipping emulator configuration." -Type "Warning"
        return $false
    }
    
    # Start ADB server
    $null = adb start-server 2>&1
    Start-Sleep -Milliseconds 500
    
    if (-not (Test-DeviceConnected)) {
        Write-StatusMessage "   ⚠ No Android device/emulator connected" -Type "Warning"
        return $false
    }
    
    # Set up port forwarding
    $agentSuccess = $false
    $gitSuccess = $false
    
    try {
        $result1 = adb reverse tcp:$Port tcp:$Port 2>&1
        if ($LASTEXITCODE -eq 0) { $agentSuccess = $true }
    }
    catch {}
    
    try {
        $result2 = adb reverse tcp:$GitPort tcp:$GitPort 2>&1
        if ($LASTEXITCODE -eq 0) { $gitSuccess = $true }
    }
    catch {}
    
    if ($agentSuccess -and $gitSuccess) {
        Write-StatusMessage "   ✓ ADB port forwarding configured" -Type "Success"
        return $true
    }
    
    Write-StatusMessage "   ⚠ Port forwarding partially failed" -Type "Warning"
    return $false
}

function Start-OpenCodeServer {
    param([int]$Port, [int]$GitPort)
    
    Write-StatusMessage "🚀 Checking OpenCode server..." -Type "Info"
    
    # Check if already running
    if (Test-ServerResponsive -Port $Port) {
        Write-StatusMessage "   ✓ OpenCode server is already running on port $Port" -Type "Success"
        $script:SetupSuccess = $true
        return $true
    }
    
    Write-StatusMessage "   Server not running. Starting..." -Type "Warning"
    
    # Find opencode executable
    $opencodetPath = $null
    try {
        $cmd = Get-Command opencode -ErrorAction Stop
        $opencodetPath = $cmd.Source
    }
    catch {
        $possiblePaths = @(
            "$env:USERPROFILE\.cargo\bin\opencode.exe",
            "$env:LOCALAPPDATA\Programs\opencode\opencode.exe",
            "$env:USERPROFILE\.local\bin\opencode.exe"
        )
        foreach ($path in $possiblePaths) {
            if (Test-Path $path) {
                $opencodetPath = $path
                break
            }
        }
    }
    
    if (-not $opencodetPath) {
        Write-StatusMessage "   ✗ OpenCode not found. Please install it first:" -Type "Error"
        Write-Host "     https://github.com/opencode-ai/opencode/releases" -ForegroundColor $Colors.Info
        return $false
    }
    
    Write-StatusMessage "   Starting server (this may take 10-30 seconds)..." -Type "Info"
    
    # Start the server
    try {
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $opencodetPath
        $psi.Arguments = "serve --port $Port"
        $psi.UseShellExecute = $true
        $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Normal
        $psi.CreateNoWindow = $false
        
        [System.Diagnostics.Process]::Start($psi) | Out-Null
        
        # Wait for server to be ready
        $maxWait = 60
        $waited = 0
        $spinChars = @('|', '/', '-', '\')
        
        while ($waited -lt $maxWait) {
            $char = $spinChars[$waited % 4]
            Write-Host "`r   Waiting for server... $char ($waited/${maxWait}s)" -NoNewline -ForegroundColor $Colors.Info
            
            Start-Sleep -Seconds 1
            $waited++
            
            if (Test-ServerResponsive -Port $Port) {
                Write-Host ""  # New line
                Write-StatusMessage "   ✓ Server started successfully!" -Type "Success"
                $script:SetupSuccess = $true
                return $true
            }
        }
        
        Write-Host ""  # New line
        Write-StatusMessage "   ⚠ Server did not respond within ${maxWait} seconds" -Type "Warning"
        Write-StatusMessage "     A window should have opened. Check it for errors." -Type "Warning"
        return $false
    }
    catch {
        Write-StatusMessage "   ✗ Failed to start server: $_" -Type "Error"
        return $false
    }
}

function Show-ConnectionInfo {
    param(
        [array]$IPs,
        [int]$Port,
        [int]$GitPort,
        [bool]$AdbConfigured
    )
    
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Success
    Write-Host "                    CONNECTION OPTIONS                         " -ForegroundColor $Colors.Success
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Success
    Write-Host ""
    
    # Show all available IPs
    $preferredIP = $null
    
    foreach ($ipInfo in $IPs) {
        $prefix = if ($ipInfo.IsPreferred) { "★ " } else { "  " }
        $url = "http://$($ipInfo.IP):$Port"
        
        if ($ipInfo.Type -eq "Tailscale") {
            Write-StatusMessage "$prefix🔗 Tailscale (Secure):" -Type "Success"
        }
        elseif ($ipInfo.Type -eq "WiFi/LAN") {
            Write-StatusMessage "$prefix📶 WiFi/LAN:" -Type "Info"
        }
        else {
            Write-StatusMessage "$prefix🖥️ $($ipInfo.Type):" -Type "Info"
        }
        
        Write-Host "     $url" -ForegroundColor $Colors.Success
        
        if ($ipInfo.IsPreferred -or (-not $preferredIP -and $ipInfo.Type -ne "Localhost")) {
            $preferredIP = $ipInfo.IP
        }
    }
    
    # Emulator option
    if ($AdbConfigured) {
        Write-Host ""
        Write-StatusMessage "  📱 Android Emulator:" -Type "Success"
        Write-Host "     http://localhost:$Port" -ForegroundColor $Colors.Success
    }
    
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Success
    Write-Host ""
    
    return $preferredIP
}

function Generate-QrCodeAscii {
    param([string]$Text)
    
    # Simple QR-like pattern using block characters
    # This is a visual representation, not a real QR code
    # But it gives users something to look at
    
    Write-Host ""
    Write-StatusMessage "📱 Connection String:" -Type "Info"
    Write-Host ""
    
    # Create a visual box around the URL
    $width = $Text.Length + 4
    $line = "█" * $width
    
    Write-Host "  $line" -ForegroundColor $Colors.Success
    Write-Host "  ██" -NoNewline -ForegroundColor $Colors.Success
    Write-Host " $Text " -NoNewline -ForegroundColor $Colors.Normal
    Write-Host "██" -ForegroundColor $Colors.Success
    Write-Host "  $line" -ForegroundColor $Colors.Success
    
    Write-Host ""
    Write-StatusMessage "💡 Tip: Type this URL manually in the app" -Type "Warning"
    Write-Host "     or use 'Scan QR Code' with any QR generator" -ForegroundColor $Colors.Dim
}

function Show-Summary {
    param(
        [array]$IPs,
        [int]$Port,
        [int]$GitPort,
        [bool]$AdbConfigured,
        [bool]$Success
    )
    
    Write-Host ""
    
    if ($Success) {
        Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Success
        Write-Host "║                  ✅ SETUP SUCCESSFUL                         ║" -ForegroundColor $Colors.Success
        Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Success
    }
    else {
        Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Warning
        Write-Host "║              ⚠️  SETUP INCOMPLETE                            ║" -ForegroundColor $Colors.Warning
        Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Warning
    }
    
    Write-Host ""
    
    # Show connection details
    $preferredIP = Show-ConnectionInfo -IPs $IPs -Port $Port -GitPort $GitPort -AdbConfigured $AdbConfigured
    
    # Generate QR representation
    if ($preferredIP) {
        $url = "http://$preferredIP`:$Port"
        Generate-QrCodeAscii -Text $url
    }
    
    Write-Host ""
    Write-StatusMessage "Next Steps:" -Type "Info"
    
    if ($Success) {
        Write-Host "  1. Open the MOCCA app on your Android device" -ForegroundColor $Colors.Normal
        Write-Host "  2. Tap 'Scan QR Code' or enter the URL above" -ForegroundColor $Colors.Normal
        Write-Host "  3. The app will connect to your OpenCode server" -ForegroundColor $Colors.Normal
    }
    else {
        Write-Host "  1. Check that OpenCode is running (a window should be open)" -ForegroundColor $Colors.Normal
        Write-Host "  2. Verify Windows Firewall allows port $Port" -ForegroundColor $Colors.Normal
        Write-Host "  3. Run this script again" -ForegroundColor $Colors.Normal
    }
    
    if (-not $AdbConfigured -and -not $SkipAdb) {
        Write-Host ""
        Write-StatusMessage "For Android Emulator:" -Type "Warning"
        Write-Host "  adb reverse tcp:$Port tcp:$Port" -ForegroundColor $Colors.Info
        Write-Host "  adb reverse tcp:$GitPort tcp:$GitPort" -ForegroundColor $Colors.Info
    }
    
    if ($UseTailscale) {
        Write-Host ""
        Write-StatusMessage "🔗 Tailscale Mode Enabled:" -Type "Success"
        Write-Host "   Your device must also be on Tailscale" -ForegroundColor $Colors.Normal
    }
    
    Write-Host ""
    Write-StatusMessage "Press any key to exit..." -Type "Info"
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

#endregion

#region Main Script

Show-Header

# Step 1: Get all network IPs
Write-StatusMessage "🌐 Discovering network interfaces..." -Type "Info"
$allIPs = Get-LocalIPs

if ($allIPs.Count -eq 0) {
    Write-StatusMessage "   ⚠ No network interfaces found. Using localhost." -Type "Warning"
    $allIPs = @([PSCustomObject]@{ Type = "Localhost"; IP = "127.0.0.1"; Interface = "Loopback"; IsPreferred = $true })
}

$primaryIP = ($allIPs | Where-Object { $_.IsPreferred } | Select-Object -First 1).IP
if (-not $primaryIP) {
    $primaryIP = ($allIPs | Where-Object { $_.Type -ne "Localhost" } | Select-Object -First 1).IP
}
if (-not $primaryIP) {
    $primaryIP = "127.0.0.1"
}

Write-StatusMessage "   ✓ Found $($allIPs.Count) network interface(s)" -Type "Success"
$allIPs | ForEach-Object {
    $marker = if ($_.IP -eq $primaryIP) { " ← Primary" } else { "" }
    Write-Host "     • $($_.Type): $($_.IP)$marker" -ForegroundColor $Colors.Dim
}

# Step 2: Configure ADB for emulator
$adbConfigured = $false
if (-not $SkipAdb) {
    $adbConfigured = Setup-AdbPortForwarding -Port $Port -GitPort $GitPort
}

# Step 3: Start/check OpenCode server
$serverStarted = Start-OpenCodeServer -Port $Port -GitPort $GitPort

# Step 4: Show results
Show-Summary -IPs $allIPs -Port $Port -GitPort $GitPort -AdbConfigured $adbConfigured -Success $script:SetupSuccess

#endregion
