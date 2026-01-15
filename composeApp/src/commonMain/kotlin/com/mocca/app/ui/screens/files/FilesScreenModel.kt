package com.mocca.app.ui.screens.files

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.FileRepository
import com.mocca.app.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FilesState(
    val currentPath: String = "",
    val files: List<FileInfo> = emptyList(),
    val selectedFile: FileInfo? = null,
    val fileContent: FileContent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pathHistory: List<String> = listOf("")
)

class FilesScreenModel(
    private val fileRepository: FileRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(FilesState())
    val state: StateFlow<FilesState> = _state.asStateFlow()
    
    init {
        loadFiles("")
    }
    
    fun loadFiles(path: String) {
        screenModelScope.launch {
            fileRepository.listFiles(path).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(
                            isLoading = true,
                            currentPath = path
                        )
                    }
                    is Resource.Success -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            files = resource.data.sortedWith(
                                compareByDescending<FileInfo> { it.isDirectory }
                                    .thenBy { it.name.lowercase() }
                            ),
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }
    
    fun navigateToDirectory(directory: FileInfo) {
        if (!directory.isDirectory) return
        
        val newPath = if (_state.value.currentPath.isEmpty()) {
            directory.name
        } else {
            "${_state.value.currentPath}/${directory.name}"
        }
        
        _state.value = _state.value.copy(
            pathHistory = _state.value.pathHistory + newPath
        )
        
        loadFiles(newPath)
    }
    
    fun navigateUp() {
        val history = _state.value.pathHistory
        if (history.size > 1) {
            val newHistory = history.dropLast(1)
            val newPath = newHistory.last()
            _state.value = _state.value.copy(pathHistory = newHistory)
            loadFiles(newPath)
        }
    }
    
    fun selectFile(file: FileInfo) {
        if (file.isDirectory) {
            navigateToDirectory(file)
        } else {
            _state.value = _state.value.copy(selectedFile = file)
            loadFileContent(file.path)
        }
    }
    
    private fun loadFileContent(path: String) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            when (val result = fileRepository.getFileContent(path)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        fileContent = result.data
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }
    
    fun closeFileViewer() {
        _state.value = _state.value.copy(
            selectedFile = null,
            fileContent = null
        )
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
