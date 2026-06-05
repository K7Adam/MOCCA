package com.mocca.app.ui.screens.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.FileInfo
import com.mocca.app.ui.components.ErrorScreen
import com.mocca.app.ui.components.GodHeader
import com.mocca.app.ui.components.GodListItem
import com.mocca.app.ui.components.LoadingScreen
import com.mocca.app.ui.components.editor.CodeEditorView
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.ui.theme.moccaClickable
import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.*

class FilesScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<FilesScreenModel>()
        val state by screenModel.state.collectAsState()

        if (state.selectedFile != null) {
            BackHandler {
                screenModel.attemptCloseViewer()
            }
        }

        Scaffold(
            topBar = {
                GodHeader(
                    title = "Files",
                    onBackClick = {
                        if (state.selectedFile != null) {
                            screenModel.attemptCloseViewer()
                        } else {
                            navigator.pop()
                        }
                    },
                    modifier = Modifier.background(AppColors.surfaceContainer, AppShapes.none),
                    subtitle = "mobile-agent-v2",
                    subtitleIcon = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = AppColors.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(AppSpacing.iconSizeSmall)
                        )
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(AppSpacing.iconButtonSize)
                                .moccaClickable(onClick = { screenModel.loadFiles(state.currentPath) }, pressedScale = 0.92f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh files",
                                tint = AppColors.onSurface
                            )
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
                    onNavigateUp = { screenModel.navigateUp() },
                    modifier = Modifier.background(AppColors.surfaceContainer, AppShapes.none)
                )

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        LoadingScreen()
                    } else if (state.error != null && state.files.isEmpty()) {
                        ErrorScreen(
                            message = state.error!!,
                            onRetry = { screenModel.loadFiles(state.currentPath) }
                        )
                    } else if (state.selectedFile != null && state.fileContent != null) {
                        // File viewer
                        GodFileViewer(
                            fileName = state.selectedFile!!.name,
                            filePath = state.selectedFile!!.path,
                            fileSize = state.selectedFile!!.size,
                            content = state.fileContent!!.content,
                            language = state.detectedLanguage,
                            isEditing = state.isEditing,
                            editedContent = state.editedContent,
                            isSaving = state.isSaving,
                            saveError = state.saveError,
                            hasUnsavedChanges = state.hasUnsavedChanges,
                            showDiscardDialog = state.showDiscardDialog,
                            showExternalChangeDialog = state.showExternalChangeDialog,
                            onClose = { screenModel.attemptCloseViewer() },
                            onToggleEdit = { screenModel.toggleEdit() },
                            onContentChange = { screenModel.updateEditedContent(it) },
                            onSave = { screenModel.saveFile() },
                            onConfirmDiscard = { screenModel.confirmDiscard() },
                            onCancelDiscard = { screenModel.cancelDiscard() },
                            onClearSaveError = { screenModel.clearSaveError() },
                            onRefreshFile = { screenModel.refreshFileContent() },
                            onConfirmReload = { screenModel.confirmReloadExternalChange() },
                            onCancelReload = { screenModel.cancelReloadExternalChange() }
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
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
                    border = BorderStroke(AppSpacing.borderThin, AppColors.outline)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = AppSpacing.cardPaddingLarge, vertical = AppSpacing.md)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            if (canNavigateUp) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(AppSpacing.xxxl)
                        .background(AppColors.surfaceVariant, AppShapes.circle)
                        .moccaClickable(onClick = onNavigateUp, pressedScale = 0.92f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate up",
                        tint = AppColors.onSurface,
                        modifier = Modifier.size(AppSpacing.iconSizeSmall)
                    )
                }
            }

            Icon(
                Icons.Default.Home,
                contentDescription = null,
                                                modifier = Modifier.size(AppSpacing.iconSizeSmall),
                tint = AppColors.primary
            )

            pathHistory.lastOrNull()?.let { path ->
                if (path.isNotEmpty()) {
                    path.split("/").filter { it.isNotEmpty() }.forEach { segment ->
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = AppColors.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(AppSpacing.iconSizeSmall)
                        )
                        Text(
                            text = segment,
                            style = AppTypography.labelMedium,
                            color = AppColors.onSurface,
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = AppColors.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(AppSpacing.xxxl)
                )
                Text(
                    text = "Directory empty",
                    style = AppTypography.labelMedium,
                    color = AppColors.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppSpacing.cardPaddingLarge),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            items(
                items = files,
                key = { it.path },
                contentType = { if (it.isDirectory) "directory" else "file" }
            ) { file ->
                GodListItem(
                    title = file.name,
                    subtitle = if (file.isDirectory) "Folder" else formatFileSize(file.size ?: 0),
                    modifier = Modifier.animateItem(
                        fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                        placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                    ),
                    icon = {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else getFileIcon(file.name),
                            contentDescription = null,
                            tint = if (file.isDirectory) AppColors.primary else AppColors.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(AppSpacing.iconSizeMedium)
                        )
                    },
                    trailing = {
                        if (file.isDirectory) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = AppColors.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(AppSpacing.iconSizeSmall)
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
    filePath: String,
    fileSize: Long?,
    content: String,
    language: String,
    isEditing: Boolean,
    editedContent: String?,
    isSaving: Boolean,
    saveError: String?,
    hasUnsavedChanges: Boolean,
    showDiscardDialog: Boolean,
    showExternalChangeDialog: Boolean,
    onClose: () -> Unit,
    onToggleEdit: () -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onCancelDiscard: () -> Unit,
    onClearSaveError: () -> Unit,
    onRefreshFile: () -> Unit,
    onConfirmReload: () -> Unit,
    onCancelReload: () -> Unit
) {
    val isBinary = isBinaryFile(fileName)
    val isTooLarge = (content.length > MAX_EDITABLE_FILE_SIZE_CHARS)

    // Discard confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onCancelDiscard,
            title = {
                Text(
                    text = "Unsaved Changes",
                    style = AppTypography.titleMedium,
                    color = AppColors.onSurface
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes. Discard them?",
                    style = AppTypography.bodyMedium,
                    color = AppColors.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text("Discard", color = AppColors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDiscard) {
                    Text("Cancel", color = AppColors.onSurface)
                }
            },
            containerColor = AppColors.surfaceContainer,
            shape = AppShapes.dialog
        )
    }

    // External file change dialog
    if (showExternalChangeDialog) {
        AlertDialog(
            onDismissRequest = onCancelReload,
            title = {
                Text(
                    text = "File Changed Externally",
                    style = AppTypography.titleMedium,
                    color = AppColors.onSurface
                )
            },
            text = {
                Text(
                    text = "This file was modified outside the editor. Reload the latest version?",
                    style = AppTypography.bodyMedium,
                    color = AppColors.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmReload) {
                    Text("Reload", color = AppColors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelReload) {
                    Text("Keep Editing", color = AppColors.onSurface)
                }
            },
            containerColor = AppColors.surfaceContainer,
            shape = AppShapes.dialog
        )
    }

    // Save error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    if (saveError != null) {
        LaunchedEffect(saveError) {
            snackbarHostState.showSnackbar(
                message = saveError,
                duration = SnackbarDuration.Short
            )
            onClearSaveError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Viewer Header
            Surface(
                color = AppColors.surface,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(AppSpacing.borderThin, AppColors.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.cardPaddingLarge, vertical = AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        getFileIcon(fileName),
                        contentDescription = null,
                        tint = AppColors.primary,
                        modifier = Modifier.size(AppSpacing.iconSizeMedium)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.lg))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasUnsavedChanges && isEditing) "$fileName *" else fileName,
                            style = AppTypography.titleSmall,
                            color = AppColors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (language != DEFAULT_LANGUAGE) {
                            Text(
                                text = language,
                                style = AppTypography.labelSmall,
                                color = AppColors.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                    // Edit/Save button — only for editable text files
                    if (!isBinary && !isTooLarge) {
                        if (isEditing) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(AppSpacing.xxxl)
                                    .background(AppColors.primary, AppShapes.circle)
                                    .moccaClickable(onClick = onSave, enabled = !isSaving, pressedScale = 0.92f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = AppColors.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Save file",
                                        tint = AppColors.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(AppSpacing.xxxl)
                                    .moccaClickable(onClick = onToggleEdit, enabled = !isSaving, pressedScale = 0.92f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel editing",
                                    tint = AppColors.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(AppSpacing.xxxl)
                                    .background(AppColors.surfaceVariant, AppShapes.circle)
                                    .moccaClickable(onClick = onToggleEdit, pressedScale = 0.92f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit file",
                                    tint = AppColors.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    // Refresh button
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(AppSpacing.iconButtonSize)
                            .moccaClickable(onClick = onRefreshFile, pressedScale = 0.92f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh file",
                            tint = AppColors.onSurface,
                            modifier = Modifier.size(AppSpacing.iconSizeSmall)
                        )
                    }
                    // Close button
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(AppSpacing.xxxl)
                            .moccaClickable(onClick = onClose, pressedScale = 0.92f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close file viewer",
                            tint = AppColors.onSurface,
                            modifier = Modifier.size(AppSpacing.iconSizeSmall)
                        )
                    }
                }
            }

            // File Content
            if (isBinary) {
                // Binary file — cannot edit
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = AppColors.onSurfaceVariant,
                            modifier = Modifier.size(AppSpacing.iconButtonSize)
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.lg))
                        Text(
                            text = "Cannot edit binary file",
                            style = AppTypography.bodyMedium,
                            color = AppColors.onSurfaceVariant
                        )
                    }
                }
            } else if (isTooLarge) {
                // Large file — read-only with message
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Surface(
                        color = AppColors.warning.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "File too large to edit (${formatFileSize(content.length.toLong())})",
                            style = AppTypography.labelMedium,
                            color = AppColors.warning,
                            modifier = Modifier.padding(horizontal = AppSpacing.cardPaddingLarge, vertical = AppSpacing.sm)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(AppColors.background)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(AppSpacing.cardPaddingLarge)
                    ) {
                        Text(
                            text = content,
                            style = AppTypography.code,
                            color = AppColors.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Text file — CodeEditorView for both read-only and edit modes
                CodeEditorView(
                    content = if (isEditing) editedContent.orEmpty() else content,
                    language = language,
                    editable = isEditing,
                    onContentChanged = onContentChange,
                    onReady = {},
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(AppColors.background)
                )
            }
        }

        // Snackbar for save errors
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(AppSpacing.lg)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = AppColors.errorContainer,
                contentColor = AppColors.onErrorContainer,
                shape = AppShapes.medium
            )
        }
    }
}

private val BINARY_EXTENSIONS = setOf(
    "png", "jpg", "jpeg", "gif", "zip", "tar", "gz", "class",
    "jar", "so", "dex", "apk", "bin", "exe", "dll", "pdf"
)

private const val MAX_EDITABLE_FILE_SIZE_CHARS = 256 * 1024
private const val DEFAULT_LANGUAGE = "plaintext"

private fun isBinaryFile(fileName: String): Boolean {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return extension in BINARY_EXTENSIONS
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
