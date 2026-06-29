package com.mocca.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import io.github.aakira.napier.Napier

/**
 * Helper to push data updates to the MOCCA home screen widget.
 *
 * Widget data is stored in SharedPreferences ("mocca_widget_prefs") and
 * read by [MoccaStatusWidget] during rendering. After updating prefs,
 * call [pushUpdate] to refresh all widget instances.
 */
object WidgetUpdateHelper {

    private const val PREFS_NAME = "mocca_widget_prefs"
    private const val KEY_CONNECTION_STATUS = "connection_status"
    private const val KEY_SESSION_COUNT = "session_count"
    private const val KEY_LAST_SYNC = "last_sync"

    /**
     * Update the widget data and trigger a visual refresh.
     *
     * @param context Android context
     * @param connectionStatus Human-readable connection status string
     * @param sessionCount Number of active sessions
     * @param lastSync Formatted last sync time string
     */
    suspend fun update(
        context: Context,
        connectionStatus: String,
        sessionCount: Int,
        lastSync: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CONNECTION_STATUS, connectionStatus)
            .putInt(KEY_SESSION_COUNT, sessionCount)
            .putString(KEY_LAST_SYNC, lastSync)
            .apply()

        Napier.d("[WidgetUpdateHelper] Pushed update: status=$connectionStatus, sessions=$sessionCount")
        MoccaStatusWidget().updateAll(context)
    }

    /**
     * Update only the connection status field.
     */
    suspend fun updateConnectionStatus(context: Context, status: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CONNECTION_STATUS, status).apply()
        MoccaStatusWidget().updateAll(context)
    }

    /**
     * Update only the session count field.
     */
    suspend fun updateSessionCount(context: Context, count: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SESSION_COUNT, count).apply()
        MoccaStatusWidget().updateAll(context)
    }
}
