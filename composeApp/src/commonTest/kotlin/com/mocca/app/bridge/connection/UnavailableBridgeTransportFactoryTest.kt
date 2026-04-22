package com.mocca.app.bridge.connection

import com.mocca.app.bridge.client.DirectBridgeTarget
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnavailableBridgeTransportFactoryTest {
    @Test
    fun openThrowsTypedUnavailableException() = runTest {
        val error = assertFailsWith<BridgeTransportUnavailableException> {
            UnavailableBridgeTransportFactory.open(
                DirectBridgeTarget(
                    host = "127.0.0.1",
                    port = 17653,
                    pairingCode = "123456"
                )
            )
        }

        assertEquals("direct-websocket", error.transport)
    }
}
