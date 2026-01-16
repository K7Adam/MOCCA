package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject

/**
 * Service to execute Git commands via the OpenCode Chat/Bash tool.
 * This is a fallback because the direct Git API endpoints are missing on the current server.
 */
class GitService(
    private val apiClient: MoccaApiClient,
    private val eventStreamRepository: EventStreamRepository,
    private val sessionRepository: SessionRepository,
    private val providerRepository: ProviderRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    private var sessionId: String? = null
    
    /**
     * Execute a git command and return the raw output string.
     */
    suspend fun execute(command: String): Result<String> = mutex.withLock {
        try {
            ensureSession()
            val sid = sessionId ?: throw IllegalStateException("Failed to create git session")
            
            // Resolve model
            val (providerId, modelId) = resolveModel()
            
            apiClient.chatAsync(
                sessionId = sid,
                modelId = modelId,
                providerId = providerId,
                parts = listOf(ChatPart.Text("Execute this command using the bash tool. Return ONLY the raw output, no markdown code blocks, no explanation. Command: $command")),
                mode = "auto" 
            ).getOrThrow()
            
            // Poll for result
            val result = withTimeout(30_000) { // 30s timeout
                var attempts = 0
                while (true) {
                    val messagesResult = apiClient.getMessages(sid)
                    val messages = messagesResult.getOrNull()
                    
                    if (messages != null && messages.isNotEmpty()) {
                        val last = messages.last()
                        if (last.info.role == MessageRole.ASSISTANT) {
                            // Check for tool part
                            val toolPart = last.parts.find { it.type == "tool" }
                            if (toolPart != null) {
                                val state = toolPart.state
                                if (state?.status == "completed") {
                                    return@withTimeout state.output ?: ""
                                } else if (state?.status == "error") {
                                    throw Exception("Tool execution failed: ${state.error}")
                                }
                            }
                            
                            // Fallback: Check text part if tool output was pasted into text
                            val textPart = last.parts.find { it.type == "text" }
                            if (textPart != null && toolPart == null) {
                                if (textPart.text?.contains("On branch") == true || textPart.text?.contains("commit ") == true) {
                                    return@withTimeout textPart.text ?: ""
                                }
                            }
                        }
                    }
                    
                    kotlinx.coroutines.delay(500)
                    attempts++
                    if (attempts % 10 == 0) {
                        Napier.d("Polling for git result... ($attempts)")
                    }
                }
                "" // Unreachable
            }
            
            // Clean up the output (remove markdown code blocks if any)
            val cleanResult = result.replace("```bash", "").replace("```", "").trim()
            
            Result.success(cleanResult)
            
        } catch (e: Exception) {
            Napier.e("Git execution failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun resolveModel(): Pair<String, String> {
        // Try to get default model from provider repo
        val resource = providerRepository.getProviders().first()
        val response = resource.dataOrNull()
        
        // 1. Check default config
        val defaultChat = response?.default?.get("chat")
        if (defaultChat != null && response != null) {
            val providers = response.all
            for (provider in providers) {
                val models = provider.models as? JsonObject
                if (models != null && models.containsKey(defaultChat)) {
                    return provider.id to defaultChat
                }
            }
        }
        
        // 2. Pick first connected provider
        response?.all?.firstOrNull { it.connected }?.let { provider ->
            val models = provider.models as? JsonObject
            val firstModel = models?.keys?.firstOrNull()
            if (firstModel != null) {
                return provider.id to firstModel
            }
        }
        
        // 3. Fallback hardcoded (dangerous but better than crash)
        return "openai" to "gpt-4o"
    }
    
    private suspend fun ensureSession() {
        if (sessionId == null) {
            // Find an existing "git-automation" session or create one
            val sessionsResource = sessionRepository.getSessions().first()
            val sessions = sessionsResource.dataOrNull() ?: emptyList()
            val existing = sessions.find { it.title == "GIT_AUTOMATION_DO_NOT_DELETE" }
            
            if (existing != null) {
                sessionId = existing.id
            } else {
                val newSession = apiClient.createSession().getOrThrow()
                apiClient.updateSession(newSession.id, "GIT_AUTOMATION_DO_NOT_DELETE")
                sessionId = newSession.id
            }
        }
    }
}
