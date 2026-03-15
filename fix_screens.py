import re

with open('composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add imports
content = content.replace('import androidx.compose.material3.MaterialTheme\n', 'import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.Scaffold\nimport androidx.compose.ui.graphics.Color\nimport com.mocca.app.ui.components.navigation.PersistentNavRow\nimport com.mocca.app.ui.components.navigation.ChatInputContent\nimport androidx.compose.material3.Surface\n')

# 2. Move state collection up
content = content.replace('        val chatState by chatScreenModel.state.collectAsState()', '        val chatState by chatScreenModel.state.collectAsState()\n        val inputText by chatScreenModel.inputText.collectAsState()\n        val shellMode by chatScreenModel.shellMode.collectAsState()')

# remove it from the bottom
content = content.replace('            val inputText by chatScreenModel.inputText.collectAsState()\n            val shellMode by chatScreenModel.shellMode.collectAsState()\n', '')


# 3. Modify ChatContent area to include ChatInputContent
chat_content_search = """                                ChatContent(
                                    screenModel = chatScreenModel, 
                                    onScrollDirectionChange = { direction: ScrollDirection -> scrollDirection = direction },
                                    onScrollToBottomStateChange = { show, hasNew ->
                                        showScrollToBottom = show
                                        hasNewMessagesWhileScrolledUp = hasNew
                                    },
                                    scrollToBottomTrigger = scrollToBottomTrigger
                                )"""

chat_content_replacement = """                                Box(modifier = Modifier.weight(1f)) {
                                    ChatContent(
                                        screenModel = chatScreenModel, 
                                        onScrollDirectionChange = { direction: ScrollDirection -> scrollDirection = direction },
                                        onScrollToBottomStateChange = { show, hasNew ->
                                            showScrollToBottom = show
                                            hasNewMessagesWhileScrolledUp = hasNew
                                        },
                                        scrollToBottomTrigger = scrollToBottomTrigger
                                    )
                                }
                                
                                Surface(
                                    modifier = Modifier.fillMaxWidth().imePadding(),
                                    color = Color.Transparent
                                ) {
                                    ChatInputContent(
                                        inputText = inputText,
                                        onInputTextChange = { chatScreenModel.updateInputText(it) },
                                        onSendClick = { chatScreenModel.sendMessage() },
                                        inputEnabled = chatState.connectionStatus is com.mocca.app.domain.model.ConnectionStatus.Connected && chatState.isSessionIdle,
                                        isSessionIdle = chatState.isSessionIdle,
                                        onAbortClick = { chatScreenModel.abortSession() },
                                        modelName = chatState.modelName,
                                        agentName = chatState.agentName,
                                        providerResponse = chatState.providerInfo,
                                        selectedProviderId = chatState.selectedProviderId,
                                        selectedModelId = chatState.selectedModelId,
                                        onModelSelected = { providerId, modelId -> chatScreenModel.selectModel(providerId, modelId) },
                                        variants = chatState.availableVariants,
                                        selectedVariantId = chatState.selectedVariantId,
                                        onVariantSelected = { chatScreenModel.selectVariant(it) },
                                        modes = chatState.modes,
                                        selectedModeId = chatState.selectedModeId,
                                        onModeSelected = { chatScreenModel.selectMode(it) },
                                        attachedFiles = chatState.attachedFiles,
                                        onRemoveAttachment = { chatScreenModel.removeAttachment(it) },
                                        onAttachClick = { filePickerLauncher.launch() },
                                        commands = chatState.commands,
                                        onCommandSelected = { chatScreenModel.executeCommand(it) },
                                        onModeSelectedForMention = { chatScreenModel.selectMode(it.id) },
                                        shellMode = shellMode,
                                        onShellModeToggle = { chatScreenModel.toggleShellMode() },
                                        onHistoryUp = { chatScreenModel.navigateHistoryUp() },
                                        onHistoryDown = { chatScreenModel.navigateHistoryDown() }
                                    )
                                }"""

content = content.replace(chat_content_search, chat_content_replacement)

# 4. Wrap SwipePanelLayout in Scaffold

swipe_search = "                SwipePanelLayout(\n                    modifier = Modifier.fillMaxSize(),"

scaffold_replacement = """                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    bottomBar = {
                        val sharedTransitionScope = LocalSharedTransitionScope.current
                        val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
            
                        val bottomBarModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                Modifier
                                    .navigationBarsPadding()
                                    .sharedBounds(
                                        rememberSharedContentState(key = "bottom_bar"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                            }
                        } else {
                            Modifier
                                .navigationBarsPadding()
                        }
                        
                        Box(
                            modifier = Modifier.padding(horizontal = AppSpacing.screenPaddingHorizontalCompact, vertical = AppSpacing.sm)
                        ) {
                            Surface(
                                modifier = bottomBarModifier.fillMaxWidth(),
                                color = AppColors.surfaceContainer,
                                shape = AppShapes.rounded2xl
                            ) {
                                PersistentNavRow(
                                    dragProgress = dragProgress,
                                    onItemClick = { panelState.state = it },
                                    showLabels = true,
                                    isAgentRunning = !chatState.isSessionIdle,
                                    modifier = Modifier.fillMaxWidth().height(NavConstants.NavigationModeHeight)
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                SwipePanelLayout(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),"""

content = content.replace(swipe_search, scaffold_replacement)

# 5. Remove UnifiedFloatingBottomBar call at the end
bottom_bar_search = r'            // Determine bottom bar mode based on current panel[\s\S]*?UnifiedFloatingBottomBar\([\s\S]*?modifier = bottomBarModifier\s*\)'
content = re.sub(bottom_bar_search, '            // Bottom bar is now handled by Scaffold', content)

# Close the scaffold
content = content.replace('            // End of content wrapper', '                }\n            // End of content wrapper')

with open('composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
