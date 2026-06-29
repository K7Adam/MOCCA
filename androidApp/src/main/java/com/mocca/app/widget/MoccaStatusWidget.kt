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
import androidx.glance.appwidget.cornerRadius
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("mocca_widget_prefs", Context.MODE_PRIVATE)
        val connectionStatus = prefs.getString("connection_status", "Not configured") ?: "Unknown"
        val sessionCount = prefs.getInt("session_count", 0)
        val lastSync = prefs.getString("last_sync", "—") ?: "—"

        val statusColor = when (connectionStatus) {
            "Connected" -> Color(0xFF4CAF50)
            "Connecting", "Reconnecting" -> Color(0xFFFFC107)
            else -> Color(0xFF9E9E9E)
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(16.dp)
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
                                fontSize = TextUnit(18f, TextUnitType.Sp)
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Connection status with colored dot
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "●",
                                style = TextStyle(color = ColorProvider(statusColor))
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = connectionStatus,
                                style = TextStyle(fontSize = TextUnit(13f, TextUnitType.Sp))
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Sessions count
                        Text(
                            text = "$sessionCount active session${if (sessionCount != 1) "s" else ""}",
                            style = TextStyle(
                                fontSize = TextUnit(12f, TextUnitType.Sp),
                                color = ColorProvider(Color(0xFF757575))
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(2.dp))

                        // Last sync
                        Text(
                            text = "Synced: $lastSync",
                            style = TextStyle(
                                fontSize = TextUnit(11f, TextUnitType.Sp),
                                color = ColorProvider(Color(0xFF9E9E9E))
                            )
                        )
                    }
                }
            }
        }
    }
}

class MoccaStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MoccaStatusWidget()
}
