package com.mocca.app.ui.screens.chat.delegates

import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ChatMessageDelegateImpl(
    private val sessionRepository: SessionRepository,
    private val scope: CoroutineScope
) : ChatMessageDelegate {
    
    private val _messages = MutableStateFlow<ImmutableList<Message>>(persistentListOf())
    override val messages = _messages.asStateFlow()
    
    private val _childSessions = MutableStateFlow<ImmutableMap<String, Session>>(persistentMapOf())
    override val childSessions = _childSessions.asStateFlow()
    
    private val _childMessages = MutableStateFlow<ImmutableMap<String, ImmutableList<Message>>>(persistentMapOf())
    override val childMessages = _childMessages.asStateFlow()
    
    private var currentLimit = 50L
    private var messagesJob: Job? = null

    override val aggregatedMessages: StateFlow<ImmutableList<Message>> = combine(_messages, _childSessions, _childMessages) { msgs, children, childMsgs ->
        val rootMessages = msgs.toMutableList()
        val syntheticMessages = children.values.map { child ->
            Message(
                id = "child-${child.id}",
                sessionId = child.parentID ?: "",
                role = MessageRole.ASSISTANT,
                parts = listOf(MessagePart.SubTask(
                    sessionId = child.id,
                    title = child.title ?: "Sub-task",
                    status = child.status,
                    messages = childMsgs[child.id] ?: emptyList(),
                    streamingText = ""
                )),
                createdAt = child.createdAt
            )
        }
        (rootMessages + syntheticMessages).sortedBy { it.createdAt }.toImmutableList()
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    override fun loadMessages(sessionId: String, limit: Long) {
        currentLimit = limit
        messagesJob?.cancel()
        messagesJob = scope.launch {
            sessionRepository.getMessages(sessionId, currentLimit).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _messages.update { resource.data?.toImmutableList() ?: it }
                    }
                    is Resource.Success -> {
                        _messages.value = resource.data.toImmutableList()
                    }
                    is Resource.Error -> {
                        _messages.update { resource.data?.toImmutableList() ?: it }
                    }
                }
            }
        }
    }

    override fun loadMoreMessages(sessionId: String) {
        currentLimit += 50
        loadMessages(sessionId, currentLimit)
    }

    override fun loadChildren(sessionId: String) {
        if (sessionId.isEmpty()) return
        scope.launch {
            sessionRepository.getChildren(sessionId).onSuccess { children ->
                _childSessions.value = children.associateBy { it.id }.toImmutableMap()
                children.forEach { child ->
                    loadChildMessages(child.id)
                }
            }
        }
    }

    override fun loadChildMessages(childId: String) {
        scope.launch {
            sessionRepository.getMessages(childId).collect { resource ->
                if (resource is Resource.Success) {
                    _childMessages.update { it.toMutableMap().apply { put(childId, resource.data.toImmutableList()) }.toImmutableMap() }
                }
            }
        }
    }

    override fun createLocalUserMessage(sessionId: String, text: String): Message {
        return sessionRepository.createLocalUserMessage(sessionId, text).also { msg ->
            _messages.update { (it + msg).toImmutableList() }
        }
    }
}
