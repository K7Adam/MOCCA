#!/usr/bin/env pwsh
#Requires -Version 7.0

<#
.SYNOPSIS
    MOCCA Setup Script v4.1 - "Actually Works" Edition

.DESCRIPTION
    Intelligent setup for MOCCA app connection with auto-port detection,
    comprehensive diagnostics, and terminal QR codes.

.EXAMPLE
    .\mocca-setup.ps1
    
.EXAMPLE
    .\mocca-setup.ps1 -Port 8080

.PARAMETER Port
    Port for OpenCode server (default: 4096, auto-detects if busy)

.PARAMETER GitPort
    Port for Git HTTP server (default: 4097, auto-detects if busy)

.PARAMETER UseTailscale
    Prioritize Tailscale IPs

.PARAMETER EnableTailscaleServe
    Configure Tailscale serve

.PARAMETER SkipAdb
    Skip ADB configuration

.PARAMETER SkipGitServer
    Skip Git HTTP server
#>

param(
    [int]$Port = 4096,
    [int]$GitPort = 4097,
    [switch]$UseTailscale,
    [switch]$EnableTailscaleServe,
    [switch]$SkipAdb,
    [switch]$SkipGitServer
)

$ErrorActionPreference = "Stop"
$script:Version = "4.1.0"
$script:StartTime = Get-Date

# Colors
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
║   MOCCA Setup v$script:Version - "Actually Works" Edition                  ║
║                                                                ║
╚════════════════════════════════════════════════════════════════╝
"@ -ForegroundColor $Colors.Accent
    Write-Host ""
}

function Test-PortAvailable {
    param([int]$TestPort)
    
    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $TestPort)
        $listener.Start()
        $listener.Stop()
        return $true
    }
    catch {
        return $false
    }
}

function Find-AvailablePort {
    param(
        [int]$StartPort = 4096,
        [int]$MaxPort = 4105
    )
    
    for ($p = $StartPort; $p -le $MaxPort; $p++) {
        if (Test-PortAvailable -TestPort $p) {
            return $p
        }
    }
    return $null
}

function Test-ServerRunning {
    param(
        [int]$TestPort,
        [int]$TimeoutMs = 2000
    )
    
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $result = $client.BeginConnect("localhost", $TestPort, $null, $null)
        $success = $result.AsyncWaitHandle.WaitOne($TimeoutMs, $false)
        $client.Close()
        return $success
    }
    catch {
        return $false
    }
}

#endregion

#region QR Code Display

function Show-TerminalQRCode {
    param(
        [string]$Text,
        [string]$Label = ""
    )
    
    Write-Host ""
    if ($Label) {
        Write-Host "  $Label" -ForegroundColor $Colors.Accent
    }
    
    # Encode URL for QR API
    $encoded = [System.Web.HttpUtility]::UrlEncode($Text)
    $qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=$encoded"
    
    # Draw a box around the connection info
    $line = "─" * [Math]::Min($Text.Length + 6, 50)
    Write-Host "  ┌$line┐" -ForegroundColor $Colors.Success
    Write-Host "  │  📱 SCAN THIS URL:" -ForegroundColor $Colors.Normal
    Write-Host "  │     $Text" -ForegroundColor $Colors.Success
    Write-Host "  └$line┘" -ForegroundColor $Colors.Success
    
    Write-Host ""
    Write-Host "  💡 QR Code: $qrUrl" -ForegroundColor $Colors.Dim
    Write-Host "     (Open this URL to see scannable QR code)" -ForegroundColor $Colors.Dim
    Write-Host ""
}

#endregion

#region Network Detection

function Get-NetworkInterfaces {
    $interfaces = @()
    
    # Method 1: Get-NetIPAddress (most reliable)
    try {
        $adapters = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue | 
            Where-Object { 
                $_.IPAddress -notmatch "^127\." -and 
                $_.IPAddress -notmatch "^169\.254" -and
                $_.InterfaceAlias -notmatch "Loopback|Virtual|VMware|Hyper-V|vEthernet|WSL|docker|TAP"
            }
        
        foreach ($adapter in $adapters) {
            $isTailscale = $adapter.IPAddress -match "^100\.(6[4-9]|[7-9][0-9]|1[0-2][0-7])\."
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
        Write-StatusMessage "   Could not enumerate network adapters: $_" -Type "Dim"
    }
    
    # Method 2: DNS resolution fallback
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
    
    # Always include localhost
    $interfaces += [PSCustomObject]@{
        Type = "Localhost"
        IP = "127.0.0.1"
        Interface = "Loopback"
        Preferred = $false
    }
    
    # Check for Tailscale hostname
    if (Get-Command tailscale -ErrorAction SilentlyContinue) {
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

function Start-OpenCodeServer {
    param([int]$OpenCodePort)
    
    Write-StatusMessage "🚀 Checking OpenCode server on port $OpenCodePort..." -Type "Info"
    
    # Check if already running on requested port
    if (Test-ServerRunning -TestPort $OpenCodePort) {
        Write-StatusMessage "   ✓ OpenCode already running on port $OpenCodePort" -Type "Success"
        return @{ Success = $true; WasAlreadyRunning = $true; Port = $OpenCodePort }
    }
    
    # Check if running on any other port (scan common range)
    Write-StatusMessage "   Checking for OpenCode on other ports..." -Type "Dim"
    for ($testPort = 4096; $testPort -le 4100; $testPort++) {
        if ($testPort -ne $OpenCodePort -and (Test-ServerRunning -TestPort $testPort)) {
            Write-StatusMessage "   ⚠ OpenCode found running on port $testPort (not $OpenCodePort)" -Type "Warning"
            $choice = Read-Host "   Use port $testPort instead? [Y/n]"
            if ($choice -ne 'n') {
                return @{ Success = $true; WasAlreadyRunning = $true; Port = $testPort }
            }
        }
    }
    
    # Check for opencode process
    $process = Get-Process -Name "opencode" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($process) {
        Write-StatusMessage "   ⚠ OpenCode process found (PID: $($process.Id)) but not responding" -Type "Warning"
        Write-StatusMessage "     Server may still be starting..." -Type "Dim"
        
        # Wait and retry
        for ($i = 0; $i -lt 10; $i++) {
            Start-Sleep -Seconds 2
            if (Test-ServerRunning -TestPort $OpenCodePort) {
                Write-StatusMessage "   ✓ OpenCode is now responding!" -Type "Success"
                return @{ Success = $true; WasAlreadyRunning = $true; Port = $OpenCodePort }
            }
        }
    }
    
    # Find opencode executable
    $exe = $null
    $searchPaths = @(
        (Get-Command opencode -ErrorAction SilentlyContinue)?.Source
        "$env:USERPROFILE\.cargo\bin\opencode.exe"
        "$env:LOCALAPPDATA\Programs\opencode\opencode.exe"
        "$env:USERPROFILE\.local\bin\opencode.exe"
        ".\opencode.exe"
        "$env:USERPROFILE\scoop\shims\opencode.exe"
        "$env:USERPROFILE\AppData\Local\Microsoft\WinGet\Packages\OpenCode*\opencode.exe"
    )
    
    foreach ($p in $searchPaths) {
        if ($p) {
            if (Test-Path $p) {
                $exe = $p
                break
            }
            # Try wildcard for winget path
            $matches = Get-Item $p -ErrorAction SilentlyContinue
            if ($matches) {
                $exe = $matches | Select-Object -First 1
                break
            }
        }
    }
    
    if (-not $exe) {
        Write-StatusMessage "   ✗ OpenCode not found!" -Type "Error"
        Write-StatusMessage "" -Type "Normal"
        Write-StatusMessage "   🔧 TO FIX:" -Type "Warning"
        Write-StatusMessage "      1. Install OpenCode:" -Type "Normal"
        Write-StatusMessage "         winget install OpenCode" -Type "Info"
        Write-StatusMessage "         OR: cargo install opencode" -Type "Info"
        Write-StatusMessage "         OR: Download from https://github.com/opencode-ai/opencode/releases" -Type "Info"
        Write-StatusMessage "" -Type "Normal"
        return @{ Success = $false; Message = "OpenCode not found"; Port = $OpenCodePort }
    }
    
    Write-StatusMessage "   Found OpenCode at: $exe" -Type "Dim"
    Write-StatusMessage "   Starting server on port $OpenCodePort..." -Type "Info"
    
    try {
        # Check if port is available
        if (-not (Test-PortAvailable -TestPort $OpenCodePort)) {
            $newPort = Find-AvailablePort -StartPort 4096
            if ($newPort) {
                Write-StatusMessage "   ⚠ Port $OpenCodePort is busy, using port $newPort instead" -Type "Warning"
                $OpenCodePort = $newPort
            }
            else {
                Write-StatusMessage "   ✗ No available ports found (tried 4096-4105)" -Type "Error"
                return @{ Success = $false; Message = "No available ports"; Port = $OpenCodePort }
            }
        }
        
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $exe
        $psi.Arguments = "serve --port $OpenCodePort"
        $psi.UseShellExecute = $true
        $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Normal
        $psi.CreateNoWindow = $false
        
        [System.Diagnostics.Process]::Start($psi) | Out-Null
        
        # Wait for startup with progress
        Write-StatusMessage "   Waiting for server..." -Type "Info" -NoNewline
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Seconds 1
            Write-Host "." -NoNewline -ForegroundColor $Colors.Info
            if (Test-ServerRunning -TestPort $OpenCodePort) {
                Write-Host ""
                Write-StatusMessage "   ✓ OpenCode server started on port $OpenCodePort!" -Type "Success"
                return @{ Success = $true; WasAlreadyRunning = $false; Port = $OpenCodePort }
            }
        }
        Write-Host ""
        Write-StatusMessage "   ⚠ Server didn't respond within 30 seconds" -Type "Warning"
        Write-StatusMessage "     An OpenCode window should have opened - check it for errors" -Type "Dim"
        return @{ Success = $false; Message = "Timeout"; Port = $OpenCodePort }
    }
    catch {
        Write-StatusMessage "   ✗ Failed to start: $_" -Type "Error"
        return @{ Success = $false; Message = $_.Exception.Message; Port = $OpenCodePort }
    }
}

function Start-GitServer {
    param([int]$GitServerPort)
    
    if ($SkipGitServer) {
        return @{ Success = $false; Message = "Skipped by user" }
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
        "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\OpenJS.Bun*\bun.exe"
    )
    
    foreach ($p in $bunPaths) {
        if ($p) {
            if (Test-Path $p) {
                $bunPath = $p
                break
            }
            $matches = Get-Item $p -ErrorAction SilentlyContinue
            if ($matches) {
                $bunPath = $matches | Select-Object -First 1
                break
            }
        }
    }
    
    if (-not $bunPath) {
        Write-StatusMessage "   ⚠ Bun not found - Git server requires Bun" -Type "Warning"
        Write-StatusMessage "   Install: winget install OpenJS.Bun OR https://bun.sh" -Type "Dim"
        return @{ Success = $false; Message = "Bun not found" }
    }
    
    # Find git plugin - CRITICAL: Must run from .opencode/ directory
    $opencodeDir = "$PSScriptRoot\.opencode"
    $pluginScript = "$opencodeDir\plugin\git-plugin.js"
    
    if (-not (Test-Path $pluginScript)) {
        Write-StatusMessage "   ⚠ Git plugin not found at $pluginScript" -Type "Warning"
        return @{ Success = $false; Message = "Plugin not found" }
    }
    
    # Check if node_modules exists (dependencies installed)
    if (-not (Test-Path "$opencodeDir\node_modules")) {
        Write-StatusMessage "   Installing dependencies..." -Type "Info"
        try {
            Push-Location $opencodeDir
            $installOutput = bun install 2>&1
            Pop-Location
            Write-StatusMessage "   ✓ Dependencies installed" -Type "Success"
        }
        catch {
            Write-StatusMessage "   ⚠ Failed to install dependencies: $_" -Type "Warning"
        }
    }
    
    Write-StatusMessage "   Starting Git HTTP server on port $GitServerPort..." -Type "Info"
    
    try {
        # Find available port if needed
        if (-not (Test-PortAvailable -TestPort $GitServerPort)) {
            $newPort = Find-AvailablePort -StartPort 4097
            if ($newPort) {
                Write-StatusMessage "   ⚠ Port $GitServerPort busy, using $newPort" -Type "Warning"
                $GitServerPort = $newPort
            }
            else {
                Write-StatusMessage "   ✗ No available ports" -Type "Error"
                return @{ Success = $false; Message = "No available ports" }
            }
        }
        
        # CRITICAL: Run from .opencode directory so imports work
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $bunPath
        $psi.Arguments = "run plugin/git-plugin.js"
        $psi.WorkingDirectory = $opencodeDir  # THIS IS THE KEY FIX!
        $psi.UseShellExecute = $false
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $psi.CreateNoWindow = $true
        $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Hidden
        
        # Set environment variable for the server port
        $psi.EnvironmentVariables["GIT_SERVER_PORT"] = $GitServerPort
        
        $process = [System.Diagnostics.Process]::Start($psi)
        
        # Wait and check
        Start-Sleep -Seconds 3
        
        if (Test-ServerRunning -TestPort $GitServerPort) {
            Write-StatusMessage "   ✓ Git server started on port $GitServerPort (PID: $($process.Id))" -Type "Success"
            return @{ Success = $true; WasAlreadyRunning = $false }
        }
        
        if ($process.HasExited) {
            $err = $process.StandardError.ReadToEnd()
            Write-StatusMessage "   ✗ Git server exited immediately" -Type "Error"
            if ($err -match "Cannot find module") {
                Write-StatusMessage "   🔧 TO FIX: Run 'bun install' in $opencodeDir" -Type "Warning"
            }
            else {
                Write-StatusMessage "   Error: $err" -Type "Dim"
            }
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
        Write-StatusMessage "   ⚠ ADB not found - install Android SDK or skip with -SkipAdb" -Type "Dim"
        return $false
    }
    
    # Start ADB server
    $null = adb start-server 2>&1
    Start-Sleep -Milliseconds 500
    
    # Check for devices
    $devices = adb devices 2>&1 | Where-Object { $_ -match "device$" -and $_ -notmatch "List" }
    if (-not $devices) {
        Write-StatusMessage "   ⚠ No emulator/device connected" -Type "Dim"
        return $false
    }
    
    # Setup port forwarding
    $null = adb reverse tcp:$MainPort tcp:$MainPort 2>&1
    $null = adb reverse tcp:$GitSrvPort tcp:$GitSrvPort 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-StatusMessage "   ✓ ADB configured (localhost:$MainPort → device)" -Type "Success"
        return $true
    }
    
    return $false
}

#endregion

#region Tailscale Integration

function Test-TailscaleAvailable {
    return $null -ne (Get-Command tailscale -ErrorAction SilentlyContinue)
}

function Enable-TailscaleServe {
    param([int]$SrvPort, [int]$GitSrvPort)
    
    if (-not (Test-TailscaleAvailable)) {
        Write-StatusMessage "📡 Tailscale not installed - skip with -EnableTailscaleServe" -Type "Dim"
        return $false
    }
    
    Write-StatusMessage "📡 Configuring Tailscale serve..." -Type "Info"
    
    try {
        # Check current status
        $serveStatus = tailscale serve status 2>&1
        
        if ($serveStatus -match "https://.*\.tail.*\.ts\.net") {
            Write-StatusMessage "   ✓ Tailscale serve already configured" -Type "Success"
            $serveStatus | Select-String "https://" | ForEach-Object {
                Write-StatusMessage "      $_" -Type "Dim"
            }
            return $true
        }
        
        # Configure serve
        Write-StatusMessage "   Setting up HTTPS endpoints..." -Type "Dim"
        $result = tailscale serve --port 443 "/" "http://localhost:$SrvPort" 2>&1
        $result2 = tailscale serve --port 443 "/git" "http://localhost:$GitSrvPort" 2>&1
        
        Write-StatusMessage "   ✓ Tailscale serve configured!" -Type "Success"
        Write-StatusMessage "     Your device is now accessible via HTTPS on your tailnet" -Type "Dim"
        return $true
    }
    catch {
        Write-StatusMessage "   ⚠ Failed to configure Tailscale: $_" -Type "Warning"
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
    $ocStatus = if ($OpenCodeRunning) { "✓ Running on port $MainPort" } else { "✗ Not running" }
    $gitStatus = if ($GitRunning) { "✓ Running on port $GitSrvPort" } else { "⚠ Not running (optional)" }
    
    Write-Host "  OpenCode: " -NoNewline
    Write-Host $ocStatus -ForegroundColor $(if ($OpenCodeRunning) { $Colors.Success } else { $Colors.Error })
    Write-Host "  Git HTTP: " -NoNewline
    Write-Host $gitStatus -ForegroundColor $(if ($GitRunning) { $Colors.Success } else { $Colors.Warning })
    Write-Host ""
    
    # Collect all connection URLs
    $connectionUrls = @()
    
    foreach ($iface in ($Interfaces | Sort-Object -Property { $_.Preferred } -Descending)) {
        $url = "http://$($iface.IP):$MainPort"
        $marker = if ($iface.Preferred) { "★ " } else { "  " }
        
        switch ($iface.Type) {
            "Tailscale-Hostname" {
                $httpsUrl = "https://$($iface.IP)"
                Write-Host "$marker🌐 Tailscale HTTPS: $httpsUrl" -ForegroundColor $Colors.Success
                $connectionUrls += [PSCustomObject]@{ Type = "Tailscale"; Url = $httpsUrl; Priority = 4 }
            }
            "Tailscale" {
                Write-Host "$marker🔗 Tailscale: $url" -ForegroundColor $Colors.Success
                $connectionUrls += [PSCustomObject]@{ Type = "Tailscale-IP"; Url = $url; Priority = 3 }
            }
            "WiFi" {
                Write-Host "$marker📶 WiFi: $url" -ForegroundColor $Colors.Info
                $connectionUrls += [PSCustomObject]@{ Type = "WiFi"; Url = $url; Priority = 2 }
            }
            "LAN" {
                Write-Host "$marker🔌 LAN: $url" -ForegroundColor $Colors.Info
                $connectionUrls += [PSCustomObject]@{ Type = "LAN"; Url = $url; Priority = 2 }
            }
            "Localhost" {
                if ($AdbOk) {
                    Write-Host "$marker📱 Emulator (ADB): http://localhost:$MainPort" -ForegroundColor $Colors.Success
                    $connectionUrls += [PSCustomObject]@{ Type = "ADB"; Url = "http://localhost:$MainPort"; Priority = 5 }
                }
            }
        }
    }
    
    Write-Host ""
    
    # ALWAYS show QR codes for top connections
    $topUrls = $connectionUrls | Sort-Object -Property Priority -Descending | Select-Object -First 3
    
    if ($topUrls.Count -gt 0) {
        Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Accent
        Write-Host "                    SCAN TO CONNECT                             " -ForegroundColor $Colors.Accent
        Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Accent
        Write-Host ""
        
        foreach ($conn in $topUrls) {
            Show-TerminalQRCode -Text $conn.Url -Label "$($conn.Type) Connection:"
        }
        
        Write-Host ""
        Write-Host "📱 Open MOCCA app → Tap 'Scan QR Code' → Scan one of the above" -ForegroundColor $Colors.Success
    }
    
    Write-Host ""
}

function Show-Troubleshooting {
    param(
        [bool]$OpenCodeOk,
        [bool]$GitOk,
        [int]$Port,
        [int]$GitPort
    )
    
    $issues = @()
    
    if (-not $OpenCodeOk) {
        $issues += @{
            Title = "OpenCode Server Not Running"
            Fixes = @(
                "Install OpenCode: winget install OpenCode"
                "Or download from: https://github.com/opencode-ai/opencode/releases"
                "Then run manually: opencode serve --port $Port"
                "Check Windows Defender/Firewall isn't blocking port $Port"
            )
        }
    }
    
    if (-not $GitOk) {
        $issues += @{
            Title = "Git Server Not Running (Optional)"
            Fixes = @(
                "Install Bun: winget install OpenJS.Bun"
                "Run: cd .opencode && bun install"
                "Or skip with: .\mocca-setup.ps1 -SkipGitServer"
            )
        }
    }
    
    if ($issues.Count -eq 0) {
        return
    }
    
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Warning
    Write-Host "║                    🔧 TROUBLESHOOTING                          ║" -ForegroundColor $Colors.Warning
    Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Warning
    Write-Host ""
    
    foreach ($issue in $issues) {
        Write-Host "❌ $($issue.Title)" -ForegroundColor $Colors.Error
        Write-Host "   Fixes:" -ForegroundColor $Colors.Normal
        foreach ($fix in $issue.Fixes) {
            Write-Host "      • $fix" -ForegroundColor $Colors.Info
        }
        Write-Host ""
    }
}

#endregion

#region Main Execution

Show-Header

# Check prerequisites
Write-StatusMessage "🔍 Scanning system..." -Type "Info"

# Get network interfaces
Write-StatusMessage "🌐 Detecting network interfaces..." -Type "Info"
$networkInterfaces = Get-NetworkInterfaces
if ($networkInterfaces.Count -gt 0) {
    Write-StatusMessage "   ✓ Found $($networkInterfaces.Count) connection option(s)" -Type "Success"
}
else {
    Write-StatusMessage "   ⚠ No network interfaces found" -Type "Warning"
}

# Setup ADB
$adbConfigured = Setup-AdbPortForwarding -MainPort $Port -GitSrvPort $GitPort

# Start servers
$openCodeResult = Start-OpenCodeServer -OpenCodePort $Port
$actualPort = $openCodeResult.Port  # May have changed if auto-detected

$gitResult = Start-GitServer -GitServerPort $GitPort

# Configure Tailscale if requested
$tailscaleServeOk = $false
if ($EnableTailscaleServe) {
    $tailscaleServeOk = Enable-TailscaleServe -SrvPort $actualPort -GitSrvPort $GitPort
}

# Display results
Show-ConnectionOptions `
    -Interfaces $networkInterfaces `
    -MainPort $actualPort `
    -GitSrvPort $GitPort `
    -AdbOk $adbConfigured `
    -OpenCodeRunning $openCodeResult.Success `
    -GitRunning $gitResult.Success

# Show troubleshooting if needed
Show-Troubleshooting `
    -OpenCodeOk $openCodeResult.Success `
    -GitOk $gitResult.Success `
    -Port $actualPort `
    -GitPort $GitPort

# Final status
$elapsed = [math]::Round(((Get-Date) - $script:StartTime).TotalSeconds)

Write-Host ""
if ($openCodeResult.Success) {
    Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Success
    Write-Host "║               ✅ SETUP COMPLETE (in ${elapsed}s)                            ║" -ForegroundColor $Colors.Success
    Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Success
    Write-Host ""
    Write-StatusMessage "📱 Open MOCCA app and scan a QR code above to connect" -Type "Info"
    Write-Host ""
    Write-StatusMessage "Press any key to exit..." -Type "Dim"
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
else {
    Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Error
    Write-Host "║               ❌ SETUP INCOMPLETE                              ║" -ForegroundColor $Colors.Error
    Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Error
    Write-Host ""
    Write-StatusMessage "See 🔧 TROUBLESHOOTING section above for fixes" -Type "Warning"
    Write-Host ""
    exit 1
}

#endregion
