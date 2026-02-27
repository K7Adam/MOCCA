package com.mocca.app.di

import com.mocca.app.api.ApiExecutor
import com.mocca.app.api.GitHubApiClient
import com.mocca.app.api.MoccaApiClient
import com.mocca.app.api.MoccaSseClient
import com.mocca.app.api.RetryPolicy
import com.mocca.app.data.GlobalActivityManager
import com.mocca.app.data.local.LocalCache
import com.mocca.app.data.local.LocalCacheFactory
import com.mocca.app.data.repository.*
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.data.security.SecureTokenStorage
import com.mocca.app.data.security.NoOpSecureTokenStorage
import com.mocca.app.ui.screens.chat.delegates.*
import com.mocca.app.ui.screens.files.FilesScreenModel
import com.mocca.app.ui.screens.git.GitDiffScreenModel
import com.mocca.app.ui.screens.git.GitScreenModel
import com.mocca.app.ui.screens.main.MainScreenModel
import com.mocca.app.ui.screens.mcp.McpScreenModel
import com.mocca.app.ui.screens.onboarding.OnboardingWizardModel
import com.mocca.app.ui.screens.panels.DashboardScreenModel
import com.mocca.app.ui.screens.sessions.SessionsScreenModel
import com.mocca.app.ui.screens.settings.SettingsScreenModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import cafe.adriel.voyager.core.model.screenModelScope

/**
 * Common Koin modules shared across all platforms.
 * Uses ConnectionManager as the single source of truth for connection state and HttpClient lifecycle.
 * Uses StateCoordinator as the single source of truth for event dispatch and state synchronization.
 */
val commonModule = module {
    // ... rest of file
    // ConnectionManager — single source of truth for connection + ApiExecutor
    single {
        ConnectionManager(
            serverConfigRepository = get(),
            networkObserver = getOrNull()
        )
    }

    // Bind ApiExecutor interface to ConnectionManager
    single<ApiExecutor> { get<ConnectionManager>() }

    // API Clients — use ApiExecutor (no direct HttpClient references)
    single {
        MoccaApiClient(
            api = get(),
            retryPolicy = RetryPolicy.Default
        )
    }
    
    single {
        MoccaSseClient(
            api = get()
        )
    }
    
    // Repositories
    single { SessionRepository(get(), get()) }
    single { 
        EventStreamRepository(
            sseClient = get(),
            networkObserver = getOrNull(),
            apiClient = get(),
            appLifecycleObserver = getOrNull(),
            notificationTracker = getOrNull<com.mocca.app.domain.manager.NotificationTracker>()
        )
    }
    singleOf(::FileRepository)
    single { GitRepository(get(), get()) }
    single { McpRepository(get()) }
    singleOf(::SettingsRepository)
    singleOf(::PreferencesManager)
    single { ConfigRepository(get()) }
    singleOf(::UpdateRepository)
    singleOf(::GitHubApiClient)
    
    // Global update notifier - allows any screen to trigger update dialog
    singleOf(::UpdateNotifier)
    
    // Periodic update check scheduler
    singleOf(::UpdateCheckScheduler)

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
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNC STATE MANAGER - Central hub for tracking sync health across all repos
    // MUST come before StateCoordinator and repositories that report sync state
    // ═══════════════════════════════════════════════════════════════════════════════
    
    singleOf(::SyncStateManager)
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATE COORDINATOR - Central hub for all event handling and state sync
    // NOTE: This MUST come after EventStreamRepository and ConnectionManager
    // ═══════════════════════════════════════════════════════════════════════════════
    
    single { 
        StateCoordinator(
            eventStreamRepository = get(),
            connectionManager = get(),
            localCache = get(),
            sessionRepository = get(),
            settingsRepository = get(),
            appLifecycleObserver = getOrNull(),
            networkObserver = getOrNull(),
            notificationTracker = getOrNull<com.mocca.app.domain.manager.NotificationTracker>(),
            moccaApiClient = get()
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // REALTIME SYNC SERVICE - Periodic polling for data without SSE events
    // Handles: MCP servers, Providers, Git status, Tools, Commands
    // ═══════════════════════════════════════════════════════════════════════════════
    
    single {
        RealtimeSyncService(
            stateCoordinator = get(),
            connectionManager = get(),
            syncStateManager = get(),
            mcpRepository = get(),
            gitRepository = get(),
            toolRepository = get(),
            agentRepository = get(),
            commandRepository = get(),
            providerRepository = get(),
            sessionRepository = get()
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CENTRALIZED STATE STORES - Single source of truth for all app state
    // NOTE: These MUST come after StateCoordinator, RealtimeSyncService and all dependencies
    // ═══════════════════════════════════════════════════════════════════════════════
    
    single { 
        AppStateStore(
            localCache = get(),
            stateCoordinator = get(),
            syncStateManager = get(),
            sessionRepository = get(),
            mcpRepository = get(),
            configRepository = get(),
            agentRepository = get(),
            providerRepository = get(),
            toolRepository = get(),
            commandRepository = get(),
            gitRepository = get(),
            realtimeSyncService = get(),
            preferencesManager = get()
        )
    }
    
    single { 
        ChatStateStore(
            localCache = get(),
            stateCoordinator = get(),
            sessionRepository = get()
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
    
    // Secure token storage for encrypting auth tokens
    // Platform-specific modules will override this with actual implementation
    single<SecureTokenStorage> {
        NoOpSecureTokenStorage
    }
    
    single {
        ServerConfigRepository(get(), get())
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
            connectionManager = get()
        )
    }
    
    // Chat screen - accepts optional initialSessionId parameter
    // NOTE: Now includes appStateStore for single source of truth state management
    factory { params ->
        val initialSessionId: String? = params.getOrNull()
        com.mocca.app.ui.screens.chat.ChatScreenModel(
            initialSessionId = initialSessionId,
            sessionRepository = get(),
            stateCoordinator = get(),
            commandRepository = get(),
            agentRepository = get(),
            appStateStore = get(),
            chatStateStore = get()
        )
    }
    
    // Files screen
    factoryOf(::FilesScreenModel)
    
    // Settings screen
    factory {
        SettingsScreenModel(
            serverConfigRepository = get(),
            connectionManager = get(),
            updateRepository = get(),
            settingsRepository = get(),
            configRepository = get(),
            updateNotifier = get(),
            preferencesManager = get()
        )
    }
    
    // Git screen
    factory {
        GitScreenModel(
            gitRepository = get(),
            stateCoordinator = get()
        )
    }
    
    // Git Diff screen
    factory { params ->
        GitDiffScreenModel(
            gitRepository = get(),
            sessionRepository = get()
        )
    }
    
    // MCP screen
    factory {
        McpScreenModel(
            mcpRepository = get()
        )
    }
    
    // Main screen - requires optional sessionId parameter
    // NOTE: No longer uses EventStreamRepository - all state from AppStateStore
    factory { params ->
        MainScreenModel(
            initialSessionId = params.getOrNull(),
            appStateStore = get(),
            sessionRepository = get(),
            connectionManager = get(),
            mcpRepository = get(),
            updateRepository = get(),
            updateNotifier = get(),
            updateCheckScheduler = get(),
            appVersionProvider = get()
        )
    }
    
    // Onboarding wizard - progressive onboarding
    factory {
        OnboardingWizardModel(
            serverConfigRepository = get(),
            connectionManager = get(),
            serverDiscovery = getOrNull()
        )
    }
    
    // Dashboard screen - uses AppStateStore for data + McpRepository for actions
    factory {
        DashboardScreenModel(
            appStateStore = get(),
            stateCoordinator = get(),
            mcpRepository = get(),
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
