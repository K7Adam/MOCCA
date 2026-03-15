import re

file_path = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/workspace/WorkspaceScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# find where GodBottomNavBar ends
content = ''.join(lines)
idx = content.rfind('    }\n}')

# Clean up trailing braces
if content.endswith('        }\n    }\n}\n'):
    content = content[:-19]

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
