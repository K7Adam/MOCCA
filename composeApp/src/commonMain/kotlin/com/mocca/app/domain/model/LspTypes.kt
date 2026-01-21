package com.mocca.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Language Server Protocol status.
 * Matches actual OpenCode server /lsp endpoint response.
 * 
 * Example response: [{"id":"kotlin-ls","name":"kotlin-ls","root":"","status":"connected"}]
 */
@Serializable
data class LspStatus(
    val id: String,
    val name: String,
    val root: String = "",
    val status: String // "connected", "disconnected", "error"
) {
    /** Check if server is connected/running */
    val isRunning: Boolean
        get() = status == "connected" || status == "running"
}
