package com.mocca.app.api

import com.mocca.app.domain.model.GitHubRelease
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
import kotlinx.serialization.json.Json

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
                return Result.failure(Exception("No releases found or repository not accessible (404)"))
            }
            
            if (response.status == HttpStatusCode.Unauthorized) {
                 return Result.failure(Exception("Unauthorized access to repository (401). Check your token."))
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
