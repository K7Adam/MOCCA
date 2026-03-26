package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Mode
import com.mocca.app.ui.components.modern.MoccaIconButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
fun AgentSelectorBottomSheet(
    modes: List<Mode>,
    selectedModeId: String?,
    onModeSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.background,
        contentColor = AppColors.onSurface,
        scrimColor = AppColors.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.outline) },
        shape = AppShapes.bottomSheetExpanded
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "// SELECT AGENT",
                    style = AppTypography.titleMedium,
                    color = AppColors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                MoccaIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = AppColors.onSurfaceVariant
                )
            }
            
            HorizontalDivider(thickness = AppSpacing.borderThin, color = AppColors.outline)
            
            // Agent list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(AppSpacing.sm)
            ) {
                item {
                    Text(
                        text = "// AVAILABLE",
                        style = AppTypography.labelSmall,
                        color = AppColors.primary,
                        modifier = Modifier.padding(start = AppSpacing.sm, top = AppSpacing.sm, bottom = AppSpacing.xs)
                    )
                }
                
                items(modes) { mode ->
                    val isSelected = mode.id == selectedModeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onModeSelected(mode.id)
                                onDismiss()
                            }
                            .background(if (isSelected) AppColors.primary.copy(alpha = 0.2f) else AppColors.background)
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "> ${mode.name.uppercase()}",
                            style = AppTypography.bodySmall,
                            color = if (isSelected) AppColors.primary else AppColors.onSurface
                        )
                        
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AppColors.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
