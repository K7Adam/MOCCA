package com.mocca.app.di

import com.mocca.app.api.ApiExecutor
import com.mocca.app.api.GitHubApiClient
import com.mocca.app.api.MoccaApiClient
import com.mocca.app.api.MoccaSseClient
import com.mocca.app.api.RetryPolicy
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeHealthChecker
import com.mocca.app.bridge.connection.BridgePairingIntentStore
import com.mocca.app.bridge.connection.BridgeTargetRepository
import com.mocca.app.bridge.connection.BridgeTargetStore
import com.mocca.app.bridge.connection.BridgeTransportFactory
import com.mocca.app.bridge.connection.LocalCacheBridgeTargetStore
import com.mocca.app.bridge.opencode.BridgeRuntimeBootstrapper
import com.mocca.app.data.GlobalActivityManager
import com.mocca.app.data.local.LocalCache
import com.mocca.app.data.local.LocalCacheFactory
import com.mocca.app.data.local.DatabasePruner
import com.mocca.app.data.repository.*
import com.mocca.app.data.repository.McpRepository
import com.mocca.app.data.security.SecureTokenStorage
import com.mocca.app.data.security.NoOpSecureTokenStorage
import com.mocca.app.ui.screens.files.FilesScreenModel
import com.mocca.app.ui.screens.git.GitDiffScreenModel
import com.mocca.app.ui.screens.git.GitScreenModel
import com.mocca.app.ui.screens.main.MainScreenModel
import com.mocca.app.ui.screens.mcp.McpScreenModel
import com.mocca.app.ui.screens.mcp.McpResourceScreenModel
import com.mocca.app.ui.screens.onboarding.OnboardingWizardModel
import com.mocca.app.ui.screens.panels.DashboardScreenModel
import com.mocca.app.ui.screens.skills.SkillsScreenModel

import com.mocca.app.ui.screens.settings.SettingsScreenModel
import com.mocca.app.ui.screens.terminal.TerminalScreenModel
import com.mocca.app.ui.screens.settings.FeatureFlagsScreenModel
import com.mocca.app.util.NoOpVoiceInputProvider
import com.mocca.app.util.VoiceInputProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Common Koin modules shared across all platforms.
 * Uses ConnectionManager as the single source of truth for connection state and HttpClient lifecycle.
 * Uses StateCoordinator as the single source of truth for event dispatch and state synchronization.
 */
val commonModule = module {
    // ConnectionManager — single source of truth for connection + ApiExecutor
    single {
        ConnectionManager(
            serverConfigRepository = get(),
            networkObserver = getOrNull()
        )
    }

    // Bind ApiExecutor interface to ConnectionManager
    single<ApiExecutor> { get<ConnectionManager>() }
    single<BridgeHealthChecker> { get<ConnectionManager>() }

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
            networkObserver = getOrNull<com.mocca.app.util.NetworkObserver>(),
            apiClient = get(),
            appLifecycleObserver = getOrNull(),
            notificationTracker = getOrNull<com.mocca.app.domain.manager.NotificationTracker>(),
            localCache = get(),
            bridgeConnectionManager = get()
        )
    }
    single {
        FileRepository(
            bridgeConnectionManager = get()
        )
    }
    single { GitRepository(get(), get()) }
    single { McpRepository(get(), get()) }
    single { SettingsRepository(get(), get()) }
    singleOf(::PreferencesManager)
    single { ConfigRepository(get()) }
    singleOf(::UpdateRepository)
    singleOf(::GitHubApiClient)
    single<BridgeTransportFactory> { get<ConnectionManager>() }
    singleOf(::BridgePairingIntentStore)
    single {
        BridgeConnectionManager(
            targetRepository = get(),
            transportFactory = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            healthChecker = get()
        )
    }
    singleOf(::BridgeRuntimeBootstrapper)
    single { AiRuntimeConfigRepository(get(), get(), get(), get()) }
    single { AiChatGateway(get(), get()) }
    
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
    singleOf(::SkillRepository)
    single { SearchRepository(apiClient = get(), fileRepository = get()) }
    singleOf(::ProjectRepository)
    single {
        SystemMonitorRepository(
            bridgeConnectionManager = get()
        )
    }
    single<VoiceInputProvider> { NoOpVoiceInputProvider }


    // STATE COORDINATOR - Central hub for all event handling and state sync
    // NOTE: This MUST come after EventStreamRepository and ConnectionManager

    
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
            moccaApiClient = get(),
            databasePruner = get()
        )
    }

    // REALTIME SYNC SERVICE - Periodic polling for data without SSE events
    // Handles: MCP servers, Providers, Git status, Tools, Commands

    
    single {
        RealtimeSyncService(
            stateCoordinator = get(),
            connectionManager = get(),
            mcpRepository = get(),
            gitRepository = get(),
            toolRepository = get(),
            agentRepository = get(),
            commandRepository = get(),
            providerRepository = get()
        )
    }

    // CENTRALIZED STATE STORES - Single source of truth for all app state
    // NOTE: These MUST come after StateCoordinator, RealtimeSyncService and all dependencies

    
    single { 
        AppStateStore(
            localCache = get(),
            stateCoordinator = get(),
            sessionRepository = get(),
            mcpRepository = get(),
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
            sessionRepository = get(),
            aiChatGateway = get()
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

    single<BridgeTargetStore> {
        LocalCacheBridgeTargetStore(get())
    }

    single {
        BridgeTargetRepository(get())
    }
    
    single {
        DatabasePruner(get())
    }
}

/**
 * ScreenModel/ViewModel module for presentation layer.
 */
val screenModelModule = module {
    // Chat screen - accepts optional initialSessionId parameter
    // NOTE: Now includes appStateStore for single source of truth state management
    factory { params ->
        val initialSessionId: String? = params.getOrNull()
        com.mocca.app.ui.screens.chat.ChatScreenModel(
            initialSessionId = initialSessionId,
            sessionRepository = get(),
            stateCoordinator = get(),
            commandRepository = get(),
            appStateStore = get(),
            chatStateStore = get(),
            aiRuntimeConfigRepository = get(),
            voiceInputProvider = get()
        )
    }
    
    // Files screen
    factoryOf(::FilesScreenModel)
    
    // Settings screen
    factory {
        SettingsScreenModel(
            serverConfigRepository = get(),
            bridgeTargetRepository = get(),
            bridgeConnectionManager = get(),
            connectionManager = get(),
            updateRepository = get(),
            settingsRepository = get(),
            configRepository = get(),
            aiRuntimeConfigRepository = get(),
            projectRepository = get(),
            updateNotifier = get(),
            preferencesManager = get()
        )
    }
    
    // Git screen
    factory {
        GitScreenModel(
            gitRepository = get()
        )
    }
    
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

    // Skills screen
    factory {
        SkillsScreenModel(
            skillRepository = get()
        )
    }

    // MCP Resource screen - parameterized by server name
    factory { params ->
        McpResourceScreenModel(
            serverName = params.get(),
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
            searchRepository = get(),
            aiRuntimeConfigRepository = get(),
            connectionManager = get(),
            mcpRepository = get(),
            updateRepository = get(),
            updateNotifier = get(),
            updateCheckScheduler = get(),
            appVersionProvider = get(),
            aiChatGateway = get()
        )
    }
    
    // Onboarding wizard - progressive onboarding
    factory {
        OnboardingWizardModel(
            serverConfigRepository = get(),
            connectionManager = get(),

            appStateStore = get(),
            bridgeConnectionManager = get()
        )
    }
    
    // Dashboard screen - uses AppStateStore for data + McpRepository for actions
    factory {
        DashboardScreenModel(
            appStateStore = get(),
            stateCoordinator = get(),
            mcpRepository = get(),
            projectRepository = get(),
            systemMonitorRepository = get()
        )
    }

    // Terminal screen
    factory { TerminalScreenModel(get(), get()) }

    // Feature flags screen
    factoryOf(::FeatureFlagsScreenModel)
}

/**
 * All common modules combined.
 */
val appModules: List<Module> = listOf(
    cacheModule,
    commonModule,
    screenModelModule
)
