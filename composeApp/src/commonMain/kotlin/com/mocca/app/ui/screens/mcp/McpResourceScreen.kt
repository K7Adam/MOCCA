package com.mocca.app.ui.screens.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mocca.app.domain.model.McpResource
import com.mocca.app.domain.model.McpResourceContent
import com.mocca.app.ui.components.modern.MoccaIconButton
import com.mocca.app.ui.theme.moccaClickable
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import org.koin.core.parameter.parametersOf

/**
 * W3-T4: MCP Resource Browsing Screen.
 * Shows all resources exposed by a connected MCP server and allows reading their content.
 */
data class McpResourceScreen(val serverName: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<McpResourceScreenModel> { parametersOf(serverName) }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(serverName) {
            screenModel.loadResources()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(AppSpacing.lg)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.surfaceContainer, AppShapes.none),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoccaIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navigator.pop() },
                        iconColor = AppColors.onSurface
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MCP_RESOURCES",
                            color = AppColors.onSurface,
                            style = AppTypography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "server: \"$serverName\"",
                            color = AppColors.onSurfaceVariant,
                            style = AppTypography.codeSmall
                        )
                    }
                    MoccaIconButton(
                        icon = Icons.Default.Refresh,
                        onClick = { screenModel.loadResources() },
                        iconColor = if (state.isLoading) AppColors.statusWaiting else AppColors.onSurfaceVariantLight
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                state.error?.let { err ->
                    Text(
                        text = "!! error: \"$err\"",
                        color = AppColors.error,
                        style = AppTypography.codeSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.error.copy(alpha = 0.08f), AppShapes.medium)
                            .padding(AppSpacing.sm)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                }

                when {
                    state.isLoading && state.resources.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LoadingIndicator(
                                    color = AppColors.statusWaiting,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(AppSpacing.md))
                                Text(
                                    text = "LOADING_RESOURCES...",
                                    color = AppColors.onSurfaceVariant,
                                    style = AppTypography.bodyMedium
                                )
                            }
                        }
                    }

                    state.resources.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(AppSpacing.borderThin, AppColors.onSurfaceVariantDark, AppShapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(AppSpacing.xl)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOff,
                                    contentDescription = null,
                                    tint = AppColors.onSurfaceVariantDark,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(AppSpacing.md))
                                Text(
                                    text = "NO_RESOURCES_FOUND",
                                    color = AppColors.onSurfaceVariant,
                                    style = AppTypography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "This server exposes no resources",
                                    color = AppColors.onSurfaceVariantDark,
                                    style = AppTypography.bodySmall
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            items(state.resources, key = { it.uri }) { resource ->
                                McpResourceItem(
                                    resource = resource,
                                    isSelected = state.selectedResource?.uri == resource.uri,
                                    onClick = { screenModel.selectResource(resource) }
                                )
                            }
                        }
                    }
                }
            }

            if (state.selectedResource != null) {
                McpResourceContentPanel(
                    resource = state.selectedResource!!,
                    content = state.selectedContent,
                    isLoading = state.isLoadingContent,
                    error = state.contentError,
                    onDismiss = { screenModel.clearSelection() }
                )
            }
        }
    }
}

@Composable
private fun McpResourceItem(
    resource: McpResource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AppColors.statusWaiting else AppColors.onSurfaceVariantDark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceVariant.copy(alpha = 0.5f), AppShapes.card)
            .border(AppSpacing.borderThin, borderColor.copy(alpha = 0.4f), AppShapes.card)
            .moccaClickable(onClick = onClick, pressedScale = 0.98f)
            .padding(AppSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = AppColors.statusWaiting,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = resource.name,
                color = AppColors.onSurface,
                style = AppTypography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            resource.mimeType?.let { mime ->
                Text(
                    text = mime,
                    color = AppColors.onSurfaceVariant,
                    style = AppTypography.labelSmall,
                    modifier = Modifier.padding(start = AppSpacing.xs)
                )
            }
        }
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = resource.uri,
            color = AppColors.syntaxString,
            style = AppTypography.codeSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        resource.description?.let { desc ->
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = desc,
                color = AppColors.onSurfaceVariant,
                style = AppTypography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun McpResourceContentPanel(
    resource: McpResource,
    content: McpResourceContent?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background.copy(alpha = 0.97f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.85f)
                .background(AppColors.surface, AppShapes.card)
                .border(AppSpacing.borderStandard, AppColors.outlineVariant, AppShapes.card)
                .clip(AppShapes.card)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.name.uppercase(),
                        color = AppColors.onSurface,
                        style = AppTypography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = resource.uri,
                        color = AppColors.syntaxString,
                        style = AppTypography.codeSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MoccaIconButton(
                    icon = Icons.Default.Close,
                    onClick = onDismiss,
                    iconColor = AppColors.onSurfaceVariantLight
                )
            }

            HorizontalDivider(thickness = AppSpacing.borderThin, color = AppColors.outline)

            // Content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingIndicator(
                                color = AppColors.statusWaiting,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            Text(
                                text = "READING_RESOURCE...",
                                color = AppColors.onSurfaceVariant,
                                style = AppTypography.bodySmall
                            )
                        }
                    }

                    error != null -> {
                        Text(
                            text = "!! error: \"$error\"",
                            color = AppColors.error,
                            style = AppTypography.codeSmall
                        )
                    }

                    content != null -> {
                        SelectionContainer {
                            Text(
                                text = content.text ?: "(binary content — ${content.mimeType ?: "unknown mime type"})",
                                color = if (content.text != null) AppColors.onSurface else AppColors.onSurfaceVariant,
                                style = AppTypography.codeSmall,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = "SELECT_RESOURCE_TO_READ",
                            color = AppColors.onSurfaceVariant,
                            style = AppTypography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

