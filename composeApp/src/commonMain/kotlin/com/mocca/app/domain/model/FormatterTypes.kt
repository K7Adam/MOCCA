package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.Serializable

/**
 * Formatter status types.
 * Matches OpenCode server /formatter endpoint response.
 */

@Serializable
@Immutable
data class FormatterStatus(
    val name: String,
    val extensions: List<String> = emptyList(),
    val enabled: Boolean = false
) {
    // Derive status for UI display
    val status: String
        get() = if (enabled) "enabled" else "disabled"
}
