# OpenCode Kotlin SDK Documentation

**Version:** 1.0.0
**Generated:** 2026-01-21
**Language:** Kotlin Multiplatform (Android-only)
**Target:** OpenCode AI Agent API

## Overview

MOCCA (Mobile OpenCode Companion App) provides the official Kotlin SDK for OpenCode. There is no separate official Kotlin SDK - MOCCA defines the standards for Kotlin integration with OpenCode's REST API, Server-Sent Events (SSE), and Git operations.

### Architecture

The SDK consists of four primary clients:

- **MoccaApiClient**: Main REST API client for OpenCode server (Port 4096)
- **GitApiClient**: Specialized Git operations on port 4097
- **MoccaSseClient**: Real-time event streaming via SSE
- **HttpClientProvider**: Dynamic HTTP client lifecycle and configuration management

### Key Concepts

- **Dual-Server Architecture**: OpenCode Server (4096) for AI operations, Git Server (4097) for version control
- **Environment Detection**: Automatic handling of Android Emulator (`10.0.2.2`) vs physical device
- **Offline-First Design**: Repositories return `Flow<Resource<T>>` for caching strategies
- **Resilience**: Circuit breaker, exponential backoff retry, and connection quality monitoring
- **Type Safety**: All API models use immutable data classes with proper serialization

## Quick Start

### 1. Dependencies

```kotlin
// build.gradle.kts (commonMain)
implementation("io.ktor:ktor-client-core:2.3.7")
implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
implementation("io.ktor:ktor-client-serialization:2.3.7")
implementation("io.ktor:ktor-client-websockets:2.3.7")
implementation("io.ktor:ktor-client-sse:2.3.7")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
implementation("io.github.aakira:napier:2.6.1")
```

### 2. Initialize Clients

```kotlin
import com.mocca.app.api.*
import com.mocca.app.domain.model.*
import io.ktor.client.engine.okhttp.OkHttpEngine

// Create server config
val serverConfig = ServerConfig(
    id = "default",
    name = "OpenCode Server",
    baseUrl = "http://10.0.2.2:4096",  // Android emulator
    connectionType = ConnectionType.LOCAL,
    authType = AuthType.BEARER,
    authToken = "your-api-token",
    isActive = true
)

// Initialize HTTP client provider
val httpClientProvider = HttpClientProvider(
    serverConfigRepository = ServerConfigRepository(localCache),
    networkObserver = null
)

// Create main API client
val apiClient = MoccaApiClient(
    httpClient = httpClientProvider.getClientSync(),
    serverConfigProvider = { serverConfig },
    retryPolicy = RetryPolicy.Default,
    httpClientProvider = httpClientProvider
)

// Create Git API client
val gitClient = GitApiClient(
    httpClientProvider = httpClientProvider,
    serverConfigProvider = { serverConfig },
    retryPolicy = RetryPolicy.Default
)

// Create SSE client
val sseClient = MoccaSseClient(
    httpClient = httpClientProvider.getClientSync(),
    serverConfigProvider = { serverConfig },
    retryPolicy = RetryPolicy.Aggressive,
    httpClientProvider = httpClientProvider
)
```

### 3. Basic Usage

```kotlin
// Check server health
val healthResult = apiClient.getHealth()
healthResult.onSuccess { appInfo ->
    println("Server version: ${appInfo.version}")
}.onFailure { error ->
    println("Health check failed: ${error.message}")
}

// List all sessions
val sessionsResult = apiClient.listSessions()
sessionsResult.onSuccess { sessions ->
    sessions.forEach { session ->
        println("Session: ${session.title} (${session.status})")
    }
}
```

## Session Management

### List Sessions

Retrieve all OpenCode sessions.

```kotlin
suspend fun listSessions(): Result<List<Session>>

val result = apiClient.listSessions()
result.onSuccess { sessions ->
    // Session fields:
    // - id: String
    // - title: String?
    // - slug: String?
    // - status: SessionStatus (IDLE, RUNNING, COMPLETED, ERROR)
    // - createdAt: Long
    // - updatedAt: Long
    // - projectID: String?
    // - directory: String?
    // - parentID: String?
    // - summary: SessionSummary?
    // - permission: List<SessionPermission>?
    // - revert: SessionRevertInfo?
    // - shareID: String?
    // - version: String?
}.onFailure { error ->
    // Handle NetworkError
}
```

### Create Session

Create a new OpenCode conversation session.

```kotlin
suspend fun createSession(): Result<Session>

val result = apiClient.createSession()
result.onSuccess { session ->
    println("Created session: ${session.id}")
}.onFailure { error ->
    println("Failed to create session: ${error.message}")
}
```

### Get Children

Get child sessions (forks) of a parent session.

```kotlin
suspend fun getChildren(sessionId: String): Result<List<Session>>

val result = apiClient.getChildren(sessionId)
result.onSuccess { children ->
    children.forEach { child ->
        println("Child session: ${child.id}")
    }
}
```

### Delete Session

Delete a session permanently.

```kotlin
suspend fun deleteSession(sessionId: String): Result<Unit>

val result = apiClient.deleteSession(sessionId)
result.onSuccess {
    println("Session deleted")
}.onFailure { error ->
    println("Delete failed: ${error.message}")
}
```

### Abort Session

Stop a currently running session.

```kotlin
suspend fun abortSession(sessionId: String): Result<Boolean>

val result = apiClient.abortSession(sessionId)
result.onSuccess { aborted ->
    if (aborted) {
        println("Session aborted successfully")
    }
}
```

### Fork Session

Create a new session from a specific message in history.

```kotlin
suspend fun forkSession(
    sessionId: String,
    messageId: String? = null
): Result<Session>

val result = apiClient.forkSession(
    sessionId = "ses_abc123",
    messageId = "msg_def456"  // Fork from this message
)
result.onSuccess { forkedSession ->
    println("Forked session: ${forkedSession.id}")
}
```

### Revert Session

Revert a session to a previous state (messages after specified message are hidden).

```kotlin
suspend fun revertSession(
    sessionId: String,
    messageId: String,
    partId: String? = null
): Result<Session>

val result = apiClient.revertSession(
    sessionId = "ses_abc123",
    messageId = "msg_def456",
    partId = "part_789"  // Optional: revert to specific part
)
result.onSuccess { revertedSession ->
    println("Session reverted. Messages after ${messageId} are hidden.")
}
```

### Unrevert Session

Restore all hidden messages in a reverted session.

```kotlin
suspend fun unrevertSession(sessionId: String): Result<Session>

val result = apiClient.unrevertSession(sessionId)
result.onSuccess { session ->
    println("Session unreverted. All messages visible again.")
}
```

### Update Session Title

Change the title of an existing session.

```kotlin
suspend fun updateSession(sessionId: String, title: String): Result<Session>

val result = apiClient.updateSession(
    sessionId = "ses_abc123",
    title = "New Session Title"
)
result.onSuccess { updatedSession ->
    println("Session title updated: ${updatedSession.title}")
}
```

### Get Session Status

Retrieve status information for all sessions.

```kotlin
suspend fun getSessionStatus(): Result<Map<String, SessionStatusInfo>>

val result = apiClient.getSessionStatus()
result.onSuccess { statusMap ->
    statusMap.forEach { (sessionId, statusInfo) ->
        // statusInfo.type: "idle", "busy", or "retry"
        // statusInfo.attempt: Int? - retry attempt count
        // statusInfo.message: String? - status message
        // statusInfo.next: Long? - next retry time
        when (statusInfo.type) {
            "idle" -> println("Session $sessionId is idle")
            "busy" -> println("Session $sessionId is busy")
            "retry" -> println("Session $sessionId is retrying")
        }
    }
}
```

### Summarize Session

Generate a title and summary for a session using AI.

```kotlin
suspend fun summarizeSession(sessionId: String): Result<Session>

val result = apiClient.summarizeSession(sessionId)
result.onSuccess { session ->
    println("Session summarized: ${session.title}")
}
```

### Initialize Session

Initialize a session with system prompts and configuration.

```kotlin
suspend fun initSession(
    sessionId: String,
    request: InitSessionRequest
): Result<Session>

val result = apiClient.initSession(
    sessionId = "ses_abc123",
    request = InitSessionRequest(
        messageID = "msg_def456",
        providerID = "anthropic",
        modelID = "claude-sonnet-4-5"
    )
)
result.onSuccess { session ->
    println("Session initialized")
}
```

### Get Session Todos

Retrieve todo list for a session.

```kotlin
suspend fun getSessionTodos(sessionId: String): Result<List<Todo>>

val result = apiClient.getSessionTodos(sessionId)
result.onSuccess { todos ->
    todos.forEach { todo ->
        // todo.id: String
        // todo.content: String
        // todo.status: TodoStatus (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)
        // todo.priority: TodoPriority (HIGH, MEDIUM, LOW)
        // todo.createdAt: Long?
        // todo.completedAt: Long?
        println("Todo: ${todo.content} [${todo.status}]")
    }
}
```

### Share Session

Make a session publicly accessible.

```kotlin
suspend fun shareSession(sessionId: String): Result<Session>

val result = apiClient.shareSession(sessionId)
result.onSuccess { sharedSession ->
    println("Session shared with ID: ${sharedSession.shareID}")
}
```

### Unshare Session

Revoke public access to a session.

```kotlin
suspend fun unshareSession(sessionId: String): Result<Session>

val result = apiClient.unshareSession(sessionId)
result.onSuccess {
    println("Session unshared. No longer public.")
}
```

## Messaging

### Get Messages

Retrieve all messages in a session.

```kotlin
suspend fun getMessages(sessionId: String): Result<List<MessageResponse>>

val result = apiClient.getMessages(sessionId)
result.onSuccess { messages ->
    messages.forEach { response ->
        val info = response.info
        val parts = response.parts

        // MessageInfo fields:
        // - id: String
        // - role: MessageRole (USER, ASSISTANT, SYSTEM)
        // - sessionID: String
        // - time: MessageTime?
        // - agent: String?
        // - model: ModelInfo?
        // - cost: Double?
        // - tokens: TokenUsage?
        // - summary: JsonElement?

        parts.forEach { part ->
            // MessagePartResponse fields:
            // - id: String
            // - type: String ("text", "tool", "file", "step-start", "step-finish")
            // - text: String? (for text parts)
            // - tool: String? (for tool parts)
            // - state: ToolStateResponse? (for tool parts)
            // - mime: String? (for file parts)
            // - url: String? (for file parts)
            // - filename: String? (for file parts)
        }
    }
}
```

### Chat (Sync)

Send a message and wait for the full response.

```kotlin
suspend fun chat(
    sessionId: String,
    modelId: String,
    providerId: String,
    parts: List<ChatPart>,
    mode: String? = null
): Result<AssistantMessageInfo>

val result = apiClient.chat(
    sessionId = "ses_abc123",
    modelId = "claude-sonnet-4-5",
    providerId = "anthropic",
    parts = listOf(
        ChatPart.Text(
            text = "Write a function to sort an array",
            id = "part_1"
        )
    ),
    mode = "standard"  // Optional mode override
)
result.onSuccess { messageInfo ->
    // AssistantMessageInfo fields:
    // - id: String
    // - role: String
    // - sessionID: String
    // - modelID: String?
    // - providerID: String?
    // - mode: String?
    // - cost: Double?
    // - tokens: TokenInfo?
    // - time: MessageTimeInfo?
    // - system: List<String>?
    println("Response ID: ${messageInfo.id}")
}
```

### Chat Async

Send a message and return immediately (response arrives via SSE).

```kotlin
suspend fun chatAsync(
    sessionId: String,
    modelId: String,
    providerId: String,
    parts: List<ChatPart>,
    mode: String? = null
): Result<Unit>

val result = apiClient.chatAsync(
    sessionId = "ses_abc123",
    modelId = "claude-sonnet-4-5",
    providerId = "anthropic",
    parts = listOf(
        ChatPart.Text(text = "Explain this code"),
        ChatPart.File(
            mime = "image/png",
            url = "https://example.com/image.png",
            filename = "screenshot.png"
        )
    )
)
result.onSuccess {
    println("Message sent. Listen to SSE for response.")
}.onFailure { error ->
    println("Failed to send message: ${error.message}")
}
```

### Reply to Permission (New)

Approve or deny a tool execution permission.

```kotlin
suspend fun replyToPermission(
    requestId: String,
    reply: PermissionResponseType,
    message: String? = null
): Result<Boolean>

val result = apiClient.replyToPermission(
    requestId = "perm_abc123",
    reply = PermissionResponseType.ALWAYS,  // ONCE, ALWAYS, or REJECT
    message = "Trusted workspace file"  // Optional message for rejection
)
result.onSuccess { approved ->
    if (approved) {
        println("Permission granted for all future uses")
    }
}
```

### List Pending Permissions

Get all pending permission requests.

```kotlin
suspend fun listPendingPermissions(): Result<List<PermissionRequest>>

val result = apiClient.listPendingPermissions()
result.onSuccess { permissions ->
    permissions.forEach { permission ->
        // PermissionRequest fields:
        // - id: String
        // - sessionId: String
        // - permission: String
        // - patterns: List<String>
        // - always: List<String>
        println("Permission request: ${permission.permission}")
    }
}
```

### Reply to Question

Answer an interactive question from the agent.

```kotlin
suspend fun replyToQuestion(
    requestId: String,
    answers: List<List<String>>
): Result<Boolean>

val result = apiClient.replyToQuestion(
    requestId = "question_abc123",
    answers = listOf(
        listOf("option1", "option2"),  // Multiple selections allowed
        listOf("selected_option")           // Single selection
    )
)
result.onSuccess {
    println("Question answered")
}
```

### Reject Question

Reject an interactive question.

```kotlin
suspend fun rejectQuestion(requestId: String): Result<Boolean>

val result = apiClient.rejectQuestion(requestId)
result.onSuccess {
    println("Question rejected")
}
```

### List Pending Questions

Get all pending question requests.

```kotlin
suspend fun listPendingQuestions(): Result<List<QuestionRequest>>

val result = apiClient.listPendingQuestions()
result.onSuccess { questions ->
    questions.forEach { question ->
        // QuestionRequest fields:
        // - id: String
        // - sessionId: String
        // - questions: List<QuestionInfo>

        question.questions.forEach { q ->
            // QuestionInfo fields:
            // - question: String
            // - header: String
            // - options: List<QuestionOption>
            // - multiple: Boolean
            println("Question: ${q.question}")
        }
    }
}
```

## Configuration

### Get Health

Check OpenCode server health and version.

```kotlin
suspend fun getHealth(): Result<AppInfo>

val result = apiClient.getHealth()
result.onSuccess { appInfo ->
    // AppInfo fields:
    // - version: String
    // - initialized: Boolean
    // - healthy: Boolean
    println("Server version: ${appInfo.version}")
}
```

### Get Providers

Retrieve all available AI model providers.

```kotlin
suspend fun getProviders(): Result<List<Provider>>

val result = apiClient.getProviders()
result.onSuccess { providers ->
    providers.forEach { provider ->
        // Provider fields:
        // - id: String
        // - name: String
        // - models: Map<String, Model>  (modelID -> Model)

        provider.models.forEach { (modelId, model) ->
            // Model fields:
            // - id: String
            // - name: String
            println("Model: ${model.name} (${provider.id}/${modelId})")
        }
    }
}
```

### Get Modes

Retrieve all available agent modes (e.g., coding, analysis, brainstorming).

```kotlin
suspend fun getModes(): Result<List<Mode>>

val result = apiClient.getModes()
result.onSuccess { modes ->
    modes.forEach { mode ->
        // Mode fields:
        // - id: String
        // - name: String
        // - description: String?
        println("Mode: ${mode.name} - ${mode.description}")
    }
}
```

### Get Config

Retrieve full server configuration.

```kotlin
suspend fun getConfig(): Result<ConfigResponse>

val result = apiClient.getConfig()
result.onSuccess { config ->
    // ConfigResponse fields:
    // - model: String? (default model)
    // - providers: List<Provider>
    // - agents: List<AgentConfig>
    // - modes: List<Mode>
    println("Default model: ${config.model}")
}
```

### Get Provider Info

Get information about currently configured provider.

```kotlin
suspend fun getProviderInfo(): Result<ProviderResponse>

val result = apiClient.getProviderInfo()
result.onSuccess { providerInfo ->
    // ProviderResponse fields:
    // - default: DefaultProvider?
    // - providers: Map<String, ProviderModel>
    println("Provider info retrieved")
}
```

### Get Providers Config

Get providers configuration.

```kotlin
suspend fun getProvidersConfig(): Result<ProvidersConfig>

val result = apiClient.getProvidersConfig()
result.onSuccess { providersConfig ->
    // ProvidersConfig fields:
    // - providers: List<Provider>
    println("Providers config retrieved")
}
```

### Update Config

Update server configuration settings.

```kotlin
suspend fun updateConfig(update: ConfigUpdate): Result<ConfigResponse>

val result = apiClient.updateConfig(
    update = ConfigUpdate(
        model = ModelConfig(
            default = "anthropic/claude-sonnet-4-5",
            reasoning = "anthropic/claude-3-7-sonnet-20250219"
        ),
        provider = "anthropic",
        theme = "dark",
        autosave = true,
        experimental = mapOf(
            "feature_x" to kotlinx.serialization.json.encodeToJson(true)
        )
    )
)
result.onSuccess { updatedConfig ->
    println("Configuration updated")
}
```

## OAuth and Authentication

### Get Provider Auth Methods

Get available authentication methods for a provider.

```kotlin
suspend fun getProviderAuthMethods(providerId: String): Result<List<ProviderAuthMethod>>

val result = apiClient.getProviderAuthMethods(providerId = "anthropic")
result.onSuccess { authMethods ->
    authMethods.forEach { method ->
        // ProviderAuthMethod fields:
        // - type: String ("api_key", "oauth", etc.)
        println("Auth method: ${method.type}")
    }
}
```

### Authorize Provider

Initiate OAuth authorization flow.

```kotlin
suspend fun authorizeProvider(providerId: String): Result<ProviderAuthAuthorization>

val result = apiClient.authorizeProvider(providerId = "openai")
result.onSuccess { authorization ->
    // ProviderAuthAuthorization fields:
    // - url: String (authorization URL)
    // - state: String (OAuth state)

    println("Open authorization URL: ${authorization.url}")
}
```

### Handle OAuth Callback

Complete OAuth flow after user authorization.

```kotlin
suspend fun handleOAuthCallback(
    providerId: String,
    code: String,
    state: String
): Result<Unit>

val result = apiClient.handleOAuthCallback(
    providerId = "openai",
    code = "auth_code_from_callback",
    state = "state_from_authorization"
)
result.onSuccess {
    println("OAuth callback handled successfully")
}
```

### Set Provider Auth

Set provider authentication credentials manually (API key).

```kotlin
suspend fun setProviderAuth(
    providerId: String,
    credentials: ProviderCredentials
): Result<Unit>

val result = apiClient.setProviderAuth(
    providerId = "anthropic",
    credentials = ProviderCredentials.ApiKey(
        apiKey = "sk-ant-..."
    )
)
result.onSuccess {
    println("Provider authentication set")
}
```

## Command Execution

### Execute Slash Command

Execute a predefined slash command.

```kotlin
suspend fun executeCommand(
    sessionId: String,
    command: String,
    arguments: String? = null,
    agent: String? = null
): Result<Unit>

val result = apiClient.executeCommand(
    sessionId = "ses_abc123",
    command = "summarize",
    arguments = "key points",
    agent = "build"
)
result.onSuccess {
    println("Command executed")
}
```

### Execute Shell Command

Execute a shell command directly.

```kotlin
suspend fun executeShell(
    sessionId: String,
    command: String,
    agent: String = "build"
): Result<String>

val result = apiClient.executeShell(
    sessionId = "ses_abc123",
    command = "npm test",
    agent = "build"
)
result.onSuccess { output ->
    println("Command output: $output")
}
```

## Project Management

### List Projects

Retrieve all available projects.

```kotlin
suspend fun listProjects(): Result<List<Project>>

val result = apiClient.listProjects()
result.onSuccess { projects ->
    projects.forEach { project ->
        // Project fields:
        // - id: String?
        // - name: String?
        // - path: String?
        // - directory: String?
        // - worktree: String?
        // - vcs: VcsType? (GIT, NONE)
        // - createdAt: Long?
        println("Project: ${project.displayName}")
    }
}
```

### Get Current Project

Get the active project.

```kotlin
suspend fun getCurrentProject(): Result<Project>

val result = apiClient.getCurrentProject()
result.onSuccess { project ->
    println("Current project: ${project.displayName}")
}
```

## MCP Operations

### Get MCP Status

Retrieve status of all MCP servers.

```kotlin
suspend fun getMcpStatus(directory: String? = null): Result<Map<String, McpServerStatus>>

val result = apiClient.getMcpStatus(directory = "/path/to/project")
result.onSuccess { statusMap ->
    statusMap.forEach { (serverName, status) ->
        // McpServerStatus fields:
        // - status: String ("connected", "disconnected", "error")
        // - error: String?
        println("MCP server $serverName: ${status.status}")
    }
}
```

### Connect MCP

Connect to an MCP server.

```kotlin
suspend fun connectMcp(
    name: String,
    directory: String? = null
): Result<Unit>

val result = apiClient.connectMcp(
    name = "filesystem",
    directory = "/path/to/project"
)
result.onSuccess {
    println("MCP server connected")
}
```

### Disconnect MCP

Disconnect from an MCP server.

```kotlin
suspend fun disconnectMcp(
    name: String,
    directory: String? = null
): Result<Unit>

val result = apiClient.disconnectMcp(name = "filesystem")
result.onSuccess {
    println("MCP server disconnected")
}
```

### Configure MCP

Update MCP server configuration.

```kotlin
suspend fun configureMcp(
    name: String,
    config: McpServerConfig
): Result<Unit>

val result = apiClient.configureMcp(
    name = "filesystem",
    config = McpServerConfig(
        // Config fields depend on MCP server type
        command = "my-mcp-server",
        args = listOf("--path", "/data")
    )
)
result.onSuccess {
    println("MCP server configured")
}
```

### Add MCP Server

Dynamically add a new MCP server.

```kotlin
suspend fun addMcpServer(
    name: String,
    config: McpServerConfig
): Result<Unit>

val result = apiClient.addMcpServer(
    name = "custom-mcp",
    config = McpServerConfig(
        command = "my-custom-server",
        args = listOf("--port", "8080")
    )
)
result.onSuccess {
    println("MCP server added")
}
```

## File Operations

### List Files

Browse the project filesystem.

```kotlin
suspend fun listFiles(path: String = "."): Result<List<FileInfo>>

val result = apiClient.listFiles(path = "src/main/kotlin")
result.onSuccess { files ->
    files.forEach { file ->
        // FileInfo fields:
        // - name: String
        // - path: String
        // - type: String ("file" or "directory")
        // - size: Long?
        // - modifiedAt: Long?

        if (file.isDirectory) {
            println("📁 ${file.name}")
        } else {
            println("📄 ${file.name} (${file.size} bytes)")
        }
    }
}
```

### Get File Content

Retrieve the content of a specific file.

```kotlin
suspend fun getFileContent(path: String): Result<FileContent>

val result = apiClient.getFileContent(path = "src/main/kotlin/Main.kt")
result.onSuccess { fileContent ->
    // FileContent fields:
    // - path: String
    // - content: String
    // - language: String?

    println("File: ${fileContent.path}")
    println("Language: ${fileContent.language}")
    println("Content length: ${fileContent.content.length}")
}
```

### Get File Status

Get file status including Git status and diagnostics.

```kotlin
suspend fun getFileStatus(path: String): Result<FileStatus>

val result = apiClient.getFileStatus(path = "src/main/kotlin/Main.kt")
result.onSuccess { fileStatus ->
    // FileStatus fields:
    // - path: String
    // - gitStatus: GitStatus?
    // - diagnostics: List<Diagnostic>

    fileStatus.diagnostics.forEach { diagnostic ->
        // Diagnostic fields:
        // - severity: DiagnosticSeverity (ERROR, WARNING, INFORMATION, HINT)
        // - message: String
        // - line: Int
        // - column: Int
        println("${diagnostic.severity}: ${diagnostic.message} at line ${diagnostic.line}")
    }
}
```

### Update File

Write content to a file (creates or overwrites).

```kotlin
suspend fun updateFile(path: String, content: String): Result<Unit>

val result = apiClient.updateFile(
    path = "src/main/kotlin/NewFile.kt",
    content = "fun hello() {\n    println(\"Hello, World!\")\n}"
)
result.onSuccess {
    println("File updated successfully")
}
```

## Search Operations

### Search Text

Search for text across project files.

```kotlin
suspend fun searchText(
    query: String,
    path: String = ""
): Result<List<SearchResult>>

val result = apiClient.searchText(
    query = "function name",
    path = "src/main/kotlin"
)
result.onSuccess { results ->
    results.forEach { searchResult ->
        // SearchResult fields:
        // - file: String
        // - line: Int
        // - column: Int
        // - match: String
        // - context: String

        println("${searchResult.file}:${searchResult.line} - ${searchResult.match}")
    }
}
```

### Find Files

Find files by name pattern.

```kotlin
suspend fun findFiles(pattern: String): Result<List<String>>

val result = apiClient.findFiles(pattern = "*.kt")
result.onSuccess { files ->
    files.forEach { filePath ->
        println(filePath)
    }
}
```

### Find Symbols

Search for code symbols (functions, classes, variables).

```kotlin
suspend fun findSymbols(query: String): Result<List<SymbolResult>>

val result = apiClient.findSymbols(query = "User")
result.onSuccess { symbols ->
    symbols.forEach { symbol ->
        // SymbolResult fields:
        // - name: String
        // - kind: String ("function", "class", "variable", etc.)
        // - file: String
        // - line: Int

        println("${symbol.kind}: ${symbol.name} at ${symbol.file}:${symbol.line}")
    }
}
```

## Agent, Tools, and Commands

### Get Agents

List available agents.

```kotlin
suspend fun getAgents(): Result<List<Agent>>

val result = apiClient.getAgents()
result.onSuccess { agents ->
    agents.forEach { agent ->
        // Agent fields:
        // - id: String
        // - name: String
        // - description: String?
        println("Agent: ${agent.name} (${agent.id})")
    }
}
```

### Get Tool IDs

Get list of available tool IDs.

```kotlin
suspend fun getToolIds(): Result<List<String>>

val result = apiClient.getToolIds()
result.onSuccess { toolIds ->
    toolIds.forEach { toolId ->
        println("Tool: $toolId")
    }
}
```

### Get Commands

List available slash commands.

```kotlin
suspend fun getCommands(): Result<List<Command>>

val result = apiClient.getCommands()
result.onSuccess { commands ->
    commands.forEach { command ->
        // Command fields:
        // - id: String
        // - name: String
        // - description: String?
        println("Command: /${command.name}")
    }
}
```

### Get Formatters

List available code formatters.

```kotlin
suspend fun getFormatters(): Result<List<FormatterStatus>>

val result = apiClient.getFormatters()
result.onSuccess { formatters ->
    formatters.forEach { formatter ->
        // FormatterStatus fields:
        // - id: String
        // - name: String
        // - enabled: Boolean
        println("Formatter: ${formatter.name}")
    }
}
```

### Get LSP Status

Get Language Server Protocol status.

```kotlin
suspend fun getLspStatus(): Result<List<LspStatus>>

val result = apiClient.getLspStatus()
result.onSuccess { lspStatusList ->
    lspStatusList.forEach { lspStatus ->
        // LspStatus fields:
        // - serverID: String
        // - path: String
        // - state: String
        println("LSP server: ${lspStatus.serverID} - ${lspStatus.state}")
    }
}
```

### Get VCS Info

Get version control system information.

```kotlin
suspend fun getVcsInfo(): Result<VcsInfo>

val result = apiClient.getVcsInfo()
result.onSuccess { vcsInfo ->
    // VcsInfo fields (response from /vcs endpoint):
    // - type: String (e.g., "git")
    // - branch: String
    // - root: String?
    println("VCS: ${vcsInfo.type} on ${vcsInfo.branch}")
}
```

### Get Session Diffs

Get file changes made in a session.

```kotlin
suspend fun getSessionDiffs(sessionId: String): Result<List<FileDiff>>

val result = apiClient.getSessionDiffs(sessionId)
result.onSuccess { diffs ->
    diffs.forEach { diff ->
        // FileDiff fields:
        // - file: String
        // - additions: Int
        // - deletions: Int
        // - hunks: List<DiffHunk>

        println("${diff.file}: +${diff.additions} -${diff.deletions}")
    }
}
```

## Terminal Operations

### List Terminals

Get all active terminals.

```kotlin
suspend fun listTerminals(): Result<List<Terminal>>

val result = apiClient.listTerminals()
result.onSuccess { terminals ->
    terminals.forEach { terminal ->
        // Terminal fields:
        // - id: String
        // - shell: String? (e.g., "bash", "zsh")
        println("Terminal ${terminal.id}: ${terminal.shell}")
    }
}
```

### Create Terminal

Create a new terminal session.

```kotlin
suspend fun createTerminal(): Result<Terminal>

val result = apiClient.createTerminal()
result.onSuccess { terminal ->
    println("Created terminal: ${terminal.id}")
}
```

### Resize Terminal

Resize terminal window.

```kotlin
suspend fun resizeTerminal(
    id: String,
    cols: Int,
    rows: Int
): Result<Unit>

val result = apiClient.resizeTerminal(
    id = "term_abc123",
    cols = 80,
    rows = 24
)
result.onSuccess {
    println("Terminal resized")
}
```

### Connect to Terminal WebSocket

Establish WebSocket connection to terminal for bidirectional communication.

```kotlin
suspend fun connectToTerminal(
    id: String,
    block: suspend DefaultClientWebSocketSession.() -> Unit
)

apiClient.connectToTerminal(id = "term_abc123") { session ->
    // Send input to terminal
    session.send("echo 'Hello, World!'")

    // Receive output from terminal
    session.incoming.receive { frame ->
        val text = frame.readText()
        println("Terminal output: $text")
    }
}
```

## Path Operations

### Get Current Path

Get current working directory path.

```kotlin
suspend fun getCurrentPath(): Result<PathInfo>

val result = apiClient.getCurrentPath()
result.onSuccess { pathInfo ->
    // PathInfo fields:
    // - cwd: String (current working directory)
    // - home: String? (user home directory)
    // - root: String? (project root directory)

    println("CWD: ${pathInfo.cwd}")
}
```

## Logging

### Send Log Entry

Send a log entry to the server.

```kotlin
suspend fun sendLog(entry: LogEntry): Result<Unit>

val result = apiClient.sendLog(
    entry = LogEntry(
        service = "my-app",
        level = "info",  // "debug", "info", "warn", "error"
        message = "User performed action X",
        extra = mapOf(
            "userId" to "123",
            "action" to "login"
        )
    )
)
result.onSuccess {
    println("Log entry sent")
}
```

## Instance Operations

### Dispose Instance

Gracefully dispose of an OpenCode instance.

```kotlin
suspend fun disposeInstance(): Result<Unit>

val result = apiClient.disposeInstance()
result.onSuccess {
    println("Instance disposed")
}
```

## Server-Sent Events (SSE)

### Subscribe to Events

Subscribe to session-specific events for real-time updates.

```kotlin
fun subscribeToEvents(): Flow<ServerEvent>

val eventFlow = sseClient.subscribeToEvents()
eventFlow.collect { event ->
    when (event) {
        is ServerEvent.Connected -> {
            // Connection established
            println("Server connected: ${event.properties.version}")
        }
        is ServerEvent.Heartbeat -> {
            // Periodic heartbeat
        }
        is ServerEvent.SessionUpdated -> {
            // Session metadata updated
            println("Session updated: ${event.properties.info.title}")
        }
        is ServerEvent.MessageUpdated -> {
            // New or updated message
            println("Message: ${event.properties.info.id}")
        }
        is ServerEvent.MessagePartUpdated -> {
            // Message part updated (e.g., tool status change)
            val part = event.properties.part
            println("Tool ${part.tool}: ${part.state?.status}")
        }
        is ServerEvent.PermissionAsked -> {
            // New permission request
            val request = PermissionRequest.fromEvent(event)
            println("Permission request: ${request.permission}")
            // Handle user approval
        }
        is ServerEvent.QuestionAsked -> {
            // New question request
            val request = QuestionRequest.fromEvent(event)
            request.questions.forEach { q ->
                println("Question: ${q.question}")
            }
        }
        is ServerEvent.FileEdited -> {
            // File edited by agent
            println("File edited: ${event.properties.file}")
        }
        is ServerEvent.AgentStatus -> {
            // Agent status change
            val props = event.properties
            println("Agent ${props.agentName}: ${props.status}")
        }
        is ServerEvent.SessionDeleted -> {
            // Session deleted
            println("Session deleted: ${event.properties.info.id}")
        }
        is ServerEvent.InstallationUpdated -> {
            // Installation update
            println("Server version: ${event.properties.version}")
        }
        else -> {
            // Unknown event type
            val unknown = event as ServerEvent.Unknown
            println("Unknown event: ${unknown.type}")
        }
    }
}
```

### Subscribe to Global Events

Subscribe to system-wide events (not session-specific).

```kotlin
fun subscribeToGlobalEvents(): Flow<ServerEvent>

val globalEventFlow = sseClient.subscribeToGlobalEvents()
globalEventFlow.collect { event ->
    when (event) {
        is ServerEvent.LspDiagnostics -> {
            // LSP diagnostics update
            println("Diagnostics for: ${event.properties.path}")
        }
        is ServerEvent.Log -> {
            // Server log entry
            println("Log [${event.properties.level}]: ${event.properties.message}")
        }
        // Other events same as session-specific stream
    }
}
```

## Git Operations

MOCCA uses a dedicated Git HTTP server on port 4097 for all Git operations.

### Get Git Status

Get current Git repository status.

```kotlin
suspend fun getStatus(): Result<GitStatusResponse>

val result = gitClient.getStatus()
result.onSuccess { gitStatus ->
    // GitStatusResponse fields:
    // - branch: String
    // - upstream: String?
    // - ahead: Int
    // - behind: Int
    // - staged: List<GitFileChange>
    // - unstaged: List<GitFileChange>
    // - untracked: List<String>
    // - conflicted: List<String>
    // - clean: Boolean

    gitStatus.staged.forEach { change ->
        // GitFileChange fields:
        // - path: String
        // - status: GitFileStatus (ADDED, MODIFIED, DELETED, RENAMED, COPIED, UNMERGED, UNKNOWN)
        println("Staged: ${change.path} (${change.status})")
    }

    gitStatus.unstaged.forEach { change ->
        println("Unstaged: ${change.path}")
    }

    gitStatus.untracked.forEach { file ->
        println("Untracked: $file")
    }
}
```

### Get Git Log

Retrieve commit history.

```kotlin
suspend fun getLog(
    limit: Int = 50,
    skip: Int = 0,
    branch: String? = null
): Result<GitLog>

val result = gitClient.getLog(
    limit = 100,
    skip = 0,
    branch = "main"
)
result.onSuccess { gitLog ->
    gitLog.commits.forEach { commit ->
        // GitCommit fields:
        // - hash: String
        // - shortHash: String
        // - message: String
        // - author: String
        // - email: String?
        // - date: Long
        // - parents: List<String>
        // - refs: List<String>

        println("${commit.shortHash}: ${commit.message} (${commit.author})")
    }
    println("Total: ${gitLog.total}, Has more: ${gitLog.hasMore}")
}
```

### Get Git Branches

List all branches.

```kotlin
suspend fun getBranches(): Result<List<GitBranch>>

val result = gitClient.getBranches()
result.onSuccess { branches ->
    branches.forEach { branch ->
        // GitBranch fields:
        // - name: String
        // - current: Boolean
        // - remote: Boolean
        // - upstream: String?
        // - ahead: Int
        // - behind: Int
        // - lastCommit: String?
        // - lastCommitTime: Long?

        val marker = if (branch.current) -> "*" else " "
        println("$marker ${branch.name}")
    }
}
```

### Get Git Diff

Get file differences.

```kotlin
suspend fun getDiff(
    path: String? = null,
    cached: Boolean = false
): Result<GitDiff>

val result = gitClient.getDiff(
    path = "src/main/kotlin/",
    cached = true  // Show staged changes
)
result.onSuccess { gitDiff ->
    gitDiff.files.forEach { file ->
        // GitDiffFile fields:
        // - path: String
        // - status: GitFileStatus
        // - additions: Int
        // - deletions: Int
        // - binary: Boolean
        // - hunks: List<GitDiffHunk>

        println("${file.path}: +${file.additions} -${file.deletions}")
    }
}
```

### Commit Changes

Create a Git commit.

```kotlin
suspend fun commit(
    message: String,
    files: List<String>? = null,
    amend: Boolean = false
): Result<GitOperationResult>

val result = gitClient.commit(
    message = "Fix authentication bug",
    files = listOf("src/auth/UserService.kt"),  // null = all staged
    amend = false
)
result.onSuccess { operationResult ->
    // GitOperationResult fields:
    // - success: Boolean
    // - message: String?
    // - error: String?

    if (operationResult.success) {
        println("Commit successful: ${operationResult.message}")
    }
}
```

### Push Changes

Push commits to remote repository.

```kotlin
suspend fun push(
    remote: String = "origin",
    branch: String? = null,
    force: Boolean = false,
    setUpstream: Boolean = false
): Result<GitOperationResult>

val result = gitClient.push(
    remote = "origin",
    branch = "feature/new-auth",
    force = false,
    setUpstream = true
)
result.onSuccess { operationResult ->
    println("Push successful: ${operationResult.message}")
}
```

### Pull Changes

Pull changes from remote repository.

```kotlin
suspend fun pull(
    remote: String = "origin",
    branch: String? = null,
    rebase: Boolean = false
): Result<GitOperationResult>

val result = gitClient.pull(
    remote = "origin",
    branch = "main",
    rebase = false
)
result.onSuccess { operationResult ->
    println("Pull successful: ${operationResult.message}")
}
```

### Fetch Changes

Fetch changes from remote repository without merging.

```kotlin
suspend fun fetch(
    remote: String = "origin",
    prune: Boolean = false,
    all: Boolean = false
): Result<GitOperationResult>

val result = gitClient.fetch(
    remote = "origin",
    prune = true,
    all = false
)
result.onSuccess { operationResult ->
    println("Fetch successful: ${operationResult.message}")
}
```

### Checkout Branch/Commit

Switch to a branch or commit.

```kotlin
suspend fun checkout(
    ref: String,
    create: Boolean = false,
    force: Boolean = false
): Result<GitOperationResult>

val result = gitClient.checkout(
    ref = "feature/new-feature",
    create = true,  // Create new branch
    force = false
)
result.onSuccess { operationResult ->
    println("Checkout successful: ${operationResult.message}")
}
```

### Stage Files

Stage files for commit.

```kotlin
suspend fun stage(files: List<String>): Result<GitOperationResult>

val result = gitClient.stage(
    files = listOf("src/main/kotlin/NewFile.kt", "README.md")
)
result.onSuccess { operationResult ->
    println("Files staged: ${operationResult.message}")
}
```

### Unstage Files

Unstage files.

```kotlin
suspend fun unstage(files: List<String>): Result<GitOperationResult>

val result = gitClient.unstage(
    files = listOf("src/main/kotlin/NewFile.kt")
)
result.onSuccess { operationResult ->
    println("Files unstaged: ${operationResult.message}")
}
```

### Discard Changes

Discard uncommitted changes.

```kotlin
suspend fun discard(files: List<String>): Result<GitOperationResult>

val result = gitClient.discard(
    files = listOf("src/main/kotlin/NewFile.kt")
)
result.onSuccess { operationResult ->
    println("Changes discarded: ${operationResult.message}")
}
```

### Get Git Remotes

List configured Git remotes.

```kotlin
suspend fun getRemotes(): Result<List<GitRemote>>

val result = gitClient.getRemotes()
result.onSuccess { remotes ->
    remotes.forEach { remote ->
        // GitRemote fields:
        // - name: String
        // - url: String
        // - pushUrl: String?
        println("${remote.name}: ${remote.url}")
    }
}
```

### Get Git Stashes

List all stashes.

```kotlin
suspend fun getStashes(): Result<List<GitStash>>

val result = gitClient.getStashes()
result.onSuccess { stashes ->
    stashes.forEach { stash ->
        // GitStash fields:
        // - index: Int
        // - message: String
        // - branch: String?
        // - date: Long?

        println("stash@{$index}: ${stash.message}")
    }
}
```

### Create Stash

Create a new stash.

```kotlin
suspend fun createStash(
    message: String? = null,
    includeUntracked: Boolean = false
): Result<GitOperationResult>

val result = gitClient.createStash(
    message = "Work in progress on new feature",
    includeUntracked = false
)
result.onSuccess { operationResult ->
    println("Stash created: ${operationResult.message}")
}
```

### Pop Stash

Apply and remove the most recent stash.

```kotlin
suspend fun popStash(index: Int = 0): Result<GitOperationResult>

val result = gitClient.popStash(index = 0)
result.onSuccess { operationResult ->
    println("Stash popped: ${operationResult.message}")
}
```

### Apply Stash

Apply a stash without removing it.

```kotlin
suspend fun applyStash(index: Int = 0): Result<GitOperationResult>

val result = gitClient.applyStash(index = 0)
result.onSuccess { operationResult ->
    println("Stash applied: ${operationResult.message}")
}
```

### Drop Stash

Remove a stash without applying it.

```kotlin
suspend fun dropStash(index: Int): Result<GitOperationResult>

val result = gitClient.dropStash(index = 0)
result.onSuccess { operationResult ->
    println("Stash dropped: ${operationResult.message}")
}
```

### Merge Branch

Merge a branch into the current branch.

```kotlin
suspend fun merge(
    branch: String,
    noFf: Boolean = false,
    squash: Boolean = false,
    message: String? = null
): Result<GitOperationResult>

val result = gitClient.merge(
    branch = "feature/new-feature",
    noFf = true,  // Disable fast-forward merge
    squash = false,
    message = "Merge feature/new-feature into main"
)
result.onSuccess { operationResult ->
    println("Merge successful: ${operationResult.message}")
}
```

### Abort Merge

Abort an in-progress merge.

```kotlin
suspend fun abortMerge(): Result<GitOperationResult>

val result = gitClient.abortMerge()
result.onSuccess { operationResult ->
    println("Merge aborted: ${operationResult.message}")
}
```

### Rebase Branch

Rebase current branch onto another branch.

```kotlin
suspend fun rebase(onto: String): Result<GitOperationResult>

val result = gitClient.rebase(onto = "main")
result.onSuccess { operationResult ->
    println("Rebase successful: ${operationResult.message}")
}
```

### Rebase Continue

Continue a rebase after resolving conflicts.

```kotlin
suspend fun rebaseContinue(): Result<GitOperationResult>

val result = gitClient.rebaseContinue()
result.onSuccess { operationResult ->
    println("Rebase continued: ${operationResult.message}")
}
```

### Rebase Abort

Abort an in-progress rebase.

```kotlin
suspend fun rebaseAbort(): Result<GitOperationResult>

val result = gitClient.rebaseAbort()
result.onSuccess { operationResult ->
    println("Rebase aborted: ${operationResult.message}")
}
```

### Rebase Skip

Skip the current commit during rebase.

```kotlin
suspend fun rebaseSkip(): Result<GitOperationResult>

val result = gitClient.rebaseSkip()
result.onSuccess { operationResult ->
    println("Commit skipped: ${operationResult.message}")
}
```

### Get Git Tags

List all Git tags.

```kotlin
suspend fun getTags(): Result<List<String>>

val result = gitClient.getTags()
result.onSuccess { tags ->
    tags.forEach { tag ->
        println("Tag: $tag")
    }
}
```

### Create Tag

Create a new Git tag.

```kotlin
suspend fun createTag(
    name: String,
    message: String? = null,
    ref: String? = null,
    annotated: Boolean = false
): Result<GitOperationResult>

val result = gitClient.createTag(
    name = "v1.0.0",
    message = "Release version 1.0.0",
    ref = "main",  // Tag this commit (null = HEAD)
    annotated = true
)
result.onSuccess { operationResult ->
    println("Tag created: ${operationResult.message}")
}
```

### Delete Tag

Delete a Git tag.

```kotlin
suspend fun deleteTag(name: String): Result<GitOperationResult>

val result = gitClient.deleteTag(name = "v1.0.0")
result.onSuccess { operationResult ->
    println("Tag deleted: ${operationResult.message}")
}
```

### Add Git Remote

Add a new remote repository.

```kotlin
suspend fun addRemote(name: String, url: String): Result<GitOperationResult>

val result = gitClient.addRemote(
    name = "upstream",
    url = "https://github.com/user/repo.git"
)
result.onSuccess { operationResult ->
    println("Remote added: ${operationResult.message}")
}
```

### Remove Remote

Delete a Git remote.

```kotlin
suspend fun removeRemote(name: String): Result<GitOperationResult>

val result = gitClient.removeRemote(name = "upstream")
result.onSuccess { operationResult ->
    println("Remote removed: ${operationResult.message}")
}
```

### Update Remote URL

Change the URL of a Git remote.

```kotlin
suspend fun setRemoteUrl(name: String, url: String): Result<GitOperationResult>

val result = gitClient.setRemoteUrl(
    name = "origin",
    url = "https://github.com/new-user/repo.git"
)
result.onSuccess { operationResult ->
    println("Remote URL updated: ${operationResult.message}")
}
```

### Start Git Server

Request OpenCode to start the Git HTTP server on port 4097.

```kotlin
suspend fun requestStartGitServer(): Result<Unit>

val result = gitClient.requestStartGitServer()
result.onSuccess {
    println("Git server start requested. OpenCode will execute start-git-server.ps1")
}
```

### Check Git Server Running

Verify if Git HTTP server is available.

```kotlin
suspend fun isServerRunning(): Boolean

val isRunning = gitClient.isServerRunning()
if (isRunning) {
    println("Git server is running on port 4097")
} else {
    println("Git server is NOT running")
}
```

### Start Git Server and Wait

Request start and poll until server is available.

```kotlin
suspend fun requestStartGitServerAndWait(
    maxWaitMs: Long = 10_000,
    pollIntervalMs: Long = 500
): Result<Boolean>

val result = gitClient.requestStartGitServerAndWait()
result.onSuccess { started ->
    if (started) {
        println("Git server started successfully")
    } else {
        println("Git server failed to start within timeout")
    }
}
```

## Error Handling

### NetworkError Types

All network errors are represented as a sealed class `NetworkError`:

```kotlin
sealed class NetworkError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    class ServerError(val statusCode: Int, message: String) : NetworkError("Server Error $statusCode: $message")
    class ConnectionError(message: String, cause: Throwable? = null) : NetworkError(message, cause)
    class TimeoutError(message: String, cause: Throwable? = null) : NetworkError(message, cause)
    class GitServerUnavailable(message: String, cause: Throwable? = null) : NetworkError(message, cause)
    class Unknown(message: String, cause: Throwable? = null) : NetworkError(message, cause)

    companion object {
        fun from(e: Throwable): NetworkError {
            return when (e) {
                is ServerResponseException -> ServerError(e.response.status.value, e.message)
                is HttpRequestTimeoutException,
                is ConnectTimeoutException,
                is SocketTimeoutException -> TimeoutError("Connection timed out", e)
                is ConnectException,
                is SocketException,
                is UnknownHostException -> ConnectionError("Connection failed: ${e.message}", e)
                is IOException -> ConnectionError("Network error: ${e.message}", e)
                else -> Unknown(e.message ?: "Unknown error", e)
            }
        }
    }
}
```

### Error Handling Pattern

Always use `Result<T>` pattern for error handling:

```kotlin
val result = apiClient.someMethod()
result.onSuccess { data ->
    // Handle successful response
}.onFailure { error ->
    when (error) {
        is NetworkError.ServerError -> {
            println("Server error (${error.statusCode}): ${error.message}")
        }
        is NetworkError.ConnectionError -> {
            println("Connection error: ${error.message}")
        }
        is NetworkError.TimeoutError -> {
            println("Timeout: ${error.message}")
        }
        is NetworkError.GitServerUnavailable -> {
            println("Git server unavailable: ${error.message}")
        }
        else -> {
            println("Unknown error: ${error.message}")
        }
    }
}
```

## Retry Policy

### Retry Policy Configuration

Control retry behavior with `RetryPolicy`:

```kotlin
data class RetryPolicy(
    val maxRetries: Int = 3,           // Maximum retry attempts
    val initialDelayMs: Long = 1000,    // Initial delay before first retry
    val maxDelayMs: Long = 30000,      // Maximum delay between retries
    val factor: Double = 2.0,           // Exponential backoff multiplier
    val jitterPercent: Double = 0.1      // Random jitter percentage
) {
    companion object {
        val Default = RetryPolicy()                      // 3 retries, 1s initial, 30s max
        val Aggressive = RetryPolicy(maxRetries = 5, initialDelayMs = 500)  // Faster retries
        val Relaxed = RetryPolicy(maxRetries = 2, initialDelayMs = 2000)   // Fewer retries
        val None = RetryPolicy(maxRetries = 0)    // No retries
    }
}
```

### Retryable Exceptions

The SDK automatically retries on these exceptions:

- `HttpRequestTimeoutException`
- `ConnectTimeoutException`
- `SocketTimeoutException`
- `ServerResponseException` (5xx errors only)
- `ConnectException`
- `SocketException`
- `UnknownHostException`
- `IOException`

Non-retryable exceptions (fail immediately):

- 4xx HTTP errors (client errors)
- `CancellationException`
- `CircuitBreakerOpenException`

### Retry Strategy

- **Read operations (GET)**: Use `safeCall()` with retry enabled
- **Write operations (POST/DELETE)**: Use `safeCallNoRetry()` to prevent duplicate side effects

## Environment Detection

### Android Emulator vs Physical Device

The SDK automatically detects the runtime environment:

```kotlin
// ServerConfigRepository provides environment-aware defaults
fun createDefaultConfig(): ServerConfig {
    val defaultHost = getPlatformDefaultHost()

    return if (defaultHost.isNotEmpty()) {
        // Android Emulator detected
        ServerConfig(
            id = "default",
            name = "Local Server",
            baseUrl = "http://$defaultHost:4096",  // 10.0.2.2 on emulator
            connectionType = ConnectionType.LOCAL,
            authType = AuthType.NONE,
            authToken = null,
            isActive = true
        )
    } else {
        // Physical device
        ServerConfig(
            id = "tailscale-default",
            name = "Tailscale Server",
            baseUrl = "https://your-tailscale-host.ts.net",
            connectionType = ConnectionType.TAILSCALE,
            authType = AuthType.NONE,
            authToken = null,
            isActive = true
        )
    }
}
```

### ADB Reverse Port Forwarding (Emulator)

For Android emulator Git operations, ADB reverse port forwarding is **required**:

```bash
# Set up ADB reverse before launching app
adb reverse tcp:4097 tcp:4097

# Verify setup
adb reverse --list
# Expected: tcp:4097 tcp:4097
```

Without this setup, all Git operations will fail with "Git server is not running" because the emulator cannot reach port 4097 on the host.

### Tailscale Integration

For physical devices connecting over Tailscale:

```kotlin
val tailscaleConfig = ServerConfig(
    id = "tailscale-default",
    name = "Tailscale Server",
    baseUrl = "https://your-tailscale-host.ts.net",
    connectionType = ConnectionType.TAILSCALE,
    authType = AuthType.BEARER,
    authToken = "your-api-token",
    isActive = true
)
```

## HttpClient Lifecycle

### Dynamic Client Recreation

`HttpClientProvider` automatically recreates the Ktor client when server configuration changes:

```kotlin
val provider = HttpClientProvider(
    serverConfigRepository = repo,
    networkObserver = observer
)

// Subscribe to server config changes (automatic)
// When config changes, old client is gracefully closed after 15s
```

### Token Refresh

Automatic token refresh support:

```kotlin
// Set refresh callback
provider.onTokenRefresh = suspend {
    // Refresh token logic
    val newToken = refreshTokenFromAuthServer()
    if (newToken != null) {
        // Save new token and force client recreation
        return newToken
    }
    return null  // Refresh failed
}

// When 401 received, trigger refresh
if (error is NetworkError.ServerError && error.statusCode == 401) {
    val refreshed = provider.refreshToken()
    if (!refreshed) {
        // Show login screen
    }
}
```

### Connection Quality Monitoring

Track request performance and connection health:

```kotlin
// Observe connection metrics
provider.connectionMetrics.collect { metrics ->
    when (metrics.quality) {
        ConnectionQuality.EXCELLENT -> {
            println("Excellent connection: ${metrics.averageLatencyMs}ms")
        }
        ConnectionQuality.GOOD -> {
            println("Good connection: ${metrics.averageLatencyMs}ms")
        }
        ConnectionQuality.POOR -> {
            println("Poor connection: ${metrics.averageLatencyMs}ms, ${metrics.successRate * 100}%")
        }
        ConnectionQuality.OFFLINE -> {
            println("Connection offline")
        }
        ConnectionQuality.UNKNOWN -> {
            println("Connection status unknown")
        }
    }
}
```

### Request Deduplication

Prevent duplicate in-flight requests:

```kotlin
val result = provider.withDeduplication("getSession:$sessionId") {
    // If this request is already in flight, wait for its result
    // Otherwise, execute the request
    apiClient.getSession(sessionId)
}
```

## Circuit Breaker

### Circuit Breaker Usage

Protect against cascading failures:

```kotlin
val circuitBreaker = CircuitBreaker(
    name = "OpenCodeAPI",
    config = CircuitBreakerConfig.Default  // or Aggressive, Relaxed
)

val result = circuitBreaker.execute {
    apiClient.someMethod()
}

result.onSuccess { data ->
    // Request succeeded
}.onFailure { error ->
    when (error) {
        is CircuitBreakerOpenException -> {
            println("Circuit breaker is OPEN - failing fast")
        }
        else -> {
            println("Request failed: ${error.message}")
        }
    }
}
```

### Circuit States

- **CLOSED**: Normal operation, requests pass through
- **OPEN**: Circuit tripped, fast-fail (no network requests)
- **HALF_OPEN**: Testing recovery, allows limited requests

### Circuit Configuration

```kotlin
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,              // Failures to trip circuit
    val resetTimeoutMs: Long = 30_000,          // Time before recovery attempt
    val successThreshold: Int = 2,              // Successes to close circuit
    val halfOpenMaxRequests: Int = 3            // Max requests in half-open
)
```

## Git Server Health Check

### GitServerChecker

Utility for fast Git server availability detection (500ms-1s timeout):

```kotlin
// Check server availability (quick check)
val result = GitServerChecker.checkServerRunning(
    httpClient = httpClientProvider.getClientSync(),
    url = "http://127.0.0.1:4097"
)

val (isRunning, message, responseTime) = result

// GitServerChecker helpers
GitServerChecker.isServerAvailable(result)      // Check if server is running
GitServerChecker.isServerUnavailable(result)    // Check if server not running
GitServerChecker.isServerSlow(result)          // Check if server > 1s response time
GitServerChecker.getStatusMessage(result)       // Get human-readable message
```

### Heartbeat Monitoring

Continuous health monitoring with callbacks:

```kotlin
// Start heartbeat (polls every 30s by default)
GitServerChecker.startHeartbeat(
    httpClient = httpClient,
    serverUrl = "http://127.0.0.1:4097",
    intervalMs = 30_000  // Custom interval
)

// Set callbacks for state changes
GitServerChecker.onServerUnhealthy = { healthState ->
    if (healthState.consecutiveFailures >= 3) {
        println("Git server unhealthy after 3 consecutive failures")
        // Show recovery dialog to user
    }
}

GitServerChecker.onServerRecovered = { healthState ->
    println("Git server recovered!")
}

// Get current health state (without check)
val currentHealth = GitServerChecker.getCurrentHealth()

// Check if heartbeat is running
val isRunning = GitServerChecker.isHeartbeatRunning()
```

### Health States

- **HEALTHY**: Server running and responding normally (< 1s)
- **DEGRADED**: Server running but slowly (> 1s response time)
- **UNHEALTHY**: Server not responding (timeout or connection refused)
- **UNKNOWN**: Status not yet checked

## Timeouts

All network requests use a 120-second timeout for long-running operations:

```kotlin
// Default timeouts (configured in HttpClientProvider)
requestTimeoutMillis = 120_000   // 2 minutes
connectTimeoutMillis = 10_000    // 10 seconds
socketTimeoutMillis = 120_000    // 2 minutes

// Git diff operations use 60s timeout
// Quick health checks use 500ms-1s timeout
```

## Best Practices

### 1. Use Kotlin Coroutines

All SDK methods are `suspend` functions designed for coroutine-based async code:

```kotlin
// Good - In coroutine scope
lifecycleScope.launch {
    val result = apiClient.listSessions()
    result.onSuccess { sessions ->
        updateUI(sessions)
    }
}

// Bad - Blocking main thread
val sessions = apiClient.listSessions()  // Will crash on Android!
```

### 2. Handle All Result States

Never ignore failures:

```kotlin
val result = apiClient.someMethod()
result.fold(
    onSuccess = { data ->
        // Handle success
    },
    onFailure = { error ->
        // Handle error (don't ignore!)
        showError(error.message)
    }
)
```

### 3. Use Chat Async for Non-Blocking UX

For chat messages that should not block UI:

```kotlin
// Use chatAsync for immediate return
val result = apiClient.chatAsync(
    sessionId = sessionId,
    modelId = modelId,
    providerId = providerId,
    parts = parts
)
result.onSuccess {
    // Listen to SSE stream for response
    // This prevents UI freezing
}
```

### 4. Refresh Git Status After Operations

After Git operations, refresh status:

```kotlin
// Good
gitClient.commit(message = "Fix bug")
val status = gitClient.getStatus()
status.onSuccess { gitStatus ->
    updateUIWithStatus(gitStatus)
}

// Bad - Assuming status didn't change
gitClient.commit(message = "Fix bug")
// Need explicit status fetch
```

### 5. Use Absolute Paths

Never use relative paths for file operations:

```kotlin
// Good
val absolutePath = "/data/data/com.mocca.app/files/project/src/main.kt"
apiClient.getFileContent(absolutePath)

// Bad - Will fail
val relativePath = "src/main.kt"
apiClient.getFileContent(relativePath)
```

### 6. Monitor Connection Quality

Use connection metrics to adapt UI behavior:

```kotlin
httpClientProvider.connectionMetrics.collect { metrics ->
    when (metrics.quality) {
        ConnectionQuality.POOR -> {
            // Reduce polling frequency
            // Show offline warning
            // Disable auto-refresh
        }
        ConnectionQuality.EXCELLENT -> {
            // Enable aggressive polling
            // Show stable connection indicator
        }
    }
}
```

## API Response Models

### Session

```kotlin
data class Session(
    val id: String,
    val title: String?,
    val slug: String?,
    val time: SessionTime?,
    val status: SessionStatus,
    val version: String?,
    val projectID: String?,
    val directory: String?,
    val parentID: String?,
    val parentId: String?,
    val summary: SessionSummary?,
    val permission: List<SessionPermission>?,
    val revert: SessionRevertInfo?,
    val shareID: String?,
    val lastFetchedAt: Long?
)
```

### Message

```kotlin
data class MessageResponse(
    val info: MessageInfo,
    val parts: List<MessagePartResponse>
)

data class MessageInfo(
    val id: String,
    val role: MessageRole,
    val sessionID: String,
    val time: MessageTime?,
    val summary: JsonElement?,
    val agent: String?,
    val model: ModelInfo?,
    val variant: String?,
    val tools: Map<String, Boolean>?,
    val cost: Double?,
    val tokens: TokenUsage?,
    val system: List<String>?,
    val error: JsonElement?
)
```

### File

```kotlin
data class FileInfo(
    val name: String,
    val path: String,
    val type: String,  // "file" or "directory"
    val size: Long?,
    val modifiedAt: Long?
)

data class FileContent(
    val path: String,
    val content: String,
    val language: String?
)
```

### Search

```kotlin
data class SearchResult(
    val file: String,
    val line: Int,
    val column: Int,
    val match: String,
    val context: String
)

data class SymbolResult(
    val name: String,
    val kind: String,
    val file: String,
    val line: Int
)
```

### Git

```kotlin
data class GitStatusResponse(
    val branch: String,
    val upstream: String?,
    val ahead: Int,
    val behind: Int,
    val staged: List<GitFileChange>,
    val unstaged: List<GitFileChange>,
    val untracked: List<String>,
    val conflicted: List<String>,
    val stashes: Int,
    val clean: Boolean
)

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val message: String,
    val author: String,
    val email: String?,
    val date: Long,
    val parents: List<String>,
    val refs: List<String>
)

data class GitBranch(
    val name: String,
    val current: Boolean,
    val remote: Boolean,
    val upstream: String?,
    val ahead: Int,
    val behind: Int,
    val lastCommit: String?,
    val lastCommitTime: Long?
)
```

## Deprecated APIs

### getAppInfo()

**Status**: DEPRECATED (DeprecationLevel.ERROR)

**Reason**: The `/app` endpoint returns HTML, not JSON. It is a frontend route, not a REST API endpoint.

**Replacement**: Use `getHealth()` instead.

### getTools()

**Status**: DEPRECATED (DeprecationLevel.ERROR)

**Reason**: The `/tool` endpoint returns HTML, not JSON. It is a frontend route, not a REST API endpoint.

**Replacement**: Use `getToolIds()` which calls `/experimental/tool/ids`.

## Port Summary

| Service | Port | Purpose |
|----------|-------|---------|
| OpenCode API | 4096 | Primary AI agent, sessions, configuration |
| Git Server | 4097 | Git operations via Smart HTTP Protocol |
| SSE | 4096 | Real-time events (uses same port as API) |
| WebSocket | 4096 (converted) | Terminal bidirectional communication |

## Version Information

- **SDK Version**: 1.0.0
- **Supported OpenCode Version**: Current
- **Last Updated**: 2026-01-21
- **Dependencies**: Ktor 2.3.7, Kotlinx Serialization 1.6.2, Napier 2.6.1

## License

This SDK is part of MOCCA (Mobile OpenCode Companion App).

---

**For more information, see the MOCCA project documentation:**
- Architecture: `AGENTS.md`
- Network Layer: `api/AGENTS.md`
- Domain Models: `domain/model/AGENTS.md`
- Data Layer: `data/repository/AGENTS.md`
- API Analysis: `OPENCODE_API_ANALYSIS.md`
