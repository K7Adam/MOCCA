package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Settings section: Privacy & Security
 * 
 * Data management actions (clear cache, reset preferences).
 * Security toggles (screen security, clear cache on exit) were removed as dead fields.
 */
@Composable
fun PrivacySecuritySection(
    onShowClearCacheDialog: () -> Unit,
    onResetPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Privacy & security",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        // Data Management
        ModuleCard(title = "Data") {
            // Clear Cache Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Clear all cache",
                        color = AppColors.onSurface,
                        style = AppTypography.bodyMedium
                    )
                    Text(
                        text = "Remove cached sessions and messages",
                        color = AppColors.outline,
                        style = AppTypography.labelSmall
                    )
                }
                
                MoccaOutlinedButton(
                    text = "Clear",
                    onClick = onShowClearCacheDialog,
                    height = AppSpacing.buttonHeightSmall
                )
            }
            
            HorizontalDivider(color = AppColors.outline, thickness = AppSpacing.borderThin)
            
            // Reset Preferences
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reset preferences",
                        color = AppColors.onSurface,
                        style = AppTypography.bodyMedium
                    )
                    Text(
                        text = "Restore all settings to defaults",
                        color = AppColors.outline,
                        style = AppTypography.labelSmall
                    )
                }
                
                MoccaOutlinedButton(
                    text = "Reset",
                    onClick = onResetPreferences,
                    height = AppSpacing.buttonHeightSmall
                )
            }
        }
    }
}
