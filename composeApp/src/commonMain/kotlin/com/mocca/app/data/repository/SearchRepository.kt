package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.SearchResult
import com.mocca.app.domain.model.SymbolResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SearchRepository(
    private val apiClient: MoccaApiClient
) {
    fun searchFiles(pattern: String): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading())
        
        apiClient.findFiles(pattern).fold(
            onSuccess = { results ->
                emit(Resource.Success(results))
            },
            onFailure = { error ->
                Napier.e("File search failed", error)
                emit(Resource.Error(error.message ?: "File search failed"))
            }
        )
    }

    fun searchSymbols(query: String): Flow<Resource<List<SymbolResult>>> = flow {
        emit(Resource.Loading())
        
        apiClient.findSymbols(query).fold(
            onSuccess = { results ->
                emit(Resource.Success(results))
            },
            onFailure = { error ->
                Napier.e("Symbol search failed", error)
                emit(Resource.Error(error.message ?: "Symbol search failed"))
            }
        )
    }

    fun searchText(query: String, path: String = ""): Flow<Resource<List<SearchResult>>> = flow {
        emit(Resource.Loading())
        
        apiClient.searchText(query, path).fold(
            onSuccess = { results ->
                emit(Resource.Success(results))
            },
            onFailure = { error ->
                Napier.e("Text search failed", error)
                emit(Resource.Error(error.message ?: "Text search failed"))
            }
        )
    }
}
