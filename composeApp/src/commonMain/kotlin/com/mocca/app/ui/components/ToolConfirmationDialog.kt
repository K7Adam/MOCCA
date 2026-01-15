package com.mocca.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.PermissionRequest
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalTypography
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Dialog for permission/tool approval requests.
 * Shows when OpenCode needs user approval to execute a tool.
 */
import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PermissionDialog(
    permission: PermissionRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onAlways: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDeny,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = TerminalColors.statusOnline
            )
        },
        title = {
            Text(stringResource(Res.string.permission_required))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.ai_wants_to),
                    style = TerminalTypography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Permission type
                Surface(
                    shape = RectangleShape,
                    color = TerminalColors.surfaceVariant
                ) {
                    Text(
                        text = permission.permission,
                        modifier = Modifier.padding(12.dp),
                        style = TerminalTypography.titleMedium,
                        color = TerminalColors.white
                    )
                }
                
                // Patterns preview if available
                if (permission.patterns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(Res.string.details),
                        style = TerminalTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Surface(
                        shape = RectangleShape,
                        color = TerminalColors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            permission.patterns.take(10).forEach { pattern ->
                                Text(
                                    text = pattern,
                                    style = TerminalTypography.bodySmall,
                                    color = TerminalColors.grey
                                )
                            }
                            if (permission.patterns.size > 10) {
                                Text(
                                    text = "... and ${permission.patterns.size - 10} more",
                                    style = TerminalTypography.bodySmall,
                                    color = TerminalColors.grey
                                )
                            }
                        }
                    }
                }
                
                // Already allowed patterns
                if (permission.always.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Already allowed: ${permission.always.take(3).joinToString(", ")}",
                        style = TerminalTypography.bodySmall,
                        color = TerminalColors.border
                    )
                }
                
                // Metadata preview if available
                if (permission.metadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        shape = RectangleShape,
                        color = TerminalColors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatMetadataMap(permission.metadata),
                            modifier = Modifier.padding(12.dp),
                            style = TerminalTypography.bodySmall,
                            color = TerminalColors.grey
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onAlways != null) {
                    TextButton(onClick = onAlways) {
                        Text("Always")
                    }
                }
                Button(
                    onClick = onApprove,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalColors.white,
                        contentColor = TerminalColors.background
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.allow))
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDeny,
                shape = RectangleShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TerminalColors.grey
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.deny))
            }
        }
    )
}

/**
 * Format JSON metadata for display.
 */
private fun formatMetadata(metadata: JsonElement): String {
    return try {
        val sb = StringBuilder()
        metadata.jsonObject.entries.take(10).forEach { (key, value) ->
            val valueStr = try {
                value.jsonPrimitive.content
            } catch (e: Exception) {
                value.toString()
            }
            // Truncate long values
            val truncated = if (valueStr.length > 200) {
                valueStr.take(200) + "..."
            } else {
                valueStr
            }
            sb.appendLine("$key: $truncated")
        }
        sb.toString().trimEnd()
    } catch (e: Exception) {
        metadata.toString().take(500)
    }
}

/**
 * Format Map metadata for display.
 */
private fun formatMetadataMap(metadata: Map<String, JsonElement>): String {
    return try {
        val sb = StringBuilder()
        metadata.entries.take(10).forEach { (key, value) ->
            val valueStr = try {
                value.jsonPrimitive.content
            } catch (e: Exception) {
                value.toString()
            }
            // Truncate long values
            val truncated = if (valueStr.length > 200) {
                valueStr.take(200) + "..."
            } else {
                valueStr
            }
            sb.appendLine("$key: $truncated")
        }
        sb.toString().trimEnd()
    } catch (e: Exception) {
        metadata.toString().take(500)
    }
}
