package com.mocca.app.ui.screens.worktree

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.WorktreeRepository
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.WorktreeInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorktreeScreenModel(
    private val worktreeRepository: WorktreeRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(WorktreeUiState())
    val uiState: StateFlow<WorktreeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            worktreeRepository.getWorktrees().collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(
                            isLoading = true,
                            worktrees = resource.data ?: state.worktrees,
                            error = null
                        )
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            worktrees = resource.data,
                            error = null
                        )
                        is Resource.Error -> state.copy(
                            isLoading = false,
                            error = resource.message,
                            worktrees = resource.data ?: state.worktrees
                        )
                    }
                }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, createError = null) }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, createError = null) }
    }

    fun createWorktree(branch: String) {
        val trimmed = branch.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(createError = "Branch name cannot be empty") }
            return
        }
        screenModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            when (val result = worktreeRepository.createWorktree(trimmed)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isCreating = false, showCreateDialog = false) }
                    load()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isCreating = false, createError = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteWorktree(id: String) {
        screenModelScope.launch {
            _uiState.update { state ->
                state.copy(deletingIds = state.deletingIds + id)
            }
            when (val result = worktreeRepository.deleteWorktree(id)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            deletingIds = state.deletingIds - id,
                            worktrees = state.worktrees.filter { it.id != id }
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            deletingIds = state.deletingIds - id,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun resetWorktree(id: String) {
        screenModelScope.launch {
            _uiState.update { state -> state.copy(resettingIds = state.resettingIds + id) }
            when (val result = worktreeRepository.resetWorktree(id)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            resettingIds = state.resettingIds - id,
                            worktrees = state.worktrees.map { w ->
                                if (w.id == id) result.data else w
                            }
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            resettingIds = state.resettingIds - id,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

@Immutable
data class WorktreeUiState(
    val isLoading: Boolean = false,
    val worktrees: List<WorktreeInfo> = emptyList(),
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val createError: String? = null,
    val deletingIds: Set<String> = emptySet(),
    val resettingIds: Set<String> = emptySet()
)
