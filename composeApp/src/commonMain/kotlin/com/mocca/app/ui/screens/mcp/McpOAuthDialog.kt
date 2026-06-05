package com.mocca.app.ui.screens.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.McpOAuthState
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaOutlinedButton
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

/**
 * Dialog shown when an MCP server requires OAuth authorization.
 * Displays the auth URL, allows copying / opening in browser,
 * and accepts the returned authorization code from the user.
 *
 * W3-T3: MCP OAuth flow UI.
 */

@Composable
fun McpOAuthDialog(
    oauthState: McpOAuthState,
    isInProgress: Boolean,
    onSubmitCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    val uriHandler = LocalUriHandler.current

    // Full-screen dim overlay — not dismissable by tap so user doesn't accidentally lose flow
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background.copy(alpha = 0.92f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(AppColors.surface, AppShapes.card)
                .border(AppSpacing.borderStandard, AppColors.statusWaiting.copy(alpha = 0.5f), AppShapes.card)
                .clip(AppShapes.card)
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AppColors.statusWaiting,
                    modifier = Modifier.size(AppSpacing.iconSizeMedium)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Column {
                    Text(
                        text = "OAUTH_AUTHORIZATION",
                        color = AppColors.onSurface,
                        style = AppTypography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "server: \"${oauthState.serverName}\"",
                        color = AppColors.onSurfaceVariant,
                        style = AppTypography.codeSmall
                    )
                }
            }

            HorizontalDivider(
                thickness = AppSpacing.borderThin,
                color = AppColors.outline
            )

            Text(
                text = "// STEP_1: Open the authorization URL",
                color = AppColors.statusOnline,
                style = AppTypography.labelMedium
            )

            if (oauthState.authUrl != null) {
                SelectionContainer {
                    Text(
                        text = oauthState.authUrl,
                        color = AppColors.syntaxString,
                        style = AppTypography.codeSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.surfaceVariant, AppShapes.medium)
                            .border(AppSpacing.borderThin, AppColors.outline, AppShapes.medium)
                            .padding(AppSpacing.sm)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MoccaOutlinedButton(
                        text = "COPY_URL",
                        onClick = {
                            clipboardManager.setText(AnnotatedString(oauthState.authUrl))
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ContentCopy
                    )
                    MoccaButton(
                        text = "OPEN_BROWSER",
                        onClick = {
                            uriHandler.openUri(oauthState.authUrl)
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Launch
                    )
                }
            } else {
                // Auth URL not yet available — still loading
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    LoadingIndicator(
                        color = AppColors.statusWaiting,
                        modifier = Modifier.size(AppSpacing.iconSizeSmall)
                    )
                    Text(
                        text = "Generating authorization URL...",
                        color = AppColors.onSurfaceVariant,
                        style = AppTypography.bodySmall
                    )
                }
            }

            HorizontalDivider(
                thickness = AppSpacing.borderThin,
                color = AppColors.outline
            )

            Text(
                text = "// STEP_2: Paste the authorization code",
                color = AppColors.statusOnline,
                style = AppTypography.labelMedium
            )

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = "AUTH_CODE",
                        color = AppColors.onSurfaceVariant,
                        style = AppTypography.labelSmall
                    )
                },
                placeholder = {
                    Text(
                        text = "Paste the code from your browser here...",
                        color = AppColors.onSurfaceVariantDark,
                        style = AppTypography.bodySmall
                    )
                },
                textStyle = AppTypography.code.copy(color = AppColors.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.statusWaiting,
                    unfocusedBorderColor = AppColors.outline,
                    cursorColor = AppColors.statusWaiting,
                    focusedContainerColor = AppColors.surfaceVariant,
                    unfocusedContainerColor = AppColors.surfaceVariant
                ),
                singleLine = true,
                enabled = !isInProgress
            )

            oauthState.error?.let { err ->
                Text(
                    text = "!! error: \"$err\"",
                    color = AppColors.error,
                    style = AppTypography.codeSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.error.copy(alpha = 0.08f), AppShapes.medium)
                        .padding(AppSpacing.sm)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                MoccaOutlinedButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isInProgress
                )
                MoccaButton(
                    text = if (isInProgress) "SUBMITTING..." else "SUBMIT_CODE",
                    onClick = { if (code.isNotBlank()) onSubmitCode(code) },
                    enabled = code.isNotBlank() && !isInProgress,
                    modifier = Modifier.weight(1f)
                )
            }

            // Spinner overlay when in progress
            if (isInProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LoadingIndicator(
                        color = AppColors.statusWaiting,
                        modifier = Modifier.size(AppSpacing.iconSizeMedium)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(
                        text = "Verifying authorization code...",
                        color = AppColors.onSurfaceVariant,
                        style = AppTypography.bodySmall
                    )
                }
            }
        }
    }
}
