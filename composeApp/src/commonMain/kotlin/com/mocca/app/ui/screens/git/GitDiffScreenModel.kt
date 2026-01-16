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
            // Fetch only the specific file's diff using the optimized repository method
            gitRepository.getFileDiff(path, cached = staged).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> {
                            val diff = resource.data
                            // Ensure the returned diff actually contains the file we asked for (validation)
                            if (diff.files.isNotEmpty()) {
                                state.copy(
                                    isLoading = false,
                                    diff = diff,
                                    error = null
                                )
                            } else {
                                state.copy(
                                    isLoading = false,
                                    error = "No changes found for $path"
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
