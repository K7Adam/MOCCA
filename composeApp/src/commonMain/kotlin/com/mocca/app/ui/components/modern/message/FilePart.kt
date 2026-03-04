package com.mocca.app.ui.components.modern.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

// ---------------------------------------------------------------------------
// File / image attachment renderer
// ---------------------------------------------------------------------------

@Composable
fun ModernFileBlock(part: MessagePart.File) {
    val isImage = part.mediaType.startsWith("image/")
    var showPreview by remember { mutableStateOf(false) }

    if (isImage && part.url != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium)
                .clickable { showPreview = true }
                .padding(AppSpacing.xxs)
        ) {
            AsyncImage(
                model = part.url,
                contentDescription = part.filename,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(AppShapes.medium),
                contentScale = ContentScale.Fit
            )
            if (part.filename != null) {
                Text(
                    text = part.filename,
                    style = AppTypography.labelExtraSmall,
                    color = AppColors.textTertiary,
                    modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                )
            }
        }
        if (showPreview) {
            Dialog(
                onDismissRequest = { showPreview = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.background.copy(alpha = 0.95f))
                        .clickable { showPreview = false },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = part.url,
                        contentDescription = part.filename,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clip(AppShapes.card),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "TAP TO CLOSE",
                        style = AppTypography.labelExtraSmall,
                        color = AppColors.textTertiary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = AppSpacing.xl)
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .border(AppSpacing.borderThin, AppColors.border, AppShapes.medium)
                .padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = part.filename ?: "ATTACHMENT",
                color = AppColors.textSecondary,
                style = AppTypography.bodySmall
            )
        }
    }
}
