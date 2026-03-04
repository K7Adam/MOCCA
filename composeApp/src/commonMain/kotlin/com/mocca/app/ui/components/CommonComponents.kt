package com.mocca.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mocca.app.ui.theme.AppShapes
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.PermissionRequest
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun PermissionRequestDialog(
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
                tint = AppColors.accent
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
                    style = AppTypography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Permission type
                Surface(
                    shape = AppShapes.medium,
                    color = AppColors.surfaceVariant
                ) {
                    Text(
                        text = permission.permission,
                        modifier = Modifier.padding(12.dp),
                        style = AppTypography.titleMedium,
                        color = AppColors.white
                    )
                }
                
                // Patterns preview if available
                if (permission.patterns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(Res.string.details),
                        style = AppTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Surface(
                        shape = AppShapes.medium,
                        color = AppColors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            permission.patterns.take(10).forEach { pattern ->
                                Text(
                                    text = pattern,
                                    style = AppTypography.bodySmall,
                                    color = AppColors.grey
                                )
                            }
                            if (permission.patterns.size > 10) {
                                Text(
                                    text = "... and ${permission.patterns.size - 10} more",
                                    style = AppTypography.bodySmall,
                                    color = AppColors.grey
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
                        style = AppTypography.bodySmall,
                        color = AppColors.border
                    )
                }
                
                // Metadata preview if available
                if (permission.metadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        shape = AppShapes.medium,
                        color = AppColors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatMetadataMap(permission.metadata),
                            modifier = Modifier.padding(12.dp),
                            style = AppTypography.bodySmall,
                            color = AppColors.grey
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
                    shape = AppShapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.white,
                        contentColor = AppColors.background
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
                shape = AppShapes.pill,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.grey
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

@Composable
fun LoadingScreen(
    message: String = stringResource(Res.string.loading)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = AppColors.white
            )
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            Text(
                text = message.uppercase(),
                style = AppTypography.bodyMedium,
                color = AppColors.grey
            )
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(AppSpacing.xxl)
        ) {
            Text(
                text = "[!]",
                style = AppTypography.displayLarge,
                color = AppColors.error
            )
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            Text(
                text = stringResource(Res.string.error_occurred).uppercase(),
                style = AppTypography.headlineSmall,
                color = AppColors.white
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = message,
                style = AppTypography.bodyMedium,
                color = AppColors.grey
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xl))
                MoccaButton(
                    text = stringResource(Res.string.retry).uppercase(),
                    onClick = onRetry
                )
            }
        }
    }
}

@Composable
fun EmptyContent(
    icon: String = "[?]",
    title: String,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(AppSpacing.xxl)
        ) {
            Text(
                text = icon,
                style = AppTypography.displayLarge,
                color = AppColors.grey
            )
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            Text(
                text = title.uppercase(),
                style = AppTypography.headlineSmall,
                color = AppColors.white
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Text(
                    text = subtitle,
                    style = AppTypography.bodyMedium,
                    color = AppColors.grey
                )
            }
            if (action != null) {
                Spacer(modifier = Modifier.height(AppSpacing.xl))
                action()
            }
        }
    }
}
