package com.mocca.app.util

import kotlinx.coroutines.flow.StateFlow

/**
 * App lifecycle state for background/foreground detection.
 * Used to optimize connection management and SSE streaming.
 */
enum class AppLifecycleState {
    FOREGROUND,
    BACKGROUND
}

/**
 * Interface for observing app lifecycle changes.
 * Platform-specific implementations provide the actual observation.
 */
interface AppLifecycleObserver {
    /**
     * Current lifecycle state as a StateFlow for reactive observation.
     */
    val lifecycleState: StateFlow<AppLifecycleState>
    
    /**
     * Whether the app is currently in the foreground.
     */
    val isForeground: Boolean get() = lifecycleState.value == AppLifecycleState.FOREGROUND
    
    /**
     * Start observing lifecycle events.
     */
    fun startObserving()
    
    /**
     * Stop observing lifecycle events.
     */
    fun stopObserving()
}

/**
 * No-op implementation for platforms without lifecycle support.
 */
class NoOpAppLifecycleObserver : AppLifecycleObserver {
    override val lifecycleState = kotlinx.coroutines.flow.MutableStateFlow(AppLifecycleState.FOREGROUND)
    override fun startObserving() {}
    override fun stopObserving() {}
}
