package com.mocca.app.ui.screens.terminal

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.TerminalRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class TerminalState(
    val output: String = "",
    val isConnected: Boolean = false,
    val error: String? = null,
    val currentTerminalId: String? = null,
    val currentCols: Int = 80,
    val currentRows: Int = 24,
    // ═══════════════════════════════════════════════════════════════════════════════
    // COMMAND HISTORY (Priority 5.3)
    // ═══════════════════════════════════════════════════════════════════════════════
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,  // -1 = not navigating history
    val currentInput: String = "" // Saved input when navigating history
)

@OptIn(FlowPreview::class)
class TerminalScreenModel(
    private val repository: TerminalRepository
) : StateScreenModel<TerminalState>(TerminalState()) {
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TERMINAL RESIZE (Priority 5.1)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    // Debounce resize requests to avoid spamming the API during animations
    private val resizeFlow = MutableSharedFlow<Pair<Int, Int>>()

    init {
        screenModelScope.launch {
            repository.terminalOutput.collect { text ->
                mutableState.value = state.value.copy(
                    output = state.value.output + text
                )
            }
        }
        
        // Debounce resize calls (500ms delay to batch rapid resizes)
        screenModelScope.launch {
            resizeFlow.debounce(500).collect { (cols, rows) ->
                val terminalId = state.value.currentTerminalId
                if (terminalId != null && (cols != state.value.currentCols || rows != state.value.currentRows)) {
                    try {
                        repository.resize(terminalId, cols, rows)
                        mutableState.value = state.value.copy(
                            currentCols = cols,
                            currentRows = rows
                        )
                    } catch (e: Exception) {
                        // Resize failures are non-critical, just log
                        io.github.aakira.napier.Napier.w("Terminal resize failed: ${e.message}")
                    }
                }
            }
        }
    }

    fun connect() {
        screenModelScope.launch {
            mutableState.value = state.value.copy(isConnected = true)
            try {
                // Check if existing terminal exists, else create
                val terminals = repository.listTerminals().getOrNull() ?: emptyList()
                val terminal = terminals.firstOrNull() ?: repository.createTerminal().getOrThrow()
                
                mutableState.value = state.value.copy(currentTerminalId = terminal.id)
                
                repository.connectToTerminal(terminal.id)
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
    
    /**
     * Request terminal resize with debouncing.
     * Call this when the terminal container size changes.
     * 
     * @param cols Number of columns (characters per line)
     * @param rows Number of rows (lines visible)
     */
    fun resizeTerminal(cols: Int, rows: Int) {
        screenModelScope.launch {
            resizeFlow.emit(cols to rows)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // COMMAND HISTORY (Priority 5.3)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Add a command to history after execution.
     * Called automatically by sendInputWithHistory.
     */
    private fun addToHistory(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return
        
        // Remove duplicate if exists, add to front
        val newHistory = (listOf(trimmed) + state.value.commandHistory.filter { it != trimmed })
            .take(MAX_HISTORY_SIZE)
        
        mutableState.value = state.value.copy(
            commandHistory = newHistory,
            historyIndex = -1
        )
    }
    
    /**
     * Send input and add to history.
     */
    fun sendInputWithHistory(text: String) {
        val command = text.trimEnd('\n')
        if (command.isNotBlank()) {
            addToHistory(command)
        }
        sendInput(text)
    }
    
    /**
     * Navigate to previous command in history (up arrow).
     * @param currentInput The current input text before navigating
     * @return The command to show, or null if no more history
     */
    fun navigateHistoryUp(currentInput: String): String? {
        val history = state.value.commandHistory
        if (history.isEmpty()) return null
        
        val currentIndex = state.value.historyIndex
        val newIndex = if (currentIndex == -1) {
            // Save current input and start at first history item
            mutableState.value = state.value.copy(currentInput = currentInput)
            0
        } else if (currentIndex < history.size - 1) {
            currentIndex + 1
        } else {
            return null // At end of history
        }
        
        mutableState.value = state.value.copy(historyIndex = newIndex)
        return history.getOrNull(newIndex)
    }
    
    /**
     * Navigate to next command in history (down arrow).
     * @return The command to show, or the saved current input if at end
     */
    fun navigateHistoryDown(): String? {
        val currentIndex = state.value.historyIndex
        if (currentIndex == -1) return null // Not in history mode
        
        val newIndex = currentIndex - 1
        
        return if (newIndex >= 0) {
            mutableState.value = state.value.copy(historyIndex = newIndex)
            state.value.commandHistory.getOrNull(newIndex)
        } else {
            // Return to current input
            mutableState.value = state.value.copy(historyIndex = -1)
            state.value.currentInput
        }
    }
    
    /**
     * Reset history navigation (called when user types or submits).
     */
    fun resetHistoryNavigation() {
        mutableState.value = state.value.copy(historyIndex = -1, currentInput = "")
    }
    
    companion object {
        private const val MAX_HISTORY_SIZE = 100
    }
}
