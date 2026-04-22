package com.mocca.app.bridge.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BridgePairingIntentStoreTest {
    @Test
    fun submitStoresLatestNonBlankPayload() {
        val store = BridgePairingIntentStore()

        store.submit("mocca://bridge/connect?v=1")
        store.submit("")

        assertEquals("mocca://bridge/connect?v=1", store.pendingPayload.value)
    }

    @Test
    fun consumeClearsOnlyMatchingPayload() {
        val store = BridgePairingIntentStore()
        store.submit("first")

        store.consume("other")
        assertEquals("first", store.pendingPayload.value)

        store.consume("first")
        assertNull(store.pendingPayload.value)
    }
}
