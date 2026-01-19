package com.mocca.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.theme.TerminalColors

/**
 * Alert dialog for Git server started successfully.
 * Shows after user confirms server start, offering to retry connection.
 */
@Composable
fun GitServerStartedDialog(
    onDismiss: () -> Unit,
    onRetryConnection: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onRetryConnection()
                onDismiss()
            }) {
                Text("Retry Connection", color = TerminalColors.statusOnline)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = TerminalColors.grey)
            }
        },
        title = {
            Text(
                "Git Server Started",
                fontWeight = FontWeight.Bold,
                color = TerminalColors.statusOnline
            )
        },
        text = {
            Text(
                "Git server is now running!\n\n" +
                "The application will attempt to connect now.",
                color = TerminalColors.whiteDim
            )
        },
        tonalElevation = 6.dp,
        icon = {
            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = TerminalColors.statusOnline)
        },
        containerColor = TerminalColors.surface,
        titleContentColor = TerminalColors.white,
        textContentColor = TerminalColors.whiteDim
    )
}
