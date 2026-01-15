package com.mocca.app.ui.screens.terminal

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.TerminalRepository
import kotlinx.coroutines.launch

data class TerminalState(
    val output: String = "",
    val isConnected: Boolean = false,
    val error: String? = null
)

class TerminalScreenModel(
    private val repository: TerminalRepository
) : StateScreenModel<TerminalState>(TerminalState()) {

    init {
        screenModelScope.launch {
            repository.terminalOutput.collect { text ->
                mutableState.value = state.value.copy(
                    output = state.value.output + text
                )
            }
        }
    }

    fun connect() {
        screenModelScope.launch {
            mutableState.value = state.value.copy(isConnected = true)
            try {
                // Check if existing terminal exists, else create
                val terminals = repository.listTerminals().getOrNull() ?: emptyList()
                val terminalId = terminals.firstOrNull()?.id ?: repository.createTerminal().getOrThrow().id
                
                repository.connectToTerminal(terminalId)
            } catch (e: Exception) {
                mutableState.value = state.value.copy(
                    isConnected = false,
                    error = e.message
                )
            }
        }
    }

    fun sendInput(text: String) {
        screenModelScope.launch {
            repository.sendInput(text)
        }
    }
    
    /**
     * Clear the terminal output buffer.
     */
    fun clearTerminal() {
        mutableState.value = state.value.copy(output = "")
    }
}
