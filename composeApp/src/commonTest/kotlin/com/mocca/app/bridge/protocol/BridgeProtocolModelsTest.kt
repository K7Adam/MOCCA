package com.mocca.app.bridge.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class BridgeProtocolModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun requestResponseAndEventFramesRoundTrip() {
        val request = json.decodeFromString<BridgeRequest>(
            """
            {
              "v": 1,
              "id": "req-1",
              "ns": "system",
              "action": "capabilities",
              "payload": { "includeExperimental": true }
            }
            """.trimIndent()
        )

        assertEquals(1, request.v)
        assertEquals("req-1", request.id)
        assertEquals("system", request.ns)
        assertEquals("capabilities", request.action)
        assertTrue(request.payload!!.jsonObject["includeExperimental"]!!.jsonPrimitive.boolean)

        val response = BridgeResponse(
            id = request.id,
            ns = request.ns,
            action = request.action,
            ok = true,
            payload = JsonObject(emptyMap())
        )

        val encodedResponse = json.decodeFromString<BridgeResponse>(json.encodeToString(response))
        assertEquals(2, encodedResponse.v)
        assertEquals("req-1", encodedResponse.id)
        assertTrue(encodedResponse.ok)

        val event = json.decodeFromString<BridgeEvent>(
            """
            {
              "v": 1,
              "ns": "ai",
              "event": "config.snapshot",
              "seq": 7,
              "payload": { "model": "openai/gpt-5" }
            }
            """.trimIndent()
        )

        assertEquals("ai", event.ns)
        assertEquals("config.snapshot", event.event)
        assertEquals(7, event.seq)
        assertEquals("openai/gpt-5", event.payload!!.jsonObject["model"]!!.jsonPrimitive.content)
    }

    @Test
    fun capabilitiesDecodeCliRouterPayload() {
        val capabilities = json.decodeFromString<BridgeCapabilities>(
            """
            {
              "protocolVersion": 2,
              "namespaces": ["system", "ai", "git"],
              "ai": {
                "opencodeConfigSnapshot": true,
                "opencodeRuntime": false,
                "sessions": false,
                "messages": false,
                "events": true,
                "eventReplay": true,
                "permissions": true,
                "questions": true,
                "sessionStatus": true,
                "usage": true
              }
            }
            """.trimIndent()
        )

        assertEquals(2, capabilities.protocolVersion)
        assertEquals(listOf("system", "ai", "git"), capabilities.namespaces)
        assertTrue(capabilities.ai.opencodeConfigSnapshot)
        assertFalse(capabilities.ai.opencodeRuntime)
        assertFalse(capabilities.ai.sessions)
        assertFalse(capabilities.ai.messages)
        assertTrue(capabilities.ai.events)
        assertTrue(capabilities.ai.eventReplay)
        assertTrue(capabilities.ai.permissions)
        assertTrue(capabilities.ai.questions)
        assertTrue(capabilities.ai.sessionStatus)
        assertTrue(capabilities.ai.usage)
    }

    @Test
    fun errorResponseCarriesStableCodeAndMessage() {
        val response = json.decodeFromString<BridgeResponse>(
            """
            {
              "v": 1,
              "id": "req-missing",
              "ns": "git",
              "action": "status",
              "ok": false,
              "error": {
                "code": "not_found",
                "message": "No handler registered for git.status"
              }
            }
            """.trimIndent()
        )

        assertFalse(response.ok)
        val error = response.error ?: fail("Expected bridge error")
        assertEquals("not_found", error.code)
        assertEquals("No handler registered for git.status", error.message)
    }
}
