package com.mocca.app.ui.screens.sessions

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.AppConnectionManager
import com.mocca.app.data.repository.AppConnectionState
import com.mocca.app.data.repository.SessionRepository
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.Session
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionsState(
    val sessions: List<Session> = emptyList(),
    val childrenMap: Map<String, List<Session>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedSessionId: String? = null,
    val connectionState: AppConnectionState = AppConnectionState.NotConfigured
)

class SessionsScreenModel(
    private val sessionRepository: SessionRepository,
    private val appConnectionManager: AppConnectionManager
) : ScreenModel {
    
    private val _state = MutableStateFlow(SessionsState())
    val state: StateFlow<SessionsState> = _state.asStateFlow()
    
    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()
    
    init {
        observeConnectionState()
        appConnectionManager.checkConnection()
    }
    
    private fun observeConnectionState() {
        screenModelScope.launch {
            appConnectionManager.connectionState.collect { connectionState ->
                val previousState = _state.value.connectionState
                _state.value = _state.value.copy(connectionState = connectionState)
                
                if (connectionState is AppConnectionState.Connected && 
                    previousState !is AppConnectionState.Connected) {
                    loadSessions()
                }
            }
        }
    }
    
    fun retryConnection() {
        appConnectionManager.checkConnection()
    }
    
    fun loadSessions() {
        screenModelScope.launch {
            sessionRepository.getSessions().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(
                            isLoading = true,
                            sessions = resource.data ?: _state.value.sessions
                        )
                    }
                    is Resource.Success -> {
                        val allSessions = resource.data
                        
                        // Robust filtering: root sessions have no parentID AND don't start with "Background:" or "look_at:"
                        // We also consider anything without parentID as a root.
                        val roots = allSessions.filter { 
                            val hasParent = !it.effectiveParentID.isNullOrBlank()
                            val isInternal = it.title.orEmpty().startsWith("Background:") || 
                                           it.title.orEmpty().startsWith("look_at:") ||
                                           it.title.orEmpty().contains("subagent", ignoreCase = true)
                            
                            !hasParent && !isInternal
                        }.sortedByDescending { it.updatedAt }
                        
                        val children = allSessions.filter { 
                            val hasParent = !it.effectiveParentID.isNullOrBlank()
                            val isInternal = it.title.orEmpty().startsWith("Background:") || 
                                           it.title.orEmpty().startsWith("look_at:") ||
                                           it.title.orEmpty().contains("subagent", ignoreCase = true)
                            
                            hasParent || isInternal
                        }.groupBy { it.effectiveParentID ?: "internal" }
                        
                        _state.value = _state.value.copy(
                            isLoading = false,
                            sessions = roots,
                            childrenMap = children,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = resource.message,
                            sessions = resource.data ?: _state.value.sessions
                        )
                    }
                }
            }
        }
    }
    
    fun createSession() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            sessionRepository.createSession().fold(
                onSuccess = { session ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        sessions = listOf(session) + _state.value.sessions,
                        selectedSessionId = session.id
                    )
                    _navigationEvent.emit(session.id)
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun deleteSession(sessionId: String) {
        screenModelScope.launch {
            sessionRepository.deleteSession(sessionId).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        sessions = _state.value.sessions.filter { it.id != sessionId },
                        selectedSessionId = if (_state.value.selectedSessionId == sessionId) null else _state.value.selectedSessionId
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun selectSession(sessionId: String) {
        _state.value = _state.value.copy(selectedSessionId = sessionId)
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun refresh() {
        if (_state.value.connectionState.isConnected) {
            loadSessions()
        } else {
            appConnectionManager.checkConnection()
        }
    }
}
