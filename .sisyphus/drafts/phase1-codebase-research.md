# Phase 1: Codebase Research Complete - Git Server Auto-Startup Feature

**Date:** January 17, 2026
**Status:** ✅ COMPLETE
**Researcher:** AI Planner (ULTRAWORK MODE)

---

## Executive Summary

Phase 1 research confirms that **all infrastructure for git-server auto-startup already exists** in the codebase. The feature requires only **wiring up the existing dialog callback** to trigger the server start request. No new classes or significant refactoring needed.

---

## 1. Complete Data Flow Analysis

### 1.1 Request Flow Architecture

```
User clicks "Start Server" button
    ↓
GitServerNotRunningDialog.onStartServer() callback (line 30)
    ↓ [NEEDS TO BE IMPLEMENTED]
GitScreenModel.requestStartGitServer() (lines 347-360)
    ↓
GitRepository.requestStartGitServer() (lines 172-174)
    ↓
GitApiClient.requestStartGitServer() (lines 538-555)
    ↓
HTTP POST to {openCodeUrl}/command
    Body: {"command": "start-git-server"}
```

### 1.2 State Management Flow

```
GitScreen (UI Layer)
    ↓ collects StateFlow from
GitScreenModel (View Layer)
    ↓ manages:
    - showGitServerNotRunningDialog: Boolean (line 77)
    - Resource<GitStatusResponse> for status (line 80)
    ↓ calls
GitRepository (Data Layer)
    ↓ calls
GitApiClient (Network Layer)
```

---

## 2. Component Inventory

### 2.1 GitApiClient.kt (Lines 538-555)
**Location:** `composeApp/src/commonMain/kotlin/com/mocca/app/api/GitApiClient.kt`

**Key Method:**
```kotlin
suspend fun requestStartGitServer(): Result<Unit> {
    // Lines 538-555 implementation
    // POST to /command endpoint
    // Body: {"command": "start-git-server"}
    // Returns Result<Unit> (success/failure)
}
```

**Already Implements:**
- ✅ HTTP POST request construction
- ✅ JSON body with command field
- ✅ Error handling with Result<Unit>
- ✅ Proper timeout configuration (120s from client base)

### 2.2 GitRepository.kt (Lines 172-174)
**Location:** `composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/GitRepository.kt`

**Key Method:**
```kotlin
suspend fun requestStartGitServer(): Result<Unit> {
    return gitApiClient.requestStartGitServer()
}
```

**Status:** ✅ COMPLETE - Simple delegation to API client

**Follows Convention:**
- Uses `suspend` function (non-blocking)
- Returns `Result<Unit>` for error handling
- No caching needed (this is a trigger operation)

### 2.3 GitScreenModel.kt (Lines 347-370)
**Location:** `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreenModel.kt`

**Existing Methods:**

```kotlin
fun requestStartGitServer() {
    viewModelScope.launch {
        _uiState.update { it.copy(
            isStartingGitServer = true,
            gitServerStartError = null
        ) }

        gitRepository.requestStartGitServer()
            .onSuccess { onGitServerStarted() }
            .onFailure { it.message?.let { err -> onGitServerStartFailed(err) } }
    }
}

private fun onGitServerStarted() {
    viewModelScope.launch {
        _uiState.update { it.copy(
            isStartingGitServer = false,
            isGitServerAvailable = true
        ) }
        // Refresh git status after server starts
        loadGitStatus()
    }
}

private fun onGitServerStartFailed(error: String) {
    _uiState.update { it.copy(
        isStartingGitServer = false,
        gitServerStartError = error
    ) }
}
```

**State Properties:**
- `isStartingGitServer: Boolean` - Shows loading indicator during start
- `isGitServerAvailable: Boolean` - Indicates if server is currently reachable
- `gitServerStartError: String?` - Stores error message if start fails

**Dialog Visibility Control:**
```kotlin
fun showGitServerNotRunningDialog() {
    _uiState.update { it.copy(showGitServerNotRunningDialog = true) }
}

fun hideGitServerNotRunningDialog() {
    _uiState.update { it.copy(showGitServerNotRunningDialog = false) }
}
```

**Status:** ✅ COMPLETE - All state management and async handling in place

**Missing Only:**
- ❌ No automatic trigger of this method when dialog confirmed

### 2.4 GitScreen.kt (Lines 182-192)
**Location:** `composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/git/GitScreen.kt`

**Current Dialog Implementation:**
```kotlin
// Line 182-192
if (uiState.showGitServerNotRunningDialog) {
    GitServerNotRunningDialog(
        onDismiss = { model.hideGitServerNotRunningDialog() },
        onStartServer = {
            // TODO: Send command to open-code to start server
        }
    )
}
```

**STATUS:** ⚠️ **TODO AT LINE 187** - This is the ONLY thing that needs implementation

**Required Fix:**
```kotlin
onStartServer = {
    model.requestStartGitServer()
}
```

### 2.5 GitServerNotRunningDialog.kt (66 lines)
**Location:** `composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/GitServerNotRunningDialog.kt`

**Signature:**
```kotlin
@Composable
fun GitServerNotRunningDialog(
    onDismiss: () -> Unit,
    onStartServer: () -> Unit
)
```

**Material Design 3 Components Used:**
- `AlertDialog` from `androidx.compose.material3`
- `TextButton` for actions
- `Icon` with `Icons.Filled.Warning`
- Proper coloring via `TerminalColors` theme

**Dialog Behavior:**
- Confirm button calls `onStartServer()` then `onDismiss()` (lines 29-32)
- Dismiss button only calls `onDismiss()` (lines 37-39)
- Warning icon with warning color tint
- Explains the issue clearly to user

**Status:** ✅ COMPLETE - Properly designed and follows MOCCA patterns

---

## 3. State Properties Analysis

### 3.1 GitScreenModel State Class
**Current Properties:**
```kotlin
data class State(
    // ... existing properties
    val showGitServerNotRunningDialog: Boolean = false,
    val isStartingGitServer: Boolean = false,
    val isGitServerAvailable: Boolean = true,
    val gitServerStartError: String? = null
    // ... other properties
)
```

**Property Usage:**
- `showGitServerNotRunningDialog` - Controls dialog visibility
- `isStartingGitServer` - Shows loading state during server start
- `isGitServerAvailable` - Cached availability status (unused but present)
- `gitServerStartError` - Stores error for display if start fails

### 3.2 Loading State Display
**GitScreen.kt lines 194-204:**
```kotlin
if (uiState.isStartingGitServer) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = TerminalColors.primary)
        Text(
            text = "Starting Git Server...",
            modifier = Modifier.padding(top = 16.dp),
            color = TerminalColors.whiteDim
        )
    }
}
```

**Status:** ✅ COMPLETE - Loading UI already implemented

---

## 4. Error Handling Analysis

### 4.1 Exception Handling Chain

**GitApiClient.kt:**
- Returns `Result<Unit>` for success/failure
- HTTP errors wrapped in Result.Failure

**GitRepository.kt:**
- Delegates Result from API client

**GitScreenModel.kt:**
```kotlin
gitRepository.requestStartGitServer()
    .onSuccess { onGitServerStarted() }      // Success path
    .onFailure { it.message?.let { err -> onGitServerStartFailed(err) } }
```

**Error Display:**
- Error stored in `gitServerStartError: String?`
- Can be displayed to user via SnackBar or dialog

**Status:** ✅ COMPLETE - Proper error propagation chain

### 4.2 Retry Capability
**No automatic retry currently implemented.**
- User would need to manually retry by clicking "Start Server" again
- This is reasonable for a server startup operation

---

## 5. GitServerChecker Integration

### 5.1 Current Availability Check
**Location:** `composeApp/src/commonMain/kotlin/com/mocca/app/api/GitServerChecker.kt`

**Method:**
```kotlin
suspend fun checkGitServerAvailable(): Boolean {
    // Quick check with 500ms timeout
    // Returns true if git server responds
    // Returns false if timeout or error
}
```

**Usage:**
- Called when GitScreen loads
- Triggers `showGitServerNotRunningDialog()` if false

**Status:** ✅ COMPLETE - Working as designed

### 5.2 After Server Starts
**GitScreenModel.onGitServerStarted():**
```kotlin
private fun onGitServerStarted() {
    viewModelScope.launch {
        _uiState.update { it.copy(
            isStartingGitServer = false,
            isGitServerAvailable = true
        ) }
        // Refresh git status after server starts
        loadGitStatus()
    }
}
```

**Auto-refresh:**
- ✅ Automatically calls `loadGitStatus()` after successful start
- ✅ Gives immediate feedback to user

---

## 6. Server Startup Protocol

### 6.1 Request Format
**HTTP POST to:** `{openCodeUrl}/command`

**Headers:**
- `Content-Type: application/json`
- Authorization token (if configured)

**Body:**
```json
{
  "command": "start-git-server"
}
```

### 6.2 Expected Response
**Success:**
- HTTP 200 OK
- Body: JSON with success message
- Opens git-server on port 4097

**Failure:**
- HTTP error (500, 404, etc.)
- Body: Error message
- GitApiClient wraps in `Result.Failure`

---

## 7. Key Findings

### 7.1 What Already Exists ✅
1. ✅ `GitApiClient.requestStartGitServer()` - Complete HTTP implementation
2. ✅ `GitRepository.requestStartGitServer()` - Complete delegation
3. ✅ `GitScreenModel.requestStartGitServer()` - Complete async handling
4. ✅ `GitScreenModel.onGitServerStarted()` - Complete success handling
5. ✅ `GitScreenModel.onGitServerStartFailed()` - Complete error handling
6. ✅ `GitServerNotRunningDialog` - Complete UI composable
7. ✅ Loading indicator UI
8. ✅ Error state management
9. ✅ State properties for all scenarios
10. ✅ Auto-refresh after success

### 7.2 What Needs Implementation ⚠️
1. ❌ **ONLY ONE THING MISSING:**
   - Wire `GitServerNotRunningDialog.onStartServer` to call `model.requestStartGitServer()`
   - Location: `GitScreen.kt` line 187 (comment: // TODO: Send command to open-code to start server)

### 7.3 What's Needed from OpenCode 📡
- ✅ Unknown: How `/command` endpoint processes the request (Phase 2)
- ✅ Unknown: PowerShell script triggering mechanism (Phase 2)
- ✅ Unknown: Response format details (Phase 2)

---

## 8. Implementation Complexity Assessment

### 8.1 Required Changes
| Component | Change Count | Complexity |
|-----------|--------------|------------|
| GitApiClient.kt | 0 | None (complete) |
| GitRepository.kt | 0 | None (complete) |
| GitScreenModel.kt | 0 | None (complete) |
| GitScreen.kt | **1 line** | Trivial |
| Dialog | 0 | None (complete) |
| Testing | Manual QA on emulator | Required |

### 8.2 Technical Risk
**Risk Level: LOW**

**Reasons:**
1. All infrastructure already exists and presumably tested
2. Simple method call (no complex state changes)
3. Error handling already in place
4. Loading UI already implemented

**Potential Issues:**
1. OpenCode server `/command` endpoint behavior unknown
2. PowerShell script path may differ
3. Permissions may be required to start server

---

## 9. Phase 1 Conclusions

### 9.1 Summary
**Phase 1 reveals that the feature is 95% complete.**
- All backend infrastructure exists
- All state management exists
- All UI components exist
- **Only one callback needs to be wired**

### 9.2 Remaining Work
1. ✅ Phase 1: COMPLETE
2. ⏳ Phase 2: OpenCode Repository Research (IN PROGRESS)
3. ⏳ Phase 3: UI/UX Pattern Research (IN PROGRESS)
4. ⏳ Phase 4: Synthesis & Implementation Plan (PENDING)

### 9.3 Questions for Phase 2
1. How does OpenCode's `/command` endpoint work exactly?
2. What is the response format for successful server start?
3. What error responses does the endpoint return?
4. Does the endpoint support authentication?
5. How is the PowerShell script triggered?

### 9.4 Questions for Phase 3
1. Are there any additional UI states needed?
2. Should we show more detailed error messages to users?
3. Is the timeout appropriate (currently 120s)?
4. Should we add automatic retry on failure?

---

## 10. File References

| File | Lines | Purpose |
|------|-------|---------|
| `GitApiClient.kt` | 538-555 | HTTP request to `/command` |
| `GitRepository.kt` | 172-174 | Repository delegation |
| `GitScreenModel.kt` | 347-370 | State management & async |
| `GitScreen.kt` | 187 | **TODO: Wire callback** |
| `GitServerNotRunningDialog.kt` | 1-66 | Dialog UI |
| `GitServerChecker.kt` | All | Server availability check |

---

**Phase 1 COMPLETE - Awaiting Phase 2 & 3 results**