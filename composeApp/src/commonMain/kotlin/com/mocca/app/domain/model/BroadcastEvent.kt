package com.mocca.app.domain.model

sealed class BroadcastEvent {
    /** Server event received */
    data class ServerEvent(val event: com.mocca.app.domain.model.ServerEvent) : BroadcastEvent()

    /** Active session changed */
    data class ActiveSessionChanged(val sessionId: String?) : BroadcastEvent()

    /** Connection state changed */
    data class ConnectionStateChanged(val status: ConnectionStatus) : BroadcastEvent()

    /** Sync completed successfully */
    data object SyncCompleted : BroadcastEvent()

    /** Sync failed */
    data class SyncFailed(val error: String) : BroadcastEvent()

    /** Global (non-session) event received - installation updates, LSP diagnostics, etc. */
    data class GlobalEvent(val event: com.mocca.app.domain.model.ServerEvent) : BroadcastEvent()

    /** Installation updated - triggers full cache invalidation and sync */
    data object InstallationUpdated : BroadcastEvent()
}
