package com.mocca.app.bridge.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DirectBridgeTargetTest {
    @Test
    fun websocketUrlUsesCliBridgePathAndPairingCode() {
        val target = DirectBridgeTarget(
            host = "127.0.0.1",
            port = 17653,
            pairingCode = "123456"
        )

        assertEquals("ws://127.0.0.1:17653/v1/ws?pairingCode=123456", target.websocketUrl)
        assertEquals("http://127.0.0.1:17653/v1/health", target.healthUrl)
    }

    @Test
    fun websocketUrlCanUseTls() {
        val target = DirectBridgeTarget(
            host = "mocca.local",
            port = 443,
            pairingCode = "123456",
            useTls = true
        )

        assertEquals("wss://mocca.local:443/v1/ws?pairingCode=123456", target.websocketUrl)
        assertEquals("https://mocca.local:443/v1/health", target.healthUrl)
    }

    @Test
    fun websocketUrlEncodesPairingCodeAsQueryValue() {
        val target = DirectBridgeTarget(
            host = "192.168.0.5",
            port = 17653,
            pairingCode = "a b+c/?"
        )

        assertEquals("ws://192.168.0.5:17653/v1/ws?pairingCode=a%20b%2Bc%2F%3F", target.websocketUrl)
    }

    @Test
    fun rejectsInvalidConnectionInputs() {
        assertFailsWith<IllegalArgumentException> {
            DirectBridgeTarget(host = "", port = 17653, pairingCode = "123456")
        }
        assertFailsWith<IllegalArgumentException> {
            DirectBridgeTarget(host = "127.0.0.1", port = 0, pairingCode = "123456")
        }
        assertFailsWith<IllegalArgumentException> {
            DirectBridgeTarget(host = "127.0.0.1", port = 17653, pairingCode = "")
        }
    }
}
