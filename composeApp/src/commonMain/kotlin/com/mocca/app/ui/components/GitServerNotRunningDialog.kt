package com.mocca.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import com.mocca.app.ui.theme.TerminalColors

/**
 * Alert dialog for Git server not running.
 * Shows when GitApiClient detects server unavailable and offers options.
 */
@Composable
fun GitServerNotRunningDialog(
    onDismiss: () -> Unit,
    onStartServer: () -> Unit,
    showAdbHelp: Boolean = false
) {
    val navigator = LocalNavigator.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (!showAdbHelp) {
                TextButton(onClick = {
                    onStartServer()
                    onDismiss()
                }) {
                    Text("Start Server", color = TerminalColors.statusOnline)
                }
            } else {
                // If ADB help is shown, main action is just "OK" (dismiss)
                TextButton(onClick = onDismiss) {
                    Text("OK", color = TerminalColors.statusOnline)
                }
            }
        },
        dismissButton = {
            if (!showAdbHelp) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TerminalColors.grey)
                }
            }
        },
        title = {
            Text(
                if (showAdbHelp) "ADB Reverse Required" else "Git Server Not Available",
                fontWeight = FontWeight.Bold,
                color = TerminalColors.white
            )
        },
        text = {
            if (showAdbHelp) {
                Text(
                    "The emulator cannot connect to the Git server on localhost (127.0.0.1:4097).\n\n" +
                    "You MUST run this command on your computer to bridge the connection:\n\n" +
                    "adb reverse tcp:4097 tcp:4097\n\n" +
                    "After running this command, try again.",
                    color = TerminalColors.whiteDim
                )
            } else {
                Text(
                    "The Git HTTP server (port 4097) is not running.\n\n" +
                    "If you're starting OpenCode for the first time, the git server " +
                    "needs to be started separately.\n\n" +
                    "Would you like to start it now?",
                    color = TerminalColors.whiteDim
                )
            }
        },
        tonalElevation = 6.dp,
        icon = {
            Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = TerminalColors.warning)
        },
        containerColor = TerminalColors.surface,
        titleContentColor = TerminalColors.white,
        textContentColor = TerminalColors.whiteDim
    )
}
