package com.mocca.app.domain.manager

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidUpdateManager(private val context: Context) : PlatformUpdateManager {

    override suspend fun saveApk(
        fileName: String,
        data: ByteReadChannel,
        contentLength: Long?,
        onProgress: suspend (Float) -> Unit
    ): String = withContext(Dispatchers.IO) {
        // Use externalCacheDir if available, otherwise cacheDir.
        // Ensure this matches 'external-cache-path' or 'cache-path' in provider_paths.xml
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val file = File(cacheDir, "update.apk") // Fixed name to avoid path traversal issues
        
        // Delete existing file to ensure clean write
        if (file.exists()) {
            file.delete()
        }
        
        val output = FileOutputStream(file)
        val buffer = ByteArray(8 * 1024)
        var bytesCopied: Long = 0
        val total = contentLength ?: -1L

        try {
            while (true) {
                val bytesRead = data.readAvailable(buffer, 0, buffer.size)
                if (bytesRead < 0) break // EOF
                
                if (bytesRead > 0) {
                    output.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead
                    if (total > 0) {
                        onProgress(bytesCopied.toFloat() / total)
                    }
                }
            }
            output.flush()
        } finally {
            output.close()
        }
        
        return@withContext file.absolutePath
    }

    override fun installApk(path: String) {
        val file = File(path)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback or notify user
        }
    }
}
