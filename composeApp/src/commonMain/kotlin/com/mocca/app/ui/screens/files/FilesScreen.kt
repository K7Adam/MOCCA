package com.mocca.app.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.FileInfo
import com.mocca.app.ui.components.LoadingScreen
import com.mocca.app.ui.components.terminal.TerminalHeader
import com.mocca.app.ui.components.terminal.TerminalIconButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

class FilesScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<FilesScreenModel>()
        val state by screenModel.state.collectAsState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .padding(AppSpacing.lg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TerminalIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navigator.pop() },
                        iconColor = AppColors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Text(
                        text = "FILE EXPLORER",
                        style = AppTypography.labelLarge,
                        color = AppColors.white,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                TerminalIconButton(
                    icon = Icons.Default.Refresh,
                    onClick = { screenModel.loadFiles(state.currentPath) },
                    contentDescription = "REFRESH",
                    iconColor = AppColors.textSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            // Breadcrumb navigation
            TerminalBreadcrumbBar(
                pathHistory = state.pathHistory,
                canNavigateUp = state.pathHistory.size > 1,
                onNavigateUp = { screenModel.navigateUp() }
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(AppShapes.card)
                    .background(AppColors.surfaceContainer, AppShapes.card)
                    .border(AppSpacing.borderThin, AppColors.border, AppShapes.card)
            ) {
                if (state.isLoading) {
                    LoadingScreen()
                } else if (state.selectedFile != null && state.fileContent != null) {
                    // File viewer
                    TerminalFileViewer(
                        fileName = state.selectedFile!!.name,
                        content = state.fileContent!!.content,
                        language = state.fileContent!!.language,
                        onClose = { screenModel.closeFileViewer() }
                    )
                } else {
                    // File list
                    TerminalFilesList(
                        files = state.files,
                        onFileClick = { screenModel.selectFile(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalBreadcrumbBar(
    pathHistory: List<String>,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.pill)
            .background(AppColors.surfaceVariant, AppShapes.pill)
            .border(AppSpacing.borderThin, AppColors.border, AppShapes.pill)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canNavigateUp) {
            TerminalIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onNavigateUp,
                size = 24.dp,
                iconColor = AppColors.textSecondary
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
        }
        
        Icon(
            Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = AppColors.statusOnline
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.xs))
        
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pathHistory.lastOrNull()?.let { path ->
                if (path.isNotEmpty()) {
                    path.split("/").forEach { segment ->
                        Text(
                            text = " / ",
                            color = AppColors.textTertiary,
                            style = AppTypography.bodySmall
                        )
                        Text(
                            text = segment,
                            style = AppTypography.bodySmall,
                            color = AppColors.white,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalFilesList(
    files: List<FileInfo>,
    onFileClick: (FileInfo) -> Unit
) {
    if (files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DIRECTORY EMPTY",
                style = AppTypography.bodyMedium,
                color = AppColors.textTertiary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            items(
                items = files,
                key = { it.path },
                contentType = { if (it.isDirectory) "directory" else "file" }
            ) { file ->
                TerminalFileItem(
                    file = file,
                    onClick = { onFileClick(file) }
                )
            }
        }
    }
}

@Composable
private fun TerminalFileItem(
    file: FileInfo,
    onClick: () -> Unit
) {
    val icon = if (file.isDirectory) Icons.Default.Folder else getFileIcon(file.name)
    val iconColor = if (file.isDirectory) AppColors.accentGreen else AppColors.textSecondary
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.md))
        
        Text(
            text = file.name,
            color = AppColors.white,
            style = AppTypography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        if (file.size != null) {
            Text(
                text = formatFileSize(file.size),
                color = AppColors.textTertiary,
                style = AppTypography.labelSmall
            )
        }
        
        if (file.isDirectory) {
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TerminalFileViewer(
    fileName: String,
    content: String,
    language: String?,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Viewer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surfaceVariant)
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                getFileIcon(fileName),
                contentDescription = null,
                tint = AppColors.accentGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName.uppercase(),
                    style = AppTypography.bodyMedium,
                    color = AppColors.white,
                    fontWeight = FontWeight.Bold
                )
                if (language != null) {
                    Text(
                        text = language.uppercase(),
                        style = AppTypography.labelSmall,
                        color = AppColors.textTertiary
                    )
                }
            }
            TerminalIconButton(
                icon = Icons.Default.Close,
                onClick = onClose,
                iconColor = AppColors.white,
                size = 32.dp
            )
        }
        
        // File Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AppColors.background)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(AppSpacing.md)
        ) {
            Text(
                text = content,
                style = AppTypography.code, // Use code typography
                color = AppColors.whiteMuted
            )
        }
    }
}

private fun getFileIcon(fileName: String): ImageVector {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "kt", "java", "py", "js", "ts", "tsx", "jsx" -> Icons.Default.Code
        "md", "txt", "json", "yaml", "yml", "xml" -> Icons.Default.Description
        "png", "jpg", "jpeg", "gif", "svg" -> Icons.Default.Image
        "gradle", "properties" -> Icons.Default.Settings
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
