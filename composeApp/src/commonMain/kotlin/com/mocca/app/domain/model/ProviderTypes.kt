package com.mocca.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Provider and model configuration types.
 * Matches OpenCode server /provider endpoint response.
 */

@Serializable
data class ProviderInfo(
    val id: String,
    val name: String,
    val source: String? = null,
    val env: List<String>? = null,
    val options: JsonElement? = null,
    // models is a Map<String, ProviderModel> in the API, use JsonElement for flexibility
    val models: JsonElement? = null
) {
    // Derive connected status from having any models
    val connected: Boolean 
        get() = (models as? JsonObject)?.isNotEmpty() == true
    
    // Get model count
    val modelCount: Int
        get() = (models as? JsonObject)?.size ?: 0
}

@Serializable
data class ProviderModel(
    val id: String,
    val name: String,
    @SerialName("providerID")
    val providerId: String? = null,
    val family: String? = null,
    val status: String? = null,
    val api: JsonElement? = null,
    val cost: JsonElement? = null,
    val limit: JsonElement? = null,
    val capabilities: JsonElement? = null
)

@Serializable
data class ProviderResponse(
    val all: List<ProviderInfo> = emptyList(),
    // default is Map<providerId, modelId> in the API
    val default: Map<String, String>? = null,
    val connected: List<String> = emptyList()
)

// Legacy type for backward compatibility
@Serializable
data class DefaultProvider(
    val provider: String? = null,
    val model: String? = null
)

@Serializable
data class ProvidersConfig(
    val providers: List<ProviderInfo> = emptyList(),
    val default: Map<String, String> = emptyMap()
)
