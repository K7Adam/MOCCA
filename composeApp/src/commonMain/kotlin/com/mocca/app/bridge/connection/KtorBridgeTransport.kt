package com.mocca.app.bridge.connection

import com.mocca.app.bridge.client.BridgeTransport
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow

class KtorBridgeTransport(
    private val session: DefaultClientWebSocketSession
) : BridgeTransport {
    override val incoming: Flow<String> = session.incoming
        .receiveAsFlow()
        .mapNotNull { frame ->
            when (frame) {
                is Frame.Text -> frame.readText()
                else -> null
            }
        }

    override suspend fun send(text: String) {
        session.send(Frame.Text(text))
    }

    override suspend fun close() {
        session.close(CloseReason(CloseReason.Codes.NORMAL, "MOCCA bridge closing"))
    }
}
