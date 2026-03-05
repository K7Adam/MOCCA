package com.mocca.app.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing

@Composable
internal fun AttachmentPreviewStrip(
    files: List<AttachedFile>,
    onRemove: (AttachedFile) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        items(files, key = { it.id }) { file ->
            AttachmentPreviewChip(file = file, onRemove = { onRemove(file) })
        }
    }
}

@Composable
internal fun AttachmentPreviewChip(
    file: AttachedFile,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(AppShapes.medium)
            .border(
                AppSpacing.borderThin,
                if (file.isImage) AppColors.accentGreen.copy(alpha = 0.4f) else AppColors.border,
                AppShapes.medium
            )
            .background(AppColors.surface, AppShapes.medium)
    ) {
        if (file.isImage && file.dataUrl != null) {
            AsyncImage(
                model = file.dataUrl,
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center)
            )
        }
        // Remove button overlay
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(AppColors.error)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRemove
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove attachment",
                tint = AppColors.white,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
