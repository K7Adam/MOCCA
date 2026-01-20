package com.mocca.app.domain.model

import kotlinx.serialization.SerialName
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

// ═══════════════════════════════════════════════════════════════════════════
// COMMAND & SHELL EXECUTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Request body for POST /session/:id/command.
 */
@Serializable
data class CommandExecutionRequest(
    val command: String,
    val arguments: String? = null,
    val agent: String? = null,
    val model: String? = null,
    @SerialName("messageID")
    val messageID: String? = null
)

/**
 * Request body for POST /session/:id/shell.
 */
@Serializable
data class ShellExecutionRequest(
    val command: String,
    val agent: String,
    val model: String? = null
)
