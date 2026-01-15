package com.mocca.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.components.terminal.TerminalButton
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.loading
import mocca.composeapp.generated.resources.error_occurred
import mocca.composeapp.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingScreen(
    message: String = stringResource(Res.string.loading)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = TerminalColors.white
            )
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            Text(
                text = message.uppercase(),
                style = TerminalTypography.bodyMedium,
                color = TerminalColors.grey
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
            .background(TerminalColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(TerminalSpacing.xxl)
        ) {
            Text(
                text = "[!]",
                style = TerminalTypography.displayLarge,
                color = TerminalColors.error
            )
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            Text(
                text = stringResource(Res.string.error_occurred).uppercase(),
                style = TerminalTypography.headlineSmall,
                color = TerminalColors.white
            )
            Spacer(modifier = Modifier.height(TerminalSpacing.sm))
            Text(
                text = message,
                style = TerminalTypography.bodyMedium,
                color = TerminalColors.grey
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(TerminalSpacing.xl))
                TerminalButton(
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
            .background(TerminalColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(TerminalSpacing.xxl)
        ) {
            Text(
                text = icon,
                style = TerminalTypography.displayLarge,
                color = TerminalColors.grey
            )
            Spacer(modifier = Modifier.height(TerminalSpacing.lg))
            Text(
                text = title.uppercase(),
                style = TerminalTypography.headlineSmall,
                color = TerminalColors.white
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(TerminalSpacing.sm))
                Text(
                    text = subtitle,
                    style = TerminalTypography.bodyMedium,
                    color = TerminalColors.grey
                )
            }
            if (action != null) {
                Spacer(modifier = Modifier.height(TerminalSpacing.xl))
                action()
            }
        }
    }
}
