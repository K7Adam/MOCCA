package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.MonitorRefreshInterval
import com.mocca.app.domain.model.PortInfo
import com.mocca.app.domain.model.ProcessInfo
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.SystemResources
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import com.mocca.app.util.TimeFormatter

@Composable
fun ProcessModule(
    processes: Resource<List<ProcessInfo>>,
    hasActiveSession: Boolean,
    isRefreshing: Boolean,
    lastUpdatedAt: Long?,
    modifier: Modifier = Modifier
) {
    ModuleCard(title = "PROCESS MONITOR", modifier = modifier) {
        SystemModuleStatus(
            resource = processes,
            hasActiveSession = hasActiveSession,
            lastUpdatedAt = lastUpdatedAt,
            emptyLabel = "No processes reported",
            isRefreshing = isRefreshing
        )

        val items = if (hasActiveSession) {
            processes.dataOrNull().orEmpty().sortedByDescending { it.cpu ?: Float.MIN_VALUE }
        } else {
            emptyList()
        }
        if (items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = "${items.size} visible processes",
                color = AppColors.primary,
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            items.take(5).forEachIndexed { index, process ->
                ProcessRow(process)
                if (index < minOf(items.lastIndex, 4)) {
                    HorizontalDivider(color = AppColors.outline.copy(alpha = 0.35f))
                }
            }
        }
    }
}

@Composable
fun PortModule(
    ports: Resource<List<PortInfo>>,
    hasActiveSession: Boolean,
    isRefreshing: Boolean,
    lastUpdatedAt: Long?,
    modifier: Modifier = Modifier
) {
    ModuleCard(title = "PORT SCANNER", modifier = modifier) {
        SystemModuleStatus(
            resource = ports,
            hasActiveSession = hasActiveSession,
            lastUpdatedAt = lastUpdatedAt,
            emptyLabel = "No listening ports reported",
            isRefreshing = isRefreshing
        )

        val items = if (hasActiveSession) ports.dataOrNull().orEmpty() else emptyList()
        if (items.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            items.take(6).forEachIndexed { index, port ->
                PortRow(port)
                if (index < minOf(items.lastIndex, 5)) {
                    HorizontalDivider(color = AppColors.outline.copy(alpha = 0.35f))
                }
            }
        }
    }
}

@Composable
fun ResourceModule(
    resources: Resource<SystemResources>,
    hasActiveSession: Boolean,
    isRefreshing: Boolean,
    lastUpdatedAt: Long?,
    refreshInterval: MonitorRefreshInterval,
    onRefreshIntervalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModuleCard(
        title = "SYSTEM RESOURCES",
        modifier = modifier,
        actionButton = {
            ModuleActionButton(
                text = refreshInterval.label,
                onClick = onRefreshIntervalClick
            )
        }
    ) {
        SystemModuleStatus(
            resource = resources,
            hasActiveSession = hasActiveSession,
            lastUpdatedAt = lastUpdatedAt,
            emptyLabel = "No resource snapshot available",
            isRefreshing = isRefreshing
        )

        if (hasActiveSession) resources.dataOrNull()?.let { info ->
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            MetricBar(
                label = "CPU",
                valueLabel = info.cpuPercent?.let { "${it.toInt()}%" } ?: "--",
                progress = info.cpuPercent?.div(100f)
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            MetricBar(
                label = "RAM",
                valueLabel = formatBytesPair(info.memoryUsed, info.memoryTotal),
                progress = info.memoryUsageFraction
            )
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            MetricBar(
                label = "DISK",
                valueLabel = formatBytesPair(info.diskUsed, info.diskTotal),
                progress = info.diskUsageFraction
            )
        }
    }
}

@Composable
private fun ProcessRow(process: ProcessInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = process.command,
                color = AppColors.onSurface,
                style = AppTypography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = listOfNotNull(process.user, "pid ${process.pid}").joinToString(" • "),
                color = AppColors.outline,
                style = AppTypography.bodySmall,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(AppSpacing.md))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = process.cpu?.let { "${it.toInt()}% CPU" } ?: "CPU --",
                color = AppColors.primary,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = process.memory ?: "mem --",
                color = AppColors.onSurfaceVariant,
                style = AppTypography.labelSmall
            )
        }
    }
}

@Composable
private fun PortRow(port: PortInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${port.protocol.uppercase()} :${port.port}",
                color = AppColors.onSurface,
                style = AppTypography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = port.address,
                color = AppColors.outline,
                style = AppTypography.bodySmall,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(AppSpacing.md))
        Text(
            text = port.process ?: "unknown process",
            color = AppColors.onSurfaceVariant,
            style = AppTypography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun MetricBar(
    label: String,
    valueLabel: String,
    progress: Float?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = AppColors.outline,
                style = AppTypography.labelSmall
            )
            Text(
                text = valueLabel,
                color = AppColors.onSurface,
                style = AppTypography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(AppShapes.pill)
                .background(AppColors.surfaceContainerHigh)
        ) {
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(AppShapes.pill)
                        .background(AppColors.primary)
                )
            }
        }
    }
}

@Composable
private fun <T> SystemModuleStatus(
    resource: Resource<T>,
    hasActiveSession: Boolean,
    lastUpdatedAt: Long?,
    emptyLabel: String,
    isRefreshing: Boolean
) {
    val text = when {
        !hasActiveSession -> "Start a session to view system info"
        resource is Resource.Loading && resource.data == null -> "Collecting remote snapshot..."
        resource is Resource.Error && resource.data != null -> {
            val base = "Showing last known values"
            lastUpdatedAt?.let { "$base • stale at ${TimeFormatter.formatTimeWithSeconds(it)}" } ?: base
        }
        resource.dataOrNull() == null -> emptyLabel
        else -> {
            val status = if (isRefreshing) "Refreshing..." else "Live snapshot"
            lastUpdatedAt?.let { "$status • ${TimeFormatter.formatTimeWithSeconds(it)}" } ?: status
        }
    }

    Text(
        text = text,
        color = when {
            !hasActiveSession -> AppColors.onSurfaceVariant
            resource is Resource.Error -> AppColors.statusWaiting
            else -> AppColors.outline
        },
        style = AppTypography.bodySmall
    )
}

private fun formatBytesPair(used: Long?, total: Long?): String {
    if (used == null || total == null || total <= 0L) return "--"
    return "${formatBytes(used)} / ${formatBytes(total)}"
}

private fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    val rounded = if (value >= 10 || unitIndex == 0) {
        value.toInt().toString()
    } else {
        (((value * 10).toInt()) / 10.0).toString()
    }
    return "$rounded ${units[unitIndex]}"
}
