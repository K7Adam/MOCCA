import re

with open('composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('inputEnabled = chatState.connectionStatus is com.mocca.app.domain.model.ConnectionStatus.Connected && chatState.isSessionIdle,', 'inputEnabled = chatState.connectionStatus is com.mocca.app.domain.model.ConnectionStatus.Connected && chatState.isSessionIdle,\n                                        placeholder = "Type a message...",')

with open('composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
