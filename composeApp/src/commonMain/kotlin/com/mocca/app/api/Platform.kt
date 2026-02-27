package com.mocca.app.api

import com.mocca.app.domain.model.ServerConfig
import io.ktor.client.engine.*

/**
 * Platform-specific HTTP engine provider.
 */
expect fun getHttpEngine(): HttpClientEngine

/**
 * Platform-specific default server configuration storage path.
 */
expect fun getDefaultConfigPath(): String

/**
 * Platform-specific default host for connecting to OpenCode server.
 * Android emulator uses NetworkConfig.DEFAULT_HOST_IP
 * Other platforms use localhost directly.
 */
expect fun getPlatformDefaultHost(): String
