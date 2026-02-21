package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String,
    val assets: List<GitHubAsset>
)

@Serializable
@Immutable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("url") val apiUrl: String,
    val size: Long,
    @SerialName("content_type") val contentType: String
)

@Immutable

data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apiUrl: String,
    val size: Long
)

/**
 * Represents the status of a GitHub Personal Access Token.
 * Used to provide user feedback about token validity.
 */
sealed class GitHubTokenStatus {
    /** Token is valid and working */
    data object Valid : GitHubTokenStatus()
    /** No token configured */
    data object Missing : GitHubTokenStatus()
    /** Token is invalid, expired, or revoked */
    @Immutable
    data class Invalid(val reason: String) : GitHubTokenStatus()
    /** Error occurred during validation (network issue, etc.) */
    @Immutable
    data class Error(val message: String) : GitHubTokenStatus()
    
    val isValid: Boolean get() = this is Valid
    val isMissing: Boolean get() = this is Missing
    val isError: Boolean get() = this is Error || this is Invalid
    val displayMessage: String? get() = when (this) {
        is Valid -> "Token is valid"
        is Missing -> "No token configured"
        is Invalid -> reason
        is Error -> message
    }
}

/**
 * Result of an update check with detailed status.
 * Distinguishes between "no updates available" and errors.
 */
sealed class UpdateCheckResult {
    /** Update is available */
    @Immutable
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateCheckResult()
    /** No update available (current version is latest) */
    data object NoUpdate : UpdateCheckResult()
    /** Error occurred during check */
    @Immutable
    data class Error(val message: String, val tokenStatus: GitHubTokenStatus? = null) : UpdateCheckResult()
}

sealed class DownloadStatus {
    @Immutable
    data class Progress(val progress: Float) : DownloadStatus()
    @Immutable
    data class Log(val message: String) : DownloadStatus()
    data object Complete : DownloadStatus()
    @Immutable
    data class Error(val message: String, val throwable: Throwable? = null) : DownloadStatus()
}
