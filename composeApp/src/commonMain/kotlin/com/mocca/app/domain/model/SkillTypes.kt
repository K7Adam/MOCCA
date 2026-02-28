package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class SkillInfo(
    val name: String,
    val description: String? = null,
    val system: String? = null,
    val tags: List<String> = emptyList()
)
