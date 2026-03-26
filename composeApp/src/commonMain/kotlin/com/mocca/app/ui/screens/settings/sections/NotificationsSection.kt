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
 * Settings section: Notifications
 * 
 * Alert preferences (permission requests, session complete, connection lost).
 */
@Composable
fun NotificationsSection(
    preferences: UserPreferences,
    onSetNotifyPermissions: (Boolean) -> Unit,
    onSetNotifySessionComplete: (Boolean) -> Unit,
    onSetNotifyConnectionLost: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Notifications",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "Alerts") {
            // Permission Notifications
            ModuleRowItem(
                title = "Permission requests",
                subtitle = "Alert when AI needs approval",
                isEnabled = preferences.notifyPermissions,
                onToggle = { onSetNotifyPermissions(!preferences.notifyPermissions) }
            )
            
            HorizontalDivider(color = AppColors.outline, thickness = AppSpacing.borderThin)
            
            // Session Complete
            ModuleRowItem(
                title = "Session complete",
                subtitle = "Alert when AI finishes task",
                isEnabled = preferences.notifySessionComplete,
                onToggle = { onSetNotifySessionComplete(!preferences.notifySessionComplete) }
            )
            
            HorizontalDivider(color = AppColors.outline, thickness = AppSpacing.borderThin)
            
            // Connection Lost
            ModuleRowItem(
                title = "Connection lost",
                subtitle = "Alert on server disconnect",
                isEnabled = preferences.notifyConnectionLost,
                onToggle = { onSetNotifyConnectionLost(!preferences.notifyConnectionLost) }
            )
        }
    }
}
