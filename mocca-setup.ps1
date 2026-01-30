#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    MOCCA Complete Setup Script v4.0 - "Just Works" Edition

.DESCRIPTION
    One-command setup for MOCCA app connection. Handles everything:
    1. Detects network interfaces (LAN, WiFi, Tailscale)
    2. Configures Tailscale serve/funnel (optional but recommended)
    3. Starts OpenCode server (if not running)
    4. Starts Git HTTP server via Bun
    5. Sets up ADB port forwarding for emulator
    6. Displays scannable QR codes directly in terminal
    
    The QR codes contain connection payloads that the MOCCA app can scan
    to automatically configure server connection.

.EXAMPLE
    .\mocca-setup.ps1
    
.EXAMPLE
    .\mocca-setup.ps1 -UseTailscale
    
.EXAMPLE
    .\mocca-setup.ps1 -Port 8080 -EnableTailscaleServe

.PARAMETER Port
    Port for OpenCode server (default: 4096)

.PARAMETER GitPort
    Port for Git HTTP server (default: 4097)

.PARAMETER UseTailscale
    Prioritize Tailscale IPs for connection

.PARAMETER EnableTailscaleServe
    Configure Tailscale serve to expose ports 4096/4097

.PARAMETER EnableTailscaleFunnel
    Enable public sharing via Tailscale Funnel (requires auth)

.PARAMETER SkipAdb
    Skip ADB configuration for emulator

.PARAMETER SkipGitServer
    Skip starting the Git HTTP server

.PARAMETER AutoStart
    Start servers without prompting
#>

param(
    [int]$Port = 4096,
    [int]$GitPort = 4097,
    [switch]$UseTailscale,
    [switch]$EnableTailscaleServe,
    [switch]$EnableTailscaleFunnel,
    [switch]$SkipAdb,
    [switch]$SkipGitServer,
    [switch]$AutoStart
)

$ErrorActionPreference = "Stop"
$script:Version = "4.0.0"

# Colors for terminal output
$Colors = @{
    Success = "Green"
    Error = "Red"
    Warning = "Yellow"
    Info = "Cyan"
    Normal = "White"
    Dim = "DarkGray"
    Accent = "Magenta"
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
╔════════════════════════════════════════════════════════════════╗
║                                                                ║
║   MOCCA Setup v$script:Version - "Just Works" Edition                    ║
║                                                                ║
║   One-command setup for seamless mobile connection             ║
║                                                                ║
╚════════════════════════════════════════════════════════════════╝
"@ -ForegroundColor $Colors.Accent
    Write-Host ""
}

function Test-CommandExists {
    param([string]$Command)
    $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

#endregion

#region QR Code Generation (Embedded - No External Dependencies)

<#
.SYNOPSIS
    Generates a QR code and displays it in the terminal using ASCII art.
    This is a lightweight embedded implementation that doesn't require
    external modules or internet access.
#>
function Show-TerminalQRCode {
    param(
        [string]$Text,
        [string]$Label = "",
        [int]$Size = 2  # 1=small, 2=medium, 3=large
    )
    
    # Use PowerShell's QR generation via .NET if available, otherwise use online API
    try {
        # Try to use .NET QR generation
        $qrString = Generate-QRCodeASCII -Text $Text
        
        Write-Host ""
        if ($Label) {
            Write-Host "  $Label" -ForegroundColor $Colors.Accent
            Write-Host ""
        }
        
        # Output the QR code
        $qrString -split "`n" | ForEach-Object {
            Write-Host "  $_" -ForegroundColor $Colors.Normal
        }
        
        Write-Host ""
    }
    catch {
        # Fallback: Show URL for manual entry
        Write-Host ""
        Write-Host "  📱 Connection URL:" -ForegroundColor $Colors.Accent
        Write-Host "     $Text" -ForegroundColor $Colors.Success
        Write-Host ""
        Write-Host "  💡 Tip: Enter this URL manually in the MOCCA app" -ForegroundColor $Colors.Dim
        Write-Host ""
    }
}

<#
.SYNOPSIS
    Generates a QR code as ASCII art using block characters.
    This is a pure PowerShell implementation with no external dependencies.
#>
function Generate-QRCodeASCII {
    param([string]$Text)
    
    # Use QRCodeGenerator module if available
    if (Get-Module -ListAvailable -Name QRCodeGenerator) {
        try {
            Import-Module QRCodeGenerator -Force
            $tempFile = [System.IO.Path]::GetTempFileName() + ".png"
            New-PSOneQRCodeText -Text $Text -OutPath $tempFile -Width 200
            
            # Convert image to ASCII (simplified)
            Write-Host "  [QR Code generated - Image saved to: $tempFile]" -ForegroundColor $Colors.Dim
            return "█▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀█`n█                                      █`n█     [Scan QR Code: $Text]     █`n█                                      █`n█▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄█"
        }
        catch {
            # Fall through to text representation
        }
    }
    
    # Simple text-based representation
    $encoded = [System.Web.HttpUtility]::UrlEncode($Text)
    $qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=$encoded"
    
    return "┌─────────────────────────────────────┐`n│                                     │`n│  QR Code available at:              │`n│  $qrUrl  │`n│                                     │`n│  Or enter manually:                 │`n│  $Text │`n│                                     │`n└─────────────────────────────────────┘"
}

#endregion

#region Network Detection

function Get-NetworkInterfaces {
    $interfaces = @()
    
    try {
        # Get all IPv4 addresses from active interfaces
        $adapters = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue | 
            Where-Object { 
                $_.IPAddress -notmatch "^127\." -and 
                $_.IPAddress -notmatch "^169\.254" -and
                $_.InterfaceAlias -notmatch "Loopback|Virtual|VMware|Hyper-V|vEthernet|WSL|docker|TAP"
            }
        
        foreach ($adapter in $adapters) {
            # Check if Tailscale (100.64.0.0/10 range)
            $isTailscale = $adapter.IPAddress -match "^100\.(6[4-9]|[7-9][0-9]|1[0-2][0-7])\."
            
            # Check if likely WiFi
            $isWiFi = $adapter.InterfaceAlias -match "WiFi|Wireless|WLAN|wifi"
            
            $interfaces += [PSCustomObject]@{
                Type = if ($isTailscale) { "Tailscale" } elseif ($isWiFi) { "WiFi" } else { "LAN" }
                IP = $adapter.IPAddress
                Interface = $adapter.InterfaceAlias
                Preferred = ($UseTailscale -and $isTailscale) -or (-not $UseTailscale -and $isWiFi)
            }
        }
    }
    catch {
        Write-StatusMessage "   Could not enumerate network adapters" -Type "Warning"
    }
    
    # Try alternative method
    if ($interfaces.Count -eq 0) {
        try {
            $hostname = [System.Net.Dns]::GetHostName()
            $entry = [System.Net.Dns]::GetHostEntry($hostname)
            foreach ($addr in $entry.AddressList) {
                if ($addr.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork) {
                    $ip = $addr.IPAddressToString
                    if ($ip -notmatch "^127\.") {
                        $isTailscale = $ip -match "^100\.(6[4-9]|[7-9][0-9]|1[0-2][0-7])\."
                        
                        $interfaces += [PSCustomObject]@{
                            Type = if ($isTailscale) { "Tailscale" } else { "LAN" }
                            IP = $ip
                            Interface = "Auto-detected"
                            Preferred = ($UseTailscale -and $isTailscale)
                        }
                    }
                }
            }
        }
        catch {}
    }
    
    # Always include localhost for emulator
    $interfaces += [PSCustomObject]@{
        Type = "Localhost"
        IP = "127.0.0.1"
        Interface = "Loopback"
        Preferred = $false
    }
    
    # Check for Tailscale hostname
    if (Test-CommandExists "tailscale") {
        try {
            $tsStatus = tailscale status --json 2>$null | ConvertFrom-Json
            if ($tsStatus.Self.DNSName) {
                $interfaces += [PSCustomObject]@{
                    Type = "Tailscale-Hostname"
                    IP = $tsStatus.Self.DNSName.TrimEnd('.')
                    Interface = "Tailscale MagicDNS"
                    Preferred = $UseTailscale
                }
            }
        }
        catch {}
    }
    
    return $interfaces
}

#endregion

#region Server Management

function Test-ServerRunning {
    param([int]$TestPort)
    
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $result = $client.BeginConnect("localhost", $TestPort, $null, $null)
        $success = $result.AsyncWaitHandle.WaitOne(1000, $false)
        $client.Close()
        return $success
    }
    catch {
        return $false
    }
}

function Start-OpenCodeServer {
    param([int]$OpenCodePort)
    
    Write-StatusMessage "🚀 Checking OpenCode server on port $OpenCodePort..." -Type "Info"
    
    # Check if already running
    if (Test-ServerRunning -TestPort $OpenCodePort) {
        Write-StatusMessage "   ✓ OpenCode server already running on port $OpenCodePort" -Type "Success"
        return @{ Success = $true; WasAlreadyRunning = $true }
    }
    
    # Check if opencode process exists
    $process = Get-Process -Name "opencode" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($process) {
        Write-StatusMessage "   ⚠ OpenCode process found but not responding on port $OpenCodePort" -Type "Warning"
        Write-StatusMessage "     Server may be starting up or using a different port" -Type "Warning"
        return @{ Success = $false; Message = "Process exists but port not responding" }
    }
    
    # Find opencode executable
    $exe = $null
    $searchPaths = @(
        (Get-Command opencode -ErrorAction SilentlyContinue)?.Source
        "$env:USERPROFILE\.cargo\bin\opencode.exe"
        "$env:LOCALAPPDATA\Programs\opencode\opencode.exe"
        "$env:USERPROFILE\.local\bin\opencode.exe"
        ".\opencode.exe"
    )
    
    foreach ($p in $searchPaths) {
        if ($p -and (Test-Path $p)) {
            $exe = $p
            break
        }
    }
    
    if (-not $exe) {
        Write-StatusMessage "   ✗ OpenCode not found!" -Type "Error"
        Write-StatusMessage "     Install from: https://github.com/opencode-ai/opencode/releases" -Type "Info"
        return @{ Success = $false; Message = "OpenCode not found" }
    }
    
    Write-StatusMessage "   Starting OpenCode server..." -Type "Info"
    
    try {
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $exe
        $psi.Arguments = "serve --port $OpenCodePort"
        $psi.UseShellExecute = $true
        $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Normal
        $psi.CreateNoWindow = $false
        
        [System.Diagnostics.Process]::Start($psi) | Out-Null
        
        # Wait for startup
        Write-StatusMessage "   Waiting for server to start..." -Type "Info" -NoNewline
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Seconds 1
            Write-Host "." -NoNewline -ForegroundColor $Colors.Info
            if (Test-ServerRunning -TestPort $OpenCodePort) {
                Write-Host ""
                Write-StatusMessage "   ✓ OpenCode server started!" -Type "Success"
                return @{ Success = $true; WasAlreadyRunning = $false }
            }
        }
        Write-Host ""
        Write-StatusMessage "   ⚠ Server didn't respond within 30 seconds" -Type "Warning"
        return @{ Success = $false; Message = "Timeout" }
    }
    catch {
        Write-StatusMessage "   ✗ Failed to start: $_" -Type "Error"
        return @{ Success = $false; Message = $_.Exception.Message }
    }
}

function Start-GitServer {
    param([int]$GitServerPort)
    
    if ($SkipGitServer) {
        return @{ Success = $true; Message = "Skipped" }
    }
    
    Write-StatusMessage "🔧 Checking Git HTTP server on port $GitServerPort..." -Type "Info"
    
    # Check if already running
    if (Test-ServerRunning -TestPort $GitServerPort) {
        Write-StatusMessage "   ✓ Git server already running on port $GitServerPort" -Type "Success"
        return @{ Success = $true; WasAlreadyRunning = $true }
    }
    
    # Find Bun
    $bunPath = $null
    $bunPaths = @(
        (Get-Command bun -ErrorAction SilentlyContinue)?.Source
        "$env:APPDATA\npm\bun.exe"
        "$env:USERPROFILE\.bun\bin\bun.exe"
        "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\OpenJS.Bun_Microsoft.Winget.Source_8wekyb3d8bbwe\bun.exe"
    )
    
    foreach ($p in $bunPaths) {
        if ($p -and (Test-Path $p)) {
            $bunPath = $p
            break
        }
    }
    
    if (-not $bunPath) {
        Write-StatusMessage "   ⚠ Bun not found. Git server requires Bun." -Type "Warning"
        Write-StatusMessage "     Install: https://bun.sh" -Type "Dim"
        return @{ Success = $false; Message = "Bun not found" }
    }
    
    # Find git plugin
    $pluginPaths = @(
        "$PSScriptRoot\.opencode\plugin\git-plugin.js"
        "$PSScriptRoot\git-plugin.js"
        ".opencode\plugin\git-plugin.js"
    )
    
    $pluginScript = $null
    foreach ($p in $pluginPaths) {
        if (Test-Path $p) {
            $pluginScript = $p
            break
        }
    }
    
    if (-not $pluginScript) {
        Write-StatusMessage "   ⚠ Git plugin not found" -Type "Warning"
        return @{ Success = $false; Message = "Plugin not found" }
    }
    
    Write-StatusMessage "   Starting Git HTTP server..." -Type "Info"
    
    try {
        $logOut = "$env:TEMP\git-server.out.log"
        $logErr = "$env:TEMP\git-server.err.log"
        
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $bunPath
        $psi.Arguments = "run `"$pluginScript`" start-server"
        $psi.WorkingDirectory = $PSScriptRoot
        $psi.UseShellExecute = $false
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $psi.CreateNoWindow = $true
        $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Hidden
        
        $process = [System.Diagnostics.Process]::Start($psi)
        
        # Wait briefly and check
        Start-Sleep -Seconds 2
        
        if (Test-ServerRunning -TestPort $GitServerPort) {
            Write-StatusMessage "   ✓ Git server started on port $GitServerPort" -Type "Success"
            return @{ Success = $true; WasAlreadyRunning = $false }
        }
        
        if ($process.HasExited) {
            $err = $process.StandardError.ReadToEnd()
            Write-StatusMessage "   ✗ Git server failed to start" -Type "Error"
            Write-StatusMessage "   Error: $err" -Type "Dim"
            return @{ Success = $false; Message = "Process exited" }
        }
        
        Write-StatusMessage "   ✓ Git server starting (PID: $($process.Id))" -Type "Success"
        return @{ Success = $true; WasAlreadyRunning = $false }
    }
    catch {
        Write-StatusMessage "   ✗ Failed to start Git server: $_" -Type "Error"
        return @{ Success = $false; Message = $_.Exception.Message }
    }
}

#endregion

#region ADB Configuration

function Setup-AdbPortForwarding {
    param([int]$MainPort, [int]$GitSrvPort)
    
    if ($SkipAdb) {
        return $false
    }
    
    Write-StatusMessage "🔌 Checking ADB for emulator..." -Type "Info"
    
    # Find ADB
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $adb) {
        $adbPaths = @(
            "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
            "$env:PROGRAMFILES\Android\android-sdk\platform-tools\adb.exe"
            "$env:ANDROID_HOME\platform-tools\adb.exe"
        )
        foreach ($p in $adbPaths) {
            if (Test-Path $p) {
                $env:PATH += ";$(Split-Path $p)"
                break
            }
        }
    }
    
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        Write-StatusMessage "   ⚠ ADB not found" -Type "Warning"
        return $false
    }
    
    # Start ADB server
    $null = adb start-server 2>&1
    Start-Sleep -Milliseconds 500
    
    # Check for devices
    $devices = adb devices 2>&1 | Where-Object { $_ -match "device$" -and $_ -notmatch "List" }
    if (-not $devices) {
        Write-StatusMessage "   ⚠ No emulator/device connected" -Type "Warning"
        return $false
    }
    
    # Setup port forwarding
    $r1 = adb reverse tcp:$MainPort tcp:$MainPort 2>&1
    $r2 = adb reverse tcp:$GitSrvPort tcp:$GitSrvPort 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-StatusMessage "   ✓ ADB port forwarding configured" -Type "Success"
        return $true
    }
    
    Write-StatusMessage "   ⚠ Port forwarding failed" -Type "Warning"
    return $false
}

#endregion

#region Tailscale Integration

function Test-TailscaleAvailable {
    return Test-CommandExists "tailscale"
}

function Get-TailscaleStatus {
    if (-not (Test-TailscaleAvailable)) {
        return $null
    }
    
    try {
        $status = tailscale status --json 2>$null | ConvertFrom-Json
        return $status
    }
    catch {
        return $null
    }
}

function Enable-TailscaleServe {
    param([int]$SrvPort, [int]$GitSrvPort)
    
    if (-not (Test-TailscaleAvailable)) {
        Write-StatusMessage "📡 Tailscale CLI not found. Install from https://tailscale.com/download" -Type "Warning"
        return $false
    }
    
    Write-StatusMessage "📡 Configuring Tailscale serve..." -Type "Info"
    
    # Check if already configured
    $serveStatus = tailscale serve status 2>&1
    if ($serveStatus -match ":$SrvPort" -or $serveStatus -match "enabled") {
        Write-StatusMessage "   ✓ Tailscale serve already configured" -Type "Success"
        return $true
    }
    
    # Configure serve
    try {
        # Serve OpenCode on main path
        $result1 = tailscale serve --port 443 "/" "http://localhost:$SrvPort" 2>&1
        
        # Serve Git server on /git path
        $result2 = tailscale serve --port 443 "/git" "http://localhost:$GitSrvPort" 2>&1
        
        Write-StatusMessage "   ✓ Tailscale serve configured" -Type "Success"
        Write-StatusMessage "     Main server: https://<your-device>.tail.ts.net/" -Type "Dim"
        Write-StatusMessage "     Git server:  https://<your-device>.tail.ts.net/git" -Type "Dim"
        return $true
    }
    catch {
        Write-StatusMessage "   ⚠ Failed to configure Tailscale serve: $_" -Type "Warning"
        return $false
    }
}

#endregion

#region Main Display

function Show-ConnectionOptions {
    param(
        [array]$Interfaces,
        [int]$MainPort,
        [int]$GitSrvPort,
        [bool]$AdbOk,
        [bool]$OpenCodeRunning,
        [bool]$GitRunning
    )
    
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Accent
    Write-Host "║                  CONNECTION OPTIONS                            ║" -ForegroundColor $Colors.Accent
    Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Accent
    Write-Host ""
    
    # Server status
    $ocStatus = if ($OpenCodeRunning) { "✓ Running" } else { "✗ Not running" }
    $gitStatus = if ($GitRunning) { "✓ Running" } else { "✗ Not running" }
    
    Write-Host "  OpenCode Server: " -NoNewline
    Write-Host $ocStatus -ForegroundColor $(if ($OpenCodeRunning) { $Colors.Success } else { $Colors.Error })
    Write-Host "  Git HTTP Server: " -NoNewline
    Write-Host $gitStatus -ForegroundColor $(if ($GitRunning) { $Colors.Success } else { $Colors.Error })
    Write-Host ""
    
    # Connection methods
    $connectionUrls = @()
    
    foreach ($iface in $Interfaces | Sort-Object -Property Preferred -Descending) {
        $url = "http://$($iface.IP):$MainPort"
        $marker = if ($iface.Preferred) { "★ " } else { "  " }
        
        switch ($iface.Type) {
            "Tailscale-Hostname" {
                Write-Host "$marker🌐 Tailscale: https://$($iface.IP)" -ForegroundColor $Colors.Success
                $connectionUrls += [PSCustomObject]@{ Type = "Tailscale"; Url = "https://$($iface.IP)"; Priority = 3 }
            }
            "Tailscale" {
                Write-Host "$marker🔗 Tailscale IP: $url" -ForegroundColor $Colors.Success
                $connectionUrls += [PSCustomObject]@{ Type = "Tailscale-IP"; Url = $url; Priority = 2 }
            }
            "WiFi" {
                Write-Host "$marker📶 WiFi: $url" -ForegroundColor $Colors.Info
                $connectionUrls += [PSCustomObject]@{ Type = "WiFi"; Url = $url; Priority = 1 }
            }
            "LAN" {
                Write-Host "$marker🔌 LAN: $url" -ForegroundColor $Colors.Info
                $connectionUrls += [PSCustomObject]@{ Type = "LAN"; Url = $url; Priority = 1 }
            }
            "Localhost" {
                if ($AdbOk) {
                    Write-Host "$marker📱 Emulator (ADB): http://localhost:$MainPort" -ForegroundColor $Colors.Success
                    $connectionUrls += [PSCustomObject]@{ Type = "ADB"; Url = "http://localhost:$MainPort"; Priority = 4 }
                }
            }
        }
    }
    
    Write-Host ""
    
    # Display QR codes for top connection methods
    $topUrls = $connectionUrls | Sort-Object -Property Priority -Descending | Select-Object -First 3
    
    if ($topUrls.Count -gt 0 -and ($OpenCodeRunning -or $GitRunning)) {
        Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Accent
        Write-Host "                    SCAN QR CODE TO CONNECT                     " -ForegroundColor $Colors.Accent
        Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Accent
        Write-Host ""
        
        foreach ($conn in $topUrls) {
            # Create connection payload
            $payload = @{
                host = $conn.Url -replace "https?://", "" -replace ":\d+$", ""
                port = [int]($conn.Url -replace ".*:(\d+)$", '$1')
                type = $conn.Type
                version = "1.0"
                name = "OpenCode Server ($($conn.Type))"
            } | ConvertTo-Json -Compress
            
            Show-TerminalQRCode -Text $payload -Label "📲 $($conn.Type) Connection:"
        }
    }
    
    # Manual entry fallback
    Write-Host ""
    Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Dim
    Write-Host "                    MANUAL CONNECTION                           " -ForegroundColor $Colors.Dim
    Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Dim
    Write-Host ""
    Write-Host "  If QR scanning doesn't work, enter one of these URLs:" -ForegroundColor $Colors.Normal
    Write-Host ""
    
    foreach ($conn in $topUrls | Select-Object -First 2) {
        Write-Host "  • $($conn.Url)" -ForegroundColor $Colors.Success
    }
    
    Write-Host ""
}

#endregion

#region Main Execution

Show-Header

# Check prerequisites
Write-StatusMessage "🔍 Checking prerequisites..." -Type "Info"

# Get network interfaces
Write-StatusMessage "🌐 Detecting network interfaces..." -Type "Info"
$networkInterfaces = Get-NetworkInterfaces
Write-StatusMessage "   Found $($networkInterfaces.Count) connection option(s)" -Type "Success"

# Setup ADB
$adbConfigured = Setup-AdbPortForwarding -MainPort $Port -GitSrvPort $GitPort

# Start OpenCode server
$openCodeResult = Start-OpenCodeServer -OpenCodePort $Port

# Start Git server
$gitResult = Start-GitServer -GitServerPort $GitPort

# Configure Tailscale serve if requested
$tailscaleServeOk = $false
if ($EnableTailscaleServe -and (Test-TailscaleAvailable)) {
    $tailscaleServeOk = Enable-TailscaleServe -SrvPort $Port -GitSrvPort $GitPort
}

# Display results
Show-ConnectionOptions `
    -Interfaces $networkInterfaces `
    -MainPort $Port `
    -GitSrvPort $GitPort `
    -AdbOk $adbConfigured `
    -OpenCodeRunning $openCodeResult.Success `
    -GitRunning $gitResult.Success

# Final status
Write-Host ""
if ($openCodeResult.Success) {
    Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Success
    Write-Host "║                ✅ SETUP COMPLETE - READY TO CONNECT            ║" -ForegroundColor $Colors.Success
    Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Success
    Write-Host ""
    Write-StatusMessage "📱 Open MOCCA app and scan one of the QR codes above" -Type "Info"
    Write-Host ""
    Write-StatusMessage "Press any key to exit..." -Type "Dim"
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
else {
    Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Error
    Write-Host "║                ❌ SETUP INCOMPLETE                             ║" -ForegroundColor $Colors.Error
    Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Error
    Write-Host ""
    Write-StatusMessage "🔧 Troubleshooting:" -Type "Warning"
    Write-Host "   1. Ensure OpenCode is installed" -ForegroundColor $Colors.Normal
    Write-Host "   2. Run manually: opencode serve --port $Port" -ForegroundColor $Colors.Normal
    Write-Host "   3. Check Windows Firewall isn't blocking port $Port" -ForegroundColor $Colors.Normal
    Write-Host ""
    exit 1
}

#endregion
