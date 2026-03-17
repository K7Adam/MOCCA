import os
import re

# Directories to audit
search_dirs = [
    r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui'
]

# Patterns to replace (Legacy -> M3 Tokens)
replacements = [
    # Colors
    (r'AppColors\.textPrimary', 'AppColors.onSurface'),
    (r'AppColors\.textSecondary', 'AppColors.onSurfaceVariant'),
    (r'AppColors\.textTertiary', 'AppColors.outline'),
    (r'AppColors\.surfaceElevated', 'AppColors.surfaceContainerHigh'),
    (r'AppColors\.cardBackground', 'AppColors.surfaceContainer'),
    (r'AppColors\.greyDark', 'AppColors.surfaceContainerHighest'),
    (r'AppColors\.greyLight', 'AppColors.outline'),
    (r'AppColors\.grey', 'AppColors.outlineVariant'),
    (r'AppColors\.indicator', 'AppColors.primary'),
    (r'AppColors\.buttonBackground', 'AppColors.primary'),
    (r'AppColors\.alertRed', 'AppColors.error'),
    (r'AppColors\.sendButton', 'AppColors.primary'),
    (r'AppColors\.input', 'AppColors.surfaceContainerLow'),
    (r'AppColors\.borderLight', 'AppColors.outlineVariant'),
    (r'AppColors\.border', 'AppColors.outline'),
    
    # Shapes
    (r'AppShapes\.extraExtraLarge', 'AppShapes.extraLarge'),
    (r'AppShapes\.xxl', 'AppShapes.extraLarge'),
    (r'AppShapes\.rounded2xl', 'AppShapes.extraLarge'),
    (r'AppShapes\.button', 'AppShapes.pill'),
    (r'AppShapes\.alertBanner', 'AppShapes.pill'),
]

def process_file(file_path):
    if "AppColors.kt" in file_path or "AppShapes.kt" in file_path:
        return
        
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    orig_content = content
    for old, new in replacements:
        content = re.sub(old, new, content)
    
    if content != orig_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Deslopped foundation in: {file_path}")

for d in search_dirs:
    for root, _, files in os.walk(d):
        for file in files:
            if file.endswith('.kt'):
                process_file(os.path.join(root, file))

print("\nFoundation deslop complete! 🥒")
