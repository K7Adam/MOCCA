package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

/**
 * Suggestion item for the popup.
 */
data class SuggestionItem(
    val id: String,
    val label: String,
    val description: String? = null,
    val type: SuggestionType
)

enum class SuggestionType {
    COMMAND, // Slash command
    MODE     // @mention
}

/**
 * Popup showing suggestions for commands or mentions.
 */
@Composable
fun SuggestionPopup(
    suggestions: List<SuggestionItem>,
    onSuggestionSelected: (SuggestionItem) -> Unit,
    onDismiss: () -> Unit
) {
    if (suggestions.isEmpty()) return

    // Calculate position manually if needed, or rely on anchoring
    // For simplicity in this terminal UI, we'll place it above the input
    // using a Popup with specific alignment or just overlapping content
    
    // Using a simple Box with absolute positioning might be tricky without coordinates,
    // so we'll behave like a standard dropdown anchored to the parent.
    // In RichChatInput, this should be placed inside a Box wrapping the input.
    
    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = Modifier
                .width(300.dp) // Fixed width for suggestions
                .heightIn(max = 200.dp)
                .background(TerminalColors.background, RectangleShape)
                .border(TerminalSpacing.borderStandard, TerminalColors.border, RectangleShape)
        ) {
            LazyColumn {
                items(suggestions) { item ->
                    SuggestionRow(item, onSuggestionSelected)
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    item: SuggestionItem,
    onClick: (SuggestionItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
            .padding(TerminalSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (item.type) {
                SuggestionType.COMMAND -> "/"
                SuggestionType.MODE -> "@"
            },
            style = TerminalTypography.bodyMedium,
            color = TerminalColors.accentGreen
        )
        Spacer(modifier = Modifier.width(TerminalSpacing.sm))
        Column {
            Text(
                text = item.label,
                style = TerminalTypography.bodyMedium,
                color = TerminalColors.white
            )
            item.description?.let { desc ->
                Text(
                    text = desc,
                    style = TerminalTypography.labelSmall,
                    color = TerminalColors.grey
                )
            }
        }
    }
}
