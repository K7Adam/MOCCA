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
    
    /**
     * Update the progress notification with detailed todo information.
     * This provides rich progress tracking with individual todo states.
     * 
     * @param sessionId The session ID
     * @param sessionTitle The session title
     * @param currentTask The currently executing task (in_progress todo)
     * @param todos List of todo items with their content and status
     * @param elapsedSeconds Elapsed time since session started
     * @param modelName The AI model being used
     */
    fun updateProgressNotificationWithTodos(
        sessionId: String,
        sessionTitle: String,
        currentTask: String?,
        todos: List<TodoProgressInfo>,
        elapsedSeconds: Long,
        modelName: String
    )
}

/**
 * Data class representing a todo item's progress information for notifications.
 */
data class TodoProgressInfo(
    val content: String,
    val status: TodoStatus,
    val priority: TodoPriority
)

/**
 * Enum representing todo status for notifications.
 * Matches domain.model.TodoStatus but kept here for platform independence.
 */
enum class TodoStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

/**
 * Enum representing todo priority for notifications.
 * Matches domain.model.TodoPriority but kept here for platform independence.
 */
enum class TodoPriority {
    HIGH,
    MEDIUM,
    LOW
}
