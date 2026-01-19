# Git Server Connection Fix - Verification Test Plan

**Date**: 2026-01-18
**Build**: androidApp-debug.apk (successfully compiled)
**Test Environment**: Android Emulator with ADB reverse configured

---

## Success Criteria (Pre-Implementation)

| Criteria Type | Description | Observable Evidence |
|---------------|-------------|---------------------|
| **Functional** | Git operations work on emulator after ADB reverse setup | App shows Git status, no "server not running" errors |
| **Observable** | Logcat shows successful connections to localhost:4097 | Logs: "FALLBACK SUCCESS: Localhost working" or "Configured URL working" |
| **Pass/Fail** | Race condition eliminated | No duplicate "[Attempt X]" messages with inconsistent numbers |

---

## Test Plan

### Objective
Verify that Git server connection works correctly on Android Emulator with ADB reverse setup, and race conditions are eliminated.

### Prerequisites
1. **ADB Reverse Setup** (MANDATORY):
   ```bash
   adb reverse tcp:4097 tcp:4097
   adb reverse --list
   # Expected: tcp:4097 tcp:4097
   ```

2. **Git Server Running**:
   - Start OpenCode server: `opencode serve --port 4096`
   - Git plugin loaded: `.opencode/plugins/git-plugin/git-plugin.js`
   - Git server accessible: `curl http://localhost:4097/health` returns `{"status":"ok"}`

3. **App Installed**:
   - Latest APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
   - App launched and connected to OpenCode server

### Test Cases

#### Test 1: Basic Git Server Connection
**Input**: Launch app, navigate to Git screen
**Expected Output**:
- App loads Git status without errors
- Logcat shows: "GitApiClient: Initialized - OpenCode URL: http://10.0.2.2:4096, Target Git Port: 4097"
- Logcat shows: "GitApiClient: [Attempt 1] Checking configured URL: http://10.0.2.2:4097"
- Logcat shows: "GitApiClient: [FALLBACK SUCCESS] Localhost working: http://127.0.0.1:4097 (XXms)"
- OR: "GitApiClient: [SUCCESS] Configured URL working: http://10.0.2.2:4097 (XXms)"

**How to Verify**:
```bash
# Monitor logs
adb logcat -c
adb logcat *:W | findstr "GitApiClient"

# Check for success patterns:
# ✅ PASS: "[FALLBACK SUCCESS] Localhost working" or "[SUCCESS] Configured URL working"
# ❌ FAIL: "Both URLs failed" or "Git server is not running"
```

**Pass Criteria**:
- [ ] No "Git server is not running" errors in logcat
- [ ] Status displays correctly (branch, commits, file changes)
- [ ] Response time < 2 seconds

---

#### Test 2: Race Condition Elimination
**Input**: Trigger multiple Git operations simultaneously (Refresh Status, Switch to Log, Switch to Branches)
**Expected Output**:
- Only one URL resolution attempt logged
- Connection attempt counter increments correctly (1, 2, 3...)
- No duplicate "[Attempt X]" with inconsistent numbers
- No redundant network calls for the same URL

**How to Verify**:
```bash
# Clear logs and start fresh
adb logcat -c

# Trigger multiple operations:
# 1. Tap "Status" tab
# 2. Wait for load, tap "Log" tab
# 3. Wait for load, tap "Branches" tab
# 4. Check logs

# ✅ PASS: Logs show "Using cached working URL" for 2nd and 3rd operations
# ❌ FAIL: Multiple "[Attempt 1]", "[Attempt 2]", "[Attempt 3]" for same operation
```

**Pass Criteria**:
- [ ] First operation shows "Attempt 1" and resolves URL
- [ ] Subsequent operations show "Using cached working URL" (not "Attempt X")
- [ ] Connection stats show: "Working URL: http://127.0.0.1:4097" (not "none")
- [ ] Total attempt count < 5 (indicates caching is working)

---

#### Test 3: Git Server Not Running Dialog
**Input**: Stop Git server on host, then trigger Git operation in app
**Expected Output**:
- App detects server not running
- Shows "Server Not Running" dialog
- Dialog provides "Start Server" button

**How to Verify**:
```bash
# Stop Git server on host
# Windows: Find process and kill it
netstat -ano | findstr :4097
taskkill /PID <pid>

# Linux/Mac:
pkill -f git-server
netstat -an | grep 4097

# Trigger Git operation in app (navigate to Git screen)
# Check logs for dialog:
adb logcat | findstr "ServerNotRunning"
```

**Pass Criteria**:
- [ ] Logcat shows: "showServerNotRunningDialog() called"
- [ ] UI displays "Server Not Running" dialog
- [ ] Error message: "Git server is not running (URL: ...)"
- [ ] "Start Server" button is clickable and functional

---

#### Test 4: Start Server Functionality
**Input**: Click "Start Server" button in "Server Not Running" dialog
**Expected Output**:
- App sends command to OpenCode server
- Polls for server availability (up to 15 seconds)
- Server becomes available
- Git operations resume working

**How to Verify**:
```bash
# Monitor OpenCode server logs (should show):
# "Received command: start-git-server"
# "Executing start-git-server.ps1"
# "Git HTTP server started on port 4097"

# Monitor app logs:
adb logcat | findstr "requestStartGitServer|Git server started"

# ✅ PASS: "Git server started successfully!"
# ✅ PASS: Connection stats updated
# ❌ FAIL: "Server did not start within 15 seconds"
```

**Pass Criteria**:
- [ ] Logcat shows: "Requesting git server start via OpenCode with polling"
- [ ] Logcat shows: "Git server started successfully!" (within 15 seconds)
- [ ] Git status displays correctly after server start
- [ ] No timeout errors

---

#### Test 5: All Git Operations
**Input**: Perform each Git operation
**Expected Output**:
- All operations complete successfully
- No "server not running" errors
- Proper data display (status, logs, branches, remotes, etc.)

**Test Operations**:
1. **Refresh Status**: Should show current branch, staged/unstaged files
2. **View Log**: Should show commit history
3. **View Branches**: Should list all branches
4. **View Remotes**: Should show configured remotes
5. **Stage File**: Should stage file successfully
6. **Unstage File**: Should unstage file successfully
7. **Commit**: Should create commit with message
8. **Pull**: Should fetch from remote
9. **Push**: Should push to remote (if configured)
10. **Checkout Branch**: Should switch branch
11. **Discard Changes**: Should discard uncommitted changes
12. **Fetch**: Should update from remote
13. **View Stashes**: Should show stash list (if any)
14. **Diff**: Should show file differences

**How to Verify**:
```bash
# Monitor logs during each operation
adb logcat | findstr "GitApiClient"

# Look for success patterns:
# ✅ PASS: "GitApiClient: getStatus", no errors
# ✅ PASS: "GitApiClient: getLog" success
# ✅ PASS: "GitApiClient: commit" success
# ❌ FAIL: "Git server is not running"
# ❌ FAIL: "Connection failed"
```

**Pass Criteria**:
- [ ] All 14 operations execute without "server not running" errors
- [ ] Each operation shows correct data in UI
- [ ] Logcat shows operation method name followed by success
- [ ] No timeout exceptions (> 10 seconds)

---

## How to Execute

### Step-by-Step Instructions

1. **Setup Environment**:
   ```bash
   # Start Android Emulator
   emulator -avd <your_avd_name>

   # Verify emulator is running
   adb devices

   # Setup ADB reverse (CRITICAL!)
   adb reverse tcp:4097 tcp:4097
   adb reverse --list
   # Should show: tcp:4097 tcp:4097
   ```

2. **Start OpenCode Server**:
   ```bash
   # Navigate to MOCCA project directory
   cd /path/to/MOCCA

   # Start OpenCode server with Git plugin
   opencode serve --port 4096
   # Verify Git plugin is loaded (check logs)
   ```

3. **Install and Launch App**:
   ```bash
   # Install APK
   adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk

   # Launch app
   adb shell am start -n com.mocca.app/.MainActivity

   # Monitor logs
   adb logcat -c
   adb logcat *:W | findstr "GitApiClient|GitServerChecker"
   ```

4. **Execute Test Cases**:
   - Follow Test Cases 1-5 above
   - Document results (PASS/FAIL)
   - Capture logcat output for evidence

5. **Cleanup**:
   ```bash
   # Clear logs
   adb logcat -c

   # If needed, uninstall app:
   adb uninstall com.mocca.app
   ```

---

## Success Criteria: ALL Tests Pass

### Required Evidence
- [ ] Test 1 PASS: Basic Git connection works
- [ ] Test 2 PASS: Race condition eliminated
- [ ] Test 3 PASS: Server not running dialog shows correctly
- [ ] Test 4 PASS: Start server functionality works
- [ ] Test 5 PASS: All Git operations work

### Overall Pass/Fail
- ✅ **PASS**: All 5 test cases pass
- ❌ **FAIL**: Any test case fails

### Logcat Evidence Collection
Save logcat output for each test case:
- `test1_basic_connection.txt`
- `test2_race_condition.txt`
- `test3_server_dialog.txt`
- `test4_start_server.txt`
- `test5_git_operations.txt`

---

## Troubleshooting Guide

### Issue: "Git server is not running" still appears
**Diagnosis**: ADB reverse not configured or Git server not running

**Fix**:
```bash
# 1. Verify ADB reverse
adb reverse --list
# If tcp:4097 is missing, re-run:
adb reverse tcp:4097 tcp:4097

# 2. Verify Git server on host
curl http://localhost:4097/health
# Expected: {"status":"ok"}

# 3. If server not running, start it manually
.\scripts\start-git-server.ps1

# 4. Restart emulator (sometimes ADB connection issues persist)
# Stop and restart emulator, then re-run ADB reverse
```

### Issue: Multiple "Attempt X" messages appear
**Diagnosis**: Race condition still present (cache not working)

**Fix**:
```bash
# 1. Verify Mutex was added to code
# Check GitApiClient.kt line 43: private val urlResolutionMutex = Mutex()

# 2. Rebuild APK
./gradlew.bat :androidApp:assembleDebug

# 3. Clear app data to reset state
adb shell pm clear com.mocca.app

# 4. Reinstall app
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk

# 5. Test again
# Should see "Using cached working URL" on subsequent operations
```

### Issue: "Start Server" button does nothing
**Diagnosis**: OpenCode server not executing command or Git plugin not loaded

**Fix**:
```bash
# 1. Verify OpenCode is running
curl http://10.0.2.2:4096/health
# Or for localhost: curl http://localhost:4096/health

# 2. Verify Git plugin is loaded
cat .opencode/plugins/git-plugin/git-plugin.js
# Should contain: hooks: { command: { executed: ... } }

# 3. Check OpenCode logs for command reception
# Look for: "Received command: start-git-server"

# 4. If plugin missing, restart OpenCode with plugin
# Stop OpenCode and restart with --plugin flag (if supported)
```

### Issue: ADB reverse shows but connection still fails
**Diagnosis**: Emulator network issue or firewall blocking

**Fix**:
```bash
# 1. Remove existing ADB reverse
adb reverse --remove-all

# 2. Re-add ADB reverse
adb reverse tcp:4097 tcp:4097

# 3. Verify from emulator
adb shell curl http://127.0.0.1:4097/health
# Expected: {"status":"ok"}

# 4. Check emulator network
adb shell ping -c 3 127.0.0.1
# Expected: 3 packets transmitted, 3 received

# 5. If still fails, restart emulator completely
```

---

## Documentation Requirements

After testing is complete:
- [ ] Update VERIFICATION_CHECKLIST.md with test results
- [ ] Update IMPLEMENTATION_SUMMARY.md with test outcomes
- [ ] Add log examples (successful and failed) to docs
- [ ] Update QUICK_REFERENCE.md with verified solutions

---

**Verification Status**: ❌ Not Started
**Date**: 2026-01-18 00:11 AM
**Tester**: Autonomous System
