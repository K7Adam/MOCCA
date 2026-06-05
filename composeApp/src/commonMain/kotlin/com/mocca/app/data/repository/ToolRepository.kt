package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.opencode.BridgeFeatureUnavailableException
import com.mocca.app.bridge.opencode.OpenCodeBridgeRepository
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tool category for grouping and UI display.
 * 
 * Reference: OPENCODE_API_ANALYSIS.md - Tool Mapping recommendation
 * "Automate the creation of Kotlin data classes from the /agent definitions 
 * to allow dynamic capability discovery in the Android UI."
 */
enum class ToolCategory(val displayName: String, val icon: String) {
    FILE_SYSTEM("File System", "📁"),
    GIT("Git", "🔀"),
    CODE_ANALYSIS("Code Analysis", "🔍"),
    BUILD_RUN("Build & Run", "▶️"),
    SEARCH("Search", "🔎"),
    TASK_MANAGEMENT("Task Management", "📋"),
    COMMUNICATION("Communication", "💬"),
    OTHER("Other", "🔧")
}

/**
 * Parsed tool information with category and capabilities.
 */
data class ToolInfo(
    val id: String,
    val category: ToolCategory,
    val isWriteOperation: Boolean,
    val requiresConfirmation: Boolean
) {
    companion object {
        /**
         * Create ToolInfo from tool ID string.
         * Infers category and capabilities from naming conventions.
         */
        fun fromId(id: String): ToolInfo {
            val category = categorizeFromId(id)
            val isWrite = isWriteOperationFromId(id)
            val requiresConfirm = requiresConfirmationFromId(id, category)
            
            return ToolInfo(
                id = id,
                category = category,
                isWriteOperation = isWrite,
                requiresConfirmation = requiresConfirm
            )
        }
        
        private fun categorizeFromId(id: String): ToolCategory {
            val lowerId = id.lowercase()
            return when {
                // Git operations
                lowerId.startsWith("git_") || lowerId.contains("git") -> ToolCategory.GIT
                
                // File system operations
                lowerId in listOf("read", "write", "edit", "glob", "filesystem") ||
                lowerId.startsWith("filesystem_") ||
                lowerId.contains("file") -> ToolCategory.FILE_SYSTEM
                
                // Search operations
                lowerId in listOf("grep", "find", "search") ||
                lowerId.startsWith("grep_") ||
                lowerId.startsWith("search_") ||
                lowerId.contains("search") -> ToolCategory.SEARCH
                
                // Build/Run operations
                lowerId in listOf("bash", "shell", "terminal", "run", "build") ||
                lowerId.startsWith("interactive_") -> ToolCategory.BUILD_RUN
                
                // Task management
                lowerId.startsWith("todo") ||
                lowerId.startsWith("task") ||
                lowerId.contains("session") ||
                lowerId.contains("background") -> ToolCategory.TASK_MANAGEMENT
                
                // Code analysis (LSP, AST)
                lowerId.startsWith("lsp_") ||
                lowerId.startsWith("ast_") ||
                lowerId.contains("symbol") ||
                lowerId.contains("diagnostic") -> ToolCategory.CODE_ANALYSIS
                
                // Communication (questions, prompts)
                lowerId == "question" ||
                lowerId.contains("prompt") ||
                lowerId.contains("message") -> ToolCategory.COMMUNICATION
                
                else -> ToolCategory.OTHER
            }
        }
        
        private fun isWriteOperationFromId(id: String): Boolean {
            val lowerId = id.lowercase()
            val writeKeywords = listOf(
                "write", "edit", "create", "delete", "remove", 
                "commit", "push", "stage", "unstage", "discard",
                "add", "update", "patch", "post"
            )
            return writeKeywords.any { lowerId.contains(it) }
        }
        
        private fun requiresConfirmationFromId(id: String, category: ToolCategory): Boolean {
            // Destructive operations always require confirmation
            val lowerId = id.lowercase()
            val destructiveKeywords = listOf(
                "delete", "remove", "drop", "reset", "force",
                "discard", "abort", "rebase"
            )
            return destructiveKeywords.any { lowerId.contains(it) } ||
                (category == ToolCategory.GIT && isWriteOperationFromId(id))
        }
    }
}

/**
 * Tool capability summary for UI display.
 */
data class ToolCapabilities(
    val totalTools: Int,
    val categoryCounts: Map<ToolCategory, Int>,
    val writeOperations: Int,
    val readOnlyOperations: Int,
    val lastUpdated: Long
)

/**
 * Repository for tool information with schema discovery.
 * 
 * Provides:
 * 1. Tool ID listing from /experimental/tool/ids
 * 2. Tool categorization for UI grouping
 * 3. Write vs read-only operation detection
 * 4. Capability summary for dashboard display
 * 
 * Reference: OPENCODE_API_ANALYSIS.md
 */
class ToolRepository(
    private val apiClient: MoccaApiClient,
    private val bridgeConnectionManager: BridgeConnectionManager
) {
    private val TAG = "ToolRepository"
    
    // Cache for parsed tool info
    private val toolCacheMutex = Mutex()
    private var cachedTools: List<ToolInfo>? = null
    private var cacheTimestamp: Long = 0L
    private val CACHE_TTL_MS = 5 * 60 * 1000L  // 5 minutes
    
    // Observable tool capabilities state
    private val _capabilities = MutableStateFlow<ToolCapabilities?>(null)
    val capabilities: StateFlow<ToolCapabilities?> = _capabilities.asStateFlow()
    
    /**
     * Get all available tool IDs from /experimental/tool/ids endpoint.
     */
    fun getToolIds(): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading())
        apiClient.getToolIds().fold(
            onSuccess = { tools ->
                // Update cache
                updateToolCache(tools)
                emit(Resource.Success(tools))
            },
            onFailure = { error ->
                Napier.e("Failed to fetch tools", error)
                emit(Resource.Error(error.message ?: "Failed to fetch tools"))
            }
        )
    }
    
    /**
     * Get parsed tool information with categories.
     * Uses cache if available and not expired.
     */
    fun getToolInfo(): Flow<Resource<List<ToolInfo>>> = flow {
        emit(Resource.Loading())
        
        // Check cache first
        toolCacheMutex.withLock {
            if (cachedTools != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
                emit(Resource.Success(cachedTools!!))
                return@flow
            }
        }
        
        // Fetch fresh data
        apiClient.getToolIds().fold(
            onSuccess = { toolIds ->
                val toolInfoList = updateToolCache(toolIds)
                emit(Resource.Success(toolInfoList))
            },
            onFailure = { error ->
                Napier.e("$TAG: Failed to fetch tool info", error)
                // Return cached data if available on error
                toolCacheMutex.withLock {
                    if (cachedTools != null) {
                        emit(Resource.Error(error.message ?: "Failed to fetch tools", cachedTools))
                    } else {
                        emit(Resource.Error(error.message ?: "Failed to fetch tools"))
                    }
                }
            }
        )
    }
    
    /**
     * Get tools grouped by category.
     */
    fun getToolsByCategory(): Flow<Resource<Map<ToolCategory, List<ToolInfo>>>> = flow {
        emit(Resource.Loading())

        // Try bridge-first
        val bridgeStatus = bridgeConnectionManager.status.value
        if (bridgeStatus is BridgeConnectionStatus.Connected) {
            try {
                val client = bridgeConnectionManager.client.value
                    ?: throw BridgeFeatureUnavailableException("MOCCA CLI connection")
                if ("tools" in bridgeStatus.capabilities.namespaces || bridgeStatus.capabilities.ai.configNormalized) {
                    Napier.d("[ToolRepository] Tools available via bridge config")
                    // Use bridge config snapshot for tool info
                    val config = OpenCodeBridgeRepository(client).fetchOpenCodeConfigSnapshot()
                    val toolIds = config.effective.tools.keys.toList()
                    val toolInfoList = updateToolCache(toolIds)
                    val grouped = toolInfoList.groupBy { it.category }
                        .toSortedMap(compareBy { it.ordinal })
                    emit(Resource.Success(grouped))
                    return@flow
                }
            } catch (e: Exception) {
                Napier.w("[ToolRepository] Bridge failed, HTTP fallback", e)
            }
        }

        apiClient.getToolIds().fold(
            onSuccess = { toolIds ->
                val toolInfoList = updateToolCache(toolIds)
                val grouped = toolInfoList.groupBy { it.category }
                    .toSortedMap(compareBy { it.ordinal })
                emit(Resource.Success(grouped))
            },
            onFailure = { error ->
                Napier.e("$TAG: Failed to fetch tools by category", error)
                emit(Resource.Error(error.message ?: "Failed to fetch tools"))
            }
        )
    }
    
    /**
     * Get capability summary for dashboard display.
     */
    suspend fun getCapabilitySummary(): ToolCapabilities? {
        // Return cached capabilities if recent
        _capabilities.value?.let { cached ->
            if (System.currentTimeMillis() - cached.lastUpdated < CACHE_TTL_MS) {
                return cached
            }
        }
        
        // Fetch fresh
        return apiClient.getToolIds().fold(
            onSuccess = { toolIds ->
                val toolInfoList = updateToolCache(toolIds)
                val capabilities = buildCapabilities(toolInfoList)
                _capabilities.value = capabilities
                capabilities
            },
            onFailure = { error ->
                Napier.e("$TAG: Failed to get capability summary", error)
                null
            }
        )
    }
    
    /**
     * Force refresh the tool cache.
     */
    suspend fun refreshCache(): Result<List<ToolInfo>> {
        return apiClient.getToolIds().map { toolIds ->
            updateToolCache(toolIds)
        }
    }
    
    /**
     * Invalidate cache to force refresh on next access.
     */
    suspend fun invalidateCache() {
        toolCacheMutex.withLock {
            cachedTools = null
            cacheTimestamp = 0L
            _capabilities.value = null
        }
    }
    
    private suspend fun updateToolCache(toolIds: List<String>): List<ToolInfo> {
        return toolCacheMutex.withLock {
            val toolInfoList = toolIds.map { ToolInfo.fromId(it) }
            cachedTools = toolInfoList
            cacheTimestamp = System.currentTimeMillis()
            
            // Update capabilities
            _capabilities.value = buildCapabilities(toolInfoList)
            
            Napier.d("$TAG: Updated tool cache with ${toolInfoList.size} tools")
            toolInfoList
        }
    }
    
    private fun buildCapabilities(tools: List<ToolInfo>): ToolCapabilities {
        val categoryCounts = tools.groupBy { it.category }
            .mapValues { it.value.size }
        val writeOps = tools.count { it.isWriteOperation }
        
        return ToolCapabilities(
            totalTools = tools.size,
            categoryCounts = categoryCounts,
            writeOperations = writeOps,
            readOnlyOperations = tools.size - writeOps,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
