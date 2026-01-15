package com.mocca.app.ui.screens.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.mocca.app.ui.screens.chat.ChatScreen
import com.mocca.app.ui.screens.files.FilesScreen
import com.mocca.app.ui.screens.git.GitScreen
import com.mocca.app.ui.screens.terminal.TerminalScreen
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

data class WorkspaceScreen(val sessionId: String) : Screen {

    @Composable
    override fun Content() {
        var selectedIndex by remember { mutableStateOf(0) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalColors.background)
        ) {
            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedIndex) {
                    0 -> ChatScreen(sessionId).Content()
                    1 -> FilesScreen().Content()
                    2 -> TerminalScreen().Content()
                    3 -> GitScreen().Content()
                }
            }
            
            // Terminal-style bottom navigation bar
            TerminalBottomNavBar(
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it }
            )
        }
    }
}

@Composable
private fun TerminalBottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        NavItem(0, "CHAT", Icons.AutoMirrored.Filled.Chat),
        NavItem(1, "FILES", Icons.Default.Folder),
        NavItem(2, "TERM", Icons.Default.Terminal),
        NavItem(3, "GIT", Icons.Default.Code)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalColors.surface)
            .border(TerminalSpacing.borderThin, TerminalColors.border, RectangleShape)
    ) {
        items.forEach { item ->
            val isSelected = selectedIndex == item.index
            TerminalNavItem(
                label = item.label,
                icon = item.icon,
                isSelected = isSelected,
                onClick = { onItemSelected(item.index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class NavItem(
    val index: Int,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun TerminalNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) TerminalColors.white else TerminalColors.surface
    val contentColor = if (isSelected) TerminalColors.background else TerminalColors.grey
    val borderColor = if (isSelected) TerminalColors.white else TerminalColors.border
    
    Column(
        modifier = modifier
            .background(backgroundColor, RectangleShape)
            .border(TerminalSpacing.borderThin, borderColor, RectangleShape)
            .clickable(onClick = onClick)
            .padding(vertical = TerminalSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(TerminalSpacing.xs))
        Text(
            text = label,
            style = TerminalTypography.labelSmall,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
