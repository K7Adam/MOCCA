#Requires -Version 7.0
<#
.SYNOPSIS
  Validates that AGENTS.md documentation files are in sync with the repository.

.DESCRIPTION
  Checks performed:
    1. Every backtick-quoted relative path in any AGENTS.md exists on disk.
    2. The `Commit:` hash in the root AGENTS.md is a valid git commit.
    3. The `Updated:` date in the root AGENTS.md is not older than 60 days (warning only).

  Exits with code 1 if any hard error is found, 0 otherwise.
  Warnings are printed to stderr but do not fail the check.

.PARAMETER RepoRoot
  Absolute path to the repository root. Defaults to the script's parent directory.

.PARAMETER MaxAgeDays
  Maximum allowed age in days for the root AGENTS.md `Updated:` field before a warning is emitted. Default 60.

.EXAMPLE
  pwsh -File scripts/check-docs-sync.ps1
#>
[CmdletBinding()]
param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [int]$MaxAgeDays = 60
)

$ErrorActionPreference = 'Stop'
Set-Location $RepoRoot

$errors = [System.Collections.Generic.List[string]]::new()
$warnings = [System.Collections.Generic.List[string]]::new()

# ---------------------------------------------------------------------------
# 1. Locate every AGENTS.md file in the repository.
# ---------------------------------------------------------------------------
$agentsFiles = Get-ChildItem -Path $RepoRoot -Filter 'AGENTS.md' -Recurse -File |
    Where-Object { $_.FullName -notmatch '[\\/]\.git[\\/]' }

if ($agentsFiles.Count -eq 0) {
    $errors.Add('No AGENTS.md files found in repository.')
}

# ---------------------------------------------------------------------------
# 2. Extract and verify backtick-quoted relative paths from each AGENTS.md.
# ---------------------------------------------------------------------------
# Path-like token inside backticks: starts with a path segment, may contain
# slashes, backslashes, dots, dashes, and a trailing extension or filename.
# We deliberately avoid matching URLs, commit hashes, or pure code snippets
# by requiring at least one path separator OR a known file extension.
$pathExtRegex = '\.(kt|kts|sq|sqm|xml|md|yaml|yml|json|ps1|sh|py|gradle|toml|properties|pro|txt)$'
$pathTokenRegex = '`([A-Za-z0-9._\-]+(?:[\\/][A-Za-z0-9._\-]+)+)`'

foreach ($file in $agentsFiles) {
    $relFile = $file.FullName.Substring($RepoRoot.Length).TrimStart('\', '/')
    $fileDir = $file.DirectoryName
    $content = Get-Content -Raw -Path $file.FullName
    $matches = [regex]::Matches($content, $pathTokenRegex)
    foreach ($m in $matches) {
        $token = $m.Groups[1].Value
        # Skip abbreviation patterns like `composeApp/.../ui/TestTags.kt`
        if ($token -match '\.\.\.') { continue }
        # Skip tokens that are clearly not file paths (no extension and no
        # obvious path component). We require either a known extension or
        # the path to start with a known top-level directory.
        $hasExt = [regex]::IsMatch($token, $pathExtRegex, 'IgnoreCase')
        $knownTopDirs = @('androidApp', 'composeApp', 'maestro-workspace', 'scripts', 'docs', 'conductor', '.github', '.agents', '.agent', '.opencode', '.devin')
        $startsKnown = $false
        foreach ($d in $knownTopDirs) {
            if ($token.StartsWith($d, 'OrdinalIgnoreCase')) { $startsKnown = $true; break }
        }
        if (-not $hasExt -and -not $startsKnown) { continue }

        # Normalize backslashes to forward slashes for cross-platform checks
        $normalized = $token -replace '\\', '/'
        # Try resolving relative to the AGENTS.md's own directory first,
        # then fall back to the repository root.
        $candidateLocal = Join-Path $fileDir $normalized
        $candidateRoot = Join-Path $RepoRoot $normalized
        if (-not (Test-Path -LiteralPath $candidateLocal) -and -not (Test-Path -LiteralPath $candidateRoot)) {
            $errors.Add("$relFile : referenced path does not exist: ``$token``")
        }
    }
}

# ---------------------------------------------------------------------------
# 3. Verify the root AGENTS.md `Commit:` hash is a valid git commit.
# ---------------------------------------------------------------------------
$rootAgents = Join-Path $RepoRoot 'AGENTS.md'
if (Test-Path $rootAgents) {
    $rootContent = Get-Content -Raw -Path $rootAgents
    $commitMatch = [regex]::Match($rootContent, '(?im)^\s*\*?\*?Commit:?\*?\*?\s*`?([0-9a-f]{7,40})`?\s*$')
    if ($commitMatch.Success) {
        $docCommit = $commitMatch.Groups[1].Value
        try {
            $null = git rev-parse --verify "$docCommit^{commit}" 2>$null
            if ($LASTEXITCODE -ne 0) {
                $errors.Add("Root AGENTS.md : Commit ``$docCommit`` is not a valid git commit.")
            }
        } catch {
            $errors.Add("Root AGENTS.md : Commit ``$docCommit`` could not be verified (git error).")
        }
    } else {
        $warnings.Add('Root AGENTS.md : no Commit: field found.')
    }

    # 4. Warn if the `Updated:` date is older than MaxAgeDays.
    $dateMatch = [regex]::Match($rootContent, '(?im)^\s*\*?\*?Updated:?\*?\*?\s*(\d{4}-\d{2}-\d{2})\s*$')
    if ($dateMatch.Success) {
        try {
            $updated = [datetime]::ParseExact($dateMatch.Groups[1].Value, 'yyyy-MM-dd', $null)
            $age = ([datetime]::Now - $updated).Days
            if ($age -gt $MaxAgeDays) {
                $warnings.Add("Root AGENTS.md : Updated date is $age days old (limit $MaxAgeDays).")
            }
        } catch {
            $warnings.Add("Root AGENTS.md : Updated date could not be parsed.")
        }
    } else {
        $warnings.Add('Root AGENTS.md : no Updated: field found.')
    }
}

# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------
if ($warnings.Count -gt 0) {
    [Console]::Error.WriteLine('WARNINGS (non-blocking):')
    foreach ($w in $warnings) { [Console]::Error.WriteLine("  - $w") }
}

if ($errors.Count -gt 0) {
    [Console]::Error.WriteLine('ERRORS (blocking):')
    foreach ($e in $errors) { [Console]::Error.WriteLine("  - $e") }
    Write-Error "Documentation sync check failed with $($errors.Count) error(s)."
    exit 1
}

Write-Host "Documentation sync check passed. ($($agentsFiles.Count) AGENTS.md files verified)"
exit 0
