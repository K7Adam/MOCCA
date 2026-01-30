package com.mocca.app.data.repository

import com.mocca.app.domain.model.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global update notifier that allows any screen to trigger the update dialog.
 * This enables SettingsScreen to show update dialog when returning to MainScreen.
 */
class UpdateNotifier {
    private val _pendingUpdate = MutableStateFlow<UpdateInfo?>(null)
    val pendingUpdate: StateFlow<UpdateInfo?> = _pendingUpdate.asStateFlow()

    /**
     * Notify that an update is available.
     * Called from SettingsScreen when manual check finds an update.
     */
    fun notifyUpdateAvailable(updateInfo: UpdateInfo) {
        _pendingUpdate.value = updateInfo
    }

    /**
     * Clear the pending update notification.
     * Called from MainScreen when update dialog is shown or dismissed.
     */
    fun clearNotification() {
        _pendingUpdate.value = null
    }
}
