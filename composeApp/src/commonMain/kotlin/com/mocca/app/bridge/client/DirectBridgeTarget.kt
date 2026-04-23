package com.mocca.app.bridge.client

import kotlinx.serialization.Serializable

const val DIRECT_BRIDGE_WEBSOCKET_PATH = "/v1/ws"
const val DIRECT_BRIDGE_HEALTH_PATH = "/v1/health"

@Serializable
enum class DirectBridgeNetwork {
    LAN,
    TAILSCALE
}

@Serializable
data class DirectBridgeTarget(
    val host: String,
    val port: Int,
    val pairingCode: String,
    val useTls: Boolean = false,
    val network: DirectBridgeNetwork? = null
) {
    init {
        require(host.isNotBlank()) { "Bridge host must not be blank" }
        require(port in 1..65535) { "Bridge port must be between 1 and 65535" }
        require(pairingCode.isNotBlank()) { "Bridge pairing code must not be blank" }
    }

    val websocketUrl: String
        get() {
            val scheme = if (useTls) "wss" else "ws"
            return "$scheme://${host.trim().asWebsocketHost()}:$port$DIRECT_BRIDGE_WEBSOCKET_PATH" +
                "?pairingCode=${pairingCode.percentEncodeQueryValue()}"
        }

    val healthUrl: String
        get() {
            val scheme = if (useTls) "https" else "http"
            return "$scheme://${host.trim().asWebsocketHost()}:$port$DIRECT_BRIDGE_HEALTH_PATH"
        }
}

private fun String.asWebsocketHost(): String {
    return if (contains(":") && !startsWith("[") && !endsWith("]")) {
        "[$this]"
    } else {
        this
    }
}

private fun String.percentEncodeQueryValue(): String {
    val bytes = encodeToByteArray()
    return buildString {
        for (byte in bytes) {
            val value = byte.toInt() and 0xff
            if (value.isQueryUnreserved()) {
                append(value.toChar())
            } else {
                append('%')
                append(QUERY_HEX[value shr 4])
                append(QUERY_HEX[value and 0x0f])
            }
        }
    }
}

private fun Int.isQueryUnreserved(): Boolean {
    return this in 'A'.code..'Z'.code ||
        this in 'a'.code..'z'.code ||
        this in '0'.code..'9'.code ||
        this == '-'.code ||
        this == '.'.code ||
        this == '_'.code ||
        this == '~'.code
}

private const val QUERY_HEX = "0123456789ABCDEF"
