package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.opencode.BridgeFeatureUnavailableException
import com.mocca.app.bridge.opencode.OpenCodeBridgeRepository
import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalCoroutinesApi::class)
class AiRuntimeConfigRepository(
    private val apiClient: MoccaApiClient,
    private val localCache: LocalCache,
    private val bridgeConnectionManager: BridgeConnectionManager,
    private val connectionManager: ConnectionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _configState = MutableStateFlow(AiConfigState())
    val configState: StateFlow<AiConfigState> = _configState.asStateFlow()

    private val _effectiveSelection = MutableStateFlow<AiEffectiveSelection?>(null)
    val effectiveSelection: StateFlow<AiEffectiveSelection?> = _effectiveSelection.asStateFlow()

    private val _modelPickerState = MutableStateFlow(ModelPickerUiState())
    val modelPickerState: StateFlow<ModelPickerUiState> = _modelPickerState.asStateFlow()

    private val _variantPickerState = MutableStateFlow(VariantPickerUiState())
    val variantPickerState: StateFlow<VariantPickerUiState> = _variantPickerState.asStateFlow()

    private val _recentModels = MutableStateFlow<List<AiRecentModel>>(emptyList())
    val recentModels: StateFlow<List<AiRecentModel>> = _recentModels.asStateFlow()

    private var currentSnapshot: AiRuntimeConfigSnapshot? = null
    private var currentSelection: AiSelection? = null
    private var lastAiEventSeq: Long? = null
    private var refreshJob: Job? = null

    init {
        scope.launch {
            combine(
                bridgeConnectionManager.status,
                connectionManager.status
            ) { bridgeStatus, serverStatus -> bridgeStatus to serverStatus }
                .distinctUntilChanged()
                .collect {
                    // Launch refresh in a child coroutine so that CancellationException
                    // (e.g. from HttpClient recreation) cancels the child, not the
                    // collect loop.  Without this, the observer dies on the first
                    // cancellation and no retry happens when the bridge later connects.
                    refreshJob?.cancel()
                    refreshJob = scope.launch {
                        try {
                            refresh(force = true)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            // Expected when a newer refresh supersedes this one.
                        } catch (e: Exception) {
                            Napier.e("[AiRuntimeConfigRepository] refresh failed in observer", e)
                        }
                    }
                }
        }

        scope.launch {
            bridgeConnectionManager.client
                .flatMapLatest { client -> client?.events ?: emptyFlow() }
                .filter { event -> event.ns == "ai" && event.event.startsWith("config.") }
                .collect { event ->
                    val seq = event.seq
                    val previous = lastAiEventSeq
                    lastAiEventSeq = seq
                    if (previous != null && seq != null && seq != previous + 1) {
                        Napier.w("[AiRuntimeConfigRepository] AI config event seq gap: $previous -> $seq")
                    }
                    refreshJob?.cancel()
                    refreshJob = scope.launch {
                        try {
                            refresh(force = true)
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            // Expected when a newer refresh supersedes this one.
                        } catch (e: Exception) {
                            Napier.e("[AiRuntimeConfigRepository] refresh failed in event observer", e)
                        }
                    }
                }
        }
    }

    suspend fun refresh(force: Boolean = false) {
        _configState.update { it.copy(status = AiConfigStatus.LOADING, stale = it.snapshot != null, errorMessage = null) }
        try {
            val bridgeStatus = bridgeConnectionManager.status.value
            val snapshot = if (bridgeStatus is BridgeConnectionStatus.Connected) {
                val client = bridgeConnectionManager.client.value
                    ?: throw BridgeFeatureUnavailableException("MOCCA CLI connection")
                if (!bridgeStatus.capabilities.ai.configNormalized) {
                    _configState.value = AiConfigState(
                        status = AiConfigStatus.UPDATE_REQUIRED,
                        errorMessage = "MOCCA CLI aktualisieren: ai.config.get wird benoetigt."
                    )
                    _effectiveSelection.value = null
                    updatePickerState(null, null, emptyList())
                    return
                }
                OpenCodeBridgeRepository(client).fetchAiRuntimeConfig(forceRefresh = force)
            } else {
                loadLegacyHttpSnapshot()
            }

            applySnapshot(snapshot)
        } catch (error: Exception) {
            Napier.e("[AiRuntimeConfigRepository] Failed to load AI runtime config", error)
            _configState.value = AiConfigState(
                status = AiConfigStatus.ERROR,
                snapshot = currentSnapshot,
                effectiveSelection = _effectiveSelection.value,
                errorMessage = error.message ?: "AI configuration could not be loaded",
                stale = currentSnapshot != null
            )
        }
    }

    suspend fun selectModel(providerId: String, modelId: String) {
        val snapshot = currentSnapshot ?: return
        val model = snapshot.findModel(providerId, modelId) ?: return
        val next = (currentSelection ?: snapshot.defaultSelection).copy(
            providerId = providerId,
            modelId = modelId,
            variantId = defaultVariantFor(model),
            explicitModel = true,
            fingerprint = snapshot.fingerprint.value,
            updatedAt = now()
        )
        applySelection(snapshot, next, persist = true)
        localCache.insertAiRecentModel(
            AiRecentModel(
                projectKey = snapshot.projectKey,
                providerId = providerId,
                modelId = modelId,
                displayName = model.name.ifBlank { model.id },
                providerName = snapshot.findProvider(providerId)?.name ?: providerId,
                lastUsedAt = now()
            )
        )
        _recentModels.value = localCache.getAiRecentModels(snapshot.projectKey)
        updatePickerState(snapshot, _effectiveSelection.value, _recentModels.value)
    }

    suspend fun selectVariant(variantId: String?) {
        val snapshot = currentSnapshot ?: return
        val current = currentSelection ?: return
        val model = snapshot.findModel(current.providerId, current.modelId) ?: return
        val normalizedVariant = variantId?.takeIf { id -> model.variants.any { it.id == id } }
        applySelection(
            snapshot = snapshot,
            selection = current.copy(
                variantId = normalizedVariant,
                fingerprint = snapshot.fingerprint.value,
                updatedAt = now()
            ),
            persist = true
        )
    }

    suspend fun selectAgentOrMode(id: String?) {
        val snapshot = currentSnapshot ?: return
        val current = currentSelection ?: snapshot.defaultSelection
        val next = if (id == null) {
            current.copy(agentId = null, modeId = null, updatedAt = now())
        } else {
            val agent = snapshot.agents.firstOrNull { it.id == id && !it.hidden }
            val mode = snapshot.modes.firstOrNull { it.id == id }
            current.copy(
                agentId = agent?.id ?: current.agentId,
                modeId = mode?.id ?: agent?.modeId ?: current.modeId,
                explicitModel = current.explicitModel,
                updatedAt = now()
            )
        }
        applySelection(snapshot, next.copy(fingerprint = snapshot.fingerprint.value), persist = true)
    }

    fun requireEffectiveSelection(): AiEffectiveSelection {
        return _effectiveSelection.value ?: throw IllegalStateException(
            _configState.value.errorMessage ?: "No valid AI model is configured"
        )
    }

    private suspend fun applySnapshot(snapshot: AiRuntimeConfigSnapshot) {
        currentSnapshot = snapshot
        val persisted = localCache.getAiSelection(snapshot.projectKey)
            ?.takeIf { it.fingerprint == snapshot.fingerprint.value }
            ?.let { validateSelection(snapshot, it) }
        val selected = persisted
            ?: validateSelection(snapshot, snapshot.defaultSelection.copy(fingerprint = snapshot.fingerprint.value))
            ?: snapshot.firstAvailableModel()?.let { (provider, model) ->
                AiSelection(
                    providerId = provider.id,
                    modelId = model.id,
                    variantId = defaultVariantFor(model),
                    agentId = snapshot.agents.firstOrNull { it.primary }?.id ?: snapshot.agents.firstOrNull()?.id,
                    modeId = snapshot.modes.firstOrNull()?.id,
                    explicitModel = false,
                    fingerprint = snapshot.fingerprint.value,
                    updatedAt = now()
                )
            }

        if (selected == null) {
            _configState.value = AiConfigState(
                status = AiConfigStatus.ERROR,
                snapshot = snapshot,
                errorMessage = "No configured provider or model was found."
            )
            _effectiveSelection.value = null
            updatePickerState(snapshot, null, emptyList())
            return
        }

        val recents = currentProjectRecents(snapshot)
        _recentModels.value = recents
        applySelection(snapshot, selected, persist = persisted == null)
        updatePickerState(snapshot, _effectiveSelection.value, recents)
    }

    private suspend fun applySelection(
        snapshot: AiRuntimeConfigSnapshot,
        selection: AiSelection,
        persist: Boolean
    ) {
        val validated = validateSelection(snapshot, selection) ?: return
        currentSelection = validated
        val effective = effectiveSelection(snapshot, validated)
        _effectiveSelection.value = effective
        _configState.value = AiConfigState(
            status = AiConfigStatus.READY,
            snapshot = snapshot,
            effectiveSelection = effective
        )
        updateVariantState(snapshot, validated)
        updatePickerState(snapshot, effective, _recentModels.value)
        if (persist) {
            localCache.saveAiSelection(snapshot.projectKey, validated.copy(updatedAt = now()))
        }
    }

    private fun validateSelection(snapshot: AiRuntimeConfigSnapshot, selection: AiSelection): AiSelection? {
        val agent = selection.agentId
            ?.let { id -> snapshot.agents.firstOrNull { it.id == id && !it.hidden } }
        val mode = selection.modeId
            ?.let { id -> snapshot.modes.firstOrNull { it.id == id } }

        val overrideModel = agent?.model
            ?.takeIf { !selection.explicitModel }
            ?.takeIf { snapshot.findModel(it.providerId, it.modelId) != null }

        val providerId = overrideModel?.providerId ?: selection.providerId
        val modelId = overrideModel?.modelId ?: selection.modelId
        val model = snapshot.findModel(providerId, modelId) ?: return null
        val variantId = selection.variantId
            ?.takeIf { candidate -> model.variants.any { it.id == candidate } }
            ?: defaultVariantFor(model)

        return selection.copy(
            providerId = providerId,
            modelId = modelId,
            variantId = variantId,
            agentId = agent?.id,
            modeId = mode?.id ?: agent?.modeId,
            fingerprint = snapshot.fingerprint.value
        )
    }

    private fun effectiveSelection(snapshot: AiRuntimeConfigSnapshot, selection: AiSelection): AiEffectiveSelection {
        val agent = selection.agentId
            ?.let { id -> snapshot.agents.firstOrNull { it.id == id && !it.hidden } }
        val mode = selection.modeId
            ?.let { id -> snapshot.modes.firstOrNull { it.id == id } }
        val modelOverride = agent?.model
            ?.takeIf { !selection.explicitModel }
            ?.takeIf { snapshot.findModel(it.providerId, it.modelId) != null }

        val providerId = modelOverride?.providerId ?: selection.providerId.orEmpty()
        val modelId = modelOverride?.modelId ?: selection.modelId.orEmpty()
        val provider = snapshot.findProvider(providerId) ?: error("Invalid AI provider selection")
        val model = snapshot.findModel(providerId, modelId) ?: error("Invalid AI model selection")
        val variant = selection.variantId?.let { id -> model.variants.firstOrNull { it.id == id } }

        return AiEffectiveSelection(
            providerId = provider.id,
            providerName = provider.name,
            modelId = model.id,
            modelName = model.name,
            variantId = variant?.id,
            variantName = variant?.name,
            agentId = agent?.id,
            agentName = agent?.name,
            modeId = mode?.id ?: agent?.modeId,
            modeName = mode?.name,
            contextLimit = model.contextLimit,
            explicitModel = selection.explicitModel,
            sourceFingerprint = snapshot.fingerprint.value
        )
    }

    private fun updateVariantState(snapshot: AiRuntimeConfigSnapshot, selection: AiSelection) {
        val model = snapshot.findModel(selection.providerId, selection.modelId)
        _variantPickerState.value = VariantPickerUiState(
            variants = model?.variants.orEmpty(),
            selectedVariantId = selection.variantId
        )
    }

    private fun updatePickerState(
        snapshot: AiRuntimeConfigSnapshot?,
        effective: AiEffectiveSelection?,
        recents: List<AiRecentModel>
    ) {
        _modelPickerState.value = ModelPickerUiState(
            current = effective,
            recentModels = recents,
            providers = snapshot?.providers.orEmpty(),
            selectedProviderId = effective?.providerId,
            selectedModelId = effective?.modelId,
            status = _configState.value.status,
            errorMessage = _configState.value.errorMessage
        )
    }

    private suspend fun currentProjectRecents(snapshot: AiRuntimeConfigSnapshot): List<AiRecentModel> {
        return selectAiRecentModelsForSnapshot(
            snapshot = snapshot,
            stored = localCache.getAiRecentModels(snapshot.projectKey)
        )
    }

    private suspend fun loadLegacyHttpSnapshot(): AiRuntimeConfigSnapshot {
        val providers = apiClient.getProviderInfo().getOrElse { ProviderResponse() }
        val config = apiClient.getConfig().getOrNull()
        val modes = apiClient.getModes().getOrElse { emptyList() }
        val agents = apiClient.getAgents().getOrElse { emptyList() }

        val normalizedProviders = providers.all.map { provider ->
            val modelObjects = provider.models as? JsonObject
            AiProviderOption(
                id = provider.id,
                name = provider.name,
                source = provider.source,
                connected = provider.connected || provider.id in providers.connected,
                models = modelObjects.orEmpty().map { (modelId, value) ->
                    val obj = value as? JsonObject
                    val name = obj?.get("name")?.jsonPrimitive?.contentOrNull ?: modelId
                    val status = obj?.get("status")?.jsonPrimitive?.contentOrNull
                    val variants = (obj?.get("variants") as? JsonObject).orEmpty().map { (variantId, variantValue) ->
                        val variantObj = variantValue as? JsonObject
                        AiModelVariantOption(
                            id = variantId,
                            name = variantObj?.get("name")?.jsonPrimitive?.contentOrNull ?: variantId,
                            description = variantObj?.get("description")?.jsonPrimitive?.contentOrNull
                        )
                    }.sortedBy { it.id }
                    AiModelOption(
                        providerId = provider.id,
                        id = modelId,
                        name = name,
                        status = status,
                        variants = variants
                    )
                }.sortedBy { it.name }
            )
        }.sortedBy { it.name }

        val aiAgents = agents.filterNot { it.hidden }.map { agent ->
            AiAgentOption(
                id = agent.id,
                name = agent.name,
                description = agent.description,
                modeId = agent.mode,
                hidden = agent.hidden,
                model = agent.model?.let { AiAgentModelRef(it.providerId, it.modelId) }
            )
        }
        val aiModes = if (modes.isNotEmpty()) {
            modes.map { AiModeOption(id = it.id, name = it.name, description = it.description) }
        } else {
            aiAgents.map { AiModeOption(id = it.id, name = it.name, description = it.description) }
        }
        val splitDefault = config?.model?.split("/", limit = 2)?.takeIf { it.size == 2 }
        val first = normalizedProviders.flatMap { provider ->
            provider.models.map { model -> provider.id to model.id }
        }.firstOrNull()
        return AiRuntimeConfigSnapshot(
            fingerprint = AiConfigFingerprint(
                value = "${config?.model.orEmpty()}|${normalizedProviders.joinToString { it.id }}|${aiAgents.joinToString { it.id }}"
            ),
            projectDir = "legacy-http",
            source = "opencode-http",
            defaultSelection = AiSelection(
                providerId = splitDefault?.get(0) ?: first?.first,
                modelId = splitDefault?.get(1) ?: first?.second,
                agentId = aiAgents.firstOrNull()?.id,
                modeId = aiModes.firstOrNull()?.id
            ),
            providers = normalizedProviders,
            agents = aiAgents,
            modes = aiModes
        )
    }

    private fun defaultVariantFor(model: AiModelOption): String? =
        model.variants.singleOrNull()?.id

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
