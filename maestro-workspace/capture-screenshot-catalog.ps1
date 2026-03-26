param(
    [string]$Device = "emulator-5554"
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$outputDir = Join-Path $projectRoot "screenshots"

if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$deviceLine = adb devices | Select-String ("^" + [regex]::Escape($Device) + "\s+device$")
if (-not $deviceLine) {
    throw "Target device '$Device' is not online. Start emulator first with: .\\maestro-workspace\\start-emulator.ps1"
}

$flowPaths = @(
    (Join-Path $projectRoot "maestro-workspace/flows/catalog/capture_panels.yaml"),
    (Join-Path $projectRoot "maestro-workspace/flows/catalog/capture_sessions_chat.yaml"),
    (Join-Path $projectRoot "maestro-workspace/flows/catalog/capture_files_git_terminal.yaml"),
    (Join-Path $projectRoot "maestro-workspace/flows/catalog/capture_settings_skills_flags.yaml"),
    (Join-Path $projectRoot "maestro-workspace/flows/catalog/capture_onboarding_connection.yaml"),
    (Join-Path $projectRoot "maestro-workspace/flows/catalog/capture_mcp.yaml")
)

Get-ChildItem -Path $projectRoot -Filter "catalog_*.png" -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem -Path $outputDir -Filter "catalog_*.png" -ErrorAction SilentlyContinue | Remove-Item -Force

$failedFlows = New-Object System.Collections.Generic.List[string]

foreach ($flowPath in $flowPaths) {
    Write-Host "Running flow: $flowPath"
    & maestro --device $Device test $flowPath
    if ($LASTEXITCODE -ne 0) {
        $failedFlows.Add($flowPath) | Out-Null
    }
}

$generatedShots = Get-ChildItem -Path $projectRoot -Filter "catalog_*.png" -ErrorAction SilentlyContinue
foreach ($shot in $generatedShots) {
    Move-Item -Path $shot.FullName -Destination (Join-Path $outputDir $shot.Name) -Force
}

Write-Host "Captured screenshots: $((Get-ChildItem -Path $outputDir -Filter 'catalog_*.png' -ErrorAction SilentlyContinue).Count)"

$capturedCount = (Get-ChildItem -Path $outputDir -Filter 'catalog_*.png' -ErrorAction SilentlyContinue).Count
if ($capturedCount -eq 0) {
    throw "No catalog screenshots were generated."
}

if ($failedFlows.Count -gt 0) {
    Write-Host "Flows with failures:"
    foreach ($failed in $failedFlows) {
        Write-Host " - $failed"
    }

    $warningFile = Join-Path $outputDir "capture_warnings.txt"
    $warningLines = @(
        "Capture completed with partial failures.",
        "Timestamp: $(Get-Date -Format o)",
        "Failed flows:"
    ) + ($failedFlows | ForEach-Object { " - $_" })
    Set-Content -Path $warningFile -Value $warningLines -Encoding UTF8
}
