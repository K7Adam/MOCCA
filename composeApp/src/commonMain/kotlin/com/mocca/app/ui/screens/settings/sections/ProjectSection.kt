package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.mocca.app.domain.model.Project
import com.mocca.app.ui.components.modern.MoccaCompactButton
import com.mocca.app.ui.components.modern.ModuleCard
import com.mocca.app.ui.components.modern.ModuleRowItem
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Settings section: Current project
 * 
 * Displays current project name and path with ability to update path.
 */
@Composable
fun ProjectSection(
    currentProject: Project,
    editingProjectPath: String,
    onSetEditingProjectPath: (String) -> Unit,
    onSaveProjectPath: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "PROJECT",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        ModuleCard(title = "CURRENT PROJECT") {
            Column(modifier = Modifier.padding(AppSpacing.sm)) {
                ModuleRowItem(
                    title = "NAME",
                    subtitle = currentProject.displayName,
                    isEnabled = true,
                    showToggle = false
                )
                HorizontalDivider(color = AppColors.border, thickness = AppSpacing.borderThin)
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Text(
                    text = "PROJECT PATH",
                    color = AppColors.textSecondary,
                    style = AppTypography.labelSmall,
                    modifier = Modifier.padding(horizontal = AppSpacing.sm)
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                OutlinedTextField(
                    value = editingProjectPath,
                    onValueChange = onSetEditingProjectPath,
                    placeholder = { Text("/path/to/project", style = AppTypography.bodySmall, color = AppColors.textTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = AppTypography.bodySmall.copy(color = AppColors.textPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onSaveProjectPath() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.accentGreen,
                        unfocusedBorderColor = AppColors.border,
                        cursorColor = AppColors.accentGreen,
                        focusedContainerColor = AppColors.background,
                        unfocusedContainerColor = AppColors.background
                    ),
                    shape = AppShapes.medium
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                MoccaCompactButton(
                    text = "UPDATE PATH",
                    onClick = onSaveProjectPath,
                    enabled = editingProjectPath.isNotBlank() &&
                        editingProjectPath != (currentProject.path ?: currentProject.directory ?: ""),
                    height = AppSpacing.buttonHeightSmall
                )
            }
        }
    }
}
