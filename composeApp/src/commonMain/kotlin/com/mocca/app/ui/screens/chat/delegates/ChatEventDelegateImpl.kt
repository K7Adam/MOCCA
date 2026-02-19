package com.mocca.app.ui.screens.chat.delegates

import com.mocca.app.data.repository.BroadcastEvent
import com.mocca.app.data.repository.StateCoordinator
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Implementation of ChatEventDelegate using StateCoordinator.
 * 
 * IMPORTANT: This now uses StateCoordinator instead of directly observing
 * EventStreamRepository. This ensures all events flow through the central
 * coordinator for proper state synchronization.
 */
class ChatEventDelegateImpl(
    private val stateCoordinator: StateCoordinator,
    private val scope: CoroutineScope
) : ChatEventDelegate {
    
    // Streaming state from StateCoordinator (which delegates to EventStreamRepository)
    override val streamingText = stateCoordinator.streamingText
    
    private val _childStreamingText = MutableStateFlow<ImmutableMap<String, String>>(persistentMapOf())
    override val childStreamingText = _childStreamingText.asStateFlow()
    
    override val isThinking = stateCoordinator.isThinking
    override val thinkingContent = stateCoordinator.thinkingContent
    
    private val _thinkingElapsedMs = MutableStateFlow(0L)
    override val thinkingElapsedMs = _thinkingElapsedMs.asStateFlow()
    
    override val pendingPermission = stateCoordinator.pendingPermission
    override val pendingQuestion = stateCoordinator.pendingQuestion
    override val connectionStatus = stateCoordinator.connectionStatus

    override fun connectToEventStream(sessionId: String) {
        // Connect via StateCoordinator instead of directly
        scope.launch {
            stateCoordinator.setActiveSession(sessionId)
        }
    }

    override fun observeEvents(sessionId: String, onMessageComplete: (String) -> Unit, onChildMessageUpdate: (String) -> Unit) {
        // Observe thinking time
        scope.launch {
            stateCoordinator.thinkingStartTime.collect { startTime ->
                if (startTime != null) {
                    while (stateCoordinator.isThinking.value) {
                        _thinkingElapsedMs.value = System.currentTimeMillis() - startTime
                        delay(100)
                    }
                } else {
                    _thinkingElapsedMs.value = 0
                }
            }
        }

        // Observe events via StateCoordinator (NOT directly from EventStreamRepository)
        scope.launch {
            stateCoordinator.eventsForMonitoredSessions().collect { event ->
                when (event) {
                    is ServerEvent.MessageUpdated -> {
                        if (event.properties.info.sessionID == sessionId) {
                            onMessageComplete(sessionId)
                        } else {
                            onChildMessageUpdate(event.properties.info.sessionID)
                        }
                    }
                    is ServerEvent.MessagePartUpdated -> {
                        val part = event.properties.part
                        val delta = event.properties.delta
                        if (part.sessionID != sessionId && part.type == "text") {
                            _childStreamingText.update { currentText ->
                                val existing = currentText[part.sessionID] ?: ""
                                val updated = if (existing.isEmpty() && !part.text.isNullOrEmpty()) part.text 
                                              else if (delta != null) existing + delta 
                                              else part.text ?: existing
                                currentText.toMutableMap().apply { put(part.sessionID, updated) }.toImmutableMap()
                            }
                        }
                    }
                    is ServerEvent.SessionIdle -> {
                        if (event.properties.sessionID == sessionId) {
                            onMessageComplete(sessionId)
                        } else {
                            _childStreamingText.update { it.toMutableMap().apply { remove(event.properties.sessionID) }.toImmutableMap() }
                            onChildMessageUpdate(event.properties.sessionID)
                        }
                    }
                    else -> {}
                }
            }
        }
        
        // Also observe session idle callback from StateCoordinator
        scope.launch {
            // This ensures we don't miss idle events
            stateCoordinator.broadcastEvents.collect { broadcastEvent ->
                when (broadcastEvent) {
                    is BroadcastEvent.ServerEvent -> {
                        val serverEvent = broadcastEvent.event
                        if (serverEvent is ServerEvent.SessionIdle && serverEvent.properties.sessionID == sessionId) {
                            Napier.v("[ChatEventDelegate] Session idle via broadcast: $sessionId")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
