package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mocca.app.domain.model.UserPreferences
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.components.modern.ModuleRowItem
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Settings section: Chat
 * 
 * Messaging preferences (auto scroll, confirm delete, show thinking blocks).
 */
@Composable
fun ChatSection(
    preferences: UserPreferences,
    onSetAutoScroll: (Boolean) -> Unit,
    onSetConfirmDelete: (Boolean) -> Unit,
    onSetShowThinkingBlocks: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "CHAT",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "MESSAGING") {
            // Auto Scroll
            ModuleRowItem(
                title = "AUTO SCROLL",
                subtitle = "Scroll to bottom on new messages",
                isEnabled = preferences.autoScroll,
                onToggle = { onSetAutoScroll(!preferences.autoScroll) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Confirm Delete
            ModuleRowItem(
                title = "CONFIRM DELETE",
                subtitle = "Ask before deleting sessions",
                isEnabled = preferences.confirmDelete,
                onToggle = { onSetConfirmDelete(!preferences.confirmDelete) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Show Thinking Blocks
            ModuleRowItem(
                title = "SHOW THINKING",
                subtitle = "Display AI reasoning blocks",
                isEnabled = preferences.showThinkingBlocks,
                onToggle = { onSetShowThinkingBlocks(!preferences.showThinkingBlocks) }
            )
        }
    }
}
