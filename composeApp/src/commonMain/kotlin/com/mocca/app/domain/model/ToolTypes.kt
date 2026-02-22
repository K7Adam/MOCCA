package com.mocca.app.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Matches OpenCode server /experimental/tool/{id} endpoint response.
 * Represents the schema and metadata for a specific tool.
 */
@Serializable
data class ToolSchema(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonObject? = null
)
