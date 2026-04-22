package com.mocca.app.bridge.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BridgePairingPayloadParserTest {
    @Test
    fun parsesMoccaDeepLinkPayload() {
        val target = BridgePairingPayloadParser.parse(
            "mocca://bridge/connect?v=1&host=192.168.0.42&port=17653&pairingCode=123456&tls=0"
        )

        assertEquals("192.168.0.42", target.host)
        assertEquals(17653, target.port)
        assertEquals("123456", target.pairingCode)
        assertEquals(false, target.useTls)
    }

    @Test
    fun parsesTlsMoccaDeepLinkPayloadWithEncodedValues() {
        val target = BridgePairingPayloadParser.parse(
            "mocca://bridge/connect?v=1&host=mocca+pc.local&port=443&pairingCode=a+b%2Bc%2F%3F&tls=1"
        )

        assertEquals("mocca pc.local", target.host)
        assertEquals(443, target.port)
        assertEquals("a b+c/?", target.pairingCode)
        assertEquals(true, target.useTls)
    }

    @Test
    fun parsesDirectWebsocketUrlPayload() {
        val target = BridgePairingPayloadParser.parse(
            "wss://mocca.local:443/v1/ws?pairingCode=123456"
        )

        assertEquals("mocca.local", target.host)
        assertEquals(443, target.port)
        assertEquals("123456", target.pairingCode)
        assertEquals(true, target.useTls)
    }

    @Test
    fun rejectsInvalidPairingPayloads() {
        assertFailsWith<BridgePairingPayloadException> {
            BridgePairingPayloadParser.parse("mocca://bridge/connect?v=1&host=&port=17653&pairingCode=123456")
        }
        assertFailsWith<BridgePairingPayloadException> {
            BridgePairingPayloadParser.parse("mocca://bridge/connect?v=1&host=192.168.0.42&port=99999&pairingCode=123456")
        }
        assertFailsWith<BridgePairingPayloadException> {
            BridgePairingPayloadParser.parse("https://mocca.local/not-a-bridge-url")
        }
    }
}
