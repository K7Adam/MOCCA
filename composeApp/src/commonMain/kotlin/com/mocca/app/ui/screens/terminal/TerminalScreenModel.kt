package com.mocca.app.ui.screens.terminal

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Terminal
import io.github.aakira.napier.Napier
import io.ktor.websocket.Frame
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel


@Immutable
data class TerminalTab(
    val terminal: Terminal,
    val output: String = "",
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null
) {
    val displayTitle: String
        get() = terminal.shell?.let { shell ->
            val name = shell.substringAfterLast("/")
            if (name.isNotBlank()) name else "Terminal"
        } ?: "Terminal"
}

@Immutable
data class TerminalState(
    val tabs: List<TerminalTab> = emptyList(),
    val activeTabId: String? = null,
    val isCreatingTab: Boolean = false,
    val isLoadingTabs: Boolean = false,
    val globalError: String? = null,
    val cols: Int = 120,
    val rows: Int = 40
) {
    val activeTab: TerminalTab?
        get() = tabs.find { it.terminal.id == activeTabId }
}


class TerminalScreenModel(
    private val apiClient: MoccaApiClient
) : ScreenModel {

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    /** Active WebSocket sessions keyed by terminal ID */
    private val wsSessions = mutableMapOf<String, DefaultClientWebSocketSession>()
    /** Receive coroutine jobs keyed by terminal ID */
    private val receiveJobs = mutableMapOf<String, Job>()


    init {
        loadExistingTerminals()
    }

    override fun onDispose() {
        receiveJobs.values.forEach { it.cancel() }
        receiveJobs.clear()
        wsSessions.values.forEach { it.cancel() }
        wsSessions.clear()
        super.onDispose()
    }


    /** Load all terminals that already exist on the server. */
    fun loadExistingTerminals() {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingTabs = true, globalError = null) }
            apiClient.listTerminals().onSuccess { terminals ->
                val tabs = terminals.map { TerminalTab(terminal = it) }
                _state.update { s ->
                    s.copy(
                        tabs = tabs,
                        activeTabId = s.activeTabId ?: tabs.firstOrNull()?.terminal?.id,
                        isLoadingTabs = false
                    )
                }
                // Auto-connect to existing terminals
                tabs.forEach { tab -> connectToTerminal(tab.terminal.id) }
            }.onFailure { e ->
                Napier.e("[TerminalScreenModel] loadExistingTerminals failed: \${e.message}")
                _state.update { it.copy(isLoadingTabs = false, globalError = e.message) }
            }
        }
    }

    /** Create a new terminal tab. */
    fun createTab() {
        screenModelScope.launch {
            _state.update { it.copy(isCreatingTab = true) }
            apiClient.createTerminal().onSuccess { terminal ->
                val newTab = TerminalTab(terminal = terminal)
                _state.update { s ->
                    s.copy(
                        tabs = s.tabs + newTab,
                        activeTabId = terminal.id,
                        isCreatingTab = false
                    )
                }
                connectToTerminal(terminal.id)
                resizeTerminal(terminal.id, _state.value.cols, _state.value.rows)
            }.onFailure { e ->
                Napier.e("[TerminalScreenModel] createTab failed: \${e.message}")
                _state.update { it.copy(isCreatingTab = false, globalError = e.message) }
            }
        }
    }

    /** Close and delete a terminal tab. */
    fun closeTab(terminalId: String) {
        screenModelScope.launch {
            disconnectFromTerminal(terminalId)
            apiClient.deleteTerminal(terminalId).onFailure { e ->
                Napier.w("[TerminalScreenModel] deleteTerminal $terminalId failed: \${e.message}")
            }
            _state.update { s ->
                val remaining = s.tabs.filter { it.terminal.id != terminalId }
                val newActive = when {
                    s.activeTabId != terminalId -> s.activeTabId
                    remaining.isNotEmpty() -> remaining.last().terminal.id
                    else -> null
                }
                s.copy(tabs = remaining, activeTabId = newActive)
            }
        }
    }

    /** Switch focus to a different tab. Lazy-connects if not yet connected. */
    fun selectTab(terminalId: String) {
        _state.update { it.copy(activeTabId = terminalId) }
        val tab = _state.value.tabs.find { it.terminal.id == terminalId }
        if (tab != null && !tab.isConnected && !tab.isConnecting) {
            connectToTerminal(terminalId)
        }
    }

    /** Send keyboard input to a terminal's WebSocket. */
    fun sendInput(terminalId: String, text: String) {
        screenModelScope.launch {
            val ws = wsSessions[terminalId]
            if (ws != null) {
                try {
                    ws.send(Frame.Text(text))
                } catch (e: Exception) {
                    Napier.e("[TerminalScreenModel] sendInput failed for $terminalId: \${e.message}")
                }
            } else {
                Napier.w("[TerminalScreenModel] sendInput: no WS for $terminalId")
            }
        }
    }

    /** Notify the server that the terminal view has been resized. */
    fun notifyResize(cols: Int, rows: Int) {
        _state.update { it.copy(cols = cols, rows = rows) }
        _state.value.tabs.forEach { tab ->
            resizeTerminal(tab.terminal.id, cols, rows)
        }
    }


    private fun connectToTerminal(terminalId: String) {
        if (receiveJobs.containsKey(terminalId)) return
        updateTab(terminalId) { it.copy(isConnecting = true, error = null) }
        val job = screenModelScope.launch {
            try {
                apiClient.connectToTerminal(terminalId) {
                    wsSessions[terminalId] = this
                    updateTab(terminalId) { it.copy(isConnecting = false, isConnected = true) }
                    // Receive loop
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            appendOutput(terminalId, frame.readText())
                        }
                    }
                }
            } catch (e: Exception) {
                Napier.e("[TerminalScreenModel] WS error for $terminalId: \${e.message}")
                updateTab(terminalId) {
                    it.copy(isConnecting = false, isConnected = false, error = e.message)
                }
            } finally {
                wsSessions.remove(terminalId)
                receiveJobs.remove(terminalId)
                updateTab(terminalId) { it.copy(isConnected = false) }
            }
        }
        receiveJobs[terminalId] = job
    }

    private fun disconnectFromTerminal(terminalId: String) {
        receiveJobs.remove(terminalId)?.cancel()
        // Cancelling the receive job tears down the WS connection
        wsSessions.remove(terminalId)
        updateTab(terminalId) { it.copy(isConnected = false, isConnecting = false) }
    }

    private fun updateTab(terminalId: String, transform: (TerminalTab) -> TerminalTab) {
        _state.update { s ->
            s.copy(tabs = s.tabs.map { tab ->
                if (tab.terminal.id == terminalId) transform(tab) else tab
            })
        }
    }

    private fun appendOutput(terminalId: String, chunk: String) {
        _state.update { s ->
            s.copy(tabs = s.tabs.map { tab ->
                if (tab.terminal.id == terminalId) {
                    val newOutput = (tab.output + chunk).takeLast(50_000)
                    tab.copy(output = newOutput)
                } else tab
            })
        }
    }

    private fun resizeTerminal(terminalId: String, cols: Int, rows: Int) {
        screenModelScope.launch {
            apiClient.resizeTerminal(terminalId, cols, rows).onFailure { e ->
                Napier.w("[TerminalScreenModel] resize $terminalId failed: \${e.message}")
            }
        }
    }
}
