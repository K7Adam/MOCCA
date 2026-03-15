package com.mocca.app.ui.screens.workspace

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.mocca.app.ui.screens.chat.ChatScreen
import com.mocca.app.ui.screens.files.FilesScreen
import com.mocca.app.ui.screens.git.GitScreen

data class DashboardTab(val sessionId: String) : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Dashboard"
            val icon = rememberVectorPainter(Icons.Filled.GridOn)
            return remember { TabOptions(index = 0u, title = title, icon = icon) }
        }

    @Composable
    override fun Content() {
        DashboardContent(sessionId)
    }
}

data class ChatTab(val sessionId: String) : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Chat"
            val icon = rememberVectorPainter(Icons.AutoMirrored.Filled.Chat)
            return remember { TabOptions(index = 1u, title = title, icon = icon) }
        }

    @Composable
    override fun Content() {
        ChatScreen(sessionId).Content()
    }
}

object ExplorerTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Explorer"
            val icon = rememberVectorPainter(Icons.Filled.Folder)
            return remember { TabOptions(index = 2u, title = title, icon = icon) }
        }

    @Composable
    override fun Content() {
        FilesScreen().Content()
    }
}

object GitTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Git"
            val icon = rememberVectorPainter(Icons.Filled.Terminal)
            return remember { TabOptions(index = 3u, title = title, icon = icon) }
        }

    @Composable
    override fun Content() {
        GitScreen().Content()
    }
}
