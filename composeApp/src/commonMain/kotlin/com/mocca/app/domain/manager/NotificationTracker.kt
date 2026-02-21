package com.mocca.app.domain.manager

/**
 * Interface for tracking active sessions and managing system notifications
 * across different platforms.
 */
interface NotificationTracker {
    
    /**
     * Start tracking an active session (e.g. show a foreground notification).
     */
    fun startSession(sessionId: String, sessionTitle: String? = null)
    
    /**
     * Stop tracking a specific session.
     */
    fun stopSession(sessionId: String)
    
    /**
     * Stop tracking all sessions.
     */
    fun stopAllSessions()
    
    /**
     * Show a permission request notification.
     */
    fun showPermissionNotification(
        sessionId: String,
        permissionId: String,
        title: String,
        description: String
    )
    
    /**
     * Dismiss a permission notification.
     */
    fun dismissPermissionNotification(permissionId: String)
    
    /**
     * Show a notification for a pending question.
     */
    fun showQuestionNotification(
        sessionId: String,
        questionId: String,
        question: String
    )
    
    /**
     * Dismiss a question notification.
     */
    fun dismissQuestionNotification(questionId: String)
    
    /**
     * Show a notification when an agent finishes its task.
     */
    fun showAgentFinishedNotification(sessionId: String, sessionTitle: String)
    
    /**
     * Show a notification when an agent encounters an error.
     */
    fun showAgentErrorNotification(sessionId: String, errorMessage: String)
    
    /**
     * Show a notification when the server connection is lost.
     */
    fun showConnectionLostNotification(reason: String? = null)
    
    /**
     * Update the progress of an active session.
     */
    fun updateProgressNotification(
        sessionId: String,
        sessionTitle: String,
        toolTitle: String?,
        modelName: String,
        elapsedSeconds: Long,
        totalCount: Int = 0,
        completedCount: Int = 0
    )
}
