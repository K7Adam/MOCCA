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
            text = "Appearance",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "Display") {
            // Show Token Counts
            ModuleRowItem(
                title = "Show token counts",
                subtitle = "Display input/output tokens in chat",
                isEnabled = preferences.showTokenCounts,
                onToggle = { onSetShowTokenCounts(!preferences.showTokenCounts) }
            )
            
            HorizontalDivider(color = AppColors.outline, thickness = AppSpacing.borderThin)
            
            // Show Timestamps
            ModuleRowItem(
                title = "Show timestamps",
                subtitle = "Display message timestamps",
                isEnabled = preferences.showTimestamps,
                onToggle = { onSetShowTimestamps(!preferences.showTimestamps) }
            )
            
            HorizontalDivider(color = AppColors.outline, thickness = AppSpacing.borderThin)
            
            // Compact Mode
            ModuleRowItem(
                title = "Compact mode",
                subtitle = "Reduced padding for higher density",
                isEnabled = preferences.compactMode,
                onToggle = { onSetCompactMode(!preferences.compactMode) }
            )
            
            HorizontalDivider(color = AppColors.outline, thickness = AppSpacing.borderThin)
            
            // Hide API Keys
            ModuleRowItem(
                title = "Hide API keys",
                subtitle = "Mask sensitive keys in settings",
                isEnabled = preferences.hideApiKeys,
                onToggle = { onSetHideApiKeys(!preferences.hideApiKeys) }
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.cardGap))
        
        // Font Scale Slider
        ModuleCard(title = "Font size") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Text scale",
                        color = AppColors.onSurface,
                        style = AppTypography.bodyMedium
                    )
                    Text(
                        text = "${preferences.fontScalePercent}%",
                        color = AppColors.primary,
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
                        thumbColor = AppColors.primary,
                        activeTrackColor = AppColors.primary,
                        inactiveTrackColor = AppColors.onSurfaceVariantDark
                    )
                )
                
                // Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Small", color = AppColors.outline, style = AppTypography.labelSmall)
                    Text("Default", color = AppColors.outline, style = AppTypography.labelSmall)
                    Text("Large", color = AppColors.outline, style = AppTypography.labelSmall)
                }
            }
        }
    }
}
