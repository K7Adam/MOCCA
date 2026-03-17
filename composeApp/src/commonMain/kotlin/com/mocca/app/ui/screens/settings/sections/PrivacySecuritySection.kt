package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mocca.app.domain.model.UserPreferences
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.components.modern.ModuleRowItem
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Settings section: Privacy & Security
 * 
 * Security preferences (screen security, clear cache on exit),
 * data management actions (clear cache, reset preferences).
 */
@Composable
fun PrivacySecuritySection(
    preferences: UserPreferences,
    onSetScreenSecurity: (Boolean) -> Unit,
    onSetClearCacheOnExit: (Boolean) -> Unit,
    onShowClearCacheDialog: () -> Unit,
    onResetPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "PRIVACY & SECURITY",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "SECURITY") {
            // Screen Security
            ModuleRowItem(
                title = "SCREEN SECURITY",
                subtitle = "Prevent screenshots",
                isEnabled = preferences.screenSecurity,
                onToggle = { onSetScreenSecurity(!preferences.screenSecurity) }
            )
            
            HorizontalDivider(color = AppColors.outline, thickness = AppSpacing.borderThin)
            
            // Clear Cache on Exit
            ModuleRowItem(
                title = "CLEAR CACHE ON EXIT",
                subtitle = "Remove local data when app closes",
                isEnabled = preferences.clearCacheOnExit,
                onToggle = { onSetClearCacheOnExit(!preferences.clearCacheOnExit) }
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.cardGap))
        
        // Data Management
        ModuleCard(title = "DATA") {
            // Clear Cache Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CLEAR ALL CACHE",
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
                    text = "CLEAR",
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
                        text = "RESET PREFERENCES",
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
                    text = "RESET",
                    onClick = onResetPreferences,
                    height = AppSpacing.buttonHeightSmall
                )
            }
        }
    }
}
