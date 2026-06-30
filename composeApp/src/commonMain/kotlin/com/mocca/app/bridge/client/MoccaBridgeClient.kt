package com.mocca.app.bridge.client

import com.mocca.app.bridge.protocol.BridgeEvent
import com.mocca.app.bridge.protocol.BridgeRequest
import com.mocca.app.bridge.protocol.BridgeResponse
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

interface BridgeTransport {
    val incoming: Flow<String>

    suspend fun send(text: String)

    suspend fun close()
}

class BridgeRequestTimeoutException(
    val ns: String,
    val action: String,
    val timeoutMillis: Long
) : RuntimeException("Bridge request $ns.$action timed out after ${timeoutMillis}ms")

class MoccaBridgeClient(
    private val transport: BridgeTransport,
    private val scope: CoroutineScope,
    private val json: Json = DefaultBridgeJson,
    private val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS
) {
    init {
        require(requestTimeoutMillis > 0) { "Bridge request timeout must be positive" }
    }

    private val pendingMutex = Mutex()
    private val pending = mutableMapOf<String, CompletableDeferred<BridgeResponse>>()
    private val _events = MutableSharedFlow<BridgeEvent>(extraBufferCapacity = 64)
    private var nextRequestNumber = 0L
    private val collectorJob: Job = scope.launch {
        try {
            transport.incoming.collect { frame ->
                handleIncomingFrame(frame)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Napier.w("[MoccaBridgeClient] incoming bridge transport failed: ${error.message}", error)
            failPending(error)
        }
    }

    val events: Flow<BridgeEvent> = _events.asSharedFlow()

    suspend fun request(
        ns: String,
        action: String,
        payload: JsonElement? = null,
        timeoutMillis: Long = requestTimeoutMillis
    ): BridgeResponse {
        require(timeoutMillis > 0) { "Bridge request timeout must be positive" }
        val id = nextRequestId()
        val deferred = CompletableDeferred<BridgeResponse>()
        val frame = json.encodeToString(
            BridgeRequest(
                id = id,
                ns = ns,
                action = action,
                payload = payload
            )
        )
        pendingMutex.withLock {
            pending[id] = deferred
        }

        try {
            return withTimeout(timeoutMillis) {
                transport.send(frame)
                deferred.await()
            }
        } catch (_: TimeoutCancellationException) {
            throw BridgeRequestTimeoutException(ns, action, timeoutMillis)
        } finally {
            pendingMutex.withLock {
                pending.remove(id)
            }
        }
    }

    suspend fun close() {
        collectorJob.cancel()
        pendingMutex.withLock {
            pending.values.forEach { deferred ->
                deferred.cancel()
            }
            pending.clear()
        }
        transport.close()
    }

    private suspend fun nextRequestId(): String = pendingMutex.withLock {
        nextRequestNumber += 1
        "req-$nextRequestNumber"
    }

    private suspend fun handleIncomingFrame(frame: String) {
        val response = decodeResponseOrNull(frame)
        if (response != null) {
            pendingMutex.withLock {
                pending[response.id]
            }?.complete(response)
            return
        }

        val event = decodeEventOrNull(frame)
        if (event != null) {
            _events.emit(event)
        }
    }

    private suspend fun failPending(error: Throwable) {
        pendingMutex.withLock {
            pending.values.forEach { deferred ->
                deferred.completeExceptionally(error)
            }
            pending.clear()
        }
    }

    private fun decodeResponseOrNull(frame: String): BridgeResponse? =
        try {
            json.decodeFromString<BridgeResponse>(frame)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun decodeEventOrNull(frame: String): BridgeEvent? =
        try {
            json.decodeFromString<BridgeEvent>(frame)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    companion object {
        const val DEFAULT_REQUEST_TIMEOUT_MILLIS: Long = 30_000

        @OptIn(ExperimentalSerializationApi::class)
        val DefaultBridgeJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
