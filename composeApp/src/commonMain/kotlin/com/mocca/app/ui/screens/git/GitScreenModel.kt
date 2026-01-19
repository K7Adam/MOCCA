package com.mocca.app.ui.screens.git

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.GitRepository
import com.mocca.app.domain.model.*
import com.mocca.app.api.GitApiClient.GitServerNotRunningException
import com.mocca.app.api.NetworkError
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Screen model for Git operations screen.
 */
class GitScreenModel(
    private val gitRepository: GitRepository
) : ScreenModel {
    
    // UI State
    private val _uiState = MutableStateFlow(GitUiState())
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()
    
    // Currently selected tab
    private val _selectedTab = MutableStateFlow(GitTab.STATUS)
    val selectedTab: StateFlow<GitTab> = _selectedTab.asStateFlow()
    
    init {
        loadStatus()
    }
    
    fun selectTab(tab: GitTab) {
        _selectedTab.value = tab
        when (tab) {
            GitTab.STATUS -> loadStatus()
            GitTab.BRANCHES -> loadBranches()
            GitTab.LOG -> loadLog()
            GitTab.REMOTES -> loadRemotes()
        }
    }
    
    fun loadStatus() {
        screenModelScope.launch {
            gitRepository.getStatus().collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(
                            isLoading = true,
                            status = resource.data ?: state.status
                        )
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            status = resource.data,
                            error = null
                        )
                        is Resource.Error -> {
                            // Check if error is "server not running" - handles both direct and wrapped exceptions
                            val cause = resource.cause
                            val isServerNotRunning = cause is GitServerNotRunningException ||
                                cause is NetworkError.GitServerUnavailable ||
                                cause?.cause is GitServerNotRunningException ||
                                resource.message.contains("Git server is not running", ignoreCase = true)
                            
                            if (isServerNotRunning) {
                                // Extract details if available
                                val exception = (cause as? GitServerNotRunningException) 
                                    ?: (cause?.cause as? GitServerNotRunningException)
                                
                                val isRefused = exception?.isConnectionRefused ?: false
                                val url = exception?.url
                                
                                Napier.i("GitServerNotRunning detected, showing dialog (refused: $isRefused)")
                                showServerNotRunningDialog(isRefused, url)
                            }
                            state.copy(
                                isLoading = false,
                                status = resource.data ?: state.status,
                                error = resource.message
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun loadBranches() {
        screenModelScope.launch {
            gitRepository.getBranches().collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            branches = resource.data,
                            error = null
                        )
                        is Resource.Error -> state.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }
    
    fun loadLog(branch: String? = null, skip: Int = 0) {
        screenModelScope.launch {
            gitRepository.getLog(branch, skip = skip).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            log = resource.data,
                            error = null
                        )
                        is Resource.Error -> state.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }
    
    fun loadRemotes() {
        screenModelScope.launch {
            gitRepository.getRemotes().collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            remotes = resource.data,
                            error = null
                        )
                        is Resource.Error -> state.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }
    
    fun stageFile(path: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.stage(listOf(path))
                .onSuccess { loadStatus() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    fun stageAll() {
        val unstaged = _uiState.value.status?.unstaged?.map { it.path } ?: return
        val untracked = _uiState.value.status?.untracked ?: emptyList()
        val allFiles = unstaged + untracked
        if (allFiles.isEmpty()) return
        
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.stage(allFiles)
                .onSuccess { loadStatus() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    fun unstageFile(path: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.unstage(listOf(path))
                .onSuccess { loadStatus() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    fun unstageAll() {
        val staged = _uiState.value.status?.staged?.map { it.path } ?: return
        if (staged.isEmpty()) return
        
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.unstage(staged)
                .onSuccess { loadStatus() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    fun discardFile(path: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.discard(listOf(path))
                .onSuccess { loadStatus() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
    
    fun commit(message: String) {
        if (message.isBlank()) {
            _uiState.update { it.copy(warningMessage = "Commit message cannot be empty") }
            return
        }
        
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.commit(message)
                .onSuccess { result ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        showCommitDialog = false,
                        commitMessage = "",
                        operationResult = result.message ?: "Commit successful"
                    ) }
                    loadStatus()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        warningMessage = error.message ?: "Operation failed"
                    ) }
                }
        }
    }
    
    fun push(force: Boolean = false) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.push(force = force)
                .onSuccess { result ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        operationResult = result.message ?: "Push successful"
                    ) }
                    loadStatus()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        warningMessage = error.message ?: "Operation failed"
                    ) }
                }
        }
    }
    
    fun pull(rebase: Boolean = false) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.pull(rebase = rebase)
                .onSuccess { result ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        operationResult = result.message ?: "Pull successful"
                    ) }
                    loadStatus()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        warningMessage = error.message ?: "Operation failed"
                    ) }
                }
        }
    }
    
    fun fetch() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.fetch()
                .onSuccess { result ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        operationResult = result.message ?: "Fetch successful"
                    ) }
                    loadStatus()
                    loadBranches()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        warningMessage = error.message ?: "Operation failed"
                    ) }
                }
        }
    }
    
    fun checkout(ref: String, create: Boolean = false) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.checkout(ref, create)
                .onSuccess { result ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        operationResult = result.message ?: "Checkout successful"
                    ) }
                    loadStatus()
                    loadBranches()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        warningMessage = error.message ?: "Operation failed"
                    ) }
                }
        }
    }
    
    fun showCommitDialog() {
        _uiState.update { it.copy(showCommitDialog = true) }
    }
    
    fun hideCommitDialog() {
        _uiState.update { it.copy(showCommitDialog = false, commitMessage = "") }
    }
    
    fun updateCommitMessage(message: String) {
        _uiState.update { it.copy(commitMessage = message) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearOperationResult() {
        _uiState.update { it.copy(operationResult = null) }
    }
    
    fun clearWarning() {
        _uiState.update { it.copy(warningMessage = null) }
    }

    fun showServerNotRunningDialog(isConnectionRefused: Boolean = false, url: String? = null) {
        Napier.i("showServerNotRunningDialog() called, updating state (refused: $isConnectionRefused, url: $url)")
        
        // If connection was refused on localhost (127.0.0.1) on an emulator, it likely means ADB reverse is missing
        val isLocalhost = url?.contains("127.0.0.1") == true || url?.contains("localhost") == true
        val isAdbReverseMissing = isConnectionRefused && isLocalhost
        
        if (isAdbReverseMissing) {
            Napier.w("Detected possible missing ADB reverse setup!")
        }
        
        _uiState.update { 
            it.copy(
                showServerNotRunningDialog = true,
                showAdbReverseHelp = isAdbReverseMissing
            ) 
        }
    }

    fun hideServerNotRunningDialog() {
        _uiState.update { it.copy(showServerNotRunningDialog = false, showAdbReverseHelp = false) }
    }

    fun showServerStartedDialog() {
        _uiState.update { it.copy(showServerStartedDialog = true) }
    }

    fun hideServerStartedDialog() {
        _uiState.update { it.copy(showServerStartedDialog = false) }
    }

    fun requestStartGitServer() {
        Napier.i("Requesting git server start via OpenCode with polling")
        screenModelScope.launch {
            // Show starting state
            _uiState.update { 
                it.copy(
                    isStartingServer = true, 
                    serverStartProgress = "Sending start command...",
                    showServerNotRunningDialog = false
                ) 
            }
            
            // Use the new polling method
            gitRepository.requestStartGitServerAndWait(
                maxWaitMs = 15_000L,  // Wait up to 15 seconds
                pollIntervalMs = 500L
            ).onSuccess { serverStarted ->
                if (serverStarted) {
                    Napier.i("Git server started successfully!")
                    _uiState.update { 
                        it.copy(
                            isStartingServer = false,
                            serverStartProgress = null,
                            showServerStartedDialog = true,
                            error = null
                        ) 
                    }
                    // Automatically reload status now that server is up
                    delay(500) // Brief delay to ensure server is fully ready
                    loadStatus()
                } else {
                    onGitServerStartFailed("Server did not start")
                }
            }.onFailure { e ->
                Napier.e("Git server start failed", e)
                onGitServerStartFailed(e.message ?: "Unknown error")
            }
        }
    }

    private fun onGitServerStartFailed(reason: String) {
        _uiState.update { 
            it.copy(
                isStartingServer = false,
                serverStartProgress = null,
                warningMessage = "Could not start git server: $reason\n\nPlease run 'start-git-server.ps1' on host manually."
            ) 
        }
    }

    fun onGitServerStarted() {
        Napier.i("Git server start request accepted")
        hideServerNotRunningDialog()
        showServerStartedDialog()
    }
}

data class GitUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val warningMessage: String? = null,
    val operationResult: String? = null,
    val status: GitStatusResponse? = null,
    val branches: List<GitBranch> = emptyList(),
    val log: GitLog? = null,
    val remotes: List<GitRemote> = emptyList(),
    val showCommitDialog: Boolean = false,
    val commitMessage: String = "",
    val showServerNotRunningDialog: Boolean = false,
    val showAdbReverseHelp: Boolean = false,
    val showServerStartedDialog: Boolean = false,
    val isStartingServer: Boolean = false,
    val serverStartProgress: String? = null
) {
    val hasChanges: Boolean get() = status?.hasChanges == true
    val currentBranch: String get() = status?.branch ?: "unknown"
    val stagedCount: Int get() = status?.staged?.size ?: 0
    val unstagedCount: Int get() = status?.unstaged?.size ?: 0
    val untrackedCount: Int get() = status?.untracked?.size ?: 0
}

enum class GitTab(val title: String) {
    STATUS("Status"),
    BRANCHES("Branches"),
    LOG("Log"),
    REMOTES("Remotes")
}