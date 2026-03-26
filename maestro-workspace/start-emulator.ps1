<#
Start MOCCA emulator for local agent-driven testing.

Default behavior is VISIBLE emulator window for manual observation.
Use -Headless only for CI-like local runs.

Usage:
  .\maestro-workspace\start-emulator.ps1
  .\maestro-workspace\start-emulator.ps1 -AvdName Pixel_9_Pro_XL
  .\maestro-workspace\start-emulator.ps1 -Headless
#>

param(
    [string]$AvdName = "Pixel_9_Pro_XL",
    [switch]$Headless,
    [int]$BootTimeoutSeconds = 300
)

$ErrorActionPreference = "Stop"

function Wait-ForDevice {
    param([int]$TimeoutSeconds)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $device = adb devices 2>$null | Select-String "emulator-\d+\s+device"
        }
        catch {
            Start-Sleep -Seconds 3
            continue
        }

        if ($device) {
            $serial = ($device.ToString().Split("`t")[0]).Trim()
            try {
                $boot = (& adb -s $serial shell getprop sys.boot_completed 2>$null).Trim()
            }
            catch {
                Start-Sleep -Seconds 3
                continue
            }
            if ($boot -eq "1") {
                Write-Host "Emulator online: $serial"
                return
            }
        }
        Start-Sleep -Seconds 3
    }

    throw "Emulator did not become ready within $TimeoutSeconds seconds"
}

function Initialize-WindowApi {
    Add-Type -AssemblyName System.Windows.Forms

    if ("MoccaWindowApi" -as [type]) {
        return
    }

    $windowApiDefinition = @"
using System;
using System.Runtime.InteropServices;
using System.Text;

public static class MoccaWindowApi {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc callback, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);

    [DllImport("user32.dll")]
    public static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool IsIconic(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowTextLength(IntPtr hWnd);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int maxCount);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetClassName(IntPtr hWnd, StringBuilder className, int maxCount);

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

    [DllImport("user32.dll")]
    public static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int X, int Y, int cx, int cy, uint flags);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool MoveWindow(IntPtr hWnd, int X, int Y, int nWidth, int nHeight, bool repaint);
}
"@

    Add-Type -TypeDefinition $windowApiDefinition -Language CSharp
}

function Get-WindowsForProcessId {
    param([uint32]$ProcessId)

    Initialize-WindowApi

    $windows = New-Object System.Collections.Generic.List[object]

    $callback = [MoccaWindowApi+EnumWindowsProc]{
        param([IntPtr]$hWnd, [IntPtr]$lParam)

        $windowProcessId = [uint32]0
        [MoccaWindowApi]::GetWindowThreadProcessId($hWnd, [ref]$windowProcessId) | Out-Null
        if ($windowProcessId -ne $ProcessId) {
            return $true
        }

        $titleLength = [MoccaWindowApi]::GetWindowTextLength($hWnd)
        $titleBuilder = New-Object System.Text.StringBuilder ($titleLength + 2)
        [MoccaWindowApi]::GetWindowText($hWnd, $titleBuilder, $titleBuilder.Capacity) | Out-Null

        $classBuilder = New-Object System.Text.StringBuilder 256
        [MoccaWindowApi]::GetClassName($hWnd, $classBuilder, $classBuilder.Capacity) | Out-Null

        $rect = New-Object MoccaWindowApi+RECT
        $hasRect = [MoccaWindowApi]::GetWindowRect($hWnd, [ref]$rect)

        $windows.Add([pscustomobject]@{
            Hwnd = $hWnd.ToInt64()
            Title = $titleBuilder.ToString()
            ClassName = $classBuilder.ToString()
            Visible = [MoccaWindowApi]::IsWindowVisible($hWnd)
            Minimized = [MoccaWindowApi]::IsIconic($hWnd)
            Rect = if ($hasRect) {
                [pscustomobject]@{
                    Left = $rect.Left
                    Top = $rect.Top
                    Right = $rect.Right
                    Bottom = $rect.Bottom
                    Width = $rect.Right - $rect.Left
                    Height = $rect.Bottom - $rect.Top
                }
            }
            else {
                $null
            }
        }) | Out-Null

        return $true
    }

    [MoccaWindowApi]::EnumWindows($callback, [IntPtr]::Zero) | Out-Null
    return $windows
}

function Test-IntersectsVirtualScreen {
    param([object]$Rect)

    if (-not $Rect) {
        return $false
    }

    $virtualScreen = [System.Windows.Forms.SystemInformation]::VirtualScreen

    return -not (
        $Rect.Right -le $virtualScreen.Left -or
        $Rect.Left -ge ($virtualScreen.Left + $virtualScreen.Width) -or
        $Rect.Bottom -le $virtualScreen.Top -or
        $Rect.Top -ge ($virtualScreen.Top + $virtualScreen.Height)
    )
}

function Ensure-EmulatorUiWindowVisible {
    param(
        [string]$TargetAvdName,
        [int]$EmulatorPid = 0,
        [int]$TimeoutSeconds = 45
    )

    Initialize-WindowApi

    $titlePattern = "^Android Emulator - $([regex]::Escape($TargetAvdName))(:\\d+)?$"
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $attempt = 0

    while ((Get-Date) -lt $deadline) {
        $attempt++
        $qemu = $null

        if ($EmulatorPid -gt 0) {
            $qemu = Get-CimInstance Win32_Process -Filter ("Name='qemu-system-x86_64.exe' AND ParentProcessId=" + $EmulatorPid) |
                Select-Object -First 1
        }

        if (-not $qemu) {
            $qemu = Get-CimInstance Win32_Process -Filter "Name='qemu-system-x86_64.exe'" |
                Select-Object -First 1
        }

        if (-not $qemu) {
            if (($attempt % 5) -eq 0) {
                Write-Host "Waiting for qemu process for AVD '$TargetAvdName'..."
            }
            Start-Sleep -Seconds 1
            continue
        }

        $windows = Get-WindowsForProcessId -ProcessId ([uint32]$qemu.ProcessId)
        $targetWindow = $windows |
            Where-Object {
                $_.Visible -and
                $_.ClassName -like 'Qt*QWindowIcon' -and
                ($_.Title -notlike 'Extended Controls*')
            } |
            Sort-Object { $_.Rect.Width * $_.Rect.Height } -Descending |
            Select-Object -First 1

        if (-not $targetWindow) {
            $targetWindow = $windows |
                Where-Object { $_.Title -match $titlePattern } |
                Select-Object -First 1
        }

        if (-not $targetWindow) {
            if (($attempt % 5) -eq 0) {
                Write-Host "Waiting for emulator UI window title for AVD '$TargetAvdName' (qemu pid=$($qemu.ProcessId))..."
            }
            Start-Sleep -Seconds 1
            continue
        }

        $targetHandle = [IntPtr]$targetWindow.Hwnd

        # Restore and force-move the emulator UI window to a known on-screen position.
        [MoccaWindowApi]::ShowWindow($targetHandle, 9) | Out-Null
        [MoccaWindowApi]::MoveWindow($targetHandle, 100, 100, 420, 760, $true) | Out-Null
        [MoccaWindowApi]::SetWindowPos($targetHandle, [IntPtr]::Zero, 100, 100, 0, 0, 0x0041) | Out-Null
        [MoccaWindowApi]::SetForegroundWindow($targetHandle) | Out-Null

        Start-Sleep -Milliseconds 400

        $postMoveState = Get-WindowsForProcessId -ProcessId ([uint32]$qemu.ProcessId) |
            Where-Object { $_.Hwnd -eq $targetWindow.Hwnd } |
            Select-Object -First 1

        if (-not $postMoveState) {
            Start-Sleep -Seconds 1
            continue
        }

        if (-not (Test-IntersectsVirtualScreen -Rect $postMoveState.Rect)) {
            [MoccaWindowApi]::MoveWindow($targetHandle, 100, 100, 420, 760, $true) | Out-Null
            Start-Sleep -Milliseconds 400

            $postMoveState = Get-WindowsForProcessId -ProcessId ([uint32]$qemu.ProcessId) |
                Where-Object { $_.Hwnd -eq $targetWindow.Hwnd } |
                Select-Object -First 1
        }

        $visibleOnScreen = $postMoveState -and
            $postMoveState.Visible -and
            (-not $postMoveState.Minimized) -and
            (Test-IntersectsVirtualScreen -Rect $postMoveState.Rect)

        if ($visibleOnScreen) {
            Write-Host "Emulator UI window restored on screen: hwnd=$($postMoveState.Hwnd), rect=$($postMoveState.Rect.Left),$($postMoveState.Rect.Top) $($postMoveState.Rect.Width)x$($postMoveState.Rect.Height)"
            return
        }

        Start-Sleep -Seconds 1
    }

    throw "Emulator UI window for AVD '$TargetAvdName' could not be made visible within $TimeoutSeconds seconds"
}

function Stop-ExistingEmulators {
    # First, request graceful shutdown for ADB-visible emulator instances.
    $running = adb devices | Select-String "emulator-\d+\s+device" | ForEach-Object { $_.ToString().Split("`t")[0].Trim() }
    foreach ($serial in $running) {
        adb -s $serial emu kill | Out-Null
    }

    if ($running.Count -gt 0) {
        Start-Sleep -Seconds 2
    }

    # Then, force-stop orphan/stale emulator launcher processes that are not ADB-visible.
    $leftoverProcesses = Get-Process -Name emulator -ErrorAction SilentlyContinue
    foreach ($process in $leftoverProcesses) {
        try {
            Stop-Process -Id $process.Id -Force -ErrorAction Stop
        }
        catch {
            Write-Warning "Could not stop stale emulator process PID $($process.Id): $($_.Exception.Message)"
        }
    }

    if ($leftoverProcesses.Count -gt 0) {
        Start-Sleep -Seconds 2
    }
}

function Reset-VisibleWindowPosition {
    param([string]$TargetAvdName)

    $userIniPath = Join-Path $env:USERPROFILE ".android\avd\$TargetAvdName.avd\emulator-user.ini"
    if (-not (Test-Path $userIniPath)) {
        return
    }

    $lines = Get-Content -Path $userIniPath
    $hasX = $false
    $hasY = $false
    $hasScale = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^window\.x\s*=') {
            $lines[$i] = 'window.x = 100'
            $hasX = $true
            continue
        }

        if ($lines[$i] -match '^window\.y\s*=') {
            $lines[$i] = 'window.y = 100'
            $hasY = $true
            continue
        }

        if ($lines[$i] -match '^window\.scale\s*=') {
            $lines[$i] = 'window.scale = 1.000000'
            $hasScale = $true
        }
    }

    if (-not $hasX) {
        $lines += 'window.x = 100'
    }
    if (-not $hasY) {
        $lines += 'window.y = 100'
    }
    if (-not $hasScale) {
        $lines += 'window.scale = 1.000000'
    }

    Set-Content -Path $userIniPath -Value $lines -Encoding Ascii
}

function Clear-CachedEmbeddedLaunchArgs {
    param([string]$TargetAvdName)

    $launchParamsPath = Join-Path $env:USERPROFILE ".android\avd\$TargetAvdName.avd\emu-launch-params.txt"
    if (-not (Test-Path $launchParamsPath)) {
        return
    }

    try {
        Remove-Item -Path $launchParamsPath -Force -ErrorAction Stop
    }
    catch {
        Write-Warning "Could not remove cached launch params at ${launchParamsPath}: $($_.Exception.Message)"
    }
}

$emulatorPath = Join-Path $env:LOCALAPPDATA "Android\Sdk\emulator\emulator.exe"
if (-not (Test-Path $emulatorPath)) {
    throw "emulator.exe not found at $emulatorPath"
}

adb start-server | Out-Null

Stop-ExistingEmulators

if (-not $Headless) {
    Clear-CachedEmbeddedLaunchArgs -TargetAvdName $AvdName
    Reset-VisibleWindowPosition -TargetAvdName $AvdName
}

$args = @("-avd", $AvdName, "-gpu", "swiftshader_indirect", "-no-boot-anim", "-no-audio")
if ($Headless) {
    $args += @("-no-window", "-no-snapshot")
}
else {
    $args += @("-no-snapshot-load")
}

$process = Start-Process -FilePath $emulatorPath -ArgumentList $args -WindowStyle Normal -PassThru
Write-Host "Emulator launch requested (visible=$([bool](-not $Headless))) for AVD: $AvdName (PID=$($process.Id))"

Wait-ForDevice -TimeoutSeconds $BootTimeoutSeconds

if (-not $Headless) {
    Ensure-EmulatorUiWindowVisible -TargetAvdName $AvdName -EmulatorPid $process.Id
}
