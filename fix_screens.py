import os
import re

ui_dir = r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui'

replacements = [
    (r'fontSize\s*=\s*10\.sp,\s*letterSpacing\s*=\s*1\.sp', r'style = com.mocca.app.ui.theme.AppTypography.labelSmall'),
    (r'fontSize\s*=\s*10\.sp', r'style = com.mocca.app.ui.theme.AppTypography.labelSmall'),
    (r'fontSize\s*=\s*12\.sp', r'style = com.mocca.app.ui.theme.AppTypography.labelMedium'),
    (r'fontSize\s*=\s*13\.sp', r'style = com.mocca.app.ui.theme.AppTypography.labelLarge'),
    (r'fontSize\s*=\s*14\.sp', r'style = com.mocca.app.ui.theme.AppTypography.bodyMedium'),
    (r'RoundedCornerShape\(topStart\s*=\s*16\.dp,\s*topEnd\s*=\s*16\.dp\)', r'com.mocca.app.ui.theme.AppShapes.topRounded'),
    (r'RoundedCornerShape\(32\.dp\)', r'com.mocca.app.ui.theme.AppShapes.extraExtraLarge'),
    (r'RoundedCornerShape\(24\.dp\)', r'com.mocca.app.ui.theme.AppShapes.extraLarge'),
    (r'RoundedCornerShape\(16\.dp\)', r'com.mocca.app.ui.theme.AppShapes.large'),
    (r'RoundedCornerShape\(12\.dp\)', r'com.mocca.app.ui.theme.AppShapes.medium'),
    (r'RoundedCornerShape\(8\.dp\)', r'com.mocca.app.ui.theme.AppShapes.small'),
    (r'RoundedCornerShape\(4\.dp\)', r'com.mocca.app.ui.theme.AppShapes.extraSmall'),
    (r'RoundedCornerShape\(50\)', r'com.mocca.app.ui.theme.AppShapes.circle'),
]

for root, dirs, files in os.walk(ui_dir):
    for file in files:
        if file.endswith('.kt') and file != 'AppShapes.kt':
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            orig_content = content
            for old, new in replacements:
                content = re.sub(old, new, content)
            
            if content != orig_content:
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated {file}")
