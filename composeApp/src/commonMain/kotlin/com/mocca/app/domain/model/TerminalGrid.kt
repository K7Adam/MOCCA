package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class TerminalGridCell(
    val char: String,
    val fg: String? = null,
    val bg: String? = null,
    val attrs: List<String> = emptyList()
)

@Serializable
@Immutable
data class TerminalGridFrame(
    val terminalId: String,
    val fullFrame: Boolean = false,
    val cols: Int,
    val rows: Int,
    val cells: Map<String, List<TerminalGridCell>> = emptyMap(),
    val cursorX: Int = 0,
    val cursorY: Int = 0,
    val cursorVisible: Boolean = true,
    val cursorStyle: String = "block",
    val title: String? = null,
    val scrollbackLength: Int = 0
)

@Immutable
data class TerminalGridRow(
    val index: Int,
    val cells: List<TerminalGridCell> = emptyList()
) {
    val text: String get() = cells.joinToString(separator = "") { it.char }
}

@Immutable
data class TerminalGrid(
    val cols: Int = 120,
    val rows: Int = 40,
    val rowData: List<TerminalGridRow> = List(40) { TerminalGridRow(index = it) },
    val cursorX: Int = 0,
    val cursorY: Int = 0,
    val cursorVisible: Boolean = true,
    val title: String? = null,
    val scrollbackLength: Int = 0
) {
    fun apply(frame: TerminalGridFrame): TerminalGrid {
        val baseRows = if (frame.fullFrame || rowData.size != frame.rows) {
            List(frame.rows) { TerminalGridRow(index = it) }
        } else {
            rowData
        }.toMutableList()

        frame.cells.forEach { (rowKey, cells) ->
            val index = rowKey.toIntOrNull() ?: return@forEach
            if (index in 0 until frame.rows) {
                while (baseRows.size <= index) {
                    baseRows += TerminalGridRow(index = baseRows.size)
                }
                baseRows[index] = TerminalGridRow(index = index, cells = cells)
            }
        }

        return copy(
            cols = frame.cols,
            rows = frame.rows,
            rowData = baseRows.take(frame.rows),
            cursorX = frame.cursorX,
            cursorY = frame.cursorY,
            cursorVisible = frame.cursorVisible,
            title = frame.title ?: title,
            scrollbackLength = frame.scrollbackLength
        )
    }
}
