@file:Suppress(
    "TooManyFunctions",
    "LongParameterList",
    "LongMethod",
    "CyclomaticComplexMethod",
    "StringLiteralDuplication",
    "MagicNumber",
    "UnusedPrivateProperty",
    "UnusedParameter"
)

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
import com.mocca.app.widget.WidgetUpdateHelper
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Foreground service for keeping connections alive during active sessions.
 *
 * Architecture:
 * - Service owns all notification state via [sessionStates] map
 * - Single summary notification shows all active sessions
 * - Updates come via binder methods (not static)
 * - Proper lifecycle management prevents notification flickering
 *
 * Features:
 * - Multi-session tracking with per-session state
 * - Android 16 Live Updates with ProgressStyle
 * - Notification grouping for multiple sessions
 * - Permission and question actionable notifications
 */
class ActiveSessionService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val binder = LocalBinder()

    /** Per-session state stored in service */
    data class SessionState(
        val sessionId: String,
        val title: String,
        val currentTask: String? = null,
        val todos: List<TodoInfo> = emptyList(),
        val elapsedSeconds: Long = 0,
        val modelName: String = "Agent",
        val startTime: Long = System.currentTimeMillis(),
        val lastUpdated: Long = System.currentTimeMillis()
    ) {
        val completedCount: Int get() = todos.count { it.status == "completed" }
        val inProgressCount: Int get() = todos.count { it.status == "in_progress" }
        val totalCount: Int get() = todos.size
        val progressPercent: Int get() = if (totalCount > 0) completedCount * 100 / totalCount else 0
    }

    /** Thread-safe session state storage */
    private val sessionStates = ConcurrentHashMap<String, SessionState>()

    /** Active session IDs for quick lookup */
    private val _activeSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val activeSessionIds: StateFlow<Set<String>> = _activeSessionIds.asStateFlow()

    // Note: accentGreen is intentionally retained here as a semantic success color
    // for completed tasks in notifications, rather than a general brand accent.
    private val accentGreen = Color.parseColor("#00D9A5")
    private val amberColor = Color.parseColor("#FFB800")
    private val greyColor = Color.parseColor("#666666")
    private val darkGreyColor = Color.parseColor("#333333")

    companion object {
        // Channel IDs
        const val CHANNEL_AGENT_ACTIVE = "agent_active_channel"
        const val CHANNEL_AGENT_FINISHED = "agent_finished_channel"
        const val CHANNEL_PERMISSION_REQUIRED = "permission_required_channel"
        const val CHANNEL_AGENT_ERROR = "agent_error_channel"
        const val CHANNEL_CONNECTION_LOST = "connection_lost_channel"
        const val CHANNEL_QUESTION_PENDING = "question_pending_channel"

        // Status constants
        const val STATUS_COMPLETED = "completed"
        const val STATUS_IN_PROGRESS = "in_progress"

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

        // Extras
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SESSION_TITLE = "session_title"
        const val EXTRA_QUESTION_REPLY = "question_reply_text"

        // Time constants
        private const val SECONDS_PER_MINUTE = 60L
        private const val MILLIS_PER_SECOND = 1000L

        /**
         * Check if the device supports promoted notifications (Live Updates).
         * Requires Android 16 (BAKLAVA / API 35) or above.
         */
        fun supportsPromotedNotifications(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
        }

        /**
         * Check if the app is allowed to post promoted notifications.
         * Returns false on devices that don't support Live Updates.
         */
        fun canPostPromotedNotifications(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
            return runCatching {
                context.getSystemService(NotificationManager::class.java)
                    .canPostPromotedNotifications()
            }.getOrDefault(false)
        }

        /**
         * Create an intent to open the system settings for promoted notifications
         * (Live Updates) for this app. Returns null if not supported.
         */
        fun createPromotedNotificationsSettingsIntent(context: Context): Intent? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return null
            return runCatching {
                // ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS was added in API 35
                // Use string literal for forward compatibility
                Intent("android.settings.ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS")
                    .setData(android.net.Uri.parse("package:${context.packageName}"))
            }.getOrNull()
        }

        fun start(context: Context, sessionId: String, sessionTitle: String? = null) {
            val intent = Intent(context, ActiveSessionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_SESSION_TITLE, sessionTitle)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context, sessionId: String) {
            val intent = Intent(context, ActiveSessionService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startService(intent)
        }

        fun stopAll(context: Context) {
            val intent = Intent(context, ActiveSessionService::class.java).apply {
                action = ACTION_STOP_ALL
            }
            context.startService(intent)
        }

        fun showPermissionNotification(
            context: Context,
            sessionId: String,
            permissionId: String,
            title: String,
            description: String
        ) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val approveIntent = Intent(context, PermissionActionReceiver::class.java).apply {
                action = PermissionActionReceiver.ACTION_PERMISSION_APPROVE
                putExtra(PermissionActionReceiver.EXTRA_PERMISSION_ID, permissionId)
                putExtra(PermissionActionReceiver.EXTRA_SESSION_ID, sessionId)
            }
            val approvePendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_PERMISSION_PREFIX + permissionId.hashCode(),
                approveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val denyIntent = Intent(context, PermissionActionReceiver::class.java).apply {
                action = PermissionActionReceiver.ACTION_PERMISSION_DENY
                putExtra(PermissionActionReceiver.EXTRA_PERMISSION_ID, permissionId)
                putExtra(PermissionActionReceiver.EXTRA_SESSION_ID, sessionId)
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

        fun dismissPermissionNotification(context: Context, permissionId: String) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.cancel(NOTIFICATION_ID_PERMISSION_PREFIX + permissionId.hashCode())
        }

        fun showAgentFinishedNotification(context: Context, sessionId: String, sessionTitle: String) {
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

        fun showAgentErrorNotification(context: Context, sessionId: String, errorMessage: String) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // The errorMessage from EventStreamRepository is formatted as "Title: Message".
            // Split it for a cleaner notification with the title as headline and message as body.
            val (title, body) = if (errorMessage.contains(": ")) {
                errorMessage.substringBefore(": ") to errorMessage.substringAfter(": ")
            } else {
                "Agent Error" to errorMessage
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_AGENT_ERROR)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(NOTIFICATION_ID_ERROR + sessionId.hashCode(), notification)
        }

        fun showConnectionLostNotification(context: Context, reason: String? = null) {
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

        fun showQuestionNotification(
            context: Context,
            sessionId: String,
            questionId: String,
            question: String,
            options: List<String> = emptyList(),
            multiple: Boolean = false
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

            val builder = NotificationCompat.Builder(context, CHANNEL_QUESTION_PENDING)
                .setContentTitle("Question from Agent")
                .setContentText(question)
                .setStyle(NotificationCompat.BigTextStyle().bigText(question))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            // Reject action (always present)
            val rejectIntent = Intent(context, QuestionActionReceiver::class.java).apply {
                action = QuestionActionReceiver.ACTION_QUESTION_REJECT
                putExtra(QuestionActionReceiver.EXTRA_QUESTION_ID, questionId)
                putExtra(QuestionActionReceiver.EXTRA_SESSION_ID, sessionId)
            }
            val rejectPendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_QUESTION_PREFIX + questionId.hashCode() + 1,
                rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_launcher_foreground, "Reject", rejectPendingIntent)

            if (options.isNotEmpty()) {
                // Option-based question: show up to 4 option buttons (Android limit)
                // For multiple-select, the user taps each option; we reply with all selected.
                // Since Android notification actions are one-shot, each option tap sends
                // a reply with that single option. For multiple-select, the user must
                // open the app to select multiple.
                val maxOptions = minOf(options.size, 4)
                for (i in 0 until maxOptions) {
                    val optionLabel = options[i]
                    val optionIntent = Intent(context, QuestionActionReceiver::class.java).apply {
                        action = QuestionActionReceiver.ACTION_QUESTION_OPTION
                        putExtra(QuestionActionReceiver.EXTRA_QUESTION_ID, questionId)
                        putExtra(QuestionActionReceiver.EXTRA_SESSION_ID, sessionId)
                        putExtra(QuestionActionReceiver.EXTRA_OPTION_LABEL, optionLabel)
                    }
                    val optionPendingIntent = PendingIntent.getBroadcast(
                        context,
                        NOTIFICATION_ID_QUESTION_PREFIX + questionId.hashCode() + 10 + i,
                        optionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    // Truncate label to 20 chars for the action button
                    val displayLabel = if (optionLabel.length > 20) {
                        optionLabel.take(17) + "..."
                    } else {
                        optionLabel
                    }
                    builder.addAction(R.drawable.ic_launcher_foreground, displayLabel, optionPendingIntent)
                }
                // If there are more than 4 options, add a "More..." action that opens the app
                if (options.size > 4) {
                    val moreIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(EXTRA_SESSION_ID, sessionId)
                    }
                    val morePendingIntent = PendingIntent.getActivity(
                        context,
                        NOTIFICATION_ID_QUESTION_PREFIX + questionId.hashCode() + 100,
                        moreIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(R.drawable.ic_launcher_foreground, "More...", morePendingIntent)
                }
            } else {
                // Text-input question: show RemoteInput for free text
                val remoteInput = androidx.core.app.RemoteInput.Builder(EXTRA_QUESTION_REPLY)
                    .setLabel("Type your answer...")
                    .build()

                val replyIntent = Intent(context, QuestionActionReceiver::class.java).apply {
                    action = QuestionActionReceiver.ACTION_QUESTION_REPLY
                    putExtra(QuestionActionReceiver.EXTRA_QUESTION_ID, questionId)
                    putExtra(QuestionActionReceiver.EXTRA_SESSION_ID, sessionId)
                }
                val replyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    NOTIFICATION_ID_QUESTION_PREFIX + questionId.hashCode(),
                    replyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                val replyAction = NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_foreground,
                    "Reply",
                    replyPendingIntent
                )
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .build()
                builder.addAction(replyAction)
            }

            notificationManager.notify(
                NOTIFICATION_ID_QUESTION_PREFIX + questionId.hashCode(),
                builder.build()
            )
        }

        fun dismissQuestionNotification(context: Context, questionId: String) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.cancel(NOTIFICATION_ID_QUESTION_PREFIX + questionId.hashCode())
        }
    }

    /**
     * Data class for todo item information in notifications.
     */
    data class TodoInfo(
        val content: String,
        val status: String,  // "pending", "in_progress", "completed", "cancelled"
        val priority: String // "high", "medium", "low"
    )

    inner class LocalBinder : Binder() {
        fun getService(): ActiveSessionService = this@ActiveSessionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    /**
     * Add or update a session's state and refresh notification.
     * This is the primary method for updating session progress.
     */
    fun updateSessionState(
        sessionId: String,
        title: String? = null,
        currentTask: String? = null,
        todos: List<TodoInfo>? = null,
        elapsedSeconds: Long? = null,
        modelName: String? = null
    ) {
        val existingState = sessionStates[sessionId]
        val now = System.currentTimeMillis()
        val defaultElapsed = (now - (existingState?.startTime ?: now)) / MILLIS_PER_SECOND

        val newState = if (existingState != null) {
            existingState.copy(
                title = title ?: existingState.title,
                currentTask = currentTask ?: existingState.currentTask,
                todos = todos ?: existingState.todos,
                elapsedSeconds = elapsedSeconds ?: defaultElapsed,
                modelName = modelName ?: existingState.modelName,
                lastUpdated = now
            )
        } else {
            SessionState(
                sessionId = sessionId,
                title = title ?: "Task Runner",
                currentTask = currentTask,
                todos = todos ?: emptyList(),
                elapsedSeconds = elapsedSeconds ?: 0,
                modelName = modelName ?: "Agent",
                startTime = now,
                lastUpdated = now
            )
        }

        sessionStates[sessionId] = newState
        _activeSessionIds.value = sessionStates.keys

        // Update the foreground notification
        updateForegroundNotification()

        // Ensure service is in foreground if we have active sessions
        if (sessionStates.isNotEmpty() && !isForeground) {
            promoteToForeground()
        }

        Napier.d("[ActiveSessionService] Updated session: $sessionId, active: ${sessionStates.size}")

        // Push widget update
        pushWidgetUpdate()
    }

    /**
     * Remove a session and update notification.
     */
    fun removeSession(sessionId: String) {
        sessionStates.remove(sessionId)
        _activeSessionIds.value = sessionStates.keys

        if (sessionStates.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
            stopSelf()
        } else {
            updateForegroundNotification()
        }

        Napier.i("[ActiveSessionService] Removed session: $sessionId, remaining: ${sessionStates.size}")

        // Push widget update
        pushWidgetUpdate()
    }

    /**
     * Push widget update with current session count and connection status.
     */
    private fun pushWidgetUpdate() {
        serviceScope.launch {
            try {
                WidgetUpdateHelper.update(
                    context = this@ActiveSessionService,
                    connectionStatus = "Connected",
                    sessionCount = sessionStates.size,
                    lastSync = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date())
                )
            } catch (_: Exception) {
                Napier.w("[ActiveSessionService] Widget update failed")
            }
        }
    }

    private var isForeground = false

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
                    updateSessionState(
                        sessionId = sessionId,
                        title = sessionTitle
                    )
                }
            }
            ACTION_STOP -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                if (sessionId != null) {
                    removeSession(sessionId)
                }
            }
            ACTION_STOP_ALL -> {
                sessionStates.clear()
                _activeSessionIds.value = emptySet()
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForeground = false
                stopSelf()
                Napier.i("[ActiveSessionService] All sessions stopped")
                pushWidgetUpdate()
            }
            ACTION_ABORT -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                if (sessionId != null) {
                    Napier.i("[ActiveSessionService] Abort requested for session: $sessionId")
                }
            }
            "com.mocca.app.action.UPDATE_SESSION" -> {
                // Handle progress updates from NotificationTracker
                val sessionId = intent.getStringExtra("sessionId")
                if (sessionId != null) {
                    val title = intent.getStringExtra("title")
                    val currentTask = intent.getStringExtra("currentTask")
                    val elapsedSeconds = intent.getLongExtra("elapsedSeconds", 0)
                    val modelName = intent.getStringExtra("modelName") ?: "Agent"

                    // Reconstruct todos from intent extras
                    val todoCount = intent.getIntExtra("todoCount", 0)
                    val todos = (0 until todoCount).map { index ->
                        TodoInfo(
                            content = intent.getStringExtra("todo_${index}_content") ?: "",
                            status = intent.getStringExtra("todo_${index}_status") ?: "pending",
                            priority = intent.getStringExtra("todo_${index}_priority") ?: "medium"
                        )
                    }

                    updateSessionState(
                        sessionId = sessionId,
                        title = title,
                        currentTask = currentTask,
                        todos = todos,
                        elapsedSeconds = elapsedSeconds,
                        modelName = modelName
                    )
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

    private fun promoteToForeground() {
        val notification = buildSummaryNotification()

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

        isForeground = true
        Napier.i("[ActiveSessionService] Promoted to foreground")
    }

    private fun updateForegroundNotification() {
        if (sessionStates.isEmpty()) return

        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = buildSummaryNotification()
        notificationManager.notify(NOTIFICATION_ID_ACTIVE, notification)
    }

    /**
     * Build the summary notification showing all active sessions.
     * This is the single foreground notification that persists while any session is active.
     */
    private fun buildSummaryNotification(): Notification {
        val sessions = sessionStates.values.toList()
        val sessionCount = sessions.size

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build content based on number of sessions
        val contentTitle = when {
            sessionCount == 1 -> {
                sessions.first().title
            }
            sessionCount > 1 -> {
                "MOCCA • $sessionCount sessions"
            }
            else -> {
                "MOCCA"
            }
        }

        // Build summary text
        val contentText = when {
            sessionCount == 1 -> {
                val session = sessions.first()
                when {
                    session.currentTask != null -> {
                        session.currentTask
                    }
                    session.inProgressCount > 0 -> {
                        val todo = session.todos.find { it.status == "in_progress" }
                        todo?.content ?: "Processing..."
                    }
                    session.totalCount > 0 -> {
                        "Progress: ${session.completedCount}/${session.totalCount}"
                    }
                    else -> {
                        "${session.modelName} • ${formatTime(session.elapsedSeconds)}"
                    }
                }
            }
            sessionCount > 1 -> {
                val completedTotal = sessions.sumOf { it.completedCount }
                val totalTodos = sessions.sumOf { it.totalCount }
                if (totalTodos > 0) {
                    "Overall: $completedTotal/$totalTodos completed"
                } else {
                    "${sessions.count { it.inProgressCount > 0 }} active"
                }
            }
            else -> {
                "Processing..."
            }
        }

        // Build short critical text for status bar chip
        val shortCriticalText = when {
            sessionCount == 1 -> {
                val session = sessions.first()
                if (session.totalCount > 0) {
                    "${session.completedCount}/${session.totalCount}"
                } else {
                    formatTime(session.elapsedSeconds)
                }
            }
            else -> {
                "$sessionCount running"
            }
        }

        // Calculate overall progress
        val totalCompleted = sessions.sumOf { it.completedCount }
        val totalTodos = sessions.sumOf { it.totalCount }
        val maxElapsed = sessions.maxOfOrNull { it.elapsedSeconds } ?: 0

        // Build big text for expanded view
        val bigText = buildBigText(sessions)

        // Use Android 16 ProgressStyle if available
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && sessionCount == 1) {
            buildAndroid16Notification(
                contentTitle = contentTitle,
                contentText = contentText,
                shortCriticalText = shortCriticalText,
                pendingIntent = pendingIntent,
                session = sessions.first(),
                bigText = bigText
            )
        } else {
            buildLegacyNotification(
                contentTitle = contentTitle,
                contentText = contentText,
                pendingIntent = pendingIntent,
                totalCompleted = totalCompleted,
                totalTodos = totalTodos,
                elapsedSeconds = maxElapsed,
                bigText = bigText
            )
        }
    }

    private fun buildBigText(sessions: List<SessionState>): String {
        val sb = StringBuilder()

        sessions.forEachIndexed { index, session ->
            if (index > 0) sb.append("\n")
            sb.append("▸ ${session.title}")

            if (session.todos.isNotEmpty()) {
                session.todos.take(3).forEach { todo ->
                    val icon = when (todo.status) {
                        "completed" -> "✓"
                        "in_progress" -> "►"
                        "cancelled" -> "✗"
                        else -> "○"
                    }
                    sb.append("\n  $icon ${todo.content.take(40)}")
                }
                if (session.todos.size > 3) {
                    sb.append("\n  ... +${session.todos.size - 3} more")
                }
            } else if (session.currentTask != null) {
                sb.append("\n  → ${session.currentTask.take(50)}")
            }
        }

        return sb.toString()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildAndroid16Notification(
        contentTitle: String,
        contentText: String,
        shortCriticalText: String,
        pendingIntent: PendingIntent,
        session: SessionState,
        bigText: String
    ): Notification {
        val segments = buildProgressSegments(session.todos)

        val progressStyle = Notification.ProgressStyle()
            .setProgress(session.completedCount * 100)
            .setProgressSegments(segments)

        if (session.todos.isNotEmpty()) {
            val points = mutableListOf<Notification.ProgressStyle.Point>()
            val progressPercent = session.progressPercent
            if (progressPercent > 0 && progressPercent < 100) {
                points.add(
                    Notification.ProgressStyle.Point(progressPercent)
                        .setColor(accentGreen)
                )
            }
            if (points.isNotEmpty()) {
                progressStyle.setProgressPoints(points)
            }
        }

        val builder = Notification.Builder(this, CHANNEL_AGENT_ACTIVE)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setWhen(System.currentTimeMillis() - session.elapsedSeconds * MILLIS_PER_SECOND)
            .setUsesChronometer(true)
            .setShortCriticalText(shortCriticalText)
            .setStyle(progressStyle)

        // Add abort action for active sessions
        val abortIntent = Intent(this, ActiveSessionService::class.java).apply {
            action = ACTION_ABORT
            putExtra(EXTRA_SESSION_ID, session.sessionId)
        }
        val abortPendingIntent = PendingIntent.getService(
            this,
            session.sessionId.hashCode(),
            abortIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            Notification.Action.Builder(
                null,
                "Abort",
                abortPendingIntent
            ).build()
        )

        // Request promoted ongoing (Live Update) if the app is allowed and
        // the notification meets all promotion requirements.
        if (canRequestPromotedOngoing()) {
            builder.setFlag(Notification.FLAG_PROMOTED_ONGOING, true)
        }

        val notification = builder.build()

        // Log promotion characteristics for debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val hasPromotable = runCatching {
                notification.hasPromotableCharacteristics()
            }.getOrDefault(false)
            if (!hasPromotable) {
                Napier.d(
                    "[ActiveSessionService] Notification lacks promotable characteristics " +
                        "for session ${session.sessionId}"
                )
            }
        }

        return notification
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun canRequestPromotedOngoing(): Boolean {
        return runCatching {
            getSystemService(NotificationManager::class.java).canPostPromotedNotifications()
        }.getOrDefault(false)
    }

    private fun buildLegacyNotification(
        contentTitle: String,
        contentText: String,
        pendingIntent: PendingIntent,
        totalCompleted: Int,
        totalTodos: Int,
        elapsedSeconds: Long,
        bigText: String
    ): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_AGENT_ACTIVE)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setProgress(totalTodos.coerceAtLeast(0), totalCompleted, totalTodos == 0)
            .setWhen(System.currentTimeMillis() - elapsedSeconds * MILLIS_PER_SECOND)
            .setUsesChronometer(true)

        val notification = builder.build()

        // Apply promoted ongoing flag for devices that support it
        // (Android 16+ / API 35+) even when using the legacy builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && canRequestPromotedOngoing()) {
            notification.flags = notification.flags or Notification.FLAG_PROMOTED_ONGOING
        }

        return notification
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildProgressSegments(todos: List<TodoInfo>): List<Notification.ProgressStyle.Segment> {
        if (todos.isEmpty()) {
            return listOf(
                Notification.ProgressStyle.Segment(100).setColor(darkGreyColor)
            )
        }

        return todos.map { todo ->
            val color = when (todo.status) {
                "completed" -> accentGreen
                "in_progress" -> amberColor
                "cancelled" -> greyColor
                else -> darkGreyColor
            }
            Notification.ProgressStyle.Segment(1).setColor(color)
        }
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / SECONDS_PER_MINUTE
        val secs = seconds % SECONDS_PER_MINUTE
        return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
    }

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
}
