package com.mocca.app.data.repository

import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

/**
 * Centralized app state store that maintains ALL application state.
 * 
 * This is the SINGLE SOURCE OF TRUTH for ALL UI state, providing:
 * - Reactive state updates via StateFlow
 * - Automatic synchronization with server via StateCoordinator + RealtimeSyncService
 * - Lifecycle-aware state management (foreground/background)
 * - Network-aware reconnection and sync
 * - Periodic polling for data without SSE events
 * 
 * Architecture:
 * ```
 * Server (SSE) → EventStreamRepository → StateCoordinator → AppStateStore → UI (StateFlow)
 *                     ↓                        ↓                  ↑
 *                LocalCache ←────────────────────────────────────┘
 *                     
 * RealtimeSyncService (periodic polling)
 *     ├── MCP Server Status
 *     ├── Git/VCS Status
 *     └── Other non-SSE data
 *           ↓
 *     AppStateStore.StateFlows update
 *           ↓
 *     UI updates automatically (NO manual refresh needed)
 * ```
 * 
 * IMPORTANT: Consumers should ONLY observe StateFlows.
 * NEVER call refresh manually - all updates are automatic.
 */
class AppStateStore(
    private val localCache: LocalCache,
    private val stateCoordinator: StateCoordinator,
    private val sessionRepository: SessionRepository,
    private val mcpRepository: McpRepository,
    private val configRepository: ConfigRepository,
    private val agentRepository: AgentRepository,
    private val providerRepository: ProviderRepository,
    private val toolRepository: ToolRepository,
    private val commandRepository: CommandRepository,
    private val gitRepository: GitRepository,
    private val realtimeSyncService: RealtimeSyncService
) {
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SESSION STATE - Reactive from DB + StateCoordinator updates
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()
    
    // Running sessions - delegated to StateCoordinator
    val runningSessionIds: StateFlow<Set<String>> = stateCoordinator.runningSessionIds
    
    // Current active session (for chat screen)
    val currentSessionId: StateFlow<String?> = stateCoordinator.activeSessionId
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONNECTION STATE - From StateCoordinator
    // ═══════════════════════════════════════════════════════════════════════════════
    
    val connectionStatus: StateFlow<ConnectionStatus> = stateCoordinator.connectionStatus
    
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
    // MCP STATE - Reactive from McpRepository (updated by RealtimeSyncService)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _mcpServers = MutableStateFlow<Map<String, McpServerInfo>>(emptyMap())
    val mcpServers: StateFlow<Map<String, McpServerInfo>> = _mcpServers.asStateFlow()
    
    private val _isMcpLoading = MutableStateFlow(false)
    val isMcpLoading: StateFlow<Boolean> = _isMcpLoading.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PROVIDER STATE - Reactive from ProviderRepository (updated by RealtimeSyncService)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _providers = MutableStateFlow<Resource<ProviderResponse>>(Resource.Loading())
    val providers: StateFlow<Resource<ProviderResponse>> = _providers.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TOOL STATE - Reactive from ToolRepository (updated by RealtimeSyncService)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _tools = MutableStateFlow<Resource<List<String>>>(Resource.Loading())
    val tools: StateFlow<Resource<List<String>>> = _tools.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // COMMAND STATE - Reactive from CommandRepository (updated by RealtimeSyncService)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _commands = MutableStateFlow<Resource<List<Command>>>(Resource.Loading())
    val commands: StateFlow<Resource<List<Command>>> = _commands.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // GIT/VCS STATE - Reactive from GitRepository (updated by RealtimeSyncService)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private val _vcsInfo = MutableStateFlow<Resource<VcsInfo>>(Resource.Loading())
    val vcsInfo: StateFlow<Resource<VcsInfo>> = _vcsInfo.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNC STATE - From StateCoordinator + RealtimeSyncService
    // ═══════════════════════════════════════════════════════════════════════════════
    
    val isSyncing: StateFlow<Boolean> = combine(
        stateCoordinator.isSyncing,
        realtimeSyncService.isSyncing
    ) { a, b -> a || b }.stateIn(storeScope, SharingStarted.Eagerly, false)
    
    val lastSyncTime: StateFlow<Long?> = realtimeSyncService.lastSyncTime
    
    /**
     * Trigger a full sync from server.
     * Delegates to StateCoordinator.
     */
    fun syncFromServer() {
        stateCoordinator.syncFromServer()
    }
    
    private var isInitialized = false
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    init {
        Napier.i("[AppStateStore] Initializing with StateCoordinator + RealtimeSyncService...")
        
        observeLocalCache()
        observeBroadcastEvents()
        observeRepositoryFlows()
    }
    
    /**
     * Start observing and syncing state.
     * Called when app starts or when connection is established.
     */
    fun start() {
        if (isInitialized) return
        isInitialized = true
        
        Napier.i("[AppStateStore] Starting state observation and realtime sync")
        
        // Load initial state from cache
        loadFromCache()
        
        // Start realtime sync service (handles periodic polling + connection sync)
        realtimeSyncService.start()
        
        // Load all data initially
        loadAllData()
    }
    
    /**
     * Called when app comes to foreground.
     * Triggers sync via RealtimeSyncService.
     */
    fun onForeground() {
        realtimeSyncService.onAppForeground()
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
     * Observe repository StateFlows for reactive updates.
     * These are updated by RealtimeSyncService periodically.
     */
    private fun observeRepositoryFlows() {
        // Observe MCP servers
        storeScope.launch {
            mcpRepository.mcpServers.collect { servers ->
                _mcpServers.value = servers
            }
        }
        
        storeScope.launch {
            mcpRepository.isLoading.collect { loading ->
                _isMcpLoading.value = loading
            }
        }
        
        // Observe Git/VCS info
        storeScope.launch {
            gitRepository.getVcsInfo().collect { resource ->
                _vcsInfo.value = resource
            }
        }
    }
    
    /**
     * Load all data from server.
     * Called on start and triggered by RealtimeSyncService.
     */
    private fun loadAllData() {
        loadConfig()
        loadAgents()
        loadProviders()
        loadTools()
        loadCommands()
    }
    
    /**
     * Load providers from server.
     */
    private fun loadProviders() {
        storeScope.launch {
            providerRepository.getProviders().collect { resource ->
                _providers.value = resource
                if (resource is Resource.Success) {
                    Napier.v("[AppStateStore] Providers loaded: ${resource.data.all.size}")
                }
            }
        }
    }
    
    /**
     * Load tools from server.
     */
    private fun loadTools() {
        storeScope.launch {
            toolRepository.getToolIds().collect { resource ->
                _tools.value = resource
                if (resource is Resource.Success) {
                    Napier.v("[AppStateStore] Tools loaded: ${resource.data.size}")
                }
            }
        }
    }
    
    /**
     * Load commands from server.
     */
    private fun loadCommands() {
        storeScope.launch {
            commandRepository.getCommands().collect { resource ->
                _commands.value = resource
                if (resource is Resource.Success) {
                    Napier.v("[AppStateStore] Commands loaded: ${resource.data.size}")
                }
            }
        }
    }
    
    /**
     * Observe broadcast events from StateCoordinator.
     * This is the primary driver of state changes.
     */
    private fun observeBroadcastEvents() {
        storeScope.launch {
            stateCoordinator.broadcastEvents.collect { event ->
                handleBroadcastEvent(event)
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EVENT HANDLING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun handleBroadcastEvent(event: BroadcastEvent) {
        when (event) {
            is BroadcastEvent.ServerEvent -> handleServerEvent(event.event)
            is BroadcastEvent.SyncCompleted -> {
                Napier.v("[AppStateStore] Sync completed - loading config")
                loadConfig()
                loadAgents()
            }
            is BroadcastEvent.SyncFailed -> {
                Napier.w("[AppStateStore] Sync failed: ${event.error}")
            }
            is BroadcastEvent.ConnectionStateChanged -> {
                Napier.v("[AppStateStore] Connection state: ${event.status}")
            }
            is BroadcastEvent.ActiveSessionChanged -> {
                Napier.v("[AppStateStore] Active session changed: ${event.sessionId}")
            }
        }
    }
    
    private fun handleServerEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.SessionUpdated -> {
                // Session is already persisted by EventStreamRepository
                // DB observer will pick up the change
                Napier.v("[AppStateStore] Session updated: ${event.properties.info.id}")
            }
            
            is ServerEvent.SessionDeleted -> {
                // DB observer will pick up the deletion
                Napier.v("[AppStateStore] Session deleted: ${event.properties.info.id}")
            }
            
            else -> { /* Other events handled by specialized stores */ }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNC OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
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
     * Load config from server (providers, models, modes).
     */
    private fun loadConfig() {
        storeScope.launch {
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
    }
    
    /**
     * Load agents from server.
     */
    private fun loadAgents() {
        storeScope.launch {
            agentRepository.getAgents().collect { resource ->
                when (resource) {
                    is Resource.Success -> _agents.value = resource.data
                    is Resource.Error -> Napier.w("[AppStateStore] Failed to load agents: ${resource.message}")
                    else -> {}
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATE MUTATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Set the current active session.
     */
    suspend fun setCurrentSession(sessionId: String?) {
        stateCoordinator.setActiveSession(sessionId)
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
    val sessionGroups: StateFlow<List<SessionGroup>> = combine(_sessions, runningSessionIds) { sessions, runningIds ->
        buildSessionGroups(sessions, runningIds)
    }.stateIn(storeScope, SharingStarted.Eagerly, emptyList())
    
    /**
     * Whether any session is currently running.
     */
    val hasAnyRunningSession: StateFlow<Boolean> = runningSessionIds.map { it.isNotEmpty() }
        .stateIn(storeScope, SharingStarted.Eagerly, false)
    
    /**
     * Model display name for UI.
     */
    val modelName: StateFlow<String> = _selectedModelId.map { modelId ->
        modelId.ifEmpty { "--" }.uppercase().replace("-", " ").take(30)
    }.stateIn(storeScope, SharingStarted.Eagerly, "--")
    
    /**
     * Mode display name for UI.
     */
    val modeName: StateFlow<String> = combine(_selectedModeId, _modes) { modeId, modes ->
        modeId?.let { id ->
            modes.find { it.id == id }?.description ?: id.uppercase()
        } ?: "--"
    }.stateIn(storeScope, SharingStarted.Eagerly, "--")
    
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
        realtimeSyncService.stop()
        storeScope.cancel()
        Napier.i("[AppStateStore] Disposed")
    }
}
