package com.mocca.app.ui.screens.files

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.FileRepository
import com.mocca.app.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable

data class FilesState(
    val currentPath: String = "",
    val files: ImmutableList<FileInfo> = persistentListOf(),
    val selectedFile: FileInfo? = null,
    val fileContent: FileContent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pathHistory: ImmutableList<String> = persistentListOf(""),
    val isEditing: Boolean = false,
    val editedContent: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val showDiscardDialog: Boolean = false,
    val pendingCloseAfterDiscard: Boolean = false,
    val showExternalChangeDialog: Boolean = false
) {
    val hasUnsavedChanges: Boolean
        get() = editedContent != null && fileContent != null && editedContent != fileContent.content

    val detectedLanguage: String
        get() = selectedFile?.name?.let { detectLanguage(it) } ?: "plaintext"
}

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
                            ).toImmutableList(),
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
            pathHistory = (_state.value.pathHistory + newPath).toImmutableList()
        )
        
        loadFiles(newPath)
    }
    
    fun navigateUp() {
        val history = _state.value.pathHistory
        if (history.size > 1) {
            val newHistory = history.dropLast(1)
            val newPath = newHistory.last()
            _state.value = _state.value.copy(pathHistory = newHistory.toImmutableList())
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
            _state.value = _state.value.copy(
                isLoading = true,
                isEditing = false,
                editedContent = null,
                saveError = null,
                showDiscardDialog = false
            )
            
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
            fileContent = null,
            isEditing = false,
            editedContent = null,
            isSaving = false,
            saveError = null,
            showDiscardDialog = false,
            pendingCloseAfterDiscard = false,
            showExternalChangeDialog = false
        )
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun attemptCloseViewer() {
        val current = _state.value
        if (current.hasUnsavedChanges) {
            _state.value = current.copy(showDiscardDialog = true, pendingCloseAfterDiscard = true)
            return
        }
        closeFileViewer()
    }

    fun toggleEdit() {
        val current = _state.value
        if (current.isEditing) {
            if (current.hasUnsavedChanges) {
                _state.value = current.copy(showDiscardDialog = true)
                return
            }
            _state.value = current.copy(
                isEditing = false,
                editedContent = null,
                saveError = null
            )
        } else {
            _state.value = current.copy(
                isEditing = true,
                editedContent = current.fileContent?.content ?: "",
                saveError = null
            )
        }
    }

    fun updateEditedContent(content: String) {
        _state.value = _state.value.copy(editedContent = content, saveError = null)
    }

    fun saveFile() {
        val current = _state.value
        val file = current.selectedFile ?: return
        val content = current.editedContent ?: return

        screenModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, saveError = null)

            when (val result = fileRepository.saveFile(file.path, content)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        isEditing = false,
                        editedContent = null,
                        fileContent = current.fileContent?.copy(content = content)
                    )
                }
                is Resource.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        saveError = result.message
                    )
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun confirmDiscard() {
        val current = _state.value
        if (current.pendingCloseAfterDiscard) {
            closeFileViewer()
        } else {
            _state.value = current.copy(
                isEditing = false,
                editedContent = null,
                saveError = null,
                showDiscardDialog = false
            )
        }
    }

    fun cancelDiscard() {
        _state.value = _state.value.copy(
            showDiscardDialog = false,
            pendingCloseAfterDiscard = false
        )
    }

    fun clearSaveError() {
        _state.value = _state.value.copy(saveError = null)
    }

    fun refreshFileContent() {
        val current = _state.value
        val file = current.selectedFile ?: return
        val existingContent = current.fileContent?.content ?: return

        screenModelScope.launch {
            when (val result = fileRepository.getFileContent(file.path)) {
                is Resource.Success -> {
                    val newContent = result.data.content
                    val state = _state.value
                    if (state.isEditing && newContent != existingContent) {
                        _state.value = state.copy(showExternalChangeDialog = true)
                    } else if (!state.isEditing) {
                        _state.value = state.copy(fileContent = result.data)
                    }
                }
                else -> { /* silent refresh failure */ }
            }
        }
    }

    fun confirmReloadExternalChange() {
        val current = _state.value
        val file = current.selectedFile ?: return
        screenModelScope.launch {
            when (val result = fileRepository.getFileContent(file.path)) {
                is Resource.Success -> {
                    _state.value = _state.value.copy(
                        fileContent = result.data,
                        isEditing = false,
                        editedContent = null,
                        showExternalChangeDialog = false
                    )
                }
                else -> {
                    _state.value = _state.value.copy(showExternalChangeDialog = false)
                }
            }
        }
    }

    fun cancelReloadExternalChange() {
        _state.value = _state.value.copy(showExternalChangeDialog = false)
    }
}

private fun detectLanguage(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "py", "pyw" -> "python"
        "js", "mjs" -> "javascript"
        "ts" -> "typescript"
        "tsx", "jsx" -> "javascript"
        "json" -> "json"
        "yaml", "yml" -> "yaml"
        "xml", "svg", "xhtml" -> "xml"
        "html", "htm" -> "html"
        "css" -> "css"
        "md", "markdown" -> "markdown"
        "sh", "bash" -> "bash"
        "go" -> "go"
        "rs" -> "rust"
        "sql" -> "sql"
        "c", "h" -> "c"
        "cpp", "cc", "cxx", "hpp" -> "cpp"
        "rb" -> "ruby"
        "php" -> "php"
        "swift" -> "swift"
        "toml", "ini", "cfg", "conf" -> "ini"
        "gradle" -> "groovy"
        "properties" -> "properties"
        "txt", "log" -> "plaintext"
        else -> "plaintext"
    }
}
