package com.mocca.app.data.repository

import com.mocca.app.api.ApiExecutor
import com.mocca.app.api.ConnectionException
import com.mocca.app.api.MoccaApiClient
import com.mocca.app.api.RetryPolicy
import com.mocca.app.bridge.client.BridgeTransport
import com.mocca.app.bridge.client.DirectBridgeTarget
import com.mocca.app.bridge.client.MoccaBridgeClient
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.connection.BridgeHealth
import com.mocca.app.bridge.connection.BridgeHealthChecker
import com.mocca.app.bridge.connection.BridgeTargetRepository
import com.mocca.app.bridge.connection.BridgeTargetStore
import com.mocca.app.bridge.connection.BridgeTransportFactory
import com.mocca.app.bridge.protocol.BridgeCapabilities
import com.mocca.app.bridge.protocol.BridgeFsCapabilities
import com.mocca.app.bridge.protocol.BridgeRequest
import com.mocca.app.bridge.protocol.BridgeResponse
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.SearchMode
import com.mocca.app.domain.model.SearchQuery
import io.ktor.client.HttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoryTest {
    private val json = MoccaBridgeClient.DefaultBridgeJson

    @Test
    fun filePatternSearchUsesNativeCliFind() = runTest {
        val manager = createConnectedBridgeManager()
        val transport = manager.transport
        val repository = SearchRepository(
            apiClient = MoccaApiClient(
                api = FailingApiExecutor(),
                retryPolicy = RetryPolicy(maxRetries = 0)
            ),
            fileRepository = FileRepository(manager.bridgeConnectionManager)
        )

        val searchJob = async {
            repository.search(SearchQuery(query = "*.kt", mode = SearchMode.FILE_PATTERN)).toList()
        }

        runCurrent()
        val request = json.decodeFromString<BridgeRequest>(transport.nextSent())
        assertEquals("fs", request.ns)
        assertEquals("find", request.action)

        transport.emitIncoming(
            BridgeResponse(
                id = request.id,
                ns = request.ns,
                action = request.action,
                ok = true,
                payload = json.encodeToJsonElement(listOf("composeApp/src/Main.kt"))
            )
        )
        runCurrent()

        val emissions = searchJob.await()
        assertIs<Resource.Loading<*>>(emissions.first())
        val success = assertIs<Resource.Success<*>>(emissions.last())
        val data = success.data as com.mocca.app.domain.model.UnifiedSearchResult
        assertEquals(1, data.fileResults.size)
        assertEquals("composeApp/src/Main.kt", data.fileResults.first().path)
    }

    private suspend fun TestScope.createConnectedBridgeManager(): ConnectedBridgeFixture {
        val targetRepository = BridgeTargetRepository(InMemoryBridgeTargetStore())
        val factory = RecordingBridgeTransportFactory()
        val manager = BridgeConnectionManager(
            targetRepository = targetRepository,
            transportFactory = factory,
            scope = backgroundScope,
            healthChecker = object : BridgeHealthChecker {
                override suspend fun check(target: DirectBridgeTarget): BridgeHealth =
                    BridgeHealth(ok = true, protocolVersion = 2, pairingRequired = true)
            }
        )
        val target = DirectBridgeTarget(host = "127.0.0.1", port = 17653, pairingCode = "123456")

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
                        protocolVersion = 2,
                        namespaces = listOf("system", "fs"),
                        fs = BridgeFsCapabilities(native = true)
                    )
                )
            )
        )
        runCurrent()
        connectJob.await()
        assertIs<BridgeConnectionStatus.Connected>(manager.status.value)
        return ConnectedBridgeFixture(manager, transport)
    }

    private data class ConnectedBridgeFixture(
        val bridgeConnectionManager: BridgeConnectionManager,
        val transport: FakeBridgeTransport
    )

    private class FailingApiExecutor : ApiExecutor {
        override suspend fun <T> execute(block: suspend HttpClient.() -> T): T {
            throw ConnectionException("HTTP fallback should not be used in this test")
        }
    }

    private class InMemoryBridgeTargetStore(
        private var rawValue: String? = null
    ) : BridgeTargetStore {
        override suspend fun readTargetJson(): String? = rawValue
        override suspend fun writeTargetJson(value: String) {
            rawValue = value
        }
        override suspend fun clearTargetJson() {
            rawValue = null
        }
    }

    private class RecordingBridgeTransportFactory : BridgeTransportFactory {
        var lastTransport: FakeBridgeTransport? = null

        override suspend fun open(target: DirectBridgeTarget): BridgeTransport {
            return FakeBridgeTransport().also { lastTransport = it }
        }
    }

    private class FakeBridgeTransport : BridgeTransport {
        private val incomingChannel = Channel<String>(Channel.UNLIMITED)
        private val outgoing = Channel<String>(Channel.UNLIMITED)

        override val incoming = incomingChannel.receiveAsFlow()

        override suspend fun send(message: String) {
            outgoing.send(message)
        }

        override suspend fun close() {
            incomingChannel.close()
            outgoing.close()
        }

        suspend fun emitIncoming(response: BridgeResponse) {
            incomingChannel.send(MoccaBridgeClient.DefaultBridgeJson.encodeToString(response))
        }

        suspend fun nextSent(): String = outgoing.receive()
    }
}
