package com.mocca.app.manager

import android.content.Context
import android.content.Intent
import com.mocca.app.domain.manager.NotificationTracker
import com.mocca.app.domain.manager.TodoProgressInfo
import com.mocca.app.service.ActiveSessionService
import com.mocca.app.service.ActiveSessionService.TodoInfo

/**
 * Android implementation of NotificationTracker.
 * 
 * Uses intent-based communication with ActiveSessionService to ensure
 * all notification state is managed by the service.
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
        // Use intent-based update
        sendUpdateIntent(
            sessionId = sessionId,
            title = sessionTitle,
            currentTask = toolTitle,
            todos = emptyList(),
            elapsedSeconds = elapsedSeconds,
            modelName = modelName
        )
    }

    override fun updateProgressNotificationWithTodos(
        sessionId: String,
        sessionTitle: String,
        currentTask: String?,
        todos: List<TodoProgressInfo>,
        elapsedSeconds: Long,
        modelName: String
    ) {
        val todoInfos = todos.map { todo ->
            TodoInfo(
                content = todo.content,
                status = when (todo.status) {
                    com.mocca.app.domain.manager.TodoStatus.PENDING -> "pending"
                    com.mocca.app.domain.manager.TodoStatus.IN_PROGRESS -> "in_progress"
                    com.mocca.app.domain.manager.TodoStatus.COMPLETED -> "completed"
                    com.mocca.app.domain.manager.TodoStatus.CANCELLED -> "cancelled"
                },
                priority = when (todo.priority) {
                    com.mocca.app.domain.manager.TodoPriority.HIGH -> "high"
                    com.mocca.app.domain.manager.TodoPriority.MEDIUM -> "medium"
                    com.mocca.app.domain.manager.TodoPriority.LOW -> "low"
                }
            )
        }

        sendUpdateIntent(
            sessionId = sessionId,
            title = sessionTitle,
            currentTask = currentTask,
            todos = todoInfos,
            elapsedSeconds = elapsedSeconds,
            modelName = modelName
        )
    }

    private fun sendUpdateIntent(
        sessionId: String,
        title: String,
        currentTask: String?,
        todos: List<TodoInfo>,
        elapsedSeconds: Long,
        modelName: String
    ) {
        val intent = Intent(context, ActiveSessionService::class.java).apply {
            action = "com.mocca.app.action.UPDATE_SESSION"
            putExtra("sessionId", sessionId)
            putExtra("title", title)
            putExtra("currentTask", currentTask)
            putExtra("elapsedSeconds", elapsedSeconds)
            putExtra("modelName", modelName)
            putExtra("todoCount", todos.size)
            todos.forEachIndexed { index, todo ->
                putExtra("todo_${index}_content", todo.content)
                putExtra("todo_${index}_status", todo.status)
                putExtra("todo_${index}_priority", todo.priority)
            }
        }
        context.startForegroundService(intent)
    }
}
