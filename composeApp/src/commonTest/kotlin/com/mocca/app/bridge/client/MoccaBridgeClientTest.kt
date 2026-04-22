package com.mocca.app.bridge.client

import com.mocca.app.bridge.protocol.BridgeEvent
import com.mocca.app.bridge.protocol.BridgeRequest
import com.mocca.app.bridge.protocol.BridgeResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MoccaBridgeClientTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun requestSendsFrameAndAwaitsMatchingResponse() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(
            transport = transport,
            scope = backgroundScope
        )

        val requestJob = async {
            client.request(
                ns = "system",
                action = "capabilities",
                payload = buildJsonObject { put("includeExperimental", true) }
            )
        }

        val sentRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        assertEquals("system", sentRequest.ns)
        assertEquals("capabilities", sentRequest.action)
        assertTrue(sentRequest.payload!!.jsonObject["includeExperimental"]!!.jsonPrimitive.content.toBoolean())

        transport.emitIncoming(
            json.encodeToString(
                BridgeResponse(
                    id = sentRequest.id,
                    ns = "system",
                    action = "capabilities",
                    ok = true,
                    payload = buildJsonObject { put("protocolVersion", 1) }
                )
            )
        )

        val response = requestJob.await()
        assertTrue(response.ok)
        assertEquals("1", response.payload!!.jsonObject["protocolVersion"]!!.jsonPrimitive.content)

        client.close()
    }

    @Test
    fun eventsAreExposedSeparatelyFromResponses() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(
            transport = transport,
            scope = backgroundScope
        )
        val eventsJob = async { client.events.take(1).toList() }
        runCurrent()

        transport.emitIncoming(
            json.encodeToString(
                BridgeEvent(
                    ns = "ai",
                    event = "config.snapshot",
                    seq = 4,
                    payload = JsonObject(emptyMap())
                )
            )
        )
        runCurrent()

        val events = eventsJob.await()
        assertEquals("ai", events.single().ns)
        assertEquals("config.snapshot", events.single().event)
        assertEquals(4, events.single().seq)

        client.close()
    }

    @Test
    fun errorResponsesCompleteMatchingRequest() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(
            transport = transport,
            scope = backgroundScope
        )

        val requestJob = async { client.request(ns = "git", action = "status") }
        val sentRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())

        transport.emitIncoming(
            json.encodeToString(
                BridgeResponse(
                    id = sentRequest.id,
                    ns = sentRequest.ns,
                    action = sentRequest.action,
                    ok = false,
                    error = com.mocca.app.bridge.protocol.BridgeError(
                        code = "not_found",
                        message = "No handler registered for git.status"
                    )
                )
            )
        )

        val response = requestJob.await()
        assertEquals(false, response.ok)
        assertEquals("not_found", response.error!!.code)

        client.close()
    }

    @Test
    fun requestTimesOutWhenNoMatchingResponseArrives() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(
            transport = transport,
            scope = backgroundScope,
            requestTimeoutMillis = 1_000
        )

        val requestJob = async {
            assertFailsWith<BridgeRequestTimeoutException> {
                client.request(ns = "system", action = "capabilities")
            }
        }
        val timedOutRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())

        advanceTimeBy(1_001)
        runCurrent()

        val timeout = requestJob.await()
        assertEquals("system", timeout.ns)
        assertEquals("capabilities", timeout.action)
        assertEquals(1_000, timeout.timeoutMillis)

        transport.emitIncoming(
            json.encodeToString(
                BridgeResponse(
                    id = timedOutRequest.id,
                    ns = timedOutRequest.ns,
                    action = timedOutRequest.action,
                    ok = true,
                    payload = buildJsonObject { put("stale", true) }
                )
            )
        )

        val nextRequestJob = async { client.request(ns = "system", action = "capabilities") }
        val nextRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        transport.emitIncoming(
            json.encodeToString(
                BridgeResponse(
                    id = nextRequest.id,
                    ns = nextRequest.ns,
                    action = nextRequest.action,
                    ok = true,
                    payload = buildJsonObject { put("fresh", true) }
                )
            )
        )

        val response = nextRequestJob.await()
        assertTrue(response.ok)
        assertEquals("true", response.payload!!.jsonObject["fresh"]!!.jsonPrimitive.content)

        client.close()
    }

    private class FakeBridgeTransport : BridgeTransport {
        private val incomingFrames = Channel<String>(Channel.UNLIMITED)
        private val sentFrames = Channel<String>(Channel.UNLIMITED)

        override val incoming = incomingFrames.receiveAsFlow()

        override suspend fun send(text: String) {
            sentFrames.send(text)
        }

        suspend fun nextSent(): String {
            return sentFrames.receive()
        }

        suspend fun emitIncoming(text: String) {
            incomingFrames.send(text)
        }

        override suspend fun close() = Unit
    }
}
