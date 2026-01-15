package com.mocca.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Slash command types.
 * Matches OpenCode server /command endpoint response.
 */

@Serializable
data class Command(
    val name: String,
    val description: String? = null,
    // template can be a String or an object {} - use JsonElement for flexibility
    val template: JsonElement? = null,
    val subtask: Boolean = false,
    val hints: List<String>? = null,
    val mcp: Boolean = false
)
