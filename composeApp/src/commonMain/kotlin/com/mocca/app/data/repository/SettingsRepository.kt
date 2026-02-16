package com.mocca.app.data.repository

import com.mocca.app.data.local.LocalCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SettingsRepository(
    private val localCache: LocalCache
) {
    companion object {
        const val KEY_GITHUB_TOKEN = "github_token"
    }

    suspend fun getGitHubToken(): String? = withContext(Dispatchers.IO) {
        // TEMPORARY: Hardcoded GitHub PAT for auto-update
        // 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
        val stored = localCache.getSetting(KEY_GITHUB_TOKEN)
        if (stored.isNullOrBlank()) {
            "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"
        } else {
            stored
        }
    }

    suspend fun saveGitHubToken(token: String) = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            localCache.deleteSetting(KEY_GITHUB_TOKEN)
        } else {
            localCache.saveSetting(KEY_GITHUB_TOKEN, token)
        }
    }
}
