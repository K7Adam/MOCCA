package com.mocca.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Language Server Protocol status types.
 * Matches OpenCode server /lsp endpoint response.
 */

@Serializable
data class LspStatus(
    val language: String,
    val status: String, // "running", "stopped", "error"
    val name: String? = null
)
