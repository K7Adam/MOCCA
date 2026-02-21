package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * MCP Server status as returned from GET /api/mcp.
 * Maps to OpenCode's McpStatus type.
 * 
 * The API returns simple status objects like {"status": "connected"}
 * Tools, resources, prompts are only available after connection in detail endpoints.
 */
@Serializable
@Immutable
data class McpServerStatus(
    val status: McpConnectionStatus,
    val error: String? = null,
    val tools: List<McpTool>? = null,
    val resources: List<McpResource>? = null,
    val prompts: List<McpPrompt>? = null
) {
    val isConnected: Boolean get() = status == McpConnectionStatus.CONNECTED
    val isTransitioning: Boolean get() = status == McpConnectionStatus.CONNECTING || status == McpConnectionStatus.DISCONNECTING
    val needsAuth: Boolean get() = status == McpConnectionStatus.NEEDS_AUTH || status == McpConnectionStatus.NEEDS_CLIENT_REGISTRATION
    val hasFailed: Boolean get() = status == McpConnectionStatus.FAILED
    val isDisabled: Boolean get() = status == McpConnectionStatus.DISABLED
    val isEnabled: Boolean get() = status != McpConnectionStatus.DISABLED
}

/**
 * MCP connection status enum matching OpenCode's status values.
 */
@Serializable
enum class McpConnectionStatus {
    @SerialName("connected")
    CONNECTED,
    @SerialName("failed")
    FAILED,
    @SerialName("needs_auth")
    NEEDS_AUTH,
    @SerialName("needs_client_registration")
    NEEDS_CLIENT_REGISTRATION,
    @SerialName("disconnected")
    DISCONNECTED,
    @SerialName("connecting")
    CONNECTING,
    @SerialName("disconnecting")
    DISCONNECTING,
    @SerialName("disabled")
    DISABLED;
    
    companion object {
        fun fromString(value: String): McpConnectionStatus {
            return when (value.lowercase()) {
                "connected" -> CONNECTED
                "failed" -> FAILED
                "needs_auth" -> NEEDS_AUTH
                "needs_client_registration" -> NEEDS_CLIENT_REGISTRATION
                "disconnected" -> DISCONNECTED
                "connecting" -> CONNECTING
                "disconnecting" -> DISCONNECTING
                "disabled" -> DISABLED
                else -> DISCONNECTED
            }
        }
    }
}

/**
 * MCP Tool definition from server.
 */
@Serializable
@Immutable
data class McpTool(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonElement? = null
)

/**
 * MCP Resource definition from server.
 */
@Serializable
@Immutable
data class McpResource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null
)

/**
 * MCP Prompt definition from server.
 */
@Serializable
@Immutable
data class McpPrompt(
    val name: String,
    val description: String? = null,
    val arguments: List<McpPromptArgument>? = null
)

/**
 * MCP Prompt argument.
 */
@Serializable
@Immutable
data class McpPromptArgument(
    val name: String,
    val description: String? = null,
    val required: Boolean = false
)

/**
 * MCP Server configuration as stored in opencode.json.
 */
@Serializable
@Immutable
data class McpServerConfig(
    val type: McpServerType = McpServerType.LOCAL,
    val command: List<String>? = null,
    val url: String? = null,
    val environment: Map<String, String>? = null,
    val enabled: Boolean = true
)

/**
 * MCP Server type enum.
 */
@Serializable
enum class McpServerType {
    @SerialName("local")
    LOCAL,
    @SerialName("remote")
    REMOTE,
    @SerialName("stdio")
    STDIO
}

/**
 * Request to connect/disconnect MCP server.
 */
@Serializable
@Immutable
data class McpConnectRequest(
    val name: String,
    val directory: String? = null
)

/**
 * Request to configure/update MCP server.
 */
@Serializable
@Immutable
data class McpConfigureRequest(
    val name: String,
    val config: McpServerConfig
)

/**
 * Combined MCP server info for UI display.
 * Includes both config and runtime status.
 */
@Immutable
data class McpServerInfo(
    val name: String,
    val status: McpServerStatus,
    val config: McpServerConfig? = null
) {
    val displayType: String
        get() = when {
            config?.type == McpServerType.REMOTE -> "REMOTE"
            config?.type == McpServerType.LOCAL -> "LOCAL_STDIO"
            config?.type == McpServerType.STDIO -> "STDIO"
            else -> "UNKNOWN"
        }
    
    val isEnabled: Boolean
        get() = config?.enabled ?: true
    
    val isConnected: Boolean
        get() = status.isConnected
    
    val isTransitioning: Boolean
        get() = status.isTransitioning
    
    val toolCount: Int
        get() = status.tools?.size ?: 0
    
    val resourceCount: Int
        get() = status.resources?.size ?: 0
    
    val promptCount: Int
        get() = status.prompts?.size ?: 0
}

/**
 * MCP state for the entire app - maps server names to their status.
 */
typealias McpStatusMap = Map<String, McpServerStatus>
