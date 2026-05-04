package com.mocca.app.domain.manager

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mocca.app.data.repository.SettingsRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ApkDownloadReceiver : BroadcastReceiver(), KoinComponent {
    private companion object {
        const val TAG = "ApkDownloadReceiver"
    }

    private val platformUpdateManager: PlatformUpdateManager by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            // BroadcastReceivers need to finish quickly. Use goAsync if we do suspend work
            val pendingResult = goAsync()

            receiverScope.launch {
                try {
                    val activeDownloadId = settingsRepository.getActiveDownloadId()

                    Napier.i(
                        "Received ACTION_DOWNLOAD_COMPLETE for ID: $downloadId. Active ID: $activeDownloadId",
                        tag = TAG
                    )

                    if (downloadId != -1L && downloadId == activeDownloadId) {
                        // This is our update APK download
                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)

                        if (cursor != null && cursor.moveToFirst()) {
                            val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (statusColumn >= 0) {
                                val status = cursor.getInt(statusColumn)
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    val uriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                    if (uriColumn >= 0) {
                                        val uriString = cursor.getString(uriColumn)
                                        if (uriString != null) {
                                            val version = settingsRepository.getDownloadedVersion() ?: ""
                                            Napier.i(
                                                "Download complete successfully. URI: $uriString. Installing...",
                                                tag = TAG
                                            )

                                            // Trigger installation
                                            platformUpdateManager.installUpdate(version, uriString)
                                        }
                                    }
                                } else {
                                    val reasonColumn = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                    val reason = if (reasonColumn >= 0) cursor.getInt(reasonColumn) else -1
                                    Napier.e(
                                        "Download failed with status $status and reason $reason",
                                        tag = TAG
                                    )
                                }
                            }
                        }
                        cursor?.close()

                        // Clear the active download since it's completed (success or fail)
                        settingsRepository.setActiveDownloadId(-1L)
                        settingsRepository.setDownloadedVersion(null)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
