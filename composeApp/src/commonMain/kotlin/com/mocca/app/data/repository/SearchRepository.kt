package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.ContextLine
import com.mocca.app.domain.model.FileContentSearchResult
import com.mocca.app.domain.model.FileSearchResult
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.ApiSearchResult
import com.mocca.app.domain.model.SearchMode
import com.mocca.app.domain.model.SearchQuery
import com.mocca.app.domain.model.ApiSymbolResult
import com.mocca.app.domain.model.UnifiedSearchResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SearchRepository(
    private val apiClient: MoccaApiClient
) {

    private fun String.toFileSearchResult(): FileSearchResult {
        val normalizedPath = replace('\\', '/')
        return FileSearchResult(
            path = this,
            name = normalizedPath.substringAfterLast('/', normalizedPath)
        )
    }

    // --- Existing search methods ---

    fun searchFiles(pattern: String): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading())
        apiClient.findFiles(pattern).fold(
            onSuccess = { results -> emit(Resource.Success(results)) },
            onFailure = { error ->
                Napier.e("File search failed", error)
                emit(Resource.Error(error.message ?: "File search failed"))
            }
        )
    }.flowOn(Dispatchers.IO)

    fun searchSymbols(query: String): Flow<Resource<List<ApiSymbolResult>>> = flow {
        emit(Resource.Loading())
        apiClient.findSymbols(query).fold(
            onSuccess = { results -> emit(Resource.Success(results)) },
            onFailure = { error ->
                Napier.e("Symbol search failed", error)
                emit(Resource.Error(error.message ?: "Symbol search failed"))
            }
        )
    }.flowOn(Dispatchers.IO)

    fun searchText(query: String, path: String = ""): Flow<Resource<List<ApiSearchResult>>> = flow {
        emit(Resource.Loading())
        apiClient.searchText(query, path).fold(
            onSuccess = { results -> emit(Resource.Success(results)) },
            onFailure = { error ->
                Napier.e("Text search failed", error)
                emit(Resource.Error(error.message ?: "Text search failed"))
            }
        )
    }.flowOn(Dispatchers.IO)

    // --- T26: Enhanced glob/grep search methods ---

    /**
     * Glob-pattern file search. Wraps [searchFiles] and converts raw paths
     * into [FileSearchResult] for consistent UI rendering.
     */
    fun searchGlob(pattern: String): Flow<Resource<List<FileSearchResult>>> = flow {
        emit(Resource.Loading())
        apiClient.findFiles(pattern).fold(
            onSuccess = { paths ->
                val results = paths.map { rawPath -> rawPath.toFileSearchResult() }
                emit(Resource.Success(results))
            },
            onFailure = { error ->
                Napier.e("Glob search failed", error)
                emit(Resource.Error(error.message ?: "Glob search failed"))
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Grep-style content search backed by /find.
     * Surrounding context is resolved locally from the matching file content.
     */
    fun searchGrep(
        query: String,
        path: String = "",
        contextLines: Int = 3
    ): Flow<Resource<List<FileContentSearchResult>>> = flow {
        emit(Resource.Loading())
        apiClient.searchGrep(query, path).fold(
            onSuccess = { results ->
                emit(Resource.Success(results.toContentSearchResults(contextLines)))
            },
            onFailure = { error ->
                Napier.e("Grep search failed", error)
                emit(Resource.Error(error.message ?: "Grep search failed"))
            }
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Unified search that dispatches to the appropriate backend method
     * based on [SearchQuery.mode] and returns a [UnifiedSearchResult].
     */
    fun search(query: SearchQuery): Flow<Resource<UnifiedSearchResult>> = flow {
        emit(Resource.Loading())
        when (query.mode) {
            SearchMode.FILE_PATTERN -> {
                apiClient.findFiles(query.query).fold(
                    onSuccess = { paths ->
                        val fileResults = paths.map { rawPath -> rawPath.toFileSearchResult() }
                        emit(Resource.Success(UnifiedSearchResult(
                            mode = query.mode,
                            fileResults = fileResults
                        )))
                    },
                    onFailure = { error ->
                        Napier.e("Unified file search failed", error)
                        emit(Resource.Error(error.message ?: "File pattern search failed"))
                    }
                )
            }
            SearchMode.TEXT_CONTENT -> {
                apiClient.searchGrep(query.query, query.path)
                    .fold(
                        onSuccess = { results ->
                            emit(Resource.Success(UnifiedSearchResult(
                                mode = query.mode,
                                textResults = results.toContentSearchResults(query.contextLines)
                            )))
                        },
                        onFailure = { error ->
                            Napier.e("Unified text search failed", error)
                            emit(Resource.Error(error.message ?: "Text search failed"))
                        }
                    )
            }
            SearchMode.SYMBOL -> {
                apiClient.findSymbols(query.query).fold(
                    onSuccess = { results ->
                        emit(Resource.Success(UnifiedSearchResult(
                            mode = query.mode,
                            symbolResults = results
                        )))
                    },
                    onFailure = { error ->
                        Napier.e("Unified symbol search failed", error)
                        emit(Resource.Error(error.message ?: "Symbol search failed"))
                    }
                )
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Convenience suspend function to fetch file content for expanding context
     * around a search match. Delegates to [MoccaApiClient.getFileContent].
     */
    suspend fun fetchFileContext(filePath: String, aroundLine: Int, contextSize: Int = 3): Resource<List<String>> =
        withContext(Dispatchers.IO) {
            apiClient.getFileContent(filePath).fold(
                onSuccess = { content ->
                    val lines = content.content.lines()
                    val start = maxOf(0, aroundLine - contextSize - 1)
                    val end = minOf(lines.size, aroundLine + contextSize)
                    Resource.Success(lines.subList(start, end))
                },
                onFailure = { error ->
                    Napier.e("Context fetch failed for $filePath", error)
                    Resource.Error(error.message ?: "Failed to fetch file context")
                }
            )
        }

    private suspend fun List<ApiSearchResult>.toContentSearchResults(
        contextSize: Int
    ): List<FileContentSearchResult> {
        if (isEmpty()) return emptyList()
        if (contextSize <= 0) {
            return map { result ->
                FileContentSearchResult(
                    path = result.file,
                    line = result.line,
                    content = result.match.ifBlank { result.context }
                )
            }
        }

        return map { result ->
            val fileContent = apiClient.getFileContent(result.file).getOrNull()?.content
            val lines = fileContent?.lines().orEmpty()
            val targetIndex = (result.line - 1).coerceAtLeast(0)
            val beforeStart = (targetIndex - contextSize).coerceAtLeast(0)
            val afterEnd = (targetIndex + contextSize).coerceAtMost(lines.lastIndex.takeIf { it >= 0 } ?: 0)

            val contextBefore = if (lines.isEmpty()) {
                emptyList()
            } else {
                (beforeStart until targetIndex.coerceAtMost(lines.size)).map { index ->
                    ContextLine(lineNumber = index + 1, content = lines[index])
                }
            }
            val contextAfter = if (lines.isEmpty() || targetIndex >= afterEnd) {
                emptyList()
            } else {
                ((targetIndex + 1)..afterEnd).map { index ->
                    ContextLine(lineNumber = index + 1, content = lines[index])
                }
            }

            FileContentSearchResult(
                path = result.file,
                line = result.line,
                content = result.match.ifBlank { result.context },
                contextBefore = contextBefore,
                contextAfter = contextAfter
            )
        }
    }
}
