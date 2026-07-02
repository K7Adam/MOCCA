package com.mocca.app.widget

/**
 * Shared SharedPreferences contract for widget data.
 *
 * Both [MoccaStatusWidget] (reader) and [WidgetUpdateHelper] (writer)
 * reference these constants to avoid drift.
 */
internal object WidgetPrefsContract {
    const val PREFS_NAME = "mocca_widget_prefs"
    const val KEY_CONNECTION_STATUS = "connection_status"
    const val KEY_SESSION_COUNT = "session_count"
    const val KEY_LAST_SYNC = "last_sync"
    const val DEFAULT_STATUS = "Not configured"
    const val DEFAULT_SYNC = "—"
}
