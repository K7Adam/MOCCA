package com.mocca.app.ui.screens.chat.delegates

import com.mocca.app.data.repository.AgentRepository
import com.mocca.app.data.repository.AppStateStore
import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlinx.datetime.Clock

/**
 * Implementation of ChatConfigDelegate that observes AppStateStore as the single source of truth.
 * 
 * IMPORTANT: This delegate NO LONGER makes its own API calls. It observes AppStateStore
 * for all config state (providers, models, modes, agents). Selection changes are delegated
 * back to AppStateStore.
 * 
 * Architecture:
 * ```
 * AppStateStore (Single Source of Truth)
 *     ↓ (observed by)
 * ChatConfigDelegateImpl
 *     ↓ (exposed to)
 * ChatScreenModel → UI
 * ```
 */
class ChatConfigDelegateImpl(
    private val appStateStore: AppStateStore,
    private val sessionRepository: SessionRepository,
    private val agentRepository: AgentRepository,
    private val scope: CoroutineScope
) {

    // OBSERVE APP STATE STORE - Single Source of Truth
    // All state flows are derived from AppStateStore, not stored locally

    
    val providerInfo: StateFlow<ProviderResponse?> = appStateStore.providerInfo
    val selectedProviderId: StateFlow<String> = appStateStore.selectedProviderId
    val selectedModelId: StateFlow<String> = appStateStore.selectedModelId
    val selectedVariantId: StateFlow<String?> = appStateStore.selectedVariantId
    val modes: StateFlow<ImmutableList<Mode>> = appStateStore.modes
        .map { it.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())
    val selectedModeId: StateFlow<String?> = appStateStore.selectedModeId
    val modelName: StateFlow<String> = appStateStore.modelName
    val recentModels: StateFlow<ImmutableList<RecentModel>> = appStateStore.recentModels
        .map { it.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())
    
    val commands: StateFlow<ImmutableList<Command>> = appStateStore.commands
        .map { resource ->
            when (resource) {
                is Resource.Success -> resource.data.toImmutableList()
                is Resource.Loading -> (resource.data ?: emptyList()).toImmutableList()
                is Resource.Error -> (resource.data ?: emptyList()).toImmutableList()
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())
    
    // Agent name is derived from selected mode
    val agentName: StateFlow<String> = combine(
        appStateStore.selectedModeId,
        appStateStore.modes,
        appStateStore.agents
    ) { modeId, modes, agents ->
        when {
            modeId != null -> {
                // First try to find mode name
                val modeName = modes.find { it.id == modeId }?.name
                if (modeName != null) {
                    modeName.uppercase()
                } else {
                    // Try to find agent name
                    val agent = agents.find { it.name == modeId }
                    agent?.name?.uppercase() ?: modeId.uppercase()
                }
            }
            agents.isNotEmpty() -> {
                // Default to first non-hidden agent
                agents.filter { !it.hidden }.firstOrNull()?.name?.uppercase() ?: "BUILD"
            }
            else -> "--"
        }
    }.stateIn(scope, SharingStarted.Eagerly, "--")
    
    // Max tokens derived from provider info and selected model
    val maxTokens: StateFlow<Int> = combine(
        appStateStore.providerInfo,
        appStateStore.selectedProviderId,
        appStateStore.selectedModelId
    ) { providerInfo, providerId, modelId ->
        if (providerInfo == null || providerId.isEmpty() || modelId.isEmpty()) {
            0
        } else {
            parseContextLimit(providerInfo, providerId, modelId)
        }
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    /**
     * Select a model - delegates to AppStateStore.
     */
    fun selectModel(providerId: String, modelId: String) {
        Napier.i("[ChatConfigDelegate] selectModel: $providerId / $modelId")
        
        // Update AppStateStore (single source of truth)
        appStateStore.selectModel(providerId, modelId)
        
        // Also update session repository for persistence
        sessionRepository.setDefaultModel(modelId, providerId)
        
        // Add to recent models
        scope.launch {
            sessionRepository.addRecentModel(providerId, modelId)
        }
    }

    /**
     * Select a variant - delegates to AppStateStore.
     */
    fun selectVariant(variantId: String?) {
        Napier.v("[ChatConfigDelegate] selectVariant: $variantId")
        appStateStore.selectVariant(variantId)
    }

    /**
     * Select a mode - delegates to AppStateStore.
     */
    fun selectMode(modeId: String?) {
        val newModeId = modeId ?: "build"
        Napier.i("[ChatConfigDelegate] selectMode: $newModeId")
        
        // Update AppStateStore (single source of truth)
        appStateStore.selectMode(newModeId)
        
        // Also update session repository for persistence
        sessionRepository.setDefaultMode(newModeId)
    }

    private fun parseContextLimit(providerResponse: ProviderResponse, providerId: String, modelId: String): Int {
        val provider = providerResponse.all.find { it.id == providerId } ?: return 0
        val modelsObj = provider.models as? JsonObject ?: return 0
        val modelObj = modelsObj[modelId] as? JsonObject ?: return 0
        val limit = modelObj["limit"] as? JsonObject ?: return 0
        return limit["context"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: limit["max_tokens"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: limit["context_window"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: limit["context_length"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: 0
    }
}
