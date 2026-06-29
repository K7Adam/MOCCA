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
 * Uses the V2 session-scoped `POST /session/:id/permissions/:permissionID` API.
 */
object PermissionActionBus {

    /**
     * Represents a permission action from notification.
     * Uses [PermissionResponseType] to match the V2 permission API.
     */
    data class PermissionAction(
        val permissionId: String,
        val sessionId: String,
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
    suspend fun emit(permissionId: String, sessionId: String, replyType: PermissionResponseType) {
        _actions.emit(PermissionAction(permissionId, sessionId, replyType))
    }

    /**
     * Try to emit without suspension (for use from Android components)
     */
    fun tryEmit(permissionId: String, sessionId: String, replyType: PermissionResponseType): Boolean {
        return _actions.tryEmit(PermissionAction(permissionId, sessionId, replyType))
    }
}
