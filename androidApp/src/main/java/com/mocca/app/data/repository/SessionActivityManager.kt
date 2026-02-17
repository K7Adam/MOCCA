package com.mocca.app.data.repository

import android.content.Context
import com.mocca.app.service.ActiveSessionService
import com.mocca.app.domain.model.ServerEvent
import com.mocca.app.domain.model.SessionStatus
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the foreground service for active sessions.
 * 
 * Observes session events and automatically starts/stops the foreground service
 * when sessions enter/exit the RUNNING state. This ensures connections are
 * maintained even when the app is backgrounded during long LLM operations.
 */
class SessionActivityManager(
    private val context: Context,
    private val eventStreamRepository: EventStreamRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _runningSessions = MutableStateFlow<Set<String>>(emptySet())
    val runningSessions: StateFlow<Set<String>> = _runningSessions.asStateFlow()
    
    private var sessionTitles = mutableMapOf<String, String>()
    
    init {
        observeSessionEvents()
    }
    
    private fun observeSessionEvents() {
        scope.launch {
            eventStreamRepository.events.collect { event ->
                when (event) {
                    is ServerEvent.SessionUpdated -> {
                        val session = event.properties.info
                        val isRunning = session.status == SessionStatus.RUNNING
                        val title = session.title ?: "Session"
                        
                        if (isRunning) {
                            sessionTitles[session.id] = title
                            startSession(session.id, title)
                        } else {
                            stopSession(session.id)
                        }
                    }
                    
                    is ServerEvent.SessionIdle -> {
                        stopSession(event.properties.sessionID)
                    }
                    
                    is ServerEvent.SessionError -> {
                        event.properties.sessionID?.let { stopSession(it) }
                    }
                    
                    is ServerEvent.SessionDeleted -> {
                        val sessionId = event.properties.info.id
                        stopSession(sessionId)
                        sessionTitles.remove(sessionId)
                    }
                    
                    else -> { /* Ignore other events */ }
                }
            }
        }
    }
    
    /**
     * Start tracking a running session and start foreground service if needed.
     */
    private fun startSession(sessionId: String, title: String) {
        if (sessionId in _runningSessions.value) return
        
        _runningSessions.value = _runningSessions.value + sessionId
        Napier.i("[SessionActivityManager] Session started: $sessionId")
        
        ActiveSessionService.start(context, sessionId, title)
    }
    
    /**
     * Stop tracking a session and update foreground service.
     */
    private fun stopSession(sessionId: String) {
        if (sessionId !in _runningSessions.value) return
        
        _runningSessions.value = _runningSessions.value - sessionId
        Napier.i("[SessionActivityManager] Session stopped: $sessionId")
        
        ActiveSessionService.stop(context, sessionId)
    }
    
    /**
     * Stop all sessions and the foreground service.
     */
    fun stopAll() {
        _runningSessions.value = emptySet()
        ActiveSessionService.stopAll(context)
        Napier.i("[SessionActivityManager] All sessions stopped")
    }
}
