package com.mocca.app.util

import com.mocca.app.domain.model.VoiceInputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface VoiceInputProvider {
    val state: StateFlow<VoiceInputState>

    fun startListening()

    fun stopListening()

    fun release()
}

object NoOpVoiceInputProvider : VoiceInputProvider {
    private val internalState = MutableStateFlow<VoiceInputState>(VoiceInputState.NotAvailable)

    override val state: StateFlow<VoiceInputState> = internalState.asStateFlow()

    override fun startListening() = Unit

    override fun stopListening() = Unit

    override fun release() = Unit
}
