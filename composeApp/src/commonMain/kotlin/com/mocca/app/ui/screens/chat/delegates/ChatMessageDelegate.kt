package com.mocca.app.ui.screens.chat.delegates

import com.mocca.app.domain.model.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.flow.StateFlow

interface ChatMessageDelegate {
    val messages: StateFlow<ImmutableList<Message>>
    val aggregatedMessages: StateFlow<ImmutableList<Message>>
    val childSessions: StateFlow<ImmutableMap<String, Session>>
    val childMessages: StateFlow<ImmutableMap<String, ImmutableList<Message>>>
    
    fun loadMessages(sessionId: String, limit: Long)
    fun loadMoreMessages(sessionId: String)
    fun loadChildren(sessionId: String)
    fun loadChildMessages(childId: String)
    fun createLocalUserMessage(sessionId: String, text: String): Message
}
