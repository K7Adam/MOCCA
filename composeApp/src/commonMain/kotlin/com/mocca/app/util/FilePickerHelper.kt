package com.mocca.app.util

import com.mocca.app.domain.model.AttachedFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlin.time.Clock

/**
 * Helper for file picker operations.
 * Defines allowed file types and converts PlatformFile to AttachedFile.
 */
object FilePickerHelper {
    
    /**
     * Allowed file extensions for attachments.
     * Based on opencode's file handling capabilities.
     */
    private val CODE_EXTENSIONS = listOf(
        "kt", "java", "py", "js", "ts", "tsx", "jsx", "go", "rs", "c", "cpp", "h", "hpp",
        "swift", "rb", "php", "scala", "groovy", "sh", "bash", "zsh", "ps1", "bat", "cmd"
    )
    
    private val CONFIG_EXTENSIONS = listOf(
        "txt", "md", "json", "yaml", "yml", "toml", "xml", "html", "css", "scss", "less",
        "csv", "ini", "conf", "cfg", "env", "properties", "gradle", "kts"
    )
    
    private val IMAGE_EXTENSIONS = listOf(
        "png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "ico"
    )
    
    private val DOCUMENT_EXTENSIONS = listOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    )
    
    val ALLOWED_EXTENSIONS: List<String> = 
        CODE_EXTENSIONS + CONFIG_EXTENSIONS + IMAGE_EXTENSIONS + DOCUMENT_EXTENSIONS
    
    /**
     * Create a FileKitType for the file picker with allowed extensions.
     */
    fun createFileType(): FileKitType = FileKitType.File(
        extensions = ALLOWED_EXTENSIONS
    )
    
    /**
     * Get MIME type from file extension.
     */
    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            // Images
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "bmp" -> "image/bmp"
            "ico" -> "image/x-icon"
            
            // Documents  
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            
            // Code/Config
            "json" -> "application/json"
            "xml" -> "application/xml"
            "html" -> "text/html"
            "css" -> "text/css"
            "js", "ts", "tsx", "jsx" -> "text/javascript"
            "md" -> "text/markdown"
            "yaml", "yml" -> "text/yaml"
            
            // Default text
            else -> "text/plain"
        }
    }
    
    /**
     * Convert a PlatformFile to AttachedFile for use in the app.
     * Uses FileKit's extension functions for file properties.
     */
    suspend fun toAttachedFile(file: PlatformFile): AttachedFile {
        val fileName = file.name
        val extension = fileName.substringAfterLast('.', "")
        val mimeType = getMimeType(extension)
        val fileSize = file.size()
        val filePath = file.path
        
        // For images, create a data URL with base64 content
        val dataUrl = if (mimeType.startsWith("image/") && fileSize < 5 * 1024 * 1024) { // < 5MB
            try {
                val bytes = file.readBytes()
                val base64 = bytesToBase64(bytes)
                "data:$mimeType;base64,$base64"
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        
        return AttachedFile(
            id = "${Clock.System.now().toEpochMilliseconds()}-${fileName.hashCode()}",
            name = fileName,
            path = filePath,
            mimeType = mimeType,
            sizeBytes = fileSize,
            dataUrl = dataUrl
        )
    }
}

// Platform-specific base64 encoding
expect fun bytesToBase64(bytes: ByteArray): String
