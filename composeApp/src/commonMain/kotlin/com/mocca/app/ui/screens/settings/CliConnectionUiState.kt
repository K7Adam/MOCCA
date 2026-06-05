package com.mocca.app.ui.screens.settings

import androidx.compose.runtime.Immutable
import com.mocca.app.bridge.client.DirectBridgeTarget
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.domain.model.AiConfigState

@Immutable
data class CliConnectionUiState(
    val headline: String,
    val statusLabel: String,
    val supportingText: String,
    val endpointLabel: String? = null,
    val networkLabel: String? = null,
    val projectLabel: String? = null,
    val capabilitySummary: String = "",
    val canReconnect: Boolean = false,
    val canForget: Boolean = false
)

fun buildCliConnectionUiState(
    target: DirectBridgeTarget?,
    bridgeStatus: BridgeConnectionStatus,
    aiConfigState: AiConfigState
): CliConnectionUiState {
    if (target == null) {
        return CliConnectionUiState(
            headline = "MOCCA CLI",
            statusLabel = "Not configured",
            supportingText = "Pair with mocca-cli to unlock native AI, files, git and terminal."
        )
    }

    val endpoint = "${target.host}:${target.port}"
    val network = target.network?.name
    return when (bridgeStatus) {
        is BridgeConnectionStatus.Connected -> {
            val capabilityLabels = buildList {
                if (bridgeStatus.capabilities.ai.configNormalized || bridgeStatus.capabilities.ai.messages) add("AI")
                if (bridgeStatus.capabilities.fs.native) add("Files")
                if (bridgeStatus.capabilities.git.native) add("Git")
                if (bridgeStatus.capabilities.terminal.ptyGrid) add("Terminal")
                if (bridgeStatus.capabilities.process.native) add("Processes")
                if (bridgeStatus.capabilities.monitor.native) add("Monitor")
            }
            CliConnectionUiState(
                headline = "MOCCA CLI connected",
                statusLabel = "Connected",
                supportingText = "Native CLI bridge is active for workspace operations.",
                endpointLabel = endpoint,
                networkLabel = network,
                projectLabel = aiConfigState.snapshot?.projectDir?.ifBlank { null },
                capabilitySummary = capabilityLabels.joinToString(", "),
                canReconnect = true,
                canForget = true
            )
        }

        is BridgeConnectionStatus.Connecting -> CliConnectionUiState(
            headline = "MOCCA CLI",
            statusLabel = "Connecting",
            supportingText = "Checking bridge health and opening the native CLI transport...",
            endpointLabel = endpoint,
            networkLabel = network,
            canForget = true
        )

        is BridgeConnectionStatus.Error -> CliConnectionUiState(
            headline = "MOCCA CLI",
            statusLabel = "Connection failed",
            supportingText = bridgeStatus.message,
            endpointLabel = endpoint,
            networkLabel = network,
            canReconnect = true,
            canForget = true
        )

        is BridgeConnectionStatus.Disconnected -> CliConnectionUiState(
            headline = "MOCCA CLI",
            statusLabel = "Disconnected",
            supportingText = "The paired target is saved, but the CLI bridge is currently offline.",
            endpointLabel = endpoint,
            networkLabel = network,
            canReconnect = true,
            canForget = true
        )

        BridgeConnectionStatus.NotConfigured -> CliConnectionUiState(
            headline = "MOCCA CLI",
            statusLabel = "Not configured",
            supportingText = "Pair with mocca-cli to unlock native AI, files, git and terminal.",
            endpointLabel = endpoint,
            networkLabel = network,
            canForget = true
        )
    }
}
