package com.mocca.app.data

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton manager for tracking global background activity across the app.
 * Used to show a subtle activity indicator when any background operation is running.
 * 
 * Examples of tracked activities:
 * - API calls
 * - SSE connection state
 * - Git operations
 * - File operations
 * - Session creation/loading
 */
class GlobalActivityManager {
    
    // Map of activity tags to their active status
    private val activeActivities = ConcurrentHashMap<String, Boolean>()
    
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private val _activityCount = MutableStateFlow(0)
    val activityCount: StateFlow<Int> = _activityCount.asStateFlow()
    
    private val _activeActivityTags = MutableStateFlow<Set<String>>(emptySet())
    val activeActivityTags: StateFlow<Set<String>> = _activeActivityTags.asStateFlow()
    
    /**
     * Start tracking an activity with a unique tag.
     * Multiple activities can be tracked simultaneously.
     */
    fun startActivity(tag: String) {
        val wasEmpty = activeActivities.isEmpty()
        activeActivities[tag] = true
        updateState()
        
        if (wasEmpty) {
            Napier.d("Global activity started: $tag")
        }
    }
    
    /**
     * End tracking an activity by its tag.
     */
    fun endActivity(tag: String) {
        activeActivities.remove(tag)
        updateState()
        
        if (activeActivities.isEmpty()) {
            Napier.d("All global activities ended (last: $tag)")
        }
    }
    
    /**
     * Check if a specific activity is currently active.
     */
    fun isActivityActive(tag: String): Boolean = activeActivities[tag] == true
    
    /**
     * End all tracked activities. Useful for cleanup.
     */
    fun endAllActivities() {
        activeActivities.clear()
        updateState()
        Napier.d("All global activities cleared")
    }
    
    private fun updateState() {
        _activityCount.value = activeActivities.size
        _isActive.value = activeActivities.isNotEmpty()
        // Create immutable copy to ensure thread safety
        _activeActivityTags.value = activeActivities.keys.toSet()
    }
    
    companion object {
        // Common activity tags for consistency
        const val TAG_SSE_CONNECT = "sse.connect"
        const val TAG_SSE_STREAM = "sse.stream"
        const val TAG_API_CALL = "api.call"
        const val TAG_SESSION_CREATE = "session.create"
        const val TAG_SESSION_LOAD = "session.load"
        const val TAG_GIT_OPERATION = "git.operation"
        const val TAG_FILE_OPERATION = "file.operation"
        const val TAG_MESSAGE_SEND = "message.send"
        const val TAG_THINKING = "ai.thinking"
    }
}
