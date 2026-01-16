package com.mocca.app.util

/**
 * Definition of a terminal command.
 */
data class TerminalCommand(
    val trigger: String, // e.g., "clear" for /clear
    val description: String,
    val action: suspend () -> Unit
)
