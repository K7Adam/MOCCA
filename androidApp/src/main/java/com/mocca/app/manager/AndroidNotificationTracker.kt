package com.mocca.app.manager

import android.content.Context
import com.mocca.app.domain.manager.NotificationTracker
import com.mocca.app.service.ActiveSessionService

/**
 * Android implementation of NotificationTracker.
 * Delegates calls to the ActiveSessionService to manage foreground services
 * and system notifications.
 */
class AndroidNotificationTracker(private val context: Context) : NotificationTracker {

    override fun startSession(sessionId: String, sessionTitle: String?) {
        ActiveSessionService.start(context, sessionId, sessionTitle)
    }

    override fun stopSession(sessionId: String) {
        ActiveSessionService.stop(context, sessionId)
    }

    override fun stopAllSessions() {
        ActiveSessionService.stopAll(context)
    }

    override fun showPermissionNotification(
        sessionId: String,
        permissionId: String,
        title: String,
        description: String
    ) {
        ActiveSessionService.showPermissionNotification(
            context, sessionId, permissionId, title, description
        )
    }

    override fun dismissPermissionNotification(permissionId: String) {
        ActiveSessionService.dismissPermissionNotification(context, permissionId)
    }

    override fun showQuestionNotification(
        sessionId: String,
        questionId: String,
        question: String
    ) {
        ActiveSessionService.showQuestionNotification(
            context, sessionId, questionId, question
        )
    }

    override fun dismissQuestionNotification(questionId: String) {
        ActiveSessionService.dismissQuestionNotification(context, questionId)
    }

    override fun showAgentFinishedNotification(sessionId: String, sessionTitle: String) {
        ActiveSessionService.showAgentFinishedNotification(context, sessionId, sessionTitle)
    }

    override fun showAgentErrorNotification(sessionId: String, errorMessage: String) {
        ActiveSessionService.showAgentErrorNotification(context, sessionId, errorMessage)
    }

    override fun showConnectionLostNotification(reason: String?) {
        ActiveSessionService.showConnectionLostNotification(context, reason)
    }

    override fun updateProgressNotification(
        sessionId: String,
        sessionTitle: String,
        toolTitle: String?,
        modelName: String,
        elapsedSeconds: Long,
        totalCount: Int,
        completedCount: Int
    ) {
        ActiveSessionService.updateProgressNotification(
            context, sessionId, sessionTitle, toolTitle, modelName, elapsedSeconds, totalCount, completedCount
        )
    }
}
