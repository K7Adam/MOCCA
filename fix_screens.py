import re

file_path = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/workspace/WorkspaceScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace imports
imports_to_add = """
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
"""

if "import cafe.adriel.voyager.navigator.tab.TabNavigator" not in content:
    content = content.replace("import cafe.adriel.voyager.navigator.currentOrThrow", "import cafe.adriel.voyager.navigator.currentOrThrow\n" + imports_to_add)

# Replace the WorkspaceScreen class
new_class = """data class WorkspaceScreen(val sessionId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        TabNavigator(DashboardTab(sessionId)) { tabNavigator ->
            Scaffold(
                topBar = {
                    val currentTitle = tabNavigator.current.options.title
                    val isDashboard = tabNavigator.current is DashboardTab
                    GodHeader(
                        title = if (isDashboard) "Workspace_01" else currentTitle,
                        onBackClick = { navigator.pop() },
                        subtitle = if (isDashboard) null else "SESSION: \",
                        actions = {
                            IconButton(onClick = { /* Settings */ }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppColors.textPrimary.copy(alpha = 0.5f))
                            }
                        }
                    )
                },
                containerColor = AppColors.background,
                bottomBar = {
                    GodBottomNavBar(tabNavigator, sessionId)
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    AnimatedContent(
                        targetState = tabNavigator.current,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                        },
                        label = "tab_transition"
                    ) { tab ->
                        tabNavigator.saveableState(key = "tab_", tab = tab) {
                            tab.Content()
                        }
                    }
                }
            }
        }
    }
}
"""

content = re.sub(r'data class WorkspaceScreen\(val sessionId: String\) : Screen \{.*?\n\s+private fun getTabTitle\(index: Int\): String = when \(index\) \{.*?\}\n\}', new_class, content, flags=re.DOTALL)

# Replace GodBottomNavBar
new_bottom_bar = """@Composable
private fun GodBottomNavBar(
    tabNavigator: TabNavigator,
    sessionId: String
) {
    val items = listOf(
        DashboardTab(sessionId),
        ChatTab(sessionId),
        ExplorerTab,
        GitTab
    )

    Surface(
        color = AppColors.background.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, AppColors.border)
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { tab ->
                val isSelected = tabNavigator.current == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = { tabNavigator.current = tab })
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Extract ImageVector safely or fallback to GridOn if something goes wrong
                    // Note: Voyager TabOptions only has Painter, not ImageVector natively, but we know the mapping
                    val iconVector = when(tab) {
                        is DashboardTab -> GridView
                        is ChatTab -> Icons.AutoMirrored.Filled.Chat
                        is ExplorerTab -> Icons.Default.Folder
                        is GitTab -> Code
                        else -> GridView
                    }
                    
                    Icon(
                        imageVector = iconVector,
                        contentDescription = tab.options.title,
                        tint = if (isSelected) AppColors.textPrimary else AppColors.textPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.options.title.uppercase(),
                        style = AppTypography.labelSmall,
                        color = if (isSelected) AppColors.textPrimary else AppColors.textPrimary.copy(alpha = 0.3f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}"""

content = re.sub(r'@Composable\s+private fun GodBottomNavBar\(\s+selectedIndex: Int,\s+onItemSelected: \(Int\) -> Unit\s+\) \{.*?\}\s+\}', new_bottom_bar, content, flags=re.DOTALL)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
