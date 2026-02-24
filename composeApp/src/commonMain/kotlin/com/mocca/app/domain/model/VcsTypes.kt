package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.Serializable

/**
 * Version Control System info from OpenCode server /vcs endpoint.
 * 
 * IMPORTANT: The OpenCode SDK only returns { branch: string }.
 * All other git status data (dirty, ahead, behind, file changes) must be
 * obtained via shell commands (git status, git rev-list).
 */

@Serializable
@Immutable
data class VcsInfo(
    val branch: String = ""
)
