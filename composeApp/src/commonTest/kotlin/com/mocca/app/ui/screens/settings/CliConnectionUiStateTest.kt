package com.mocca.app.ui.screens.settings

import com.mocca.app.bridge.client.DirectBridgeNetwork
import com.mocca.app.bridge.client.DirectBridgeTarget
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.protocol.BridgeAiCapabilities
import com.mocca.app.bridge.protocol.BridgeCapabilities
import com.mocca.app.bridge.protocol.BridgeFsCapabilities
import com.mocca.app.bridge.protocol.BridgeGitCapabilities
import com.mocca.app.bridge.protocol.BridgeMonitorCapabilities
import com.mocca.app.bridge.protocol.BridgeProcessCapabilities
import com.mocca.app.bridge.protocol.BridgeTerminalCapabilities
import com.mocca.app.domain.model.AiAgentOption
import com.mocca.app.domain.model.AiConfigFingerprint
import com.mocca.app.domain.model.AiConfigState
import com.mocca.app.domain.model.AiConfigStatus
import com.mocca.app.domain.model.AiRuntimeConfigSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliConnectionUiStateTest {
    @Test
    fun connectedBridgeBuildsCapabilitySummaryFromCliAndAiConfig() {
        val state = buildCliConnectionUiState(
            target = DirectBridgeTarget(
                host = "192.168.0.10",
                port = 17653,
                pairingCode = "123456",
                network = DirectBridgeNetwork.LAN
            ),
            bridgeStatus = BridgeConnectionStatus.Connected(
                target = DirectBridgeTarget(
                    host = "192.168.0.10",
                    port = 17653,
                    pairingCode = "123456",
                    network = DirectBridgeNetwork.LAN
                ),
                capabilities = BridgeCapabilities(
                    protocolVersion = 1,
                    namespaces = listOf("system", "ai", "fs", "git", "terminal", "process", "monitor"),
                    ai = BridgeAiCapabilities(configNormalized = true, messages = true),
                    fs = BridgeFsCapabilities(native = true),
                    git = BridgeGitCapabilities(native = true),
                    terminal = BridgeTerminalCapabilities(ptyGrid = true),
                    process = BridgeProcessCapabilities(native = true),
                    monitor = BridgeMonitorCapabilities(native = true)
                )
            ),
            aiConfigState = AiConfigState(
                status = AiConfigStatus.READY,
                snapshot = AiRuntimeConfigSnapshot(
                    fingerprint = AiConfigFingerprint("abc"),
                    projectDir = "C:/repo",
                    source = "mocca-cli",
                    providers = emptyList(),
                    agents = listOf(AiAgentOption(id = "build", name = "Build"))
                )
            )
        )

        assertEquals("MOCCA CLI connected", state.headline)
        assertEquals("Connected", state.statusLabel)
        assertEquals("192.168.0.10:17653", state.endpointLabel)
        assertEquals("LAN", state.networkLabel)
        assertTrue(state.capabilitySummary.contains("AI"))
        assertTrue(state.capabilitySummary.contains("Files"))
        assertTrue(state.capabilitySummary.contains("Git"))
        assertEquals("C:/repo", state.projectLabel)
    }

    @Test
    fun missingTargetShowsPairingPrompt() {
        val state = buildCliConnectionUiState(
            target = null,
            bridgeStatus = BridgeConnectionStatus.NotConfigured,
            aiConfigState = AiConfigState()
        )

        assertEquals("MOCCA CLI", state.headline)
        assertEquals("Not configured", state.statusLabel)
        assertEquals("Pair with mocca-cli to unlock native AI, files, git and terminal.", state.supportingText)
        assertTrue(state.canForget.not())
        assertTrue(state.canReconnect.not())
    }
}
