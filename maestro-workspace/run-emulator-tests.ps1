# Helper script to run Maestro tests on the first available emulator
# Usage: .\run-emulator-tests.ps1 <path-to-test-or-plan>

param(
    [Parameter(Mandatory = $true)]
    [string]$TestPath
)

$devices = adb devices
$emulators = $devices | Select-String -Pattern "emulator-\d+" | ForEach-Object { $_.ToString().Split("`t")[0].Trim() }

if ($null -eq $emulators -or $emulators.Count -eq 0) {
    Write-Error "No emulators found. Please start an Android emulator first."
    exit 1
}

$targetDevice = $emulators | Select-Object -First 1
Write-Host "Targeting emulator: $targetDevice"

# Check if path exists
if (-not (Test-Path $TestPath)) {
    Write-Error "Test path not found: $TestPath"
    exit 1
}

# Run Maestro
maestro --device $targetDevice test $TestPath
