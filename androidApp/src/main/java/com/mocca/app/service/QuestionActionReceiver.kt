package com.mocca.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.mocca.app.data.repository.QuestionActionBus
import io.github.aakira.napier.Napier

/**
 * BroadcastReceiver for handling question actions from notifications.
 *
 * Supports:
 * - Inline text replies via RemoteInput (quick answer without opening the app)
 * - Reject action (dismiss the question)
 *
 * Uses the V2 session-scoped question API.
 */
class QuestionActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_QUESTION_REPLY = "com.mocca.app.action.QUESTION_REPLY"
        const val ACTION_QUESTION_REJECT = "com.mocca.app.action.QUESTION_REJECT"

        const val EXTRA_QUESTION_ID = "question_id"
        const val EXTRA_SESSION_ID = "session_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val questionId = intent.getStringExtra(EXTRA_QUESTION_ID)
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)

        if (questionId == null) {
            Napier.w("[QuestionActionReceiver] Missing question_id extra")
            return
        }

        if (sessionId == null) {
            Napier.w("[QuestionActionReceiver] Missing session_id extra")
            return
        }

        when (intent.action) {
            ACTION_QUESTION_REPLY -> {
                // Extract the reply text from RemoteInput
                val replyText = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(ActiveSessionService.EXTRA_QUESTION_REPLY)
                    ?.toString()
                    ?.trim()

                if (replyText.isNullOrEmpty()) {
                    Napier.w("[QuestionActionReceiver] Empty reply text for question $questionId")
                    return
                }

                Napier.i("[QuestionActionReceiver] Question $questionId reply: ${replyText.take(50)}...")

                // Emit to shared bus for processing
                val emitted = QuestionActionBus.tryEmitReply(questionId, sessionId, listOf(listOf(replyText)))

                if (!emitted) {
                    Napier.w("[QuestionActionReceiver] Failed to emit reply - buffer full")
                }

                // Dismiss the notification
                ActiveSessionService.dismissQuestionNotification(context, questionId)
            }

            ACTION_QUESTION_REJECT -> {
                Napier.i("[QuestionActionReceiver] Question $questionId rejected via notification")

                val emitted = QuestionActionBus.tryEmitReject(questionId, sessionId)

                if (!emitted) {
                    Napier.w("[QuestionActionReceiver] Failed to emit reject - buffer full")
                }

                // Dismiss the notification
                ActiveSessionService.dismissQuestionNotification(context, questionId)
            }

            else -> {
                Napier.w("[QuestionActionReceiver] Unknown action: ${intent.action}")
            }
        }
    }
}
