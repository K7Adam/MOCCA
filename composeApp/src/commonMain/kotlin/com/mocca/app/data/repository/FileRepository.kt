package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FileRepository(
    private val apiClient: MoccaApiClient
) {
    fun listFiles(path: String = ""): Flow<Resource<List<FileInfo>>> = flow {
        emit(Resource.Loading())
        
        val result = apiClient.listFiles(path)
        result.fold(
            onSuccess = { files ->
                emit(Resource.Success(files))
            },
            onFailure = { error ->
                Napier.e("Failed to list files", error)
                emit(Resource.Error(error.message ?: "Failed to list files"))
            }
        )
    }

    suspend fun getFileContent(path: String): Resource<FileContent> {
        return apiClient.getFileContent(path).fold(
            onSuccess = { content ->
                Resource.Success(content)
            },
            onFailure = { error ->
                Napier.e("Failed to get file content", error)
                Resource.Error(error.message ?: "Failed to get file content")
            }
        )
    }

    suspend fun saveFile(path: String, content: String): Resource<Unit> {
        return apiClient.updateFile(path, content).fold(
            onSuccess = { Resource.Success(it) },
            onFailure = { error ->
                Napier.e("Failed to save file", error)
                Resource.Error(error.message ?: "Failed to save file")
            }
        )
    }

    suspend fun getFileStatus(path: String): Resource<FileStatus> {
        return apiClient.getFileStatus(path).fold(
            onSuccess = { status ->
                Resource.Success(status)
            },
            onFailure = { error ->
                Napier.e("Failed to get file status", error)
                Resource.Error(error.message ?: "Failed to get file status")
            }
        )
    }

    fun searchText(query: String, path: String = ""): Flow<Resource<List<ApiSearchResult>>> = flow {
        emit(Resource.Loading())
        
        val result = apiClient.searchText(query, path)
        result.fold(
            onSuccess = { results ->
                emit(Resource.Success(results))
            },
            onFailure = { error ->
                Napier.e("Failed to search text", error)
                emit(Resource.Error(error.message ?: "Search failed"))
            }
        )
    }

    fun findFiles(pattern: String): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading())
        
        val result = apiClient.findFiles(pattern)
        result.fold(
            onSuccess = { files ->
                emit(Resource.Success(files))
            },
            onFailure = { error ->
                Napier.e("Failed to find files", error)
                emit(Resource.Error(error.message ?: "Find failed"))
            }
        )
    }

    fun findSymbols(query: String): Flow<Resource<List<ApiSymbolResult>>> = flow {
        emit(Resource.Loading())
        
        val result = apiClient.findSymbols(query)
        result.fold(
            onSuccess = { symbols ->
                emit(Resource.Success(symbols))
            },
            onFailure = { error ->
                Napier.e("Failed to find symbols", error)
                emit(Resource.Error(error.message ?: "Symbol search failed"))
            }
        )
    }
}
