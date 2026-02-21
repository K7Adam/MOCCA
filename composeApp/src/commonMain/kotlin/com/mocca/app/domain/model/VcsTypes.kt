package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.Serializable

/**
 * Version Control System types.
 * Matches OpenCode server /vcs endpoint response.
 */

@Serializable
@Immutable
data class VcsInfo(
    val type: String = "git",
    val branch: String? = null,
    val dirty: Boolean = false,
    val ahead: Int = 0,
    val behind: Int = 0,
    val remote: String? = null,
    val changeCount: Int = 0
)
