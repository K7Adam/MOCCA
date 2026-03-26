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

    object Dashboard

    object Screen

    object Tab

    object Input
}
