package com.mocca.app.domain.manager

import com.mocca.app.domain.model.DownloadStatus

interface PlatformUpdateManager {
    suspend fun enqueueDownload(url: String, fileName: String, version: String): Long
    fun getDownloadStatus(downloadId: Long): DownloadStatus
    fun installUpdate(version: String, downloadedPath: String)
}
