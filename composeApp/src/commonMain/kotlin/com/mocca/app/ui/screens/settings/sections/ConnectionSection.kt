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
 * Settings section: Connection
 * 
 * Network preferences (auto reconnect, data saver mode).
 */
@Composable
fun ConnectionSection(
    preferences: UserPreferences,
    onSetAutoReconnect: (Boolean) -> Unit,
    onSetDataSaverMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Connection",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "Network") {
            // Auto Reconnect
            ModuleRowItem(
                title = "Auto reconnect",
                subtitle = "Reconnect when connection drops",
                isEnabled = preferences.autoReconnect,
                onToggle = { onSetAutoReconnect(!preferences.autoReconnect) }
            )
            
            HorizontalDivider(color = AppColors.outline, thickness = AppSpacing.borderThin)
            
            // Data Saver Mode
            ModuleRowItem(
                title = "Data saver",
                subtitle = "Reduce background network usage",
                isEnabled = preferences.dataSaverMode,
                onToggle = { onSetDataSaverMode(!preferences.dataSaverMode) }
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            
            Text(
                text = "Data Saver disables background sync and reduces network calls.",
                color = AppColors.outline,
                style = AppTypography.labelSmall
            )
        }
    }
}
