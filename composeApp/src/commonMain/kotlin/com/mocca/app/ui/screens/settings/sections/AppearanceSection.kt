package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mocca.app.domain.model.UserPreferences
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.components.modern.ModuleRowItem
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Settings section: Appearance
 * 
 * Display preferences (token counts, timestamps, compact mode, API key masking),
 * font scale slider.
 */
@Composable
fun AppearanceSection(
    preferences: UserPreferences,
    onSetShowTokenCounts: (Boolean) -> Unit,
    onSetShowTimestamps: (Boolean) -> Unit,
    onSetCompactMode: (Boolean) -> Unit,
    onSetHideApiKeys: (Boolean) -> Unit,
    onSetFontScale: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "APPEARANCE",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "DISPLAY") {
            // Show Token Counts
            ModuleRowItem(
                title = "SHOW TOKEN COUNTS",
                subtitle = "Display input/output tokens in chat",
                isEnabled = preferences.showTokenCounts,
                onToggle = { onSetShowTokenCounts(!preferences.showTokenCounts) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Show Timestamps
            ModuleRowItem(
                title = "SHOW TIMESTAMPS",
                subtitle = "Display message timestamps",
                isEnabled = preferences.showTimestamps,
                onToggle = { onSetShowTimestamps(!preferences.showTimestamps) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Compact Mode
            ModuleRowItem(
                title = "COMPACT MODE",
                subtitle = "Reduced padding for higher density",
                isEnabled = preferences.compactMode,
                onToggle = { onSetCompactMode(!preferences.compactMode) }
            )
            
            HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
            
            // Hide API Keys
            ModuleRowItem(
                title = "HIDE API KEYS",
                subtitle = "Mask sensitive keys in settings",
                isEnabled = preferences.hideApiKeys,
                onToggle = { onSetHideApiKeys(!preferences.hideApiKeys) }
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.cardGap))
        
        // Font Scale Slider
        ModuleCard(title = "FONT SIZE") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEXT SCALE",
                        color = AppColors.textPrimary,
                        style = AppTypography.bodyMedium
                    )
                    Text(
                        text = "${preferences.fontScalePercent}%",
                        color = AppColors.accentGreen,
                        style = AppTypography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                
                // Font scale slider
                var sliderValue by remember { mutableStateOf(preferences.fontScale) }
                
                androidx.compose.material3.Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onSetFontScale(sliderValue) },
                    valueRange = 0.8f..1.4f,
                    steps = 5,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = AppColors.accentGreen,
                        activeTrackColor = AppColors.accentGreen,
                        inactiveTrackColor = AppColors.textSecondaryDark
                    )
                )
                
                // Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Small", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                    Text("Default", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                    Text("Large", color = AppColors.textTertiary, style = AppTypography.labelSmall)
                }
            }
        }
    }
}
