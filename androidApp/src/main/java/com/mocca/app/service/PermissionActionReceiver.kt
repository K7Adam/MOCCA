package com.mocca.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mocca.app.data.repository.PermissionActionBus
import com.mocca.app.domain.model.PermissionResponseType
import io.github.aakira.napier.Napier

/**
 * BroadcastReceiver for handling permission actions from notifications.
 *
 * This receiver handles APPROVE/DENY actions from permission request notifications
 * and forwards them to the shared PermissionActionBus for processing by the app.
 *
 * Uses the V2 session-scoped `POST /session/:id/permissions/:permissionID` API.
 */
class PermissionActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PERMISSION_APPROVE = "com.mocca.app.action.PERMISSION_APPROVE"
        const val ACTION_PERMISSION_DENY = "com.mocca.app.action.PERMISSION_DENY"

        const val EXTRA_PERMISSION_ID = "permission_id"
        const val EXTRA_SESSION_ID = "session_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val permissionId = intent.getStringExtra(EXTRA_PERMISSION_ID)
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)

        if (permissionId == null) {
            Napier.w("[PermissionActionReceiver] Missing permission_id extra")
            return
        }

        if (sessionId == null) {
            Napier.w("[PermissionActionReceiver] Missing session_id extra")
            return
        }

        val replyType = when (intent.action) {
            ACTION_PERMISSION_APPROVE -> PermissionResponseType.ONCE
            ACTION_PERMISSION_DENY -> PermissionResponseType.REJECT
            else -> {
                Napier.w("[PermissionActionReceiver] Unknown action: ${intent.action}")
                return
            }
        }

        Napier.i(
            "[PermissionActionReceiver] Permission $permissionId session=$sessionId " +
                "reply=${replyType.value} via notification"
        )

        // Emit to shared bus for processing via replyToPermission
        val emitted = PermissionActionBus.tryEmit(permissionId, sessionId, replyType)

        if (!emitted) {
            Napier.w("[PermissionActionReceiver] Failed to emit action - buffer full")
        }

        // Dismiss the notification
        ActiveSessionService.dismissPermissionNotification(context, permissionId)

        Napier.i("[PermissionActionReceiver] Permission ${replyType.value} via notification")
    }
}
