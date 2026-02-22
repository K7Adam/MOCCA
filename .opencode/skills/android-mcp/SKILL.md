---
name: android-mcp
description: MCP server for Android device automation via ADB. Use when controlling Android emulators or physical devices, automating UI interactions, capturing screenshots, running ADB commands, or testing Android apps. Provides tools for clicks, swipes, typing, screenshots, and shell access.
related_skills:
  - kotlin-best-practices
  - taste-skill-compose
---

# Android-MCP Skill

Android-MCP is a lightweight MCP server that bridges AI agents with Android devices. It enables LLM agents to perform real-world tasks like app navigation, UI interaction, and automated QA testing through ADB and the Android Accessibility API.

## Prerequisites

- Python 3.13+
- ADB (Android Debug Bridge) installed and in PATH
- UV Package Manager (`curl -LsSf https://astral.sh/uv/install.sh | sh`)
- Android 10+ device or emulator

## Configuration

### OpenCode Configuration

Add to `opencode.json`:

```json
{
  "mcp": {
    "android-mcp": {
      "type": "local",
      "command": ["uvx", "android-mcp"],
      "enabled": true
    }
  }
}
```

### With Specific Device

```json
{
  "mcp": {
    "android-mcp": {
      "type": "local",
      "command": ["uvx", "android-mcp", "--device", "emulator-5554"],
      "enabled": true
    }
  }
}
```

---

## Device Selection Protocol (CRITICAL)

### The Multi-Device Problem

Android-MCP connects to ONE device at a time. When multiple devices are connected, the server throws an error.

### Mandatory Device Check

**ALWAYS** check connected devices before using Android-MCP tools:

```bash
adb devices
```

Expected output:
```
List of devices attached
emulator-5554   device
```

### Device Selection Logic

```
IF user specified a device (emulator or physical):
  → Configure android-mcp with --device flag
  
IF only one device connected:
  → Use it (android-mcp auto-connects to first available)
  
IF multiple devices connected AND user did not specify:
  → ASK USER which device to use
  → List all devices: adb devices -l
  
IF no devices connected:
  → Check emulator status: adb devices
  → Start emulator if needed
  → Or ask user to connect physical device
```

### Device Serial Formats

| Device Type | Serial Format | Example |
|-------------|---------------|---------|
| Emulator | `emulator-XXXX` | `emulator-5554` |
| Physical USB | Alphanumeric serial | `ABC123DEF456` |
| Physical TCP/IP | `IP:PORT` | `192.168.1.100:5555` |

### Common Device Commands

```bash
# List all devices with details
adb devices -l

# Connect to TCP/IP device
adb connect 192.168.1.100:5555

# Disconnect specific device
adb disconnect 192.168.1.100:5555

# Kill ADB server (resets all connections)
adb kill-server && adb start-server
```

---

## Available Tools

### 1. Snapshot

Get device state with optional screenshot.

**Parameters:**
- `use_vision` (bool, optional): Include screenshot. Default: `false`

**Returns:**
- Tree state string (UI hierarchy)
- Screenshot (PNG) if `use_vision=True`

**Usage:**
```
# Get UI hierarchy only
Snapshot(use_vision=False)

# Get UI hierarchy + screenshot
Snapshot(use_vision=True)
```

**Best Practice:** Always call `Snapshot` first to understand current screen state before interacting.

---

### 2. Click

Click at specific coordinates.

**Parameters:**
- `x` (int): X coordinate
- `y` (int): Y coordinate

**Returns:** `Clicked on (x,y)`

**Usage:**
```
Click(x=500, y=800)
```

**Tip:** Use `Snapshot` to find coordinates or text to click.

---

### 3. LongClick

Long press at specific coordinates.

**Parameters:**
- `x` (int): X coordinate
- `y` (int): Y coordinate

**Returns:** `Long Clicked on (x,y)`

**Usage:**
```
LongClick(x=500, y=800)
```

**Use Cases:**
- Context menus
- Widget configuration
- Text selection

---

### 4. Swipe

Swipe from one point to another.

**Parameters:**
- `x1` (int): Start X coordinate
- `y1` (int): Start Y coordinate
- `x2` (int): End X coordinate
- `y2` (int): End Y coordinate

**Returns:** `Swiped from (x1,y1) to (x2,y2)`

**Usage:**
```
# Scroll down
Swipe(x1=500, y1=1500, x2=500, y2=500)

# Scroll up
Swipe(x1=500, y1=500, x2=500, y2=1500)
```

---

### 5. Drag

Drag from one location and drop at another.

**Parameters:**
- `x1` (int): Start X coordinate
- `y1` (int): Start Y coordinate
- `x2` (int): End X coordinate
- `y2` (int): End Y coordinate

**Returns:** `Dragged from (x1,y1) and dropped on (x2,y2)`

**Usage:**
```
Drag(x1=200, y1=500, x2=800, y2=500)
```

**Use Cases:**
- Reordering items
- Moving UI elements
- Drag-and-drop operations

---

### 6. Type

Type text at a specific location.

**Parameters:**
- `text` (str): Text to type
- `x` (int): X coordinate to tap before typing
- `y` (int): Y coordinate to tap before typing
- `clear` (bool, optional): Clear existing text before typing. Default: `false`

**Returns:** `Typed "text" on (x,y)`

**Usage:**
```
# Type without clearing
Type(text="Hello World", x=500, y=800, clear=False)

# Replace existing text
Type(text="New Text", x=500, y=800, clear=True)
```

---

### 7. Press

Press hardware/software buttons.

**Parameters:**
- `button` (str): Button name

**Returns:** `Pressed the "button" button`

**Available Buttons:**
| Button | Description |
|--------|-------------|
| `BACK` | Navigate back |
| `HOME` | Go to home screen |
| `MENU` | Open menu |
| `POWER` | Power button |
| `VOLUME_UP` | Volume up |
| `VOLUME_DOWN` | Volume down |
| `VOLUME_MUTE` | Mute volume |
| `CAMERA` | Camera button |
| `DPAD_UP/DOWN/LEFT/RIGHT` | Navigation pad |
| `ENTER` | Confirm/Enter |
| `TAB` | Tab key |

**Usage:**
```
Press(button="BACK")
Press(button="HOME")
```

---

### 8. Notification

Access the notification panel.

**Parameters:** None

**Returns:** `Accessed notification bar`

**Usage:**
```
Notification()
```

**Note:** Opens notification shade. Use `Press(button="BACK")` to close.

---

### 9. Wait

Pause execution for specified duration.

**Parameters:**
- `duration` (int): Wait time in seconds

**Returns:** `Waited for {duration} seconds`

**Usage:**
```
Wait(duration=2)
```

**Use Cases:**
- Waiting for app to load
- Animation completion
- Network operations

---

## ADB Commands Reference

### Device Management

```bash
# List connected devices
adb devices

# List with details
adb devices -l

# Connect to TCP/IP device
adb connect <IP>:<PORT>

# Disconnect device
adb disconnect <IP>:<PORT>

# Restart ADB server
adb kill-server && adb start-server
```

### App Management

```bash
# Install APK
adb install <path/to/app.apk>

# Install with permissions granted
adb install -g <path/to/app.apk>

# Uninstall app
adb uninstall <package.name>

# Clear app data
adb shell pm clear <package.name>

# List all packages
adb shell pm list packages

# List packages filtered
adb shell pm list packages | grep <keyword>
```

### File Operations

```bash
# Push file to device
adb push <local_path> /sdcard/<remote_path>

# Pull file from device
adb pull /sdcard/<remote_path> <local_path>

# Pull entire directory
adb pull /sdcard/DCIM/ ./backup/
```

### Logcat & Debugging

```bash
# Real-time logs
adb logcat

# Filter by tag
adb logcat -s <TAG>

# Filter by package
adb logcat --pid=$(adb shell pidof -s <package.name>)

# Clear logcat buffer
adb logcat -c

# Dump to file
adb logcat -d > logcat.txt
```

### Screen Capture

```bash
# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Record screen (max 3 minutes)
adb shell screenrecord /sdcard/video.mp4
# Ctrl+C to stop
adb pull /sdcard/video.mp4
```

### Input Simulation

```bash
# Tap at coordinates
adb shell input tap <x> <y>

# Swipe
adb shell input swipe <x1> <y1> <x2> <y2> <duration_ms>

# Type text
adb shell input text "Hello"

# Key event
adb shell input keyevent <keycode>
# Common keycodes: 3=HOME, 4=BACK, 24=VOLUME_UP, 25=VOLUME_DOWN
```

### Shell Commands

```bash
# Interactive shell
adb shell

# Run single command
adb shell <command>

# Get device properties
adb shell getprop

# Get specific property
adb shell getprop ro.build.version.sdk
```

### Intents

```bash
# Start activity
adb shell am start -n <package>/<activity>

# Start with action
adb shell am start -a android.intent.action.VIEW -d <URL>

# Start service
adb shell am startservice -n <package>/<service>

# Broadcast
adb shell am broadcast -a <action>
```

### Emulator Commands

```bash
# List running emulators
adb devices | grep emulator

# Connect to emulator console
telnet localhost 5554

# Emulator control (from console)
power ac off    # Simulate battery
sms send 5556667777 "Hello"  # Send SMS
geo fix 12.34 56.78  # Set GPS location
```

---

## Workflows

### 1. Basic App Testing

```
1. Check device: adb devices
2. Get state: Snapshot(use_vision=True)
3. Navigate: Click/Swipe/Press
4. Verify: Snapshot(use_vision=True)
5. Repeat steps 3-4
```

### 2. Form Filling

```
1. Get screen: Snapshot(use_vision=True)
2. For each field:
   a. Click on field: Click(x, y)
   b. Type text: Type(text="value", x=x, y=y, clear=True)
3. Submit: Click on submit button
4. Verify: Snapshot(use_vision=False)
```

### 3. Scroll & Search

```
1. Get initial state: Snapshot(use_vision=False)
2. Check if element exists in tree
3. If not found:
   a. Swipe down: Swipe(x1=500, y1=1500, x2=500, y2=500)
   b. Wait: Wait(duration=1)
   c. Get new state: Snapshot(use_vision=False)
   d. Repeat from step 2
```

### 4. App Installation & Launch

```
1. Check device: adb devices
2. Install: adb install -g app.apk
3. Launch: adb shell am start -n <package>/<activity>
4. Wait for load: Wait(duration=3)
5. Verify launch: Snapshot(use_vision=True)
```

---

## Best Practices

### Do

- Always check `adb devices` before using Android-MCP
- Use `Snapshot` to understand screen state before interacting
- Add `Wait` between actions for animations/loading
- Use `use_vision=True` when visual verification is needed
- Clear text fields with `clear=True` before typing
- Handle back navigation with `Press(button="BACK")`

### Don't

- Don't assume device is connected - always verify
- Don't use hardcoded coordinates without verification
- Don't skip `Wait` during slow operations
- Don't forget to close notifications with `Press(button="BACK")`
- Don't use physical devices for automated testing without user consent

---

## Troubleshooting

### Device Not Found

```bash
# Check if device is connected
adb devices

# Restart ADB server
adb kill-server && adb start-server

# For emulators, verify emulator is running
adb devices | grep emulator
```

### Multiple Devices Error

```bash
# List all devices
adb devices

# Either:
# 1. Disconnect extra devices
adb disconnect <IP>:<PORT>

# 2. Or configure android-mcp with specific device
# Add --device flag to MCP config
```

### Permission Denied

```bash
# Check USB debugging is enabled
# Settings > Developer Options > USB Debugging

# Revoke and re-authorize USB debugging
adb kill-server
# Then reconnect device and authorize on device
```

### App Not Responding

```bash
# Check if app is running
adb shell pidof <package.name>

# Force stop app
adb shell am force-stop <package.name>

# Clear app data
adb shell pm clear <package.name>
```

### Screenshot Issues

```bash
# Verify screen is on
adb shell input keyevent 26  # Power button

# Check for screen lock
# Use Snapshot(use_vision=True) to see lock screen
```

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SCREENSHOT_QUANTIZED` | Reduce screenshot token usage | `false` |

---

## Related Resources

- [Android-MCP Repository](https://github.com/CursorTouch/Android-MCP)
- [ADB Official Documentation](https://developer.android.com/studio/command-line/adb)
- [Android Key Events](https://developer.android.com/reference/android/view/KeyEvent)
- [MOCCA Project AGENTS.md](../../../AGENTS.md) - For Android development patterns
