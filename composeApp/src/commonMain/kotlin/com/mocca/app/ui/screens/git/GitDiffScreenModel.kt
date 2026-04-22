package com.mocca.app.ui.screens.git

import androidx.compose.runtime.Immutable
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
            gitRepository.getDiff(path, staged).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(isLoading = false, diff = resource.data, error = null)
                        is Resource.Error -> state.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }
}

@Immutable
data class GitDiffUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val diff: GitDiff? = null
)
