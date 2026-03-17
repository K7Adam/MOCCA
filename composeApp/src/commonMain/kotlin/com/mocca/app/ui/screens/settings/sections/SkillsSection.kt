package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Settings section: Skills
 * 
 * Browse server-side agent skills.
 */
@Composable
fun SkillsSection(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "SKILLS",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "SERVER SKILLS") {
            Text(
                text = "View agent skills registered on the OpenCode server.",
                style = AppTypography.labelSmall,
                color = AppColors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            MoccaButton(
                text = "BROWSE SKILLS",
                onClick = { navigator.push(com.mocca.app.ui.screens.skills.SkillsScreen) },
                height = AppSpacing.buttonHeightCompact
            )
        }
    }
}
