package com.mocca.app.util

/**
 * General formatting utilities for the MOCCA app.
 */
object FormatUtils {
    
    /**
     * Format a large number (e.g. token counts) into a compact string (e.g. 1.2M, 15K).
     */
    fun formatCompactNumber(count: Int): String {
        return when {
            count >= 1_000_000 -> {
                val millions = count / 1_000_000f
                if (millions >= 10) millions.toInt().toString() + "M" 
                else ((millions * 10).toInt() / 10f).toString() + "M"
            }
            count >= 1_000 -> {
                val thousands = count / 1_000f
                if (thousands >= 10) thousands.toInt().toString() + "K"
                else ((thousands * 10).toInt() / 10f).toString() + "K"
            }
            else -> count.toString()
        }
    }

    fun formatCost(cost: Double): String {
        return if (cost < 0.01 && cost > 0.0) "<$0.01"
        else "$${"%.2f".format(cost)}"
    }
}
