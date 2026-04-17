[CmdletBinding()]
param(
    [string]$RepoRoot,
    [switch]$CheckOnly,
    [switch]$SkipUpdate,
    [switch]$SkipSkills,
    [switch]$SkipDocsSmoke,
    [string]$DocsQuery = "Jetpack Compose state management"
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "== $Title =="
}

function New-CommandRecord {
    param(
        [string]$Name,
        [string]$Command,
        [int]$ExitCode,
        [string[]]$Output,
        [bool]$Skipped
    )

    [pscustomobject]@{
        name = $Name
        command = $Command
        exitCode = $ExitCode
        skipped = $Skipped
        output = $Output
    }
}

function Save-Status {
    $cacheDir = Join-Path $RepoRoot ".agents\cache"
    New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null
    $statusPath = Join-Path $cacheDir "android-agent-tooling-status.json"
    $script:Status | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $statusPath -Encoding UTF8
    Write-Host "Status written to $statusPath"
}

function Invoke-Android {
    param(
        [string]$Name,
        [string[]]$Arguments,
        [switch]$Optional,
        [switch]$ReadOnly
    )

    $commandText = "android " + ($Arguments -join " ")
    Write-Host ">> $commandText"

    if ($CheckOnly -and -not $ReadOnly) {
        $script:Status.commands += New-CommandRecord -Name $Name -Command $commandText -ExitCode 0 -Output @("Skipped because -CheckOnly was set.") -Skipped $true
        return @()
    }

    $output = @()
    $exitCode = 0

    try {
        $output = & $script:AndroidPath @Arguments 2>&1 | ForEach-Object { $_.ToString() }
        $exitCode = if ($null -eq $LASTEXITCODE) { 0 } else { $LASTEXITCODE }
    } catch {
        $output = @($_.Exception.Message)
        $exitCode = 1
    }

    $script:Status.commands += New-CommandRecord -Name $Name -Command $commandText -ExitCode $exitCode -Output $output -Skipped $false

    if ($exitCode -ne 0 -and -not $Optional) {
        throw "Command failed with exit code ${exitCode}: $commandText"
    }

    return $output
}

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
}

$resolvedRepoRoot = (Resolve-Path -LiteralPath $RepoRoot).Path
$wrapperPath = Join-Path $resolvedRepoRoot "scripts\android-cli.ps1"
$script:Status = [ordered]@{
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    repoRoot = $resolvedRepoRoot
    checkOnly = [bool]$CheckOnly
    androidCliFound = $false
    androidCliPath = $null
    androidVersion = $null
    commands = @()
    notes = @()
}

Write-Section "MOCCA Android agent tooling"
Write-Host "Repo root: $resolvedRepoRoot"

$androidCommand = Get-Command android -ErrorAction SilentlyContinue
if ($null -ne $androidCommand -and $androidCommand.Source) {
    $script:AndroidPath = $androidCommand.Source
} elseif (Test-Path -LiteralPath $wrapperPath) {
    $script:AndroidPath = $wrapperPath
} else {
    $script:AndroidPath = $null
}

if ($null -eq $script:AndroidPath) {
    $script:Status.notes += "Android CLI was not found on PATH and the repo wrapper is missing. Install it from https://developer.android.com/tools/agents, then rerun this script."
    $script:Status.notes += "Android CLI 0.7 documents the android emulator command as disabled on Windows; use MOCCA maestro-workspace scripts for emulator startup."
    Save-Status

    Write-Warning "Android CLI was not found on PATH."
    Write-Host "Official entry point: https://developer.android.com/tools/agents"
    Write-Host "After installation, rerun this script without -CheckOnly."

    if ($CheckOnly) {
        exit 0
    }

    exit 1
}

$script:Status.androidCliFound = $true
$script:Status.androidCliPath = $script:AndroidPath
Write-Host "Android CLI: $script:AndroidPath"

Write-Section "Version"
$versionOutput = Invoke-Android -Name "version" -Arguments @("--version") -Optional -ReadOnly
$script:Status.androidVersion = ($versionOutput -join "`n").Trim()

if (-not $SkipUpdate) {
    Write-Section "Update Android CLI"
    Invoke-Android -Name "update" -Arguments @("update") -Optional
}

Write-Section "Initialize Android CLI skill"
Invoke-Android -Name "init" -Arguments @("init") -Optional

if (-not $SkipSkills) {
    Write-Section "Install official Android skills"
    Invoke-Android -Name "skills-list-before" -Arguments @("skills", "list", "--long") -Optional -ReadOnly
    Invoke-Android -Name "skills-add-all" -Arguments @("skills", "add", "--all") -Optional
    Invoke-Android -Name "skills-list-after" -Arguments @("skills", "list", "--long") -Optional -ReadOnly
}

Write-Section "Describe MOCCA project"
$describeOutput = Invoke-Android -Name "describe" -Arguments @("describe", "--project_dir=$resolvedRepoRoot") -Optional -ReadOnly
if (($describeOutput -join "`n") -match "gradlew") {
    $script:Status.notes += "android describe currently fails for this Windows workspace because Android CLI invokes extensionless gradlew instead of gradlew.bat. Use Gradle/Maestro repo commands until Android CLI fixes that Windows behavior."
}

if (-not $SkipDocsSmoke) {
    Write-Section "Android Knowledge Base smoke check"
    $docsOutput = Invoke-Android -Name "docs-search" -Arguments @("docs", "search", $DocsQuery) -Optional -ReadOnly
    $cacheDir = Join-Path $resolvedRepoRoot ".agents\cache"
    New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null
    $docsPath = Join-Path $cacheDir "android-docs-search-smoke.txt"
    $docsOutput | Set-Content -LiteralPath $docsPath -Encoding UTF8
    $script:Status.notes += "Knowledge Base search output written to $docsPath"
}

Save-Status
Write-Host "Android agent tooling bootstrap finished."
