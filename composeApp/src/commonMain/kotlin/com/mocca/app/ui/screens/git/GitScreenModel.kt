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
        loadStashes()
    }
    
    fun selectTab(tab: GitTab) {
        _selectedTab.value = tab
        when (tab) {
            GitTab.STATUS -> {
                loadStatus()
                loadStashes()
            }
            GitTab.BRANCHES -> loadBranches()
            GitTab.LOG -> loadLog()
            GitTab.REMOTES -> loadRemotes()
            GitTab.TAGS -> loadTags()
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
                            val cause = resource.cause
                            val isServerNotRunning = cause is GitServerNotRunningException ||
                                cause is NetworkError.GitServerUnavailable ||
                                cause?.cause is GitServerNotRunningException ||
                                resource.message.contains("Git server is not running", ignoreCase = true)
                            
                            if (isServerNotRunning) {
                                val exception = (cause as? GitServerNotRunningException) 
                                    ?: (cause?.cause as? GitServerNotRunningException)
                                showServerNotRunningDialog(exception?.isConnectionRefused ?: false, exception?.url)
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
                        is Resource.Success -> state.copy(isLoading = false, branches = resource.data, error = null)
                        is Resource.Error -> state.copy(isLoading = false, error = resource.message)
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
                        is Resource.Success -> state.copy(isLoading = false, log = resource.data, error = null)
                        is Resource.Error -> state.copy(isLoading = false, error = resource.message)
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
                        is Resource.Success -> state.copy(isLoading = false, remotes = resource.data, error = null)
                        is Resource.Error -> state.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }

    fun loadTags() {
        screenModelScope.launch {
            gitRepository.getTags().collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(isLoading = false, tags = resource.data, error = null)
                        is Resource.Error -> state.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }

    fun loadStashes() {
        screenModelScope.launch {
            gitRepository.getStashes().collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state
                        is Resource.Success -> state.copy(stashes = resource.data)
                        is Resource.Error -> state
                    }
                }
            }
        }
    }
    
    fun stageFile(path: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.stage(listOf(path)).onSuccess { loadStatus() }.onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun stageAll() {
        val unstaged = _uiState.value.status?.unstaged?.map { it.path } ?: return
        val untracked = _uiState.value.status?.untracked ?: emptyList()
        val allFiles = unstaged + untracked
        if (allFiles.isEmpty()) return
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.stage(allFiles).onSuccess { loadStatus() }.onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun unstageFile(path: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.unstage(listOf(path)).onSuccess { loadStatus() }.onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun unstageAll() {
        val staged = _uiState.value.status?.staged?.map { it.path } ?: return
        if (staged.isEmpty()) return
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.unstage(staged).onSuccess { loadStatus() }.onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun discardFile(path: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.discard(listOf(path)).onSuccess { loadStatus() }.onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun commit(message: String) {
        if (message.isBlank()) { _uiState.update { it.copy(warningMessage = "Commit message cannot be empty") }; return }
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.commit(message).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, showCommitDialog = false, commitMessage = "", operationResult = result.message ?: "Commit successful") }
                loadStatus()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") } }
        }
    }
    
    fun push(force: Boolean = false) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.push(force = force).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message ?: "Push successful") }
                loadStatus()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") } }
        }
    }
    
    fun pull(rebase: Boolean = false) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.pull(rebase = rebase).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message ?: "Pull successful") }
                loadStatus()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") } }
        }
    }
    
    fun fetch() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.fetch().onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message ?: "Fetch successful") }
                loadStatus(); loadBranches()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") } }
        }
    }
    
    fun checkout(ref: String, create: Boolean = false) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.checkout(ref, create).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message ?: "Checkout successful") }
                loadStatus(); loadBranches()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") } }
        }
    }

    fun createStash(message: String?) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.createStash(message).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message, showStashDialog = false, stashMessage = "") }
                loadStatus(); loadStashes()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }

    fun popStash(index: Int) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.popStash(index).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStatus(); loadStashes()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }

    fun applyStash(index: Int) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.applyStash(index).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStatus()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }

    fun dropStash(index: Int) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.dropStash(index).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStashes()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }

    fun merge(branch: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.merge(branch).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStatus()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }

    fun rebase(branch: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.rebase(branch).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStatus()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }

    fun addRemote(name: String, url: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.addRemote(name, url).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadRemotes()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }

    fun removeRemote(name: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.removeRemote(name).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadRemotes()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }

    fun createTag(name: String, message: String?) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.createTag(name, message).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadTags()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }

    fun deleteTag(name: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.deleteTag(name).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadTags()
            }.onFailure { error -> _uiState.update { it.copy(isLoading = false, warningMessage = error.message) } }
        }
    }
    
    fun showCommitDialog() { _uiState.update { it.copy(showCommitDialog = true) } }
    fun hideCommitDialog() { _uiState.update { it.copy(showCommitDialog = false, commitMessage = "") } }
    fun updateCommitMessage(message: String) { _uiState.update { it.copy(commitMessage = message) } }
    fun showStashDialog() { _uiState.update { it.copy(showStashDialog = true) } }
    fun hideStashDialog() { _uiState.update { it.copy(showStashDialog = false, stashMessage = "") } }
    fun updateStashMessage(message: String) { _uiState.update { it.copy(stashMessage = message) } }
    fun showAddRemoteDialog() { _uiState.update { it.copy(showAddRemoteDialog = true) } }
    fun hideAddRemoteDialog() { _uiState.update { it.copy(showAddRemoteDialog = false) } }
    fun showCreateTagDialog() { _uiState.update { it.copy(showCreateTagDialog = true) } }
    fun hideCreateTagDialog() { _uiState.update { it.copy(showCreateTagDialog = false) } }
    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearOperationResult() { _uiState.update { it.copy(operationResult = null) } }
    fun clearWarning() { _uiState.update { it.copy(warningMessage = null) } }

    fun showServerNotRunningDialog(isConnectionRefused: Boolean = false, url: String? = null) {
        _uiState.update { it.copy(showServerNotRunningDialog = true, showAdbReverseHelp = isConnectionRefused && (url?.contains("127.0.0.1") == true || url?.contains("localhost") == true)) }
    }

    fun hideServerNotRunningDialog() { _uiState.update { it.copy(showServerNotRunningDialog = false, showAdbReverseHelp = false) } }
    fun showServerStartedDialog() { _uiState.update { it.copy(showServerStartedDialog = true) } }
    fun hideServerStartedDialog() { _uiState.update { it.copy(showServerStartedDialog = false) } }

    fun requestStartGitServer() {
        screenModelScope.launch {
            val currentAttempt = _uiState.value.serverStartAttempt + 1
            val maxAttempts = _uiState.value.maxServerStartAttempts
            
            _uiState.update { it.copy(
                isStartingServer = true, 
                serverStartProgress = "Sending start command (attempt $currentAttempt/$maxAttempts)...", 
                showServerNotRunningDialog = true, // Show dialog with progress
                serverStartAttempt = currentAttempt
            ) }
            
            gitRepository.requestStartGitServerAndWait(maxWaitMs = 15_000L, pollIntervalMs = 500L).onSuccess { serverStarted ->
                if (serverStarted) {
                    _uiState.update { it.copy(
                        isStartingServer = false, 
                        serverStartProgress = null, 
                        showServerNotRunningDialog = false,
                        showServerStartedDialog = true, 
                        error = null,
                        serverStartAttempt = 0
                    )}
                    delay(500)
                    loadStatus()
                } else { 
                    onGitServerStartFailed("Server did not start", currentAttempt, maxAttempts) 
                }
            }.onFailure { e -> 
                onGitServerStartFailed(e.message ?: "Unknown error", currentAttempt, maxAttempts) 
            }
        }
    }

    private fun onGitServerStartFailed(reason: String, attempt: Int, maxAttempts: Int) {
        val canRetry = attempt < maxAttempts
        
        _uiState.update { it.copy(
            isStartingServer = canRetry, // Keep loading if we're going to retry
            serverStartProgress = if (canRetry) "Retrying..." else null,
            showServerNotRunningDialog = !canRetry, // Show dialog only if exhausted all retries
            warningMessage = if (!canRetry) "Could not start Git services after $maxAttempts attempts.\n\nError: $reason" else null,
            serverStartAttempt = if (canRetry) attempt else 0
        )}
        
        // Auto-retry if we haven't exhausted attempts
        if (canRetry) {
            screenModelScope.launch {
                delay(2000) // Wait 2 seconds before retry
                requestStartGitServer()
            }
        }
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
    val tags: List<String> = emptyList(),
    val stashes: List<GitStash> = emptyList(),
    val showCommitDialog: Boolean = false,
    val commitMessage: String = "",
    val showStashDialog: Boolean = false,
    val stashMessage: String = "",
    val showAddRemoteDialog: Boolean = false,
    val showCreateTagDialog: Boolean = false,
    val showServerNotRunningDialog: Boolean = false,
    val showAdbReverseHelp: Boolean = false,
    val showServerStartedDialog: Boolean = false,
    val isStartingServer: Boolean = false,
    val serverStartProgress: String? = null,
    val serverStartAttempt: Int = 0,
    val maxServerStartAttempts: Int = 3
) {
    val hasChanges: Boolean get() = status?.hasChanges == true
    val currentBranch: String get() = status?.branch ?: "unknown"
    val stagedCount: Int get() = status?.staged?.size ?: 0
    val unstagedCount: Int get() = status?.unstaged?.size ?: 0
    val untrackedCount: Int get() = status?.untracked?.size ?: 0
}

enum class GitTab(val title: String) {
    STATUS("Status"), BRANCHES("Branches"), LOG("Log"), REMOTES("Remotes"), TAGS("Tags")
}
