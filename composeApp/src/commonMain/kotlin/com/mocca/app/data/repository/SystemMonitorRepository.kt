package com.mocca.app.data.repository

import com.mocca.app.bridge.client.NativeCliUnavailableException
import com.mocca.app.bridge.client.requestPayload
import com.mocca.app.bridge.client.requireClient
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.opencode.BridgeResponseException
import com.mocca.app.domain.model.PortInfo
import com.mocca.app.domain.model.ProcessInfo
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.SystemResources
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class SystemMonitorRepository(
    private val bridgeConnectionManager: BridgeConnectionManager,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    companion object {
        private const val TAG = "SystemMonitorRepository"
    }

    val nativeMonitorAvailable: Flow<Boolean> =
        bridgeConnectionManager.status
            .map { it.hasNativeMonitorCapabilities() }
            .distinctUntilChanged()

    fun isNativeMonitorAvailable(): Boolean =
        bridgeConnectionManager.status.value.hasNativeMonitorCapabilities()

    suspend fun getProcesses(sessionId: String? = null): Resource<List<ProcessInfo>> {
        return bridgeRequest("process.list") {
            bridgeConnectionManager.requireClient("process.list")
                .requestPayload(ns = "process", action = "list", json = json)
        }
    }

    suspend fun getPorts(sessionId: String? = null): Resource<List<PortInfo>> {
        return bridgeRequest("ports.list") {
            bridgeConnectionManager.requireClient("ports.list")
                .requestPayload(ns = "ports", action = "list", json = json)
        }
    }

    suspend fun getSystemResources(sessionId: String? = null): Resource<SystemResources> {
        return bridgeRequest("monitor.snapshot") {
            bridgeConnectionManager.requireClient("monitor.snapshot")
                .requestPayload(ns = "monitor", action = "snapshot", json = json)
        }
    }

    private suspend fun <T> bridgeRequest(feature: String, block: suspend () -> T): Resource<T> {
        return try {
            Resource.Success(block())
        } catch (error: NativeCliUnavailableException) {
            Napier.d("$TAG $feature skipped: ${error.message}")
            Resource.Error(error.toResourceMessage("MOCCA CLI bridge is not connected"), cause = error)
        } catch (error: Exception) {
            Napier.w("$TAG $feature failed through MOCCA CLI", error)
            Resource.Error(error.toResourceMessage("Unable to read system information"), cause = error)
        }
    }

    private fun Exception.toResourceMessage(fallback: String): String {
        return when (this) {
            is NativeCliUnavailableException -> message ?: "MOCCA CLI bridge is not connected"
            is BridgeResponseException -> message ?: fallback
            else -> message ?: fallback
        }
    }

    private fun BridgeConnectionStatus.hasNativeMonitorCapabilities(): Boolean =
        this is BridgeConnectionStatus.Connected &&
            capabilities.process.native &&
            capabilities.ports.native &&
            capabilities.monitor.native
}
