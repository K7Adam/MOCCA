package com.mocca.app.domain.manager

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
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
        val file = File(context.externalCacheDir ?: context.cacheDir, fileName)
        val output = FileOutputStream(file)
        val input = data.toInputStream()
        val buffer = ByteArray(8 * 1024)
        var bytesCopied: Long = 0
        var bytes: Int = input.read(buffer)
        
        val total = contentLength ?: -1L

        while (bytes >= 0) {
            output.write(buffer, 0, bytes)
            bytesCopied += bytes
            
            if (total > 0) {
                onProgress(bytesCopied.toFloat() / total)
            }
            
            bytes = input.read(buffer)
        }
        
        output.close()
        input.close()
        
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

        context.startActivity(intent)
    }
}
