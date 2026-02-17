package com.mocca.app.ui.screens.chat.delegates

import com.mocca.app.domain.model.*
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.flow.StateFlow

interface ChatEventDelegate {
    val streamingText: StateFlow<String>
    val childStreamingText: StateFlow<ImmutableMap<String, String>>
    val isThinking: StateFlow<Boolean>
    val thinkingContent: StateFlow<String>
    val thinkingElapsedMs: StateFlow<Long>
    val pendingPermission: StateFlow<PermissionRequest?>
    val pendingQuestion: StateFlow<QuestionRequest?>
    val connectionStatus: StateFlow<ConnectionStatus>
    
    fun connectToEventStream(sessionId: String)
    fun observeEvents(sessionId: String, onMessageComplete: (String) -> Unit, onChildMessageUpdate: (String) -> Unit)
}
