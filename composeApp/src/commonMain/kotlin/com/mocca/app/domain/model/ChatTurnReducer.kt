package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChatTurnState(
    val messagesById: Map<String, Message> = emptyMap(),
    val sessionActivities: Map<String, AgentActivity> = emptyMap(),
    val pendingPermissionsBySession: Map<String, List<PermissionRequest>> = emptyMap(),
    val pendingQuestionsBySession: Map<String, List<QuestionRequest>> = emptyMap()
) {
    fun messagesForSession(sessionId: String): List<Message> =
        messagesById.values
            .filter { it.sessionId == sessionId }
            .sortedBy { it.createdAt }

    fun latestAssistantMessage(sessionId: String): Message? =
        messagesForSession(sessionId)
            .lastOrNull { it.role == MessageRole.ASSISTANT }
}

@Immutable
data class AgentActivity(
    val sessionId: String,
    val stage: String = STAGE_IDLE,
    val messageId: String? = null,
    val partId: String? = null,
    val title: String? = null
) {
    companion object {
        const val STAGE_IDLE = "idle"
        const val STAGE_QUEUED = "queued"
        const val STAGE_REASONING = "reasoning"
        const val STAGE_RUNNING = "running"
        const val STAGE_WRITING = "writing"
        const val STAGE_TOOL = "tool"
        const val STAGE_ERROR = "error"
    }
}

object ChatTurnReducer {
    fun reduce(state: ChatTurnState, event: ServerEvent): ChatTurnState = when (event) {
        is ServerEvent.MessageUpdated -> state.upsertMessageInfo(event.properties.info)
        is ServerEvent.MessagePartUpdated -> state.upsertMessagePart(event.properties.part)
        is ServerEvent.MessagePartDelta -> state.applyPartDelta(event.properties)
        is ServerEvent.SessionUpdated -> state.withActivity(
            sessionId = event.properties.info.id,
            stage = if (event.properties.info.status == SessionStatus.RUNNING) {
                AgentActivity.STAGE_RUNNING
            } else {
                AgentActivity.STAGE_IDLE
            }
        )
        is ServerEvent.SessionIdle -> state.withActivity(
            sessionId = event.properties.sessionID,
            stage = AgentActivity.STAGE_IDLE
        )
        is ServerEvent.SessionError -> event.properties.sessionID?.let { sessionId ->
            state.withActivity(sessionId = sessionId, stage = AgentActivity.STAGE_ERROR)
        } ?: state
        is ServerEvent.PermissionAsked -> state.upsertPermission(PermissionRequest.fromEvent(event))
        is ServerEvent.PermissionUpdated -> state.upsertPermission(PermissionRequest.fromLegacyEvent(event))
        is ServerEvent.PermissionReplied -> state.removePermission(
            sessionId = event.properties.sessionID,
            requestId = event.properties.requestID
        )
        is ServerEvent.QuestionAsked -> state.upsertQuestion(QuestionRequest.fromEvent(event))
        is ServerEvent.QuestionReplied -> state.removeQuestion(
            sessionId = event.properties.sessionID,
            requestId = event.properties.requestID
        )
        is ServerEvent.QuestionRejected -> state.removeQuestion(
            sessionId = event.properties.sessionID,
            requestId = event.properties.requestID
        )
        else -> state
    }

    private fun ChatTurnState.upsertMessageInfo(info: AssistantMessageInfo): ChatTurnState {
        val existing = messagesById[info.id]
        val role = when (info.role.lowercase()) {
            "user" -> MessageRole.USER
            "system" -> MessageRole.SYSTEM
            else -> MessageRole.ASSISTANT
        }
        val message = Message(
            id = info.id,
            sessionId = info.sessionID,
            role = role,
            parts = existing?.parts.orEmpty(),
            createdAt = info.time?.created ?: existing?.createdAt ?: 0L,
            model = info.modelID ?: existing?.model,
            cost = info.cost ?: existing?.cost,
            tokens = info.tokens?.toTokenUsage() ?: existing?.tokens,
            isRead = existing?.isRead ?: true,
            metadata = existing?.metadata,
            isStreaming = info.time?.completed == null
        )
        return copy(messagesById = messagesById + (message.id to message))
            .withActivity(info.sessionID, AgentActivity.STAGE_RUNNING, messageId = info.id)
    }

    private fun ChatTurnState.upsertMessagePart(part: MessagePartInfo): ChatTurnState {
        val existing = messagesById[part.messageID]
        val message = existing ?: Message(
            id = part.messageID,
            sessionId = part.sessionID,
            role = MessageRole.ASSISTANT,
            parts = emptyList(),
            createdAt = part.time?.start ?: 0L,
            isStreaming = true
        )
        val mappedPart = part.toMessagePart() ?: return this
        val updatedMessage = message.copy(
            parts = message.parts.replaceById(part.id, mappedPart),
            isStreaming = true
        )
        return copy(messagesById = messagesById + (part.messageID to updatedMessage))
            .withActivity(
                sessionId = part.sessionID,
                stage = part.activityStage(),
                messageId = part.messageID,
                partId = part.id,
                title = part.state?.title ?: part.tool
            )
    }

    private fun ChatTurnState.applyPartDelta(delta: MessagePartDeltaProperties): ChatTurnState {
        if (delta.field != "text") return this
        val message = messagesById[delta.messageID] ?: return this
        var matched = false
        val updatedParts = message.parts.map { part ->
            when {
                part is MessagePart.Text && part.id == delta.partID -> {
                    matched = true
                    part.copy(text = part.text + delta.delta)
                }
                part is MessagePart.Reasoning && part.id == delta.partID -> {
                    matched = true
                    part.copy(content = part.content + delta.delta)
                }
                part is MessagePart.Thinking && part.id == delta.partID -> {
                    matched = true
                    part.copy(content = part.content + delta.delta)
                }
                else -> part
            }
        }
        if (!matched) return this
        val updatedMessage = message.copy(parts = updatedParts, isStreaming = true)
        return copy(messagesById = messagesById + (message.id to updatedMessage))
            .withActivity(
                sessionId = delta.sessionID,
                stage = updatedParts.firstOrNull { it.partId == delta.partID }.activityStageForDelta(),
                messageId = delta.messageID,
                partId = delta.partID
            )
    }

    private fun ChatTurnState.upsertPermission(permission: PermissionRequest): ChatTurnState {
        val current = pendingPermissionsBySession[permission.sessionId].orEmpty()
        return copy(
            pendingPermissionsBySession = pendingPermissionsBySession + (
                permission.sessionId to (current.filterNot { it.id == permission.id } + permission)
                )
        )
    }

    private fun ChatTurnState.removePermission(sessionId: String, requestId: String): ChatTurnState {
        val current = pendingPermissionsBySession[sessionId].orEmpty()
        return copy(
            pendingPermissionsBySession = pendingPermissionsBySession + (
                sessionId to current.filterNot { it.id == requestId }
                )
        )
    }

    private fun ChatTurnState.upsertQuestion(question: QuestionRequest): ChatTurnState {
        val current = pendingQuestionsBySession[question.sessionId].orEmpty()
        return copy(
            pendingQuestionsBySession = pendingQuestionsBySession + (
                question.sessionId to (current.filterNot { it.id == question.id } + question)
                )
        )
    }

    private fun ChatTurnState.removeQuestion(sessionId: String, requestId: String): ChatTurnState {
        val current = pendingQuestionsBySession[sessionId].orEmpty()
        return copy(
            pendingQuestionsBySession = pendingQuestionsBySession + (
                sessionId to current.filterNot { it.id == requestId }
                )
        )
    }

    private fun ChatTurnState.withActivity(
        sessionId: String,
        stage: String,
        messageId: String? = null,
        partId: String? = null,
        title: String? = null
    ): ChatTurnState = copy(
        sessionActivities = sessionActivities + (
            sessionId to AgentActivity(
                sessionId = sessionId,
                stage = stage,
                messageId = messageId,
                partId = partId,
                title = title
            )
            )
    )
}

private fun TokenInfo.toTokenUsage(): TokenUsage = TokenUsage(
    input = input,
    output = output,
    reasoning = reasoning,
    cache = cache?.let { CacheUsage(read = it.read, write = it.write) }
)

private fun MessagePartInfo.toMessagePart(): MessagePart? = when (type) {
    "text" -> MessagePart.Text(id = id, text = text.orEmpty())
    "reasoning" -> MessagePart.Reasoning(
        content = text.orEmpty(),
        timeMs = time?.durationMs() ?: 0L,
        id = id
    )
    "thinking" -> MessagePart.Thinking(
        content = text.orEmpty(),
        durationMs = time?.durationMs(),
        id = id
    )
    "tool" -> {
        val richState = state?.toRichToolState()
        MessagePart.ToolInvocation(
            id = callID ?: id,
            name = tool ?: "unknown",
            input = state?.input?.toString().orEmpty(),
            state = richState?.status ?: ToolState.PENDING,
            richState = richState ?: RichToolState.Pending(),
            output = state?.output,
            error = state?.error,
            title = state?.title
        )
    }
    "file" -> MessagePart.File(
        mediaType = mime ?: "application/octet-stream",
        url = url,
        filename = filename
    )
    else -> null
}

private fun MessagePartInfo.activityStage(): String = when (type) {
    "tool" -> AgentActivity.STAGE_TOOL
    "reasoning", "thinking" -> AgentActivity.STAGE_REASONING
    "text" -> AgentActivity.STAGE_WRITING
    else -> AgentActivity.STAGE_RUNNING
}

private fun MessagePart?.activityStageForDelta(): String = when (this) {
    is MessagePart.Reasoning, is MessagePart.Thinking -> AgentActivity.STAGE_REASONING
    is MessagePart.Text -> AgentActivity.STAGE_WRITING
    is MessagePart.ToolInvocation -> AgentActivity.STAGE_TOOL
    else -> AgentActivity.STAGE_RUNNING
}

private fun MessagePartTime.durationMs(): Long? =
    end?.let { finishedAt -> start.takeIf { it > 0 }?.let { startedAt -> finishedAt - startedAt } }

private fun List<MessagePart>.replaceById(partId: String, replacement: MessagePart): List<MessagePart> {
    var replaced = false
    val updated = map { part ->
        if (part.partId == partId) {
            replaced = true
            replacement
        } else {
            part
        }
    }
    return if (replaced) updated else updated + replacement
}

private val MessagePart.partId: String?
    get() = when (this) {
        is MessagePart.Text -> id
        is MessagePart.Reasoning -> id
        is MessagePart.Thinking -> id
        is MessagePart.ToolInvocation -> id
        is MessagePart.ToolResult -> id
        is MessagePart.File -> null
        is MessagePart.SubTask -> sessionId
    }
