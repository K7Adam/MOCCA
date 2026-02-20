package com.mocca.app.data.repository

import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import com.mocca.app.domain.model.GlobalSyncState
import com.mocca.app.domain.model.SyncState
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
    private val syncStateManager: SyncStateManager,
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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // GLOBAL SYNC STATE - From SyncStateManager (Atomic Pulse)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Global sync state - the SINGLE source of truth for data freshness.
     * This decouples "connection status" from "data freshness".
     * 
     * States:
     * - NotSynced: Never synced
     * - Syncing: Currently syncing (with progress)
     * - Fresh: All data is fresh
     * - Partial: Some data fresh, some stale
     * - Failed: Critical failure
     */
    val globalSyncState: StateFlow<GlobalSyncState> = syncStateManager.globalState
    
    /**
     * Per-repository sync states for granular visibility.
     */
    val repoSyncStates: StateFlow<Map<String, SyncState>> = syncStateManager.repoStates
    
    /**
     * Human-readable sync status for UI display.
     */
    val syncStatusText: StateFlow<String> = syncStateManager.globalState
        .map { state -> formatSyncState(state) }
        .stateIn(storeScope, SharingStarted.Eagerly, "Not synced")
    
    /**
     * Whether the app data is ready to use (at least partially synced).
     */
    val isDataReady: StateFlow<Boolean> = syncStateManager.globalState
        .map { it.isUsable }
        .stateIn(storeScope, SharingStarted.Eagerly, false)
    
    /**
     * Whether all critical repositories are fresh.
     */
    val areCriticalReposFresh: Boolean
        get() = syncStateManager.areCriticalReposFresh()
    
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
                Napier.v("[AppStateStore] Sync completed - loading all dashboard data")
                loadAllData()
                
                // Refresh Git status and other repository polling
                storeScope.launch {
                    gitRepository.getVcsInfo().collect { resource ->
                        _vcsInfo.value = resource
                    }
                }
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
            is BroadcastEvent.GlobalEvent -> {
                Napier.v("[AppStateStore] Global event: ${event.event.type}")
                // Handle global events like installation updates
                when (event.event) {
                    is ServerEvent.InstallationUpdated -> {
                        Napier.i("[AppStateStore] Installation updated - triggering full sync")
                        forceFullSync()
                    }
                    else -> { /* Other global events */ }
                }
            }
            is BroadcastEvent.InstallationUpdated -> {
                Napier.i("[AppStateStore] Installation updated broadcast - triggering full sync")
                forceFullSync()
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
     * Also loads default model/provider from server config.
     */
    private fun loadConfig() {
        storeScope.launch {
            // First, load default config from server (sets default model/provider)
            try {
                sessionRepository.loadDefaultConfig()
                Napier.i("[AppStateStore] Default config loaded from server")
            } catch (e: Exception) {
                Napier.w("[AppStateStore] Failed to load default config: ${e.message}")
            }
            
            // Load provider info
            sessionRepository.getProviderInfo().fold(
                onSuccess = { info ->
                    _providerInfo.value = info
                    Napier.i("[AppStateStore] Provider info loaded: ${info.all.size} providers")
                    
                    // Set default model if not already set
                    if (_selectedModelId.value.isEmpty()) {
                        val (modelId, providerId) = sessionRepository.getDefaultModelProvider()
                        if (modelId.isNotEmpty()) {
                            _selectedModelId.value = modelId
                            _selectedProviderId.value = providerId
                            Napier.i("[AppStateStore] Default model set: $providerId / $modelId")
                        } else {
                            // Fallback: try to find first available model
                            info.all.firstOrNull { provider ->
                                (provider.models as? Map<*, *>)?.isNotEmpty() == true
                            }?.let { provider ->
                                val modelsMap = provider.models as? Map<*, *> ?: return@let
                                val firstModelId = modelsMap.keys.firstOrNull()?.toString()
                                if (firstModelId != null) {
                                    _selectedProviderId.value = provider.id
                                    _selectedModelId.value = firstModelId
                                    Napier.i("[AppStateStore] Fallback model set: ${provider.id} / $firstModelId")
                                }
                            }
                        }
                    }
                },
                onFailure = { Napier.w("[AppStateStore] Failed to load provider info: ${it.message}") }
            )
            
            // Load modes from server
            sessionRepository.getModes().fold(
                onSuccess = { modes ->
                    _modes.value = modes
                    Napier.i("[AppStateStore] Modes loaded: ${modes.size} modes")
                    if (_selectedModeId.value == null && modes.isNotEmpty()) {
                        _selectedModeId.value = modes.first().id
                        Napier.i("[AppStateStore] Default mode set: ${modes.first().id}")
                    }
                },
                onFailure = { Napier.w("[AppStateStore] Failed to load modes: ${it.message}") }
            )
        }
    }
    
    /**
     * Load agents from server.
     * Also derives modes from agents if modes endpoint hasn't provided any.
     */
    private fun loadAgents() {
        storeScope.launch {
            agentRepository.getAgents().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val agents = resource.data
                        _agents.value = agents
                        Napier.i("[AppStateStore] Agents loaded: ${agents.size} agents")
                        
                        // If we don't have modes yet, derive them from agents
                        if (_modes.value.isEmpty() && agents.isNotEmpty()) {
                            val derivedModes = agents
                                .filter { !it.hidden }
                                .map { agent ->
                                    Mode(
                                        id = agent.name,
                                        name = agent.name,
                                        description = agent.description
                                    )
                                }
                            _modes.value = derivedModes
                            Napier.i("[AppStateStore] Derived ${derivedModes.size} modes from agents")
                            
                            // Set default mode if not set
                            if (_selectedModeId.value == null && derivedModes.isNotEmpty()) {
                                val defaultMode = sessionRepository.getDefaultMode()
                                _selectedModeId.value = if (derivedModes.any { it.id == defaultMode }) {
                                    defaultMode
                                } else {
                                    derivedModes.first().id
                                }
                                Napier.i("[AppStateStore] Default mode set from agents: ${_selectedModeId.value}")
                            }
                        }
                    }
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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNC HELPERS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Force a full sync from server.
     * Use when user explicitly requests refresh.
     */
    fun forceFullSync() {
        realtimeSyncService.forceFullSync()
    }
    
    /**
     * Format sync state for UI display.
     */
    private fun formatSyncState(state: GlobalSyncState): String {
        return when (state) {
            is GlobalSyncState.NotSynced -> "Not synced"
            is GlobalSyncState.Syncing -> {
                if (state.currentRepo != null) {
                    "Syncing ${state.currentRepo}..."
                } else {
                    "Syncing..."
                }
            }
            is GlobalSyncState.Fresh -> {
                val age = Clock.System.now().toEpochMilliseconds() - state.lastSyncMs
                when {
                    age < 5000 -> "Synced"
                    age < 60000 -> "Synced ${age / 1000}s ago"
                    else -> "Synced ${age / 60000}m ago"
                }
            }
            is GlobalSyncState.Partial -> {
                val total = state.freshRepos.size + state.staleRepos.size + state.failedRepos.size
                "${state.freshRepos.size}/$total synced"
            }
            is GlobalSyncState.Failed -> "Sync failed"
        }
    }
}
