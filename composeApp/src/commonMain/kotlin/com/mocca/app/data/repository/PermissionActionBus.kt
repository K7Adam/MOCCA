package com.mocca.app.data.repository

import com.mocca.app.domain.model.PermissionResponseType
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
 * - Android Notification -> BroadcastReceiver -> PermissionActionBus -> replyToPermission
 *
 * Uses the top-level /permission/:requestID/reply API (not session-scoped).
 */
object PermissionActionBus {

    /**
     * Represents a permission action from notification.
     * Uses [PermissionResponseType] to match the /permission/:requestID/reply API.
     */
    data class PermissionAction(
        val permissionId: String,
        val replyType: PermissionResponseType
    )

    private val _actions = MutableSharedFlow<PermissionAction>(extraBufferCapacity = 16)

    /**
     * Flow of permission actions from notifications
     */
    val actions: SharedFlow<PermissionAction> = _actions.asSharedFlow()

    /**
     * Emit a permission action from notification
     */
    suspend fun emit(permissionId: String, replyType: PermissionResponseType) {
        _actions.emit(PermissionAction(permissionId, replyType))
    }

    /**
     * Try to emit without suspension (for use from Android components)
     */
    fun tryEmit(permissionId: String, replyType: PermissionResponseType): Boolean {
        return _actions.tryEmit(PermissionAction(permissionId, replyType))
    }
}
