package com.mocca.app.ui.screens.panels

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.AppStateStore
import com.mocca.app.data.repository.BroadcastEvent
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.data.repository.ProjectRepository
import com.mocca.app.data.repository.StateCoordinator
import com.mocca.app.domain.model.*
import com.mocca.app.domain.model.GlobalSyncState
import com.mocca.app.domain.model.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for DashboardPanel.
 * 
 * IMPORTANT: This now observes AppStateStore for ALL data.
 * NO manual refresh calls are needed - all updates are automatic via:
 * - StateCoordinator (SSE events for sessions, messages)
 * - RealtimeSyncService (periodic polling for MCP, providers, tools, git)
 * 
 * The dashboard will ALWAYS show fresh data without any user action.
 */
class DashboardScreenModel(
    private val appStateStore: AppStateStore,
    private val stateCoordinator: StateCoordinator,
    private val mcpRepository: McpRepository,
    private val projectRepository: ProjectRepository
) : ScreenModel {
    
    data class State(
        // Provider data (from AppStateStore - auto-updated)
        val providers: Resource<ProviderResponse> = Resource.Loading(),
        
        // Projects (from AppStateStore - auto-updated)
        val projects: Resource<List<Project>> = Resource.Loading(),
        val currentProject: Resource<Project> = Resource.Loading(),
        
        // Agents (from AppStateStore - auto-updated)
        val agents: Resource<List<Agent>> = Resource.Loading(),
        
        // Tools (from AppStateStore - auto-updated)
        val tools: Resource<List<String>> = Resource.Loading(),
        
        // Slash commands (from AppStateStore - auto-updated)
        val commands: Resource<List<Command>> = Resource.Loading(),
        
        // Formatters
        val formatters: Resource<List<FormatterStatus>> = Resource.Loading(),
        
        // LSP status
        val lspStatus: Resource<List<LspStatus>> = Resource.Loading(),
        
        // VCS/Git info (from AppStateStore - auto-updated)
        val vcsInfo: Resource<VcsInfo> = Resource.Loading(),
        
        // MCP servers (from AppStateStore - auto-updated)
        val mcpServers: Resource<Map<String, McpServerStatus>> = Resource.Loading(),
        
        // Global sync state (from SyncStateManager)
        val globalSyncState: GlobalSyncState = GlobalSyncState.NotSynced,
        
        // Per-repository sync states
        val repoSyncStates: Map<String, SyncState> = emptyMap(),
        
        // Sync state
        val isSyncing: Boolean = false
    ) {
        // Derived properties for UI convenience
        val connectedProviders: List<ProviderInfo>
            get() = (providers as? Resource.Success)?.data?.all?.filter { it.connected } ?: emptyList()
        
        val providerCount: Int
            get() = (providers as? Resource.Success)?.data?.connected?.size ?: 0
        
        val defaultProviderName: String?
            get() = (providers as? Resource.Success)?.data?.default?.keys?.firstOrNull()
        
        val defaultModelName: String?
            get() = (providers as? Resource.Success)?.data?.default?.values?.firstOrNull()
        
        val toolCount: Int
            get() = (tools as? Resource.Success)?.data?.size ?: 0
        
        val agentCount: Int
            get() = (agents as? Resource.Success)?.data?.size ?: 0
        
        val commandCount: Int
            get() = (commands as? Resource.Success)?.data?.size ?: 0
        
        val gitBranch: String
            get() = (vcsInfo as? Resource.Success)?.data?.branch ?: "unknown"
        
        val gitDirty: Boolean
            get() = (vcsInfo as? Resource.Success)?.data?.dirty ?: false
        
        val gitAhead: Int
            get() = (vcsInfo as? Resource.Success)?.data?.ahead ?: 0
        
        val gitBehind: Int
            get() = (vcsInfo as? Resource.Success)?.data?.behind ?: 0
        
        val gitChangeCount: Int
            get() = (vcsInfo as? Resource.Success)?.data?.changeCount ?: 0
        
        val activeLspServers: List<LspStatus>
            get() = (lspStatus as? Resource.Success)?.data?.filter { it.isRunning } ?: emptyList()
        
        val connectedMcpServers: List<Pair<String, McpServerStatus>>
            get() = (mcpServers as? Resource.Success)?.data
                ?.filter { it.value.isConnected }
                ?.map { it.key to it.value } ?: emptyList()
        
        val isLoading: Boolean
            get() = isSyncing || listOf(providers, agents, tools, commands, formatters, lspStatus, vcsInfo, mcpServers)
                .any { it is Resource.Loading }
        
        val hasErrors: Boolean
            get() = listOf(providers, agents, tools, commands, formatters, lspStatus, vcsInfo, mcpServers)
                .any { it is Resource.Error }
    }
    
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    
    init {
        // Start observing centralized state from AppStateStore
        observeAppStateStore()
        observeEvents()
        
        // Start the sync service (if not already started)
        appStateStore.start()
    }
    
    /**
     * Observe centralized state from AppStateStore.
     * All data updates automatically - NO manual refresh needed.
     */
    private fun observeAppStateStore() {
        // Observe providers
        screenModelScope.launch {
            appStateStore.providers.collect { providers ->
                _state.update { it.copy(providers = providers) }
            }
        }
        
        // Observe projects
        screenModelScope.launch {
            projectRepository.getProjects().collect { projects ->
                _state.update { it.copy(projects = projects) }
            }
        }
        
        // Observe currentProject
        screenModelScope.launch {
            projectRepository.getCurrentProject().collect { cp ->
                _state.update { it.copy(currentProject = cp) }
            }
        }
        
        // Observe agents
        screenModelScope.launch {
            appStateStore.agents.collect { agents ->
                _state.update { it.copy(agents = Resource.Success(agents)) }
            }
        }
        
        // Observe tools
        screenModelScope.launch {
            appStateStore.tools.collect { tools ->
                _state.update { it.copy(tools = tools) }
            }
        }
        
        // Observe commands
        screenModelScope.launch {
            appStateStore.commands.collect { commands ->
                _state.update { it.copy(commands = commands) }
            }
        }
        
        // Observe VCS/Git info
        screenModelScope.launch {
            appStateStore.vcsInfo.collect { vcsInfo ->
                _state.update { it.copy(vcsInfo = vcsInfo) }
            }
        }
        
        // Observe MCP servers
        screenModelScope.launch {
            appStateStore.mcpServers.collect { servers ->
                _state.update { state ->
                    state.copy(
                        mcpServers = Resource.Success(servers.mapValues { it.value.status })
                    )
                }
            }
        }
        
        // Observe sync state
        screenModelScope.launch {
            appStateStore.isSyncing.collect { isSyncing ->
                _state.update { it.copy(isSyncing = isSyncing) }
            }
        }
        
        // Observe global sync state
        screenModelScope.launch {
            appStateStore.globalSyncState.collect { globalSyncState ->
                _state.update { it.copy(globalSyncState = globalSyncState) }
            }
        }
        
        // Observe per-repository sync states
        screenModelScope.launch {
            appStateStore.repoSyncStates.collect { repoSyncStates ->
                _state.update { it.copy(repoSyncStates = repoSyncStates) }
            }
        }
    }
    
    private fun observeEvents() {
        // Observe file events for Git refresh
        screenModelScope.launch {
            stateCoordinator.broadcastEvents.collect { broadcastEvent ->
                when (broadcastEvent) {
                    is BroadcastEvent.ServerEvent -> {
                        val event = broadcastEvent.event
                        if (event is ServerEvent.FileEdited || event is ServerEvent.FileWatcherUpdated) {
                            // Git status will be refreshed by RealtimeSyncService
                            // No manual action needed
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Trigger an immediate sync.
     * This is optional - data syncs automatically.
     */
    fun syncNow() {
        appStateStore.syncFromServer()
    }
    
    /**
     * Force a full sync of all data.
     * Use when user explicitly requests refresh.
     */
    fun forceFullSync() {
        appStateStore.forceFullSync()
    }
    
    /**
     * Called when app comes to foreground.
     */
    fun onForeground() {
        appStateStore.onForeground()
    }
    
    /**
     * Connect to an MCP server.
     * Delegates to McpRepository which will update AppStateStore via StateFlow.
     */
    fun connectMcpServer(name: String) {
        screenModelScope.launch {
            mcpRepository.connect(name)
            // MCP servers StateFlow will update automatically
        }
    }
    
    /**
     * Disconnect from an MCP server.
     * Delegates to McpRepository which will update AppStateStore via StateFlow.
     */
    fun disconnectMcpServer(name: String) {
        screenModelScope.launch {
            mcpRepository.disconnect(name)
            // MCP servers StateFlow will update automatically
        }
    }
}