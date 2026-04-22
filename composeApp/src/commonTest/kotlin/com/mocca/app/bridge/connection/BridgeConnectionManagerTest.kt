package com.mocca.app.bridge.connection

import com.mocca.app.bridge.client.BridgeTransport
import com.mocca.app.bridge.client.DirectBridgeTarget
import com.mocca.app.bridge.client.MoccaBridgeClient
import com.mocca.app.bridge.protocol.BridgeAiCapabilities
import com.mocca.app.bridge.protocol.BridgeCapabilities
import com.mocca.app.bridge.protocol.BridgeError
import com.mocca.app.bridge.protocol.BridgeRequest
import com.mocca.app.bridge.protocol.BridgeResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BridgeConnectionManagerTest {
    private val json = MoccaBridgeClient.DefaultBridgeJson

    @Test
    fun connectWithoutTargetSetsNotConfigured() = runTest {
        val targetRepository = BridgeTargetRepository(InMemoryBridgeTargetStore())
        val manager = BridgeConnectionManager(
            targetRepository = targetRepository,
            transportFactory = RecordingBridgeTransportFactory(),
            scope = backgroundScope
        )

        manager.connect()

        assertIs<BridgeConnectionStatus.NotConfigured>(manager.status.value)
        assertNull(manager.client.value)
    }

    @Test
    fun connectOpensTransportRequestsCapabilitiesPersistsTargetAndMarksConnected() = runTest {
        val targetRepository = BridgeTargetRepository(InMemoryBridgeTargetStore())
        val factory = RecordingBridgeTransportFactory()
        val healthChecker = RecordingBridgeHealthChecker()
        val manager = BridgeConnectionManager(
            targetRepository = targetRepository,
            transportFactory = factory,
            scope = backgroundScope,
            healthChecker = healthChecker
        )
        val target = DirectBridgeTarget(
            host = "192.168.0.10",
            port = 17653,
            pairingCode = "123456"
        )

        val connectJob = async { manager.connect(target) }
        runCurrent()
        val transport = factory.lastTransport!!
        val capabilitiesRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        assertEquals("system", capabilitiesRequest.ns)
        assertEquals("capabilities", capabilitiesRequest.action)

        transport.emitIncoming(
            BridgeResponse(
                id = capabilitiesRequest.id,
                ns = capabilitiesRequest.ns,
                action = capabilitiesRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    BridgeCapabilities(
                        protocolVersion = 1,
                        namespaces = listOf("system", "ai"),
                        ai = BridgeAiCapabilities(opencodeConfigSnapshot = true)
                    )
                )
            )
        )
        runCurrent()
        connectJob.await()

        val status = assertIs<BridgeConnectionStatus.Connected>(manager.status.value)
        assertEquals(target, status.target)
        assertEquals(1, status.capabilities.protocolVersion)
        assertEquals(listOf(target), healthChecker.checkedTargets)
        assertEquals(target, targetRepository.activeTarget.value)
        assertEquals(target, targetRepository.loadPersistedTarget())
        assertSame(target, factory.openedTargets.single())
        assertTrue(manager.client.value != null)
    }

    @Test
    fun healthFailureDoesNotOpenTransport() = runTest {
        val targetRepository = BridgeTargetRepository(InMemoryBridgeTargetStore())
        val factory = RecordingBridgeTransportFactory()
        val manager = BridgeConnectionManager(
            targetRepository = targetRepository,
            transportFactory = factory,
            scope = backgroundScope,
            healthChecker = RecordingBridgeHealthChecker(
                result = BridgeHealth(ok = false)
            )
        )
        val target = DirectBridgeTarget(
            host = "192.168.0.10",
            port = 17653,
            pairingCode = "123456"
        )

        manager.connect(target)

        val status = assertIs<BridgeConnectionStatus.Error>(manager.status.value)
        assertEquals("bridge_health_failed", status.code)
        assertTrue(factory.openedTargets.isEmpty())
        assertNull(manager.client.value)
    }

    @Test
    fun connectFailureClosesTransportAndMarksError() = runTest {
        val targetRepository = BridgeTargetRepository(InMemoryBridgeTargetStore())
        val factory = RecordingBridgeTransportFactory()
        val manager = BridgeConnectionManager(
            targetRepository = targetRepository,
            transportFactory = factory,
            scope = backgroundScope
        )
        val target = DirectBridgeTarget(
            host = "127.0.0.1",
            port = 17653,
            pairingCode = "123456"
        )

        val connectJob = async { manager.connect(target) }
        runCurrent()
        val transport = factory.lastTransport!!
        val capabilitiesRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        transport.emitIncoming(
            BridgeResponse(
                id = capabilitiesRequest.id,
                ns = capabilitiesRequest.ns,
                action = capabilitiesRequest.action,
                ok = false,
                error = BridgeError(
                    code = "not_found",
                    message = "No handler registered"
                )
            )
        )
        runCurrent()
        connectJob.await()

        val status = assertIs<BridgeConnectionStatus.Error>(manager.status.value)
        assertEquals("not_found", status.code)
        assertTrue(transport.wasClosed)
        assertNull(manager.client.value)
        assertNull(targetRepository.activeTarget.value)
    }

    @Test
    fun disconnectClosesActiveClientAndMarksDisconnected() = runTest {
        val targetRepository = BridgeTargetRepository(InMemoryBridgeTargetStore())
        val factory = RecordingBridgeTransportFactory()
        val manager = BridgeConnectionManager(
            targetRepository = targetRepository,
            transportFactory = factory,
            scope = backgroundScope
        )
        val target = DirectBridgeTarget(
            host = "127.0.0.1",
            port = 17653,
            pairingCode = "123456"
        )

        val connectJob = async { manager.connect(target) }
        runCurrent()
        val transport = factory.lastTransport!!
        val capabilitiesRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        transport.emitIncoming(
            BridgeResponse(
                id = capabilitiesRequest.id,
                ns = capabilitiesRequest.ns,
                action = capabilitiesRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    BridgeCapabilities(
                        protocolVersion = 1,
                        namespaces = listOf("system"),
                    )
                )
            )
        )
        runCurrent()
        connectJob.await()

        manager.disconnect()

        assertTrue(transport.wasClosed)
        assertNull(manager.client.value)
        assertIs<BridgeConnectionStatus.Disconnected>(manager.status.value)
    }

    @Test
    fun connectFromPairingPayloadParsesAndConnectsTarget() = runTest {
        val targetRepository = BridgeTargetRepository(InMemoryBridgeTargetStore())
        val factory = RecordingBridgeTransportFactory()
        val manager = BridgeConnectionManager(
            targetRepository = targetRepository,
            transportFactory = factory,
            scope = backgroundScope
        )

        val connectJob = async {
            manager.connectFromPairingPayload(
                "mocca://bridge/connect?v=1&host=192.168.0.42&port=17653&pairingCode=123456&tls=0"
            )
        }
        runCurrent()
        val transport = factory.lastTransport!!
        val capabilitiesRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        transport.emitIncoming(
            BridgeResponse(
                id = capabilitiesRequest.id,
                ns = capabilitiesRequest.ns,
                action = capabilitiesRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    BridgeCapabilities(
                        protocolVersion = 1,
                        namespaces = listOf("system", "ai"),
                        ai = BridgeAiCapabilities(opencodeConfigSnapshot = true)
                    )
                )
            )
        )
        runCurrent()
        connectJob.await()

        val status = assertIs<BridgeConnectionStatus.Connected>(manager.status.value)
        assertEquals("192.168.0.42", status.target.host)
        assertEquals("123456", status.target.pairingCode)
    }

    @Test
    fun connectFromInvalidPairingPayloadMarksError() = runTest {
        val manager = BridgeConnectionManager(
            targetRepository = BridgeTargetRepository(InMemoryBridgeTargetStore()),
            transportFactory = RecordingBridgeTransportFactory(),
            scope = backgroundScope
        )

        manager.connectFromPairingPayload("not-a-mocca-pairing-payload")

        val status = assertIs<BridgeConnectionStatus.Error>(manager.status.value)
        assertEquals("invalid_pairing_payload", status.code)
    }

    private class RecordingBridgeTransportFactory : BridgeTransportFactory {
        val openedTargets = mutableListOf<DirectBridgeTarget>()
        var lastTransport: FakeBridgeTransport? = null

        override suspend fun open(target: DirectBridgeTarget): BridgeTransport {
            openedTargets += target
            return FakeBridgeTransport().also { lastTransport = it }
        }
    }

    private class RecordingBridgeHealthChecker(
        private val result: BridgeHealth = BridgeHealth(
            ok = true,
            protocolVersion = 1,
            pairingRequired = true,
            websocketPath = "/v1/ws"
        )
    ) : BridgeHealthChecker {
        val checkedTargets = mutableListOf<DirectBridgeTarget>()

        override suspend fun check(target: DirectBridgeTarget): BridgeHealth {
            checkedTargets += target
            return result
        }
    }

    private class FakeBridgeTransport : BridgeTransport {
        private val incomingFrames = Channel<String>(Channel.UNLIMITED)
        private val sentFrames = Channel<String>(Channel.UNLIMITED)
        var wasClosed = false

        override val incoming = incomingFrames.receiveAsFlow()

        override suspend fun send(text: String) {
            sentFrames.send(text)
        }

        suspend fun nextSent(): String {
            return sentFrames.receive()
        }

        suspend fun emitIncoming(response: BridgeResponse) {
            incomingFrames.send(MoccaBridgeClient.DefaultBridgeJson.encodeToString(response))
        }

        override suspend fun close() {
            wasClosed = true
        }
    }

    private class InMemoryBridgeTargetStore(
        var rawValue: String? = null
    ) : BridgeTargetStore {
        override suspend fun readTargetJson(): String? = rawValue

        override suspend fun writeTargetJson(value: String) {
            rawValue = value
        }

        override suspend fun clearTargetJson() {
            rawValue = null
        }
    }
}
