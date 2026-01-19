package com.mocca.app.api

import com.mocca.app.domain.model.GitHubRelease
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.readRemaining
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json

class GitHubApiClient {
    private val client = HttpClient(getHttpEngine()) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getReleases(owner: String, repo: String): Result<List<GitHubRelease>> {
        return try {
            val response = client.get("https://api.github.com/repos/$owner/$repo/releases")
            
            if (response.status == HttpStatusCode.NotFound) {
                return Result.failure(Exception("No releases found"))
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

    suspend fun downloadFile(url: String, destination: Path, onProgress: (Float) -> Unit): Result<Path> {
        return try {
            client.prepareGet(url).execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.bodyAsChannel()
                val totalBytes = httpResponse.headers["Content-Length"]?.toLong() ?: -1L
                var bytesRead = 0L
                
                // Using SystemFileSystem from kotlinx-io (or okio if available, but let's stick to simple file IO for now if possible)
                // Since we are in commonMain, we need a way to write to file.
                // composeApp dependencies include `okio` implicitly via Ktor? No, maybe not.
                // Actually, Ktor has file support but it's platform specific.
                // Let's defer the actual writing to a platform-specific implementation or Repository 
                // that has access to FileSystem.
                // But wait, Ktor 3.0 supports kotlinx-io.
                
                // For now, I'll return the channel or handle it in Repository.
                // Let's implement the writing here if we can, or just return the channel.
                // But the interface says downloadFile...
                
                // Let's use a simpler approach: Pass a callback for writing bytes?
                // No, let's keep it simple. I'll just stream it to a buffer in the repository 
                // or use a platform-specific Sink.
                
                // REVISION: I'll move download logic to Repository which can inject a FileSystem wrapper.
                throw NotImplementedError("Download moved to Repository")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Expose client for streaming in repository
    fun getClient() = client
}
