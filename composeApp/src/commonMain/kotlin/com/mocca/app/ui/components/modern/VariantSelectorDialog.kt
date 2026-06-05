package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.AiModelVariantOption
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.moccaClickable

@Composable
fun VariantSelectorDialog(
    variants: List<*>,
    selectedVariantId: String?,
    onVariantSelected: (variantId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val normalizedVariants = variants.mapNotNull { variant ->
        when (variant) {
            is AiModelVariantOption -> variant
            is String -> AiModelVariantOption(id = variant, name = variant)
            else -> null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.background,
        contentColor = AppColors.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.outline) },
        shape = AppShapes.bottomSheetExpanded
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Variant",
                    style = AppTypography.titleMedium,
                    color = AppColors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                MoccaIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    contentDescription = "Close",
                    iconColor = AppColors.onSurfaceVariant
                )
            }

            HorizontalDivider(thickness = AppSpacing.borderThin, color = AppColors.outline)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(AppSpacing.sm)
            ) {
                if (normalizedVariants.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        Text(
                            text = "No variants available",
                            style = AppTypography.labelSmall,
                            color = AppColors.onSurfaceVariant,
                            modifier = Modifier.padding(AppSpacing.md)
                        )
                    }
                } else {
                    item(key = "available", contentType = "section-header") {
                        Text(
                            text = "AVAILABLE",
                            style = AppTypography.labelSmall,
                            color = AppColors.primary,
                            modifier = Modifier.padding(
                                start = AppSpacing.sm,
                                top = AppSpacing.sm,
                                bottom = AppSpacing.xs
                            )
                        )
                    }

                    items(
                        items = normalizedVariants,
                        key = { variant -> variant.id },
                        contentType = { "variant-row" }
                    ) { variant ->
                        val isSelected = variant.id == selectedVariantId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .moccaClickable(
                                    onClick = {
                                        onVariantSelected(variant.id)
                                        onDismiss()
                                    },
                                    pressedScale = 0.99f
                                )
                                .background(
                                    if (isSelected) AppColors.primary.copy(alpha = 0.18f) else AppColors.background
                                )
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = variant.name.ifBlank { variant.id },
                                    style = AppTypography.bodySmall,
                                    color = if (isSelected) AppColors.primary else AppColors.onSurface
                                )
                                variant.description?.let { description ->
                                    Text(
                                        text = description,
                                        style = AppTypography.labelSmall,
                                        color = AppColors.outline,
                                        maxLines = 2
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = AppColors.primary,
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
