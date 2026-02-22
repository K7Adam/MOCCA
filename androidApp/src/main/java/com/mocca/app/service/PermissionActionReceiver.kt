package com.mocca.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mocca.app.data.repository.PermissionActionBus
import io.github.aakira.napier.Napier

/**
 * BroadcastReceiver for handling permission actions from notifications.
 *
 * This receiver handles APPROVE/DENY actions from permission request notifications
 * and forwards them to the shared PermissionActionBus for processing by the app.
 */
class PermissionActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PERMISSION_APPROVE = "com.mocca.app.action.PERMISSION_APPROVE"
        const val ACTION_PERMISSION_DENY = "com.mocca.app.action.PERMISSION_DENY"

        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_PERMISSION_ID = "permission_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val permissionId = intent.getStringExtra(EXTRA_PERMISSION_ID)

        if (sessionId == null || permissionId == null) {
            Napier.w("[PermissionActionReceiver] Missing required extras")
            return
        }

        val isApproved = when (intent.action) {
            ACTION_PERMISSION_APPROVE -> true
            ACTION_PERMISSION_DENY -> false
            else -> {
                Napier.w("[PermissionActionReceiver] Unknown action: ${intent.action}")
                return
            }
        }

        val actionStr = if (isApproved) "approved" else "denied"
        Napier.i(
            "[PermissionActionReceiver] Permission $permissionId " +
                "$actionStr for session $sessionId"
        )

        // Emit to shared bus for processing by SessionRepository
        val emitted = PermissionActionBus.tryEmit(sessionId, permissionId, isApproved)

        if (!emitted) {
            Napier.w("[PermissionActionReceiver] Failed to emit action - buffer full")
        }

        // Dismiss the notification
        ActiveSessionService.dismissPermissionNotification(context, permissionId)

        // Log result
        val resultStr = if (isApproved) "granted" else "denied"
        Napier.i("[PermissionActionReceiver] Permission $resultStr via notification")
    }
}
