package com.mocca.app.ui.screens.git

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.bridge.opencode.BridgeResponseException
import com.mocca.app.bridge.protocol.BridgeConfirmation
import com.mocca.app.bridge.protocol.BridgeConfirmationDetails
import com.mocca.app.data.repository.GitRepository
import com.mocca.app.domain.model.GitBranch
import com.mocca.app.domain.model.GitLog
import com.mocca.app.domain.model.GitOperationResult
import com.mocca.app.domain.model.GitRemote
import com.mocca.app.domain.model.GitStash
import com.mocca.app.domain.model.GitStatusResponse
import com.mocca.app.domain.model.Resource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class GitScreenModel(
    private val gitRepository: GitRepository,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : ScreenModel {

    private val _uiState = MutableStateFlow(GitUiState())
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(GitTab.STATUS)
    val selectedTab: StateFlow<GitTab> = _selectedTab.asStateFlow()

    private var pendingConfirmedAction: ((BridgeConfirmation) -> Unit)? = null

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
            GitTab.STASHES -> loadStashes()
        }
    }

    fun loadStatus() {
        screenModelScope.launch {
            gitRepository.getStatus().collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true, status = resource.data ?: state.status)
                        is Resource.Success -> state.copy(isLoading = false, isNotGitRepo = false, status = resource.data, error = null)
                        is Resource.Error -> state.copy(
                            isLoading = false,
                            isNotGitRepo = resource.message.contains("not a git repository", ignoreCase = true),
                            status = resource.data ?: state.status,
                            error = if (resource.message.contains("not a git repository", ignoreCase = true)) null else resource.message
                        )
                    }
                }
            }
        }
    }

    fun loadBranches() = collectResource(
        flow = { gitRepository.getBranches() },
        onSuccess = { state, data -> state.copy(isLoading = false, branches = data.toImmutableList(), error = null) }
    )

    fun loadLog(branch: String? = null, skip: Int = 0) = collectResource(
        flow = { gitRepository.getLog(branch = branch, skip = skip) },
        onSuccess = { state, data -> state.copy(isLoading = false, log = data, error = null) }
    )

    fun loadRemotes() = collectResource(
        flow = { gitRepository.getRemotes() },
        onSuccess = { state, data -> state.copy(isLoading = false, remotes = data.toImmutableList(), error = null) }
    )

    fun loadTags() = collectResource(
        flow = { gitRepository.getTags() },
        onSuccess = { state, data -> state.copy(isLoading = false, tags = data.toImmutableList(), error = null) }
    )

    fun loadStashes() {
        screenModelScope.launch {
            gitRepository.getStashes().collect { resource ->
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

    fun stageFile(path: String) = runGitOperation { gitRepository.stage(listOf(path)) }

    fun stageAll() {
        val unstaged = _uiState.value.status?.unstaged?.map { it.path }.orEmpty()
        val untracked = _uiState.value.status?.untracked.orEmpty()
        val allFiles = unstaged + untracked
        if (allFiles.isNotEmpty()) runGitOperation { gitRepository.stage(allFiles) }
    }

    fun unstageFile(path: String) = runGitOperation { gitRepository.unstage(listOf(path)) }

    fun unstageAll() {
        val staged = _uiState.value.status?.staged?.map { it.path }.orEmpty()
        if (staged.isNotEmpty()) runGitOperation { gitRepository.unstage(staged) }
    }

    fun discardFile(path: String, confirmation: BridgeConfirmation? = null) {
        runGitOperation(
            retry = { confirm -> discardFile(path, confirm) }
        ) {
            gitRepository.discard(listOf(path), confirmation)
        }
    }

    fun commit(message: String) {
        if (message.isBlank()) {
            _uiState.update { it.copy(warningMessage = "Commit message cannot be empty") }
            return
        }
        runGitOperation(onSuccess = {
            _uiState.update { state -> state.copy(showCommitDialog = false, commitMessage = "") }
        }) {
            gitRepository.commit(message)
        }
    }

    fun push(force: Boolean = false, confirmation: BridgeConfirmation? = null) {
        runGitOperation(
            retry = { confirm -> push(force, confirm) }
        ) {
            gitRepository.push(force = force, confirmation = confirmation)
        }
    }

    fun pull(rebase: Boolean = false) = runGitOperation { gitRepository.pull(rebase = rebase) }

    fun fetch() = runGitOperation(onSuccess = { loadBranches() }) { gitRepository.fetch() }

    fun checkout(ref: String, create: Boolean = false, confirmation: BridgeConfirmation? = null) {
        runGitOperation(
            retry = { confirm -> checkout(ref, create, confirmation = confirm) },
            onSuccess = { loadBranches() }
        ) {
            gitRepository.checkout(ref = ref, create = create, confirmation = confirmation)
        }
    }

    fun createStash(message: String?) = runGitOperation(onSuccess = {
        _uiState.update { it.copy(showStashDialog = false, stashMessage = "") }
        loadStashes()
    }) {
        gitRepository.createStash(message)
    }

    fun popStash(index: Int) = runGitOperation(onSuccess = { loadStashes() }) { gitRepository.popStash(index) }

    fun applyStash(index: Int) = runGitOperation { gitRepository.applyStash(index) }

    fun dropStash(index: Int, confirmation: BridgeConfirmation? = null) {
        runGitOperation(
            retry = { confirm -> dropStash(index, confirm) },
            onSuccess = { loadStashes() }
        ) {
            gitRepository.dropStash(index, confirmation)
        }
    }

    fun merge(branch: String) = runGitOperation { gitRepository.merge(branch) }

    fun rebase(branch: String) = runGitOperation { gitRepository.rebase(branch) }

    fun addRemote(name: String, url: String) = runGitOperation(onSuccess = { loadRemotes() }) { gitRepository.addRemote(name, url) }

    fun removeRemote(name: String, confirmation: BridgeConfirmation? = null) {
        runGitOperation(
            retry = { confirm -> removeRemote(name, confirm) },
            onSuccess = { loadRemotes() }
        ) {
            gitRepository.removeRemote(name, confirmation)
        }
    }

    fun createTag(name: String, message: String?) = runGitOperation(onSuccess = { loadTags() }) { gitRepository.createTag(name, message) }

    fun deleteTag(name: String, confirmation: BridgeConfirmation? = null) {
        runGitOperation(
            retry = { confirm -> deleteTag(name, confirm) },
            onSuccess = { loadTags() }
        ) {
            gitRepository.deleteTag(name, confirmation)
        }
    }

    fun confirmPendingOperation() {
        val pending = _uiState.value.pendingConfirmation ?: return
        val action = pendingConfirmedAction ?: return
        _uiState.update { it.copy(pendingConfirmation = null) }
        pendingConfirmedAction = null
        action(BridgeConfirmation(operationId = pending.operationId))
    }

    fun cancelPendingOperation() {
        pendingConfirmedAction = null
        _uiState.update { it.copy(pendingConfirmation = null) }
    }

    private fun <T> collectResource(
        flow: () -> kotlinx.coroutines.flow.Flow<Resource<T>>,
        onSuccess: (GitUiState, T) -> GitUiState
    ) {
        screenModelScope.launch {
            flow().collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> onSuccess(state, resource.data)
                        is Resource.Error -> state.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }

    private fun runGitOperation(
        retry: ((BridgeConfirmation) -> Unit)? = null,
        onSuccess: () -> Unit = {},
        operation: suspend () -> Result<GitOperationResult>
    ) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, warningMessage = null) }
            operation().onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        operationResult = result.message?.ifBlank { "Operation successful" } ?: "Operation successful"
                    )
                }
                onSuccess()
                loadStatus()
            }.onFailure { error ->
                if (error is BridgeResponseException && error.code == "confirmation_required" && retry != null) {
                    val details = error.decodeConfirmationDetails()
                    if (details != null) {
                        pendingConfirmedAction = retry
                        _uiState.update { it.copy(isLoading = false, pendingConfirmation = details) }
                        return@launch
                    }
                }
                _uiState.update { it.copy(isLoading = false, warningMessage = error.message ?: "Operation failed") }
            }
        }
    }

    private fun BridgeResponseException.decodeConfirmationDetails(): BridgeConfirmationDetails? {
        return try {
            details?.let { json.decodeFromJsonElement<BridgeConfirmationDetails>(it) }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
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
    val showCreateTagDialog: Boolean = false,
    val pendingConfirmation: BridgeConfirmationDetails? = null
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
