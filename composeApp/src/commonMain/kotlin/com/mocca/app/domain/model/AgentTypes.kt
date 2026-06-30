package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Agent configuration types.
 * Matches OpenCode server /agent endpoint response.
 */

@Serializable
@Immutable
data class Agent(
    val name: String,
    val mode: String? = null,
    val description: String? = null,
    val prompt: String? = null,
    @SerialName("native")
    private val nativeValue: Boolean? = false,
    @SerialName("hidden")
    private val hiddenValue: Boolean? = false,
    val color: String? = null,
    val model: AgentModel? = null,
    val permission: List<AgentPermission>? = null,
    val options: JsonElement? = null
) {
    // Use name as id for UI compatibility
    val id: String get() = name
    val native: Boolean get() = nativeValue == true
    val hidden: Boolean get() = hiddenValue == true
}

@Serializable
@Immutable
data class AgentModel(
    @SerialName("providerID")
    val providerId: String? = null,
    @SerialName("modelID")
    val modelId: String? = null
)

@Serializable
@Immutable
data class AgentPermission(
    val permission: String,
    val action: String,
    val pattern: String
)
