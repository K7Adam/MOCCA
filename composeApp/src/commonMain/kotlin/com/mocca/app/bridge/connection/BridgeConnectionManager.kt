package com.mocca.app.bridge.connection

import com.mocca.app.bridge.client.BridgeTransport
import com.mocca.app.bridge.client.DirectBridgeTarget
import com.mocca.app.bridge.client.MoccaBridgeClient
import com.mocca.app.bridge.opencode.BridgeResponseException
import com.mocca.app.bridge.opencode.OpenCodeBridgeRepository
import com.mocca.app.bridge.protocol.BridgeCapabilities
import com.mocca.app.bridge.protocol.BRIDGE_PROTOCOL_VERSION
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min
import kotlinx.serialization.Serializable

interface BridgeTransportFactory {
    suspend fun open(target: DirectBridgeTarget): BridgeTransport
}

interface BridgeHealthChecker {
    suspend fun check(target: DirectBridgeTarget): BridgeHealth
}

@Serializable
data class BridgeHealth(
    val ok: Boolean,
    val protocolVersion: Int? = null,
    val pairingRequired: Boolean = true,
    val websocketPath: String? = null
)

class BridgeTransportUnavailableException(
    val transport: String
) : RuntimeException("Bridge transport $transport is not available")

object UnavailableBridgeTransportFactory : BridgeTransportFactory {
    override suspend fun open(target: DirectBridgeTarget): BridgeTransport {
        throw BridgeTransportUnavailableException("direct-websocket")
    }
}

sealed class BridgeConnectionStatus {
    data object NotConfigured : BridgeConnectionStatus()
    data object Connecting : BridgeConnectionStatus()
    data class Connected(
        val target: DirectBridgeTarget,
        val capabilities: BridgeCapabilities
    ) : BridgeConnectionStatus()

    data class Disconnected(val reason: String? = null) : BridgeConnectionStatus()
    data class Error(
        val message: String,
        val code: String? = null
    ) : BridgeConnectionStatus()
}

class BridgeConnectionManager(
    private val targetRepository: BridgeTargetRepository,
    private val transportFactory: BridgeTransportFactory,
    private val scope: CoroutineScope,
    private val healthChecker: BridgeHealthChecker? = null
) {
    private val mutex = Mutex()
    private var reconnectJob: Job? = null

    private val _status = MutableStateFlow<BridgeConnectionStatus>(BridgeConnectionStatus.Disconnected())
    val status: StateFlow<BridgeConnectionStatus> = _status.asStateFlow()

    private val _client = MutableStateFlow<MoccaBridgeClient?>(null)
    val client: StateFlow<MoccaBridgeClient?> = _client.asStateFlow()

    suspend fun connect(target: DirectBridgeTarget? = targetRepository.activeTarget.value) {
        reconnectJob?.cancel()
        mutex.withLock {
            val resolvedTarget = target ?: targetRepository.loadPersistedTarget()
            if (resolvedTarget == null) {
                closeActiveClient()
                _status.value = BridgeConnectionStatus.NotConfigured
                return
            }

            closeActiveClient()
            _status.value = BridgeConnectionStatus.Connecting

            val health = try {
                healthChecker?.check(resolvedTarget)
            } catch (error: Exception) {
                _status.value = BridgeConnectionStatus.Error(
                    message = error.message ?: "Bridge health check failed",
                    code = "bridge_health_failed"
                )
                return
            }
            if (health?.ok == false) {
                _status.value = BridgeConnectionStatus.Error(
                    message = "MOCCA CLI bridge health check failed",
                    code = "bridge_health_failed"
                )
                return
            }

            // Fail fast on protocol version mismatch instead of waiting for a 30s timeout.
            // The CLI health endpoint reports its protocol version, so we can detect
            // incompatibilities before opening the WebSocket connection.
            val serverVersion = health?.protocolVersion
            if (serverVersion != null && serverVersion != BRIDGE_PROTOCOL_VERSION) {
                _status.value = BridgeConnectionStatus.Error(
                    message = "Protocol version mismatch: app uses v$BRIDGE_PROTOCOL_VERSION " +
                        "but CLI reports v$serverVersion. " +
                        if (serverVersion < BRIDGE_PROTOCOL_VERSION)
                            "Please update your MOCCA CLI (npm update -g @mocca/cli)."
                        else
                            "Please update the MOCCA app.",
                    code = "protocol_version_mismatch"
                )
                return
            }

            val transport = try {
                transportFactory.open(resolvedTarget)
            } catch (error: Exception) {
                _status.value = BridgeConnectionStatus.Error(
                    message = error.message ?: "Unable to open bridge transport"
                )
                return
            }

            val nextClient = MoccaBridgeClient(
                transport = transport,
                scope = scope
            )

            try {
                val capabilities = OpenCodeBridgeRepository(nextClient).fetchCapabilities()
                targetRepository.save(resolvedTarget)
                _client.value = nextClient
                _status.value = BridgeConnectionStatus.Connected(
                    target = resolvedTarget,
                    capabilities = capabilities
                )
            } catch (error: BridgeResponseException) {
                nextClient.close()
                _client.value = null
                _status.value = BridgeConnectionStatus.Error(
                    message = error.message ?: "Bridge request failed",
                    code = error.code
                )
            } catch (error: Exception) {
                nextClient.close()
                _client.value = null
                _status.value = BridgeConnectionStatus.Error(
                    message = error.message ?: "Bridge connection failed"
                )
            }
        }
    }

    suspend fun connectFromPairingPayload(payload: String) {
        val target = try {
            BridgePairingPayloadParser.parse(payload)
        } catch (error: BridgePairingPayloadException) {
            _status.value = BridgeConnectionStatus.Error(
                message = error.message ?: "Invalid bridge pairing payload",
                code = "invalid_pairing_payload"
            )
            return
        }
        connect(target)
    }

    suspend fun disconnect() {
        mutex.withLock {
            closeActiveClient()
            _status.value = BridgeConnectionStatus.Disconnected()
            startReconnectLoop()
        }
    }

    /**
     * Start a reconnect loop with exponential backoff.
     * Launches a coroutine that retries [connect] up to 10 times with increasing delays.
     * Cancels any existing reconnect job before starting.
     */
    private fun startReconnectLoop(delayMs: Long = 1000) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var attempt = 0
            val maxAttempts = 10
            while (attempt < maxAttempts && status.value is BridgeConnectionStatus.Disconnected) {
                attempt++
                delay(delayMs * (1 shl min(attempt - 1, 5)))
                Napier.i("[BridgeConnectionManager] Reconnect attempt $attempt/$maxAttempts")
                connect()
            }
            if (status.value is BridgeConnectionStatus.Disconnected) {
                _status.value = BridgeConnectionStatus.Disconnected("Max reconnect attempts reached")
            }
        }
    }

    private suspend fun closeActiveClient() {
        _client.value?.close()
        _client.value = null
    }
}
