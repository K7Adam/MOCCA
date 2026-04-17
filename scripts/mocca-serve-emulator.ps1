[CmdletBinding()]
param(
    [int]$Port = 41873,
    [string]$Hostname = "0.0.0.0",
    [switch]$Foreground,
    [switch]$Restart,
    [switch]$DryRun,
    [string]$LogDir
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "== $Title =="
}

function Get-NetworkConfigConstant {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Name
    )

    $content = Get-Content -LiteralPath $Path -Raw
    $pattern = 'const\s+val\s+' + [regex]::Escape($Name) + '\s*=\s*"([^"]*)"'
    $match = [regex]::Match($content, $pattern)
    if (-not $match.Success) {
        throw "Could not find NetworkConfig.$Name in $Path"
    }
    return $match.Groups[1].Value
}

function Test-Health {
    param(
        [Parameter(Mandatory = $true)][int]$Port,
        [Parameter(Mandatory = $true)][string]$Username,
        [Parameter(Mandatory = $true)][string]$Password
    )

    $pair = "${Username}:${Password}"
    $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
    Invoke-RestMethod `
        -Uri "http://127.0.0.1:$Port/global/health" `
        -Headers @{ Authorization = "Basic $encoded" } `
        -TimeoutSec 10
}

$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$NetworkConfigPath = Join-Path $RepoRoot "composeApp\src\commonMain\kotlin\com\mocca\app\api\NetworkConfig.kt"
$Username = Get-NetworkConfigConstant -Path $NetworkConfigPath -Name "DEFAULT_USERNAME"
$Password = Get-NetworkConfigConstant -Path $NetworkConfigPath -Name "DEFAULT_PASSWORD"

if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $RepoRoot ".agents\cache"
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$opencodeCommand = Get-Command opencode -ErrorAction SilentlyContinue
if ($null -eq $opencodeCommand) {
    $bunOpencode = Join-Path $env:USERPROFILE ".bun\bin\opencode.exe"
    if (Test-Path -LiteralPath $bunOpencode) {
        $opencodePath = $bunOpencode
    } else {
        throw "OpenCode CLI was not found on PATH and $bunOpencode does not exist."
    }
} else {
    $opencodePath = $opencodeCommand.Source
}

Write-Section "MOCCA emulator OpenCode server"
Write-Host "Repo:        $RepoRoot"
Write-Host "OpenCode:    $opencodePath"
Write-Host "Bind:        $Hostname`:$Port"
Write-Host "MOCCA host:  10.0.2.2"
Write-Host "MOCCA port:  $Port"
Write-Host "Username:    $Username"
Write-Host "Password:    from NetworkConfig.DEFAULT_PASSWORD"

$existingListeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
if ($existingListeners.Count -gt 0) {
    $owningProcesses = $existingListeners | Select-Object -ExpandProperty OwningProcess -Unique
    if ($Restart) {
        foreach ($processId in $owningProcesses) {
            $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
            if ($process -and $process.ProcessName -like "opencode*") {
                Write-Host "Stopping existing opencode process on port $Port (PID $processId)."
                Stop-Process -Id $processId -Force
            } else {
                throw "Port $Port is used by PID $processId, not an opencode process. Refusing to stop it."
            }
        }
        Start-Sleep -Seconds 2
    } else {
        Write-Section "Existing listener"
        $existingListeners | Select-Object LocalAddress, LocalPort, OwningProcess | Format-Table -AutoSize
        try {
            $health = Test-Health -Port $Port -Username $Username -Password $Password
            Write-Host "Existing server health: $($health | ConvertTo-Json -Compress)"
            exit 0
        } catch {
            throw "Port $Port is already in use and did not pass the MOCCA health check. Use -Restart only if it is an opencode process you want to replace."
        }
    }
}

$env:OPENCODE_SERVER_USERNAME = $Username
$env:OPENCODE_SERVER_PASSWORD = $Password
$serveArgs = @("serve", "--port", $Port, "--hostname", $Hostname, "--print-logs", "--log-level", "INFO")

Write-Section "Command"
Write-Host "$opencodePath $($serveArgs -join ' ')"

if ($DryRun) {
    Write-Section "Dry run"
    Write-Host "No server was started."
    exit 0
}

if ($Foreground) {
    Write-Section "Starting in foreground"
    & $opencodePath @serveArgs
    exit $LASTEXITCODE
}

$stdoutLog = Join-Path $LogDir "opencode-$Port.out.log"
$stderrLog = Join-Path $LogDir "opencode-$Port.err.log"
Remove-Item -LiteralPath $stdoutLog, $stderrLog -ErrorAction SilentlyContinue

Write-Section "Starting in background"
$process = Start-Process `
    -FilePath $opencodePath `
    -ArgumentList $serveArgs `
    -WorkingDirectory $RepoRoot `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

$deadline = (Get-Date).AddSeconds(30)
do {
    Start-Sleep -Milliseconds 500
    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
} while ($null -eq $listener -and (Get-Date) -lt $deadline -and -not $process.HasExited)

if ($process.HasExited) {
    Write-Host "OpenCode exited early. stderr:"
    if (Test-Path -LiteralPath $stderrLog) {
        Get-Content -LiteralPath $stderrLog -Tail 80
    }
    exit 1
}

if ($null -eq $listener) {
    throw "OpenCode did not start listening on port $Port within 30 seconds."
}

$health = Test-Health -Port $Port -Username $Username -Password $Password

Write-Section "Ready"
Write-Host "PID:         $($process.Id)"
Write-Host "Health:      $($health | ConvertTo-Json -Compress)"
Write-Host "Stdout log:  $stdoutLog"
Write-Host "Stderr log:  $stderrLog"
Write-Host ""
Write-Host "MOCCA manual connection:"
Write-Host "  Host:      10.0.2.2"
Write-Host "  Port:      $Port"
Write-Host "  Username:  $Username"
Write-Host "  HTTPS:     off"
