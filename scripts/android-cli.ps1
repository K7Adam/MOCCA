[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

$ErrorActionPreference = "Stop"
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

function Get-AndroidCliBundle {
    $cliHome = if ($env:ANDROID_CLI_HOME) { $env:ANDROID_CLI_HOME } else { Join-Path $env:USERPROFILE ".android\cli" }
    if (-not (Test-Path -LiteralPath $cliHome)) {
        return $null
    }

    $bundle = Get-ChildItem -LiteralPath (Join-Path $cliHome "bundles") -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "main.jar") } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $bundle) {
        return $null
    }

    $java = Join-Path $bundle.FullName "jre\bin\java.exe"
    if (-not (Test-Path -LiteralPath $java)) {
        $javaCommand = Get-Command java -ErrorAction SilentlyContinue
        if ($null -eq $javaCommand) {
            return $null
        }
        $java = $javaCommand.Source
    }

    [pscustomobject]@{
        CliHome = (Resolve-Path -LiteralPath $cliHome).Path
        BundleDir = $bundle.FullName
        Java = $java
        Jar = Join-Path $bundle.FullName "main.jar"
    }
}

function Get-AndroidLauncherPath {
    $cliHome = if ($env:ANDROID_CLI_HOME) { $env:ANDROID_CLI_HOME } else { Join-Path $env:USERPROFILE ".android\cli" }
    $binDir = Join-Path $cliHome "bin"
    New-Item -ItemType Directory -Force -Path $binDir | Out-Null
    Join-Path $binDir "android.exe"
}

$androidCommand = Get-Command android -ErrorAction SilentlyContinue
if ($null -ne $androidCommand -and $androidCommand.Source -and $androidCommand.Source -ne $PSCommandPath) {
    & $androidCommand.Source @Arguments
    exit $LASTEXITCODE
}

$launcherPath = Get-AndroidLauncherPath
if (Test-Path -LiteralPath $launcherPath) {
    & $launcherPath @Arguments
    exit $LASTEXITCODE
}

$bundle = Get-AndroidCliBundle
if ($null -eq $bundle) {
    Write-Error "Android CLI was not found on PATH and no usable bundle exists under $env:USERPROFILE\.android\cli. Install Android CLI from https://developer.android.com/tools/agents."
    exit 1
}

& $bundle.Java "-Dandroid.cli.dir=$($bundle.CliHome)" "-Dandroid.cli.exe=$launcherPath" -jar $bundle.Jar @Arguments
exit $LASTEXITCODE
