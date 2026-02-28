package com.mocca.app.ui.screens.git

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.GitRepository
import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.data.repository.StateCoordinator
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Screen model for Git operations screen.
 * Uses StateCoordinator to observe the active session ID for shell-based git operations.
 * All git write/read operations are routed through OpenCode's built-in endpoints.
 */
class GitScreenModel(
    private val gitRepository: GitRepository,
    private val stateCoordinator: StateCoordinator
) : ScreenModel {
    
    // UI State
    private val _uiState = MutableStateFlow(GitUiState())
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()
    
    // Currently selected tab
    private val _selectedTab = MutableStateFlow(GitTab.STATUS)
    val selectedTab: StateFlow<GitTab> = _selectedTab.asStateFlow()
    
    // Cached session ID for git operations
    private var _currentSessionId: String? = null
    
    init {
        observeActiveSession()
    }
    
    /**
     * Reactively observe the globally active Session ID.
     * When it becomes available or changes, automatically load Git status.
     */
    private fun observeActiveSession() {
        screenModelScope.launch {
            stateCoordinator.activeSessionId
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { sessionId ->
                    Napier.i("[GitScreenModel] Active session ID received ($sessionId), loading Git status...")
                    _currentSessionId = sessionId
                    // If we already had an active tab, refresh its data with the new session
                    when (_selectedTab.value) {
                        GitTab.STATUS -> loadStatus()
                        GitTab.LOG -> loadLog()
                        GitTab.BRANCHES -> loadBranches()
                        GitTab.STASHES -> loadStashes() // We load this preemptively below anyway, but it ensures UI stays in sync
                        else -> {
                            loadStatus()
                        }
                    }
                    loadStashes() // Preemptively load stashes for the counter icon
                }
        }
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
            GitTab.STASHES -> loadStashes()
        }
    }
    
    fun loadStatus() {
        screenModelScope.launch {
            gitRepository.getStatus(_currentSessionId).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(
                            isLoading = true,
                            status = resource.data ?: state.status
                        )
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            isNotGitRepo = false,
                            status = resource.data,
                            error = null
                        )
                        is Resource.Error -> state.copy(
                            isLoading = false,
                            isNotGitRepo = resource.message == "Not a git repository",
                            status = resource.data ?: state.status,
                            error = if (resource.message == "Not a git repository") null else resource.message
                        )
                    }
                }
            }
        }
    }
    
    fun loadBranches() {
        screenModelScope.launch {
            gitRepository.getBranches(_currentSessionId).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(isLoading = false, branches = resource.data.toImmutableList(), error = null)
                        is Resource.Error -> state.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }
    
    fun loadLog(branch: String? = null, skip: Int = 0) {
        screenModelScope.launch {
            gitRepository.getLog(_currentSessionId, branch, skip = skip).collect { resource ->
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
            gitRepository.getRemotes(_currentSessionId).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(isLoading = false, remotes = resource.data.toImmutableList(), error = null)
                        is Resource.Error -> state.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }

    fun loadTags() {
        screenModelScope.launch {
            gitRepository.getTags(_currentSessionId).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(isLoading = false, tags = resource.data.toImmutableList(), error = null)
                        is Resource.Error -> state.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }

    fun loadStashes() {
        screenModelScope.launch {
            gitRepository.getStashes(_currentSessionId).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state
                        is Resource.Success -> state.copy(stashes = resource.data.toImmutableList())
                        is Resource.Error -> state
                    }
                }
            }
        }
    }
    
    /**
     * Helper to run git write operations that require a sessionId.
     * Shows error if no session is available.
     */
    private inline fun withSessionId(crossinline action: suspend (String) -> Unit) {
        val sessionId = _currentSessionId
        if (sessionId == null) {
            _uiState.update { it.copy(isLoading = false, error = "No active session available for Git operations") }
            return
        }
        screenModelScope.launch { action(sessionId) }
    }
    
    fun stageFile(path: String) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.stage(sessionId, listOf(path))
                .onSuccess { loadStatus() }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun stageAll() {
        val unstaged = _uiState.value.status?.unstaged?.map { it.path } ?: return
        val untracked = _uiState.value.status?.untracked ?: emptyList()
        val allFiles = unstaged + untracked
        if (allFiles.isEmpty()) return
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.stage(sessionId, allFiles)
                .onSuccess { loadStatus() }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun unstageFile(path: String) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.unstage(sessionId, listOf(path))
                .onSuccess { loadStatus() }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun unstageAll() {
        val staged = _uiState.value.status?.staged?.map { it.path } ?: return
        if (staged.isEmpty()) return
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.unstage(sessionId, staged)
                .onSuccess { loadStatus() }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun discardFile(path: String) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.discard(sessionId, listOf(path))
                .onSuccess { loadStatus() }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
    
    fun commit(message: String) {
        if (message.isBlank()) {
            _uiState.update { it.copy(warningMessage = "Commit message cannot be empty") }
            return
        }
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.commit(sessionId, message).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, showCommitDialog = false, commitMessage = "", operationResult = result.message ?: "Commit successful") }
                loadStatus()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") }
            }
        }
    }
    
    fun push(force: Boolean = false) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.push(sessionId, force = force).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message ?: "Push successful") }
                loadStatus()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") }
            }
        }
    }
    
    fun pull(rebase: Boolean = false) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.pull(sessionId, rebase = rebase).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message ?: "Pull successful") }
                loadStatus()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") }
            }
        }
    }
    
    fun fetch() {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.fetch(sessionId).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message ?: "Fetch successful") }
                loadStatus(); loadBranches()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") }
            }
        }
    }
    
    fun checkout(ref: String, create: Boolean = false) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.checkout(sessionId, ref, create).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message ?: "Checkout successful") }
                loadStatus(); loadBranches()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") }
            }
        }
    }

    fun createStash(message: String?) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.createStash(sessionId, message).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message, showStashDialog = false, stashMessage = "") }
                loadStatus(); loadStashes()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
        }
    }

    fun popStash(index: Int) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.popStash(sessionId, index).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStatus(); loadStashes()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
        }
    }

    fun applyStash(index: Int) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.applyStash(sessionId, index).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStatus()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
        }
    }

    fun dropStash(index: Int) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.dropStash(sessionId, index).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStashes()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
        }
    }

    fun merge(branch: String) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.merge(sessionId, branch).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStatus()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
        }
    }

    fun rebase(branch: String) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.rebase(sessionId, branch).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadStatus()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
        }
    }

    fun addRemote(name: String, url: String) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.addRemote(sessionId, name, url).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadRemotes()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
        }
    }

    fun removeRemote(name: String) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.removeRemote(sessionId, name).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadRemotes()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
        }
    }

    fun createTag(name: String, message: String?) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.createTag(sessionId, name, message).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadTags()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
        }
    }

    fun deleteTag(name: String) {
        withSessionId { sessionId ->
            _uiState.update { it.copy(isLoading = true) }
            gitRepository.deleteTag(sessionId, name).onSuccess { result ->
                _uiState.update { it.copy(isLoading = false, operationResult = result.message) }
                loadTags()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message) }
            }
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
}

@Immutable

data class GitUiState(
    val isLoading: Boolean = false,
    val isNotGitRepo: Boolean = false,
    val error: String? = null,
    val warningMessage: String? = null,
    val operationResult: String? = null,
    val status: GitStatusResponse? = null,
    val branches: ImmutableList<GitBranch> = persistentListOf(),
    val log: GitLog? = null,
    val remotes: ImmutableList<GitRemote> = persistentListOf(),
    val tags: ImmutableList<String> = persistentListOf(),
    val stashes: ImmutableList<GitStash> = persistentListOf(),
    val showCommitDialog: Boolean = false,
    val commitMessage: String = "",
    val showStashDialog: Boolean = false,
    val stashMessage: String = "",
    val showAddRemoteDialog: Boolean = false,
    val showCreateTagDialog: Boolean = false
) {
    val hasChanges: Boolean get() = status?.hasChanges == true
    val currentBranch: String get() = status?.branch ?: "unknown"
    val stagedCount: Int get() = status?.staged?.size ?: 0
    val unstagedCount: Int get() = status?.unstaged?.size ?: 0
    val untrackedCount: Int get() = status?.untracked?.size ?: 0
}

enum class GitTab(val title: String) {
    STATUS("Status"), BRANCHES("Branches"), LOG("Log"), REMOTES("Remotes"), TAGS("Tags"), STASHES("Stashes")
}
