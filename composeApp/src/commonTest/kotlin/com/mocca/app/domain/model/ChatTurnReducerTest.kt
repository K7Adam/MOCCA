package com.mocca.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatTurnReducerTest {

    @Test
    fun reducerAppliesOpenCodeTextAndReasoningDeltasByPartId() {
        val messageInfo = AssistantMessageInfo(
            id = "msg_1",
            role = "assistant",
            sessionID = "ses_1",
            time = MessageTimeInfo(created = 100)
        )

        val withMessage = ChatTurnReducer.reduce(
            ChatTurnState(),
            ServerEvent.MessageUpdated(properties = MessageUpdatedProperties(info = messageInfo))
        )
        val withText = ChatTurnReducer.reduce(
            withMessage,
            ServerEvent.MessagePartUpdated(
                properties = MessagePartUpdatedProperties(
                    part = MessagePartInfo(
                        id = "prt_text",
                        type = "text",
                        messageID = "msg_1",
                        sessionID = "ses_1",
                        text = "Hello"
                    )
                )
            )
        )
        val withReasoning = ChatTurnReducer.reduce(
            withText,
            ServerEvent.MessagePartUpdated(
                properties = MessagePartUpdatedProperties(
                    part = MessagePartInfo(
                        id = "prt_reasoning",
                        type = "reasoning",
                        messageID = "msg_1",
                        sessionID = "ses_1",
                        text = "Plan"
                    )
                )
            )
        )

        val reduced = listOf(
            ServerEvent.MessagePartDelta(
                properties = MessagePartDeltaProperties(
                    sessionID = "ses_1",
                    messageID = "msg_1",
                    partID = "prt_text",
                    field = "text",
                    delta = " world"
                )
            ),
            ServerEvent.MessagePartDelta(
                properties = MessagePartDeltaProperties(
                    sessionID = "ses_1",
                    messageID = "msg_1",
                    partID = "prt_reasoning",
                    field = "text",
                    delta = " more"
                )
            )
        ).fold(withReasoning, ChatTurnReducer::reduce)

        val message = reduced.messagesById.getValue("msg_1")
        val text = assertIs<MessagePart.Text>(message.parts[0])
        val reasoning = assertIs<MessagePart.Reasoning>(message.parts[1])

        assertEquals("Hello world", text.text)
        assertEquals("Plan more", reasoning.content)
        assertEquals("reasoning", reduced.sessionActivities.getValue("ses_1").stage)
    }

    @Test
    fun reducerQueuesPermissionsAndQuestionsPerSession() {
        val permission = ServerEvent.PermissionAsked(
            properties = PermissionAskedProperties(
                id = "per_1",
                sessionID = "ses_1",
                permission = "edit",
                patterns = listOf("src/**"),
                tool = PermissionToolInfo(messageId = "msg_1", callId = "call_1")
            )
        )
        val question = ServerEvent.QuestionAsked(
            properties = QuestionAskedProperties(
                id = "que_1",
                sessionID = "ses_1",
                questions = listOf(QuestionInfo(question = "Proceed?", header = "Confirm"))
            )
        )

        val state = listOf(permission, question).fold(ChatTurnState(), ChatTurnReducer::reduce)

        assertEquals(listOf("per_1"), state.pendingPermissionsBySession.getValue("ses_1").map { it.id })
        assertEquals(listOf("que_1"), state.pendingQuestionsBySession.getValue("ses_1").map { it.id })

        val replied = ChatTurnReducer.reduce(
            state,
            ServerEvent.PermissionReplied(
                properties = PermissionRepliedProperties(
                    sessionID = "ses_1",
                    requestID = "per_1",
                    reply = "once"
                )
            )
        )

        assertTrue(replied.pendingPermissionsBySession["ses_1"].orEmpty().isEmpty())
        assertEquals(listOf("que_1"), replied.pendingQuestionsBySession.getValue("ses_1").map { it.id })

        val questionReplied = ChatTurnReducer.reduce(
            replied,
            ServerEvent.QuestionReplied(
                properties = QuestionRepliedProperties(
                    sessionID = "ses_1",
                    requestID = "que_1",
                    answers = listOf(listOf("Proceed"))
                )
            )
        )

        assertTrue(questionReplied.pendingQuestionsBySession["ses_1"].orEmpty().isEmpty())
    }

    @Test
    fun messageResponseMapsOpenCodeReasoningAndStepFinishTokens() {
        val message = Message.fromResponse(
            MessageResponse(
                info = MessageInfo(
                    id = "msg_1",
                    role = MessageRole.ASSISTANT,
                    sessionID = "ses_1",
                    time = MessageTime(created = 10)
                ),
                parts = listOf(
                    MessagePartResponse(
                        id = "prt_reasoning",
                        sessionID = "ses_1",
                        messageID = "msg_1",
                        type = "reasoning",
                        text = "I should inspect the repo first",
                        time = MessagePartTime(start = 10, end = 25)
                    ),
                    MessagePartResponse(
                        id = "prt_finish",
                        sessionID = "ses_1",
                        messageID = "msg_1",
                        type = "step-finish",
                        reason = "stop",
                        cost = 0.42,
                        tokens = TokenUsage(
                            input = 100,
                            output = 25,
                            reasoning = 60,
                            cache = CacheUsage(read = 9, write = 3)
                        )
                    )
                )
            )
        )

        val reasoning = assertIs<MessagePart.Reasoning>(message.parts.single())
        assertEquals("I should inspect the repo first", reasoning.content)
        assertEquals(15, reasoning.timeMs)
        assertEquals(0.42, message.cost)
        assertEquals(60, message.tokens?.reasoning)
        assertEquals(9, message.tokens?.cache?.read)
    }

    @Test
    fun reducerKeepsStepMarkersOutOfVisiblePartsButAppliesStepFinishUsage() {
        val withMessage = ChatTurnReducer.reduce(
            ChatTurnState(),
            ServerEvent.MessageUpdated(
                properties = MessageUpdatedProperties(
                    info = AssistantMessageInfo(
                        id = "msg_1",
                        role = "assistant",
                        sessionID = "ses_1",
                        time = MessageTimeInfo(created = 10)
                    )
                )
            )
        )

        val withStepStart = ChatTurnReducer.reduce(
            withMessage,
            ServerEvent.MessagePartUpdated(
                properties = MessagePartUpdatedProperties(
                    part = MessagePartInfo(
                        id = "prt_start",
                        type = "step-start",
                        messageID = "msg_1",
                        sessionID = "ses_1"
                    )
                )
            )
        )
        val withText = ChatTurnReducer.reduce(
            withStepStart,
            ServerEvent.MessagePartUpdated(
                properties = MessagePartUpdatedProperties(
                    part = MessagePartInfo(
                        id = "prt_text",
                        type = "text",
                        messageID = "msg_1",
                        sessionID = "ses_1",
                        text = "Done"
                    )
                )
            )
        )
        val finished = ChatTurnReducer.reduce(
            withText,
            ServerEvent.MessagePartUpdated(
                properties = MessagePartUpdatedProperties(
                    part = MessagePartInfo(
                        id = "prt_finish",
                        type = "step-finish",
                        messageID = "msg_1",
                        sessionID = "ses_1",
                        cost = 0.01,
                        tokens = TokenInfo(input = 3, output = 2, reasoning = 1)
                    )
                )
            )
        )

        val message = finished.messagesById.getValue("msg_1")
        assertEquals(1, message.parts.size)
        assertIs<MessagePart.Text>(message.parts.single())
        assertEquals(0.01, message.cost)
        assertEquals(1, message.tokens?.reasoning)
        assertEquals(false, message.isStreaming)
    }

    @Test
    fun tokenUsageIsVisibleForReasoningOrCacheOnlyUsage() {
        assertTrue(TokenUsage(reasoning = 1).hasVisibleDetails)
        assertTrue(TokenUsage(cache = CacheUsage(read = 1)).hasVisibleDetails)
        assertTrue(TokenUsage(cache = CacheUsage(write = 1)).hasVisibleDetails)
    }

    @Test
    fun sessionIdleMarksLatestAssistantMessageAsNotStreaming() {
        val withMessage = ChatTurnReducer.reduce(
            ChatTurnState(),
            ServerEvent.MessageUpdated(
                properties = MessageUpdatedProperties(
                    info = AssistantMessageInfo(
                        id = "msg_1",
                        role = "assistant",
                        sessionID = "ses_1",
                        time = MessageTimeInfo(created = 10)
                    )
                )
            )
        )
        val withText = ChatTurnReducer.reduce(
            withMessage,
            ServerEvent.MessagePartUpdated(
                properties = MessagePartUpdatedProperties(
                    part = MessagePartInfo(
                        id = "prt_text",
                        type = "text",
                        messageID = "msg_1",
                        sessionID = "ses_1",
                        text = "Hello"
                    )
                )
            )
        )

        val idle = ChatTurnReducer.reduce(
            withText,
            ServerEvent.SessionIdle(properties = SessionIdleProperties(sessionID = "ses_1"))
        )

        val message = idle.messagesById.getValue("msg_1")
        assertEquals(false, message.isStreaming)
        assertEquals(AgentActivity.STAGE_IDLE, idle.sessionActivities.getValue("ses_1").stage)
    }

    @Test
    fun sessionErrorMarksLatestAssistantMessageAsNotStreaming() {
        val withMessage = ChatTurnReducer.reduce(
            ChatTurnState(),
            ServerEvent.MessageUpdated(
                properties = MessageUpdatedProperties(
                    info = AssistantMessageInfo(
                        id = "msg_1",
                        role = "assistant",
                        sessionID = "ses_1",
                        time = MessageTimeInfo(created = 10)
                    )
                )
            )
        )
        val withText = ChatTurnReducer.reduce(
            withMessage,
            ServerEvent.MessagePartUpdated(
                properties = MessagePartUpdatedProperties(
                    part = MessagePartInfo(
                        id = "prt_text",
                        type = "text",
                        messageID = "msg_1",
                        sessionID = "ses_1",
                        text = "Hello"
                    )
                )
            )
        )

        val error = ChatTurnReducer.reduce(
            withText,
            ServerEvent.SessionError(
                properties = SessionErrorProperties(
                    sessionID = "ses_1",
                    error = ErrorInfo(message = "boom")
                )
            )
        )

        val message = error.messagesById.getValue("msg_1")
        assertEquals(false, message.isStreaming)
        assertEquals(AgentActivity.STAGE_ERROR, error.sessionActivities.getValue("ses_1").stage)
    }
}
