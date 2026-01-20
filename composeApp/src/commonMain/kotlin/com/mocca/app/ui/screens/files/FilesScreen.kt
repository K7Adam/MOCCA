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
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalShapes
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography
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
                .background(TerminalColors.background)
                .padding(TerminalSpacing.lg)
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
                        iconColor = TerminalColors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(TerminalSpacing.md))
                    Text(
                        text = "FILE EXPLORER",
                        style = TerminalTypography.labelLarge,
                        color = TerminalColors.white,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                TerminalIconButton(
                    icon = Icons.Default.Refresh,
                    onClick = { screenModel.loadFiles(state.currentPath) },
                    contentDescription = "REFRESH",
                    iconColor = TerminalColors.textSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            // Breadcrumb navigation
            TerminalBreadcrumbBar(
                pathHistory = state.pathHistory,
                canNavigateUp = state.pathHistory.size > 1,
                onNavigateUp = { screenModel.navigateUp() }
            )
            
            Spacer(modifier = Modifier.height(TerminalSpacing.md))
            
            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(TerminalShapes.card)
                    .background(TerminalColors.surfaceContainer, TerminalShapes.card)
                    .border(TerminalSpacing.borderThin, TerminalColors.border, TerminalShapes.card)
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
            .clip(TerminalShapes.pill)
            .background(TerminalColors.surfaceVariant, TerminalShapes.pill)
            .border(TerminalSpacing.borderThin, TerminalColors.border, TerminalShapes.pill)
            .padding(horizontal = TerminalSpacing.md, vertical = TerminalSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canNavigateUp) {
            TerminalIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onNavigateUp,
                size = 24.dp,
                iconColor = TerminalColors.textSecondary
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
        }
        
        Icon(
            Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = TerminalColors.statusOnline
        )
        
        Spacer(modifier = Modifier.width(TerminalSpacing.xs))
        
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pathHistory.lastOrNull()?.let { path ->
                if (path.isNotEmpty()) {
                    path.split("/").forEach { segment ->
                        Text(
                            text = " / ",
                            color = TerminalColors.textTertiary,
                            style = TerminalTypography.bodySmall
                        )
                        Text(
                            text = segment,
                            style = TerminalTypography.bodySmall,
                            color = TerminalColors.white,
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
                style = TerminalTypography.bodyMedium,
                color = TerminalColors.textTertiary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(TerminalSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TerminalSpacing.xs)
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
    val iconColor = if (file.isDirectory) TerminalColors.accentGreen else TerminalColors.textSecondary
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TerminalShapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = TerminalSpacing.sm, horizontal = TerminalSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(TerminalSpacing.md))
        
        Text(
            text = file.name,
            color = TerminalColors.white,
            style = TerminalTypography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        if (file.size != null) {
            Text(
                text = formatFileSize(file.size),
                color = TerminalColors.textTertiary,
                style = TerminalTypography.labelSmall
            )
        }
        
        if (file.isDirectory) {
            Spacer(modifier = Modifier.width(TerminalSpacing.sm))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TerminalColors.textTertiary,
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
                .background(TerminalColors.surfaceVariant)
                .padding(horizontal = TerminalSpacing.md, vertical = TerminalSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                getFileIcon(fileName),
                contentDescription = null,
                tint = TerminalColors.accentGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(TerminalSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName.uppercase(),
                    style = TerminalTypography.bodyMedium,
                    color = TerminalColors.white,
                    fontWeight = FontWeight.Bold
                )
                if (language != null) {
                    Text(
                        text = language.uppercase(),
                        style = TerminalTypography.labelSmall,
                        color = TerminalColors.textTertiary
                    )
                }
            }
            TerminalIconButton(
                icon = Icons.Default.Close,
                onClick = onClose,
                iconColor = TerminalColors.white,
                size = 32.dp
            )
        }
        
        // File Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(TerminalColors.background)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(TerminalSpacing.md)
        ) {
            Text(
                text = content,
                style = TerminalTypography.code, // Use code typography
                color = TerminalColors.whiteMuted
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
