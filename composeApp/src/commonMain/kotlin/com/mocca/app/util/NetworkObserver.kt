package com.mocca.app.util

import kotlinx.coroutines.flow.Flow

/**
 * Network connectivity observer interface.
 */
interface NetworkObserver {
    val isOnline: Flow<Boolean>
    fun isCurrentlyOnline(): Boolean
}

expect class NetworkObserverImpl : NetworkObserver
