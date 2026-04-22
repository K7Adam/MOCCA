package com.mocca.app.ui.screens.terminal

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.bridge.client.requestPayload
import com.mocca.app.bridge.client.requireClient
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.opencode.BridgeResponseException
import com.mocca.app.domain.model.Terminal
import com.mocca.app.domain.model.TerminalGrid
import com.mocca.app.domain.model.TerminalGridFrame
import io.github.aakira.napier.Napier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Immutable
data class TerminalTab(
    val terminal: Terminal,
    val grid: TerminalGrid = TerminalGrid(),
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null
) {
    val displayTitle: String
        get() = grid.title
            ?: terminal.shell?.substringAfterLast("/")?.substringAfterLast("\\")?.takeIf { it.isNotBlank() }
            ?: "Terminal"
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
    private val bridgeConnectionManager: BridgeConnectionManager,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : ScreenModel {

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    init {
        observeBridgeEvents()
        loadExistingTerminals()
    }

    fun loadExistingTerminals() {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingTabs = true, globalError = null) }
            try {
                val client = bridgeConnectionManager.requireClient("terminal.list")
                val terminals = client.requestPayload<List<TerminalListItem>>("terminal", "list", json = json)
                val tabs = terminals.map { item ->
                    TerminalTab(
                        terminal = Terminal(id = item.id, shell = item.shell),
                        grid = TerminalGrid(cols = item.cols, rows = item.rows, title = item.title),
                        isConnected = !item.exited
                    )
                }
                _state.update { state ->
                    state.copy(
                        tabs = tabs,
                        activeTabId = state.activeTabId ?: tabs.firstOrNull()?.terminal?.id,
                        isLoadingTabs = false
                    )
                }
                tabs.forEach { tab -> requestSnapshot(tab.terminal.id) }
            } catch (error: Exception) {
                Napier.e("[TerminalScreenModel] loadExistingTerminals failed: ${error.message}", error)
                _state.update { it.copy(isLoadingTabs = false, globalError = error.toUiMessage("Unable to load terminals")) }
            }
        }
    }

    fun createTab() {
        screenModelScope.launch {
            _state.update { it.copy(isCreatingTab = true, globalError = null) }
            try {
                val client = bridgeConnectionManager.requireClient("terminal.spawn")
                val terminal = client.requestPayload<TerminalSpawnResponse>(
                    ns = "terminal",
                    action = "spawn",
                    payload = json.encodeToJsonElement(TerminalSpawnRequest(cols = _state.value.cols, rows = _state.value.rows)),
                    json = json
                )
                val tab = TerminalTab(
                    terminal = Terminal(id = terminal.id, shell = terminal.shell),
                    grid = TerminalGrid(cols = terminal.cols, rows = terminal.rows, title = terminal.title),
                    isConnected = true
                )
                _state.update { state ->
                    state.copy(
                        tabs = state.tabs.filterNot { it.terminal.id == terminal.id } + tab,
                        activeTabId = terminal.id,
                        isCreatingTab = false
                    )
                }
                requestSnapshot(terminal.id)
            } catch (error: Exception) {
                Napier.e("[TerminalScreenModel] createTab failed: ${error.message}", error)
                _state.update { it.copy(isCreatingTab = false, globalError = error.toUiMessage("Unable to create terminal")) }
            }
        }
    }

    fun closeTab(terminalId: String) {
        screenModelScope.launch {
            try {
                val client = bridgeConnectionManager.requireClient("terminal.kill")
                client.request(
                    ns = "terminal",
                    action = "kill",
                    payload = json.encodeToJsonElement(TerminalIdRequest(terminalId))
                )
            } catch (error: Exception) {
                Napier.w("[TerminalScreenModel] terminal.kill failed: ${error.message}", error)
            }
            removeTab(terminalId)
        }
    }

    fun selectTab(terminalId: String) {
        _state.update { it.copy(activeTabId = terminalId) }
        requestSnapshot(terminalId)
    }

    fun sendInput(terminalId: String, text: String) {
        screenModelScope.launch {
            try {
                val client = bridgeConnectionManager.requireClient("terminal.write")
                client.request(
                    ns = "terminal",
                    action = "write",
                    payload = json.encodeToJsonElement(TerminalWriteRequest(terminalId = terminalId, data = text))
                )
            } catch (error: Exception) {
                Napier.e("[TerminalScreenModel] sendInput failed: ${error.message}", error)
                updateTab(terminalId) { it.copy(error = error.toUiMessage("Unable to send input")) }
            }
        }
    }

    fun notifyResize(cols: Int, rows: Int) {
        _state.update { it.copy(cols = cols, rows = rows) }
        _state.value.tabs.forEach { tab ->
            screenModelScope.launch {
                try {
                    val client = bridgeConnectionManager.requireClient("terminal.resize")
                    client.request(
                        ns = "terminal",
                        action = "resize",
                        payload = json.encodeToJsonElement(TerminalResizeBridgeRequest(tab.terminal.id, cols, rows))
                    )
                } catch (error: Exception) {
                    Napier.w("[TerminalScreenModel] resize failed: ${error.message}", error)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeBridgeEvents() {
        screenModelScope.launch {
            bridgeConnectionManager.client
                .flatMapLatest { client -> client?.events ?: flowOf() }
                .filter { it.ns == "terminal" }
                .collectLatest { event ->
                    val payload = event.payload ?: return@collectLatest
                    when (event.event) {
                        "terminal.spawned" -> {
                            val spawned = json.decodeFromJsonElement<TerminalSpawnedEvent>(payload)
                            upsertTab(spawned)
                        }
                        "terminal.state" -> {
                            val frame = json.decodeFromJsonElement<TerminalGridFrame>(payload)
                            updateTab(frame.terminalId) { tab ->
                                tab.copy(
                                    grid = tab.grid.apply(frame),
                                    isConnecting = false,
                                    isConnected = true,
                                    error = null
                                )
                            }
                        }
                        "terminal.exited" -> {
                            val exited = json.decodeFromJsonElement<TerminalExitedEvent>(payload)
                            updateTab(exited.terminalId) { it.copy(isConnected = false, isConnecting = false) }
                        }
                        "terminal.error" -> {
                            val terminalError = json.decodeFromJsonElement<TerminalErrorEvent>(payload)
                            updateTab(terminalError.terminalId) { it.copy(error = terminalError.message, isConnecting = false) }
                        }
                    }
                }
        }
    }

    private fun requestSnapshot(terminalId: String) {
        screenModelScope.launch {
            try {
                val client = bridgeConnectionManager.requireClient("terminal.snapshot")
                val frame = client.requestPayload<TerminalGridFrame>(
                    ns = "terminal",
                    action = "snapshot",
                    payload = json.encodeToJsonElement(TerminalIdRequest(terminalId)),
                    json = json
                )
                updateTab(terminalId) { it.copy(grid = it.grid.apply(frame), isConnected = true) }
            } catch (error: Exception) {
                Napier.w("[TerminalScreenModel] snapshot failed: ${error.message}", error)
            }
        }
    }

    private fun upsertTab(event: TerminalSpawnedEvent) {
        _state.update { state ->
            val tab = TerminalTab(
                terminal = Terminal(id = event.terminalId, shell = event.shell),
                grid = TerminalGrid(cols = event.cols, rows = event.rows, title = event.title),
                isConnected = true
            )
            state.copy(
                tabs = state.tabs.filterNot { it.terminal.id == event.terminalId } + tab,
                activeTabId = state.activeTabId ?: event.terminalId
            )
        }
    }

    private fun removeTab(terminalId: String) {
        _state.update { state ->
            val remaining = state.tabs.filter { it.terminal.id != terminalId }
            state.copy(
                tabs = remaining,
                activeTabId = when {
                    state.activeTabId != terminalId -> state.activeTabId
                    remaining.isNotEmpty() -> remaining.last().terminal.id
                    else -> null
                }
            )
        }
    }

    private fun updateTab(terminalId: String, transform: (TerminalTab) -> TerminalTab) {
        _state.update { state ->
            state.copy(tabs = state.tabs.map { tab ->
                if (tab.terminal.id == terminalId) transform(tab) else tab
            })
        }
    }

    private fun Exception.toUiMessage(fallback: String): String {
        return when (this) {
            is BridgeResponseException -> message ?: fallback
            else -> message ?: fallback
        }
    }
}

@Serializable
private data class TerminalSpawnRequest(val cols: Int, val rows: Int)

@Serializable
private data class TerminalSpawnResponse(val id: String, val shell: String? = null, val title: String? = null, val cols: Int, val rows: Int)

@Serializable
private data class TerminalListItem(val id: String, val shell: String? = null, val title: String? = null, val cols: Int = 120, val rows: Int = 40, val exited: Boolean = false)

@Serializable
private data class TerminalIdRequest(val terminalId: String)

@Serializable
private data class TerminalWriteRequest(val terminalId: String, val data: String)

@Serializable
private data class TerminalResizeBridgeRequest(val terminalId: String, val cols: Int, val rows: Int)

@Serializable
private data class TerminalSpawnedEvent(val terminalId: String, val shell: String? = null, val title: String? = null, val cols: Int, val rows: Int)

@Serializable
private data class TerminalExitedEvent(val terminalId: String)

@Serializable
private data class TerminalErrorEvent(val terminalId: String, val message: String)
