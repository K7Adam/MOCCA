package com.mocca.app.bridge.opencode

import com.mocca.app.bridge.client.BridgeTransport
import com.mocca.app.bridge.client.MoccaBridgeClient
import com.mocca.app.bridge.protocol.BridgeAiCapabilities
import com.mocca.app.bridge.protocol.BridgeCapabilities
import com.mocca.app.bridge.protocol.BridgeError
import com.mocca.app.bridge.protocol.BridgeRequest
import com.mocca.app.bridge.protocol.BridgeResponse
import com.mocca.app.bridge.protocol.OpenCodeConfigSnapshot
import com.mocca.app.bridge.protocol.OpenCodeCredentialInfo
import com.mocca.app.bridge.protocol.OpenCodeInstallInfo
import com.mocca.app.bridge.protocol.OpenCodeAgentInfo
import com.mocca.app.bridge.protocol.OpenCodeCommandInfo
import com.mocca.app.bridge.protocol.OpenCodeMcpServerInfo
import com.mocca.app.bridge.protocol.OpenCodeRuntimeEnsureResponse
import com.mocca.app.bridge.protocol.OpenCodeRuntimeServerConnection
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
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OpenCodeBridgeRepositoryTest {
    private val json = MoccaBridgeClient.DefaultBridgeJson

    @Test
    fun fetchCapabilitiesCachesUntilForceRefresh() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(transport = transport, scope = backgroundScope)
        val repository = OpenCodeBridgeRepository(client = client, json = json)

        val firstJob = async { repository.fetchCapabilities() }
        val firstRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        transport.emitIncoming(
            BridgeResponse(
                id = firstRequest.id,
                ns = firstRequest.ns,
                action = firstRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    BridgeCapabilities(
                        protocolVersion = 2,
                        namespaces = listOf("system", "ai"),
                        ai = BridgeAiCapabilities(events = true)
                    )
                )
            )
        )
        runCurrent()

        val first = firstJob.await()
        val cached = repository.fetchCapabilities()
        assertEquals(first, cached)
        assertEquals(false, transport.hasPendingSentFrame())

        val refreshedJob = async { repository.fetchCapabilities(forceRefresh = true) }
        val refreshRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        transport.emitIncoming(
            BridgeResponse(
                id = refreshRequest.id,
                ns = refreshRequest.ns,
                action = refreshRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    BridgeCapabilities(
                        protocolVersion = 2,
                        namespaces = listOf("system", "ai", "git"),
                        ai = BridgeAiCapabilities(events = true, eventReplay = true)
                    )
                )
            )
        )
        runCurrent()

        val refreshed = refreshedJob.await()
        assertEquals(listOf("system", "ai", "git"), refreshed.namespaces)
        assertTrue(refreshed.ai.eventReplay)

        client.close()
    }

    @Test
    fun fetchOpenCodeConfigSnapshotRequestsCapabilitiesThenSnapshot() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(transport = transport, scope = backgroundScope)
        val repository = OpenCodeBridgeRepository(client = client, json = json)

        val snapshotJob = async { repository.fetchOpenCodeConfigSnapshot() }

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
                        protocolVersion = 2,
                        namespaces = listOf("system", "ai"),
                        ai = BridgeAiCapabilities(opencodeConfigSnapshot = true)
                    )
                )
            )
        )
        runCurrent()

        val snapshotRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        assertEquals("ai", snapshotRequest.ns)
        assertEquals("config.snapshot", snapshotRequest.action)
        transport.emitIncoming(
            BridgeResponse(
                id = snapshotRequest.id,
                ns = snapshotRequest.ns,
                action = snapshotRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    OpenCodeConfigSnapshot(
                        installed = OpenCodeInstallInfo(
                            available = true,
                            command = "opencode",
                            version = "1.14.19"
                        )
                    )
                )
            )
        )
        runCurrent()

        val snapshot = snapshotJob.await()
        assertTrue(snapshot.installed.available)
        assertEquals("1.14.19", snapshot.installed.version)

        client.close()
    }

    @Test
    fun fetchOpenCodeConfigSnapshotRejectsMissingCapability() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(transport = transport, scope = backgroundScope)
        val repository = OpenCodeBridgeRepository(client = client, json = json)

        val snapshotJob = async {
            assertFailsWith<BridgeFeatureUnavailableException> {
                repository.fetchOpenCodeConfigSnapshot()
            }
        }

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
                        namespaces = listOf("system"),
                        ai = BridgeAiCapabilities(opencodeConfigSnapshot = false)
                    )
                )
            )
        )
        runCurrent()

        val error = snapshotJob.await()
        assertEquals("ai.config.snapshot", error.feature)
        assertEquals(false, transport.hasPendingSentFrame())

        client.close()
    }

    @Test
    fun ensureOpenCodeRuntimeRequestsCapabilitiesThenRuntimeEnsure() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(transport = transport, scope = backgroundScope)
        val repository = OpenCodeBridgeRepository(client = client, json = json)

        val runtimeJob = async { repository.ensureOpenCodeRuntime() }

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
                        protocolVersion = 2,
                        namespaces = listOf("system", "ai"),
                        ai = BridgeAiCapabilities(opencodeRuntime = true)
                    )
                )
            )
        )
        runCurrent()

        val runtimeRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        assertEquals("ai", runtimeRequest.ns)
        assertEquals("runtime.ensure", runtimeRequest.action)
        transport.emitIncoming(
            BridgeResponse(
                id = runtimeRequest.id,
                ns = runtimeRequest.ns,
                action = runtimeRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    OpenCodeRuntimeEnsureResponse(
                        status = "ready",
                        server = OpenCodeRuntimeServerConnection(
                            baseUrl = "http://192.168.0.42:49200",
                            host = "192.168.0.42",
                            port = 49200,
                            username = "mocca",
                            password = "secret",
                            useHttps = false
                        )
                    )
                )
            )
        )
        runCurrent()

        val runtime = runtimeJob.await()
        assertEquals("ready", runtime.status)
        assertEquals("192.168.0.42", runtime.server.host)
        assertEquals(49200, runtime.server.port)

        client.close()
    }

    @Test
    fun fetchCapabilitiesThrowsTypedBridgeResponseExceptionOnErrorResponse() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(transport = transport, scope = backgroundScope)
        val repository = OpenCodeBridgeRepository(client = client, json = json)

        val capabilitiesJob = async {
            assertFailsWith<BridgeResponseException> {
                repository.fetchCapabilities()
            }
        }

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

        val error = capabilitiesJob.await()
        assertEquals("not_found", error.code)
        assertEquals("system", error.ns)
        assertEquals("capabilities", error.action)

        client.close()
    }

    @Test
    fun fetchConfigProjectionsUseDedicatedBridgeRoutes() = runTest {
        val transport = FakeBridgeTransport()
        val client = MoccaBridgeClient(transport = transport, scope = backgroundScope)
        val repository = OpenCodeBridgeRepository(client = client, json = json)

        val credentialsJob = async { repository.fetchCredentials() }
        val credentialsRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        assertEquals("providers", credentialsRequest.ns)
        assertEquals("credentials.list", credentialsRequest.action)
        transport.emitIncoming(
            BridgeResponse(
                id = credentialsRequest.id,
                ns = credentialsRequest.ns,
                action = credentialsRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    listOf(OpenCodeCredentialInfo(name = "anthropic", type = "api"))
                )
            )
        )
        runCurrent()
        assertEquals("anthropic", credentialsJob.await().single().name)

        val agentsJob = async { repository.fetchAgents() }
        val agentsRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        assertEquals("ai", agentsRequest.ns)
        assertEquals("agents.list", agentsRequest.action)
        transport.emitIncoming(
            BridgeResponse(
                id = agentsRequest.id,
                ns = agentsRequest.ns,
                action = agentsRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    listOf(OpenCodeAgentInfo(name = "build", primary = true))
                )
            )
        )
        runCurrent()
        assertTrue(agentsJob.await().single().primary)

        val commandsJob = async { repository.fetchCommands() }
        val commandsRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        assertEquals("commands", commandsRequest.ns)
        assertEquals("list", commandsRequest.action)
        transport.emitIncoming(
            BridgeResponse(
                id = commandsRequest.id,
                ns = commandsRequest.ns,
                action = commandsRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    listOf(OpenCodeCommandInfo(name = "lint", description = "Run lint"))
                )
            )
        )
        runCurrent()
        assertEquals("Run lint", commandsJob.await().single().description)

        val mcpServersJob = async { repository.fetchMcpServers() }
        val mcpServersRequest = json.decodeFromString<BridgeRequest>(transport.nextSent())
        assertEquals("mcp", mcpServersRequest.ns)
        assertEquals("servers.list", mcpServersRequest.action)
        transport.emitIncoming(
            BridgeResponse(
                id = mcpServersRequest.id,
                ns = mcpServersRequest.ns,
                action = mcpServersRequest.action,
                ok = true,
                payload = json.encodeToJsonElement(
                    listOf(OpenCodeMcpServerInfo(name = "filesystem", type = "local", enabled = true))
                )
            )
        )
        runCurrent()
        assertEquals("filesystem", mcpServersJob.await().single().name)

        client.close()
    }

    private suspend fun FakeBridgeTransport.emitIncoming(response: BridgeResponse) {
        emitIncoming(json.encodeToString(response))
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

        fun hasPendingSentFrame(): Boolean {
            return sentFrames.tryReceive().isSuccess
        }

        suspend fun emitIncoming(text: String) {
            incomingFrames.send(text)
        }

        override suspend fun close() = Unit
    }
}
