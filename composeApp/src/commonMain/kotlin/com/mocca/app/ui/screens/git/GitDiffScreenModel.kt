package com.mocca.app.ui.screens.git

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.GitRepository
import com.mocca.app.domain.model.GitDiff
import com.mocca.app.domain.model.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GitDiffScreenModel(
    private val gitRepository: GitRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(GitDiffUiState())
    val uiState: StateFlow<GitDiffUiState> = _uiState.asStateFlow()

    fun loadDiff(path: String, staged: Boolean) {
        screenModelScope.launch {
            // We request the diff for the specific file
            // Since GitRepository.getDiff usually returns full diff, 
            // we might need to filter it or update repository to support path filtering.
            // For now, we fetch full diff and filter locally.
            
            gitRepository.getDiff(cached = staged).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> {
                            val allFiles = resource.data?.files ?: emptyList()
                            val fileDiff = allFiles.find { it.path == path }
                            
                            if (fileDiff != null) {
                                state.copy(
                                    isLoading = false,
                                    diff = GitDiff(files = listOf(fileDiff)), // Wrap in GitDiff
                                    error = null
                                )
                            } else {
                                state.copy(
                                    isLoading = false,
                                    error = "File not found in diff"
                                )
                            }
                        }
                        is Resource.Error -> state.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }
}

data class GitDiffUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val diff: GitDiff? = null
)
