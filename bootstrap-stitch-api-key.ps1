#!/usr/bin/env pwsh
# Sync STITCH_API_KEY from Gemini Stitch extension into user environment.

param(
    [switch]$Quiet
)

$configPath = Join-Path $env:USERPROFILE ".gemini\extensions\stitch\gemini-extension-apikey.json"

if (-not (Test-Path $configPath)) {
    if (-not $Quiet) {
        Write-Host "Stitch Gemini extension config not found: $configPath" -ForegroundColor Yellow
    }
    exit 0
}

try {
    $json = Get-Content $configPath -Raw | ConvertFrom-Json
    $apiKey = $json.mcpServers.stitch.headers.'X-Goog-Api-Key'
}
catch {
    Write-Error "Failed to parse ${configPath}: $($_.Exception.Message)"
    exit 1
}

if (-not $apiKey -or $apiKey -eq "YOUR_API_KEY") {
    if (-not $Quiet) {
        Write-Host "No concrete Stitch API key found in Gemini extension config." -ForegroundColor Yellow
    }
    exit 0
}

[Environment]::SetEnvironmentVariable("STITCH_API_KEY", $apiKey, "User")

if (-not $Quiet) {
    Write-Host "STITCH_API_KEY synced to user environment (length=$($apiKey.Length))." -ForegroundColor Green
}
