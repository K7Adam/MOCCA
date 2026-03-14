package com.mocca.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mocca.app.domain.model.MessagePart

/**
 * Rich tool card dispatcher — routes to the universal tool card.
 */
@Composable
fun RichToolCard(
    part: MessagePart.ToolInvocation,
    result: MessagePart.ToolResult? = null,
    modifier: Modifier = Modifier
) {
    UniversalToolCard(
        invocation = part,
        result = result,
        modifier = modifier
    )
}
