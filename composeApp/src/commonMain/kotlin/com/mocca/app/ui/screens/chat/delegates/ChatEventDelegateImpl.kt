package com.mocca.app.ui.screens.chat.delegates

import com.mocca.app.data.repository.EventStreamRepository
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ChatEventDelegateImpl(
    private val eventStreamRepository: EventStreamRepository,
    private val scope: CoroutineScope
) : ChatEventDelegate {
    
    override val streamingText = eventStreamRepository.streamingText
    
    private val _childStreamingText = MutableStateFlow<ImmutableMap<String, String>>(persistentMapOf())
    override val childStreamingText = _childStreamingText.asStateFlow()
    
    override val isThinking = eventStreamRepository.isThinking
    override val thinkingContent = eventStreamRepository.thinkingContent
    
    private val _thinkingElapsedMs = MutableStateFlow(0L)
    override val thinkingElapsedMs = _thinkingElapsedMs.asStateFlow()
    
    override val pendingPermission = eventStreamRepository.pendingPermission
    override val pendingQuestion = eventStreamRepository.pendingQuestion
    override val connectionStatus = eventStreamRepository.connectionStatus

    override fun connectToEventStream(sessionId: String) {
        eventStreamRepository.connect(scope, sessionId)
    }

    override fun observeEvents(sessionId: String, onMessageComplete: (String) -> Unit, onChildMessageUpdate: (String) -> Unit) {
        scope.launch {
            eventStreamRepository.thinkingStartTime.collect { startTime ->
                if (startTime != null) {
                    while (eventStreamRepository.isThinking.value) {
                        _thinkingElapsedMs.value = System.currentTimeMillis() - startTime
                        delay(100)
                    }
                } else {
                    _thinkingElapsedMs.value = 0
                }
            }
        }

        scope.launch {
            eventStreamRepository.eventsForMonitoredSessions().collect { event ->
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
    }
}
