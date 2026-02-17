package com.mocca.app.util

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of AppLifecycleObserver.
 * Uses ProcessLifecycleOwner to detect app background/foreground transitions.
 * 
 * This is critical for:
 * - Pausing SSE streaming when app is backgrounded (saves battery)
 * - Reducing health check frequency when backgrounded
 * - Resuming connections when app returns to foreground
 */
class AndroidAppLifecycleObserver(
    private val application: Application
) : AppLifecycleObserver, DefaultLifecycleObserver {
    
    private val _lifecycleState = MutableStateFlow(AppLifecycleState.FOREGROUND)
    override val lifecycleState: StateFlow<AppLifecycleState> = _lifecycleState.asStateFlow()
    
    private var isObserving = false
    
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            // App came to foreground
            Napier.i("[AppLifecycle] App moved to FOREGROUND")
            _lifecycleState.value = AppLifecycleState.FOREGROUND
        }
        
        override fun onStop(owner: LifecycleOwner) {
            // App went to background (screen off, user switched apps, etc.)
            Napier.i("[AppLifecycle] App moved to BACKGROUND")
            _lifecycleState.value = AppLifecycleState.BACKGROUND
        }
    }
    
    override fun startObserving() {
        if (isObserving) return
        isObserving = true
        
        // Register with ProcessLifecycleOwner for app-wide lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        Napier.i("[AppLifecycle] Started observing app lifecycle")
    }
    
    override fun stopObserving() {
        if (!isObserving) return
        isObserving = false
        
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        Napier.i("[AppLifecycle] Stopped observing app lifecycle")
    }
}
