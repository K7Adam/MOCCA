package com.mocca.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import androidx.compose.ui.platform.LocalClipboardManager

@Composable
internal fun CodeBlock(
    code: String,
    label: String? = null,
    language: String = "text",
    maxLines: Int = 50
) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!label.isNullOrBlank()) {
                Text(
                    text = label,
                    style = AppTypography.labelSmall,
                    color = AppColors.grey,
                    modifier = Modifier.padding(bottom = AppSpacing.xs)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .clip(AppShapes.small)
                    .clickable { 
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(horizontal = AppSpacing.sm, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedContent(
                    targetState = copied,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "copyIcon"
                ) { isCopied ->
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(12.dp),
                        tint = if (isCopied) AppColors.accentGreen else AppColors.textTertiary
                    )
                }
                Text(
                    text = if (copied) "COPIED" else "COPY",
                    style = AppTypography.labelExtraSmall,
                    color = if (copied) AppColors.accentGreen else AppColors.textTertiary
                )
            }
        }
        
        Surface(
            shape = AppShapes.medium,
            color = AppColors.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            val scrollState = rememberScrollState()
            Text(
                text = code.lines().take(maxLines).joinToString("\n"),
                style = AppTypography.code,
                color = AppColors.white,
                modifier = Modifier
                    .padding(AppSpacing.md)
                    .horizontalScroll(scrollState)
            )
        }
    }
}

@Composable
internal fun ErrorBlock(error: String) {
    Surface(
        shape = AppShapes.medium,
        color = AppColors.error.copy(alpha = 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AppColors.error
            )
            Text(
                text = error,
                style = AppTypography.bodySmall,
                color = AppColors.error
            )
        }
    }
}

@Composable
internal fun DiffView(oldText: String, newText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .border(AppSpacing.borderThin, AppColors.border.copy(alpha = 0.5f), AppShapes.medium),
        verticalArrangement = Arrangement.spacedBy(0.dp) // Seamless
    ) {
        // Removed lines
        if (oldText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.diffDeletion.copy(alpha = 0.3f))
                    .padding(AppSpacing.sm)
            ) {
                Row {
                    Text(
                        text = "-",
                        style = AppTypography.codeSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.diffDeletionText,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = oldText.trimEnd(),
                        style = AppTypography.codeSmall,
                        color = AppColors.diffDeletionText.copy(alpha = 0.9f),
                        fontFamily = AppTypography.monoFamily
                    )
                }
            }
        }
        
        // Added lines
        if (newText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.diffAddition.copy(alpha = 0.3f))
                    .padding(AppSpacing.sm)
            ) {
                Row {
                    Text(
                        text = "+",
                        style = AppTypography.codeSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.diffAdditionText,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = newText.trimEnd(),
                        style = AppTypography.codeSmall,
                        color = AppColors.diffAdditionText.copy(alpha = 1.0f),
                        fontFamily = AppTypography.monoFamily
                    )
                }
            }
        }
    }
}

@Composable
internal fun FileListView(output: String) {
    val files = output.trim().lines().filter { it.isNotBlank() }.take(50)
    val fileCount = output.trim().lines().count { it.isNotBlank() }
    
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(
            text = "Found $fileCount file${if (fileCount != 1) "s" else ""}",
            style = AppTypography.labelSmall,
            color = AppColors.grey
        )
        
        Surface(
            shape = AppShapes.medium,
            color = AppColors.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(AppSpacing.sm)
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                files.forEach { file ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        modifier = Modifier.padding(vertical = AppSpacing.xxs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    AppColors.white.copy(alpha = 0.6f),
                                    AppShapes.medium
                                )
                        )
                        Text(
                            text = file.trim(),
                            style = AppTypography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = AppColors.white
                        )
                    }
                }
                if (fileCount > 50) {
                    Text(
                        text = "... and ${fileCount - 50} more",
                        style = AppTypography.labelSmall,
                        color = AppColors.grey
                    )
                }
            }
        }
    }
}

@Composable
internal fun GrepResultsView(output: String) {
    val lines = output.trim().lines().filter { it.isNotBlank() }.take(30)
    val totalMatches = output.trim().lines().count { it.isNotBlank() }
    
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(
            text = "Found $totalMatches match${if (totalMatches != 1) "es" else ""}",
            style = AppTypography.labelSmall,
            color = AppColors.grey
        )
        
        Surface(
            shape = AppShapes.medium,
            color = AppColors.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(AppSpacing.sm)
                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                lines.forEach { line ->
                    // Parse grep format: file:line:content
                    val parts = line.split(":", limit = 3)
                    Row(
                        modifier = Modifier.padding(vertical = AppSpacing.xxs),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        if (parts.size >= 2) {
                            Text(
                                text = parts[0].substringAfterLast('/'),
                                style = AppTypography.labelSmall,
                                color = AppColors.white
                            )
                            if (parts.size >= 3) {
                                Text(
                                    text = ":${parts[1]}",
                                    style = AppTypography.labelSmall,
                                    color = AppColors.grey
                                )
                            }
                        }
                    }
                    Text(
                        text = if (parts.size >= 3) parts[2] else line,
                        style = AppTypography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = AppSpacing.sm),
                        color = AppColors.whiteDim
                    )
                }
            }
        }
    }
}
