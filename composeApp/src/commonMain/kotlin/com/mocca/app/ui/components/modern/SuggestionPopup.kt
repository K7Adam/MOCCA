package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mocca.app.ui.components.navigation.NavConstants
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

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
 * Premium styled with subtle border and consistent theming.
 * 
 * IMPORTANT: This popup is triggered by the "/" and "@" buttons in the chat input.
 * It must be positioned above the input field at the bottom of the screen.
 *
 * @param suggestions List of suggestions to display
 * @param onSuggestionSelected Callback when a suggestion is selected
 * @param onDismiss Callback when the popup should be dismissed
 * @param offset Optional offset to position the popup (defaults to bottom-center above input)
 */
@Composable
fun SuggestionPopup(
    suggestions: List<SuggestionItem>,
    onSuggestionSelected: (SuggestionItem) -> Unit,
    onDismiss: () -> Unit,
    offset: IntOffset = IntOffset(0, -280) // Default offset to position above bottom bar
) {
    if (suggestions.isEmpty()) return

    Popup(
        alignment = Alignment.BottomCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
        offset = offset
    ) {
        Box(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .heightIn(min = NavConstants.PopupItemMinHeight, max = 200.dp)
                .background(AppColors.surface, AppShapes.medium)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AppColors.borderLight.copy(alpha = 0.4f),
                            AppColors.border.copy(alpha = 0.2f)
                        )
                    ),
                    shape = AppShapes.medium
                )
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
            .heightIn(min = NavConstants.PopupItemMinHeight)
            .clickable { onClick(item) }
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Prefix badge with icon styling
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = AppColors.accentGreen.copy(alpha = 0.1f),
                    shape = AppShapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (item.type) {
                    SuggestionType.COMMAND -> "/"
                    SuggestionType.MODE -> "@"
                },
                style = AppTypography.labelMedium,
                color = AppColors.accentGreen,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = item.label,
                style = AppTypography.bodySmall,
                color = AppColors.textPrimary
            )
            item.description?.let { desc ->
                Text(
                    text = desc,
                    style = AppTypography.labelSmall,
                    color = AppColors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
