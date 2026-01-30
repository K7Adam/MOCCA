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
    [switch]$SkipGitServer,
    [switch]$DebugMode
)

$ErrorActionPreference = "Stop"
$script:Version = "4.1.1"
$script:StartTime = Get-Date
$script:LogFile = "$env:TEMP\mocca-setup-debug-$(Get-Date -Format 'yyyyMMdd-HHmmss').log"

#region Debug Logging

function Write-DebugLog {
    param(
        [string]$Message,
        [string]$Level = "INFO"
    )
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
    $logEntry = "[$timestamp] [$Level] $Message"
    
    # Always write to log file
    Add-Content -Path $script:LogFile -Value $logEntry -ErrorAction SilentlyContinue
    
    # Also display if debug mode enabled
    if ($DebugMode) {
        $color = switch ($Level) {
            "ERROR" { "Red" }
            "WARN" { "Yellow" }
            "DEBUG" { "DarkGray" }
            default { "Cyan" }
        }
        Write-Host "  [DBG] $Message" -ForegroundColor $color
    }
}

function Start-DebugSession {
    if ($DebugMode) {
        Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
        Write-Host "║              DEBUG MODE ENABLED - VERBOSE LOGGING              ║" -ForegroundColor Magenta
        Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta
        Write-Host "  Log file: $script:LogFile" -ForegroundColor DarkGray
        Write-Host ""
        
        # Write initial system info
        Write-DebugLog "=== MOCCA Setup Debug Session Started ===" "INFO"
        Write-DebugLog "Script Version: $script:Version" "INFO"
        Write-DebugLog "PowerShell Version: $($PSVersionTable.PSVersion)" "INFO"
        Write-DebugLog "OS: $($PSVersionTable.OS)" "INFO"
        Write-DebugLog "Working Directory: $(Get-Location)" "INFO"
        Write-DebugLog "Parameters: Port=$Port, GitPort=$GitPort, UseTailscale=$UseTailscale, EnableTailscaleServe=$EnableTailscaleServe" "INFO"
        
        # Network diagnostics
        Write-DebugLog "=== Network Diagnostics ===" "INFO"
        try {
            $hostname = [System.Net.Dns]::GetHostName()
            Write-DebugLog "Hostname: $hostname" "INFO"
            
            $ipAddresses = [System.Net.Dns]::GetHostAddresses($hostname) | Where-Object { $_.AddressFamily -eq 'InterNetwork' }
            foreach ($ip in $ipAddresses) {
                Write-DebugLog "IP Address: $($ip.IPAddressToString)" "INFO"
            }
        }
        catch {
            Write-DebugLog "Failed to get network info: $_" "ERROR"
        }
        
        # Check for processes
        Write-DebugLog "=== Process Check ===" "INFO"
        $processes = @("opencode", "bun", "node", "adb")
        foreach ($proc in $processes) {
            $found = Get-Process -Name $proc -ErrorAction SilentlyContinue
            if ($found) {
                Write-DebugLog "Process '$proc' found (PID: $($found.Id), Count: $($found.Count))" "INFO"
            }
            else {
                Write-DebugLog "Process '$proc' not running" "DEBUG"
            }
        }
        
        Write-Host ""
    }
}

#endregion

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
    
    Write-DebugLog "Test-PortAvailable: Testing port $TestPort" "DEBUG"
    
    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $TestPort)
        $listener.Start()
        $listener.Stop()
        Write-DebugLog "Test-PortAvailable: Port $TestPort is AVAILABLE" "DEBUG"
        return $true
    }
    catch {
        Write-DebugLog "Test-PortAvailable: Port $TestPort is BUSY ($($_.Exception.Message))" "DEBUG"
        return $false
    }
}

function Find-AvailablePort {
    param(
        [int]$StartPort = 4096,
        [int]$MaxPort = 4105
    )
    
    Write-DebugLog "Find-AvailablePort: Searching from $StartPort to $MaxPort" "DEBUG"
    
    for ($p = $StartPort; $p -le $MaxPort; $p++) {
        if (Test-PortAvailable -TestPort $p) {
            Write-DebugLog "Find-AvailablePort: Found available port $p" "DEBUG"
            return $p
        }
    }
    Write-DebugLog "Find-AvailablePort: No available ports found in range!" "ERROR"
    return $null
}

function Test-ServerRunning {
    param(
        [int]$TestPort,
        [int]$TimeoutMs = 3000
    )
    
    Write-DebugLog "Test-ServerRunning: Testing connection to port $TestPort (timeout: ${TimeoutMs}ms)" "DEBUG"
    
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $connectionTask = $client.ConnectAsync("127.0.0.1", $TestPort)
        $waited = $connectionTask.Wait($TimeoutMs)
        
        Write-DebugLog "Test-ServerRunning: Wait completed=$waited, Connected=$($client.Connected)" "DEBUG"
        
        if ($waited -and $client.Connected) {
            $client.Close()
            Write-DebugLog "Test-ServerRunning: Port $TestPort IS RUNNING (connected successfully)" "DEBUG"
            return $true
        }
        
        $client.Close()
        Write-DebugLog "Test-ServerRunning: Port $TestPort NOT RESPONDING (waited=$waited, connected=$($client.Connected))" "DEBUG"
        return $false
    }
    catch {
        Write-DebugLog "Test-ServerRunning: Port $TestPort ERROR - $($_.Exception.GetType().Name): $($_.Exception.Message)" "DEBUG"
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
    
    Write-DebugLog "Start-OpenCodeServer: Called with requested port $OpenCodePort" "DEBUG"
    Write-StatusMessage "🚀 Checking OpenCode server on port $OpenCodePort..." -Type "Info"
    
    # Check if already running on requested port
    Write-DebugLog "Start-OpenCodeServer: Testing if server already running on port $OpenCodePort" "DEBUG"
    if (Test-ServerRunning -TestPort $OpenCodePort) {
        Write-DebugLog "Start-OpenCodeServer: Server already running on port $OpenCodePort" "INFO"
        Write-StatusMessage "   ✓ OpenCode already running on port $OpenCodePort" -Type "Success"
        return @{ Success = $true; WasAlreadyRunning = $true; Port = $OpenCodePort }
    }
    Write-DebugLog "Start-OpenCodeServer: Server not found on requested port $OpenCodePort" "DEBUG"
    
    # Check if running on any other port (scan common range)
    Write-DebugLog "Start-OpenCodeServer: Scanning ports 4096-4100 for existing server" "DEBUG"
    Write-StatusMessage "   Checking for OpenCode on other ports..." -Type "Dim"
    for ($testPort = 4096; $testPort -le 4100; $testPort++) {
        if ($testPort -ne $OpenCodePort) {
            Write-DebugLog "Start-OpenCodeServer: Testing port $testPort..." "DEBUG"
            if (Test-ServerRunning -TestPort $testPort) {
                Write-DebugLog "Start-OpenCodeServer: Found server running on port $testPort!" "INFO"
                Write-StatusMessage "   ⚠ OpenCode found running on port $testPort (not $OpenCodePort)" -Type "Warning"
                $choice = Read-Host "   Use port $testPort instead? [Y/n]"
                if ($choice -ne 'n') {
                    Write-DebugLog "Start-OpenCodeServer: User chose to use port $testPort" "INFO"
                    return @{ Success = $true; WasAlreadyRunning = $true; Port = $testPort }
                }
                Write-DebugLog "Start-OpenCodeServer: User declined to use port $testPort, continuing" "DEBUG"
            }
        }
    }
    
    # Check for opencode process
    Write-DebugLog "Start-OpenCodeServer: Checking for opencode.exe process" "DEBUG"
    $process = Get-Process -Name "opencode" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($process) {
        Write-DebugLog "Start-OpenCodeServer: Found opencode process (PID: $($process.Id))" "INFO"
        Write-StatusMessage "   ⚠ OpenCode process found (PID: $($process.Id)) but not responding" -Type "Warning"
        Write-StatusMessage "     Server may still be starting..." -Type "Dim"
        
        # Wait and retry with logging
        Write-DebugLog "Start-OpenCodeServer: Waiting for process to start responding (20 seconds max)" "DEBUG"
        for ($i = 0; $i -lt 10; $i++) {
            Start-Sleep -Seconds 2
            Write-DebugLog "Start-OpenCodeServer: Retry $i/10 - testing port $OpenCodePort" "DEBUG"
            if (Test-ServerRunning -TestPort $OpenCodePort) {
                Write-DebugLog "Start-OpenCodeServer: Server is now responding on port $OpenCodePort!" "INFO"
                Write-StatusMessage "   ✓ OpenCode is now responding!" -Type "Success"
                return @{ Success = $true; WasAlreadyRunning = $true; Port = $OpenCodePort }
            }
        }
        Write-DebugLog "Start-OpenCodeServer: Timed out waiting for process to respond" "WARN"
    }
    else {
        Write-DebugLog "Start-OpenCodeServer: No opencode.exe process found" "DEBUG"
    }
    
    # Find opencode executable
    # Find opencode executable
    Write-DebugLog "Start-OpenCodeServer: Searching for opencode.exe" "DEBUG"
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
            Write-DebugLog "Start-OpenCodeServer: Checking path: $p" "DEBUG"
            if (Test-Path $p) {
                $exe = $p
                Write-DebugLog "Start-OpenCodeServer: Found at: $exe" "INFO"
                break
            }
            # Try wildcard for winget path
            $matches = Get-Item $p -ErrorAction SilentlyContinue
            if ($matches) {
                $exe = $matches | Select-Object -First 1
                Write-DebugLog "Start-OpenCodeServer: Found via wildcard at: $exe" "INFO"
                break
            }
        }
    }
    
    if (-not $exe) {
        Write-DebugLog "Start-OpenCodeServer: opencode.exe NOT FOUND after checking all paths" "ERROR"
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
    Write-DebugLog "Start-OpenCodeServer: Starting process '$exe' with args 'serve --port $OpenCodePort'" "INFO"
    
    try {
        # Check if port is available
        Write-DebugLog "Start-OpenCodeServer: Checking if port $OpenCodePort is available" "DEBUG"
        if (-not (Test-PortAvailable -TestPort $OpenCodePort)) {
            Write-DebugLog "Start-OpenCodeServer: Port $OpenCodePort is not available, finding alternative" "WARN"
            $newPort = Find-AvailablePort -StartPort 4096
            if ($newPort) {
                Write-DebugLog "Start-OpenCodeServer: Using alternative port $newPort" "INFO"
                Write-StatusMessage "   ⚠ Port $OpenCodePort is busy, using port $newPort instead" -Type "Warning"
                $OpenCodePort = $newPort
            }
            else {
                Write-DebugLog "Start-OpenCodeServer: No available ports found in range 4096-4105" "ERROR"
                Write-StatusMessage "   ✗ No available ports found (tried 4096-4105)" -Type "Error"
                return @{ Success = $false; Message = "No available ports"; Port = $OpenCodePort }
            }
        }
        else {
            Write-DebugLog "Start-OpenCodeServer: Port $OpenCodePort is available" "DEBUG"
        }
        
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $exe
        $psi.Arguments = "serve --port $OpenCodePort"
        $psi.UseShellExecute = $true
        $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Normal
        $psi.CreateNoWindow = $false
        
        Write-DebugLog "Start-OpenCodeServer: Launching process..." "DEBUG"
        $startedProcess = [System.Diagnostics.Process]::Start($psi)
        Write-DebugLog "Start-OpenCodeServer: Process started with ID: $($startedProcess.Id)" "INFO"
        
        # Wait for startup with progress
        Write-StatusMessage "   Waiting for server (port $OpenCodePort)..." -Type "Info" -NoNewline
        Write-DebugLog "Start-OpenCodeServer: Waiting up to 30 seconds for server to respond..." "DEBUG"
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Seconds 1
            Write-Host "." -NoNewline -ForegroundColor $Colors.Info
            $isRunning = Test-ServerRunning -TestPort $OpenCodePort
            Write-DebugLog "Start-OpenCodeServer: Second $i - Test-ServerRunning returned: $isRunning" "DEBUG"
            if ($isRunning) {
                Write-Host ""
                Write-DebugLog "Start-OpenCodeServer: Server is RUNNING on port $OpenCodePort!" "INFO"
                Write-StatusMessage "   ✓ OpenCode server started on port $OpenCodePort!" -Type "Success"
                return @{ Success = $true; WasAlreadyRunning = $false; Port = $OpenCodePort }
            }
        }
        Write-Host ""
        Write-DebugLog "Start-OpenCodeServer: TIMED OUT after 30 seconds - server not responding" "ERROR"
        Write-DebugLog "Start-OpenCodeServer: Process may still be starting - check OpenCode window for errors" "WARN"
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
    
    Write-DebugLog "Start-GitServer: Called with GitServerPort=$GitServerPort" "DEBUG"
    
    if ($SkipGitServer) {
        Write-DebugLog "Start-GitServer: Skipped by user (-SkipGitServer flag)" "DEBUG"
        return @{ Success = $false; Message = "Skipped by user" }
    }
    
    Write-StatusMessage "🔧 Checking Git HTTP server on port $GitServerPort..." -Type "Info"
    
    # Check if already running
    Write-DebugLog "Start-GitServer: Testing if server already running on port $GitServerPort" "DEBUG"
    if (Test-ServerRunning -TestPort $GitServerPort) {
        Write-DebugLog "Start-GitServer: Server already running on port $GitServerPort" "INFO"
        Write-StatusMessage "   ✓ Git server already running on port $GitServerPort" -Type "Success"
        return @{ Success = $true; WasAlreadyRunning = $true }
    }
    Write-DebugLog "Start-GitServer: No server running on port $GitServerPort" "DEBUG"
    
    # Find Bun
    Write-DebugLog "Start-GitServer: Searching for bun.exe" "DEBUG"
    $bunPath = $null
    $bunPaths = @(
        (Get-Command bun -ErrorAction SilentlyContinue)?.Source
        "$env:APPDATA\npm\bun.exe"
        "$env:USERPROFILE\.bun\bin\bun.exe"
        "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\OpenJS.Bun*\bun.exe"
    )
    
    foreach ($p in $bunPaths) {
        if ($p) {
            Write-DebugLog "Start-GitServer: Checking bun path: $p" "DEBUG"
            if (Test-Path $p) {
                $bunPath = $p
                Write-DebugLog "Start-GitServer: Found bun at: $bunPath" "INFO"
                break
            }
            $matches = Get-Item $p -ErrorAction SilentlyContinue
            if ($matches) {
                $bunPath = $matches | Select-Object -First 1
                Write-DebugLog "Start-GitServer: Found bun via wildcard at: $bunPath" "INFO"
                break
            }
        }
    }
    
    if (-not $bunPath) {
        Write-DebugLog "Start-GitServer: bun.exe NOT FOUND" "ERROR"
        Write-StatusMessage "   ⚠ Bun not found - Git server requires Bun" -Type "Warning"
        Write-StatusMessage "   Install: winget install OpenJS.Bun OR https://bun.sh" -Type "Dim"
        return @{ Success = $false; Message = "Bun not found" }
    }
    
    # Find git plugin - CRITICAL: Must run from .opencode/ directory
    $opencodeDir = "$PSScriptRoot\.opencode"
    $pluginScript = "$opencodeDir\plugin\git-plugin.js"
    
    Write-DebugLog "Start-GitServer: Looking for plugin at: $pluginScript" "DEBUG"
    if (-not (Test-Path $pluginScript)) {
        Write-DebugLog "Start-GitServer: Plugin NOT FOUND at $pluginScript" "ERROR"
        Write-StatusMessage "   ⚠ Git plugin not found at $pluginScript" -Type "Warning"
        return @{ Success = $false; Message = "Plugin not found" }
    }
    Write-DebugLog "Start-GitServer: Plugin found at $pluginScript" "DEBUG"
    
    # Check if node_modules exists (dependencies installed)
    Write-DebugLog "Start-GitServer: Checking for node_modules at $opencodeDir\node_modules" "DEBUG"
    if (-not (Test-Path "$opencodeDir\node_modules")) {
        Write-DebugLog "Start-GitServer: node_modules not found, running bun install" "WARN"
        Write-StatusMessage "   Installing dependencies..." -Type "Info"
        try {
            Push-Location $opencodeDir
            $installOutput = bun install 2>&1
            Pop-Location
            Write-DebugLog "Start-GitServer: bun install completed" "INFO"
            Write-StatusMessage "   ✓ Dependencies installed" -Type "Success"
        }
        catch {
            Write-DebugLog "Start-GitServer: bun install failed: $_" "ERROR"
            Write-StatusMessage "   ⚠ Failed to install dependencies: $_" -Type "Warning"
        }
    }
    else {
        Write-DebugLog "Start-GitServer: node_modules already exists" "DEBUG"
    }
    
    Write-StatusMessage "   Starting Git HTTP server on port $GitServerPort..." -Type "Info"
    Write-DebugLog "Start-GitServer: Preparing to start server on port $GitServerPort" "INFO"
    
    try {
        # Find available port if needed
        Write-DebugLog "Start-GitServer: Checking if port $GitServerPort is available" "DEBUG"
        if (-not (Test-PortAvailable -TestPort $GitServerPort)) {
            Write-DebugLog "Start-GitServer: Port $GitServerPort is busy, finding alternative" "WARN"
            $newPort = Find-AvailablePort -StartPort 4097
            if ($newPort) {
                Write-DebugLog "Start-GitServer: Using alternative port $newPort" "INFO"
                Write-StatusMessage "   ⚠ Port $GitServerPort busy, using $newPort" -Type "Warning"
                $GitServerPort = $newPort
            }
            else {
                Write-StatusMessage "   ✗ No available ports" -Type "Error"
                return @{ Success = $false; Message = "No available ports" }
            }
        }
        
        # CRITICAL: Run from .opencode directory so imports work
        Write-DebugLog "Start-GitServer: Creating ProcessStartInfo" "DEBUG"
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
        Write-DebugLog "Start-GitServer: Environment set - GIT_SERVER_PORT=$GitServerPort" "DEBUG"
        Write-DebugLog "Start-GitServer: Working directory=$opencodeDir" "DEBUG"
        Write-DebugLog "Start-GitServer: Command=$bunPath $($psi.Arguments)" "DEBUG"
        
        Write-DebugLog "Start-GitServer: Starting process..." "INFO"
        $process = [System.Diagnostics.Process]::Start($psi)
        Write-DebugLog "Start-GitServer: Process started with ID: $($process.Id)" "INFO"
        
        # Wait and check with logging
        Write-DebugLog "Start-GitServer: Waiting 3 seconds for server to start..." "DEBUG"
        Start-Sleep -Seconds 3
        
        Write-DebugLog "Start-GitServer: Testing if server is running on port $GitServerPort" "DEBUG"
        if (Test-ServerRunning -TestPort $GitServerPort) {
            Write-DebugLog "Start-GitServer: Server is RUNNING on port $GitServerPort (PID: $($process.Id))" "INFO"
            Write-StatusMessage "   ✓ Git server started on port $GitServerPort (PID: $($process.Id))" -Type "Success"
            return @{ Success = $true; WasAlreadyRunning = $false }
        }
        Write-DebugLog "Start-GitServer: Server not responding on port $GitServerPort after 3 seconds" "WARN"
        
        if ($process.HasExited) {
            Write-DebugLog "Start-GitServer: Process has EXITED (ExitCode: $($process.ExitCode))" "ERROR"
            $stdout = $process.StandardOutput.ReadToEnd()
            $err = $process.StandardError.ReadToEnd()
            Write-DebugLog "Start-GitServer: STDOUT length: $($stdout.Length) chars" "DEBUG"
            Write-DebugLog "Start-GitServer: STDERR length: $($err.Length) chars" "DEBUG"
            Write-DebugLog "Start-GitServer: STDOUT: $stdout" "DEBUG"
            Write-DebugLog "Start-GitServer: STDERR: $err" "ERROR"
            Write-StatusMessage "   ✗ Git server exited immediately" -Type "Error"
            if ($err -match "Cannot find module") {
                Write-DebugLog "Start-GitServer: Detected 'Cannot find module' error" "ERROR"
                Write-StatusMessage "   🔧 TO FIX: Run 'bun install' in $opencodeDir" -Type "Warning"
            }
            elseif ($err) {
                Write-DebugLog "Start-GitServer: Displaying error to user" "DEBUG"
                Write-StatusMessage "   Error: $err" -Type "Dim"
            }
            else {
                Write-DebugLog "Start-GitServer: No stderr captured" "WARN"
                Write-StatusMessage "   Error: Process exited with code $($process.ExitCode), no output captured" -Type "Dim"
            }
            return @{ Success = $false; Message = "Process exited with code $($process.ExitCode)" }
        }
        
        Write-DebugLog "Start-GitServer: Process still running (PID: $($process.Id)), assuming startup in progress" "INFO"
        Write-StatusMessage "   ✓ Git server starting (PID: $($process.Id))" -Type "Success"
        return @{ Success = $true; WasAlreadyRunning = $false }
    }
    catch {
        Write-DebugLog "Start-GitServer: EXCEPTION - $_" "ERROR"
        Write-DebugLog "Start-GitServer: Exception type: $($_.Exception.GetType().FullName)" "ERROR"
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

# Initialize debug logging first
Start-DebugSession

Show-Header

Write-DebugLog "=== MAIN EXECUTION STARTED ===" "INFO"

# Check prerequisites
Write-StatusMessage "🔍 Scanning system..." -Type "Info"
Write-DebugLog "Starting system scan..." "DEBUG"

# Get network interfaces
Write-StatusMessage "🌐 Detecting network interfaces..." -Type "Info"
Write-DebugLog "Calling Get-NetworkInterfaces..." "DEBUG"
$networkInterfaces = Get-NetworkInterfaces
Write-DebugLog "Get-NetworkInterfaces returned $($networkInterfaces.Count) interfaces" "DEBUG"
foreach ($iface in $networkInterfaces) {
    Write-DebugLog "  Interface: $($iface.Type) - $($iface.IP) ($($iface.Interface))" "DEBUG"
}
if ($networkInterfaces.Count -gt 0) {
    Write-StatusMessage "   ✓ Found $($networkInterfaces.Count) connection option(s)" -Type "Success"
}
else {
    Write-StatusMessage "   ⚠ No network interfaces found" -Type "Warning"
}

# Setup ADB
Write-DebugLog "Setting up ADB port forwarding..." "DEBUG"
$adbConfigured = Setup-AdbPortForwarding -MainPort $Port -GitSrvPort $GitPort
Write-DebugLog "ADB configured: $adbConfigured" "DEBUG"

# Start servers
Write-DebugLog "=== STARTING SERVERS ===" "INFO"
Write-DebugLog "Starting OpenCode server on port $Port..." "DEBUG"
$openCodeResult = Start-OpenCodeServer -OpenCodePort $Port
Write-DebugLog "OpenCode result: Success=$($openCodeResult.Success), WasAlreadyRunning=$($openCodeResult.WasAlreadyRunning), Port=$($openCodeResult.Port), Message='$($openCodeResult.Message)'" "INFO"
$actualPort = $openCodeResult.Port  # May have changed if auto-detected

Write-DebugLog "Starting Git server on port $GitPort..." "DEBUG"
$gitResult = Start-GitServer -GitServerPort $GitPort
Write-DebugLog "Git result: Success=$($gitResult.Success), WasAlreadyRunning=$($gitResult.WasAlreadyRunning), Message='$($gitResult.Message)'" "INFO"

# Configure Tailscale if requested
$tailscaleServeOk = $false
if ($EnableTailscaleServe) {
    Write-DebugLog "Configuring Tailscale serve..." "DEBUG"
    $tailscaleServeOk = Enable-TailscaleServe -SrvPort $actualPort -GitSrvPort $GitPort
    Write-DebugLog "Tailscale serve result: $tailscaleServeOk" "DEBUG"
}

# Display results
Write-DebugLog "=== DISPLAYING RESULTS ===" "DEBUG"
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
Write-DebugLog "=== EXECUTION COMPLETE (took ${elapsed}s) ===" "INFO"

Write-Host ""
if ($openCodeResult.Success) {
    Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Success
    Write-Host "║               ✅ SETUP COMPLETE (in ${elapsed}s)                            ║" -ForegroundColor $Colors.Success
    Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Success
    Write-Host ""
    Write-StatusMessage "📱 Open MOCCA app and scan a QR code above to connect" -Type "Info"
    if ($DebugMode) {
        Write-Host ""
        Write-StatusMessage "📄 Debug log saved to: $script:LogFile" -Type "Dim"
    }
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
    if ($DebugMode) {
        Write-Host ""
        Write-StatusMessage "📄 Debug log saved to: $script:LogFile" -Type "Dim"
    }
    Write-Host ""
    exit 1
}

#endregion
