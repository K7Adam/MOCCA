package com.mocca.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mocca.app.MainActivity
import com.mocca.app.android.R
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service for keeping connections alive during active sessions.
 *
 * This service is started when a session enters RUNNING state and stopped
 * when all sessions are IDLE. It ensures that:
 * - SSE connections are maintained even when the app is backgrounded
 * - Network operations continue during long LLM responses
 * - The user is notified of active background processing
 * - Actionable notifications for permissions and questions
 * - Android 16 Live Updates for running agent tasks with todo progress
 */
class ActiveSessionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _activeSessions = MutableStateFlow<Set<String>>(emptySet())
    val activeSessions: StateFlow<Set<String>> = _activeSessions.asStateFlow()

    private val binder = LocalBinder()

    // ─────────────────────────────────────────────────────────────────────────
    // Theme Colors - Pitch Black OLED with Mint Green accents
    // ─────────────────────────────────────────────────────────────────────────

    private val accentGreen = Color.parseColor("#00D9A5")
    private val pitchBlack = Color.parseColor("#000000")
    private val darkGrey = Color.parseColor("#1A1A1A")
    private val lightGrey = Color.parseColor("#666666")

    // ─────────────────────────────────────────────────────────────────────────
    // Notification Channel IDs - Multiple channels for different event types
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        // Channel IDs
        const val CHANNEL_AGENT_ACTIVE = "agent_active_channel"
        const val CHANNEL_AGENT_FINISHED = "agent_finished_channel"
        const val CHANNEL_PERMISSION_REQUIRED = "permission_required_channel"
        const val CHANNEL_AGENT_ERROR = "agent_error_channel"
        const val CHANNEL_CONNECTION_LOST = "connection_lost_channel"
        const val CHANNEL_QUESTION_PENDING = "question_pending_channel"

        // Notification IDs
        private const val NOTIFICATION_ID_ACTIVE = 1001
        private const val NOTIFICATION_ID_PERMISSION_PREFIX = 2000
        private const val NOTIFICATION_ID_QUESTION_PREFIX = 3000
        private const val NOTIFICATION_ID_ERROR = 4000
        private const val NOTIFICATION_ID_CONNECTION = 5000

        // Actions
        private const val ACTION_START = "com.mocca.app.action.START_SESSION"
        private const val ACTION_STOP = "com.mocca.app.action.STOP_SESSION"
        private const val ACTION_STOP_ALL = "com.mocca.app.action.STOP_ALL_SESSIONS"
        private const val ACTION_ABORT = "com.mocca.app.action.ABORT_SESSION"
        private const val ACTION_PERMISSION_APPROVE = "com.mocca.app.action.PERMISSION_APPROVE"
        private const val ACTION_PERMISSION_DENY = "com.mocca.app.action.PERMISSION_DENY"

        // Extras
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SESSION_TITLE = "session_title"
        const val EXTRA_PERMISSION_ID = "permission_id"
        const val EXTRA_MODEL_NAME = "model_name"
        const val EXTRA_ELAPSED_TIME = "elapsed_time"
        const val EXTRA_TOOL_TITLE = "tool_title"
        const val EXTRA_TOTAL_COUNT = "total_count"
        const val EXTRA_COMPLETED_COUNT = "completed_count"

        // Time constants
        private const val SECONDS_PER_MINUTE = 60L
        private const val MILLIS_PER_SECOND = 1000L

        // ─────────────────────────────────────────────────────────────────────
        // Public API - Start/Stop Service
        // ─────────────────────────────────────────────────────────────────────

        /**
         * Start the service for a session.
         */
        fun start(context: Context, sessionId: String, sessionTitle: String? = null) {
            val intent = Intent(context, ActiveSessionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_SESSION_TITLE, sessionTitle)
            }
            context.startForegroundService(intent)
        }

        /**
         * Stop the service for a specific session.
         */
        fun stop(context: Context, sessionId: String) {
            val intent = Intent(context, ActiveSessionService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startService(intent)
        }

        /**
         * Stop all sessions and the service.
         */
        fun stopAll(context: Context) {
            val intent = Intent(context, ActiveSessionService::class.java).apply {
                action = ACTION_STOP_ALL
            }
            context.startService(intent)
        }

        // ─────────────────────────────────────────────────────────────────────
        // Public API - Notifications
        // ─────────────────────────────────────────────────────────────────────

        /**
         * Show a permission request notification with Approve/Deny actions.
         */
        fun showPermissionNotification(
            context: Context,
            sessionId: String,
            permissionId: String,
            title: String,
            description: String
        ) {
            val serviceIntent = Intent(context, ActiveSessionService::class.java)
            context.startService(serviceIntent)

            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val approveIntent = Intent(context, PermissionActionReceiver::class.java).apply {
                action = PermissionActionReceiver.ACTION_PERMISSION_APPROVE
                putExtra(PermissionActionReceiver.EXTRA_SESSION_ID, sessionId)
                putExtra(PermissionActionReceiver.EXTRA_PERMISSION_ID, permissionId)
            }
            val approvePendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_PERMISSION_PREFIX + permissionId.hashCode(),
                approveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val denyIntent = Intent(context, PermissionActionReceiver::class.java).apply {
                action = PermissionActionReceiver.ACTION_PERMISSION_DENY
                putExtra(PermissionActionReceiver.EXTRA_SESSION_ID, sessionId)
                putExtra(PermissionActionReceiver.EXTRA_PERMISSION_ID, permissionId)
            }
            val denyPendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_PERMISSION_PREFIX + permissionId.hashCode() + 1,
                denyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_PERMISSION_REQUIRED)
                .setContentTitle("Permission Required")
                .setContentText(title)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n\n$description"))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .addAction(R.drawable.ic_launcher_foreground, "Approve", approvePendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Deny", denyPendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .build()

            notificationManager.notify(
                NOTIFICATION_ID_PERMISSION_PREFIX + permissionId.hashCode(),
                notification
            )
        }

        /**
         * Dismiss a permission notification.
         */
        fun dismissPermissionNotification(context: Context, permissionId: String) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.cancel(NOTIFICATION_ID_PERMISSION_PREFIX + permissionId.hashCode())
        }

        /**
         * Show an agent finished notification.
         */
        fun showAgentFinishedNotification(
            context: Context,
            sessionId: String,
            sessionTitle: String
        ) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                sessionId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_AGENT_FINISHED)
                .setContentTitle("Task Completed")
                .setContentText(sessionTitle)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()

            notificationManager.notify(sessionId.hashCode(), notification)
        }

        /**
         * Show an agent error notification.
         */
        fun showAgentErrorNotification(
            context: Context,
            sessionId: String,
            errorMessage: String
        ) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val notification = NotificationCompat.Builder(context, CHANNEL_AGENT_ERROR)
                .setContentTitle("Agent Error")
                .setContentText(errorMessage)
                .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .build()

            notificationManager.notify(NOTIFICATION_ID_ERROR + sessionId.hashCode(), notification)
        }

        /**
         * Show a connection lost notification.
         */
        fun showConnectionLostNotification(
            context: Context,
            reason: String? = null
        ) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val notification = NotificationCompat.Builder(context, CHANNEL_CONNECTION_LOST)
                .setContentTitle("Connection Lost")
                .setContentText(reason ?: "Unable to reach OpenCode server")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()

            notificationManager.notify(NOTIFICATION_ID_CONNECTION, notification)
        }

        /**
         * Show a question pending notification.
         */
        fun showQuestionNotification(
            context: Context,
            sessionId: String,
            questionId: String,
            question: String
        ) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                questionId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_QUESTION_PENDING)
                .setContentTitle("Question from Agent")
                .setContentText(question)
                .setStyle(NotificationCompat.BigTextStyle().bigText(question))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(
                NOTIFICATION_ID_QUESTION_PREFIX + questionId.hashCode(),
                notification
            )
        }

        /**
         * Dismiss a question notification.
         */
        fun dismissQuestionNotification(context: Context, questionId: String) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.cancel(NOTIFICATION_ID_QUESTION_PREFIX + questionId.hashCode())
        }

        /**
         * Update the active session notification with progress (legacy method).
         */
        fun updateProgressNotification(
            context: Context,
            progressInfo: ProgressInfo
        ) {
            // Convert to empty todo list and use new method
            updateProgressNotificationWithTodos(
                context = context,
                sessionTitle = progressInfo.sessionTitle,
                currentTask = progressInfo.toolTitle,
                todos = emptyList(),
                elapsedSeconds = progressInfo.elapsedSeconds,
                modelName = progressInfo.modelName
            )
        }

        /**
         * Update the progress notification with detailed todo information.
         * Uses Android 16 ProgressStyle for promoted live notifications.
         *
         * @param context Android context
         * @param sessionTitle The session title
         * @param currentTask The currently executing task (in_progress todo)
         * @param todos List of todo items with their content and status
         * @param elapsedSeconds Elapsed time since session started
         * @param modelName The AI model being used
         */
        fun updateProgressNotificationWithTodos(
            context: Context,
            sessionTitle: String,
            currentTask: String?,
            todos: List<TodoInfo>,
            elapsedSeconds: Long,
            modelName: String
        ) {
            val serviceIntent = Intent(context, ActiveSessionService::class.java)
            context.startService(serviceIntent)

            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calculate progress
            val completedCount = todos.count { it.status == "completed" }
            val inProgressCount = todos.count { it.status == "in_progress" }
            val totalCount = todos.size

            // Format elapsed time
            val minutes = elapsedSeconds / SECONDS_PER_MINUTE
            val seconds = elapsedSeconds % SECONDS_PER_MINUTE
            val timeStr = if (minutes > 0) {
                "${minutes}m ${seconds}s"
            } else {
                "${seconds}s"
            }

            // Build short critical text for status bar chip
            val shortCriticalText = if (totalCount > 0) {
                "$completedCount/$totalCount"
            } else {
                timeStr
            }

            // Build content text
            val contentText = when {
                currentTask != null -> currentTask
                inProgressCount > 0 -> {
                    val inProgressTodo = todos.find { it.status == "in_progress" }
                    inProgressTodo?.content ?: "Processing..."
                }
                totalCount > 0 -> "Task progress: $completedCount/$totalCount"
                else -> "$modelName • $timeStr"
            }

            // Build the notification based on Android version
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                buildAndroid16ProgressNotification(
                    context = context,
                    sessionTitle = sessionTitle,
                    contentText = contentText,
                    shortCriticalText = shortCriticalText,
                    pendingIntent = pendingIntent,
                    todos = todos,
                    elapsedSeconds = elapsedSeconds
                )
            } else {
                buildLegacyProgressNotification(
                    context = context,
                    sessionTitle = sessionTitle,
                    contentText = contentText,
                    pendingIntent = pendingIntent,
                    todos = todos,
                    elapsedSeconds = elapsedSeconds,
                    totalCount = totalCount,
                    completedCount = completedCount
                )
            }

            notificationManager.notify(NOTIFICATION_ID_ACTIVE, notification)
        }

        /**
         * Build Android 16+ ProgressStyle notification with segments and points.
         */
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        private fun buildAndroid16ProgressNotification(
            context: Context,
            sessionTitle: String,
            contentText: String,
            shortCriticalText: String,
            pendingIntent: PendingIntent,
            todos: List<TodoInfo>,
            elapsedSeconds: Long
        ): Notification {
            val completedCount = todos.count { it.status == "completed" }
            val totalCount = todos.size.coerceAtLeast(1)

            // Build progress segments based on todo statuses
            val segments = buildProgressSegments(todos)

            // Build the ProgressStyle
            // Note: Android 16 ProgressStyle.setProgress() takes single Int parameter
            val progressStyle = Notification.ProgressStyle()
                .setProgress(completedCount * 100) // Current progress value
                .setProgressSegments(segments)

            // Add progress points for visual milestones
            if (todos.isNotEmpty()) {
                val points = mutableListOf<Notification.ProgressStyle.Point>()
                
                // Add point at current progress position
                val progressPercent = (completedCount * 100f / totalCount).toInt()
                if (progressPercent > 0 && progressPercent < 100) {
                    points.add(
                        Notification.ProgressStyle.Point(progressPercent)
                            .setColor(Color.parseColor("#00D9A5")) // Mint green
                    )
                }
                
                if (points.isNotEmpty()) {
                    progressStyle.setProgressPoints(points)
                }
            }

            return Notification.Builder(context, CHANNEL_AGENT_ACTIVE)
                .setContentTitle(sessionTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setWhen(System.currentTimeMillis() - elapsedSeconds * MILLIS_PER_SECOND)
                .setUsesChronometer(true)
                .setShortCriticalText(shortCriticalText)
                .setStyle(progressStyle)
                .build()
        }

        /**
         * Build progress segments based on todo statuses.
         * Each todo becomes a segment with color based on its status.
         */
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        private fun buildProgressSegments(todos: List<TodoInfo>): List<Notification.ProgressStyle.Segment> {
            if (todos.isEmpty()) {
                // Indeterminate progress segment
                return listOf(
                    Notification.ProgressStyle.Segment(100)
                        .setColor(Color.parseColor("#333333"))
                )
            }

            return todos.map { todo ->
                val color = when (todo.status) {
                    "completed" -> Color.parseColor("#00D9A5")  // Mint green
                    "in_progress" -> Color.parseColor("#FFB800") // Amber
                    "cancelled" -> Color.parseColor("#666666")   // Grey
                    "pending" -> Color.parseColor("#333333")     // Dark grey
                    else -> Color.parseColor("#333333")
                }
                Notification.ProgressStyle.Segment(1).setColor(color)
            }
        }

        /**
         * Build legacy (pre-Android 16) notification with BigTextStyle.
         */
        private fun buildLegacyProgressNotification(
            context: Context,
            sessionTitle: String,
            contentText: String,
            pendingIntent: PendingIntent,
            todos: List<TodoInfo>,
            elapsedSeconds: Long,
            totalCount: Int,
            completedCount: Int
        ): Notification {
            // Build big text with todo list
            val bigTextBuilder = StringBuilder()
            bigTextBuilder.append(contentText)
            
            if (todos.isNotEmpty()) {
                bigTextBuilder.append("\n\n")
                bigTextBuilder.append("━".repeat(20))
                bigTextBuilder.append("\n")
                
                todos.take(5).forEach { todo ->
                    val icon = when (todo.status) {
                        "completed" -> "✓"
                        "in_progress" -> "►"
                        "cancelled" -> "✗"
                        "pending" -> "○"
                        else -> "○"
                    }
                    bigTextBuilder.append("$icon ${todo.content}\n")
                }
                
                if (todos.size > 5) {
                    bigTextBuilder.append("... +${todos.size - 5} more")
                }
            }

            return NotificationCompat.Builder(context, CHANNEL_AGENT_ACTIVE)
                .setContentTitle(sessionTitle)
                .setContentText(contentText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(bigTextBuilder.toString())
                )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setProgress(totalCount.coerceAtLeast(0), completedCount, totalCount == 0)
                .setWhen(System.currentTimeMillis() - elapsedSeconds * MILLIS_PER_SECOND)
                .setUsesChronometer(true)
                .build()
        }
    }

    /**
     * Data class for progress notification parameters.
     */
    data class ProgressInfo(
        val sessionTitle: String,
        val toolTitle: String?,
        val modelName: String,
        val elapsedSeconds: Long,
        val totalCount: Int = 0,
        val completedCount: Int = 0
    )

    /**
     * Data class for todo item information in notifications.
     */
    data class TodoInfo(
        val content: String,
        val status: String,  // "pending", "in_progress", "completed", "cancelled"
        val priority: String // "high", "medium", "low"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Service Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    inner class LocalBinder : Binder() {
        fun getService(): ActiveSessionService = this@ActiveSessionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createAllNotificationChannels()
        Napier.i("[ActiveSessionService] Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                val sessionTitle = intent.getStringExtra(EXTRA_SESSION_TITLE)
                if (sessionId != null) {
                    addActiveSession(sessionId, sessionTitle)
                }
            }
            ACTION_STOP -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                if (sessionId != null) {
                    removeActiveSession(sessionId)
                }
            }
            ACTION_STOP_ALL -> {
                stopAllSessions()
            }
            ACTION_ABORT -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                if (sessionId != null) {
                    Napier.i("[ActiveSessionService] Abort requested for session: $sessionId")
                }
            }
            ACTION_PERMISSION_APPROVE -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                val permissionId = intent.getStringExtra(EXTRA_PERMISSION_ID)
                if (sessionId != null && permissionId != null) {
                    Napier.i("[ActiveSessionService] Permission approved: $permissionId")
                    dismissPermissionNotification(permissionId)
                }
            }
            ACTION_PERMISSION_DENY -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                val permissionId = intent.getStringExtra(EXTRA_PERMISSION_ID)
                if (sessionId != null && permissionId != null) {
                    Napier.i("[ActiveSessionService] Permission denied: $permissionId")
                    dismissPermissionNotification(permissionId)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Napier.i("[ActiveSessionService] Service destroyed")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Session Management
    // ─────────────────────────────────────────────────────────────────────────

    private fun addActiveSession(sessionId: String, sessionTitle: String?) {
        val currentSessions = _activeSessions.value
        if (sessionId in currentSessions) return

        val newSessions = currentSessions + sessionId
        _activeSessions.value = newSessions

        Napier.i("[ActiveSessionService] Added session: $sessionId, total: ${newSessions.size}")

        if (currentSessions.isEmpty()) {
            startForeground(sessionTitle)
        } else {
            updateNotification()
        }
    }

    private fun removeActiveSession(sessionId: String) {
        val currentSessions = _activeSessions.value
        val newSessions = currentSessions - sessionId
        _activeSessions.value = newSessions

        Napier.i("[ActiveSessionService] Removed session: $sessionId, remaining: ${newSessions.size}")

        if (newSessions.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            updateNotification()
        }
    }

    private fun stopAllSessions() {
        _activeSessions.value = emptySet()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Napier.i("[ActiveSessionService] All sessions stopped")
    }

    private fun dismissPermissionNotification(permissionId: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID_PERMISSION_PREFIX + permissionId.hashCode())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Foreground Service
    // ─────────────────────────────────────────────────────────────────────────

    private fun startForeground(sessionTitle: String?) {
        val notification = createActiveSessionNotification(sessionTitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID_ACTIVE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID_ACTIVE, notification)
        }

        Napier.i("[ActiveSessionService] Started foreground")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification Channels
    // ─────────────────────────────────────────────────────────────────────────

    private fun createAllNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannels(listOf(
            createAgentActiveChannel(),
            createAgentFinishedChannel(),
            createPermissionChannel(),
            createErrorChannel(),
            createConnectionChannel(),
            createQuestionChannel()
        ))
    }

    private fun createAgentActiveChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_AGENT_ACTIVE,
            "Active Agent",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when an agent is actively processing tasks"
            setShowBadge(false)
        }
    }

    private fun createAgentFinishedChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_AGENT_FINISHED,
            "Task Completed",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies when an agent completes a task"
            setShowBadge(true)
        }
    }

    private fun createPermissionChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_PERMISSION_REQUIRED,
            "Permission Required",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Urgent: Agent needs permission to proceed"
            setShowBadge(true)
            enableVibration(true)
        }
    }

    private fun createErrorChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_AGENT_ERROR,
            "Agent Errors",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when an agent encounters an error"
            setShowBadge(true)
        }
    }

    private fun createConnectionChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_CONNECTION_LOST,
            "Connection Status",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies when connection to server is lost"
            setShowBadge(false)
        }
    }

    private fun createQuestionChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_QUESTION_PENDING,
            "Agent Questions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Agent is waiting for your input"
            setShowBadge(true)
            enableVibration(true)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification Builders
    // ─────────────────────────────────────────────────────────────────────────

    private fun createActiveSessionNotification(sessionTitle: String?): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_AGENT_ACTIVE)
            .setContentTitle("MOCCA")
            .setContentText(
                sessionTitle?.let { "Processing: $it" }
                    ?: "Processing AI response..."
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setProgress(0, 0, true)
            .build()
    }

    private fun updateNotification() {
        val sessionCount = _activeSessions.value.size
        val notificationManager = getSystemService(NotificationManager::class.java)

        val notification = NotificationCompat.Builder(this, CHANNEL_AGENT_ACTIVE)
            .setContentTitle("MOCCA")
            .setContentText(
                when {
                    sessionCount == 1 -> "Processing 1 session..."
                    else -> "Processing $sessionCount sessions..."
                }
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setProgress(0, 0, true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ACTIVE, notification)
    }
}
