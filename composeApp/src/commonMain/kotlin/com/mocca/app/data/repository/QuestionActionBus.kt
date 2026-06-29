package com.mocca.app.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shared event bus for question actions from notifications.
 *
 * This allows the Android BroadcastReceiver to communicate with the
 * shared Kotlin repository layer without direct coupling.
 *
 * Architecture:
 * - Android Notification -> QuestionActionReceiver -> QuestionActionBus -> replyToQuestion
 *
 * Uses the V2 question API: `POST /session/:id/question/:questionID/reply`
 */
object QuestionActionBus {

    /**
     * Represents a question reply action from notification.
     */
    data class QuestionReplyAction(
        val questionId: String,
        val sessionId: String,
        val answers: List<List<String>>
    )

    /**
     * Represents a question reject action from notification.
     */
    data class QuestionRejectAction(
        val questionId: String,
        val sessionId: String
    )

    private val _replyActions = MutableSharedFlow<QuestionReplyAction>(extraBufferCapacity = 16)
    val replyActions: SharedFlow<QuestionReplyAction> = _replyActions.asSharedFlow()

    private val _rejectActions = MutableSharedFlow<QuestionRejectAction>(extraBufferCapacity = 16)
    val rejectActions: SharedFlow<QuestionRejectAction> = _rejectActions.asSharedFlow()

    /**
     * Emit a question reply action from notification.
     */
    suspend fun emitReply(questionId: String, sessionId: String, answers: List<List<String>>) {
        _replyActions.emit(QuestionReplyAction(questionId, sessionId, answers))
    }

    /**
     * Try to emit a reply without suspension (for use from Android components)
     */
    fun tryEmitReply(questionId: String, sessionId: String, answers: List<List<String>>): Boolean {
        return _replyActions.tryEmit(QuestionReplyAction(questionId, sessionId, answers))
    }

    /**
     * Emit a question reject action from notification.
     */
    suspend fun emitReject(questionId: String, sessionId: String) {
        _rejectActions.emit(QuestionRejectAction(questionId, sessionId))
    }

    /**
     * Try to emit a reject without suspension (for use from Android components)
     */
    fun tryEmitReject(questionId: String, sessionId: String): Boolean {
        return _rejectActions.tryEmit(QuestionRejectAction(questionId, sessionId))
    }
}
