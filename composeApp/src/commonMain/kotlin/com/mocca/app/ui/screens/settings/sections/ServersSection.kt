package com.mocca.app.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mocca.app.domain.model.ServerConfig
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.screens.settings.ServerConnectionStatus
import com.mocca.app.ui.screens.settings.TerminalServerCard
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import kotlinx.collections.immutable.ImmutableList

/**
 * Settings section: Server management
 * 
 * Displays configured servers with connection status, active selection,
 * and actions (edit, delete, check connection).
 */
@Composable
fun ServersSection(
    servers: ImmutableList<ServerConfig>,
    activeServerId: String?,
    connectionStatuses: Map<String, ServerConnectionStatus>,
    onActivate: (String) -> Unit,
    onEdit: (ServerConfig) -> Unit,
    onDelete: (String) -> Unit,
    onCheckConnection: (ServerConfig) -> Unit,
    onAddNewServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "SERVERS",
            color = AppColors.textSecondary,
            style = AppTypography.labelSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        servers.forEach { server ->
            TerminalServerCard(
                server = server,
                isActive = server.id == activeServerId,
                connectionStatus = connectionStatuses[server.id] ?: ServerConnectionStatus.UNKNOWN,
                onActivate = { onActivate(server.id) },
                onEdit = { onEdit(server) },
                onDelete = { onDelete(server.id) },
                onCheckConnection = { onCheckConnection(server) }
            )
            Spacer(modifier = Modifier.height(AppSpacing.cardGap))
        }
        
        MoccaButton(
            text = "ADD SERVER",
            onClick = onAddNewServer,
            modifier = Modifier.fillMaxWidth(),
            height = AppSpacing.buttonHeightCompact
        )
    }
}
