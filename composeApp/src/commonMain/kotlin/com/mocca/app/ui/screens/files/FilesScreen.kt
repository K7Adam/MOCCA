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
        
        Scaffold(
            topBar = {
                GodHeader(
                    title = "Files",
                    onBackClick = { navigator.pop() },
                    subtitle = "mobile-agent-v2",
                    subtitleIcon = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = AppColors.white.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    actions = {
                        IconButton(onClick = { screenModel.loadFiles(state.currentPath) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AppColors.white)
                        }
                    }
                )
            },
            containerColor = AppColors.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Breadcrumb navigation
                GodBreadcrumbBar(
                    pathHistory = state.pathHistory,
                    canNavigateUp = state.pathHistory.size > 1,
                    onNavigateUp = { screenModel.navigateUp() }
                )
                
                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        LoadingScreen()
                    } else if (state.selectedFile != null && state.fileContent != null) {
                        // File viewer
                        GodFileViewer(
                            fileName = state.selectedFile!!.name,
                            content = state.fileContent!!.content,
                            language = state.fileContent!!.language,
                            onClose = { screenModel.closeFileViewer() }
                        )
                    } else {
                        // File list
                        GodFilesList(
                            files = state.files,
                            onFileClick = { screenModel.selectFile(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GodBreadcrumbBar(
    pathHistory: List<String>,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit
) {
    Surface(
        color = AppColors.background,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, AppColors.white.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (canNavigateUp) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier
                        .size(32.dp)
                        .background(AppColors.white.copy(alpha = 0.05f), AppShapes.circle)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Up",
                        tint = AppColors.white,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AppColors.accentGreen
            )
            
            pathHistory.lastOrNull()?.let { path ->
                if (path.isNotEmpty()) {
                    path.split("/").filter { it.isNotEmpty() }.forEach { segment ->
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = AppColors.white.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = segment,
                            style = AppTypography.labelMedium,
                            color = AppColors.white,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GodFilesList(
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
                style = AppTypography.labelMedium,
                color = AppColors.white.copy(alpha = 0.2f),
                letterSpacing = 1.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = files,
                key = { it.path },
                contentType = { if (it.isDirectory) "directory" else "file" }
            ) { file ->
                GodListItem(
                    title = file.name,
                    subtitle = if (file.isDirectory) "Folder" else formatFileSize(file.size ?: 0),
                    icon = {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else getFileIcon(file.name),
                            contentDescription = null,
                            tint = if (file.isDirectory) AppColors.accentGreen else AppColors.white.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        if (file.isDirectory) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = AppColors.white.copy(alpha = 0.2f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = { onFileClick(file) }
                )
            }
        }
    }
}

@Composable
private fun GodFileViewer(
    fileName: String,
    content: String,
    language: String?,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Viewer Header
        Surface(
            color = AppColors.surface,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, AppColors.white.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getFileIcon(fileName),
                    contentDescription = null,
                    tint = AppColors.accentGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = AppTypography.titleSmall,
                        color = AppColors.white,
                        fontWeight = FontWeight.Bold
                    )
                    if (language != null) {
                        Text(
                            text = language.uppercase(),
                            style = AppTypography.labelSmall,
                            color = AppColors.white.copy(alpha = 0.4f)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(AppColors.white.copy(alpha = 0.05f), AppShapes.circle)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppColors.white,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // File Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AppColors.background)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = content,
                style = AppTypography.code,
                color = AppColors.whiteDim
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
