package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.screens.settings.FeatureFlagsScreen
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Settings section: Experimental
 * 
 * Feature flags management.
 */
@Composable
fun ExperimentalSection(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "EXPERIMENTAL",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "FEATURE FLAGS") {
            Text(
                text = "Manage server-wide experimental feature flags and global config options.",
                style = AppTypography.labelSmall,
                color = AppColors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            MoccaButton(
                text = "MANAGE FLAGS",
                onClick = { navigator.push(FeatureFlagsScreen) },
                height = AppSpacing.buttonHeightCompact
            )
        }
    }
}
