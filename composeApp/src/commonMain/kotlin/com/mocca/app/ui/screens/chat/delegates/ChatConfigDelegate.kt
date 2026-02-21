package com.mocca.app.ui.screens.chat.delegates

import com.mocca.app.domain.model.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

interface ChatConfigDelegate {
    val providerInfo: StateFlow<ProviderResponse?>
    val selectedProviderId: StateFlow<String>
    val selectedModelId: StateFlow<String>
    val selectedVariantId: StateFlow<String?>
    val modes: StateFlow<ImmutableList<Mode>>
    val selectedModeId: StateFlow<String?>
    val modelName: StateFlow<String>
    val agentName: StateFlow<String>
    val maxTokens: StateFlow<Int>
    val recentModels: StateFlow<ImmutableList<RecentModel>>
    val commands: StateFlow<ImmutableList<Command>>
    
    fun loadConfig()
    fun loadRecentModels()
    fun selectModel(providerId: String, modelId: String)
    fun selectVariant(variantId: String?)
    fun selectMode(modeId: String?)
}
