package com.mocca.app.api

import android.os.Build
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

actual fun getHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        // 10 minutes to accommodate large APK downloads via auto-update
        // This must match or exceed Ktor's socketTimeoutMillis for downloads
        readTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
        writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        retryOnConnectionFailure(true)

        // Trust all certificates to allow connections to local servers/Tailscale IPs
        val trustAllCerts = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf<TrustManager>(trustAllCerts), SecureRandom())
        sslSocketFactory(sslContext.socketFactory, trustAllCerts)
        hostnameVerifier { _, _ -> true }


        // HTTP/2 Multiplexing & Connection Pooling
        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequestsPerHost = 16
            maxRequests = 32
        }
        dispatcher(dispatcher)
        
        connectionPool(okhttp3.ConnectionPool(10, 5, java.util.concurrent.TimeUnit.MINUTES))
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
 * - Android emulator uses 10.0.2.2.
 * - Physical devices default to empty string, prompting user to configure Tailscale IP.
 */
actual fun getPlatformDefaultHost(): String = if (isEmulator()) "10.0.2.2" else ""
