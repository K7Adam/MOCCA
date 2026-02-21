package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class FileSearchResult(
    val path: String,
    val name: String? = null
)

@Serializable
@Immutable
data class SymbolSearchResult(
    val name: String,
    val kind: String,
    val path: String,
    val line: Int? = null,
    val column: Int? = null
)

@Serializable
@Immutable
data class TextSearchResult(
    val path: String,
    val line: Int,
    val content: String,
    val matches: List<TextMatch> = emptyList()
)

@Serializable
@Immutable
data class TextMatch(
    val start: Int,
    val end: Int
)
