package com.mocca.app.data.repository

import com.mocca.app.api.GitHubApiClient
import com.mocca.app.api.GitHubReleaseFetchResult
import com.mocca.app.api.UpdateManifestFetchResult
import com.mocca.app.api.getHttpEngine
import com.mocca.app.domain.manager.PlatformUpdateManager
import com.mocca.app.domain.model.DownloadStatus
import com.mocca.app.domain.model.GitHubAsset
import com.mocca.app.domain.model.GitHubRelease
import com.mocca.app.domain.model.GitHubTokenStatus
import com.mocca.app.domain.model.UpdateCheckResult
import com.mocca.app.domain.model.UpdateInfo
import com.mocca.app.domain.model.UpdateManifest
import com.mocca.app.domain.model.UpdateSource
import com.mocca.app.domain.provider.AppVersionProvider
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

class UpdateRepository(
    private val gitHubApiClient: GitHubApiClient,
    private val platformUpdateManager: PlatformUpdateManager,
    private val appVersionProvider: AppVersionProvider,
    private val settingsRepository: SettingsRepository
) {
    private companion object {
        const val RELEASE_OWNER = "K7Adam"
        const val RELEASE_REPO = "MOCCA"
        const val PUBLIC_UPDATE_MANIFEST_URL = "https://k7adam.github.io/MOCCA/update/latest.json"
    }

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
        val manifestEtag = settingsRepository.getUpdateManifestEtag()
        val releaseEtag = settingsRepository.getUpdateReleaseEtag()
        val cachedUpdate = settingsRepository.getCachedUpdateInfo()
        
        Napier.d(
            "Checking for updates. Current version: $currentVersion. Manifest ETag present: ${!manifestEtag.isNullOrBlank()}. Token fallback present: ${!token.isNullOrBlank()}",
            tag = "UpdateRepository"
        )

        when (val manifestResult = gitHubApiClient.getUpdateManifest(PUBLIC_UPDATE_MANIFEST_URL, manifestEtag)) {
            is UpdateManifestFetchResult.Success -> {
                settingsRepository.setUpdateManifestEtag(manifestResult.etag)
                return evaluateManifest(manifestResult.manifest, currentVersion)
            }
            is UpdateManifestFetchResult.NotModified -> {
                cachedUpdate?.let { return evaluateCachedRelease(it, currentVersion) }
            }
            is UpdateManifestFetchResult.Failure -> {
                Napier.w(
                    "Public update manifest failed (${manifestResult.statusCode}): ${manifestResult.message}. Falling back to GitHub releases.",
                    tag = "UpdateRepository"
                )
            }
        }

        return fetchGitHubReleaseWithFallback(currentVersion, token, releaseEtag, cachedUpdate)
    }

    private suspend fun fetchGitHubReleaseWithFallback(
        currentVersion: String,
        token: String?,
        etag: String?,
        cachedUpdate: UpdateInfo?
    ): UpdateCheckResult {
        val publicResult = gitHubApiClient.getLatestRelease(
            owner = RELEASE_OWNER,
            repo = RELEASE_REPO,
            token = null,
            etag = etag
        )

        return when (publicResult) {
            is GitHubReleaseFetchResult.Success -> {
                settingsRepository.setUpdateReleaseEtag(publicResult.etag)
                evaluateRelease(publicResult.release, currentVersion, UpdateSource.PublicGitHubRelease)
            }
            is GitHubReleaseFetchResult.NotModified -> {
                cachedUpdate?.let { evaluateCachedRelease(it, currentVersion) }
                    ?: fetchWithoutEtag(currentVersion, token)
            }
            is GitHubReleaseFetchResult.Failure -> {
                Napier.w("Public GitHub update check failed: ${publicResult.message}", tag = "UpdateRepository")
                if (!token.isNullOrBlank()) {
                    fetchAuthenticatedFallback(currentVersion, token, etag)
                } else {
                    UpdateCheckResult.Error(
                        message = "${publicResult.message}. Public updates do not require a GitHub token, but a token can help if GitHub rate limits this device.",
                        tokenStatus = if (publicResult.statusCode == 403) GitHubTokenStatus.Missing else null
                    )
                }
            }
        }
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

        val token = settingsRepository.getGitHubToken()
        val useApi = !token.isNullOrBlank() && updateInfo.source == UpdateSource.AuthenticatedGitHubRelease
        val initialUrl = if (useApi) updateInfo.apiUrl else updateInfo.downloadUrl

        send(DownloadStatus.Log("Release source: ${updateInfo.source}"))
        send(DownloadStatus.Log("Authenticated asset download: $useApi"))
        updateInfo.digest?.let { send(DownloadStatus.Log("Expected asset digest: $it")) }

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

            val resolvedUrl = finalUrl ?: currentUrl

            send(DownloadStatus.Log("Final URL resolved. Enqueuing to background downloader..."))

            // 2. Enqueue in Platform Download Manager
            val downloadId = platformUpdateManager.enqueueDownload(resolvedUrl, fileName, updateInfo.version)
            
            // Save active download ID and version
            settingsRepository.setActiveDownloadId(downloadId)
            settingsRepository.setDownloadedVersion(updateInfo.version)
            settingsRepository.setDownloadedDigest(updateInfo.digest)

            // 3. Poll for progress
            pollDownloadProgress(downloadId).collect { status ->
                send(status)
            }

        } catch (e: Exception) {
            Napier.e("Download initialization failed", e, "UpdateRepository")
            send(DownloadStatus.Log("ERROR: ${e.message}"))
            send(DownloadStatus.Error(e.message ?: "Download failed", e))
            settingsRepository.setDownloadedDigest(null)
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

    private suspend fun fetchWithoutEtag(currentVersion: String, token: String?): UpdateCheckResult {
        Napier.d("Cached release metadata missing after 304. Re-fetching without ETag.", tag = "UpdateRepository")
        val result = gitHubApiClient.getLatestRelease(
            owner = RELEASE_OWNER,
            repo = RELEASE_REPO,
            token = null,
            etag = null
        )
        return when (result) {
            is GitHubReleaseFetchResult.Success -> {
                settingsRepository.setUpdateReleaseEtag(result.etag)
                evaluateRelease(result.release, currentVersion, UpdateSource.PublicGitHubRelease)
            }
            is GitHubReleaseFetchResult.NotModified -> UpdateCheckResult.NoUpdate
            is GitHubReleaseFetchResult.Failure -> {
                if (!token.isNullOrBlank()) {
                    fetchAuthenticatedFallback(currentVersion, token, etag = null)
                } else {
                    UpdateCheckResult.Error(result.message)
                }
            }
        }
    }

    private suspend fun fetchAuthenticatedFallback(
        currentVersion: String,
        token: String,
        etag: String?
    ): UpdateCheckResult {
        Napier.d("Trying authenticated GitHub update fallback.", tag = "UpdateRepository")
        return when (val result = gitHubApiClient.getLatestRelease(RELEASE_OWNER, RELEASE_REPO, token, etag)) {
            is GitHubReleaseFetchResult.Success -> {
                settingsRepository.setUpdateReleaseEtag(result.etag)
                evaluateRelease(result.release, currentVersion, UpdateSource.AuthenticatedGitHubRelease)
            }
            is GitHubReleaseFetchResult.NotModified -> {
                settingsRepository.getCachedUpdateInfo()?.let { evaluateCachedRelease(it, currentVersion) }
                    ?: UpdateCheckResult.NoUpdate
            }
            is GitHubReleaseFetchResult.Failure -> {
                UpdateCheckResult.Error(
                    message = result.message,
                    tokenStatus = tokenStatusForFailure(result)
                )
            }
        }
    }

    private suspend fun evaluateRelease(
        release: GitHubRelease,
        currentVersion: String,
        source: UpdateSource
    ): UpdateCheckResult {
        val asset = release.findApkAsset()
        if (asset == null) {
            Napier.w("Release ${release.tagName} has no APK asset", tag = "UpdateRepository")
            return UpdateCheckResult.Error("Release ${release.tagName} found but no APK asset is available")
        }

        val updateInfo = release.toUpdateInfo(asset, source)
        settingsRepository.setCachedUpdateInfo(updateInfo)
        return evaluateUpdateInfo(updateInfo, currentVersion)
    }

    private suspend fun evaluateManifest(
        manifest: UpdateManifest,
        currentVersion: String
    ): UpdateCheckResult {
        if (manifest.apkUrl.isBlank()) {
            return UpdateCheckResult.Error("Update manifest is missing an APK URL")
        }

        val updateInfo = UpdateInfo(
            version = manifest.version,
            releaseNotes = manifest.releaseNotes,
            downloadUrl = manifest.apkUrl,
            apiUrl = "",
            size = manifest.size,
            digest = manifest.digest,
            publishedAt = manifest.publishedAt,
            source = UpdateSource.PublicManifest
        )
        settingsRepository.setCachedUpdateInfo(updateInfo)
        return evaluateUpdateInfo(updateInfo, currentVersion)
    }

    private fun evaluateCachedRelease(updateInfo: UpdateInfo, currentVersion: String): UpdateCheckResult {
        return evaluateUpdateInfo(updateInfo.copy(source = UpdateSource.CachedGitHubRelease), currentVersion)
    }

    private fun evaluateUpdateInfo(updateInfo: UpdateInfo, currentVersion: String): UpdateCheckResult {
        val remoteTag = normalizeVersionTag(updateInfo.version)
        val currentTag = normalizeVersionTag(currentVersion)

        Napier.d("Latest release: $remoteTag, Current: $currentTag", tag = "UpdateRepository")
        return if (isNewer(remoteTag, currentTag)) {
            Napier.i("Update found: ${updateInfo.version}", tag = "UpdateRepository")
            UpdateCheckResult.UpdateAvailable(updateInfo)
        } else {
            Napier.d("No update available. Latest: $remoteTag, Current: $currentTag", tag = "UpdateRepository")
            UpdateCheckResult.NoUpdate
        }
    }

    private fun GitHubRelease.findApkAsset(): GitHubAsset? {
        return assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true) && !it.name.contains("debug", ignoreCase = true)
        } ?: assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true)
        }
    }

    private fun GitHubRelease.toUpdateInfo(asset: GitHubAsset, source: UpdateSource): UpdateInfo =
        UpdateInfo(
            version = tagName,
            releaseNotes = body,
            downloadUrl = asset.downloadUrl,
            apiUrl = asset.apiUrl,
            size = asset.size,
            digest = asset.digest,
            publishedAt = publishedAt,
            source = source
        )

    private fun tokenStatusForFailure(failure: GitHubReleaseFetchResult.Failure): GitHubTokenStatus? =
        when (failure.statusCode) {
            401 -> GitHubTokenStatus.Invalid("Token is invalid or expired")
            403 -> GitHubTokenStatus.Invalid("Token lacks required permissions or has hit a rate limit")
            else -> null
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
