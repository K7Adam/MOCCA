package com.mocca.app.bridge.connection

import com.mocca.app.bridge.client.DirectBridgeTarget
import com.mocca.app.data.local.LocalCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val BRIDGE_TARGET_SETTING_KEY = "mocca.bridge.direct.target"

interface BridgeTargetStore {
    suspend fun readTargetJson(): String?

    suspend fun writeTargetJson(value: String)

    suspend fun clearTargetJson()
}

class LocalCacheBridgeTargetStore(
    private val localCache: LocalCache
) : BridgeTargetStore {
    override suspend fun readTargetJson(): String? {
        return localCache.getSetting(BRIDGE_TARGET_SETTING_KEY)
    }

    override suspend fun writeTargetJson(value: String) {
        localCache.saveSetting(BRIDGE_TARGET_SETTING_KEY, value)
    }

    override suspend fun clearTargetJson() {
        localCache.deleteSetting(BRIDGE_TARGET_SETTING_KEY)
    }
}

class BridgeTargetRepository(
    private val store: BridgeTargetStore,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    private val _activeTarget = MutableStateFlow<DirectBridgeTarget?>(null)
    val activeTarget: StateFlow<DirectBridgeTarget?> = _activeTarget.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    suspend fun load() {
        _activeTarget.value = loadPersistedTarget()
        _isLoaded.value = true
    }

    suspend fun save(target: DirectBridgeTarget) {
        store.writeTargetJson(json.encodeToString(target))
        _activeTarget.value = target
        _isLoaded.value = true
    }

    suspend fun clear() {
        store.clearTargetJson()
        _activeTarget.value = null
        _isLoaded.value = true
    }

    suspend fun loadPersistedTarget(): DirectBridgeTarget? {
        val raw = store.readTargetJson() ?: return null
        return try {
            json.decodeFromString<DirectBridgeTarget>(raw)
        } catch (_: SerializationException) {
            store.clearTargetJson()
            null
        } catch (_: IllegalArgumentException) {
            store.clearTargetJson()
            null
        }
    }
}
