package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.Serializable

/**
 * Session diff types.
 * Matches OpenCode SDK FileDiff: { file, before, after, additions, deletions }.
 * before/after are nullable to support patch-only diffs where full content is not provided.
 */

@Serializable
@Immutable
data class FileDiff(
    val file: String,
    val before: String? = null,
    val after: String? = null,
    val additions: Int = 0,
    val deletions: Int = 0
)
