package com.mocca.app.bridge.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

const val BRIDGE_PROTOCOL_VERSION: Int = 1

@Serializable
data class BridgeRequest(
    val v: Int = BRIDGE_PROTOCOL_VERSION,
    val id: String,
    val ns: String,
    val action: String,
    val payload: JsonElement? = null
)

@Serializable
data class BridgeResponse(
    val v: Int = BRIDGE_PROTOCOL_VERSION,
    val id: String,
    val ns: String,
    val action: String,
    val ok: Boolean,
    val payload: JsonElement? = null,
    val error: BridgeError? = null
)

@Serializable
data class BridgeEvent(
    val v: Int = BRIDGE_PROTOCOL_VERSION,
    val ns: String,
    val event: String,
    val seq: Long? = null,
    val payload: JsonElement? = null
)

@Serializable
data class BridgeError(
    val code: String,
    val message: String,
    val details: JsonElement? = null
)

@Serializable
data class BridgeCapabilities(
    val protocolVersion: Int,
    val namespaces: List<String>,
    val ai: BridgeAiCapabilities = BridgeAiCapabilities()
)

@Serializable
data class BridgeAiCapabilities(
    val opencodeConfigSnapshot: Boolean = false,
    val opencodeRuntime: Boolean = false,
    val sessions: Boolean = false,
    val messages: Boolean = false
)

@Serializable
data class OpenCodeRuntimeEnsureResponse(
    val status: String,
    val server: OpenCodeRuntimeServerConnection
)

@Serializable
data class OpenCodeRuntimeServerConnection(
    val baseUrl: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val useHttps: Boolean = false
)

@Serializable
data class OpenCodeConfigSnapshot(
    val installed: OpenCodeInstallInfo,
    val configFiles: List<OpenCodeConfigFile> = emptyList(),
    val effective: OpenCodeEffectiveConfig = OpenCodeEffectiveConfig(),
    val credentials: List<OpenCodeCredentialInfo> = emptyList(),
    val agents: List<OpenCodeAgentInfo> = emptyList(),
    val commands: List<OpenCodeCommandInfo> = emptyList(),
    val mcpServers: List<OpenCodeMcpServerInfo> = emptyList()
)

@Serializable
data class OpenCodeInstallInfo(
    val available: Boolean,
    val command: String,
    val version: String? = null,
    val error: String? = null
)

@Serializable
data class OpenCodeConfigFile(
    val scope: String,
    val path: String,
    val config: JsonElement? = null
)

@Serializable
data class OpenCodeEffectiveConfig(
    val model: String? = null,
    val plugins: List<String> = emptyList(),
    val tools: Map<String, Boolean> = emptyMap(),
    val raw: JsonElement? = null
)

@Serializable
data class OpenCodeCredentialInfo(
    val name: String,
    val type: String
)

@Serializable
data class OpenCodeAgentInfo(
    val name: String,
    val primary: Boolean = false
)

@Serializable
data class OpenCodeCommandInfo(
    val name: String,
    val description: String? = null
)

@Serializable
data class OpenCodeMcpServerInfo(
    val name: String,
    val type: String? = null,
    val enabled: Boolean? = null
)
