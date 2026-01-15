# MOCCA Developer Settings Analysis

**Generated**: 2026-01-14
**Project**: MOCCA (Mobile OpenCode Companion App)
**Purpose**: Identify useful configurations for hidden developer settings screen

---

## EXECUTIVE SUMMARY

Based on comprehensive codebase analysis, MOCCA has **42 tunable parameters** across network, UI, database, authentication, and debugging layers. A hidden developer settings screen would provide powerful debugging, testing, and optimization capabilities for advanced users and developers.

---

## CATEGORY 1: NETWORK CONFIGURATIONS (9 tunables)

### HTTP Client Settings
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Request Timeout | 120s | HttpClientProvider.kt | **Adjustable**: 30s-300s (for testing slow/fast servers) |
| Connect Timeout | 10s | HttpClientProvider.kt | **Adjustable**: 5s-30s |
| Socket Timeout | 120s | HttpClientProvider.kt | **Adjustable**: 30s-300s |
| JSON Lenient Mode | true | HttpClientProvider.kt | **Toggle**: Strict/Lenient parsing |
| Ignore Unknown Keys | true | HttpClientProvider.kt | **Toggle**: Fail on unknown fields (debug mode) |

### SSE Configuration
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Reconnect Delay | 3s | MoccaSseClient.kt | **Adjustable**: 1s-10s (faster/slower reconnection) |
| Max Reconnect Attempts | 10 | EventStreamRepository.kt | **Adjustable**: 1-100 (infinite option for dev) |
| Event Buffer Size | 128 | EventStreamRepository.kt | **Adjustable**: 32-1024 (handle high-volume events) |
| WS Ping Interval | 30s | HttpClientProvider.kt | **Adjustable**: 10s-120s (WebSocket keep-alive) |

### Retry Policy
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Default Max Retries | 3 | RetryPolicy.kt | **Adjustable**: 0-10 (disable retries for testing) |
| Aggressive Max Retries | 5 | RetryPolicy.kt | **Adjustable**: 1-20 (SSE-specific) |
| Backoff Factor | 2.0x | RetryPolicy.kt | **Adjustable**: 1.0x-5.0x (linear to aggressive exponential) |
| Initial Delay | 1000ms | RetryPolicy.kt | **Adjustable**: 100ms-10s |
| Max Delay | 30s | RetryPolicy.kt | **Adjustable**: 5s-120s |

---

## CATEGORY 2: UI & ANIMATION SETTINGS (6 tunables)

### Swipe Navigation
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Panel Animation Duration | 250ms | SwipePanelLayout.kt | **Adjustable**: 0ms-1000ms (disable animations: 0ms) |
| Swipe Threshold | 0.3f | SwipePanelLayout.kt | **Adjustable**: 0.1f-0.9f (easier/harder to trigger) |
| Panel Easing | FastOutSlowInEasing | SwipePanelLayout.kt | **Toggle**: Linear/Custom easing |

### Terminal Effects
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Typing Delay | 50ms | TerminalText.kt | **Adjustable**: 0ms-500ms (instant: 0ms) |
| Erase Delay | 40ms | TerminalText.kt | **Adjustable**: 0ms-500ms |
| Cursor Blink Duration | 530ms | TerminalText.kt | **Adjustable**: 0ms (disabled)-2000ms |
| Pause Delay | 2000ms | TerminalText.kt | **Adjustable**: 0ms-10000ms |

---

## CATEGORY 3: DATA & PERFORMANCE SETTINGS (4 tunables)

### Cache Management
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Event Buffer Capacity | 128 | EventStreamRepository.kt | **Adjustable**: 32-2048 (monitor memory impact) |
| Cache TTL | None (infinite) | N/A | **Feature**: Add TTL (1min-24h) for automatic invalidation |
| Session Auto-Purge | Manual only | N/A | **Feature**: Auto-purge older than X days |

### Database Operations
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Query Timeout | Default SQLite | LocalCache.android.kt | **Adjustable**: 5s-60s (for large queries) |
| Batch Size | None (implicit) | N/A | **Feature**: Batch insert size for imports |

---

## CATEGORY 4: AUTHENTICATION & SECURITY (3 tunables)

### Authentication Modes
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Auth Type | Bearer/Basic/NONE | Config.kt | **Toggle**: Enable experimental auth methods |
| Token Refresh Strategy | Auto-on-config-change | HttpClientProvider.kt | **Feature**: Manual refresh button |
| Cleartext Traffic | Allowed (local) | network_security_config.xml | **Toggle**: Force HTTPS for all connections |

---

## CATEGORY 5: LOGGING & DEBUGGING (8 tunables)

### Log Levels
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| App Log Level | DEBUG | MoccaApp.kt (Napier) | **Adjustable**: VERBOSE-ERROR |
| Koin Log Level | ERROR | Modules.kt | **Adjustable**: INFO-ERROR |
| Network Logs | Implicit | HTTP Client | **Toggle**: Enable request/response logging |
| SSE Event Logs | Implicit | EventStreamRepository.kt | **Toggle**: Log all events (high volume) |

### Debug Features
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Crash Reporting | None | N/A | **Feature**: Toggle Sentry/Firebase integration |
| Analytics | None | N/A | **Feature**: Toggle analytics collection |
| Stack Trace Display | Standard | N/A | **Toggle**: Show full stack traces in UI |
| Debug Layout Overlays | None | N/A | **Feature**: Enable Compose layout inspector |

---

## CATEGORY 6: API & INTEGRATION (5 tunables)

### API Configuration
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Base URL Override | From ServerConfig | ServerConfigRepository.kt | **Feature**: Override active server URL (dev server) |
| API Version | Fixed | MoccaApiClient.kt | **Feature**: Version selector (v1/v2/beta) |
| Enable Experimental Endpoints | Partial | MoccaApiClient.kt | **Toggle**: Enable `/experimental/*` endpoints |

### Feature Flags
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Session Sync | isSynced flag | Session.sq | **Toggle**: Enable background sync |
| MCP Integration | Enabled | McpRepository.kt | **Toggle**: Disable MCP (fallback mode) |
| LSP Real-time | Enabled | EventStreamRepository.kt | **Toggle**: Disable LSP diagnostics streaming |

---

## CATEGORY 7: TERMINAL & EMULATION (3 tunables)

### WebSocket Terminal
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Terminal Font Size | System default | TerminalScreen.kt | **Adjustable**: 10sp-24sp |
| Terminal Scrollback | Implicit | TerminalScreen.kt | **Adjustable**: 100-10000 lines |
| Auto-reconnect WS | Enabled | TerminalRepository.kt | **Toggle**: Disable auto-reconnect |

---

## CATEGORY 8: TESTING & MOCKING (4 tunables)

### Development Tools
| Parameter | Current Value | Location | Developer Value |
|-----------|---------------|----------|-----------------|
| Mock API Responses | None | N/A | **Feature**: Enable mock mode for offline testing |
| Fake SSE Events | None | N/A | **Feature**: Inject fake events for UI testing |
| Force Network Failure | None | N/A | **Feature**: Simulate network conditions |
| Response Delay Injection | None | N/A | **Feature**: Add artificial delays to test loading states |

---

## RECOMMENDED DEVELOPER SETTINGS CATEGORIES

### 1. Performance Tuning
**Purpose**: Optimize app performance for different network conditions
- All network timeouts (Request, Connect, Socket)
- Retry policies (count, backoff)
- SSE reconnection settings
- Cache TTL and buffer sizes

### 2. UI/UX Debugging
**Purpose**: Test animations and responsiveness
- Animation durations (disable for testing)
- Typing/cursor effects (disable for speed)
- Swipe thresholds (adjust sensitivity)
- Layout inspector overlays

### 3. Network Debugging
**Purpose**: Debug connectivity and API issues
- Request/response logging
- SSE event logging
- Force network failure mode
- Response delay injection
- API version selector

### 4. Feature Flags
**Purpose**: Enable/disable experimental features
- MCP integration toggle
- LSP real-time diagnostics toggle
- Session background sync toggle
- Experimental endpoints access

### 5. Data Management
**Purpose**: Manage local cache and storage
- Clear all caches button
- Export database (SQLite dump)
- Import database
- Auto-purge old sessions
- Reset app state (factory reset)

### 6. Authentication Testing
**Purpose**: Test different auth methods
- Force cleartext traffic (for local dev)
- Auth method selector
- Token refresh manual trigger
- Simulate auth failure

### 7. Terminal Configuration
**Purpose**: Customize terminal experience
- Font size adjustment
- Scrollback buffer size
- WS auto-reconnect toggle
- Terminal color themes (add themes)

---

## HIDDEN MENU ACTIVATION PATTERNS

### Recommended: Multi-Tap on Version Number
**Location**: SettingsScreen.kt → "About Application" section
**Pattern**: 7 taps on version text (e.g., "v2.4.0-STABLE")
**Rationale**: Industry standard, unobtrusive, easy to discover

### Alternative Patterns
1. **Long-press on app icon** (Home screen) - 3 seconds
2. **Secret gesture**: Triple-tap on the bottom-right corner of MainScreen
3. **Shake-to-reveal**: Shake device 3 times rapidly (requires accelerometer)
4. **Special command**: Type `/dev` in chat input (if chat is accessible)
5. **Keyboard shortcut**: Ctrl+Shift+D (physical keyboard only)

**Security Considerations**:
- Add confirmation dialog: "Developer Mode: This may expose sensitive information and affect app stability. Enable?"
- Add toggle in Settings to permanently enable/disable (after first activation)
- Add pin/biometric protection for sensitive operations (database export, token viewing)

---

## IMPLEMENTATION PRIORITY

### Phase 1: High-Value Quick Wins (Days 1-2)
- Network timeout adjustments (critical for testing)
- Disable animations toggle (speed testing)
- Log level adjustment (debugging)
- Clear cache button (common need)
- Force network failure simulation (offline testing)

### Phase 2: Core Debugging Tools (Days 3-4)
- Request/response logging
- SSE event logging
- Feature flags (MCP, LSP, Sync)
- API version selector
- Base URL override

### Phase 3: Advanced Testing (Days 5-7)
- Mock API responses
- Fake SSE event injection
- Database export/import
- Terminal configuration
- Performance metrics dashboard

### Phase 4: Polish & Security (Days 8-10)
- Hidden menu activation (multi-tap pattern)
- Pin/biometric protection
- Developer mode toggle (persistent)
- Crash reporting integration
- Analytics toggle

---

## TECHNICAL IMPLEMENTATION NOTES

### Storage Strategy
Use **ServerConfigEntity** table extension:
```kotlin
// Add to ServerConfig.sq
CREATE TABLE DeveloperPrefs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    key TEXT NOT NULL UNIQUE,
    value TEXT NOT NULL
);
```

### Key-Value Mapping
```kotlin
data class DeveloperPref(
    val key: String,  // e.g., "network.request_timeout"
    val value: String // JSON-encoded any type
)
```

### Live Reload
Modify `HttpClientProvider` to observe `DeveloperPrefsRepository` and recreate client on timeout changes:
```kotlin
val prefs = developerPrefsRepository.getPrefsFlow()
prefs.collectLatest { config ->
    if (config.networkTimeoutsChanged) {
        refreshHttpClient()
    }
}
```

### State Management
Create `DeveloperSettingsScreenModel` with `StateFlow<DeveloperSettingsState>`:
```kotlin
data class DeveloperSettingsState(
    val networkTimeouts: NetworkTimeouts,
    val uiAnimations: UIAnimations,
    val loggingLevels: LoggingLevels,
    val featureFlags: FeatureFlags,
    // ...
)
```

---

## EXAMPLE UI STRUCTURE

```
Developer Settings (Hidden)
├── Performance Tuning
│   ├── Network Timeouts [Expandable]
│   │   ├── Request Timeout: 120s [Slider: 30-300]
│   │   ├── Connect Timeout: 10s [Slider: 5-30]
│   │   └── Socket Timeout: 120s [Slider: 30-300]
│   ├── Retry Policy [Expandable]
│   │   ├── Max Retries: 3 [Stepper: 0-10]
│   │   ├── Backoff Factor: 2.0x [Slider: 1.0-5.0]
│   │   └── Initial Delay: 1000ms [Slider: 100-10000]
│   └── SSE Configuration [Expandable]
│       ├── Reconnect Delay: 3s [Slider: 1-10]
│       ├── Max Retries: 10 [Stepper: 1-100]
│       └── Buffer Size: 128 [Stepper: 32-1024]
├── Debugging
│   ├── Log Levels
│   │   ├── App Log: DEBUG [Dropdown: VERBOSE, DEBUG, INFO, WARN, ERROR]
│   │   └── Network Logs: [Toggle]
│   ├── Network Debugging
│   │   ├── Log Requests/Responses [Toggle]
│   │   ├── Log SSE Events [Toggle]
│   │   ├── Force Network Failure [Toggle]
│   │   └── Response Delay: 0ms [Slider: 0-5000]
│   └── Layout Inspector [Toggle]
├── Feature Flags
│   ├── MCP Integration [Toggle: ON]
│   ├── LSP Real-time Diagnostics [Toggle: ON]
│   ├── Session Background Sync [Toggle: OFF]
│   └── Enable Experimental Endpoints [Toggle: OFF]
├── Data Management
│   ├── Clear All Caches [Button]
│   ├── Export Database [Button]
│   ├── Import Database [Button]
│   ├── Auto-Purge Sessions: Never [Dropdown: Never, 7 days, 30 days, 90 days]
│   └── Reset App State [Button: DANGER]
├── UI/UX
│   ├── Animations [Expandable]
│   │   ├── Panel Animation: 250ms [Slider: 0-1000]
│   │   ├── Typing Delay: 50ms [Slider: 0-500]
│   │   ├── Cursor Blink: 530ms [Slider: 0-2000]
│   │   └── Disable All Animations [Toggle]
│   └── Swipe Sensitivity: 0.3 [Slider: 0.1-0.9]
├── API Configuration
│   ├── Base URL Override: [Text Input]
│   ├── API Version: v1 [Dropdown: v1, v2, beta]
│   └── Force Cleartext Traffic [Toggle: OFF]
└── About
    ├── Version: v2.4.0-STABLE (tap 7 times to hide this menu)
    └── Build: ANDROID_KMP
```

---

## RATIONALE FOR EACH SETTING

### Why These Settings Matter for MOCCA

1. **Network Timeouts**: AI operations (chat, git, LSP indexing) can take 60-180 seconds. Developers need to test with different timeout values to handle various server loads.

2. **Retry Policies**: Unstable mobile networks require aggressive retrying. Adjusting retry behavior helps test edge cases and optimize battery usage.

3. **SSE Configuration**: Real-time event streaming is critical for tool approval and message updates. Tuning buffer sizes prevents event loss during high-volume periods.

4. **UI Animations**: Terminal-style apps should be snappy. Developers need to test with animations disabled to ensure performance is acceptable.

5. **Log Levels**: Debugging connection issues or SSE event problems requires verbose logging. Toggleable logging prevents log spam in production.

6. **Feature Flags**: MCP and LSP integrations may fail on certain servers. Disabling them allows testing with minimal functionality.

7. **Mock Mode**: Offline testing without a real server requires mocking API responses and SSE events.

8. **Database Export**: Bug reproduction often requires the exact state of the local SQLite database.

9. **API Version Selector**: Testing against different OpenCode server versions requires version switching.

10. **Force Cleartext**: Local development often uses HTTP. Forcing HTTPS would break local dev, so a toggle is essential.

---

## FILE LOCATIONS FOR IMPLEMENTATION

| Component | File Path |
|-----------|-----------|
| Settings UI | `/composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SettingsScreen.kt` |
| Settings Model | `/composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/settings/SettingsScreenModel.kt` |
| Server Config Schema | `/composeApp/src/commonMain/sqldelight/com/mocca/app/db/ServerConfig.sq` |
| HTTP Client Factory | `/composeApp/src/commonMain/kotlin/com/mocca/app/api/HttpClientProvider.kt` |
| SSE Client | `/composeApp/src/commonMain/kotlin/com/mocca/app/api/MoccaSseClient.kt` |
| Retry Policy | `/composeApp/src/commonMain/kotlin/com/mocca/app/api/RetryPolicy.kt` |
| Event Stream Repository | `/composeApp/src/commonMain/kotlin/com/mocca/app/data/repository/EventStreamRepository.kt` |
| Swipe Panel Layout | `/composeApp/src/commonMain/kotlin/com/mocca/app/ui/navigation/SwipePanelLayout.kt` |
| Terminal Text Component | `/composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/terminal/TerminalText.kt` |
| Koin DI Modules | `/composeApp/src/commonMain/kotlin/com/mocca/app/di/Modules.kt` |
| App Initialization | `/androidApp/src/main/java/com/mocca/app/MoccaApp.kt` |
| Network Security Config | `/androidApp/src/main/res/xml/network_security_config.xml` |

---

## CONCLUSION

MOCCA has a rich configuration surface with **42 tunable parameters** across 8 categories. A hidden developer settings screen would significantly enhance debugging, testing, and optimization capabilities for both developers and advanced users.

**Recommended activation**: 7-tap pattern on version number in Settings → About section.

**Estimated implementation time**: 10 days for full feature set, 2 days for high-value quick wins.

**Risk assessment**: Low (all settings are optional, defaults remain unchanged).

**User impact**: High (improves developer productivity, enables better testing and debugging).
