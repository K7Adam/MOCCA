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
) : ChatConfigDelegate {
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // OBSERVE APP STATE STORE - Single Source of Truth
    // All state flows are derived from AppStateStore, not stored locally
    // ═══════════════════════════════════════════════════════════════════════════════
    
    override val providerInfo: StateFlow<ProviderResponse?> = appStateStore.providerInfo
    override val selectedProviderId: StateFlow<String> = appStateStore.selectedProviderId
    override val selectedModelId: StateFlow<String> = appStateStore.selectedModelId
    override val selectedVariantId: StateFlow<String?> = appStateStore.selectedVariantId
    override val modes: StateFlow<ImmutableList<Mode>> = appStateStore.modes
        .map { it.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())
    override val selectedModeId: StateFlow<String?> = appStateStore.selectedModeId
    override val modelName: StateFlow<String> = appStateStore.modelName
    override val recentModels: StateFlow<ImmutableList<RecentModel>> = appStateStore.recentModels
        .map { it.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())
    
    // Agent name is derived from selected mode
    override val agentName: StateFlow<String> = combine(
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
    override val maxTokens: StateFlow<Int> = combine(
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
     * Load config - triggers AppStateStore to load data.
     * This is called when the chat screen initializes.
     */
    override fun loadConfig() {
        // AppStateStore.loadConfig() is called on connection via RealtimeSyncService
        // But we also trigger it here to ensure config is loaded when chat opens
        Napier.i("[ChatConfigDelegate] loadConfig() called - triggering AppStateStore sync")
        
        scope.launch(Dispatchers.IO) {
            // Ensure default config is loaded in session repository
            sessionRepository.loadDefaultConfig()
        }
    }

    /**
     * Load recent models from AppStateStore.
     */
    override fun loadRecentModels() {
        // Recent models are already observed from AppStateStore
        // This method exists for backward compatibility
        Napier.v("[ChatConfigDelegate] loadRecentModels() - state observed from AppStateStore")
    }

    /**
     * Select a model - delegates to AppStateStore.
     */
    override fun selectModel(providerId: String, modelId: String) {
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
    override fun selectVariant(variantId: String?) {
        Napier.v("[ChatConfigDelegate] selectVariant: $variantId")
        appStateStore.selectVariant(variantId)
    }

    /**
     * Select a mode - delegates to AppStateStore.
     */
    override fun selectMode(modeId: String?) {
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
