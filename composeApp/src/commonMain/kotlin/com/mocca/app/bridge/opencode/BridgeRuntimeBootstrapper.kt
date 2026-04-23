package com.mocca.app.bridge.opencode

import com.mocca.app.bridge.client.DirectBridgeTarget
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.data.repository.ConnectionManager
import com.mocca.app.data.repository.ServerConfigRepository
import com.mocca.app.domain.model.ConnectionStatus
import com.mocca.app.domain.model.ServerConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BridgeRuntimeBootstrapper(
    private val bridgeConnectionManager: BridgeConnectionManager,
    private val serverConfigRepository: ServerConfigRepository,
    private val connectionManager: ConnectionManager
) {
    private val mutex = Mutex()

    suspend fun ensureRuntimeServer(target: DirectBridgeTarget): ServerConfig = mutex.withLock {
        val bridgeStatus = ensureBridgeConnected(target)
        val activeConfig = connectionManager.activeConfig.value
        if (activeConfig?.id?.startsWith(CLI_SERVER_ID_PREFIX) == true) {
            when (connectionManager.status.value) {
                is ConnectionStatus.Connected -> return@withLock activeConfig
                is ConnectionStatus.Connecting,
                is ConnectionStatus.Reconnecting -> {
                    runCatching {
                        waitForOpenCodeConnection()
                        return@withLock connectionManager.activeConfig.value ?: activeConfig
                    }.onFailure { error ->
                        Napier.w(
                            "[BridgeRuntimeBootstrapper] Existing CLI runtime connection did not settle; retrying via bridge: ${error.message}"
                        )
                    }
                }
                else -> Unit
            }
        }

        val client = bridgeConnectionManager.client.value
            ?: error("MOCCA CLI bridge connected without an active client")
        val repository = OpenCodeBridgeRepository(client)
        if (!bridgeStatus.capabilities.ai.opencodeRuntime) {
            throw BridgeFeatureUnavailableException("ai.runtime.ensure")
        }

        val runtime = repository.ensureOpenCodeRuntime()
        val server = runtime.server
        val config = ServerConfig(
            id = "mocca-cli-${server.host}-${server.port}",
            name = "MOCCA CLI (${server.host})",
            host = server.host,
            port = server.port,
            username = server.username,
            password = server.password,
            isActive = true,
            useHttps = server.useHttps
        )

        val latestConfig = connectionManager.activeConfig.value
        if (latestConfig != null && latestConfig.isSameRuntimeEndpointAs(config)) {
            when (connectionManager.status.value) {
                is ConnectionStatus.Connected -> return@withLock latestConfig
                is ConnectionStatus.Connecting,
                is ConnectionStatus.Reconnecting -> {
                    waitForOpenCodeConnection()
                    return@withLock connectionManager.activeConfig.value ?: latestConfig
                }
                else -> Unit
            }
        }

        serverConfigRepository.saveServer(config)
        connectionManager.connect(config)
        waitForOpenCodeConnection()
        Napier.i("[BridgeRuntimeBootstrapper] OpenCode runtime ready at ${server.baseUrl}")
        config
    }

    private suspend fun ensureBridgeConnected(target: DirectBridgeTarget): BridgeConnectionStatus.Connected {
        val currentStatus = bridgeConnectionManager.status.value
        if (
            currentStatus is BridgeConnectionStatus.Connected &&
            currentStatus.target == target &&
            bridgeConnectionManager.client.value != null
        ) {
            return currentStatus
        }

        bridgeConnectionManager.connect(target)
        val bridgeStatus = bridgeConnectionManager.status.value
        if (bridgeStatus is BridgeConnectionStatus.Connected) {
            return bridgeStatus
        }

        val message = when (bridgeStatus) {
            is BridgeConnectionStatus.Error -> bridgeStatus.message
            BridgeConnectionStatus.NotConfigured -> "MOCCA CLI bridge target is missing"
            else -> "MOCCA CLI bridge did not connect"
        }
        error(message)
    }

    private suspend fun waitForOpenCodeConnection() {
        repeat(MAX_CONNECTION_WAIT_ATTEMPTS) { attempt ->
            delay(CONNECTION_WAIT_INTERVAL_MS)
            when (val status = connectionManager.status.value) {
                is ConnectionStatus.Connected -> return
                is ConnectionStatus.Error -> error(status.message)
                is ConnectionStatus.Disconnected -> error(
                    status.reason ?: "OpenCode runtime disconnected"
                )
                else -> Napier.d(
                    "[BridgeRuntimeBootstrapper] Waiting for OpenCode runtime ($attempt/$MAX_CONNECTION_WAIT_ATTEMPTS)"
                )
            }
        }
        error("MOCCA CLI started OpenCode, but MOCCA could not reach the runtime server")
    }

    private companion object {
        const val CLI_SERVER_ID_PREFIX = "mocca-cli-"
        const val MAX_CONNECTION_WAIT_ATTEMPTS = 24
        const val CONNECTION_WAIT_INTERVAL_MS = 250L
    }
}

private fun ServerConfig.isSameRuntimeEndpointAs(other: ServerConfig): Boolean {
    return host == other.host &&
        port == other.port &&
        username == other.username &&
        password == other.password &&
        useHttps == other.useHttps
}
