package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class SearchMode {
    FILE_PATTERN,
    TEXT_CONTENT,
    SYMBOL
}

@Serializable
@Immutable
data class SearchQuery(
    val query: String,
    val mode: SearchMode = SearchMode.TEXT_CONTENT,
    val path: String = "",
    val contextLines: Int = 3
)

@Serializable
@Immutable
data class FileSearchResult(
    val path: String,
    val name: String? = null
)

@Serializable
@Immutable
data class ContextLine(
    val lineNumber: Int,
    val content: String
)

@Serializable
@Immutable
data class FileContentSearchResult(
    val path: String,
    val line: Int,
    val content: String,
    val contextBefore: List<ContextLine> = emptyList(),
    val contextAfter: List<ContextLine> = emptyList()
)

@Serializable
@Immutable
data class ApiSearchResult(
    val file: String,
    val line: Int,
    val match: String,
    val context: String = ""
)

@Serializable
@Immutable
data class ApiSymbolResult(
    val name: String,
    val kind: String,
    val path: String,
    val line: Int? = null,
    val column: Int? = null
)

@Immutable
data class UnifiedSearchResult(
    val mode: SearchMode,
    val fileResults: List<FileSearchResult> = emptyList(),
    val textResults: List<FileContentSearchResult> = emptyList(),
    val symbolResults: List<ApiSymbolResult> = emptyList()
)
