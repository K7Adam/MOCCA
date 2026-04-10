package com.mocca.app.ui.screens.mcp

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.domain.model.McpResource
import com.mocca.app.domain.model.McpResourceContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class McpResourceScreenState(
    val resources: ImmutableList<McpResource> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedResource: McpResource? = null,
    val selectedContent: McpResourceContent? = null,
    val isLoadingContent: Boolean = false,
    val contentError: String? = null
)

class McpResourceScreenModel(
    private val serverName: String,
    private val mcpRepository: McpRepository
) : ScreenModel {

    private val _state = MutableStateFlow(McpResourceScreenState())
    val state: StateFlow<McpResourceScreenState> = _state.asStateFlow()

    fun loadResources() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            mcpRepository.listResources(serverName)
                .onSuccess { list ->
                    _state.update {
                        it.copy(isLoading = false, resources = list.toImmutableList())
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun selectResource(resource: McpResource) {
        _state.update {
            it.copy(
                selectedResource = resource,
                selectedContent = null,
                contentError = null,
                isLoadingContent = true
            )
        }
        screenModelScope.launch {
            mcpRepository.readResource(serverName, resource.uri).fold(
                onSuccess = { content ->
                    _state.update { it.copy(isLoadingContent = false, selectedContent = content) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoadingContent = false, contentError = e.message) }
                }
            )
        }
    }

    fun clearSelection() {
        _state.update {
            it.copy(
                selectedResource = null,
                selectedContent = null,
                contentError = null,
                isLoadingContent = false
            )
        }
    }
}
