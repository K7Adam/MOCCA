package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Terminal-styled variant selection dialog.
 * Shows available variants for the selected model.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantSelectorDialog(
    variants: List<String>,
    selectedVariantId: String?,
    onVariantSelected: (variantId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.background,
        contentColor = AppColors.white,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.border) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
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
                    text = "// SELECT VARIANT",
                    style = AppTypography.titleMedium,
                    color = AppColors.white,
                    fontWeight = FontWeight.Bold
                )
                MoccaIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = AppColors.grey
                )
            }
            
            HorizontalDivider(
                thickness = AppSpacing.borderThin,
                color = AppColors.border
            )
            
            // Variant list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(AppSpacing.sm)
            ) {
                if (variants.isEmpty()) {
                    item {
                        Text(
                            text = "// NO VARIANTS AVAILABLE",
                            style = AppTypography.labelSmall,
                            color = AppColors.grey,
                            modifier = Modifier.padding(AppSpacing.md)
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "// AVAILABLE",
                            style = AppTypography.labelSmall,
                            color = AppColors.accentGreen,
                            modifier = Modifier.padding(
                                start = AppSpacing.sm,
                                top = AppSpacing.sm,
                                bottom = AppSpacing.xs
                            )
                        )
                    }
                    
                    items(variants) { variantId ->
                        val isSelected = variantId == selectedVariantId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onVariantSelected(variantId)
                                    onDismiss()
                                }
                                .background(
                                    if (isSelected) 
                                        AppColors.accentGreen.copy(alpha = 0.2f) 
                                    else 
                                        AppColors.background
                                )
                                .padding(
                                    horizontal = AppSpacing.md,
                                    vertical = AppSpacing.sm
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "> ${variantId.uppercase()}",
                                style = AppTypography.bodySmall,
                                color = if (isSelected) AppColors.accentGreen else AppColors.white
                            )
                            
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = AppColors.accentGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
