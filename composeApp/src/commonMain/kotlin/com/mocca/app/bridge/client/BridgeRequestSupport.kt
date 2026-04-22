package com.mocca.app.bridge.client

import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.opencode.BridgePayloadDecodeException
import com.mocca.app.bridge.opencode.BridgeResponseException
import com.mocca.app.bridge.protocol.BridgeResponse
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class NativeCliUnavailableException(
    val feature: String
) : RuntimeException("MOCCA CLI bridge is required for $feature")

suspend fun BridgeConnectionManager.requireClient(feature: String): MoccaBridgeClient {
    return client.value ?: throw NativeCliUnavailableException(feature)
}

suspend inline fun <reified T> MoccaBridgeClient.requestPayload(
    ns: String,
    action: String,
    payload: JsonElement? = null,
    json: Json = MoccaBridgeClient.DefaultBridgeJson
): T {
    return request(ns = ns, action = action, payload = payload).decodePayloadOrThrow(json)
}

inline fun <reified T> Json.toBridgePayload(value: T): JsonElement = encodeToJsonElement(value)

inline fun <reified T> BridgeResponse.decodePayloadOrThrow(json: Json = MoccaBridgeClient.DefaultBridgeJson): T {
    if (!ok) {
        throw BridgeResponseException(
            ns = ns,
            action = action,
            code = error?.code ?: "bridge_error",
            message = error?.message ?: "Bridge request failed",
            details = error?.details
        )
    }

    val payload = payload ?: throw BridgePayloadDecodeException(
        ns = ns,
        action = action,
        message = "Missing payload"
    )

    return try {
        json.decodeFromJsonElement(payload)
    } catch (error: SerializationException) {
        throw BridgePayloadDecodeException(
            ns = ns,
            action = action,
            message = error.message ?: error::class.simpleName.orEmpty()
        )
    } catch (error: IllegalArgumentException) {
        throw BridgePayloadDecodeException(
            ns = ns,
            action = action,
            message = error.message ?: error::class.simpleName.orEmpty()
        )
    }
}
