package com.mocca.app.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shared event bus for permission actions from notifications.
 * 
 * This allows the Android BroadcastReceiver to communicate with the
 * shared Kotlin repository layer without direct coupling.
 * 
 * Architecture:
 * - Android Notification -> BroadcastReceiver -> PermissionActionBus -> SessionRepository
 */
object PermissionActionBus {
    
    /**
     * Represents a permission action from notification
     */
    data class PermissionAction(
        val sessionId: String,
        val permissionId: String,
        val isApproved: Boolean
    )
    
    private val _actions = MutableSharedFlow<PermissionAction>(extraBufferCapacity = 16)
    
    /**
     * Flow of permission actions from notifications
     */
    val actions: SharedFlow<PermissionAction> = _actions.asSharedFlow()
    
    /**
     * Emit a permission action from notification
     */
    suspend fun emit(sessionId: String, permissionId: String, isApproved: Boolean) {
        _actions.emit(PermissionAction(sessionId, permissionId, isApproved))
    }
    
    /**
     * Try to emit without suspension (for use from Android components)
     */
    fun tryEmit(sessionId: String, permissionId: String, isApproved: Boolean): Boolean {
        return _actions.tryEmit(PermissionAction(sessionId, permissionId, isApproved))
    }
}
