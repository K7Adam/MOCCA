package com.mocca.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Version Control System types.
 * Matches OpenCode server /vcs endpoint response.
 */

@Serializable
data class VcsInfo(
    val type: String = "git",
    val branch: String? = null,
    val dirty: Boolean = false,
    val ahead: Int = 0,
    val behind: Int = 0,
    val remote: String? = null
)
