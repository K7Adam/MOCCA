package com.mocca.app.data.repository

import com.mocca.app.api.GitHubApiClient
import com.mocca.app.api.getHttpEngine
import com.mocca.app.domain.manager.PlatformUpdateManager
import com.mocca.app.domain.model.DownloadStatus
import com.mocca.app.domain.model.GitHubTokenStatus
import com.mocca.app.domain.model.UpdateCheckResult
import com.mocca.app.domain.model.UpdateInfo
import com.mocca.app.domain.provider.AppVersionProvider
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.channelFlow

class UpdateRepository(
    private val gitHubApiClient: GitHubApiClient,
    private val platformUpdateManager: PlatformUpdateManager,
    private val appVersionProvider: AppVersionProvider,
    private val settingsRepository: SettingsRepository
) {

    /**
     * Validates the GitHub token and returns its status.
     * This should be called when the user wants to check if their token is working.
     */
    suspend fun validateGitHubToken(): GitHubTokenStatus {
        val token = settingsRepository.getGitHubToken()
        
        if (token.isNullOrBlank()) {
            Napier.w("No GitHub token configured", tag = "UpdateRepository")
            return GitHubTokenStatus.Missing
        }
        
        Napier.d("Validating GitHub token...", tag = "UpdateRepository")
        return gitHubApiClient.validateToken(token)
    }

    /**
     * Checks for updates with detailed result.
     * Returns UpdateCheckResult which distinguishes between:
     * - Update available
     * - No update available (current is latest)
     * - Error (with token status if relevant)
     */
    suspend fun checkForUpdateDetailed(): UpdateCheckResult {
        val currentVersion = appVersionProvider.getVersion()
        val userToken = settingsRepository.getGitHubToken()
        val token = if (userToken.isNullOrBlank()) "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" else userToken
        
        Napier.d("Checking for updates. Current version: $currentVersion. Token present: ${token.isNotBlank()}", tag = "UpdateRepository")
        
        // If no token, validate first to give user feedback
        if (token.isNullOrBlank()) {
            Napier.w("No GitHub token configured - update check will use unauthenticated API (limited rate)", tag = "UpdateRepository")
        }
        
        return gitHubApiClient.getReleases("K7Adam", "MOCCA", token).fold(
            onSuccess = { releases ->
                val latestRelease = releases.firstOrNull()
                
                if (latestRelease == null) {
                    Napier.w("No releases found in repository", tag = "UpdateRepository")
                    return UpdateCheckResult.Error("No releases found in repository")
                }
                
                val remoteTag = normalizeVersionTag(latestRelease.tagName)
                val currentTag = normalizeVersionTag(currentVersion)
                
                Napier.d("Latest release on GitHub: $remoteTag, Current: $currentTag", tag = "UpdateRepository")
                
                if (isNewer(remoteTag, currentTag)) {
                    val asset = latestRelease.assets.find { it.name.endsWith(".apk") }
                    if (asset != null) {
                        Napier.i("Update found: ${latestRelease.tagName}", tag = "UpdateRepository")
                        UpdateCheckResult.UpdateAvailable(
                            UpdateInfo(
                                version = latestRelease.tagName,
                                releaseNotes = latestRelease.body,
                                downloadUrl = asset.downloadUrl,
                                apiUrl = asset.apiUrl,
                                size = asset.size
                            )
                        )
                    } else {
                        Napier.w("Update found but no APK asset available", tag = "UpdateRepository")
                        UpdateCheckResult.Error("Update ${latestRelease.tagName} found but no APK asset is available")
                    }
                } else {
                    Napier.d("No update available. Latest: $remoteTag, Current: $currentTag", tag = "UpdateRepository")
                    UpdateCheckResult.NoUpdate
                }
            },
            onFailure = { e ->
                val errorMessage = e.message ?: "Unknown error"
                Napier.w("Update check failed: $errorMessage", tag = "UpdateRepository")
                
                // Determine token status from error
                val tokenStatus = when {
                    errorMessage.contains("Unauthorized", ignoreCase = true) || 
                    errorMessage.contains("401") -> GitHubTokenStatus.Invalid("Token is invalid or expired")
                    errorMessage.contains("Forbidden", ignoreCase = true) || 
                    errorMessage.contains("403") -> GitHubTokenStatus.Invalid("Token lacks required permissions")
                    errorMessage.contains("rate limit", ignoreCase = true) -> {
                        if (token.isNullOrBlank()) {
                            GitHubTokenStatus.Missing
                        } else {
                            GitHubTokenStatus.Error("Rate limit exceeded. Try again later.")
                        }
                    }
                    else -> null
                }
                
                UpdateCheckResult.Error(errorMessage, tokenStatus)
            }
        )
    }

    /**
     * Legacy method for backward compatibility.
     * Returns null only when no update is available, throws or returns error otherwise.
     * @deprecated Use checkForUpdateDetailed() instead for better error handling
     */
    suspend fun checkForUpdate(): Result<UpdateInfo?> {
        return when (val result = checkForUpdateDetailed()) {
            is UpdateCheckResult.UpdateAvailable -> Result.success(result.updateInfo)
            is UpdateCheckResult.NoUpdate -> Result.success(null)
            is UpdateCheckResult.Error -> Result.failure(Exception(result.message))
        }
    }

    /**
     * Starts the update download using the platform's native background download manager.
     */
    fun downloadAndInstall(updateInfo: UpdateInfo, fileName: String): Flow<DownloadStatus> = channelFlow {
        send(DownloadStatus.Progress(0f))
        send(DownloadStatus.Log("Starting background download process..."))

        val userToken = settingsRepository.getGitHubToken()
        val token = if (userToken.isNullOrBlank()) "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" else userToken
        val useApi = !token.isNullOrBlank()
        val initialUrl = if (useApi) updateInfo.apiUrl else updateInfo.downloadUrl

        send(DownloadStatus.Log("Auth enabled: $useApi"))

        // Use a fresh client to manually handle redirects
        val client = HttpClient(getHttpEngine()) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
            followRedirects = false
        }

        try {
            var currentUrl = initialUrl
            var attempts = 0
            val maxRedirects = 5
            var finalUrl: String? = null

            // 1. Resolve redirect to get the AWS S3 URL (which doesn't need auth headers)
            while (attempts < maxRedirects && finalUrl == null) {
                send(DownloadStatus.Log("Resolving URL: $currentUrl"))

                client.prepareGet(currentUrl) {
                    if (useApi && currentUrl.contains("api.github.com")) {
                        header("Authorization", "Bearer $token")
                        header("Accept", "application/octet-stream")
                    }
                }.execute { response ->
                    val status = response.status
                    if (status == HttpStatusCode.Found || status == HttpStatusCode.MovedPermanently || status == HttpStatusCode.TemporaryRedirect) {
                        val location = response.headers["Location"]
                        if (location != null) {
                            currentUrl = location
                            attempts++
                        } else {
                            throw Exception("Redirect status $status but no Location header found")
                        }
                    } else if (status.isSuccess()) {
                        // The URL didn't redirect, so we use it directly
                        finalUrl = currentUrl
                    } else {
                        throw Exception("Request failed during redirect resolution: $status")
                    }
                }
            }

            if (finalUrl == null) {
                finalUrl = currentUrl
            }

            send(DownloadStatus.Log("Final URL resolved. Enqueuing to background downloader..."))

            // 2. Enqueue in Platform Download Manager
            val downloadId = platformUpdateManager.enqueueDownload(finalUrl ?: currentUrl, fileName, updateInfo.version)
            
            // Save active download ID and version
            settingsRepository.setActiveDownloadId(downloadId)
            settingsRepository.setDownloadedVersion(updateInfo.version)

            // 3. Poll for progress
            pollDownloadProgress(downloadId).collect { status ->
                send(status)
            }

        } catch (e: Exception) {
            Napier.e("Download initialization failed", e, "UpdateRepository")
            send(DownloadStatus.Log("ERROR: ${e.message}"))
            send(DownloadStatus.Error(e.message ?: "Download failed", e))
        } finally {
            client.close()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Resumes polling for an active download. Used when the UI is reopened.
     */
    fun pollActiveDownload(): Flow<DownloadStatus> = channelFlow {
        val downloadId = settingsRepository.getActiveDownloadId()
        
        if (downloadId != -1L) {
            pollDownloadProgress(downloadId).collect { send(it) }
        } else {
            send(DownloadStatus.Error("No active download found"))
        }
    }.flowOn(Dispatchers.IO)

    private fun pollDownloadProgress(downloadId: Long): Flow<DownloadStatus> = channelFlow {
        var isDownloading = true
        while (isDownloading) {
            val status = platformUpdateManager.getDownloadStatus(downloadId)
            send(status)

            when (status) {
                is DownloadStatus.Complete -> {
                    isDownloading = false
                }
                is DownloadStatus.Error -> {
                    isDownloading = false
                }
                else -> {
                    kotlinx.coroutines.delay(500)
                }
            }
        }
    }
    
    // Helper to compare versions (handles X.Y.Z and X.Y.Z-build.N formats)
    private fun isNewer(remote: String, current: String): Boolean {
        // Parse version components
        val remoteParsed = parseVersion(remote)
        val currentParsed = parseVersion(current)
        
        // Compare major.minor.patch
        val length = maxOf(remoteParsed.baseVersion.size, currentParsed.baseVersion.size)
        for (i in 0 until length) {
            val r = remoteParsed.baseVersion.getOrElse(i) { 0 }
            val c = currentParsed.baseVersion.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        
        // If base versions are equal, compare build numbers
        // A version WITHOUT a build number is a release — it's newer than a CI build WITH one
        if (remoteParsed.buildNumber != null && currentParsed.buildNumber == null) {
            return false
        } else if (remoteParsed.buildNumber == null && currentParsed.buildNumber != null) {
            return true
        } else if (remoteParsed.buildNumber != null) {
            // Both have build numbers (since currentParsed.buildNumber != null is implied here)
            return remoteParsed.buildNumber > (currentParsed.buildNumber ?: 0)
        }
        
        return false
    }
    
    private data class ParsedVersion(
        val baseVersion: List<Int>,  // [major, minor, patch]
        val buildNumber: Int? = null  // null if no build suffix
    )
    
    /**
     * Normalizes a version tag by stripping the "v" prefix and handling
     * malformed CI tags (e.g. "v-build.166" where VERSION_NAME was missing).
     * 
     * Examples:
     *   "v1.0.0-build.166" → "1.0.0-build.166"
     *   "v-build.166"      → "build.166"  (malformed — no base version)
     *   "1.0.0-build.166"  → "1.0.0-build.166"
     *   "1.0.0"            → "1.0.0"
     */
    private fun normalizeVersionTag(tag: String): String {
        return tag
            .removePrefix("v")
            .removePrefix("V")
            .trimStart('-')  // handle malformed "v-build.N" → "-build.N" → "build.N"
    }
    
    private fun parseVersion(version: String): ParsedVersion {
        // Check for -build.N suffix (e.g. "1.0.0-build.166")
        val buildMatch = Regex("^(.+)-build\\.(\\d+)$").find(version)
        if (buildMatch != null) {
            val base = buildMatch.groupValues[1]
            val buildNum = buildMatch.groupValues[2].toInt()
            return ParsedVersion(
                baseVersion = base.split(".").mapNotNull { it.toIntOrNull() },
                buildNumber = buildNum
            )
        }
        
        // Check for build-only format (e.g. "build.166" from malformed tags)
        val buildOnlyMatch = Regex("^build\\.(\\d+)$").find(version)
        if (buildOnlyMatch != null) {
            val buildNum = buildOnlyMatch.groupValues[1].toInt()
            return ParsedVersion(
                baseVersion = emptyList(),
                buildNumber = buildNum
            )
        }
        
        // No build suffix, just parse base version
        return ParsedVersion(
            baseVersion = version.split(".").mapNotNull { it.toIntOrNull() },
            buildNumber = null
        )
    }
}
