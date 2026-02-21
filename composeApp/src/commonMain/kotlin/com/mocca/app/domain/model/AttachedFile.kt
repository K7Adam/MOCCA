package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.Serializable

/**
 * Represents a file attached to a chat message.
 * Used for image attachments, documents, etc.
 */
@Serializable
@Immutable
data class AttachedFile(
    val id: String,
    val name: String,
    val path: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    /** Base64 data URL for inline content (e.g., images) */
    val dataUrl: String? = null
) {
    val isImage: Boolean
        get() = mimeType.startsWith("image/")
    
    val displaySize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> "${sizeBytes / (1024 * 1024)} MB"
        }
    
    /**
     * Convert to ChatPart.File for sending via API.
     */
    fun toChatPart(): ChatPart.File {
        val url = dataUrl ?: "file://$path"
        return ChatPart.File(
            mime = mimeType,
            url = url,
            filename = name
        )
    }
}
