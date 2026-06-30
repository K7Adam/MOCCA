package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Slash command types.
 * Matches OpenCode server /command endpoint response.
 */

@Serializable
@Immutable
data class Command(
    val name: String,
    val description: String? = null,
    // template can be a String or an object {} - use JsonElement for flexibility
    val template: JsonElement? = null,
    @SerialName("subtask")
    private val subtaskValue: Boolean? = false,
    val hints: List<String>? = null,
    @SerialName("mcp")
    private val mcpValue: Boolean? = false
) {
    val subtask: Boolean get() = subtaskValue == true
    val mcp: Boolean get() = mcpValue == true
}

/**
 * Built-in OpenCode CLI commands that are always available.
 * These commands are defined by the OpenCode TUI and should be included
 * even if the server doesn't return them from the /command endpoint.
 * 
 * Reference: https://opencode.ai/docs/de/tui/#befehle
 */
val BUILTIN_COMMANDS = listOf(
    Command("compact", "Compact the current session (alias: /summarize)"),
    Command("connect", "Add a provider to OpenCode"),
    Command("details", "Toggle tool execution details"),
    Command("editor", "Open external editor for composing messages"),
    Command("exit", "Exit OpenCode (aliases: /quit, /q)"),
    Command("export", "Export current conversation to Markdown"),
    Command("help", "Show the help dialog"),
    Command("init", "Create or update AGENTS.md file"),
    Command("models", "List available models"),
    Command("new", "Start a new session (alias: /clear)"),
    Command("redo", "Redo a previously undone message"),
    Command("sessions", "List and switch between sessions (aliases: /resume, /continue)"),
    Command("share", "Share current session"),
    Command("themes", "List available themes"),
    Command("thinking", "Toggle thinking/reasoning block visibility"),
    Command("undo", "Undo last message in the conversation"),
    Command("unshare", "Unshare current session")
)

/**
 * Merge API commands with built-in commands.
 * API commands take precedence (they can override built-ins with custom versions).
 * Built-in commands fill in gaps where the server doesn't provide them.
 */
fun mergeCommands(apiCommands: List<Command>): List<Command> {
    val apiCommandNames = apiCommands.map { it.name.lowercase() }.toSet()
    val mergedCommands = apiCommands.toMutableList()
    
    // Add built-in commands that aren't already provided by API
    BUILTIN_COMMANDS.forEach { builtin ->
        if (builtin.name.lowercase() !in apiCommandNames) {
            mergedCommands.add(builtin)
        }
    }
    
    return mergedCommands.sortedBy { it.name }
}

// COMMAND & SHELL EXECUTION


/**
 * Request body for POST /session/:id/command.
 */
@Serializable
@Immutable
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
@Immutable
data class ShellExecutionRequest(
    val command: String,
    val agent: String,
    val model: String? = null
)
