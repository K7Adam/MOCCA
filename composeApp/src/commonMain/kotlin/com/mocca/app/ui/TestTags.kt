package com.mocca.app.ui

/**
 * Centralized test tag constants for Maestro E2E selectors.
 *
 * Naming convention: hierarchical screen/role identifiers.
 * Structure: `ScreenName.elementRole` or `ScreenName.Container.subElement`
 *
 * Example usage in Compose:
 *   `Modifier.testTag(TestTags.Dashboard.connectionStatus)`
 *
 * Example Maestro selector:
 *   `assertVisible: { id: "dashboard_connection_status" }`
 */
object TestTags {

    object Nav {
        const val sessions = "nav_sessions"
        const val chat = "nav_chat"
        const val tools = "nav_tools"
    }

    object Onboarding {
        const val welcomeScreen = "onboarding_welcome_screen"
        const val connectStep = "onboarding_connect_step"
        const val connectingStep = "onboarding_connecting_step"
        const val getStartedButton = "onboarding_get_started_button"
        const val qrScanButton = "onboarding_qr_scan_button"
        const val pairingLinkInput = "onboarding_pairing_link_input"
        const val connectButton = "onboarding_connect_button"
        const val backButton = "onboarding_back_button"
        const val retryButton = "onboarding_retry_button"
    }

    object Settings {
        const val screen = "settings_screen"
        const val backButton = "settings_back_button"
        const val cliConnectionSection = "settings_cli_connection_section"
        const val bridgeSection = "settings_bridge_section"
        const val serverConfig = "settings_server_config"
        const val refreshButton = "settings_refresh_button"
        const val reconnectButton = "settings_reconnect_button"
        const val disconnectButton = "settings_disconnect_button"
        const val forgetButton = "settings_forget_button"
        const val providerAuthSection = "settings_provider_auth_section"
        const val appConfigSection = "settings_app_config_section"
        const val syncConfigButton = "settings_sync_config_button"
        const val projectSection = "settings_project_section"
        const val projectPathInput = "settings_project_path_input"
        const val updatePathButton = "settings_update_path_button"
        const val appUpdatesSection = "settings_app_updates_section"
        const val githubTokenInput = "settings_github_token_input"
        const val saveTokenButton = "settings_save_token_button"
        const val validateTokenButton = "settings_validate_token_button"
        const val checkUpdatesButton = "settings_check_updates_button"
        const val clearCacheDialog = "settings_clear_cache_dialog"
        const val clearCacheConfirmButton = "settings_clear_cache_confirm_button"
        const val clearCacheCancelButton = "settings_clear_cache_cancel_button"
    }

    object Chat {
        const val inputContent = "chat_input_content"
        const val inputTextField = "chat_input_text_field"
        const val sendButton = "chat_send_button"
        const val abortButton = "chat_abort_button"
        const val attachButton = "chat_attach_button"
        const val micButton = "chat_mic_button"
        const val messageList = "chat_message_list"
        const val sessionSelector = "chat_session_selector"
        const val commandPaletteButton = "chat_command_palette_button"
        const val historyUpButton = "chat_history_up_button"
        const val historyDownButton = "chat_history_down_button"
        const val modelSelectorButton = "chat_model_selector_button"
        const val variantSelectorButton = "chat_variant_selector_button"
    }

    object Dashboard {
        const val navigationRow = "dashboard_navigation_row"
        const val sessionsTab = "dashboard_sessions_tab"
        const val chatTab = "dashboard_chat_tab"
        const val toolsTab = "dashboard_tools_tab"
        const val settingsNav = "dashboard_settings_nav"
        const val gitNav = "dashboard_git_nav"
        const val filesNav = "dashboard_files_nav"
        const val terminalNav = "dashboard_terminal_nav"
        const val mcpNav = "dashboard_mcp_nav"
    }

    object Bridge {
        const val connectionStatus = "bridge_connection_status"
        const val pairingQr = "bridge_pairing_qr"
        const val disconnectButton = "bridge_disconnect_button"
        const val pairingStatus = "bridge_pairing_status"
        const val networkHint = "bridge_network_hint"
    }

    object Update {
        const val dialog = "update_dialog"
        const val downloadButton = "update_download_button"
        const val laterButton = "update_later_button"
        const val retryButton = "update_retry_button"
        const val dismissButton = "update_dismiss_button"
        const val progressBar = "update_progress_bar"
        const val logsSection = "update_logs_section"
        const val copyLogsButton = "update_copy_logs_button"
        const val checkUpdatesButton = "update_check_updates_button"
    }

    object Dialog {
        const val permissionRequest = "dialog_permission_request"
        const val permissionAllowButton = "dialog_permission_allow_button"
        const val permissionDenyButton = "dialog_permission_deny_button"
        const val questionDialog = "dialog_question"
        const val questionInput = "dialog_question_input"
        const val questionConfirmButton = "dialog_question_confirm_button"
        const val questionCancelButton = "dialog_question_cancel_button"
    }

    object Terminal {
        const val screen = "terminal_screen"
        const val tabBar = "terminal_tab_bar"
        const val newTabButton = "terminal_new_tab_button"
        const val emptyState = "terminal_empty_state"
        const val createButton = "terminal_create_button"
        const val content = "terminal_content"
        const val outputArea = "terminal_output_area"
        const val inputBar = "terminal_input_bar"
        const val inputField = "terminal_input_field"
        const val sendButton = "terminal_send_button"
    }

    object Screen

    object Tab

    object Input
}
