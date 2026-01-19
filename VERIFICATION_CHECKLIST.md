# Git Server Connection Fix - Verification Checklist

**Date**: 2026-01-18
**Issue**: Android Emulator cannot reach Git Server at `10.0.2.2:4097`
**Fix**: Smart localhost fallback using `adb reverse tcp:4097 tcp:4097`

---

## Pre-Verification Setup

### 1. Environment Requirements
- [ ] Android Emulator running (API 36 or compatible)
- [ ] OpenCode server running on host: `opencode serve --port 4096`
- [ ] Git plugin loaded in `.opencode/plugins/git-plugin/`
- [ ] `adb reverse tcp:4097 tcp:4097` active on emulator
- [ ] Build and install debug APK: `.\gradlew.bat :androidApp:assembleDebug && adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### 2. Verify ADB Reverse Setup
```bash
# Check if adb reverse is active
adb reverse --list
# Expected output should include: tcp:4097 tcp:4097

# If not active, set it up manually:
adb reverse tcp:4097 tcp:4097
```

### 3. Check OpenCode Git Plugin
```bash
# Verify plugin is in correct location
ls .opencode/plugins/git-plugin/git-plugin.js
# Should exist with command.executed hook

# Start OpenCode and verify plugin loads
opencode serve --port 4096
# Check logs for: "git-plugin loaded successfully"
```

---

## Core Functionality Testing

### Test 1: Initial Git Server Connection
**Goal**: Verify app can detect and connect to Git server

**Steps**:
1. Launch MOCCA app
2. Navigate to Git screen
3. Check logcat for initialization messages:
   ```
   GitApiClient: Initialized - OpenCode URL: http://10.0.2.2:4096, Target Git Port: 4097
   GitApiClient: Using Git server URL: http://10.0.2.2:4097
   ```

**Expected Results**:
- [ ] App shows Git status screen without errors
- [ ] Logcat shows "Server check passed" or appropriate fallback message
- [ ] No connection timeout errors in logcat
- [ ] Git status displays correctly (branch, commits, etc.)

**Log Patterns to Check**:
```
# Success:
GitApiClient: Server check passed at http://10.0.2.2:4097 (response time: 50ms)

# Fallback to localhost:
GitApiClient: [FALLBACK SUCCESS] Localhost working: http://127.0.0.1:4097 (response: 75ms)
GitApiClient: Connection Stats - Total: 2, Configured: 1, Localhost: 1, Working URL: http://127.0.0.1:4097
```

---

### Test 2: Localhost Fallback Logic
**Goal**: Verify fallback triggers when configured URL fails

**Setup**: Temporarily break configured URL connection
```bash
# Block 10.0.2.2:4097 on emulator (simulating connection failure)
adb shell iptables -A OUTPUT -p tcp --dport 4097 -d 10.0.2.2 -j REJECT
```

**Steps**:
1. Force app to re-check Git server (restart app or trigger git operation)
2. Monitor logcat for fallback behavior

**Expected Log Sequence**:
```
GitApiClient: [Attempt 1] Checking configured URL: http://10.0.2.2:4097
GitApiClient: [ATTEMPT 1] Configured URL failed: Connection refused, trying localhost
GitApiClient: [FALLBACK SUCCESS] Localhost working: http://127.0.0.1:4097 (response: 80ms)
GitApiClient: Connection Stats - Total: 1, Configured: 1, Localhost: 1, Working URL: http://127.0.0.1:4097
```

**Cleanup**:
```bash
# Remove the block
adb shell iptables -D OUTPUT -p tcp --dport 4097 -d 10.0.2.2 -j REJECT
```

---

### Test 3: "Start Server" Button Functionality
**Goal**: Verify app can request Git server start from OpenCode

**Steps**:
1. Stop Git server if running:
   ```bash
   # Find and kill the git server process on host
   ps aux | grep git-server
   kill <pid>
   ```

2. In MOCCA app, trigger a Git operation (e.g., "Refresh Status")
3. App should detect server not running
4. Tap "Start Server" button in the dialog that appears

**Expected Log Sequence**:
```
GitApiClient: Requesting git server start at http://10.0.2.2:4096/command
GitApiClient: Git server start request sent successfully
GitApiClient: Start command sent, polling for server availability...
GitApiClient: Polling server at: http://127.0.0.1:4097
GitApiClient: Git server is now available at http://127.0.0.1:4097 after 1200ms (3 attempts)
```

**Expected OpenCode Host Logs**:
```
Received command: start-git-server
Executing start-git-server.ps1
Git HTTP server started on port 4097
```

**Results to Verify**:
- [ ] OpenCode executes `start-git-server.ps1` script
- [ ] Git server starts on port 4097
- [ ] App detects server becomes available within 10 seconds
- [ ] Git operations resume working
- [ ] No "server not running" errors after startup

---

### Test 4: Connection Statistics Tracking
**Goal**: Verify connection attempt tracking works correctly

**Steps**:
1. Use app for various Git operations (status, log, branches, diff, etc.)
2. Monitor logcat for connection statistics
3. Trigger connection changes (block/unblock configured URL)
4. Check that stats update correctly

**Expected Log Patterns**:
```
# Initial connection
GitApiClient: Connection Stats - Total: 1, Configured: 1, Localhost: 0, Working URL: http://10.0.2.2:4097

# After fallback
GitApiClient: Connection Stats - Total: 2, Configured: 1, Localhost: 1, Working URL: http127.0.0.1:4097

# Multiple operations use cached URL
GitApiClient: Using cached working URL: http://127.0.0.1:4097
```

---

## Diagnostic Commands

### Check ADB Reverse Status
```bash
adb reverse --list
# Should include: tcp:4097 tcp:4097
```

### Test Git Server from Host
```bash
curl http://localhost:4097/health
# Expected: {"status":"ok"}

curl http://localhost:4097/status
# Expected: JSON with git status
```

### Test Git Server from Emulator
```bash
adb shell curl http://127.0.0.1:4097/health
# Expected: {"status":"ok"}
```

### Monitor App Logs
```bash
# Clear and filter logs
adb logcat -c
adb logcat *:W | findstr "GitApiClient|GitServerChecker"

# Filter for connection issues
adb logcat | findstr "connection|timeout|refused"
```

---

## Known Issues & Limitations

### 1. Plugin Response
- **Issue**: OpenCode ignores `start-git-server` command and app receives no confirmation
- **Workaround**: Plugin logs command receipt but OpenCode doesn't execute it reliably
- **Impact**: "Start Server" button may not work as intended

### 2. Emulator Network
- **Issue**: `10.0.2.2:4097` always times out on Android Emulator
- **Workaround**: Fallback to `127.0.0.1:4097` via `adb reverse`
- **Impact**: Requires adb reverse setup before using app

### 3. Server Startup Timing
- **Issue**: Git server may take 1-3 seconds to start
- **Workaround**: App polls for up to 10 seconds
- **Impact**: Users may see brief delay during startup

---

## Regression Testing

### Test Git Operations
Perform each operation and verify success:
- [ ] **Status**: Refresh git status
- [ ] **Log**: View commit history
- [ ] **Branches**: List and checkout branches
- [ ] **Diff**: View file changes
- [ ] **Stage**: Stage files for commit
- [ ] **Commit**: Create a commit
- [ ] **Push/Pull**: Sync with remote (if available)
- [ ] **Stash**: Create/view stashes
- [ ] **Remotes**: List remote repositories

### Test Error Handling
- [ ] Disconnect from network and verify graceful error messages
- [ ] Stop Git server and verify "Server Not Running" dialog
- [ ] Block configured URL and verify fallback works
- [ ] Block both URLs and verify proper error handling

### Test Performance
- [ ] Measure response time for Git status (should be < 500ms)
- [ ] Test with large repository (100+ files)
- [ ] Verify no UI freezes during git operations
- [ ] Check memory usage doesn't leak

---

## Success Criteria

All of the following must pass:

1. ✅ App can connect to Git server without manual intervention
2. ✅ Localhost fallback works when configured URL fails
3. ✅ "Start Server" button triggers server startup (when OpenCode executes command)
4. ✅ All Git operations work correctly
5. ✅ Connection statistics are tracked and logged
6. ✅ Error messages are clear and actionable
7. ✅ No performance degradation
8. ✅ No regressions in existing functionality

---

## Troubleshooting

### Issue: "Server Not Running" dialog always appears
**Solutions**:
1. Check if Git server is running on host: `netstat -an | grep 4097`
2. Verify adb reverse is active: `adb reverse --list`
3. Check OpenCode logs for errors
4. Try manual server start: `.\start-git-server.ps1`

### Issue: Fallback not triggering
**Solutions**:
1. Clear app data: `adb shell pm clear com.mocca.app`
2. Restart emulator
3. Check logcat for error messages
4. Verify plugin is loaded in OpenCode

### Issue: Git operations slow or timeout
**Solutions**:
1. Check Git server logs
2. Verify no firewall blocking port 4097
3. Check emulator network performance
4. Reduce repository size for testing

---

## Documentation Updates

After verification is complete:
- [ ] Update AGENTS.md with fallback logic details
- [ ] Update README.md with adb reverse requirement
- [ ] Add troubleshooting section to project docs
- [ ] Document connection statistics in API documentation

---

**Verification Completed**: [ ] Yes / [ ] No
**Date Verified**: ___________
**Issues Found**: ___________________________
**Notes**: ___________________________
