package com.mocca.app.bridge.connection

import com.mocca.app.bridge.client.DirectBridgeTarget

class BridgePairingPayloadException(
    message: String
) : RuntimeException(message)

object BridgePairingPayloadParser {
    fun parse(payload: String): DirectBridgeTarget {
        val value = payload.trim()
        if (value.startsWith("mocca://bridge/connect?", ignoreCase = true)) {
            return parseMoccaDeepLink(value)
        }
        if (value.startsWith("ws://", ignoreCase = true) || value.startsWith("wss://", ignoreCase = true)) {
            return parseWebsocketUrl(value)
        }
        throw BridgePairingPayloadException("Unsupported MOCCA bridge pairing payload")
    }

    private fun parseMoccaDeepLink(value: String): DirectBridgeTarget {
        val query = value.substringAfter("?", missingDelimiterValue = "")
        val params = parseQuery(query)
        return DirectBridgeTarget(
            host = params.required("host"),
            port = params.requiredPort("port"),
            pairingCode = params.required("pairingCode"),
            useTls = params["tls"] == "1" || params["tls"].equals("true", ignoreCase = true)
        )
    }

    private fun parseWebsocketUrl(value: String): DirectBridgeTarget {
        val useTls = value.startsWith("wss://", ignoreCase = true)
        val withoutScheme = value.substringAfter("://")
        val authority = withoutScheme.substringBefore("/")
        val query = withoutScheme.substringAfter("?", missingDelimiterValue = "")
        val params = parseQuery(query)
        val host = authority.substringBefore(":")
        val port = authority.substringAfter(":", missingDelimiterValue = if (useTls) "443" else "80")

        if (!withoutScheme.substringAfter("/", missingDelimiterValue = "").startsWith("v1/ws")) {
            throw BridgePairingPayloadException("WebSocket URL is not a MOCCA bridge endpoint")
        }

        return DirectBridgeTarget(
            host = decodeQueryValue(host),
            port = port.toPort("port"),
            pairingCode = params.required("pairingCode"),
            useTls = useTls
        )
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split("&")
            .filter { it.isNotBlank() }
            .associate { part ->
                val key = part.substringBefore("=")
                val value = part.substringAfter("=", missingDelimiterValue = "")
                decodeQueryValue(key) to decodeQueryValue(value)
            }
    }

    private fun Map<String, String>.required(key: String): String {
        val value = this[key]
        if (value.isNullOrBlank()) {
            throw BridgePairingPayloadException("Pairing payload is missing $key")
        }
        return value
    }

    private fun Map<String, String>.requiredPort(key: String): Int {
        return required(key).toPort(key)
    }

    private fun String.toPort(key: String): Int {
        val port = toIntOrNull()
        if (port == null || port !in 1..65535) {
            throw BridgePairingPayloadException("Pairing payload has invalid $key")
        }
        return port
    }

    private fun decodeQueryValue(value: String): String {
        val bytes = mutableListOf<Byte>()
        var index = 0
        while (index < value.length) {
            when (val char = value[index]) {
                '+' -> {
                    bytes += ' '.code.toByte()
                    index += 1
                }
                '%' -> {
                    if (index + 2 >= value.length) {
                        throw BridgePairingPayloadException("Pairing payload contains invalid percent encoding")
                    }
                    val byte = value.substring(index + 1, index + 3).toIntOrNull(16)
                        ?: throw BridgePairingPayloadException("Pairing payload contains invalid percent encoding")
                    bytes += byte.toByte()
                    index += 3
                }
                else -> {
                    bytes += char.code.toByte()
                    index += 1
                }
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}
