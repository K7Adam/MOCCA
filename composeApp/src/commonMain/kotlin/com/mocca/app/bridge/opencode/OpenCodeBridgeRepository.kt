package com.mocca.app.bridge.opencode

import com.mocca.app.bridge.client.MoccaBridgeClient
import com.mocca.app.bridge.client.toBridgePayload
import com.mocca.app.bridge.protocol.BridgeCapabilities
import com.mocca.app.bridge.protocol.BridgeResponse
import com.mocca.app.bridge.protocol.OpenCodeAgentInfo
import com.mocca.app.bridge.protocol.OpenCodeCommandInfo
import com.mocca.app.bridge.protocol.OpenCodeConfigSnapshot
import com.mocca.app.bridge.protocol.OpenCodeCredentialInfo
import com.mocca.app.bridge.protocol.OpenCodeMcpServerInfo
import com.mocca.app.bridge.protocol.OpenCodeRuntimeEnsureResponse
import com.mocca.app.domain.model.AiBridgeMessageRequest
import com.mocca.app.domain.model.AiRuntimeConfigSnapshot
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

class BridgeResponseException(
    val ns: String,
    val action: String,
    val code: String,
    message: String,
    val details: JsonElement? = null
) : RuntimeException("Bridge request $ns.$action failed with $code: $message")

class BridgePayloadDecodeException(
    val ns: String,
    val action: String,
    message: String
) : RuntimeException("Bridge response $ns.$action payload could not be decoded: $message")

class BridgeFeatureUnavailableException(
    val feature: String
) : RuntimeException("Bridge feature $feature is not available")

class OpenCodeBridgeRepository(
    private val client: MoccaBridgeClient,
    private val json: Json = MoccaBridgeClient.DefaultBridgeJson
) {
    private companion object {
        const val RUNTIME_ENSURE_TIMEOUT_MILLIS = 90_000L
    }

    private var cachedCapabilities: BridgeCapabilities? = null

    suspend fun fetchCapabilities(forceRefresh: Boolean = false): BridgeCapabilities {
        if (!forceRefresh) {
            cachedCapabilities?.let { return it }
        }

        return client.request(ns = "system", action = "capabilities")
            .decodePayloadOrThrow<BridgeCapabilities>()
            .also { cachedCapabilities = it }
    }

    suspend fun fetchOpenCodeConfigSnapshot(): OpenCodeConfigSnapshot {
        val capabilities = fetchCapabilities()
        if (!capabilities.ai.opencodeConfigSnapshot) {
            throw BridgeFeatureUnavailableException("ai.config.snapshot")
        }

        return client.request(ns = "ai", action = "config.snapshot")
            .decodePayloadOrThrow()
    }

    suspend fun fetchAiRuntimeConfig(forceRefresh: Boolean = false): AiRuntimeConfigSnapshot {
        val capabilities = fetchCapabilities()
        if (!capabilities.ai.configNormalized) {
            throw BridgeFeatureUnavailableException("ai.config.get")
        }

        return client.request(
            ns = "ai",
            action = if (forceRefresh) "config.refresh" else "config.get"
        ).decodePayloadOrThrow()
    }

    suspend fun ensureOpenCodeRuntime(): OpenCodeRuntimeEnsureResponse {
        val capabilities = fetchCapabilities()
        if (!capabilities.ai.opencodeRuntime) {
            throw BridgeFeatureUnavailableException("ai.runtime.ensure")
        }

        return client.request(
            ns = "ai",
            action = "runtime.ensure",
            timeoutMillis = RUNTIME_ENSURE_TIMEOUT_MILLIS
        )
            .decodePayloadOrThrow()
    }

    suspend fun fetchCredentials(): List<OpenCodeCredentialInfo> {
        return client.request(ns = "providers", action = "credentials.list")
            .decodePayloadOrThrow()
    }

    suspend fun fetchAgents(): List<OpenCodeAgentInfo> {
        return client.request(ns = "ai", action = "agents.list")
            .decodePayloadOrThrow()
    }

    suspend fun fetchCommands(): List<OpenCodeCommandInfo> {
        return client.request(ns = "commands", action = "list")
            .decodePayloadOrThrow()
    }

    suspend fun fetchMcpServers(): List<OpenCodeMcpServerInfo> {
        return client.request(ns = "mcp", action = "servers.list")
            .decodePayloadOrThrow()
    }

    suspend fun sendMessage(request: AiBridgeMessageRequest): Unit {
        val capabilities = fetchCapabilities()
        if (!capabilities.ai.messages) {
            throw BridgeFeatureUnavailableException("ai.messages.send")
        }

        client.request(
            ns = "ai",
            action = "messages.send",
            payload = json.toBridgePayload(request)
        ).decodePayloadOrThrow<JsonElement>()
    }

    private inline fun <reified T> BridgeResponse.decodePayloadOrThrow(): T {
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
}
