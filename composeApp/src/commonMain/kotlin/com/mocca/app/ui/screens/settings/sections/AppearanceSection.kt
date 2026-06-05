package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.UserPreferences
import com.mocca.app.ui.screens.settings.SettingsCard
import com.mocca.app.ui.screens.settings.SettingsRowItem
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Settings section: Appearance
 * 
 * Display preferences (token counts, timestamps, compact mode, API key masking),
 * font scale slider, and code font family picker.
 */
@Composable
fun AppearanceSection(
    preferences: UserPreferences,
    onSetShowTokenCounts: (Boolean) -> Unit,
    onSetShowTimestamps: (Boolean) -> Unit,

    onSetCodeFontFamily: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Appearance",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        SettingsCard(title = "Display") {
            SettingsRowItem(
                title = "Show Token Counts",
                subtitle = "Display input/output tokens in chat",
                isEnabled = preferences.showTokenCounts,
                onToggle = { onSetShowTokenCounts(!preferences.showTokenCounts) }
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            
            // Show Timestamps
            SettingsRowItem(
                title = "Show Timestamps",
                subtitle = "Display message timestamps",
                isEnabled = preferences.showTimestamps,
                onToggle = { onSetShowTimestamps(!preferences.showTimestamps) }
            )

        }
        

        
        Spacer(modifier = Modifier.height(AppSpacing.cardGap))
        
        // Code Font Picker
        SettingsCard(title = "Code Font") {
            Text(
                text = "Font used in code editor, terminal, and file viewer",
                color = AppColors.onSurfaceVariant,
                style = AppTypography.labelSmall
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            UserPreferences.CODE_FONT_OPTIONS.forEachIndexed { index, (key, displayName) ->
                val isSelected = preferences.codeFontFamily == key
                val fontFamily = AppTypography.monoFamilyFor(key)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetCodeFontFamily(key) }
                        .padding(vertical = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                color = if (isSelected) AppColors.primary else AppColors.bgRaised,
                                shape = CircleShape
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            color = if (isSelected) AppColors.primary else AppColors.onSurface,
                            style = AppTypography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AppTypography.fontPreviewText,
                            color = AppColors.onSurfaceVariant,
                            style = AppTypography.code.copy(fontFamily = fontFamily),
                            maxLines = 1
                        )
                    }
                }
                
                if (index < UserPreferences.CODE_FONT_OPTIONS.lastIndex) {
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                }
            }
        }
    }
}
