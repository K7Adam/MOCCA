package com.mocca.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mocca.app.MainActivity

/**
 * MOCCA home screen widget showing connection status and quick-launch action.
 *
 * Uses Glance + Material 3 for a native, compose-based widget experience.
 * Tapping the widget launches the main activity.
 *
 * Widget data is pushed from the app via SharedPreferences ("mocca_widget_prefs"):
 * - connection_status: String ("Connected", "Connecting", "Disconnected", etc.)
 * - session_count: Int
 * - last_sync: String
 */
class MoccaStatusWidget : GlanceAppWidget() {

    private companion object {
        const val PREFS_NAME = "mocca_widget_prefs"
        const val KEY_CONNECTION_STATUS = "connection_status"
        const val KEY_SESSION_COUNT = "session_count"
        const val KEY_LAST_SYNC = "last_sync"
        const val DEFAULT_STATUS = "Not configured"
        const val DEFAULT_SYNC = "—"

        val STATUS_COLOR_CONNECTED = Color(0xFF4CAF50)
        val STATUS_COLOR_CONNECTING = Color(0xFFFFC107)
        val STATUS_COLOR_DEFAULT = Color(0xFF9E9E9E)
        val COLOR_SECONDARY_TEXT = Color(0xFF757575)
        val COLOR_TERTIARY_TEXT = Color(0xFF9E9E9E)

        val FONT_SIZE_TITLE = TextUnit(18f, TextUnitType.Sp)
        val FONT_SIZE_STATUS = TextUnit(13f, TextUnitType.Sp)
        val FONT_SIZE_SESSIONS = TextUnit(12f, TextUnitType.Sp)
        val FONT_SIZE_SYNC = TextUnit(11f, TextUnitType.Sp)

        val PADDING_OUTER = 16.dp
        val SPACER_SMALL = 4.dp
        val SPACER_MEDIUM = 8.dp
        val SPACER_TINY = 2.dp
        val SPACER_DOT = 6.dp
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val connectionStatus = prefs.getString(KEY_CONNECTION_STATUS, DEFAULT_STATUS) ?: "Unknown"
        val sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0)
        val lastSync = prefs.getString(KEY_LAST_SYNC, DEFAULT_SYNC) ?: DEFAULT_SYNC

        val statusColor = when (connectionStatus) {
            "Connected" -> STATUS_COLOR_CONNECTED
            "Connecting", "Reconnecting" -> STATUS_COLOR_CONNECTING
            else -> STATUS_COLOR_DEFAULT
        }

        provideContent { renderWidget(connectionStatus, sessionCount, lastSync, statusColor) }
    }

    @androidx.compose.runtime.Composable
    private fun renderWidget(
        connectionStatus: String,
        sessionCount: Int,
        lastSync: String,
        statusColor: Color
    ) {
        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(PADDING_OUTER)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Title
                    Text(
                        text = "MOCCA",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = FONT_SIZE_TITLE
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(SPACER_SMALL))

                    // Connection status with colored dot
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "●",
                            style = TextStyle(color = ColorProvider(statusColor))
                        )
                        Spacer(modifier = GlanceModifier.width(SPACER_DOT))
                        Text(
                            text = connectionStatus,
                            style = TextStyle(fontSize = FONT_SIZE_STATUS)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(SPACER_MEDIUM))

                    // Sessions count
                    Text(
                        text = "$sessionCount active session${if (sessionCount != 1) "s" else ""}",
                        style = TextStyle(
                            fontSize = FONT_SIZE_SESSIONS,
                            color = ColorProvider(COLOR_SECONDARY_TEXT)
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(SPACER_TINY))

                    // Last sync
                    Text(
                        text = "Synced: $lastSync",
                        style = TextStyle(
                            fontSize = FONT_SIZE_SYNC,
                            color = ColorProvider(COLOR_TERTIARY_TEXT)
                        )
                    )
                }
            }
        }
    }
}

class MoccaStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MoccaStatusWidget()
}
