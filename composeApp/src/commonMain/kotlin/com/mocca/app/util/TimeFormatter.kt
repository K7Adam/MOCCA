package com.mocca.app.util

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Centralized time formatting utilities.
 * Uses kotlinx-datetime for cross-platform support.
 */
object TimeFormatter {
    
    /**
     * Format timestamp as HH:mm (24-hour format).
     */
    fun formatTime(epochMillis: Long): String {
        if (epochMillis == 0L) return "--:--"
        return try {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            "--:--"
        }
    }
    
    /**
     * Format timestamp as HH:mm:ss.
     */
    fun formatTimeWithSeconds(epochMillis: Long): String {
        if (epochMillis == 0L) return "--:--:--"
        return try {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.hour.toString().padStart(2, '0')}:" +
            "${local.minute.toString().padStart(2, '0')}:" +
            "${local.second.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            "--:--:--"
        }
    }
    
    /**
     * Format timestamp as relative time (e.g., "2m ago", "1h ago", "yesterday").
     */
    fun formatTimeAgo(epochMillis: Long): String {
        if (epochMillis == 0L) return "--"
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val diff = now - epochMillis
            
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            when {
                seconds < 60 -> "just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days == 1L -> "yesterday"
                days < 7 -> "${days}d ago"
                else -> formatDate(epochMillis)
            }
        } catch (e: Exception) {
            "--"
        }
    }
    
    /**
     * Format timestamp as date (e.g., "Jan 15").
     */
    fun formatDate(epochMillis: Long): String {
        if (epochMillis == 0L) return "--"
        return try {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            "$month ${local.day}"
        } catch (e: Exception) {
            "--"
        }
    }
    
    /**
     * Format timestamp as full date/time (e.g., "Jan 15, 14:30").
     */
    fun formatDateTime(epochMillis: Long): String {
        if (epochMillis == 0L) return "--"
        return try {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            "$month ${local.day}, ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
        } catch (e: Exception) {
            "--"
        }
    }
    
    /**
     * Format duration in milliseconds as human-readable string.
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs < 0) return "--"
        
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
