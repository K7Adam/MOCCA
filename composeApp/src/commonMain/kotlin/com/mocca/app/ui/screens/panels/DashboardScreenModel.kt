package com.mocca.app.ui.screens.panels

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.AppStateStore
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.data.repository.ProjectRepository
import com.mocca.app.data.repository.StateCoordinator
import com.mocca.app.data.repository.SystemMonitorRepository
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private const val SYSTEM_MONITOR_UNAVAILABLE_MESSAGE = "Connect MOCCA CLI to enable system monitor"

/** Dashboard screen model. Observes AppStateStore for auto-updates. */
class DashboardScreenModel(
    private val appStateStore: AppStateStore,
    private val stateCoordinator: StateCoordinator,
    private val mcpRepository: McpRepository,
    private val projectRepository: ProjectRepository,
    private val systemMonitorRepository: SystemMonitorRepository
) : ScreenModel {

    @Immutable
    data class SystemMonitorState(
        val processes: Resource<ImmutableList<ProcessInfo>> = Resource.Loading(),
        val ports: Resource<ImmutableList<PortInfo>> = Resource.Loading(),
        val resources: Resource<SystemResources> = Resource.Loading(),
        val refreshInterval: MonitorRefreshInterval = MonitorRefreshInterval.SECONDS_15,
        val lastUpdatedAt: Long? = null,
        val isRefreshing: Boolean = false,
        val isAvailable: Boolean = false
    )
    
    @Immutable
    data class State(
        // From AppStateStore (auto-updated)
        val providers: Resource<ProviderResponse> = Resource.Loading(),
        val projects: Resource<ImmutableList<Project>> = Resource.Loading(),
        val currentProject: Resource<Project> = Resource.Loading(),
        val agents: Resource<ImmutableList<Agent>> = Resource.Loading(),
        val tools: Resource<ImmutableList<String>> = Resource.Loading(),
        val commands: Resource<ImmutableList<Command>> = Resource.Loading(),
        val vcsInfo: Resource<VcsInfo> = Resource.Loading(),
        val gitStatus: GitStatusResponse? = null,
        val mcpServers: Resource<Map<String, McpServerStatus>> = Resource.Loading(),
        val currentSessionId: String? = null,
        val systemMonitor: SystemMonitorState = SystemMonitorState(),
        val isSseConnected: Boolean = false,
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
        
        val connectedMcpServers: ImmutableList<Pair<String, McpServerStatus>>
            get() = ((mcpServers as? Resource.Success)?.data
                ?.filter { it.value.isConnected }
                ?.map { it.key to it.value } ?: emptyList()).toImmutableList()
        
        val isLoading: Boolean
            get() = isSyncing || listOf(providers, agents, tools, commands, vcsInfo, mcpServers)
                .any { it is Resource.Loading }
        
        val hasErrors: Boolean
            get() = listOf(providers, agents, tools, commands, vcsInfo, mcpServers)
                .any { it is Resource.Error }

        val hasActiveSession: Boolean
            get() = !currentSessionId.isNullOrBlank()
    }
    
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var systemMonitorJob: Job? = null
    
    init {
        observeAppStateStore()
        observeEvents()
        observeConnectionState()
        observeActiveSession()
        observeNativeMonitorAvailability()
        startSystemMonitorPolling()
        appStateStore.start()
    }
    
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
    
    private fun loadProjectsWhenConnected() {
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
        
        screenModelScope.launch {
            projectRepository.getCurrentProject().collect { cp ->
                _state.update { it.copy(currentProject = cp) }
            }
        }
    }
    
    private fun observeAppStateStore() {
        screenModelScope.launch {
            appStateStore.providers.collect { providers ->
                _state.update { it.copy(providers = providers) }
            }
        }
        
        screenModelScope.launch {
            appStateStore.agents.collect { agents ->
                _state.update { it.copy(agents = Resource.Success(agents.toImmutableList())) }
            }
        }
        
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
        
        screenModelScope.launch {
            appStateStore.vcsInfo.collect { vcsInfo ->
                _state.update { it.copy(vcsInfo = vcsInfo) }
            }
        }

        screenModelScope.launch {
            appStateStore.gitStatus.collect { gitStatus ->
                _state.update { it.copy(gitStatus = gitStatus) }
            }
        }
        
        screenModelScope.launch {
            appStateStore.mcpServers.collect { servers ->
                _state.update { state ->
                    state.copy(
                        mcpServers = Resource.Success(servers.mapValues { it.value.status })
                    )
                }
            }
        }
        
        screenModelScope.launch {
            appStateStore.isSyncing.collect { isSyncing ->
                _state.update { it.copy(isSyncing = isSyncing) }
            }
        }
        
        screenModelScope.launch {
            appStateStore.isSseConnected.collect { isSseConnected ->
                _state.update { it.copy(isSseConnected = isSseConnected) }
            }
        }
    }

    private fun observeActiveSession() {
        screenModelScope.launch {
            appStateStore.currentSessionId.collect { sessionId ->
                _state.update { state ->
                    state.copy(
                        currentSessionId = sessionId,
                        systemMonitor = if (sessionId.isNullOrBlank()) {
                            state.systemMonitor.copy(isRefreshing = false)
                        } else {
                            state.systemMonitor
                        }
                    )
                }
                if (!sessionId.isNullOrBlank()) {
                    refreshSystemMonitor()
                }
            }
        }
    }

    private fun observeNativeMonitorAvailability() {
        screenModelScope.launch {
            systemMonitorRepository.nativeMonitorAvailable.collect { available ->
                _state.update { state ->
                    val nextMonitor = if (available) {
                        state.systemMonitor.copy(isAvailable = true)
                    } else {
                        state.systemMonitor.copy(
                            processes = state.systemMonitor.processes.toUnavailableMonitorResource(),
                            ports = state.systemMonitor.ports.toUnavailableMonitorResource(),
                            resources = state.systemMonitor.resources.toUnavailableMonitorResource(),
                            isRefreshing = false,
                            isAvailable = false
                        )
                    }
                    state.copy(systemMonitor = nextMonitor)
                }
                if (available) {
                    refreshSystemMonitor()
                }
            }
        }
    }
    
    private fun observeEvents() {
        screenModelScope.launch {
            stateCoordinator.broadcastEvents.collect { broadcastEvent ->
                when (broadcastEvent) {
                    is BroadcastEvent.ServerEvent -> {
                        val event = broadcastEvent.event
                        if (event is ServerEvent.FileEdited || event is ServerEvent.FileWatcherUpdated) {
                            // Git status refreshed by sync service
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startSystemMonitorPolling() {
        systemMonitorJob?.cancel()
        systemMonitorJob = screenModelScope.launch {
            while (isActive) {
                val snapshot = _state.value
                val interval = snapshot.systemMonitor.refreshInterval.pollMs
                if (interval == null) {
                    delay(1_000)
                    continue
                }

                if (!systemMonitorRepository.isNativeMonitorAvailable()) {
                    markSystemMonitorUnavailable()
                    delay(1_000)
                    continue
                }

                refreshSystemMonitor()
                delay(interval)
            }
        }
    }

    fun cycleSystemMonitorRefreshInterval() {
        _state.update { state ->
            state.copy(
                systemMonitor = state.systemMonitor.copy(
                    refreshInterval = state.systemMonitor.refreshInterval.next()
                )
            )
        }
    }

    private fun refreshSystemMonitor() {
        if (_state.value.systemMonitor.refreshInterval == MonitorRefreshInterval.OFF) return
        if (_state.value.systemMonitor.isRefreshing) return
        if (!systemMonitorRepository.isNativeMonitorAvailable()) {
            markSystemMonitorUnavailable()
            return
        }

        screenModelScope.launch {
            val current = _state.value.systemMonitor
            _state.update { state ->
                state.copy(systemMonitor = state.systemMonitor.copy(isRefreshing = true))
            }

            val processesDeferred = async { systemMonitorRepository.getProcesses() }
            val portsDeferred = async { systemMonitorRepository.getPorts() }
            val resourcesDeferred = async { systemMonitorRepository.getSystemResources() }

            val processes = processesDeferred.await().toImmutableListResource()
            val ports = portsDeferred.await().toImmutableListResource()
            val resources = resourcesDeferred.await()

            val anySucceeded = processes is Resource.Success || ports is Resource.Success || resources is Resource.Success
            val updatedAt = if (anySucceeded) Clock.System.now().toEpochMilliseconds() else current.lastUpdatedAt

            _state.update { state ->
                state.copy(
                    systemMonitor = state.systemMonitor.copy(
                        processes = mergeResource(state.systemMonitor.processes, processes),
                        ports = mergeResource(state.systemMonitor.ports, ports),
                        resources = mergeResource(state.systemMonitor.resources, resources),
                        lastUpdatedAt = updatedAt,
                        isRefreshing = false,
                        isAvailable = true
                    )
                )
            }
        }
    }

    private fun markSystemMonitorUnavailable() {
        _state.update { state ->
            state.copy(
                systemMonitor = state.systemMonitor.copy(
                    processes = state.systemMonitor.processes.toUnavailableMonitorResource(),
                    ports = state.systemMonitor.ports.toUnavailableMonitorResource(),
                    resources = state.systemMonitor.resources.toUnavailableMonitorResource(),
                    isRefreshing = false,
                    isAvailable = false
                )
            )
        }
    }

    private fun <T> Resource<List<T>>.toImmutableListResource(): Resource<ImmutableList<T>> =
        map { it.toImmutableList() }

    private fun <T> Resource<T>.toUnavailableMonitorResource(): Resource<T> =
        Resource.Error(SYSTEM_MONITOR_UNAVAILABLE_MESSAGE, dataOrNull())

    private fun <T> mergeResource(current: Resource<T>, incoming: Resource<T>): Resource<T> {
        return when (incoming) {
            is Resource.Success -> incoming
            is Resource.Loading -> Resource.Loading(incoming.data ?: current.dataOrNull())
            is Resource.Error -> Resource.Error(incoming.message, incoming.data ?: current.dataOrNull(), incoming.cause)
        }
    }
    
    fun syncNow() {
        appStateStore.syncFromServer()
    }
    
    fun forceFullSync() {
        appStateStore.forceFullSync()
        refreshSystemMonitor()
    }
    
    fun onForeground() {
        appStateStore.onForeground()
    }
    
    fun connectMcpServer(name: String) {
        screenModelScope.launch {
            mcpRepository.connect(name)
        }
    }
    
    fun disconnectMcpServer(name: String) {
        screenModelScope.launch {
            mcpRepository.disconnect(name)
        }
    }
}
