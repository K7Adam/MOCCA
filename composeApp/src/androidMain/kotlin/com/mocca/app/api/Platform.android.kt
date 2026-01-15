package com.mocca.app.api

import android.os.Build
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

actual fun getHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        retryOnConnectionFailure(true)
    }
}

actual fun getDefaultConfigPath(): String = "/data/data/com.mocca.app/files/config"

/**
 * Detect if running on an Android emulator.
 */
fun isEmulator(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
            || "google_sdk" == Build.PRODUCT
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu"))
}

/**
 * Platform-specific default host for connecting to OpenCode server.
 * - Android emulator uses 10.0.2.2 to reach host machine's localhost.
 * - Physical devices default to empty string, prompting user to configure Tailscale IP.
 */
actual fun getPlatformDefaultHost(): String = if (isEmulator()) "10.0.2.2" else ""
