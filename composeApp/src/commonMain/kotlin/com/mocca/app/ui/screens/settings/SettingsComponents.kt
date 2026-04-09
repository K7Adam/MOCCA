package com.mocca.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocca.app.api.NetworkConfig
import com.mocca.app.domain.model.ServerConfig
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Status indicator dot
 */
@Composable
fun StatusDot(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
    )
}

/**
 * Connection status enum
 */
enum class ServerConnectionStatus {
    UNKNOWN,
    CHECKING,
    CONNECTED,
    FAILED
}

/**
 * Terminal server card with connection status and actions
 */
@Composable
fun TerminalServerCard(
    server: ServerConfig,
    isActive: Boolean,
    connectionStatus: ServerConnectionStatus,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCheckConnection: () -> Unit
) {
    val borderColor = if (isActive) AppColors.primary else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surfaceContainerHigh, AppShapes.card)
            .then(
                if (borderColor != null) {
                    Modifier.border(AppSpacing.borderThin, borderColor, AppShapes.card)
                } else Modifier
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onActivate)
                .background(if (isActive) AppColors.primary.copy(alpha = 0.1f) else Color.Transparent)
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(
                color = if (isActive) AppColors.primary else AppColors.onSurfaceVariant,
                size = 12.dp
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    color = if (isActive) AppColors.primary else AppColors.onSurface,
                    style = AppTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = server.baseUrl,
                    color = AppColors.onSurfaceVariant,
                    style = AppTypography.bodySmall
                )
            }

            if (isActive) {
                Text(
                    text = "Selected",
                    color = AppColors.primary,
                    style = AppTypography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(
            thickness = AppSpacing.borderThin,
            color = AppColors.outline
        )

        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClickLabel = "Check connection for ${server.name}",
                        onClick = onCheckConnection
                    )
                    .padding(horizontal = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusIcon, statusColor) = when (connectionStatus) {
                    ServerConnectionStatus.UNKNOWN -> Icons.Default.QuestionMark to AppColors.outline
                    ServerConnectionStatus.CHECKING -> Icons.Default.Sync to AppColors.statusWaiting
                    ServerConnectionStatus.CONNECTED -> Icons.Default.CheckCircle to AppColors.statusOnline
                    ServerConnectionStatus.FAILED -> Icons.Default.Error to AppColors.error
                }

                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(
                    text = connectionStatus.name,
                    color = statusColor,
                    style = AppTypography.labelSmall
                )
            }

            // Edit/Delete
            MoccaIconButton(
                icon = Icons.Default.Edit,
                onClick = onEdit,
                iconColor = AppColors.onSurfaceVariant,
                contentDescription = "Edit server"
            )
            MoccaIconButton(
                icon = Icons.Default.Delete,
                onClick = onDelete,
                iconColor = AppColors.error,
                contentDescription = "Delete server"
            )
        }
    }
}

/**
 * Terminal server edit dialog
 */
@Composable
fun TerminalServerEditDialog(
    server: ServerConfig,
    isNewServer: Boolean,
    onSave: (ServerConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(server.name.ifBlank { "DigitalOcean OpenCode" }) }
    var host by remember { mutableStateOf(server.host.ifBlank { NetworkConfig.DEFAULT_HOST_IP }) }
    var port by remember { mutableStateOf(if (server.port == 0) NetworkConfig.OPENCODE_SERVER_PORT.toString() else server.port.toString()) }
    var username by remember { mutableStateOf(server.username.ifBlank { NetworkConfig.DEFAULT_USERNAME }) }
    var password by remember { mutableStateOf(server.password.ifBlank { NetworkConfig.DEFAULT_PASSWORD }) }

    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.surfaceContainerHigh,
        shape = AppShapes.dialog,
        title = {
            Text(
                text = if (isNewServer) "Add server" else "Edit server",
                color = AppColors.onSurface,
                style = AppTypography.headlineSmall
            )
        },
        text = {
            Column {
                Spacer(modifier = Modifier.height(AppSpacing.sm))

                MoccaInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Server name",
                    placeholder = "My Server",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                MoccaInput(
                    value = host,
                    onValueChange = { host = it },
                    label = "Host",
                    placeholder = NetworkConfig.DEFAULT_HOST_IP,
                    hint = "Tailscale hostname, LAN IP, or localhost",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                MoccaInput(
                    value = port,
                    onValueChange = { port = it },
                    label = "Port",
                    placeholder = "4242",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                MoccaInput(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    placeholder = NetworkConfig.DEFAULT_USERNAME,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                MoccaInput(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "Leave empty if none",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onSave(
                            server.copy(
                                name = name,
                                host = host,
                                port = port.toIntOrNull() ?: 4242,
                                username = username.ifBlank { NetworkConfig.DEFAULT_USERNAME },
                                password = password
                            )
                        )
                    })
                )
            }
        },
        confirmButton = {
            MoccaButton(
                text = "Save",
                onClick = {
                    onSave(
                        server.copy(
                            name = name,
                            host = host,
                            port = port.toIntOrNull() ?: 4242,
                            username = username.ifBlank { NetworkConfig.DEFAULT_USERNAME },
                            password = password
                        )
                    )
                },
                enabled = name.isNotBlank() && host.isNotBlank(),
                height = AppSpacing.buttonHeightCompact
            )
        },
        dismissButton = {
            MoccaTextButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}
