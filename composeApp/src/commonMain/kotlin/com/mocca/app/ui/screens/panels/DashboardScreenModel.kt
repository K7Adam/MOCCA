package com.mocca.app.ui.screens.panels

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.AppStateStore
import com.mocca.app.data.repository.BroadcastEvent
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.data.repository.ProjectRepository
import com.mocca.app.data.repository.StateCoordinator
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
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
    
    @Immutable
    
    data class State(
        // Provider data (from AppStateStore - auto-updated)
        val providers: Resource<ProviderResponse> = Resource.Loading(),
        
        // Projects (from AppStateStore - auto-updated)
        val projects: Resource<ImmutableList<Project>> = Resource.Loading(),
        val currentProject: Resource<Project> = Resource.Loading(),
        
        // Agents (from AppStateStore - auto-updated)
        val agents: Resource<ImmutableList<Agent>> = Resource.Loading(),
        
        // Tools (from AppStateStore - auto-updated)
        val tools: Resource<ImmutableList<String>> = Resource.Loading(),
        
        // Slash commands (from AppStateStore - auto-updated)
        val commands: Resource<ImmutableList<Command>> = Resource.Loading(),
        
        // Formatters
        val formatters: Resource<ImmutableList<FormatterStatus>> = Resource.Loading(),
        
        // LSP status
        val lspStatus: Resource<ImmutableList<LspStatus>> = Resource.Loading(),
        
        // VCS/Git info (from AppStateStore - auto-updated)
        val vcsInfo: Resource<VcsInfo> = Resource.Loading(),
        
        // Full Git Status (from AppStateStore - auto-updated)
        val gitStatus: GitStatusResponse? = null,
        
        // MCP servers (from AppStateStore - auto-updated)
        val mcpServers: Resource<Map<String, McpServerStatus>> = Resource.Loading(),
        
        // Global sync state (from SyncStateManager)
        val globalSyncState: GlobalSyncState = GlobalSyncState.NotSynced,
        
        // Per-repository sync states
        val repoSyncStates: Map<String, SyncState> = emptyMap(),
        
        // SSE connection status (real-time event streaming)
        val isSseConnected: Boolean = false,
        
        // Sync state
        val isSyncing: Boolean = false
    ) {
        // Derived properties for UI convenience
        val connectedProviders: ImmutableList<ProviderInfo>
            get() = ((providers as? Resource.Success)?.data?.all?.filter { it.connected } ?: emptyList()).toImmutableList()
        
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
            get() = gitStatus?.branch ?: (vcsInfo as? Resource.Success)?.data?.branch?.ifBlank { null } ?: "unknown"
            
        val changedFilesCount: Int
            get() = gitStatus?.let { it.staged.size + it.unstaged.size + it.untracked.size } ?: 0
        
        val activeLspServers: ImmutableList<LspStatus>
            get() = ((lspStatus as? Resource.Success)?.data?.filter { it.isRunning } ?: emptyList()).toImmutableList()
        
        val connectedMcpServers: ImmutableList<Pair<String, McpServerStatus>>
            get() = ((mcpServers as? Resource.Success)?.data
                ?.filter { it.value.isConnected }
                ?.map { it.key to it.value } ?: emptyList()).toImmutableList()
        
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
        observeConnectionState()
        
        // Start the sync service (if not already started)
        appStateStore.start()
    }
    
    /**
     * Observe connection state and trigger data loading when connected.
     * This prevents premature API calls before connection is established.
     */
    private fun observeConnectionState() {
        screenModelScope.launch {
            stateCoordinator.connectionStatus.collect { status ->
                if (status.isConnected) {
                    Napier.i("[DashboardScreenModel] Connection established - loading projects")
                    loadProjectsWhenConnected()
                }
            }
        }
    }
    
    /**
     * Load projects data only when connected.
     * This is called once when connection is established.
     */
    private fun loadProjectsWhenConnected() {
        // Observe projects - only start collecting when connected
        screenModelScope.launch {
            projectRepository.getProjects().collect { projects ->
                val mapped = when (projects) {
                    is Resource.Success -> Resource.Success(projects.data.toImmutableList())
                    is Resource.Loading -> Resource.Loading(projects.data?.toImmutableList())
                    is Resource.Error -> Resource.Error(projects.message, projects.data?.toImmutableList())
                }
                _state.update { it.copy(projects = mapped) }
            }
        }
        
        // Observe currentProject
        screenModelScope.launch {
            projectRepository.getCurrentProject().collect { cp ->
                _state.update { it.copy(currentProject = cp) }
            }
        }
    }
    
    /**
     * Observe centralized state from AppStateStore.
     * All data updates automatically - NO manual refresh needed.
     * 
     * NOTE: Projects are observed separately in loadProjectsWhenConnected() 
     * to prevent premature API calls before connection is established.
     */
    private fun observeAppStateStore() {
        // Observe providers
        screenModelScope.launch {
            appStateStore.providers.collect { providers ->
                _state.update { it.copy(providers = providers) }
            }
        }
        
        // Projects are observed via loadProjectsWhenConnected() - DO NOT observe here
        
        // Observe agents
        screenModelScope.launch {
            appStateStore.agents.collect { agents ->
                _state.update { it.copy(agents = Resource.Success(agents.toImmutableList())) }
            }
        }
        
        // Observe tools
        screenModelScope.launch {
            appStateStore.tools.collect { tools ->
                val mapped = when (tools) {
                    is Resource.Success -> Resource.Success(tools.data.toImmutableList())
                    is Resource.Loading -> Resource.Loading(tools.data?.toImmutableList())
                    is Resource.Error -> Resource.Error(tools.message, tools.data?.toImmutableList())
                }
                _state.update { it.copy(tools = mapped) }
            }
        }
        
        // Observe commands
        screenModelScope.launch {
            appStateStore.commands.collect { commands ->
                val mapped = when (commands) {
                    is Resource.Success -> Resource.Success(commands.data.toImmutableList())
                    is Resource.Loading -> Resource.Loading(commands.data?.toImmutableList())
                    is Resource.Error -> Resource.Error(commands.message, commands.data?.toImmutableList())
                }
                _state.update { it.copy(commands = mapped) }
            }
        }
        
        // Observe VCS/Git info
        screenModelScope.launch {
            appStateStore.vcsInfo.collect { vcsInfo ->
                _state.update { it.copy(vcsInfo = vcsInfo) }
            }
        }

        // Observe Git status (full status including changed file counts)
        screenModelScope.launch {
            appStateStore.gitStatus.collect { gitStatus ->
                _state.update { it.copy(gitStatus = gitStatus) }
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
        
        // Observe SSE connection status (real-time event streaming)
        screenModelScope.launch {
            appStateStore.isSseConnected.collect { isSseConnected ->
                _state.update { it.copy(isSseConnected = isSseConnected) }
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