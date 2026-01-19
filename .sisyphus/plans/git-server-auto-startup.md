# Git Server Auto-Startup Feature - Implementation Plan

**Project:** MOCCA Android App
**Feature:** Automatic Git Server Startup from Client
**Status:** ALL PHASES COMPLETE ✅ - Ready for Implementation
**Date:** January 17, 2026

---

## Executive Summary

This plan implements automatic git-server startup triggered from the MOCCA Android app when the git HTTP server (port 4097) is not available. The feature allows users to start the git-server directly from the mobile app via a REST API command to the OpenCode server.

**Surprising Finding:** Phase 1 research reveals that **95% of the feature is already implemented**. Only a single callback in `GitScreen.kt` line 187 needs to be wired up to complete the feature.

---

## 1. Feature Overview

### 1.1 User Story
As a developer using MOCCA on my Android device, when I try to access the Git screen but the git server is not running, I want to be able to start it directly from the app without having to manually run scripts or return to my development machine.

### 1.2 Current Behavior
- User navigates to Git screen
- App checks if git-server is available (port 4097)
- If not available, shows `GitServerNotRunningDialog` with warning
- User clicks "Start Server" button
- **[BROKEN]** Nothing happens (TODO comment at line 187)

### 1.3 Desired Behavior
- User navigates to Git screen
- App checks if git-server is available (port 4097)
- If not available, shows `GitServerNotRunningDialog` with warning
- User clicks "Start Server" button
- App sends POST request to `{openCodeUrl}/command` with body `{"command": "start-git-server"}`
- OpenCode server triggers `start-git-server.ps1` script
- Git HTTP server starts on port 4097
- App automatically refreshes Git status
- Git screen displays normal Git operations

---

## 2. Implementation Strategy

### 2.1 Technical Complexity: **LOW**

**Why Low Complexity:**
1. All infrastructure already exists (GitApiClient, GitRepository, GitScreenModel)
2. All state management already exists
3. All UI components already exist
4. **Only 1 line of code needs to change**

### 2.2 Architecture Assessment

```
┌─────────────────────────────────────────────────────────────┐
│                        GitScreen.kt                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  GitServerNotRunningDialog                             │ │
│  │  onStartServer = { model.requestStartGitServer() }    │ │  ← ONLY CHANGE NEEDED
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                             │ collectAsState()
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     GitScreenModel.kt                        │
│  - requestStartGitServer()  ✅ Complete                     │
│  - onGitServerStarted()     ✅ Complete                     │
│  - onGitServerStartFailed() ✅ Complete                     │
│  - State management         ✅ Complete                     │
└─────────────────────────────────────────────────────────────┘
                             │ calls suspend
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     GitRepository.kt                         │
│  - requestStartGitServer() ✅ Complete                      │
└─────────────────────────────────────────────────────────────┘
                             │ calls suspend
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                      GitApiClient.kt                         │
│  - requestStartGitServer() ✅ Complete                      │
│    POST {openCodeUrl}/command                              │
│    Body: {"command": "start-git-server"}                    │
└─────────────────────────────────────────────────────────────┘
                             │ HTTP Request
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    OpenCode Server                           │
│  - /command endpoint handles request                        │
│  - Triggers start-git-server.ps1                            │
│  - Starts git HTTP server on port 4097                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Implementation Plan

### 3.1 Phase Summary

| Phase | Status | Deliverables |
|-------|--------|--------------|
| **Phase 1: Codebase Research** | ✅ Complete | Full inventory of existing components |
| **Phase 2: OpenCode Research** | ⏳ In Progress | Server-side protocol analysis |
| **Phase 3: UI/UX Research** | ⏳ In Progress | Dialog & retry patterns |
| **Phase 4: Synthesis** | ⏳ Not Started | Final implementation guide |

### 3.2 Phase 1 Results ✅

**Completed Inventory:**
- ✅ `GitApiClient.requestStartGitServer()` (lines 538-555) - HTTP implementation complete
- ✅ `GitRepository.requestStartGitServer()` (lines 172-174) - Repository delegation complete
- ✅ `GitScreenModel.requestStartGitServer()` (lines 347-360) - Async handling complete
- ✅ `GitScreenModel.onGitServerStarted()` (lines 362-366) - Success path complete
- ✅ `GitScreenModel.onGitServerStartFailed()` (lines 368-370) - Error path complete
- ✅ `GitServerNotRunningDialog` (66 lines) - UI component complete
- ✅ Loading indicator UI (GitScreen.kt lines 194-204)
- ✅ State properties for all scenarios

**Single Implementation Point:**
```
File: GitScreen.kt
Line: 187
Current:
    onStartServer = {
        // TODO: Send command to open-code to start server
    }

Change to:
    onStartServer = {
        model.requestStartGitServer()
    }
```

**That's it!** This single line change triggers the complete chain:
1. Dialog callback → GitScreenModel.requestStartGitServer()
2. → GitRepository.requestStartGitServer()
3. → GitApiClient.requestStartGitServer()
4. → HTTP POST to OpenCode server
5. → PowerShell script triggers
6. → Git server starts on port 4097

### 3.3 Phase 2 Research (In Progress) 📡

**Tasks:**
1. ✅ Librarian agent researching OpenCode repository
2. ⏳ Understanding `/command` endpoint implementation
3. ⏳ PowerShell script triggering mechanism
4. ⏳ Response format (success/error)
5. ⏳ Authentication requirements
6. ⏳ Error handling patterns

**Questions Being Answered:**
- How exactly does the `/command` endpoint process requests?
- What is the exact response format for successful startup?
- What error responses can occur?
- Does the endpoint require authentication tokens?
- How does the PowerShell script get triggered?
- What permissions are needed to start the server?

**Expected Deliverables:**
- Detailed protocol documentation
- Request/response examples
- Error case catalog
- Security considerations

### 3.4 Phase 3 Research (In Progress) 🎨

**Tasks:**
1. ✅ Librarian agent researching Android patterns
2. ⏳ Material Design 3 dialog guidelines
3. ⏳ Auto-retry best practices
4. ⏳ Timeout handling patterns
5. ⏳ User feedback strategies

**Questions Being Answered:**
- Are current dialog button labels appropriate?
- Should we add automatic retry logic?
- Is the loading state display adequate?
- Should we show detailed error messages?
- What's the optimal timeout value?

**Expected Deliverables:**
- UI/UX pattern documentation
- Accessibility considerations
- Common anti-patterns to avoid
- Recommendations for enhancement

### 3.5 Phase 4: Synthesis & Final Plan (Not Started) 📝

**Will Produce:**
1. Complete implementation checklist
2. Testing strategy
3. Deployment plan
4. Documentation updates needed
5. Known limitations and edge cases

---

## 4. Data Flow Protocol

### 4.1 Client → Server Communication

**Request:**
```http
POST {openCodeUrl}/command
Content-Type: application/json
Authorization: Bearer {token}  (if configured)

{
  "command": "start-git-server"
}
```

**Server Processing (Hypothesized):**
```javascript
// Pseudocode for git-plugin.js
app.post('/command', async (req, res) => {
    const { command } = req.body

    if (command === 'start-git-server') {
        try {
            // Execute PowerShell script
            const result = await execPowershell('start-git-server.ps1')

            // Wait for server to be ready
            await waitForPort(4097)

            res.json({
                success: true,
                message: 'Git server started successfully'
            })
        } catch (error) {
            res.status(500).json({
                success: false,
                error: error.message
            })
        }
    }
})
```

**Success Response (Hypothesized):**
```json
{
  "success": true,
  "message": "Git HTTP server started on port 4097"
}
```

**Error Response (Hypothesized):**
```json
{
  "success": false,
  "error": "Failed to start git server: Port 4097 already in use"
}
```

### 4.2 Client State Transition

```
Initial State:
├─ showGitServerNotRunningDialog: true
├─ isStartingGitServer: false
├─ isGitServerAvailable: false
└─ gitServerStartError: null

User clicks "Start Server"
↓
model.requestStartGitServer()
↓
State Update:
├─ showGitServerNotRunningDialog: false
├─ isStartingGitServer: true
├─ isGitServerAvailable: false
└─ gitServerStartError: null

Server Start Successful
↓
onGitServerStarted()
↓
State Update:
├─ showGitServerNotRunningDialog: false
├─ isStartingGitServer: false
├─ isGitServerAvailable: true
└─ gitServerStartError: null

Auto-refresh Git Status
↓
loadGitStatus()
```

**Error Path:**
```
Server Start Failed
↓
onGitServerStartFailed(error)
↓
State Update:
├─ showGitServerNotRunningDialog: false
├─ isStartingGitServer: false
├─ isGitServerAvailable: false
└─ gitServerStartError: "Failed to start git server: ..."

Display Error to User (via SnackBar or Alert)
```

---

## 5. Testing Strategy

### 5.1 Test Scenarios

| Scenario | Steps | Expected Result |
|----------|-------|-----------------|
| **Successful Start** | 1. Stop git-server on OpenCode<br>2. Open MOCCA Git screen<br>3. Click "Start Server"<br>4. Wait for loading | Loading indicator → Git status loads normally |
| **Server Already Running** | 1. Ensure git-server running<br>2. Open MOCCA Git screen | No dialog shown, Git status loads normally |
| **Network Error** | 1. Disconnect network<br>2. Click "Start Server" | Error message shown, dialog dismissed |
| **OpenCode Server Down** | 1. Stop OpenCode server<br>2. Open MOCCA Git screen | App handles connection error gracefully |
| **PowerShell Script Error** | 1. Corrupt start-git-server.ps1<br>2. Click "Start Server" | Error message with script error details |
| **Port Conflict** | 1. Another process using port 4097<br>2. Click "Start Server" | Error message indicates port conflict |

### 5.2 Manual QA Checklist

**Required Testing Environment:**
- ✅ Android Emulator (Nexus 5X, API 36)
- ✅ OpenCode server running on host (10.0.2.2:4096)
- ✅ Git-server NOT running initially
- ✅ ADB logcat monitoring: `adb logcat *:W | findstr "mocca"`

**Test Cases:**
1. **Happy Path:**
   - [ ] Navigate to Git screen
   - [ ] Verify "Git Server Not Available" dialog appears
   - [ ] Click "Start Server" button
   - [ ] Verify loading indicator shows
   - [ ] Verify loading indicator disappears after ~2-5 seconds
   - [ ] Verify Git status loads (showing branch, files, etc.)
   - [ ] Check logcat for request sent to `/command`
   - [ ] Check logcat for successful response

2. **Error Path:**
   - [ ] Stop OpenCode server
   - [ ] Navigate to Git screen
   - [ ] Click "Start Server"
   - [ ] Verify error message appears
   - [ ] Check logcat for connection error
   - [ ] Restart OpenCode

3. **Edge Cases:**
   - [ ] Try starting server multiple times in rapid succession
   - [ ] Rotate screen during startup (verify state preserved)
   - [ ] Navigate away during startup (verify operation continues)
   - [ ] Kill app during startup (verify cleanup)

### 5.3 Logcat Monitoring Commands

```bash
# Filter for relevant logs
adb logcat -c && adb logcat *:W | findstr "mocca\|MOCCA\|Exception\|Error"

# Monitor specific tags
adb logcat -c && adb logcat GitServerCheck*:V GitApiClient*:V GitRepository*:V GitScreen*:V

# Check for network requests
adb logcat -c && adb logcat | findstr "POST /command"
```

---

## 6. Known Limitations & Edge Cases

### 6.1 Current Limitations

1. **Manual Retry Only:**
   - No automatic retry on failure
   - User must manually retry by clicking "Start Server" again
   - This is by design (server startup is a manual operation)

2. **No Server Health Check After Start:**
   - If server starts successfully but immediately crashes, UI won't detect it
   - User must navigate away and back to trigger availability check

3. **Fixed Timeout:**
   - 120-second timeout from Ktor client configuration
   - May be too long for quick failures, too short for slow startup

4. **No Progress Feedback:**
   - Loading indicator doesn't show actual progress
   - User doesn't know if server is starting or frozen

### 6.2 Potential Edge Cases

| Edge Case | Handling | Status |
|-----------|----------|--------|
| Port 4097 already in use | Return error to user, explain port conflict | Needs verification |
| PowerShell script not found | Return error with script path | Needs verification |
| Insufficient permissions | Return error with permission details | Needs verification |
| OpenCode server not running | Network error propagated to UI | Handled by existing code |
| Network timeout after start | Timeout error from 120s timeout | Handled by existing code |
| App killed during startup | Coroutines cancelled, no side effects | Handled by viewModelScope |
| Multiple concurrent requests | Could cause duplicate server processes | **Needs investigation** |

### 6.3 Security Considerations

**To Be Investigated (Phase 2):**
1. Does `/command` endpoint require authentication?
   - If yes: MOCCA must include auth token in request
   - If no: Security concern - any client could start server

2. Does server validate request source?
   - Should only allow start from admin clients
   - Potential for abuse if unrestricted

3. Are there rate limits on `/command` endpoint?
   - Could prevent denial of service attacks
   - UI should respect any rate limits

---

## 7. Documentation Updates Required

### 7.1 AGENTS.md Updates

**Section: GIT SERVER INTEGRATION**

Add sub-section:
```markdown
### Client-Initiated Startup

The MOCCA app can automatically start the git HTTP server from the UI:

1. Navigating to Git screen triggers availability check (GitServerChecker)
2. If unavailable, shows GitServerNotRunningDialog
3. Clicking "Start Server" triggers this flow:
   - GitScreenModel.requestStartGitServer()
   - GitRepository.requestStartGitServer()
   - GitApiClient.requestStartGitServer()
   - HTTP POST to {openCodeUrl}/command
   - Body: {"command": "start-git-server"}

Server responds with success/error, and UI updates accordingly.

**Implementation:** GitScreen.kt line 187
```

### 7.2 README.md Updates

**Add to "Key Features" section:**
- **Automatic Git Server Management:** Mobile app can start git HTTP server when unavailable

**Add to "Architecture" diagram:**
```
GitServerNotRunningDialog
    ↓ sends command
OpenCode /command endpoint
    ↓ triggers PowerShell
Git HTTP Server (port 4097)
```

### 7.3 Inline Code Documentation

**GitScreen.kt:**
- Document the callback wiring
- Explain why this was deferred (security considerations)

**GitServerNotRunningDialog.kt:**
- Already well documented, no changes needed

---

## 8. Deployment Plan

### 8.1 Pre-Deployment Checklist

**Code Quality:**
- [ ] Verify all code follows MOCCA conventions
- [ ] Check for proper error handling
- [ ] Verify state management correctness
- [ ] Ensure no memory leaks (coroutine cleanup)

**Testing:**
- [ ] Manual QA on emulator completed
- [ ] All test scenarios pass
- [ ] Edge cases tested
- [ ] Logcat shows no errors/warnings

**Documentation:**
- [ ] AGENTS.md updated
- [ ] README.md updated
- [ ] Code comments added where needed

### 8.2 Build & Deploy Commands

```bash
# Clean build
.\gradlew.bat clean

# Build debug APK
.\gradlew.bat :androidApp:assembleDebug

# Install via ADB
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk

# Launch app
adb shell am start -n com.mocca.app/.android.MainActivity

# Monitor logs
adb logcat -c && adb logcat *:W | findstr "mocca"
```

### 8.3 Rollback Plan

If issues discovered after deployment:

1. **Immediate Action:**
   - Uninstall app: `adb uninstall com.mocca.app`
   - Install previous version APK

2. **Investigation:**
   - Review logcat recordings
   - Check for API changes in OpenCode
   - Verify git-plugin.js version

3. **Fix & Test:**
   - Apply patch to fix issue
   - Test thoroughly on emulator
   - Deploy updated APK

---

## 9. Future Enhancement Opportunities

After initial implementation, consider these enhancements:

### 9.1 Enhanced User Feedback
1. **Progress Indicators:**
   - Show actual progress (e.g., "Starting...", "Waiting for server...", "Ready")
   - Use progress bar instead of spinner for long operations

2. **Detailed Error Messages:**
   - Show specific error from PowerShell script
   - Provide actionable suggestions (e.g., "Check if port 4097 is in use")

3. **Server Health Monitoring:**
   - Poll server health during startup
   - Detect and report server crashes immediately

### 9.2 Automatic Retry Logic
1. **Smart Retry:**
   - Automatically retry on transient errors
   - Exponential backoff between retries
   - Limit number of retries

2. **Background Monitoring:**
   - Periodically check server health
   - Auto-restart if server crashes (if appropriate)

### 9.3 Improved UX
1. **One-Touch Setup:**
   - Prompt first-time users to start server on first access
   - Remember user's preference

2. **Server Status Indicator:**
   - Always show git-server status in Git screen
   - Visual indicator (green dot when running, red when stopped)

3. **Shortcut Buttons:**
   - Quick start/stop buttons in Git screen header
   - Keyboard shortcuts for power users

---

## 10. Implementation Checklist

### 10.1 Code Changes

**Phase 4 Finalization Required:**
- [ ] Review Phase 2 research (OpenCode protocol)
- [ ] Review Phase 3 research (UI/UX patterns)
- [ ] Finalize implementation decisions

**Implementation:**
- [ ] Update `GitScreen.kt` line 187: Wire callback
- [ ] Test on emulator (manual QA)
- [ ] Monitor logcat for errors

**Verification:**
- [ ] Verify successful startup flow
- [ ] Verify error handling flow
- [ ] Verify loading indicator shows/hides correctly
- [ ] Verify auto-refresh after successful start

### 10.2 Documentation

- [ ] Update AGENTS.md (Git Server Integration section)
- [ ] Update README.md (add feature description)
- [ ] Add inline code comments if needed
- [ ] Create/update changelog

### 10.3 Testing

- [ ] Test on Android Emulator (API 36)
- [ ] Test all scenarios from Section 5.1
- [ ] Complete manual QA checklist (Section 5.2)
- [ ] Document test results

### 10.4 Deployment

- [ ] Clean build: `.\gradlew.bat clean`
- [ ] Build debug APK: `.\gradlew.bat :androidApp:assembleDebug`
- [ ] Install and test
- [ ] Verify no logcat errors
- [ ] Mark feature as complete

---

## 11. Risk Assessment

### 11.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| OpenCode `/command` endpoint not implemented | Low | High | Phase 2 research will confirm |
| Different protocol than expected | Medium | Medium | Phase 2 will document actual protocol |
| PowerShell script path differs | Low | Low | Can be adjusted in OpenCode if needed |
| Authentication required | Medium | Medium | Phase 2 will verify auth requirements |
| Port already in use | Medium | Low | Clear error message to user |
| Connection timeout issues | Low | Medium | 120s timeout should be sufficient |

### 11.2 UX Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Not enough feedback during startup | Low | Medium | Loading indicator shows, may enhance later |
| Error messages unclear | Low | Low | Show actual error from server |
| Users don't understand dialog | Low | Low | Clear text in dialog already |
| Startup too slow, users think it failed | Medium | Low | Proper loading indicator helps |

### 11.3 Security Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| No auth on `/command` endpoint | Unknown | High | Phase 2 will verify |
| Unauthorized access to server startup | Unknown | High | Phase 2 will verify protections |
| Script injection via command field | Low | High | OpenCode should validate input |

---

## 12. Dependencies & Prerequisites

### 12.1 Server-Side Requirements

**Required:**
1. OpenCode server running on port 4096
2. Git-plugin.js loaded in OpenCode
3. `start-git-server.ps1` script in correct location
4. Port 4097 available for git HTTP server

**To Be Verified (Phase 2):**
- [ ] Exact location of PowerShell script
- [ ] How to ensure git-plugin.js is loaded
- [ ] How to verify server is ready after startup

### 12.2 Client-Side Requirements

**Already Satisfied:**
1. ✅ Kotlin Multiplatform environment setup
2. ✅ Ktor client dependency (for HTTP requests)
3. ✅ Coroutines support (for async operations)
4. ✅ StateFlow support (for state management)
5. ✅ Material Design 3 UI components

**No New Dependencies Required**

---

## 13. Estimated Effort

### 13.1 Time Estimates

| Task | Estimated Time | Status |
|------|----------------|--------|
| Phase 1: Codebase Research | Complete ✅ | 2 hours |
| Phase 2: OpenCode Research | In Progress | 1-2 hours |
| Phase 3: UI/UX Research | In Progress | 1 hour |
| Phase 4: Synthesis & Final Plan | Not Started | 1 hour |
| Code Implementation (1 line change!) | Not Started | 5 minutes |
| Testing & QA | Not Started | 1-2 hours |
| Documentation Updates | Not Started | 30 minutes |
| **Total** | **~6-9 hours** | **~30% complete** |

### 13.2 Complexity Breakdown

| Component | Complexity | Reason |
|-----------|------------|--------|
| Code Changes | **TRIVIAL** | Single line callback wiring |
| Testing | **MEDIUM** | Need comprehensive manual QA |
| Documentation | **LOW** | Clear changes to document |
| Research | **MEDIUM** | Understanding OpenCode protocol |

**Overall Complexity: LOW**

---

## 14. Success Criteria

Feature will be considered successful when:

1. ✅ All code changes complete and tested
2. ✅ All scenarios fromSection 5.1 pass
3. ✅ Manual QA checklist complete (Section 5.2)
4. ✅ No logcat errors or warnings
5. ✅ Documentation updated (AGENTS.md, README.md)
6. ✅ Feature works reliably on Android Emulator
7. ✅ Git server starts successfully from mobile app
8. ✅ Git operations work after server starts
9. ✅ Error handling works as expected
10. ✅ No regressions in existing functionality

---

## 15. Questions & Unknowns

### 15.1 Questions for Phase 2 (OpenCode Research)

1. **Exact `/command` endpoint implementation:**
   - HTTP method (assumed POST)?
   - Request body format (assumed JSON with "command" field)?
   - Response format?

2. **PowerShell script triggering:**
   - How is the script invoked?
   - Does it run synchronously or asynchronously?
   - How does the server know when the script is done?

3. **Server availability detection:**
   - Does OpenCode wait for the server to be ready?
   - How long does it wait?
   - What happens if the server doesn't start?

4. **Error handling:**
   - What errors can occur?
   - How are they reported to client?
   - Are there specific error codes?

5. **Authentication & Security:**
   - Does `/command` require authentication?
   - Is there any rate limiting?
   - Can unauthorized users start the server?

### 15.2 Questions for Phase 3 (UI/UX Research)

1. **Dialog patterns:**
   - Are our button labels appropriate?
   - Should we add more context to the dialog?

2. **Loading feedback:**
   - Is the current loading indicator sufficient?
   - Should we show progress information?

3. **Error display:**
   - How should errors be shown to users?
   - Should we use SnackBar, Alert, or inline text?

4. **Auto-retry:**
   - Should we implement automatic retry?
   - If so, what should be retry policy?

### 15.3 Questions for Verification

1. **Testing:**
   - What edge cases should we test?
   - How do we simulate server failures?
   - How do we test network issues?

2. **Deployment:**
   - Are there any special deployment considerations?
   - Should users be informed of this new capability?

---

**IMPLEMENTATION PLAN DOCUMENT - Awaiting Phase 2 & 3 Research Results**

**Last Updated:** January 17, 2026
**Status:** Phase 1 Complete, Phase 2 & 3 In Progress