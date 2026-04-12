package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ProcessInfo(
    val pid: String,
    val command: String,
    val cpu: Float? = null,
    val memory: String? = null,
    val user: String? = null
)

@Immutable
data class PortInfo(
    val port: Int,
    val protocol: String,
    val process: String? = null,
    val address: String
)

@Immutable
data class SystemResources(
    val cpuPercent: Float? = null,
    val memoryUsed: Long? = null,
    val memoryTotal: Long? = null,
    val diskUsed: Long? = null,
    val diskTotal: Long? = null
) {
    val memoryUsageFraction: Float?
        get() = if (memoryTotal != null && memoryTotal > 0 && memoryUsed != null) {
            (memoryUsed.toFloat() / memoryTotal.toFloat()).coerceIn(0f, 1f)
        } else null

    val diskUsageFraction: Float?
        get() = if (diskTotal != null && diskTotal > 0 && diskUsed != null) {
            (diskUsed.toFloat() / diskTotal.toFloat()).coerceIn(0f, 1f)
        } else null
}

enum class MonitorRefreshInterval(
    val label: String,
    val pollMs: Long?
) {
    SECONDS_5("5s", 5_000L),
    SECONDS_15("15s", 15_000L),
    SECONDS_30("30s", 30_000L),
    OFF("Off", null);

    fun next(): MonitorRefreshInterval = when (this) {
        SECONDS_5 -> SECONDS_15
        SECONDS_15 -> SECONDS_30
        SECONDS_30 -> OFF
        OFF -> SECONDS_5
    }
}
