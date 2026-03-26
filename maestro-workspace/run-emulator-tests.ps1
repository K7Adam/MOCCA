<#
Helper script to run Maestro tests deterministically on Android emulator.

Usage:
  .\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml
  .\run-emulator-tests.ps1 maestro-workspace/testplans/regression.yaml -DeviceId emulator-5554
  .\run-emulator-tests.ps1 maestro-workspace/testplans/smoke.yaml -BuildApk
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$TestPath,

    [string]$DeviceId,

    [switch]$BuildApk,

    [switch]$SkipInstall,

    [switch]$SkipClearState,

    [int]$BootTimeoutSeconds = 180,

    [string]$PackageName = "com.mocca.app"
)

$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath {
    param([string]$PathInput)

    if ([System.IO.Path]::IsPathRooted($PathInput)) {
        return (Resolve-Path $PathInput).Path
    }

    $repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
    $candidateFromRepo = Join-Path $repoRoot $PathInput
    if (Test-Path $candidateFromRepo) {
        return (Resolve-Path $candidateFromRepo).Path
    }

    if (Test-Path $PathInput) {
        return (Resolve-Path $PathInput).Path
    }

    throw "Path not found: $PathInput"
}

function Get-ConnectedEmulators {
    $lines = adb devices
    $devices = @()

    foreach ($line in $lines) {
        if ($line -match '^(emulator-\d+)\s+device$') {
            $devices += $matches[1]
        }
    }

    return $devices
}

function Wait-ForEmulatorBoot {
    param(
        [string]$Target,
        [int]$TimeoutSeconds
    )

    & adb -s $Target wait-for-device | Out-Null

    $started = Get-Date
    while ((Get-Date) -lt $started.AddSeconds($TimeoutSeconds)) {
        $bootCompleted = (& adb -s $Target shell getprop sys.boot_completed 2>$null).Trim()
        if ($bootCompleted -eq "1") {
            return
        }
        Start-Sleep -Seconds 2
    }

    throw "Emulator $Target did not boot within $TimeoutSeconds seconds"
}

Write-Host "== MOCCA Maestro Emulator Runner ==" -ForegroundColor Cyan

$null = adb start-server

$testPathResolved = Resolve-AbsolutePath -PathInput $TestPath
$repoRootResolved = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$defaultApkPath = Join-Path $repoRootResolved "androidApp\build\outputs\apk\debug\androidApp-debug.apk"

$emulators = Get-ConnectedEmulators
if ($emulators.Count -eq 0) {
    throw "No running emulator found. Start one with .\\maestro-workspace\\start-emulator.ps1 (visible) and retry."
}

$targetDevice = $DeviceId
if ([string]::IsNullOrWhiteSpace($targetDevice)) {
    $targetDevice = $emulators | Sort-Object | Select-Object -First 1
}

if (-not ($emulators -contains $targetDevice)) {
    throw "Target device '$targetDevice' is not an active emulator. Active: $($emulators -join ', ')"
}

Write-Host "Target emulator: $targetDevice"
Write-Host "Test plan/flow: $testPathResolved"

Wait-ForEmulatorBoot -Target $targetDevice -TimeoutSeconds $BootTimeoutSeconds
Write-Host "Emulator boot check passed"

if ($BuildApk) {
    Write-Host "Building debug APK..."
    Push-Location $repoRootResolved
    try {
        & .\gradlew.bat :androidApp:assembleDebug
    }
    finally {
        Pop-Location
    }
}

if (-not $SkipInstall) {
    if (-not (Test-Path $defaultApkPath)) {
        throw "Debug APK not found at $defaultApkPath. Build first or run with -BuildApk."
    }

    Write-Host "Installing APK: $defaultApkPath"
    & adb -s $targetDevice install -r $defaultApkPath | Out-Host
}

if (-not $SkipClearState) {
    Write-Host "Clearing app data for package $PackageName"
    & adb -s $targetDevice shell pm clear $PackageName | Out-Host
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$artifactDir = Join-Path $PSScriptRoot (".maestro\artifacts\" + $timestamp)
$null = New-Item -ItemType Directory -Force -Path $artifactDir
$logcatPath = Join-Path $artifactDir "logcat.txt"

Write-Host "Running Maestro..."
& adb -s $targetDevice logcat -c
& maestro --device $targetDevice test $testPathResolved
$maestroExitCode = $LASTEXITCODE

Write-Host "Collecting logcat: $logcatPath"
& adb -s $targetDevice logcat -d > $logcatPath

if ($maestroExitCode -ne 0) {
    Write-Host "Maestro failed. Last logcat lines:" -ForegroundColor Yellow
    Get-Content $logcatPath | Select-Object -Last 80
}

Write-Host "Artifacts directory: $artifactDir"
exit $maestroExitCode
