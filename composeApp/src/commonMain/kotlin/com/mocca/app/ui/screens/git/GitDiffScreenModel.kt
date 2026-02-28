package com.mocca.app.ui.screens.git

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.GitRepository
import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GitDiffScreenModel(
    private val gitRepository: GitRepository,
    private val sessionRepository: SessionRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(GitDiffUiState())
    val uiState: StateFlow<GitDiffUiState> = _uiState.asStateFlow()

    fun loadDiff(path: String, staged: Boolean) {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Get a session ID for the diff endpoint
            val sessionId = getActiveSessionId()
            if (sessionId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No active session available") }
                return@launch
            }

            gitRepository.getSessionDiffs(sessionId).collect { resource ->
                _uiState.update { state ->
                    when (resource) {
                        is Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> {
                            val fileDiffs = resource.data
                            // Filter to the specific file path
                            val targetDiffs = fileDiffs.filter { it.file == path }
                            if (targetDiffs.isNotEmpty()) {
                                val diff = fileDiffsToGitDiff(targetDiffs)
                                state.copy(
                                    isLoading = false,
                                    diff = diff,
                                    error = null
                                )
                            } else {
                                // Try partial match (file might be in subdirectory)
                                val partialDiffs = fileDiffs.filter { it.file.endsWith(path) || path.endsWith(it.file) }
                                if (partialDiffs.isNotEmpty()) {
                                    val diff = fileDiffsToGitDiff(partialDiffs)
                                    state.copy(isLoading = false, diff = diff, error = null)
                                } else {
                                    state.copy(isLoading = false, error = "No changes found for $path")
                                }
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

    private suspend fun getActiveSessionId(): String? {
        val resource = sessionRepository.getSessions().firstOrNull { it !is Resource.Loading }
        return when (resource) {
            is Resource.Success -> resource.data.firstOrNull()?.id
            is Resource.Loading -> resource.data?.firstOrNull()?.id
            is Resource.Error -> resource.data?.firstOrNull()?.id
            null -> null
        }
    }

    /**
     * Converts OpenCode FileDiff (from /session/:id/diff) to GitDiff.
     * 
     * The OpenCode SDK FileDiff has { file, before, after, additions, deletions }.
     * We generate diff hunks by comparing the before/after content line-by-line.
     */
    private fun fileDiffsToGitDiff(fileDiffs: List<FileDiff>): GitDiff {
        val files = fileDiffs.map { fileDiff ->
            val beforeLines = fileDiff.before.lines()
            val afterLines = fileDiff.after.lines()
            
            // Generate a simple diff by comparing lines
            val diffLines = mutableListOf<GitDiffLine>()
            val maxLines = maxOf(beforeLines.size, afterLines.size)
            var additions = 0
            var deletions = 0
            
            // Simple line-by-line comparison
            var i = 0
            while (i < maxLines) {
                val oldLine = beforeLines.getOrNull(i)
                val newLine = afterLines.getOrNull(i)
                
                when {
                    oldLine == null && newLine != null -> {
                        // Added line
                        additions++
                        diffLines.add(GitDiffLine(
                            type = DiffLineType.ADDITION,
                            content = newLine,
                            oldLineNumber = null,
                            newLineNumber = i + 1
                        ))
                    }
                    oldLine != null && newLine == null -> {
                        // Deleted line
                        deletions++
                        diffLines.add(GitDiffLine(
                            type = DiffLineType.DELETION,
                            content = oldLine,
                            oldLineNumber = i + 1,
                            newLineNumber = null
                        ))
                    }
                    oldLine != newLine -> {
                        // Modified line: show as deletion + addition
                        deletions++
                        additions++
                        diffLines.add(GitDiffLine(
                            type = DiffLineType.DELETION,
                            content = oldLine ?: "",
                            oldLineNumber = i + 1,
                            newLineNumber = null
                        ))
                        diffLines.add(GitDiffLine(
                            type = DiffLineType.ADDITION,
                            content = newLine ?: "",
                            oldLineNumber = null,
                            newLineNumber = i + 1
                        ))
                    }
                    else -> {
                        // Context line (unchanged)
                        diffLines.add(GitDiffLine(
                            type = DiffLineType.CONTEXT,
                            content = oldLine ?: "",
                            oldLineNumber = i + 1,
                            newLineNumber = i + 1
                        ))
                    }
                }
                i++
            }
            
            // Use the API-provided counts if available, fallback to computed
            val finalAdditions = if (fileDiff.additions > 0) fileDiff.additions else additions
            val finalDeletions = if (fileDiff.deletions > 0) fileDiff.deletions else deletions
            
            val hunk = GitDiffHunk(
                oldStart = 1,
                oldLines = beforeLines.size,
                newStart = 1,
                newLines = afterLines.size,
                header = "@@ -1,${beforeLines.size} +1,${afterLines.size} @@",
                lines = diffLines
            )
            
            GitDiffFile(
                path = fileDiff.file,
                status = when {
                    fileDiff.before.isEmpty() -> GitFileStatus.ADDED
                    fileDiff.after.isEmpty() -> GitFileStatus.DELETED
                    else -> GitFileStatus.MODIFIED
                },
                additions = finalAdditions,
                deletions = finalDeletions,
                hunks = listOf(hunk)
            )
        }
        return GitDiff(
            files = files,
            additions = files.sumOf { it.additions },
            deletions = files.sumOf { it.deletions }
        )
    }
}

@Immutable
data class GitDiffUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val diff: GitDiff? = null
)
