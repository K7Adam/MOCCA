package com.mocca.app.data.repository

import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import com.mocca.app.util.AppLifecycleObserver
import com.mocca.app.util.AppLifecycleState
import com.mocca.app.util.NetworkObserver
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

/**
 * Centralized app state store that maintains all application state.
 * 
 * This is the single source of truth for UI state, providing:
 * - Reactive state updates via StateFlow
 * - Automatic synchronization with server via SSE events
 * - Lifecycle-aware state management (foreground/background)
 * - Network-aware reconnection and sync
 * 
 * Architecture:
 * ```
 * Server (SSE) → EventStreamRepository → AppStateStore → UI (StateFlow)
 *                     ↓                        ↑
 *                LocalCache ←─────────────────┘
 * ```
 * 
 * Consumers should observe the StateFlows, never call refresh directly.
 */
class AppStateStore(
    private val localCache: LocalCache,
    private val eventStreamRepository: EventStreamRepository,
    private val sessionRepository: SessionRepository,
    private val connectionManager: ConnectionManager,
    private val appLifecycleObserver: AppLifecycleObserver?,
    private val networkObserver: NetworkObserver?,
    private val mcpRepository: McpRepository,
    private val configRepository: ConfigRepository,
    private val agentRepository: AgentRepository
) {
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION STATE - Reactive from DB + SSE updates
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()
    
    private val _runningSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val runningSessionIds: StateFlow<Set<String>> = _runningSessionIds.asStateFlow()
    
    // Current active session (for chat screen)
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONNECTION STATE - From ConnectionManager
    // ═══════════════════════════════════════════════════════════════════════════════
    
    val connectionStatus: StateFlow<ConnectionStatus> = connectionManager.status
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONFIG STATE - Models, providers, agents
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _providerInfo = MutableStateFlow<ProviderResponse?>(null)
    val providerInfo: StateFlow<ProviderResponse?> = _providerInfo.asStateFlow()
    
    private val _selectedProviderId = MutableStateFlow("")
    val selectedProviderId: StateFlow<String> = _selectedProviderId.asStateFlow()
    
    private val _selectedModelId = MutableStateFlow("")
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()
    
    private val _selectedVariantId = MutableStateFlow<String?>(null)
    val selectedVariantId: StateFlow<String?> = _selectedVariantId.asStateFlow()
    
    private val _modes = MutableStateFlow<List<Mode>>(emptyList())
    val modes: StateFlow<List<Mode>> = _modes.asStateFlow()
    
    private val _selectedModeId = MutableStateFlow<String?>(null)
    val selectedModeId: StateFlow<String?> = _selectedModeId.asStateFlow()
    
    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()
    
    private val _recentModels = MutableStateFlow<List<RecentModel>>(emptyList())
    val recentModels: StateFlow<List<RecentModel>> = _recentModels.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MCP STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    val mcpServers: Flow<Map<String, McpServerInfo>> = mcpRepository.mcpServers
    val isMcpLoading: Flow<Boolean> = mcpRepository.isLoading
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNC STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()
    
    private var isInitialized = false
    private var syncJob: Job? = null
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    init {
        Napier.i("[AppStateStore] Initializing...")
        
        // Wire up callbacks for lifecycle and connection events
        eventStreamRepository.onAppResume = { syncFromServer() }
        connectionManager.onConnectionEstablished = suspend { 
            start()
            syncFromServer()
        }
        
        observeLocalCache()
        observeSseEvents()
        observeLifecycle()
        observeConnectionState()
    }
    
    /**
     * Start observing and syncing state.
     * Called when app starts or when connection is established.
     */
    fun start() {
        if (isInitialized) return
        isInitialized = true
        
        Napier.i("[AppStateStore] Starting state observation")
        
        // Load initial state from cache
        loadFromCache()
        
        // Sync from server if connected
        if (connectionStatus.value.isConnected) {
            syncFromServer()
        }
    }
    
    /**
     * Observe local database for reactive updates.
     * This ensures UI is always in sync with DB changes.
     */
    private fun observeLocalCache() {
        // Observe sessions from DB
        storeScope.launch {
            localCache.observeAllSessions().collect { sessions ->
                _sessions.value = sessions.sortedByDescending { it.updatedAt }
                Napier.v("[AppStateStore] Sessions updated from cache: ${sessions.size}")
            }
        }
        
        // Observe recent models from DB
        storeScope.launch {
            localCache.getRecentModels().let { models ->
                _recentModels.value = models
            }
        }
    }
    
    /**
     * Observe SSE events for real-time updates.
     * This is the primary driver of state changes.
     */
    private fun observeSseEvents() {
        storeScope.launch {
            eventStreamRepository.events.collect { event ->
                handleSseEvent(event)
            }
        }
    }
    
    /**
     * Observe app lifecycle for foreground/background handling.
     */
    private fun observeLifecycle() {
        appLifecycleObserver?.let { observer ->
            storeScope.launch {
                observer.lifecycleState.collect { state ->
                    when (state) {
                        AppLifecycleState.FOREGROUND -> {
                            Napier.i("[AppStateStore] App foregrounded - syncing state")
                            onForeground()
                        }
                        AppLifecycleState.BACKGROUND -> {
                            Napier.i("[AppStateStore] App backgrounded")
                            // State is maintained, SSE continues if session is active
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Observe connection state for reconnection sync.
     */
    private fun observeConnectionState() {
        storeScope.launch {
            connectionManager.status.collect { status ->
                if (status.isConnected && !_isSyncing.value) {
                    Napier.i("[AppStateStore] Connection established - syncing state")
                    syncFromServer()
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EVENT HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun handleSseEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.SessionUpdated -> {
                val session = event.properties.info
                val isRunning = session.status == SessionStatus.RUNNING
                
                _runningSessionIds.update { current ->
                    if (isRunning) current + session.id else current - session.id
                }
                
                // Session is already persisted by EventStreamRepository
                // DB observer will pick up the change
                Napier.v("[AppStateStore] Session updated: ${session.id}, running: $isRunning")
            }
            
            is ServerEvent.SessionIdle -> {
                val sessionId = event.properties.sessionID
                _runningSessionIds.update { it - sessionId }
                Napier.v("[AppStateStore] Session idle: $sessionId")
            }
            
            is ServerEvent.SessionError -> {
                event.properties.sessionID?.let { sessionId ->
                    _runningSessionIds.update { it - sessionId }
                }
            }
            
            is ServerEvent.SessionDeleted -> {
                val sessionId = event.properties.info.id
                _runningSessionIds.update { it - sessionId }
                // DB observer will pick up the deletion
            }
            
            else -> { /* Other events handled by specialized stores */ }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Called when app comes to foreground.
     * Syncs any state that may have changed while backgrounded.
     */
    private fun onForeground() {
        storeScope.launch {
            // Check if we need to sync (if more than 30 seconds since last sync)
            val now = Clock.System.now().toEpochMilliseconds()
            val lastSync = _lastSyncTime.value
            val needsSync = lastSync == null || (now - lastSync) > 30_000L
            
            if (needsSync && connectionStatus.value.isConnected) {
                syncFromServer()
            }
            
            // Always refresh session status on foreground
            refreshSessionStatus()
        }
    }
    
    /**
     * Load initial state from local cache.
     */
    private fun loadFromCache() {
        storeScope.launch {
            try {
                // Sessions are loaded via DB observer
                val sessions = localCache.getAllSessions()
                _sessions.value = sessions.sortedByDescending { it.updatedAt }
                
                // Load recent models
                _recentModels.value = localCache.getRecentModels()
                
                Napier.i("[AppStateStore] Loaded ${sessions.size} sessions from cache")
            } catch (e: Exception) {
                Napier.e("[AppStateStore] Failed to load from cache", e)
            }
        }
    }
    
    /**
     * Sync all state from server.
     * This is called on connection, foreground, and can be triggered manually.
     */
    fun syncFromServer() {
        syncJob?.cancel()
        syncJob = storeScope.launch {
            _isSyncing.value = true
            
            try {
                // Sync sessions
                sessionRepository.refreshSessions()
                
                // Sync config
                loadConfig()
                
                // Sync agents
                loadAgents()
                
                // Refresh MCP
                mcpRepository.refresh()
                
                // Refresh session status
                refreshSessionStatus()
                
                _lastSyncTime.value = Clock.System.now().toEpochMilliseconds()
                Napier.i("[AppStateStore] Sync completed successfully")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("[AppStateStore] Sync failed", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Refresh real-time session status (idle/busy).
     */
    private suspend fun refreshSessionStatus() {
        sessionRepository.getSessionStatus().fold(
            onSuccess = { statusMap ->
                val runningIds = statusMap.filter { (_, status) ->
                    status.isBusy || status.isRetrying
                }.keys
                _runningSessionIds.value = runningIds
                Napier.v("[AppStateStore] Refreshed session status: ${runningIds.size} running")
            },
            onFailure = { error ->
                Napier.w("[AppStateStore] Failed to refresh session status: ${error.message}")
            }
        )
    }
    
    /**
     * Load config from server (providers, models, modes).
     */
    private suspend fun loadConfig() {
        // Load provider info
        sessionRepository.getProviderInfo().fold(
            onSuccess = { info ->
                _providerInfo.value = info
                
                // Set default model if not already set
                if (_selectedModelId.value.isEmpty()) {
                    sessionRepository.getDefaultModelProvider().let { (modelId, providerId) ->
                        if (modelId.isNotEmpty()) {
                            _selectedModelId.value = modelId
                            _selectedProviderId.value = providerId
                        }
                    }
                }
            },
            onFailure = { Napier.w("[AppStateStore] Failed to load provider info") }
        )
        
        // Load modes
        sessionRepository.getModes().fold(
            onSuccess = { modes ->
                _modes.value = modes
                if (_selectedModeId.value == null && modes.isNotEmpty()) {
                    _selectedModeId.value = modes.first().id
                }
            },
            onFailure = { Napier.w("[AppStateStore] Failed to load modes") }
        )
    }
    
    /**
     * Load agents from server.
     */
    private suspend fun loadAgents() {
        agentRepository.getAgents().collect { resource ->
            when (resource) {
                is Resource.Success -> _agents.value = resource.data
                is Resource.Error -> Napier.w("[AppStateStore] Failed to load agents: ${resource.message}")
                else -> {}
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATE MUTATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Set the current active session.
     */
    fun setCurrentSession(sessionId: String?) {
        _currentSessionId.value = sessionId
    }
    
    /**
     * Select a model/provider combination.
     */
    fun selectModel(providerId: String, modelId: String) {
        _selectedProviderId.value = providerId
        _selectedModelId.value = modelId
        _selectedVariantId.value = null
        
        // Save to recent models
        storeScope.launch {
            localCache.insertRecentModel(RecentModel(providerId, modelId, Clock.System.now().toEpochMilliseconds()))
            _recentModels.value = localCache.getRecentModels()
        }
    }
    
    /**
     * Select a variant.
     */
    fun selectVariant(variantId: String?) {
        _selectedVariantId.value = variantId
    }
    
    /**
     * Select a mode.
     */
    fun selectMode(modeId: String?) {
        _selectedModeId.value = modeId
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DERIVED STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get session groups (parent-child hierarchy).
     */
    val sessionGroups: StateFlow<List<SessionGroup>> = combine(_sessions, _runningSessionIds) { sessions, runningIds ->
        buildSessionGroups(sessions, runningIds)
    }.stateIn(storeScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Whether any session is currently running.
     */
    val hasAnyRunningSession: StateFlow<Boolean> = _runningSessionIds.map { it.isNotEmpty() }
        .stateIn(storeScope, SharingStarted.WhileSubscribed(5000), false)
    
    /**
     * Model display name for UI.
     */
    val modelName: StateFlow<String> = _selectedModelId.map { modelId ->
        modelId.ifEmpty { "--" }.uppercase().replace("-", " ").take(30)
    }.stateIn(storeScope, SharingStarted.WhileSubscribed(5000), "--")
    
    /**
     * Mode display name for UI.
     */
    val modeName: StateFlow<String> = combine(_selectedModeId, _modes) { modeId, modes ->
        modeId?.let { id ->
            modes.find { it.id == id }?.description ?: id.uppercase()
        } ?: "--"
    }.stateIn(storeScope, SharingStarted.WhileSubscribed(5000), "--")
    
    /**
     * Build session groups from flat session list.
     */
    private fun buildSessionGroups(sessions: List<Session>, runningIds: Set<String>): List<SessionGroup> {
        // Separate root sessions from child/internal sessions
        val rootSessions = sessions.filter { session ->
            val hasParent = !session.effectiveParentID.isNullOrBlank()
            val isInternal = session.title.orEmpty().let { title ->
                title.startsWith("Background:") || 
                title.startsWith("look_at:") ||
                title.contains("subagent", ignoreCase = true)
            }
            !hasParent && !isInternal
        }
        
        // Group children by parent ID
        val childrenByParent = sessions.filter { session ->
            val hasParent = !session.effectiveParentID.isNullOrBlank()
            val isInternal = session.title.orEmpty().let { title ->
                title.startsWith("Background:") || 
                title.startsWith("look_at:") ||
                title.contains("subagent", ignoreCase = true)
            }
            hasParent || isInternal
        }.groupBy { it.effectiveParentID ?: "internal" }
        
        return rootSessions.map { parent ->
            val children = childrenByParent[parent.id] ?: emptyList()
            SessionGroup(
                parent = parent,
                children = children.sortedByDescending { it.updatedAt },
                isExpanded = true
            )
        }.sortedByDescending { it.lastActivityTime }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════════
    
    fun dispose() {
        storeScope.cancel()
        Napier.i("[AppStateStore] Disposed")
    }
}
