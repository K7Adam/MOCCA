package com.mocca.app.api

/**
 * Sealed class for network-related errors.
 * Provides a unified error handling mechanism across the app.
 */
sealed class NetworkError(override val message: String, override val cause: Throwable? = null) : Exception(message, cause) {
    class ServerError(val statusCode: Int, message: String) : NetworkError("Server Error $statusCode: $message")
    class ConnectionError(message: String, cause: Throwable? = null) : NetworkError(message, cause)
    class TimeoutError(message: String, cause: Throwable? = null) : NetworkError(message, cause)
    class Unknown(message: String, cause: Throwable? = null) : NetworkError(message, cause)

    companion object {
        fun from(e: Throwable): NetworkError {
            return when (e) {
                is NetworkError -> e
                is io.ktor.client.plugins.ServerResponseException -> ServerError(e.response.status.value, e.message)
                is io.ktor.client.plugins.ClientRequestException -> ServerError(e.response.status.value, e.message)
                is io.ktor.client.network.sockets.ConnectTimeoutException,
                is io.ktor.client.network.sockets.SocketTimeoutException,
                is io.ktor.client.plugins.HttpRequestTimeoutException -> TimeoutError("Connection timed out", e)
                is java.net.ConnectException,
                is java.net.SocketException,
                is java.net.UnknownHostException -> ConnectionError("Connection failed: ${e.message}", e)
                else -> Unknown(e.message ?: "Unknown error", e)
            }
        }
    }
}
