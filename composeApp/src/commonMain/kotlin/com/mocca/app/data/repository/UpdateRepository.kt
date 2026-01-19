package com.mocca.app.data.repository

import com.mocca.app.api.GitHubApiClient
import com.mocca.app.domain.manager.PlatformUpdateManager
import com.mocca.app.domain.model.UpdateInfo
import com.mocca.app.domain.provider.AppVersionProvider
import io.github.aakira.napier.Napier
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UpdateRepository(
    private val gitHubApiClient: GitHubApiClient,
    private val platformUpdateManager: PlatformUpdateManager,
    private val appVersionProvider: AppVersionProvider,
    private val settingsRepository: SettingsRepository
) {

    suspend fun checkForUpdate(): Result<UpdateInfo?> {
        val currentVersion = appVersionProvider.getVersion()
        val token = settingsRepository.getGitHubToken()
        Napier.d("Checking for updates. Current version: $currentVersion. Token present: ${token != null}", tag = "UpdateRepository")
        
        return gitHubApiClient.getReleases("K7Adam", "MOCCA", token).mapCatching { releases ->
            val latestRelease = releases.firstOrNull() ?: return@mapCatching null
            
            // Handle success
            val remoteTag = latestRelease.tagName.removePrefix("v")
            val currentTag = currentVersion.removePrefix("v")
            
            Napier.d("Latest release on GitHub: $remoteTag, Current: $currentTag", tag = "UpdateRepository")
            
            if (isNewer(remoteTag, currentTag)) {
                val asset = latestRelease.assets.find { it.name.endsWith(".apk") }
                if (asset != null) {
                    UpdateInfo(
                        version = latestRelease.tagName,
                        releaseNotes = latestRelease.body,
                        downloadUrl = asset.downloadUrl,
                        apiUrl = asset.apiUrl,
                        size = asset.size
                    )
                } else {
                    Napier.w("Update found but no APK asset available", tag = "UpdateRepository")
                    null
                }
            } else {
                Napier.d("No update available. Latest: $remoteTag, Current: $currentTag", tag = "UpdateRepository")
                null
            }
        }.recover { e ->
            // Handle failure gracefully - log detailed error
            val errorMessage = e.message ?: "Unknown error"
            
            if (errorMessage.contains("No releases found") || errorMessage.contains("Not Found") || errorMessage.contains("404")) {
                Napier.w(
                    "Failed to access GitHub releases. Repository may be private or not accessible. Error: $errorMessage",
                    tag = "UpdateRepository"
                )
                null
            } else if (errorMessage.contains("Unauthorized") || errorMessage.contains("401")) {
                Napier.w("Unauthorized access to GitHub API. Check token.", tag = "UpdateRepository")
                null
            } else {
                Napier.e("Unexpected error during update check: $errorMessage", e, tag = "UpdateRepository")
                throw e
            }
        }
    }

    fun downloadAndInstall(updateInfo: UpdateInfo, fileName: String): Flow<Float> = flow {
        emit(0f)
        try {
            val token = settingsRepository.getGitHubToken()
            val useApi = !token.isNullOrBlank()
            val url = if (useApi) updateInfo.apiUrl else updateInfo.downloadUrl
            
            Napier.d("Starting download. Use API: $useApi, URL: $url", tag = "UpdateRepository")

            gitHubApiClient.getClient().prepareGet(url) {
                if (useApi) {
                    header("Authorization", "Bearer $token")
                    header("Accept", "application/octet-stream")
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw Exception("Download failed with status: ${response.status}")
                }
                
                val length = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                val channel = response.bodyAsChannel()
                
                val path = platformUpdateManager.saveApk(fileName, channel, length) { progress ->
                   emit(progress)
                }
                
                platformUpdateManager.installApk(path)
            }
            emit(1f)
        } catch (e: Exception) {
            Napier.e("Download failed", e, "UpdateRepository")
            throw e
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
        // A version WITH a build number is considered newer than one WITHOUT
        if (remoteParsed.buildNumber != null && currentParsed.buildNumber == null) {
            return true
        } else if (remoteParsed.buildNumber == null && currentParsed.buildNumber != null) {
            return false
        } else if (remoteParsed.buildNumber != null) {
            // Both have build numbers (since currentParsed.buildNumber != null is implied here)
             // But wait, the previous `else if` covered `currentParsed.buildNumber != null` when `remote` is null.
             // If we are here, `remoteParsed.buildNumber` is NOT null.
             // So `currentParsed.buildNumber` could be null OR not null?
             // No, the first `if` handled `remote != null && current == null`.
             // So if we are here, `currentParsed.buildNumber` MUST be NOT null.
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
