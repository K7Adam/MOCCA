package com.mocca.app.domain.model

/**
 * UI-specific grouping of message parts.
 */
sealed class MessagePartGroup {
    data class Single(val part: MessagePart) : MessagePartGroup()
    data class ToolGroup(val tools: List<Pair<MessagePart.ToolInvocation, MessagePart.ToolResult?>>) : MessagePartGroup()
}

/**
 * Groups message parts for presentation.
 * Consecutive tool invocations and results are grouped together.
 */
fun List<MessagePart>.groupForUi(): List<MessagePartGroup> {
    val result = mutableListOf<MessagePartGroup>()
    val toolBuffer = mutableListOf<Pair<MessagePart.ToolInvocation, MessagePart.ToolResult?>>()
    val partsList = this
    var i = 0
    while (i < partsList.size) {
        val part = partsList[i]
        if (part is MessagePart.ToolInvocation) {
            val nextPart = partsList.getOrNull(i + 1)
            val toolResult = if (nextPart is MessagePart.ToolResult) { i++; nextPart } else null
            toolBuffer.add(Pair(part, toolResult))
        } else {
            if (toolBuffer.isNotEmpty()) {
                result.add(MessagePartGroup.ToolGroup(toolBuffer.toList()))
                toolBuffer.clear()
            }
            result.add(MessagePartGroup.Single(part))
        }
        i++
    }
    if (toolBuffer.isNotEmpty()) {
        result.add(MessagePartGroup.ToolGroup(toolBuffer.toList()))
    }
    return result
}
