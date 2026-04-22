package com.mocca.app.data.repository

import com.mocca.app.bridge.client.NativeCliUnavailableException
import com.mocca.app.bridge.client.requestPayload
import com.mocca.app.bridge.client.requireClient
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.opencode.BridgeResponseException
import com.mocca.app.domain.model.ApiSearchResult
import com.mocca.app.domain.model.ApiSymbolResult
import com.mocca.app.domain.model.FileContent
import com.mocca.app.domain.model.FileInfo
import com.mocca.app.domain.model.FileStatus
import com.mocca.app.domain.model.Resource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class FileRepository(
    private val bridgeConnectionManager: BridgeConnectionManager,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    fun listFiles(path: String = ""): Flow<Resource<List<FileInfo>>> = flow {
        emit(Resource.Loading())
        try {
            val client = bridgeConnectionManager.requireClient("fs.list")
            val files = client.requestPayload<List<FileInfo>>(
                ns = "fs",
                action = "list",
                payload = json.encodeToJsonElement(FsPathRequest(path)),
                json = json
            )
            emit(Resource.Success(files))
        } catch (error: Exception) {
            Napier.e("Failed to list files through MOCCA CLI", error)
            emit(Resource.Error(error.toResourceMessage("Failed to list files")))
        }
    }.flowOn(Dispatchers.Default)

    suspend fun getFileContent(path: String): Resource<FileContent> {
        return try {
            val client = bridgeConnectionManager.requireClient("fs.read")
            Resource.Success(
                client.requestPayload(
                    ns = "fs",
                    action = "read",
                    payload = json.encodeToJsonElement(FsReadRequest(path)),
                    json = json
                )
            )
        } catch (error: Exception) {
            Napier.e("Failed to get file content through MOCCA CLI", error)
            Resource.Error(error.toResourceMessage("Failed to get file content"))
        }
    }

    suspend fun saveFile(path: String, content: String): Resource<Unit> {
        return try {
            val client = bridgeConnectionManager.requireClient("fs.write")
            client.requestPayload<FsOperationResult>(
                ns = "fs",
                action = "write",
                payload = json.encodeToJsonElement(FsWriteRequest(path = path, content = content)),
                json = json
            )
            Resource.Success(Unit)
        } catch (error: Exception) {
            Napier.e("Failed to save file through MOCCA CLI", error)
            Resource.Error(error.toResourceMessage("Failed to save file"))
        }
    }

    suspend fun getFileStatus(path: String): Resource<FileStatus> {
        return Resource.Success(FileStatus(path = path))
    }

    fun searchText(query: String, path: String = ""): Flow<Resource<List<ApiSearchResult>>> = flow {
        emit(Resource.Loading())
        try {
            val client = bridgeConnectionManager.requireClient("fs.search")
            val results = client.requestPayload<List<ApiSearchResult>>(
                ns = "fs",
                action = "search",
                payload = json.encodeToJsonElement(FsSearchRequest(query = query, path = path)),
                json = json
            )
            emit(Resource.Success(results))
        } catch (error: Exception) {
            Napier.e("Failed to search text through MOCCA CLI", error)
            emit(Resource.Error(error.toResourceMessage("Search failed")))
        }
    }.flowOn(Dispatchers.Default)

    fun findFiles(pattern: String): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading())
        try {
            val client = bridgeConnectionManager.requireClient("fs.find")
            val files = client.requestPayload<List<String>>(
                ns = "fs",
                action = "find",
                payload = json.encodeToJsonElement(FsFindRequest(pattern = pattern)),
                json = json
            )
            emit(Resource.Success(files))
        } catch (error: Exception) {
            Napier.e("Failed to find files through MOCCA CLI", error)
            emit(Resource.Error(error.toResourceMessage("Find failed")))
        }
    }.flowOn(Dispatchers.Default)

    fun findSymbols(query: String): Flow<Resource<List<ApiSymbolResult>>> = flow {
        emit(Resource.Loading())
        emit(Resource.Success(emptyList()))
    }

    private fun Exception.toResourceMessage(fallback: String): String {
        return when (this) {
            is NativeCliUnavailableException -> message ?: "MOCCA CLI bridge is not connected"
            is BridgeResponseException -> message ?: fallback
            else -> message ?: fallback
        }
    }
}

@Serializable
private data class FsPathRequest(val path: String = "")

@Serializable
private data class FsReadRequest(
    val path: String,
    val encoding: String = "utf8"
)

@Serializable
private data class FsWriteRequest(
    val path: String,
    val content: String
)

@Serializable
private data class FsSearchRequest(
    val query: String,
    val path: String = "",
    val maxResults: Int = 100
)

@Serializable
private data class FsFindRequest(
    val pattern: String,
    val path: String = ""
)

@Serializable
private data class FsOperationResult(
    val success: Boolean = false,
    val path: String? = null
)
