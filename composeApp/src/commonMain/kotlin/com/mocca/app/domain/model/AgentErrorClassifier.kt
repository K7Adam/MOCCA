package com.mocca.app.domain.model

import io.github.aakira.napier.Napier

/**
 * Categorizes agent/session errors into user-friendly, actionable messages.
 *
 * Raw server error strings are often technical, null, or generic ("Unknown error").
 * This classifier maps them to [AgentError] instances with a category, a human-readable
 * title, a concise message, and an optional actionable hint the user can follow.
 */
object AgentErrorClassifier {

    /**
     * Severity controls visual treatment in the UI and notification priority.
     */
    enum class ErrorSeverity { CRITICAL, WARNING, INFO }

    /**
     * Category drives the icon, color accent, and actionable hint.
     */
    enum class ErrorCategory {
        MODEL_UNAVAILABLE,
        PROVIDER_AUTH,
        RATE_LIMIT,
        CONTEXT_LENGTH,
        NETWORK,
        SESSION_EXPIRED,
        AGENT_CRASH,
        UNKNOWN,
    }

    /**
     * Classified error with all metadata needed for UI rendering and notifications.
     */
    data class AgentError(
        val category: ErrorCategory,
        val title: String,
        val message: String,
        val hint: String? = null,
        val severity: ErrorSeverity = ErrorSeverity.WARNING,
        /** The original raw error string, kept for diagnostics/logging. */
        val rawMessage: String? = null,
    )

    /**
     * Classify a raw error [message] and optional [code] from a `session.error` or
     * `agent.status(error)` event.
     *
     * Returns a non-null [AgentError] — never throws, never returns "Unknown error"
     * without an actionable hint.
     */
    fun classify(
        message: String?,
        code: String? = null,
        agentName: String? = null,
    ): AgentError {
        val raw = message?.trim().orEmpty()
        val lower = raw.lowercase()
        val codeLower = code?.lowercase().orEmpty()

        Napier.d("[AgentErrorClassifier] Classifying: message='$raw', code='$code', agent='$agentName'")

        // --- Code-based matching (highest confidence) ---
        if (codeLower.isNotEmpty()) {
            codeMatch(codeLower, raw, agentName)?.let { return it }
        }

        // --- Content-based matching ---
        return when {
            // Model / provider issues
            "model" in lower && ("not found" in lower || "unavailable" in lower || "does not exist" in lower) ->
                modelUnavailable(raw, agentName)

            "provider" in lower && ("not found" in lower || "unavailable" in lower || "not configured" in lower) ->
                modelUnavailable(raw, agentName)

            "no model" in lower || "model is required" in lower || "no valid ai model" in lower ->
                AgentError(
                    category = ErrorCategory.MODEL_UNAVAILABLE,
                    title = "No Model Selected",
                    message = "No AI model is configured for this session.",
                    hint = "Select a provider and model in the chat settings before sending a message.",
                    severity = ErrorSeverity.CRITICAL,
                    rawMessage = raw,
                )

            // Auth issues
            "unauthorized" in lower || "authentication" in lower || "api key" in lower ||
                "401" in lower || "403" in lower || "permission denied" in lower ->
                AgentError(
                    category = ErrorCategory.PROVIDER_AUTH,
                    title = "Provider Authentication Failed",
                    message = "The AI provider rejected the request due to missing or invalid credentials.",
                    hint = "Check your API key in the provider settings and try again.",
                    severity = ErrorSeverity.CRITICAL,
                    rawMessage = raw,
                )

            // Rate limiting
            "rate limit" in lower || "rate_limit" in lower || "429" in lower ||
                "too many requests" in lower || "quota" in lower ->
                AgentError(
                    category = ErrorCategory.RATE_LIMIT,
                    title = "Rate Limit Reached",
                    message = "The AI provider is throttling requests. Too many messages were sent in a short period.",
                    hint = "Wait a moment and try again. If this persists, check your provider plan limits.",
                    severity = ErrorSeverity.WARNING,
                    rawMessage = raw,
                )

            // Context length
            "context length" in lower || "token limit" in lower || "maximum context" in lower ||
                "too long" in lower || "context window" in lower ->
                AgentError(
                    category = ErrorCategory.CONTEXT_LENGTH,
                    title = "Conversation Too Long",
                    message = "The conversation exceeded the model's context window.",
                    hint = "Start a new session, compact the current one, or switch to a model with a larger context window.",
                    severity = ErrorSeverity.WARNING,
                    rawMessage = raw,
                )

            // Network issues
            "connection" in lower && ("refused" in lower || "reset" in lower || "timeout" in lower) ->
                AgentError(
                    category = ErrorCategory.NETWORK,
                    title = "Connection Problem",
                    message = "MOCCA CLI could not reach the AI provider. The network connection was interrupted.",
                    hint = "Check your internet connection and verify the provider endpoint is accessible.",
                    severity = ErrorSeverity.WARNING,
                    rawMessage = raw,
                )

            "timeout" in lower || "timed out" in lower ->
                AgentError(
                    category = ErrorCategory.NETWORK,
                    title = "Request Timed Out",
                    message = "The AI provider did not respond in time.",
                    hint = "The model may be under heavy load. Try again in a moment.",
                    severity = ErrorSeverity.WARNING,
                    rawMessage = raw,
                )

            // Session lifecycle
            "session" in lower && ("expired" in lower || "disposed" in lower || "not found" in lower) ->
                AgentError(
                    category = ErrorCategory.SESSION_EXPIRED,
                    title = "Session Unavailable",
                    message = "This session is no longer active on the server.",
                    hint = "The session may have been disposed or expired. Create a new session to continue.",
                    severity = ErrorSeverity.CRITICAL,
                    rawMessage = raw,
                )

            // Agent crash / internal error
            "panic" in lower || "crash" in lower || "fatal" in lower || "internal error" in lower ->
                AgentError(
                    category = ErrorCategory.AGENT_CRASH,
                    title = "Agent Stopped Unexpectedly",
                    message = "The AI agent encountered an internal error and had to stop.",
                    hint = "Try sending your message again. If the problem persists, restart MOCCA CLI.",
                    severity = ErrorSeverity.CRITICAL,
                    rawMessage = raw,
                )

            // Fallback: unknown but not empty
            raw.isNotEmpty() -> {
                val displayMessage = if (raw.length > 200) raw.take(197) + "..." else raw
                AgentError(
                    category = ErrorCategory.UNKNOWN,
                    title = agentName?.let { "$it Error" } ?: "Agent Error",
                    message = displayMessage,
                    hint = "Try sending your message again. If the problem persists, check MOCCA CLI logs.",
                    severity = ErrorSeverity.WARNING,
                    rawMessage = raw,
                )
            }

            // Completely empty / null
            else -> AgentError(
                category = ErrorCategory.UNKNOWN,
                title = agentName?.let { "$it Error" } ?: "Agent Error",
                message = "The agent stopped without providing error details.",
                hint = "Check that your selected model and provider are available, then try again.",
                severity = ErrorSeverity.WARNING,
                rawMessage = null,
            )
        }
    }

    private fun codeMatch(code: String, raw: String, agentName: String?): AgentError? = when (code) {
        "model_not_found", "model_unavailable" -> modelUnavailable(raw, agentName)
        "provider_not_configured", "no_model" -> AgentError(
            category = ErrorCategory.MODEL_UNAVAILABLE,
            title = "No Model Selected",
            message = "No AI model is configured for this session.",
            hint = "Select a provider and model in the chat settings before sending a message.",
            severity = ErrorSeverity.CRITICAL,
            rawMessage = raw,
        )
        "unauthorized", "auth_failed", "invalid_api_key" -> AgentError(
            category = ErrorCategory.PROVIDER_AUTH,
            title = "Provider Authentication Failed",
            message = "The AI provider rejected the request due to missing or invalid credentials.",
            hint = "Check your API key in the provider settings and try again.",
            severity = ErrorSeverity.CRITICAL,
            rawMessage = raw,
        )
        "rate_limited", "rate_limit_exceeded" -> AgentError(
            category = ErrorCategory.RATE_LIMIT,
            title = "Rate Limit Reached",
            message = "The AI provider is throttling requests.",
            hint = "Wait a moment and try again.",
            severity = ErrorSeverity.WARNING,
            rawMessage = raw,
        )
        "context_too_long", "context_length_exceeded" -> AgentError(
            category = ErrorCategory.CONTEXT_LENGTH,
            title = "Conversation Too Long",
            message = "The conversation exceeded the model's context window.",
            hint = "Start a new session or switch to a model with a larger context window.",
            severity = ErrorSeverity.WARNING,
            rawMessage = raw,
        )
        "session_not_found", "session_expired", "session_disposed" -> AgentError(
            category = ErrorCategory.SESSION_EXPIRED,
            title = "Session Unavailable",
            message = "This session is no longer active on the server.",
            hint = "Create a new session to continue.",
            severity = ErrorSeverity.CRITICAL,
            rawMessage = raw,
        )
        "timeout", "request_timeout" -> AgentError(
            category = ErrorCategory.NETWORK,
            title = "Request Timed Out",
            message = "The AI provider did not respond in time.",
            hint = "The model may be under heavy load. Try again in a moment.",
            severity = ErrorSeverity.WARNING,
            rawMessage = raw,
        )
        else -> null
    }

    private fun modelUnavailable(raw: String, agentName: String?): AgentError = AgentError(
        category = ErrorCategory.MODEL_UNAVAILABLE,
        title = "Model Unavailable",
        message = "The selected AI model could not be found or is not available.",
        hint = "Choose a different model in the chat settings, or verify your provider configuration.",
        severity = ErrorSeverity.CRITICAL,
        rawMessage = raw,
    )
}
