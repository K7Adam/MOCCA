package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class AiConfigFingerprint(
    val value: String = ""
)

@Serializable
@Immutable
data class AiRuntimeConfigSnapshot(
    val fingerprint: AiConfigFingerprint = AiConfigFingerprint(),
    val projectDir: String = "",
    val source: String = "unknown",
    val defaultSelection: AiSelection = AiSelection(),
    val providers: List<AiProviderOption> = emptyList(),
    val agents: List<AiAgentOption> = emptyList(),
    val modes: List<AiModeOption> = emptyList()
) {
    val projectKey: String get() = "${source}:${projectDir.ifBlank { "default" }}"

    fun findProvider(providerId: String?): AiProviderOption? =
        providerId?.let { id -> providers.firstOrNull { it.id == id } }

    fun findModel(providerId: String?, modelId: String?): AiModelOption? =
        findProvider(providerId)?.models?.firstOrNull { it.id == modelId }

    fun firstAvailableModel(): Pair<AiProviderOption, AiModelOption>? =
        providers.asSequence()
            .filter { it.connected }
            .flatMap { provider -> provider.models.asSequence().map { model -> provider to model } }
            .firstOrNull()
}

@Serializable
@Immutable
data class AiProviderOption(
    val id: String,
    val name: String,
    val source: String? = null,
    val connected: Boolean = false,
    val models: List<AiModelOption> = emptyList()
)

@Serializable
@Immutable
data class AiModelOption(
    val providerId: String,
    val id: String,
    val name: String = id,
    val status: String? = null,
    val contextLimit: Int? = null,
    val capabilities: List<String> = emptyList(),
    val variants: List<AiModelVariantOption> = emptyList()
)

@Serializable
@Immutable
data class AiModelVariantOption(
    val id: String,
    val name: String = id,
    val description: String? = null
)

@Serializable
@Immutable
data class AiAgentModelRef(
    val providerId: String? = null,
    val modelId: String? = null
)

@Serializable
@Immutable
data class AiAgentOption(
    val id: String,
    val name: String = id,
    val description: String? = null,
    val modeId: String? = null,
    val hidden: Boolean = false,
    val primary: Boolean = false,
    val model: AiAgentModelRef? = null
)

@Serializable
@Immutable
data class AiModeOption(
    val id: String,
    val name: String = id,
    val description: String? = null
)

@Serializable
@Immutable
data class AiSelection(
    val providerId: String? = null,
    val modelId: String? = null,
    val variantId: String? = null,
    val agentId: String? = null,
    val modeId: String? = null,
    val explicitModel: Boolean = false,
    val fingerprint: String? = null,
    val updatedAt: Long = 0L
)

@Serializable
@Immutable
data class AiEffectiveSelection(
    val providerId: String,
    val providerName: String,
    val modelId: String,
    val modelName: String,
    val variantId: String? = null,
    val variantName: String? = null,
    val agentId: String? = null,
    val agentName: String? = null,
    val modeId: String? = null,
    val modeName: String? = null,
    val contextLimit: Int? = null,
    val explicitModel: Boolean = false,
    val sourceFingerprint: String = ""
) {
    val displayModel: String get() = modelName.ifBlank { modelId }
    val displayProvider: String get() = providerName.ifBlank { providerId }
    val displayAgentOrMode: String get() = agentName ?: modeName ?: "--"
}

enum class AiConfigStatus {
    LOADING,
    READY,
    UPDATE_REQUIRED,
    ERROR
}

@Immutable
data class AiConfigState(
    val status: AiConfigStatus = AiConfigStatus.LOADING,
    val snapshot: AiRuntimeConfigSnapshot? = null,
    val effectiveSelection: AiEffectiveSelection? = null,
    val errorMessage: String? = null,
    val stale: Boolean = false
) {
    val isReady: Boolean get() = status == AiConfigStatus.READY && effectiveSelection != null
}

@Serializable
@Immutable
data class AiRecentModel(
    val projectKey: String,
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val providerName: String,
    val lastUsedAt: Long
)

internal const val AI_RECENT_MODEL_LIMIT = 8

internal fun selectAiRecentModelsForSnapshot(
    snapshot: AiRuntimeConfigSnapshot,
    stored: List<AiRecentModel>
): List<AiRecentModel> {
    return stored
        .asSequence()
        .filter { it.projectKey == snapshot.projectKey }
        .sortedByDescending { it.lastUsedAt }
        .distinctBy { it.providerId to it.modelId }
        .take(AI_RECENT_MODEL_LIMIT)
        .toList()
}

@Immutable
data class ModelPickerUiState(
    val current: AiEffectiveSelection? = null,
    val recentModels: List<AiRecentModel> = emptyList(),
    val providers: List<AiProviderOption> = emptyList(),
    val selectedProviderId: String? = null,
    val selectedModelId: String? = null,
    val status: AiConfigStatus = AiConfigStatus.LOADING,
    val errorMessage: String? = null
) {
    val isAvailable: Boolean get() = providers.any { it.models.isNotEmpty() }
}

@Immutable
data class VariantPickerUiState(
    val variants: List<AiModelVariantOption> = emptyList(),
    val selectedVariantId: String? = null
)

@Serializable
@Immutable
data class AiBridgeMessageModel(
    @SerialName("providerID")
    val providerId: String,
    @SerialName("modelID")
    val modelId: String
)

@Serializable
@Immutable
data class AiBridgeMessageRequest(
    val sessionId: String,
    val text: String? = null,
    val parts: List<ChatPart> = emptyList(),
    val model: AiBridgeMessageModel,
    val variant: String? = null,
    val agent: String? = null,
    val legacyMode: String? = null
)
