package com.mocca.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FileSearchResult(
    val path: String,
    val name: String? = null
)

@Serializable
data class SymbolSearchResult(
    val name: String,
    val kind: String,
    val path: String,
    val line: Int? = null,
    val column: Int? = null
)

@Serializable
data class TextSearchResult(
    val path: String,
    val line: Int,
    val content: String,
    val matches: List<TextMatch> = emptyList()
)

@Serializable
data class TextMatch(
    val start: Int,
    val end: Int
)
