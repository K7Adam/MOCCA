package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.Serializable

/**
 * Session diff types.
 * Matches OpenCode SDK FileDiff: { file, before, after, additions, deletions }.
 */

@Serializable
@Immutable
data class FileDiff(
    val file: String,
    val before: String = "",
    val after: String = "",
    val additions: Int = 0,
    val deletions: Int = 0
)
