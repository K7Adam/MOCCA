package com.mocca.app.api

import io.ktor.client.HttpClient

/**
 * Abstraction for executing HTTP requests against the current connection.
 *
 * Consumers never hold an HttpClient reference directly. They call [execute]
 * which delegates to the current, properly-configured HttpClient managed by
 * [ConnectionManager]. This eliminates stale-client bugs when server config changes.
 */
interface ApiExecutor {

    /**
     * Execute a request against the current HttpClient.
     *
     * @throws ConnectionException if not connected or the client is unavailable.
     */
    suspend fun <T> execute(block: suspend HttpClient.() -> T): T
}

/**
 * Thrown when attempting to execute a request while not connected.
 */
class ConnectionException(message: String, cause: Throwable? = null) : Exception(message, cause)
