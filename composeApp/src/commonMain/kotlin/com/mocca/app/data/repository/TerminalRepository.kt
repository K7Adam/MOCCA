package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.Terminal
import io.github.aakira.napier.Napier
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class TerminalRepository(
    private val apiClient: MoccaApiClient
) {
    // Active terminal session
    private var activeSession: WebSocketSession? = null
    
    // Incoming data flow
    private val _terminalOutput = MutableSharedFlow<String>(replay = 100)
    val terminalOutput: SharedFlow<String> = _terminalOutput.asSharedFlow()

    suspend fun listTerminals(): Result<List<Terminal>> {
        return apiClient.listTerminals()
    }

    suspend fun createTerminal(): Result<Terminal> {
        return apiClient.createTerminal()
    }

    suspend fun connectToTerminal(id: String) {
        try {
            apiClient.connectToTerminal(id) {
                activeSession = this
                Napier.d("Connected to terminal $id")
                
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            _terminalOutput.emit(text)
                        }
                    }
                } catch (e: Exception) {
                    Napier.e("Terminal connection error", e)
                } finally {
                    activeSession = null
                    Napier.d("Disconnected from terminal $id")
                }
            }
        } catch (e: Exception) {
            Napier.e("Failed to connect to terminal", e)
            throw e
        }
    }

    suspend fun sendInput(text: String) {
        activeSession?.send(Frame.Text(text))
    }

    suspend fun resize(id: String, cols: Int, rows: Int) {
        apiClient.resizeTerminal(id, cols, rows)
    }
}
