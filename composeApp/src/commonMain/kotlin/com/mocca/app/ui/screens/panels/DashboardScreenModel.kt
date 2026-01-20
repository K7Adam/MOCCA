package com.mocca.app.ui.screens.panels

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.*
import com.mocca.app.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for DashboardPanel.
 * Aggregates data from all OpenCode feature endpoints.
 */
class DashboardScreenModel(
    private val providerRepository: ProviderRepository,
    private val agentRepository: AgentRepository,
    private val toolRepository: ToolRepository,
    private val commandRepository: CommandRepository,
    private val formatterRepository: FormatterRepository,
    private val lspRepository: LspRepository,
    private val gitRepository: GitRepository,
    private val mcpRepository: McpRepository,
    private val eventStreamRepository: EventStreamRepository,
    private val projectRepository: ProjectRepository
) : ScreenModel {
    
    data class State(
        // Provider data
        val providers: Resource<ProviderResponse> = Resource.Loading(),
        
        // Projects
        val projects: Resource<List<Project>> = Resource.Loading(),
        val currentProject: Resource<Project> = Resource.Loading(),
        
        // Agents
        val agents: Resource<List<Agent>> = Resource.Loading(),
        
        // Tools
        val tools: Resource<List<String>> = Resource.Loading(),
        
        // Slash commands
        val commands: Resource<List<Command>> = Resource.Loading(),
        
        // Formatters
        val formatters: Resource<List<FormatterStatus>> = Resource.Loading(),
        
        // LSP status
        val lspStatus: Resource<List<LspStatus>> = Resource.Loading(),
        
        // VCS/Git info
        val vcsInfo: Resource<VcsInfo> = Resource.Loading(),
        
        // MCP servers
        val mcpServers: Resource<Map<String, McpServerStatus>> = Resource.Loading()
    ) {
        // Derived properties for UI convenience
        val connectedProviders: List<ProviderInfo>
            get() = (providers as? Resource.Success)?.data?.all?.filter { it.connected } ?: emptyList()
        
        // Get provider count from the connected list
        val providerCount: Int
            get() = (providers as? Resource.Success)?.data?.connected?.size ?: 0
        
        // Get first default provider/model pair for display
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
            get() = (lspStatus as? Resource.Success)?.data?.filter { it.status == "running" } ?: emptyList()
        
        val connectedMcpServers: List<Pair<String, McpServerStatus>>
            get() = (mcpServers as? Resource.Success)?.data
                ?.filter { it.value.isConnected }
                ?.map { it.key to it.value } ?: emptyList()
        
        val isLoading: Boolean
            get() = listOf(providers, agents, tools, commands, formatters, lspStatus, vcsInfo, mcpServers)
                .any { it is Resource.Loading }
        
        val hasErrors: Boolean
            get() = listOf(providers, agents, tools, commands, formatters, lspStatus, vcsInfo, mcpServers)
                .any { it is Resource.Error }
    }
    
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    
    init {
        observeEvents()
        loadAllData()
    }
    
    private fun observeEvents() {
        screenModelScope.launch {
            eventStreamRepository.events.collect { event ->
                if (event is ServerEvent.FileEdited || event is ServerEvent.FileWatcherUpdated) {
                    loadVcsInfo()
                }
            }
        }
        
        screenModelScope.launch {
            gitRepository.getVcsInfo().collect { resource ->
                _state.update { it.copy(vcsInfo = resource) }
            }
        }
        
        screenModelScope.launch {
            mcpRepository.mcpServers.collect { servers ->
                _state.update { state ->
                    state.copy(
                        mcpServers = Resource.Success(servers.values.associateBy({ it.name }, { it.status }))
                    )
                }
            }
        }
    }
    
    /**
     * Load all initial data.
     */
    fun loadAllData() {
        screenModelScope.launch {
            loadProviders()
        }
        screenModelScope.launch {
            loadAgents()
        }
        screenModelScope.launch {
            loadTools()
        }
        screenModelScope.launch {
            loadCommands()
        }
        screenModelScope.launch {
            loadFormatters()
        }
        screenModelScope.launch {
            loadLspStatus()
        }
        screenModelScope.launch {
            loadVcsInfo()
        }
        screenModelScope.launch {
            loadMcpServers()
        }
        screenModelScope.launch {
            loadProjects()
        }
        screenModelScope.launch {
            loadCurrentProject()
        }
    }
    
    private suspend fun loadProviders() {
        providerRepository.getProviders().collect { resource ->
            _state.update { it.copy(providers = resource) }
        }
    }
    
    private suspend fun loadProjects() {
        projectRepository.getProjects().collect { resource ->
            _state.update { it.copy(projects = resource) }
        }
    }

    private suspend fun loadCurrentProject() {
        projectRepository.getCurrentProject().collect { resource ->
            _state.update { it.copy(currentProject = resource) }
        }
    }
    
    private suspend fun loadAgents() {
        agentRepository.getAgents().collect { resource ->
            _state.update { it.copy(agents = resource) }
        }
    }
    
    private suspend fun loadTools() {
        toolRepository.getToolIds().collect { resource ->
            _state.update { it.copy(tools = resource) }
        }
    }
    
    private suspend fun loadCommands() {
        commandRepository.getCommands().collect { resource ->
            _state.update { it.copy(commands = resource) }
        }
    }
    
    private suspend fun loadFormatters() {
        formatterRepository.getFormatters().collect { resource ->
            _state.update { it.copy(formatters = resource) }
        }
    }
    
    private suspend fun loadLspStatus() {
        lspRepository.getLspStatus().collect { resource ->
            _state.update { it.copy(lspStatus = resource) }
        }
    }
    
    private suspend fun loadVcsInfo() {
        gitRepository.getVcsInfo().collect { resource ->
            _state.update { it.copy(vcsInfo = resource) }
        }
    }
    
    private suspend fun loadMcpServers() {
        val resource = mcpRepository.refresh()
        val mcpResource: Resource<Map<String, McpServerStatus>> = when (resource) {
            is Resource.Success -> Resource.Success(resource.data.mapValues { it.value.status })
            is Resource.Loading -> Resource.Loading(resource.data?.mapValues { it.value.status })
            is Resource.Error -> Resource.Error(resource.message, resource.data?.mapValues { it.value.status })
        }
        _state.update { it.copy(mcpServers = mcpResource) }
    }
    
    /**
     * Refresh all data.
     */
    fun refresh() {
        loadAllData()
    }
    
    /**
     * Connect to an MCP server.
     */
    fun connectMcpServer(name: String) {
        screenModelScope.launch {
            mcpRepository.connect(name)
            loadMcpServers()
        }
    }
    
    /**
     * Disconnect from an MCP server.
     */
    fun disconnectMcpServer(name: String) {
        screenModelScope.launch {
            mcpRepository.disconnect(name)
            loadMcpServers()
        }
    }
}