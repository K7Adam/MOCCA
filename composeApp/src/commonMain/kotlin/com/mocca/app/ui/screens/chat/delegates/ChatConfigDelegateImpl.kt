package com.mocca.app.ui.screens.chat.delegates

import com.mocca.app.data.repository.AgentRepository
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

class ChatConfigDelegateImpl(
    private val sessionRepository: SessionRepository,
    private val agentRepository: AgentRepository,
    private val scope: CoroutineScope
) : ChatConfigDelegate {
    
    private val _providerInfo = MutableStateFlow<ProviderResponse?>(null)
    override val providerInfo = _providerInfo.asStateFlow()
    
    private val _selectedProviderId = MutableStateFlow("")
    override val selectedProviderId = _selectedProviderId.asStateFlow()
    
    private val _selectedModelId = MutableStateFlow("")
    override val selectedModelId = _selectedModelId.asStateFlow()
    
    private val _selectedVariantId = MutableStateFlow<String?>(null)
    override val selectedVariantId = _selectedVariantId.asStateFlow()
    
    private val _modes = MutableStateFlow<ImmutableList<Mode>>(persistentListOf())
    override val modes = _modes.asStateFlow()
    
    private val _selectedModeId = MutableStateFlow<String?>(null)
    override val selectedModeId = _selectedModeId.asStateFlow()
    
    private val _modelName = MutableStateFlow("--")
    override val modelName = _modelName.asStateFlow()
    
    private val _agentName = MutableStateFlow("--")
    override val agentName = _agentName.asStateFlow()
    
    private val _maxTokens = MutableStateFlow(0)
    override val maxTokens = _maxTokens.asStateFlow()
    
    private val _recentModels = MutableStateFlow<ImmutableList<RecentModel>>(persistentListOf())
    override val recentModels = _recentModels.asStateFlow()

    override fun loadConfig() {
        scope.launch(Dispatchers.IO) {
            sessionRepository.loadDefaultConfig()
            sessionRepository.getProviderInfo().onSuccess { providerResponse ->
                val (defaultModelId, defaultProviderId) = sessionRepository.getDefaultModelProvider()
                
                val finalProviderId = if (_selectedProviderId.value.isEmpty()) defaultProviderId else _selectedProviderId.value
                val finalModelId = if (_selectedModelId.value.isEmpty()) defaultModelId else _selectedModelId.value
                
                _providerInfo.value = providerResponse
                _selectedProviderId.value = finalProviderId
                _selectedModelId.value = finalModelId
                
                if (finalModelId.isNotEmpty()) {
                    _modelName.value = finalModelId.uppercase().replace("-", " ").take(30)
                    _maxTokens.value = parseContextLimit(providerResponse, finalProviderId, finalModelId)
                }
            }
            
            agentRepository.getAgents().collect { resource ->
                if (resource is Resource.Success) {
                    val agents = resource.data
                    val newModes = agents.filter { !it.hidden }.map { agent ->
                        Mode(id = agent.name, name = agent.name, description = agent.description)
                    }.toImmutableList()
                    
                    val defaultMode = sessionRepository.getDefaultMode()
                    val currentSelection = _selectedModeId.value
                    val newSelection = if (newModes.any { it.id == currentSelection }) currentSelection else defaultMode
                    
                    _modes.value = newModes
                    _selectedModeId.value = newSelection
                    
                    if (newSelection != null) {
                        val modeName = newModes.find { it.id == newSelection }?.name ?: newSelection.uppercase()
                        _agentName.value = modeName.uppercase()
                    }
                }
            }
        }
    }

    override fun loadRecentModels() {
        scope.launch {
            val recent = sessionRepository.getRecentModels()
            _recentModels.value = recent.toImmutableList()
        }
    }

    override fun selectModel(providerId: String, modelId: String) {
        sessionRepository.setDefaultModel(modelId, providerId)
        _selectedProviderId.value = providerId
        _selectedModelId.value = modelId
        _selectedVariantId.value = null
        _modelName.value = modelId.uppercase().replace("-", " ").take(30)
        _providerInfo.value?.let { _maxTokens.value = parseContextLimit(it, providerId, modelId) }
        
        scope.launch {
            sessionRepository.addRecentModel(providerId, modelId)
            loadRecentModels()
        }
    }

    override fun selectVariant(variantId: String?) {
        _selectedVariantId.value = variantId
    }

    override fun selectMode(modeId: String?) {
        val newModeId = modeId ?: "build"
        sessionRepository.setDefaultMode(newModeId)
        _selectedModeId.value = newModeId
        val modeName = _modes.value.find { it.id == newModeId }?.name ?: newModeId.uppercase()
        _agentName.value = modeName.uppercase()
    }

    private fun parseContextLimit(providerResponse: ProviderResponse, providerId: String, modelId: String): Int {
        val provider = providerResponse.all.find { it.id == providerId } ?: return 0
        val modelsObj = provider.models as? JsonObject ?: return 0
        val modelObj = modelsObj[modelId] as? JsonObject ?: return 0
        val limit = modelObj["limit"] as? JsonObject ?: return 0
        return limit["context"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: limit["max_tokens"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: limit["context_length"]?.let { (it as? JsonPrimitive)?.intOrNull }
            ?: 0
    }
}
