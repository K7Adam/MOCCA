package com.mocca.app.bridge.connection

import com.mocca.app.bridge.client.DirectBridgeTarget
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BridgeTargetRepositoryTest {
    @Test
    fun savePersistsTargetAndUpdatesState() = runTest {
        val store = InMemoryBridgeTargetStore()
        val repository = BridgeTargetRepository(store)
        val target = DirectBridgeTarget(
            host = "192.168.0.12",
            port = 17653,
            pairingCode = "123456"
        )

        repository.save(target)

        assertEquals(target, repository.activeTarget.value)
        assertTrue(repository.isLoaded.value)
        assertEquals(target, repository.loadPersistedTarget())
        assertTrue(store.rawValue!!.contains("\"host\":\"192.168.0.12\""))
    }

    @Test
    fun loadRestoresPersistedTarget() = runTest {
        val target = DirectBridgeTarget(
            host = "mocca.local",
            port = 443,
            pairingCode = "654321",
            useTls = true
        )
        val store = InMemoryBridgeTargetStore()
        BridgeTargetRepository(store).save(target)

        val repository = BridgeTargetRepository(store)
        repository.load()

        assertEquals(target, repository.activeTarget.value)
        assertTrue(repository.isLoaded.value)
    }

    @Test
    fun loadClearsInvalidPersistedTarget() = runTest {
        val store = InMemoryBridgeTargetStore(rawValue = "{not-json")
        val repository = BridgeTargetRepository(store)

        repository.load()

        assertNull(repository.activeTarget.value)
        assertTrue(repository.isLoaded.value)
        assertTrue(store.wasCleared)
    }

    @Test
    fun clearRemovesPersistedTargetAndUpdatesState() = runTest {
        val store = InMemoryBridgeTargetStore()
        val repository = BridgeTargetRepository(store)
        repository.save(
            DirectBridgeTarget(
                host = "127.0.0.1",
                port = 17653,
                pairingCode = "123456"
            )
        )

        repository.clear()

        assertNull(repository.activeTarget.value)
        assertTrue(repository.isLoaded.value)
        assertNull(store.rawValue)
        assertTrue(store.wasCleared)
    }

    @Test
    fun startsUnloadedUntilLoadOrSaveRuns() {
        val repository = BridgeTargetRepository(InMemoryBridgeTargetStore())

        assertFalse(repository.isLoaded.value)
        assertNull(repository.activeTarget.value)
    }

    private class InMemoryBridgeTargetStore(
        var rawValue: String? = null
    ) : BridgeTargetStore {
        var wasCleared = false

        override suspend fun readTargetJson(): String? {
            return rawValue
        }

        override suspend fun writeTargetJson(value: String) {
            wasCleared = false
            rawValue = value
        }

        override suspend fun clearTargetJson() {
            wasCleared = true
            rawValue = null
        }
    }
}
