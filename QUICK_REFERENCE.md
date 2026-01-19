# Git Server Connection - Quick Reference

## Quick Setup

### 1. Start Emulator
```bash
emulator -avd <your_avd_name> -writable-system
```

### 2. Setup ADB Reverse (CRITICAL - Must Do This First!)
```bash
# Verify ADB is connected to emulator
adb devices
# Expected output: List of devices attached
# emulator-5554   device

# Set up ADB reverse for Git Server (port 4097)
adb reverse tcp:4097 tcp:4097

# Verify ADB reverse is active
adb reverse --list
# Expected output MUST include: tcp:4097 tcp:4097
```

**⚠️ If `adb reverse --list` does NOT show `tcp:4097`, Git operations WILL FAIL!**

**Common Issues & Fixes:**

| Issue | Symptom | Fix |
|--------|-----------|------|
| `adb devices` shows nothing | Emulator not running | Start emulator first |
| `adb reverse` hangs | Multiple emulators or devices | Close other devices/emulators |
| Port already in use | Error message | `adb reverse --remove-all` then retry |
| `reverse: not found` | ADB version too old | Update Android SDK Platform Tools |
| Connection fails after restart | ADB reverse lost | Re-run `adb reverse` command |

**Verification Commands:**
```bash
# Test from host (should work)
curl http://localhost:4097/health

# Test from emulator (should work with ADB reverse)
adb shell curl http://127.0.0.1:4097/health
# Expected: {"status":"ok"}

# Check if Git server process is running
netstat -an | findstr 4097  # Windows
netstat -an | grep 4097        # Linux/Mac
# Expected: LISTENING on port 4097
```

### 3. Start OpenCode Server
```bash
opencode serve --port 4096
```

### 4. Build and Install App
```bash
.\gradlew.bat :androidApp:assembleDebug
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

---

## Common Commands

### Monitor App Logs
```bash
# Clear and filter logs
adb logcat -c
adb logcat *:W | findstr "GitApiClient|GitServerChecker"

# Filter for connection issues
adb logcat | findstr "connection|timeout|refused"
```

### Check Git Server Status
```bash
# From host machine
curl http://localhost:4097/health
curl http://localhost:4097/status

# From emulator
adb shell curl http://127.0.0.1:4097/health
```

### Restart Git Server
```bash
# Find and kill existing server
ps aux | grep git-server
kill <pid>

# Start manually (for testing)
.\scripts\start-git-server.ps1
```

### Reset App Connection Cache
```bash
# Clear app data to force reconnection
adb shell pm clear com.mocca.app

# Or restart app multiple times
```

---

## Log Patterns

### Success
```
GitApiClient: [SUCCESS] Configured URL working: http://10.0.2.2:4097 (50ms)
GitApiClient: Server check passed at http://10.0.2.2:4097 (response time: 50ms)
```

### Fallback
```
GitApiClient: [FALLBACK SUCCESS] Localhost working: http://127.0.0.1:4097 (75ms)
GitApiClient: Connection Stats - Total: 1, Configured: 1, Localhost: 1, Working URL: http://127.0.0.1:4097
```

### Error
```
GitApiClient: [ATTEMPT 1] Configured URL failed: Connection refused, trying localhost
GitApiClient: [ATTEMPT 2] Localhost failed: Connection refused
GitApiClient: Server check failed at http://10.0.2.2:4097: Git server is not running
```

### Server Startup
```
GitApiClient: Requesting git server start at http://10.0.2.2:4096/command
GitApiClient: Git server is now available at http://127.0.0.1:4097 after 1200ms (3 attempts)
```

---

## Troubleshooting

### Issue: "Server Not Running" Dialog
**Check**:
1. Is Git server running? `ps aux | grep git-server`
2. Is port 4097 open? `netstat -an | grep 4097`
3. Is adb reverse active? `adb reverse --list`

**Fix**:
```bash
# Restart adb reverse
adb reverse --remove-all
adb reverse tcp:4097 tcp:4097

# Restart Git server
.\scripts\start-git-server.ps1

# Restart app
adb shell am force-stop com.mocca.app
adb shell am start -n com.mocca.app/com.mocca.app.MainActivity
```

### Issue: Connection Timeout
**Check**:
1. Network connectivity
2. Firewall blocking port 4097
3. Emulator network state

**Fix**:
```bash
# Restart emulator network
adb shell svc wifi disable
adb shell svc wifi enable

# Check connection
adb shell ping -c 3 127.0.0.1
```

### Issue: Plugin Not Working
**Check**:
1. Plugin location: `.opencode/plugins/git-plugin/git-plugin.js`
2. Plugin logs in OpenCode
3. Plugin has `command.executed` hook

**Fix**:
```bash
# Verify plugin exists
cat .opencode/plugins/git-plugin/git-plugin.js

# Restart OpenCode
# Check logs for: "git-plugin loaded successfully"
```

---

## Performance Metrics

### Expected Response Times
- Quick check: < 500ms
- Status operation: < 1s
- Log operation: < 2s
- Diff operation: < 5s (large files)

### Diagnostics
```kotlin
// Access connection stats programmatically
val stats = gitApiClient.getConnectionStats()
// {totalAttempts=5, configuredUrlAttempts=3, localhostAttempts=2, ...}
```

---

## Development Tips

### Testing Fallback Logic
```bash
# Block configured URL to force fallback
adb shell iptables -A OUTPUT -p tcp --dport 4097 -d 10.0.2.2 -j REJECT

# Test app
# ... perform operations ...

# Unblock
adb shell iptables -D OUTPUT -p tcp --dport 4097 -d 10.0.2.2 -j REJECT
```

### Testing Server Startup
```bash
# Stop Git server
kill $(cat /tmp/git-server.pid)

# Use app to trigger "Start Server"
# Monitor logs for startup sequence
```

### Testing Connection Resilience
```bash
# Simulate network interruption
adb shell svc wifi disable
# Wait 10 seconds
adb shell svc wifi enable

# App should reconnect automatically
```

---

## File Locations

| File | Path |
|------|------|
| GitApiClient | `composeApp/src/commonMain/kotlin/com/mocca/app/api/GitApiClient.kt` |
| GitServerChecker | `composeApp/src/commonMain/kotlin/com/mocca/app/api/GitServerChecker.kt` |
| Git Plugin | `.opencode/plugins/git-plugin/git-plugin.js` |
| Start Script | `scripts/start-git-server.ps1` (as configured) |
| Verification | `VERIFICATION_CHECKLIST.md` |
| Summary | `IMPLEMENTATION_SUMMARY.md` |
| API Docs | `composeApp/src/commonMain/kotlin/com/mocca/app/api/AGENTS.md` |

---

## Key Code Sections

### Connection Resolution
```kotlin
private suspend fun resolveConfiguredGitUrl(): String {
    // 1. Try cached URL
    cachedWorkingUrl?.let { return it }

    // 2. Parse configured URL
    // 3. Try configured URL first
    // 4. Fallback to localhost if needed
    // 5. Cache working URL
}
```

### Connection Check
```kotlin
private suspend fun ensureServerRunning(): Result<Unit> {
    val url = gitServerUrl()
    val (isRunning, message, responseTime) = quickCheckServer()

    // Handle success/slow/timeout
}
```

### Server Startup
```kotlin
suspend fun requestStartGitServerAndWait(
    maxWaitMs: Long = 10_000L,
    pollIntervalMs: Long = 500L
): Result<Boolean> {
    // 1. Send start command
    // 2. Poll for availability
    // 3. Return success/failure
}
```

---

## Useful Links

- **Full Verification Checklist**: `VERIFICATION_CHECKLIST.md`
- **Implementation Details**: `IMPLEMENTATION_SUMMARY.md`
- **API Documentation**: `composeApp/src/commonMain/kotlin/com/mocca/app/api/AGENTS.md`
- **Project README**: `README.md`

---

**Last Updated**: 2026-01-18
