package com.mocca.app.di

import com.mocca.app.api.GitHubApiClient
import com.mocca.app.api.GitApiClient
import com.mocca.app.api.HttpClientProvider
import com.mocca.app.api.MoccaApiClient
import com.mocca.app.api.MoccaSseClient
import com.mocca.app.api.RetryPolicy
import com.mocca.app.data.GlobalActivityManager
import com.mocca.app.data.local.LocalCache
import com.mocca.app.data.local.LocalCacheFactory
import com.mocca.app.data.repository.*
import com.mocca.app.data.repository.AppConnectionManager
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.ui.screens.chat.ChatScreenModel
import com.mocca.app.ui.screens.files.FilesScreenModel
import com.mocca.app.ui.screens.git.GitDiffScreenModel
import com.mocca.app.ui.screens.git.GitScreenModel
import com.mocca.app.ui.screens.main.MainScreenModel
import com.mocca.app.ui.screens.mcp.McpScreenModel
import com.mocca.app.ui.screens.onboarding.OnboardingScreenModel
import com.mocca.app.ui.screens.panels.DashboardScreenModel
import com.mocca.app.ui.screens.sessions.SessionsScreenModel
import com.mocca.app.ui.screens.settings.SettingsScreenModel
import com.mocca.app.ui.screens.terminal.TerminalScreenModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Common Koin modules shared across all platforms.
 */
val commonModule = module {
    // Dynamic HttpClient Provider - recreates client when server config changes
    single {
        HttpClientProvider(
            serverConfigRepository = get(),
            networkObserver = getOrNull()
        )
    }
    
    // API Clients - use HttpClientProvider for dynamic client access
    single {
        val httpClientProvider: HttpClientProvider = get()
        val serverConfigRepo: ServerConfigRepository = get()
        MoccaApiClient(
            httpClient = httpClientProvider.getClientSync(),
            serverConfigProvider = { serverConfigRepo.getActiveServerConfig() },
            retryPolicy = RetryPolicy.Default,
            httpClientProvider = httpClientProvider
        )
    }
    
    single {
        val httpClientProvider: HttpClientProvider = get()
        val serverConfigRepo: ServerConfigRepository = get()
        MoccaSseClient(
            httpClient = httpClientProvider.getClientSync(),
            serverConfigProvider = { serverConfigRepo.getActiveServerConfig() },
            retryPolicy = RetryPolicy.Aggressive,
            httpClientProvider = httpClientProvider
        )
    }
    
    // Repositories
    single { SessionRepository(get(), get()) }
    single { 
        EventStreamRepository(
            sseClient = get(),
            httpClientProvider = get(),
            networkObserver = getOrNull(),
            localCache = get(),
            apiClient = get()
        )
    }
    
    // Services
    single {
        val httpClientProvider: HttpClientProvider = get()
        val serverConfigRepo: ServerConfigRepository = get()
        GitApiClient(
            httpClientProvider = httpClientProvider,
            serverConfigProvider = { serverConfigRepo.getActiveServerConfig() }
        )
    }
    
    singleOf(::FileRepository)
    singleOf(::TerminalRepository)
    single { GitRepository(get(), get()) }
    single { McpRepository(get()) }
    singleOf(::SettingsRepository)
    single { ConfigRepository(get()) }
    singleOf(::UpdateRepository)
    singleOf(::GitHubApiClient)
    
    // Global Activity Manager - singleton for tracking background activity
    single { GlobalActivityManager() }
    
    // New repositories for OpenCode features
    singleOf(::ProviderRepository)
    singleOf(::AgentRepository)
    singleOf(::ToolRepository)
    singleOf(::CommandRepository)
    singleOf(::FormatterRepository)
    singleOf(::LspRepository)
    singleOf(::SearchRepository)
    singleOf(::ProjectRepository)
    
    single {
        AppConnectionManager(
            serverConfigRepository = get(),
            sessionRepository = get(),
            eventStreamRepository = get(),
            networkObserver = getOrNull()
        )
    }
}

/**
 * Cache module - platform-specific implementations provide LocalCacheFactory.
 */
val cacheModule = module {
    single<LocalCache> {
        val factory: LocalCacheFactory = get()
        factory.create()
    }
    
    single {
        ServerConfigRepository(get())
    }
}

/**
 * ScreenModel/ViewModel module for presentation layer.
 */
val screenModelModule = module {
    // Sessions screen
    factory {
        SessionsScreenModel(
            sessionRepository = get(),
            appConnectionManager = get()
        )
    }
    
    // Chat screen - accepts optional initialSessionId parameter
    factory { params ->
        ChatScreenModel(
            initialSessionId = params.getOrNull(),
            sessionRepository = get(),
            eventStreamRepository = get(),
            commandRepository = get(),
            agentRepository = get()
        )
    }
    
    // Files screen
    factoryOf(::FilesScreenModel)
    
    // Settings screen
    factory {
        SettingsScreenModel(
            serverConfigRepository = get(),
            appConnectionManager = get(),
            updateRepository = get(),
            settingsRepository = get(),
            configRepository = get()
        )
    }
    
    // Terminal screen
    factoryOf(::TerminalScreenModel)
    
    // Git screen
    factoryOf(::GitScreenModel)
    
    // Git Diff screen
    factory { params ->
        GitDiffScreenModel(
            gitRepository = get()
        )
    }
    
    // MCP screen
    factory {
        McpScreenModel(
            mcpRepository = get()
        )
    }
    
    // Main screen - requires optional sessionId parameter
    factory { params ->
        MainScreenModel(
            initialSessionId = params.getOrNull(),
            sessionRepository = get(),
            eventStreamRepository = get(),
            appConnectionManager = get(),
            mcpRepository = get(),
            updateRepository = get()
        )
    }
    
    // Onboarding screen
    factory {
        OnboardingScreenModel(
            serverConfigRepository = get(),
            appConnectionManager = get()
        )
    }
    
    // Dashboard screen
    factory {
        DashboardScreenModel(
            providerRepository = get(),
            agentRepository = get(),
            toolRepository = get(),
            commandRepository = get(),
            formatterRepository = get(),
            lspRepository = get(),
            gitRepository = get(),
            mcpRepository = get(),
            eventStreamRepository = get(),
            projectRepository = get()
        )
    }
}

/**
 * All common modules combined.
 */
val appModules: List<Module> = listOf(
    cacheModule,
    commonModule,
    screenModelModule
)