package com.mocca.app.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shared event bus for content received from external sources (share sheet, deep links).
 *
 * The Android layer (MainActivity) publishes shared text into this bus,
 * and the chat UI subscribes to pre-fill the message input.
 */
object SharedContentBus {
    private val _sharedContent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sharedContent: SharedFlow<String> = _sharedContent.asSharedFlow()

    fun publish(content: String) {
        _sharedContent.tryEmit(content)
    }
}
