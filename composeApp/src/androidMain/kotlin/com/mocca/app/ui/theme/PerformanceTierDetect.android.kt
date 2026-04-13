package com.mocca.app.ui.theme

import android.app.ActivityManager
import android.content.Context
import org.koin.mp.KoinPlatformTools

actual fun detectPerformanceTier(): PerformanceTier {
    return runCatching {
        val context = KoinPlatformTools.defaultContext().get().get<Context>()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return PerformanceTier.MEDIUM

        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        when {
            memoryInfo.totalMem >= 6_000_000_000L -> PerformanceTier.HIGH
            memoryInfo.totalMem >= 3_000_000_000L -> PerformanceTier.MEDIUM
            else -> PerformanceTier.LOW
        }
    }.getOrDefault(PerformanceTier.MEDIUM)
}
