import re

file_path = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/workspace/WorkspaceScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('subtitle = if (isDashboard) null else "SESSION: ",', 'subtitle = if (isDashboard) null else "SESSION: " + sessionId,')
content = content.replace('tabNavigator.saveableState(key = "tab_", tab = tab) {', 'tabNavigator.saveableState(key = "tab_" + tab.options.index.toString(), tab = tab) {')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
