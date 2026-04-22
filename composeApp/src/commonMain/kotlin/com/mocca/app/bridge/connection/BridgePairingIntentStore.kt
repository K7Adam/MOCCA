package com.mocca.app.bridge.connection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BridgePairingIntentStore {
    private val _pendingPayload = MutableStateFlow<String?>(null)
    val pendingPayload: StateFlow<String?> = _pendingPayload.asStateFlow()

    fun submit(payload: String) {
        if (payload.isNotBlank()) {
            _pendingPayload.value = payload
        }
    }

    fun consume(payload: String) {
        if (_pendingPayload.value == payload) {
            _pendingPayload.value = null
        }
    }
}
