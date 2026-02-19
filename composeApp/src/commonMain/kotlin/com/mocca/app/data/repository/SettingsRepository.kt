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

    /**
     * Gets the GitHub Personal Access Token from storage.
     * Returns null if no token is configured.
     * 
     * Note: GitHub PATs are required for:
     * - Private repository access
     * - Higher rate limits (5000 requests/hour vs 60 for unauthenticated)
     * 
     * Create a token at: https://github.com/settings/tokens
     * Required scopes: repo (for private repos) or public_repo (for public repos)
     */
    suspend fun getGitHubToken(): String? = withContext(Dispatchers.IO) {
        localCache.getSetting(KEY_GITHUB_TOKEN)
    }

    suspend fun saveGitHubToken(token: String) = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            localCache.deleteSetting(KEY_GITHUB_TOKEN)
        } else {
            localCache.saveSetting(KEY_GITHUB_TOKEN, token)
        }
    }

    /**
     * Clears the stored GitHub token.
     */
    suspend fun clearGitHubToken() = withContext(Dispatchers.IO) {
        localCache.deleteSetting(KEY_GITHUB_TOKEN)
    }
}
