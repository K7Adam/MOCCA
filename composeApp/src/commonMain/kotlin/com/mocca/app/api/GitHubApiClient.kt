package com.mocca.app.api

import com.mocca.app.domain.model.GitHubRelease
import com.mocca.app.domain.model.GitHubTokenStatus
import io.ktor.client.plugins.HttpTimeout
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Token validation response from GitHub API.
 * Used to check if a token is valid and what scopes it has.
 */
@Serializable
data class GitHubTokenInfo(
    @kotlinx.serialization.SerialName("id")
    val id: Long? = null,
    @kotlinx.serialization.SerialName("url")
    val url: String? = null,
    @kotlinx.serialization.SerialName("scopes")
    val scopes: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("note")
    val note: String? = null,
    @kotlinx.serialization.SerialName("created_at")
    val createdAt: String? = null,
    @kotlinx.serialization.SerialName("expires_at")
    val expiresAt: String? = null
)

class GitHubApiClient {
    private val client = HttpClient(getHttpEngine()) {
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000 // 5 minutes for large APK downloads
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 300_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Validates a GitHub Personal Access Token.
     * Returns detailed status including whether the token is valid, expired, or has issues.
     * 
     * @param token The GitHub PAT to validate
     * @return GitHubTokenStatus with validation results
     */
    suspend fun validateToken(token: String): GitHubTokenStatus {
        if (token.isBlank()) {
            return GitHubTokenStatus.Missing
        }

        return try {
            // Use the token verification endpoint
            // Note: For fine-grained PATs, we check by making a simple API call
            val response = client.get("https://api.github.com/user") {
                header("Authorization", "Bearer $token")
                header("Accept", "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
                header("User-Agent", "MOCCA-Android-Client")
            }

            when {
                response.status == HttpStatusCode.Unauthorized -> {
                    Napier.w("GitHub token validation failed: Unauthorized (401)", tag = "GitHubApiClient")
                    GitHubTokenStatus.Invalid("Token is invalid or has been revoked")
                }
                response.status == HttpStatusCode.Forbidden -> {
                    Napier.w("GitHub token validation failed: Forbidden (403)", tag = "GitHubApiClient")
                    GitHubTokenStatus.Invalid("Token lacks required permissions")
                }
                response.status.isSuccess() -> {
                    Napier.i("GitHub token validated successfully", tag = "GitHubApiClient")
                    GitHubTokenStatus.Valid
                }
                else -> {
                    Napier.w("GitHub token validation returned unexpected status: ${response.status}", tag = "GitHubApiClient")
                    GitHubTokenStatus.Error("Unexpected response: ${response.status}")
                }
            }
        } catch (e: Exception) {
            Napier.e("GitHub token validation failed", e, "GitHubApiClient")
            GitHubTokenStatus.Error(e.message ?: "Network error during validation")
        }
    }

    suspend fun getReleases(owner: String, repo: String, token: String? = null): Result<List<GitHubRelease>> {
        return try {
            val response = client.get("https://api.github.com/repos/$owner/$repo/releases") {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
                header("Accept", "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
                header("User-Agent", "MOCCA-Android-Client")
            }
            
            if (response.status == HttpStatusCode.NotFound) {
                return Result.failure(Exception("No releases found or repository not accessible (404). The repository may be private and your token may not have access."))
            }
            
            if (response.status == HttpStatusCode.Unauthorized) {
                 return Result.failure(Exception("Unauthorized access to repository (401). Your GitHub token may be expired or revoked."))
            }

            if (response.status == HttpStatusCode.Forbidden) {
                return Result.failure(Exception("Access forbidden (403). Rate limit exceeded or token lacks required permissions."))
            }
            
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("GitHub API Error: ${response.status}"))
            }
            
            Result.success(response.body())
        } catch (e: Exception) {
            Napier.e("Failed to fetch releases", e, "GitHubApiClient")
            Result.failure(e)
        }
    }

    // Expose client for streaming in repository
    fun getClient() = client
}
