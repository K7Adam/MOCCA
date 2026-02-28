package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class WorktreeInfo(
    val id: String,
    val path: String,
    val branch: String? = null,
    val sessionID: String? = null,
    val status: WorktreeStatus = WorktreeStatus.UNKNOWN,
    val error: String? = null
)

@Serializable
enum class WorktreeStatus {
    @SerialName("ready") READY,
    @SerialName("creating") CREATING,
    @SerialName("failed") FAILED,
    @SerialName("unknown") UNKNOWN
}

@Serializable
@Immutable
data class WorktreeCreateRequest(
    val branch: String? = null,
    val sessionID: String? = null
)

@Serializable
@Immutable
data class WorktreeResetRequest(
    val id: String
)
