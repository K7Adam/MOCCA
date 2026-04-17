package com.mocca.app.domain.model

sealed class VoiceInputState {
    data object Idle : VoiceInputState()
    data object Listening : VoiceInputState()
    data class PartialResult(val text: String) : VoiceInputState()
    data class FinalResult(val text: String) : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
    data object NotAvailable : VoiceInputState()
    data object NeedsPermission : VoiceInputState()
}
