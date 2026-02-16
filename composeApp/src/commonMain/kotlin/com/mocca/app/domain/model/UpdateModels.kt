package com.mocca.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("url") val apiUrl: String,
    val size: Long,
    @SerialName("content_type") val contentType: String
)

data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apiUrl: String,
    val size: Long
)

sealed class DownloadStatus {
    data class Progress(val progress: Float) : DownloadStatus()
    data class Log(val message: String) : DownloadStatus()
    data object Complete : DownloadStatus()
    data class Error(val message: String, val throwable: Throwable? = null) : DownloadStatus()
}
