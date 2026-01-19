package com.mocca.app.data.repository

import com.mocca.app.api.GitHubApiClient
import com.mocca.app.domain.manager.PlatformUpdateManager
import com.mocca.app.domain.model.UpdateInfo
import com.mocca.app.domain.provider.AppVersionProvider
import io.github.aakira.napier.Napier
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.request.prepareGet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UpdateRepository(
    private val gitHubApiClient: GitHubApiClient,
    private val platformUpdateManager: PlatformUpdateManager,
    private val appVersionProvider: AppVersionProvider
) {

    suspend fun checkForUpdate(): Result<UpdateInfo?> {
        val currentVersion = appVersionProvider.getVersion()
        Napier.d("Checking for updates. Current version: $currentVersion", tag = "UpdateRepository")
        
        return gitHubApiClient.getReleases("K7Adam", "MOCCA").mapCatching { releases ->
            val latestRelease = releases.firstOrNull() ?: return@mapCatching null
            
            // Handle success
            val remoteTag = latestRelease.tagName.removePrefix("v")
            val currentTag = currentVersion.removePrefix("v")
            
            Napier.d("Latest release on GitHub: $remoteTag, Current: $currentTag", tag = "UpdateRepository")
            
            // Compare including build numbers if present (simple string check for now or handle build metadata)
            // Ideally: Use a proper SemVer parser. 
            // For "1.0.0-build.123", we might want to compare 123 if base versions match.
            
            if (isNewer(remoteTag, currentTag)) {
                val asset = latestRelease.assets.find { it.name.endsWith(".apk") }
                if (asset != null) {
                    UpdateInfo(
                        version = latestRelease.tagName,
                        releaseNotes = latestRelease.body,
                        downloadUrl = asset.downloadUrl,
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
            
            if (errorMessage.contains("No releases found") || errorMessage.contains("Not Found")) {
                Napier.w(
                    "Failed to access GitHub releases. Repository may be private or not accessible. Error: $errorMessage",
                    tag = "UpdateRepository"
                )
                // Return null instead of throwing to prevent UI crash
                null
            } else if (errorMessage.contains("GitHub API Error")) {
                Napier.w("GitHub API error during update check: $errorMessage", e, tag = "UpdateRepository")
                null
            } else {
                Napier.e("Unexpected error during update check: $errorMessage", e, tag = "UpdateRepository")
                throw e
            }
        }
    }

    fun downloadAndInstall(url: String, fileName: String): Flow<Float> = flow {
        emit(0f)
        try {
            gitHubApiClient.getClient().prepareGet(url).execute { response ->
                val length = response.headers["Content-Length"]?.toLong()
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
        } else if (remoteParsed.buildNumber != null && currentParsed.buildNumber != null) {
            return remoteParsed.buildNumber > currentParsed.buildNumber
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
