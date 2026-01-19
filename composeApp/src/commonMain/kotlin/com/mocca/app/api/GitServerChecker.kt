package com.mocca.app.api

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withTimeout
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.system.measureTimeMillis

/**
 * Git Server Checker - Fast connectivity check utility.
 */
object GitServerChecker {
    private const val TAG = "GitServerChecker"
    private const val QUICK_CHECK_TIMEOUT_MS = 500L
    private const val CONNECTION_TIMEOUT_MS = 1_000L

    suspend fun checkServerRunning(
        httpClient: HttpClient,
        url: String
    ): Triple<Boolean?, String, Long?> {
        var responseTime: Long = 0
        return try {
            withTimeout(CONNECTION_TIMEOUT_MS) {
                var status: HttpStatusCode? = null
                responseTime = measureTimeMillis {
                    val response = httpClient.get(url) {
                        timeout {
                            requestTimeoutMillis = QUICK_CHECK_TIMEOUT_MS
                            connectTimeoutMillis = QUICK_CHECK_TIMEOUT_MS
                        }
                    }
                    status = response.status
                }

                when (status) {
                    HttpStatusCode.OK -> {
                        Triple(true, "Git server is running", responseTime)
                    }
                    HttpStatusCode.NotFound -> {
                        Triple(false, "Git server not found (endpoint missing)", null)
                    }
                    else -> {
                        Triple(false, "Git server error: $status", null)
                    }
                }
            }
        } catch (e: ConnectException) {
            Napier.d("$TAG: Connection refused - server not running")
            Triple(false, "Git server is not running", null)
        } catch (e: SocketTimeoutException) {
            Napier.w("$TAG: Connection timed out - server may be slow or not listening")
            Triple(null, "Git server not responding (timeout)", null)
        } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
            // Handle Ktor's HttpRequestTimeoutException - this is expected when server is not running
            Napier.d("$TAG: Quick check timeout - server not available (${e.message})")
            Triple(false, "Git server is not running", null)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            // Handle withTimeout cancellation - server didn't respond within 1s
            Napier.d("$TAG: Connection timeout - server not responding")
            Triple(null, "Git server not responding (timeout)", null)
        } catch (e: Exception) {
            Napier.e("$TAG: Unexpected error: ${e.message}", e)
            Triple(false, "Connection failed: ${e.message}", null)
        }
    }

    fun isServerAvailable(checkResult: Triple<Boolean?, String, Long?>): Boolean {
        return checkResult.first == true
    }

    fun isServerUnavailable(checkResult: Triple<Boolean?, String, Long?>): Boolean {
        return checkResult.first == false && checkResult.second.contains("not running")
    }

    fun isServerSlow(checkResult: Triple<Boolean?, String, Long?>): Boolean {
        return checkResult.third != null && checkResult.third!! > 1_000L
    }

    fun getStatusMessage(checkResult: Triple<Boolean?, String, Long?>): String {
        return when {
            isServerAvailable(checkResult) -> {
                if (isServerSlow(checkResult)) {
                    "Git server is running (slow response: ${checkResult.third}ms)"
                } else {
                    "Git server is running and responding"
                }
            }
            isServerUnavailable(checkResult) -> "Git server is not running"
            else -> "Cannot connect to Git server (connection timeout)"
        }
    }

    suspend fun checkPort(
        httpClient: HttpClient,
        host: String,
        port: Int
    ): Triple<Boolean?, String, Long?> {
        val url = "http://$host:$port/health"
        return checkServerRunning(httpClient, url)
    }
}
