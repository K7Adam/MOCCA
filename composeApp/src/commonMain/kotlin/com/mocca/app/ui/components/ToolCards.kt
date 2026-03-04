package com.mocca.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mocca.app.domain.model.MessagePart

/**
 * Rich tool card dispatcher — routes to tool-specific card composables
 * based on the tool name. All card implementations are internal functions
 * in the same package.
 */
@Composable
fun RichToolCard(
    part: MessagePart.ToolInvocation,
    modifier: Modifier = Modifier
) {
    val toolName = part.name.lowercase()

    when {
        toolName == "bash" || toolName == "shell" -> BashToolCard(part, modifier)
        toolName == "edit" || toolName == "multiedit" -> EditToolCard(part, modifier)
        toolName == "read" -> ReadToolCard(part, modifier)
        toolName == "glob" -> GlobToolCard(part, modifier)
        toolName == "grep" || toolName == "search" -> GrepToolCard(part, modifier)
        toolName == "list" || toolName == "ls" -> ListToolCard(part, modifier)
        toolName == "write" -> WriteToolCard(part, modifier)
        toolName == "todowrite" || toolName == "todo" -> TodoToolCard(part, modifier)
        toolName == "webfetch" || toolName == "web_search" -> WebFetchToolCard(part, modifier)
        toolName == "task" -> TaskToolCard(part, modifier)
        else -> GenericToolCard(part, modifier)
    }
}
