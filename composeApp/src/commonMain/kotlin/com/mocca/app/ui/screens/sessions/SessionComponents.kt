package com.mocca.app.ui.screens.sessions

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.Session
import com.mocca.app.domain.model.SessionStatus
import com.mocca.app.ui.components.modern.*
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope
import com.mocca.app.ui.navigation.LocalSharedTransitionScope
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.moccaClickable
import com.mocca.app.ui.theme.AppTypography
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import mocca.composeapp.generated.resources.Res
import mocca.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ModernConnectionProgressContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingIndicator(
            color = AppColors.statusWaiting,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Text(
            text = message,
            style = AppTypography.labelSmall,
            color = AppColors.onSurfaceVariantLight
        )
    }
}

@Composable
internal fun ModernEmptySessionsContent(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.no_sessions),
            style = AppTypography.headlineSmall,
            color = AppColors.onSurface
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = stringResource(Res.string.no_sessions_hint),
            style = AppTypography.bodySmall,
            color = AppColors.onSurfaceVariantLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        MoccaButton(
            text = "New session",
            onClick = onCreateClick,
            icon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

@Composable
internal fun ModernSessionsList(
    sessions: List<Session>,
    childrenMap: Map<String, List<Session>>,
    selectedSessionId: String?,
    onSessionClick: (Session) -> Unit,
    onDeleteClick: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        items(
            items = sessions,
            key = { it.id },
            contentType = { "session" }
        ) { session ->
            MoccaSessionCard(
                session = session,
                childSessions = childrenMap[session.id] ?: emptyList(),
                isSelected = session.id == selectedSessionId,
                onClick = { onSessionClick(session) },
                onDeleteClick = { onDeleteClick(session) }
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MoccaSessionCard(
    session: Session,
    childSessions: List<Session>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val borderColor = if (isSelected) AppColors.primary else AppColors.outline
    val bgColor = if (isSelected) AppColors.primary.copy(alpha = 0.05f) else Color.Transparent
    
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    val modifier = Modifier
        .fillMaxWidth()
        .clip(AppShapes.medium)
        .background(bgColor, AppShapes.medium)
        .border(AppSpacing.borderThin, borderColor, AppShapes.medium)
        .moccaClickable(onClick = onClick, pressedScale = 0.98f)

    val finalModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            modifier.sharedBounds(
                rememberSharedContentState(key = "session_card_${session.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        modifier
    }

    Box(modifier = finalModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator dot
            if (isSelected) {
                StatusDot(color = AppColors.primary, size = 6.dp)
                Spacer(modifier = Modifier.width(AppSpacing.sm))
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.title ?: stringResource(Res.string.untitled_session),
                    style = AppTypography.labelMedium,
                    color = if (isSelected) AppColors.primary else AppColors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    ModernStatusChip(status = session.status)
                    Text(
                        text = formatTime(session.updatedAt),
                        style = AppTypography.labelExtraSmall,
                        color = AppColors.onSurfaceVariant
                    )
                    if (session.summaryFiles > 0) {
                        Text(
                            text = "\u2022 ${session.summaryFiles}F",
                            style = AppTypography.labelExtraSmall,
                            color = AppColors.onSurfaceVariantLight
                        )
                    }
                }
            }
            
            MoccaIconButton(
                icon = Icons.Default.Delete,
                onClick = onDeleteClick,
                iconColor = AppColors.error.copy(alpha = 0.6f),
                size = 32.dp,
                contentDescription = stringResource(Res.string.delete_session)
            )
        }
    }
}

@Composable
internal fun ModernStatusChip(status: SessionStatus) {
    val (color, textRes) = when (status) {
        SessionStatus.IDLE -> AppColors.onSurfaceVariant to Res.string.session_idle
        SessionStatus.RUNNING -> AppColors.statusOnline to Res.string.session_running
        SessionStatus.COMPLETED -> AppColors.success to Res.string.session_completed
        SessionStatus.ERROR -> AppColors.error to Res.string.session_error
    }
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), AppShapes.small)
            .padding(horizontal = AppSpacing.xs, vertical = 1.dp)
    ) {
        Text(
            text = stringResource(textRes).uppercase(),
            style = AppTypography.labelExtraSmall,
            color = color
        )
    }
}

internal fun formatTime(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        ""
    }
}

// Session search bar


@Composable
internal fun ModernSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Box(
        modifier = modifier
            .background(AppColors.surface.copy(alpha = 0.8f), AppShapes.medium)
            .border(AppSpacing.borderThin, AppColors.outline, AppShapes.medium)
            .padding(AppSpacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AppColors.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = AppTypography.labelSmall.copy(
                    color = AppColors.onSurface
                ),
                cursorBrush = SolidColor(AppColors.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* Already filtering live */ }),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search...",
                                style = AppTypography.labelSmall,
                                color = AppColors.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            
            if (query.isNotEmpty()) {
                MoccaIconButton(
                    icon = Icons.Default.Clear,
                    onClick = onClear,
                    iconColor = AppColors.onSurfaceVariant,
                    size = 28.dp,
                    contentDescription = "Clear"
                )
            }
        }
    }
}

@Composable
internal fun TerminalNotConnectedContent(
    title: String,
    message: String,
    onConfigureClick: () -> Unit,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.CloudOff
) {
    Column(
        modifier = modifier.padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = AppColors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Text(
            text = title,
            style = AppTypography.headlineSmall,
            color = AppColors.onSurface
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = message,
            style = AppTypography.bodySmall,
            color = AppColors.onSurfaceVariantLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            MoccaButton(
                text = "Configure",
                onClick = onConfigureClick,
                icon = Icons.Default.Settings,
                modifier = Modifier.weight(1f)
            )
            if (onRetryClick != null) {
                MoccaOutlinedButton(
                    text = "Retry",
                    onClick = onRetryClick,
                    icon = Icons.Default.Refresh,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
