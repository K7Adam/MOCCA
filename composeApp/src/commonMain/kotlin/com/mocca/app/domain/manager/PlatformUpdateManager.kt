package com.mocca.app.domain.manager

import io.ktor.utils.io.ByteReadChannel

interface PlatformUpdateManager {
    suspend fun saveApk(fileName: String, data: ByteReadChannel, contentLength: Long?, onProgress: suspend (Float) -> Unit): String
    fun installApk(path: String)
}
