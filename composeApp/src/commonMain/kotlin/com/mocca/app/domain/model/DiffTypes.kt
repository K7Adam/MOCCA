package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Session diff types.
 * Matches OpenCode server /session/{id}/diff endpoint response.
 */

@Serializable
@Immutable
data class FileDiff(
    val path: String,
    val original: String? = null,
    val modified: String? = null,
    val hunks: List<DiffHunk> = emptyList()
)

@Serializable
@Immutable
data class DiffHunk(
    @SerialName("old_start")
    val oldStart: Int,
    @SerialName("old_lines")
    val oldLines: Int,
    @SerialName("new_start")
    val newStart: Int,
    @SerialName("new_lines")
    val newLines: Int,
    val lines: List<String> = emptyList()
)
