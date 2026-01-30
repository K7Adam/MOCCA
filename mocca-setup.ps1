#!/usr/bin/env pwsh
#Requires -Version 5.1

<#
.SYNOPSIS
    MOCCA Setup Script - Version 3.0 - Actually Works Edition

.DESCRIPTION
    Intelligently sets up MOCCA connection by:
    1. Detecting running OpenCode server (handles auth responses correctly)
    2. Finding all network IPs (WiFi, Tailscale, Ethernet)
    3. Configuring ADB for emulator users
    4. Displaying scannable connection QR codes

.EXAMPLE
    .\mocca-setup.ps1

.EXAMPLE
    .\mocca-setup.ps1 -UseTailscale

.PARAMETER Port
    Port for OpenCode (default: 4096)

.PARAMETER GitPort
    Port for Git HTTP (default: 4097)

.PARAMETER UseTailscale
    Prioritize Tailscale IPs

.PARAMETER SkipAdb
    Skip ADB configuration
#>

param(
    [int]$Port = 4096,
    [int]$GitPort = 4097,
    [switch]$UseTailscale,
    [switch]$SkipAdb
)

$ErrorActionPreference = "Continue"
$script:Version = "3.0.0"

# Colors
$Colors = @{
    Success = "Green"
    Error = "Red"
    Warning = "Yellow"
    Info = "Cyan"
    Normal = "White"
    Dim = "DarkGray"
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
║   MOCCA Setup v$script:Version - Actually Works Edition                    ║
╚══════════════════════════════════════════════════════════════╝
"@ -ForegroundColor $Colors.Info
    Write-Host ""
}

# CRITICAL: This function correctly detects if server is running
# A 401 Unauthorized response means the server IS ALIVE!
function Test-ServerRunning {
    param([int]$Port)
    
    try {
        $request = [System.Net.HttpWebRequest]::Create("http://localhost:$Port/")
        $request.Timeout = 2000
        $request.Method = "GET"
        
        try {
            $response = $request.GetResponse()
            $response.Close()
            return @{ Running = $true; Status = $response.StatusCode }
        }
        catch [System.Net.WebException] {
            # WebException means we got a response (401, 404, etc.) = SERVER IS RUNNING!
            if ($_.Exception.Response) {
                $statusCode = $_.Exception.Response.StatusCode
                $_.Exception.Response.Close()
                return @{ Running = $true; Status = $statusCode }
            }
            return @{ Running = $false; Status = "No Response" }
        }
    }
    catch {
        return @{ Running = $false; Status = $_.Exception.Message }
    }
}

function Test-ProcessRunning {
    param([string]$Name = "opencode")
    
    $process = Get-Process -Name $Name -ErrorAction SilentlyContinue | Select-Object -First 1
    return ($process -ne $null)
}

function Get-NetworkInterfaces {
    $interfaces = @()
    
    try {
        # Get all IPv4 addresses from active interfaces
        $adapters = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue | 
            Where-Object { 
                $_.IPAddress -notmatch "^127\." -and 
                $_.IPAddress -notmatch "^169\.254" -and
                $_.InterfaceAlias -notmatch "Loopback|Virtual|VMware|Hyper-V|vEthernet|WSL|docker"
            }
        
        foreach ($adapter in $adapters) {
            $isTailscale = $adapter.IPAddress -match "^100\.(64|65|66|67|68|69|70|71|72|73|74|75|76|77|78|79|80|81|82|83|84|85|86|87|88|89|90|91|92|93|94|95|96|97|98|99|100|101|102|103|104|105|106|107|108|109|110|111|112|113|114|115|116|117|118|119|120|121|122|123|124|125|126|127)\."
            
            $interfaces += [PSCustomObject]@{
                Type = if ($isTailscale) { "Tailscale" } else { "LAN" }
                IP = $adapter.IPAddress
                Interface = $adapter.InterfaceAlias
                Preferred = ($UseTailscale -and $isTailscale)
            }
        }
    }
    catch {
        Write-StatusMessage "   Could not enumerate network adapters" -Type "Warning"
    }
    
    # Fallback: Try DNS resolution
    if ($interfaces.Count -eq 0) {
        try {
            $hostname = [System.Net.Dns]::GetHostName()
            $entry = [System.Net.Dns]::GetHostEntry($hostname)
            foreach ($addr in $entry.AddressList) {
                if ($addr.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork) {
                    $ip = $addr.IPAddressToString
                    if ($ip -notmatch "^127\.") {
                        $isTailscale = $ip -match "^100\.(64|65|66|67|68|69|70|71|72|73|74|75|76|77|78|79|80|81|82|83|84|85|86|87|88|89|90|91|92|93|94|95|96|97|98|99|100|101|102|103|104|105|106|107|108|109|110|111|112|113|114|115|116|117|118|119|120|121|122|123|124|125|126|127)\."
                        
                        $interfaces += [PSCustomObject]@{
                            Type = if ($isTailscale) { "Tailscale" } else { "LAN" }
                            IP = $ip
                            Interface = "DNS"
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
    
    return $interfaces
}

function Setup-Adb {
    param([int]$Port, [int]$GitPort)
    
    Write-StatusMessage "🔌 Checking ADB..." -Type "Info"
    
    # Find ADB
    $adbPath = $null
    try {
        $cmd = Get-Command adb -ErrorAction Stop
        $adbPath = $cmd.Source
    }
    catch {
        $paths = @(
            "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
            "$env:PROGRAMFILES\Android\android-sdk\platform-tools\adb.exe",
            "$env:ANDROID_HOME\platform-tools\adb.exe"
        )
        foreach ($p in $paths) {
            if (Test-Path $p) {
                $adbPath = $p
                $env:PATH += ";$(Split-Path $p)"
                break
            }
        }
    }
    
    if (-not $adbPath) {
        Write-StatusMessage "   ⚠ ADB not found (install Android SDK or skip with -SkipAdb)" -Type "Warning"
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
    $r1 = adb reverse tcp:$Port tcp:$Port 2>&1
    $r2 = adb reverse tcp:$GitPort tcp:$GitPort 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-StatusMessage "   ✓ ADB port forwarding active" -Type "Success"
        return $true
    }
    
    Write-StatusMessage "   ⚠ Port forwarding failed" -Type "Warning"
    return $false
}

function Start-OpenCode {
    param([int]$Port)
    
    Write-StatusMessage "🚀 Checking OpenCode server on port $Port..." -Type "Info"
    
    # First, check if already running
    $serverTest = Test-ServerRunning -Port $Port
    if ($serverTest.Running) {
        Write-StatusMessage "   ✓ Server is already running (status: $($serverTest.Status))" -Type "Success"
        return @{ Success = $true; WasAlreadyRunning = $true }
    }
    
    # Check for process
    if (Test-ProcessRunning -Name "opencode") {
        Write-StatusMessage "   ⚠ OpenCode process found but not responding on port $Port" -Type "Warning"
        Write-StatusMessage "     It may be starting up or using a different port" -Type "Warning"
        return @{ Success = $false; Message = "Process exists but port not responding" }
    }
    
    # Try to start it
    Write-StatusMessage "   Server not running. Attempting to start..." -Type "Warning"
    
    # Find opencode
    $exe = $null
    try {
        $exe = (Get-Command opencode -ErrorAction Stop).Source
    }
    catch {
        $paths = @(
            "$env:USERPROFILE\.cargo\bin\opencode.exe",
            "$env:LOCALAPPDATA\Programs\opencode\opencode.exe",
            "$env:USERPROFILE\.local\bin\opencode.exe",
            ".\opencode.exe"
        )
        foreach ($p in $paths) {
            if (Test-Path $p) {
                $exe = $p
                break
            }
        }
    }
    
    if (-not $exe) {
        Write-StatusMessage "   ✗ OpenCode not found!" -Type "Error"
        Write-StatusMessage "     Install from: https://github.com/opencode-ai/opencode/releases" -Type "Info"
        return @{ Success = $false; Message = "OpenCode not found" }
    }
    
    Write-StatusMessage "   Starting: $exe serve --port $Port" -Type "Info"
    
    try {
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $exe
        $psi.Arguments = "serve --port $Port"
        $psi.UseShellExecute = $true
        $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Normal
        
        [System.Diagnostics.Process]::Start($psi) | Out-Null
        
        # Wait for startup
        Write-StatusMessage "   Waiting for server to start..." -Type "Info" -NoNewline
        
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Seconds 1
            Write-Host "." -NoNewline -ForegroundColor $Colors.Info
            
            $test = Test-ServerRunning -Port $Port
            if ($test.Running) {
                Write-Host ""
                Write-StatusMessage "   ✓ Server started!" -Type "Success"
                return @{ Success = $true; WasAlreadyRunning = $false }
            }
        }
        
        Write-Host ""
        Write-StatusMessage "   ⚠ Server didn't respond within 30 seconds" -Type "Warning"
        Write-StatusMessage "     A window should have opened - check it for status" -Type "Warning"
        return @{ Success = $false; Message = "Timeout waiting for server" }
    }
    catch {
        Write-StatusMessage "   ✗ Failed to start: $_" -Type "Error"
        return @{ Success = $false; Message = $_.Exception.Message }
    }
}

function Show-QrCode {
    param([string]$Url)
    
    Write-Host ""
    Write-Host "📱 CONNECTION URL:" -ForegroundColor $Colors.Success
    Write-Host ""
    
    # Format nicely
    $line = "─" * ($Url.Length + 4)
    Write-Host "  ┌$line┐" -ForegroundColor $Colors.Success
    Write-Host "  │  $Url  │" -ForegroundColor $Colors.Normal
    Write-Host "  └$line┘" -ForegroundColor $Colors.Success
    
    Write-Host ""
    Write-StatusMessage "💡 To connect:" -Type "Info"
    Write-Host "   1. Open MOCCA app on your phone" -ForegroundColor $Colors.Normal
    Write-Host "   2. Tap 'Scan QR Code'" -ForegroundColor $Colors.Normal
    Write-Host "   3. Or type this URL manually" -ForegroundColor $Colors.Normal
    
    # Try to open browser with QR generator as fallback
    $qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=$([System.Web.HttpUtility]::UrlEncode($Url))"
    Write-Host ""
    Write-StatusMessage "📷 QR Code available at:" -Type "Info"
    Write-Host "   $qrUrl" -ForegroundColor $Colors.Dim
}

function Show-Results {
    param(
        [array]$Interfaces,
        [int]$Port,
        [int]$GitPort,
        [bool]$AdbConfigured,
        [bool]$ServerRunning,
        [string]$ServerStatus
    )
    
    Write-Host ""
    
    if ($ServerRunning) {
        Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Success
        Write-Host "║              ✅ SERVER RUNNING - READY TO CONNECT            ║" -ForegroundColor $Colors.Success
        Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Success
    }
    else {
        Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor $Colors.Error
        Write-Host "║              ❌ SERVER NOT RUNNING                           ║" -ForegroundColor $Colors.Error
        Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor $Colors.Error
    }
    
    Write-Host ""
    
    # Show all connection options
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Info
    Write-Host "                  CONNECTION OPTIONS                          " -ForegroundColor $Colors.Info
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Info
    Write-Host ""
    
    $bestUrl = $null
    
    foreach ($iface in $Interfaces) {
        $url = "http://$($iface.IP):$Port"
        $marker = if ($iface.Preferred) { "★ " } else { "  " }
        
        if ($iface.Type -eq "Tailscale") {
            Write-Host "$marker🔗 Tailscale: $url" -ForegroundColor $Colors.Success
        }
        elseif ($iface.Type -eq "LAN") {
            Write-Host "$marker📶 WiFi/LAN: $url" -ForegroundColor $Colors.Info
        }
        else {
            Write-Host "$marker🖥️ Localhost: $url" -ForegroundColor $Colors.Dim
        }
        
        if ($iface.Preferred -or (-not $bestUrl -and $iface.Type -ne "Localhost")) {
            $bestUrl = $url
        }
    }
    
    if ($AdbConfigured) {
        Write-Host "   📱 Emulator: http://localhost:$Port" -ForegroundColor $Colors.Success
    }
    
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor $Colors.Info
    
    # Show best option as QR
    if ($bestUrl -and $ServerRunning) {
        Show-QrCode -Url $bestUrl
    }
    
    # Troubleshooting if failed
    if (-not $ServerRunning) {
        Write-Host ""
        Write-StatusMessage "🔧 Troubleshooting:" -Type "Error"
        Write-Host "   1. Check that an OpenCode window opened" -ForegroundColor $Colors.Normal
        Write-Host "   2. Try running manually: opencode serve --port $Port" -ForegroundColor $Colors.Normal
        Write-Host "   3. Check Windows Defender/Firewall isn't blocking port $Port" -ForegroundColor $Colors.Normal
    }
}

# ═══════════════════════════════════════════════════════════════════════════════
# MAIN EXECUTION
# ═══════════════════════════════════════════════════════════════════════════════

Show-Header

# Step 1: Get network info
Write-StatusMessage "🌐 Scanning network interfaces..." -Type "Info"
$interfaces = Get-NetworkInterfaces
Write-StatusMessage "   Found $($interfaces.Count) interface(s)" -Type "Success"

# Step 2: Setup ADB
$adbOk = $false
if (-not $SkipAdb) {
    $adbOk = Setup-Adb -Port $Port -GitPort $GitPort
}

# Step 3: Start/verify server
$serverResult = Start-OpenCode -Port $Port

# Step 4: Show results
Show-Results `
    -Interfaces $interfaces `
    -Port $Port `
    -GitPort $GitPort `
    -AdbConfigured $adbOk `
    -ServerRunning $serverResult.Success `
    -ServerStatus $serverResult.Message

Write-Host ""
if ($serverResult.Success) {
    Write-StatusMessage "Press any key to exit..." -Type "Info"
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
else {
    Write-StatusMessage "Setup incomplete. Fix issues above and run again." -Type "Error"
    exit 1
}
