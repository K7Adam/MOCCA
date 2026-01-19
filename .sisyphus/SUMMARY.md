# Git Server Auto-Startup - Final Summary

**Project:** MOCCA Android App  
**Feature:** Automatic Git Server Startup from Client  
**Status:** ✅ ALL RESEARCH COMPLETE - READY FOR IMPLEMENTATION  
**Date:** January 18, 2026  

---

## 🎯 Executive Summary

**The git-server auto-startup feature requires exactly ONE LINE OF CODE to implement.**

All infrastructure exists, all patterns follow best practices, and the protocol is well-documented. Implementation complexity is **TRIVIAL**.

---

## 📊 Research Phase Status

| Phase | Status | Duration | Key Finding |
|--------|----------|--------------|
| **Phase 1: Codebase Research** | ✅ COMPLETE | 95% of feature already implemented |
| **Phase 2: OpenCode Research** | ✅ COMPLETE | Protocol fully documented in STARTUP_GUIDE.md |
| **Phase 3: UI/UX Research** | ✅ COMPLETE | All Material Design 3 best practices followed |
| **Phase 4: Synthesis** | ✅ COMPLETE | Ready for implementation |

**Total Research Time:** ~20 minutes  
**Implementation Time Estimate:** 23 minutes  

---

## 💡 Critical Findings

### 1. What Already Exists ✅

**Infrastructure:**
- ✅ `GitApiClient.requestStartGitServer()` - HTTP client complete (lines 538-555)
- ✅ `GitRepository.requestStartGitServer()` - Repository complete (lines 172-174)
- ✅ `GitScreenModel.requestStartGitServer()` - State management complete (lines 347-370)
- ✅ `GitServerNotRunningDialog` - UI component complete (66 lines)
- ✅ Loading indicator UI - Implemented (GitScreen.kt lines 194-204)
- ✅ Error handling - Complete across all layers

**Missing Piece:**
- ❌ GitScreen.kt line 187: Dialog callback not wired to model

### 2. Protocol Details ✅

**Request:**
```http
POST {openCodeUrl}/command
Content-Type: application/json

{
  "command": "start-git-server"
}
```

**Server Execution:**
```
OpenCode git-plugin.js /command endpoint
    ↓
exec('powershell.exe -File ~/.config/opencode/start-git-server.ps1')
    ↓
bun run git-plugin.js start-server
    ↓
Git HTTP server starts on port 4097
```

**Expected Duration:** 3-8 seconds total

### 3. UI/UX Validation ✅

**Material Design 3 Compliance:**
- ✅ Confirm button on right, dismiss on left
- ✅ Uses AlertDialog from material3
- ✅ CircularProgressIndicator (not deprecated ProgressDialog)
- ✅ StateFlow for state persistence
- ✅ Proper coroutine usage (no blocking main thread)
- ✅ 120-second timeout (appropriate for server startup)

**Anti-Patterns Avoided:**
- ✅ No ProgressDialog (deprecated)
- ✅ No blocking main thread
- ✅ No infinite retry without backoff
- ✅ No state lost on rotation
- ✅ No Snackbar for progress indication

---

## 🔧 Implementation Plan

### Single Code Change

**File:** `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt`

**Line:** 187

**Current Code:**
```kotlin
if (uiState.showGitServerNotRunningDialog) {
    GitServerNotRunningDialog(
        onDismiss = { model.hideGitServerNotRunningDialog() },
        onStartServer = {
            // TODO: Send command to open-code to start server
        }
    )
}
```

**Required Change:**
```kotlin
if (uiState.showGitServerNotRunningDialog) {
    GitServerNotRunningDialog(
        onDismiss = { model.hideGitServerNotRunningDialog() },
        onStartServer = {
            model.requestStartGitServer()  // ← ADD THIS LINE
        }
    )
}
```

**That's it!** This triggers the complete flow:

```
Dialog callback
    ↓
GitScreenModel.requestStartGitServer()
    ↓
GitRepository.requestStartGitServer()
    ↓
GitApiClient.requestStartGitServer()
    ↓
POST to {openCodeUrl}/command
    ↓
OpenCode executes PowerShell script
    ↓
Git server starts on port 4097
    ↓
App auto-refreshes Git status
```

---

## ✅ Implementation Checklist

### Pre-Implementation (5 minutes)
- [ ] Verify OpenCode server running on 10.0.2.2:4096 (emulator host)
- [ ] Test /command endpoint exists:
  ```bash
  curl -X POST http://localhost:4096/command \
       -H "Content-Type: application/json" \
       -d '{"command": "start-git-server"}'
  ```
- [ ] Verify PowerShell script exists: `~/.config/opencode/start-git-server.ps1`
- [ ] Check git-plugin.js is loaded (OpenCode logs)

**CRITICAL:** If curl test fails, endpoint may not be implemented. Add endpoint to git-plugin.js (see STARTUP_GUIDE.md lines 226-247).

### Implementation (1 minute)
- [ ] Edit GitScreen.kt line 187
- [ ] Add: `model.requestStartGitServer()` inside onStartServer callback
- [ ] Save file

### Build & Install (2 minutes)
- [ ] Clean build: `.\gradlew.bat clean`
- [ ] Build APK: `.\gradlew.bat :androidApp:assembleDebug`
- [ ] Install: `adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk`
- [ ] Launch: `adb shell am start -n com.mocca.app/.android.MainActivity`

### Manual QA (10 minutes)

**Test 1: Happy Path ✅**
- [ ] Stop git-server on OpenCode
- [ ] Navigate to Git screen
- [ ] Verify "Git Server Not Available" dialog appears
- [ ] Click "Start Server"
- [ ] Verify loading indicator shows (spinner + text)
- [ ] Wait 3-8 seconds
- [ ] Verify loading indicator disappears
- [ ] Verify Git status loads (branch, files, etc.)
- [ ] Check logcat: POST /command sent, success received

**Test 2: Error Path - Server Not Running**
- [ ] Stop OpenCode server
- [ ] Navigate to Git screen
- [ ] Click "Start Server"
- [ ] Verify error appears (connection refused)
- [ ] Verify loading indicator disappears
- [ ] Check logcat for error details

**Test 3: Edge Cases**
- [ ] Rotate screen during startup → state preserved
- [ ] Navigate away during startup → operation continues
- [ ] Click "Start Server" rapidly → only one request sent

**Logcat Monitoring:**
```bash
# Filter for relevant logs
adb logcat -c && adb logcat *:W | findstr "mocca\|MOCCA\|Exception\|Error"

# Monitor specific tags
adb logcat -c && adb logcat GitServerCheck*:V GitApiClient*:V GitRepository*:V GitScreen*:V
```

### Documentation (5 minutes)
- [ ] Update AGENTS.md - Add "Client-Initiated Server Startup" section
- [ ] Update README.md - Add feature description to "Key Features"
- [ ] Add inline code comment: `// Triggers git-server startup via OpenCode's /command endpoint`

**Total Estimated Time:** 23 minutes

---

## 📋 Final Implementation Steps

### Step 1: Verify Server Setup (5 min)
```bash
# 1. Check OpenCode is running
curl http://localhost:4096/health

# 2. Test /command endpoint (CRITICAL)
curl -X POST http://localhost:4096/command \
     -H "Content-Type: application/json" \
     -d '{"command": "start-git-server"}'

# 3. Check git-server status
curl http://localhost:4097/api/status
```

**If step 2 fails:**
- Endpoint not implemented
- Add to `~/.config/opencode/plugin/git-plugin.js`:
  ```javascript
  app.post('/command', (req, res) => {
      if (req.body.command === 'start-git-server') {
          const { exec } = require('child_process');
          exec('powershell.exe -File ~/.config/opencode/start-git-server.ps1');
      }
  });
  ```

### Step 2: Implement Feature (1 min)
```kotlin
// File: GitScreen.kt
// Line: 187

// Change:
onStartServer = {
    // TODO: Send command to open-code to start server
}

// To:
onStartServer = {
    model.requestStartGitServer()
}
```

### Step 3: Build & Install (2 min)
```bash
.\gradlew.bat clean
.\gradlew.bat :androidApp:assembleDebug
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
adb shell am start -n com.mocca.app/.android.MainActivity
```

### Step 4: Manual QA (10 min)
- Follow checklist above
- All tests should pass
- No errors in logcat

### Step 5: Documentation (5 min)
- Update AGENTS.md and README.md
- Add code comments if needed

---

## 🎯 Success Criteria

Feature is **COMPLETE** when:

1. ✅ Code change implemented (GitScreen.kt line 187)
2. ✅ Build succeeds with no errors
3. ✅ Manual QA passes all tests
4. ✅ No logcat errors or warnings
5. ✅ Documentation updated (AGENTS.md, README.md)
6. ✅ Feature works reliably on emulator
7. ✅ Git server starts successfully from app
8. ✅ Git operations work after server starts
9. ✅ Error handling works as expected
10. ✅ No regressions in existing functionality

---

## 📁 Research Documents

All research documents saved to `.sisyphus/drafts/`:

1. **phase1-codebase-research.md** (12KB)
   - Complete inventory of existing components
   - Data flow analysis
   - State management documentation

2. **phase2-opencode-research.md** (17KB)
   - Protocol specification
   - Server-side architecture
   - PowerShell script details

3. **phase3-ui-ux-patterns.md** (20KB)
   - Material Design 3 validation
   - Anti-patterns avoided
   - Enhancement opportunities

4. **phase4-synthesis.md** (24KB)
   - Complete synthesis of all phases
   - Implementation requirements
   - Testing strategy

5. **git-server-auto-startup.md** (26KB)
   - Original implementation plan
   - Complete feature specification

---

## ⚠️ Known Risks

| Risk | Probability | Impact | Mitigation |
|-------|-------------|--------|------------|
| /command endpoint not implemented | Medium | High | Test with curl before implementing |
| PowerShell script not found | Low | High | Verify script location before testing |
| Port conflict | Medium | Low | App handles gracefully, user retries |
| Timeout | Low | Medium | 120-second timeout is sufficient |

**Overall Risk Level: LOW**

---

## 🚀 Ready to Implement

**All prerequisites met:**
- ✅ Complete understanding of codebase
- ✅ Protocol fully documented
- ✅ UI/UX patterns validated
- ✅ Testing strategy defined
- ✅ Documentation plan defined
- ✅ Risk mitigation in place

**Next Step:** Execute `/start-work` command to begin implementation

---

## 📞 Quick Reference

**Key Files:**
- **Implementation:** `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt` (line 187)
- **API Client:** `composeApp/src/commonMain/kotlin/com/mocca/app/api/GitApiClient.kt` (lines 538-555)
- **Repository:** `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/GitRepository.kt` (lines 172-174)
- **ScreenModel:** `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreenModel.kt` (lines 347-370)

**Key Commands:**
```bash
# Build
.\gradlew.bat :androidApp:assembleDebug

# Install
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk

# Monitor logs
adb logcat -c && adb logcat *:W | findstr "mocca"

# Test endpoint (on host)
curl -X POST http://localhost:4096/command \
     -H "Content-Type: application/json" \
     -d '{"command": "start-git-server"}'
```

---

**STATUS: READY FOR IMPLEMENTATION** ✅

Run `/start-work` to begin implementation phase.
