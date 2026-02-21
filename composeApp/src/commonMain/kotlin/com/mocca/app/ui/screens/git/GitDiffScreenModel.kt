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
                            val targetDiffs = fileDiffs.filter { it.path == path }
                            if (targetDiffs.isNotEmpty()) {
                                val diff = fileDiffsToGitDiff(targetDiffs)
                                state.copy(
                                    isLoading = false,
                                    diff = diff,
                                    error = null
                                )
                            } else {
                                // Try partial match (file might be in subdirectory)
                                val partialDiffs = fileDiffs.filter { it.path.endsWith(path) || path.endsWith(it.path) }
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
     * Converts OpenCode FileDiff (from /session/:id/diff) to GitDiff
     * for display by GitDiffScreen UI components.
     */
    private fun fileDiffsToGitDiff(fileDiffs: List<FileDiff>): GitDiff {
        val files = fileDiffs.map { fileDiff ->
            var additions = 0
            var deletions = 0
            val hunks = fileDiff.hunks.map { hunk ->
                var oldLine = hunk.oldStart
                var newLine = hunk.newStart
                val lines = hunk.lines.map { rawLine ->
                    when {
                        rawLine.startsWith("+") -> {
                            additions++
                            GitDiffLine(
                                type = DiffLineType.ADDITION,
                                content = rawLine.substring(1),
                                oldLineNumber = null,
                                newLineNumber = newLine++
                            )
                        }
                        rawLine.startsWith("-") -> {
                            deletions++
                            GitDiffLine(
                                type = DiffLineType.DELETION,
                                content = rawLine.substring(1),
                                oldLineNumber = oldLine++,
                                newLineNumber = null
                            )
                        }
                        else -> {
                            val content = if (rawLine.startsWith(" ")) rawLine.substring(1) else rawLine
                            GitDiffLine(
                                type = DiffLineType.CONTEXT,
                                content = content,
                                oldLineNumber = oldLine++,
                                newLineNumber = newLine++
                            )
                        }
                    }
                }
                GitDiffHunk(
                    oldStart = hunk.oldStart,
                    oldLines = hunk.oldLines,
                    newStart = hunk.newStart,
                    newLines = hunk.newLines,
                    header = "@@ -${hunk.oldStart},${hunk.oldLines} +${hunk.newStart},${hunk.newLines} @@",
                    lines = lines
                )
            }
            GitDiffFile(
                path = fileDiff.path,
                status = GitFileStatus.MODIFIED,
                additions = additions,
                deletions = deletions,
                hunks = hunks
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
