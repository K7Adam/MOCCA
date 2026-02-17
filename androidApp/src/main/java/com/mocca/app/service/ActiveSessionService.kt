package com.mocca.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mocca.app.MainActivity
import com.mocca.app.android.R
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
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
 */
class ActiveSessionService : Service() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _activeSessions = MutableStateFlow<Set<String>>(emptySet())
    val activeSessions: StateFlow<Set<String>> = _activeSessions.asStateFlow()
    
    private val binder = LocalBinder()
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "active_session_channel"
        private const val NOTIFICATION_ID = 1001
        
        private const val ACTION_START = "com.mocca.app.action.START_SESSION"
        private const val ACTION_STOP = "com.mocca.app.action.STOP_SESSION"
        private const val ACTION_STOP_ALL = "com.mocca.app.action.STOP_ALL_SESSIONS"
        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_SESSION_TITLE = "session_title"
        
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
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): ActiveSessionService = this@ActiveSessionService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Napier.i("[ActiveSessionService] Service destroyed")
    }
    
    private fun addActiveSession(sessionId: String, sessionTitle: String?) {
        val currentSessions = _activeSessions.value
        if (sessionId in currentSessions) return
        
        val newSessions = currentSessions + sessionId
        _activeSessions.value = newSessions
        
        Napier.i("[ActiveSessionService] Added session: $sessionId, total: ${newSessions.size}")
        
        // Start foreground if this is the first session
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
            // Stop foreground and service
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
    
    private fun startForeground(sessionTitle: String?) {
        val notification = createNotification(sessionTitle)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        Napier.i("[ActiveSessionService] Started foreground")
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Active Sessions",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when MOCCA is processing AI responses in the background"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(sessionTitle: String?): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
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
            .build()
    }
    
    private fun updateNotification() {
        val sessionCount = _activeSessions.value.size
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
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
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
