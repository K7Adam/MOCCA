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
                    null
                }
            } else {
                null
            }
        }.recover { e ->
            // Handle failure gracefully - log warning but don't crash
            if (e.message?.contains("No releases found") == true) {
                Napier.i("No releases found on GitHub yet", tag = "UpdateRepository")
                null // Return null to indicate no update available
            } else {
                Napier.w("Update check failed: ${e.message}", e, tag = "UpdateRepository")
                throw e // Re-throw other errors
            }
        }
    }
    
    // Helper to compare versions (simple semver)
    private fun isNewer(remote: String, current: String): Boolean {
        // Handle build numbers: 1.0.0-build.42 vs 1.0.0
        // If base versions differ, compare bases.
        // If bases equal, compare build numbers.
        
        val remoteBase = remote.substringBefore("-")
        val currentBase = current.substringBefore("-")
        
        if (remoteBase != currentBase) {
            val remoteParts = remoteBase.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = currentBase.split(".").mapNotNull { it.toIntOrNull() }
            
            val length = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until length) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
        
        // Base versions equal, check build number
        val remoteBuild = remote.substringAfter("build.", "").toIntOrNull() ?: 0
        val currentBuild = current.substringAfter("build.", "").toIntOrNull() ?: 0
        
        return remoteBuild > currentBuild
    }
            } else {
                null
            }
        }.recover { e ->
            // Handle failure gracefully - log warning but don't crash
            if (e.message?.contains("No releases found") == true) {
                Napier.i("No releases found on GitHub yet", tag = "UpdateRepository")
                null // Return null to indicate no update available
            } else {
                Napier.w("Update check failed: ${e.message}", e, tag = "UpdateRepository")
                throw e // Re-throw other errors
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
    
    // Helper to compare versions (simple semver)
    private fun isNewer(remote: String, current: String): Boolean {
        // Simple comparison for now. Ideally use a library or stronger logic.
        // Assuming X.Y.Z
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        
        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
