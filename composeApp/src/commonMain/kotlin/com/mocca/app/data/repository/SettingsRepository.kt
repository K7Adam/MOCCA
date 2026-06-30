package com.mocca.app.data.repository

import com.mocca.app.data.local.LocalCache
import com.mocca.app.data.security.SecureTokenStorage
import com.mocca.app.domain.model.UpdateInfo
import com.mocca.app.domain.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(
    private val localCache: LocalCache,
    private val secureTokenStorage: SecureTokenStorage
) {
    companion object {
        // Session State
        const val KEY_LAST_SESSION_ID = "last_session_id"
        
        // GitHub / Updates
        const val KEY_GITHUB_TOKEN = "github_token"
        
        // Appearance
        const val KEY_SHOW_TOKEN_COUNTS = "show_token_counts"
        const val KEY_SHOW_TIMESTAMPS = "show_timestamps"
        const val KEY_CODE_FONT_FAMILY = "code_font_family"
        
        // Notifications
        const val KEY_NOTIFY_PERMISSIONS = "notify_permissions"
        const val KEY_NOTIFY_SESSION_COMPLETE = "notify_session_complete"
        const val KEY_NOTIFY_CONNECTION_LOST = "notify_connection_lost"
        
        // Updates
        const val KEY_AUTO_UPDATE_CHECK_INTERVAL = "auto_update_check_interval"
        const val KEY_ACTIVE_DOWNLOAD_ID = "active_download_id"
        const val KEY_DOWNLOADED_VERSION = "downloaded_version"
        const val KEY_DOWNLOADED_DIGEST = "downloaded_digest"
        const val KEY_UPDATE_MANIFEST_ETAG = "update_manifest_etag"
        const val KEY_UPDATE_RELEASE_ETAG = "update_release_etag"
        const val KEY_CACHED_UPDATE_INFO = "cached_update_info"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Session State
    suspend fun getLastSessionId(): String? = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_LAST_SESSION_ID)
    }

    suspend fun saveLastSessionId(sessionId: String?) = withContext(Dispatchers.IO) {
        if (sessionId.isNullOrBlank()) {
            localCache.deleteSetting(KEY_LAST_SESSION_ID)
        } else {
            localCache.saveSetting(KEY_LAST_SESSION_ID, sessionId)
        }
    }

    // GitHub Token — stored encrypted via SecureTokenStorage
    suspend fun getGitHubToken(): String? = withContext(Dispatchers.IO) {
        val stored = localCache.getSetting(KEY_GITHUB_TOKEN)
        when {
            stored.isNullOrBlank() -> null
            secureTokenStorage.isEncrypted(stored) -> {
                secureTokenStorage.decrypt(stored)
            }
            else -> {
                // Backward compatibility: migrate plaintext token to encrypted storage
                val encrypted = secureTokenStorage.encrypt(stored)
                localCache.saveSetting(KEY_GITHUB_TOKEN, encrypted)
                stored
            }
        }
    }

    suspend fun saveGitHubToken(token: String) = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            localCache.deleteSetting(KEY_GITHUB_TOKEN)
        } else {
            val encrypted = secureTokenStorage.encrypt(token)
            localCache.saveSetting(KEY_GITHUB_TOKEN, encrypted)
        }
    }

    suspend fun clearGitHubToken() = withContext(Dispatchers.IO) {
        localCache.deleteSetting(KEY_GITHUB_TOKEN)
    }

    // User Preferences (typed getters/setters)
    suspend fun getUserPreferences(): UserPreferences = withContext(Dispatchers.IO) {
        UserPreferences(
            lastSessionId = getLastSessionId(),
            showTokenCounts = getBoolean(KEY_SHOW_TOKEN_COUNTS, true),
            showTimestamps = getBoolean(KEY_SHOW_TIMESTAMPS, true),
            codeFontFamily = getString(KEY_CODE_FONT_FAMILY, UserPreferences.DEFAULT_CODE_FONT),
            notifyPermissions = getBoolean(KEY_NOTIFY_PERMISSIONS, true),
            notifySessionComplete = getBoolean(KEY_NOTIFY_SESSION_COMPLETE, true),
            notifyConnectionLost = getBoolean(KEY_NOTIFY_CONNECTION_LOST, true),
            autoUpdateCheckIntervalMinutes = getInt(KEY_AUTO_UPDATE_CHECK_INTERVAL, 10)
        )
    }

    // Appearance Settings
    suspend fun getShowTokenCounts(): Boolean = getBoolean(KEY_SHOW_TOKEN_COUNTS, true)
    suspend fun setShowTokenCounts(value: Boolean) = setBoolean(KEY_SHOW_TOKEN_COUNTS, value)

    suspend fun getShowTimestamps(): Boolean = getBoolean(KEY_SHOW_TIMESTAMPS, true)
    suspend fun setShowTimestamps(value: Boolean) = setBoolean(KEY_SHOW_TIMESTAMPS, value)

    suspend fun getCodeFontFamily(): String = getString(KEY_CODE_FONT_FAMILY, UserPreferences.DEFAULT_CODE_FONT)
    suspend fun setCodeFontFamily(value: String) = withContext(Dispatchers.IO) {
        localCache.saveSetting(KEY_CODE_FONT_FAMILY, value)
    }

    // Notification Settings
    suspend fun getNotifyPermissions(): Boolean = getBoolean(KEY_NOTIFY_PERMISSIONS, true)
    suspend fun setNotifyPermissions(value: Boolean) = setBoolean(KEY_NOTIFY_PERMISSIONS, value)

    suspend fun getNotifySessionComplete(): Boolean = getBoolean(KEY_NOTIFY_SESSION_COMPLETE, true)
    suspend fun setNotifySessionComplete(value: Boolean) = setBoolean(KEY_NOTIFY_SESSION_COMPLETE, value)

    suspend fun getNotifyConnectionLost(): Boolean = getBoolean(KEY_NOTIFY_CONNECTION_LOST, true)
    suspend fun setNotifyConnectionLost(value: Boolean) = setBoolean(KEY_NOTIFY_CONNECTION_LOST, value)

    // Update Settings
    suspend fun getAutoUpdateCheckInterval(): Int = getInt(KEY_AUTO_UPDATE_CHECK_INTERVAL, 10)
    suspend fun setAutoUpdateCheckInterval(value: Int) = setInt(KEY_AUTO_UPDATE_CHECK_INTERVAL, value)

    suspend fun getActiveDownloadId(): Long = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_ACTIVE_DOWNLOAD_ID)?.toLongOrNull() ?: -1L
    }

    suspend fun setActiveDownloadId(id: Long) = withContext(Dispatchers.IO) {
        if (id == -1L) {
            localCache.deleteSetting(KEY_ACTIVE_DOWNLOAD_ID)
        } else {
            localCache.saveSetting(KEY_ACTIVE_DOWNLOAD_ID, id.toString())
        }
    }

    suspend fun getDownloadedVersion(): String? = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_DOWNLOADED_VERSION)
    }

    suspend fun setDownloadedVersion(version: String?) = withContext(Dispatchers.IO) {
        if (version.isNullOrBlank()) {
            localCache.deleteSetting(KEY_DOWNLOADED_VERSION)
        } else {
            localCache.saveSetting(KEY_DOWNLOADED_VERSION, version)
        }
    }

    suspend fun getDownloadedDigest(): String? = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_DOWNLOADED_DIGEST)
    }

    suspend fun setDownloadedDigest(digest: String?) = withContext(Dispatchers.IO) {
        if (digest.isNullOrBlank()) {
            localCache.deleteSetting(KEY_DOWNLOADED_DIGEST)
        } else {
            localCache.saveSetting(KEY_DOWNLOADED_DIGEST, digest)
        }
    }

    suspend fun getUpdateReleaseEtag(): String? = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_UPDATE_RELEASE_ETAG)
    }

    suspend fun setUpdateReleaseEtag(etag: String?) = withContext(Dispatchers.IO) {
        if (etag.isNullOrBlank()) {
            localCache.deleteSetting(KEY_UPDATE_RELEASE_ETAG)
        } else {
            localCache.saveSetting(KEY_UPDATE_RELEASE_ETAG, etag)
        }
    }

    suspend fun getUpdateManifestEtag(): String? = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_UPDATE_MANIFEST_ETAG)
    }

    suspend fun setUpdateManifestEtag(etag: String?) = withContext(Dispatchers.IO) {
        if (etag.isNullOrBlank()) {
            localCache.deleteSetting(KEY_UPDATE_MANIFEST_ETAG)
        } else {
            localCache.saveSetting(KEY_UPDATE_MANIFEST_ETAG, etag)
        }
    }

    suspend fun getCachedUpdateInfo(): UpdateInfo? = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_CACHED_UPDATE_INFO)?.let { raw ->
            try {
                json.decodeFromString<UpdateInfo>(raw)
            } catch (_: SerializationException) {
                localCache.deleteSetting(KEY_CACHED_UPDATE_INFO)
                null
            }
        }
    }

    suspend fun setCachedUpdateInfo(updateInfo: UpdateInfo?) = withContext(Dispatchers.IO) {
        if (updateInfo == null) {
            localCache.deleteSetting(KEY_CACHED_UPDATE_INFO)
        } else {
            localCache.saveSetting(KEY_CACHED_UPDATE_INFO, json.encodeToString(updateInfo))
        }
    }

    // Helper Methods
    private suspend fun getBoolean(key: String, default: Boolean): Boolean = withContext(Dispatchers.IO) {
        localCache.getSetting(key)?.toBooleanStrictOrNull() ?: default
    }

    private suspend fun setBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        localCache.saveSetting(key, value.toString())
    }

    private suspend fun getFloat(key: String, default: Float): Float = withContext(Dispatchers.IO) {
        localCache.getSetting(key)?.toFloatOrNull() ?: default
    }

    private suspend fun setFloat(key: String, value: Float) = withContext(Dispatchers.IO) {
        localCache.saveSetting(key, value.toString())
    }

    private suspend fun getInt(key: String, default: Int): Int = withContext(Dispatchers.IO) {
        localCache.getSetting(key)?.toIntOrNull() ?: default
    }

    private suspend fun setInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        localCache.saveSetting(key, value.toString())
    }

    private suspend fun getString(key: String, default: String): String = withContext(Dispatchers.IO) {
        localCache.getSetting(key) ?: default
    }
}
