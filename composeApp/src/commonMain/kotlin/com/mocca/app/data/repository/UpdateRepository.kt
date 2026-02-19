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
        val token = settingsRepository.getGitHubToken()
        
        Napier.d("Checking for updates. Current version: $currentVersion. Token present: ${token != null}", tag = "UpdateRepository")
        
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
                
                val remoteTag = latestRelease.tagName.removePrefix("v")
                val currentTag = currentVersion.removePrefix("v")
                
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
                Napier.e("Update check failed: $errorMessage", e, tag = "UpdateRepository")
                
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

    fun downloadAndInstall(updateInfo: UpdateInfo, fileName: String): Flow<DownloadStatus> = channelFlow {
        send(DownloadStatus.Progress(0f))
        send(DownloadStatus.Log("Starting download process..."))

        val token = settingsRepository.getGitHubToken()
        val useApi = !token.isNullOrBlank()
        val initialUrl = if (useApi) updateInfo.apiUrl else updateInfo.downloadUrl

        send(DownloadStatus.Log("Auth enabled: $useApi"))
        send(DownloadStatus.Log("Initial URL: $initialUrl"))

        // Use a fresh client to manually handle redirects and headers
        val client = HttpClient(getHttpEngine()) {
            install(HttpTimeout) {
                requestTimeoutMillis = 600_000 // 10 mins
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 600_000
            }
            followRedirects = false // CRITICAL: Manual redirect handling
        }

        try {
            var currentUrl = initialUrl
            var attempts = 0
            val maxRedirects = 5
            var downloadSuccess = false

            while (attempts < maxRedirects && !downloadSuccess) {
                send(DownloadStatus.Log("Requesting: $currentUrl"))

                // Use prepareGet to STREAM the response instead of buffering it
                client.prepareGet(currentUrl) {
                    // Only add auth headers to GitHub API URLs, NOT S3 URLs
                    if (useApi && currentUrl.contains("api.github.com")) {
                        header("Authorization", "Bearer $token")
                        header("Accept", "application/octet-stream")
                        send(DownloadStatus.Log("Added Authorization header"))
                    } else {
                        send(DownloadStatus.Log("Skipped Authorization header (external domain)"))
                    }
                }.execute { response ->
                    val status = response.status
                    send(DownloadStatus.Log("Response Status: $status"))

                    if (status == HttpStatusCode.Found || status == HttpStatusCode.MovedPermanently || status == HttpStatusCode.TemporaryRedirect) {
                        val location = response.headers["Location"]
                        if (location != null) {
                            send(DownloadStatus.Log("Redirecting to: $location"))
                            currentUrl = location
                            attempts++
                        } else {
                            throw Exception("Redirect status $status but no Location header found")
                        }
                    } else if (status.isSuccess()) {
                        // 2. Download File via Streaming
                        val length = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                        send(DownloadStatus.Log("Starting download stream. Content-Length: $length"))

                        val responseChannel = response.bodyAsChannel()

                        val path = platformUpdateManager.saveApk(fileName, responseChannel, length) { progress ->
                            // channelFlow allows send() from any coroutine context
                            // The flowOn(Dispatchers.IO) ensures we're already on IO dispatcher
                            send(DownloadStatus.Progress(progress))
                        }

                        send(DownloadStatus.Log("Download saved to: $path"))
                        send(DownloadStatus.Log("Verifying file..."))
                        send(DownloadStatus.Log("Triggering installation..."))
                        
                        // Attempt to install the APK
                        try {
                            platformUpdateManager.installApk(path)
                            downloadSuccess = true
                            send(DownloadStatus.Complete)
                        } catch (e: Exception) {
                            // Check if it's a signature mismatch or other install error
                            val errorMessage = e.message ?: "Installation failed"
                            Napier.e("APK installation failed: $errorMessage", e, "UpdateRepository")
                            send(DownloadStatus.Error(errorMessage, e))
                            downloadSuccess = false
                        }
                    } else {
                        val errorBody = response.bodyAsChannel().readRemaining().readText()
                        throw Exception("Request failed: $status. Body: $errorBody")
                    }
                }
            }

            if (!downloadSuccess) {
                throw Exception("Download failed after $attempts redirects or no success response")
            }

        } catch (e: Exception) {
            Napier.e("Download failed", e, "UpdateRepository")
            send(DownloadStatus.Log("ERROR: ${e.message}"))
            send(DownloadStatus.Error(e.message ?: "Download failed", e))
        } finally {
            client.close()
        }
    }.flowOn(Dispatchers.IO)
    
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
        // A version WITH a build number is considered newer than one WITHOUT
        if (remoteParsed.buildNumber != null && currentParsed.buildNumber == null) {
            return true
        } else if (remoteParsed.buildNumber == null && currentParsed.buildNumber != null) {
            return false
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
    
    private fun parseVersion(version: String): ParsedVersion {
        // Check for -build.N suffix
        val buildMatch = Regex("^(.+)-build\\.(\\d+)$").find(version)
        
        return if (buildMatch != null) {
            val base = buildMatch.groupValues[1]
            val buildNum = buildMatch.groupValues[2].toInt()
            ParsedVersion(
                baseVersion = base.split(".").mapNotNull { it.toIntOrNull() },
                buildNumber = buildNum
            )
        } else {
            // No build suffix, just parse base version
            ParsedVersion(
                baseVersion = version.split(".").mapNotNull { it.toIntOrNull() },
                buildNumber = null
            )
        }
    }
}
