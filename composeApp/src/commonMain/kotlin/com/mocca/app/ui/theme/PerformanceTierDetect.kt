package com.mocca.app.ui.theme

/**
 * Detect the performance tier of the current device.
 * Uses platform-specific heuristics (RAM, CPU, SDK version).
 */
expect fun detectPerformanceTier(): PerformanceTier
