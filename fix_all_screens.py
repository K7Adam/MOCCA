import os
import re

# Directories to audit
search_dirs = [
    r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui\screens',
    r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui\components'
]

# Patterns to replace
# This is a surgical operation, Morty! No room for errors!
replacements = [
    # Shapes
    (r'RoundedCornerShape\(4\.dp\)', 'AppShapes.small'),
    (r'RoundedCornerShape\(8\.dp\)', 'AppShapes.medium'),
    (r'RoundedCornerShape\(12\.dp\)', 'AppShapes.large'),
    (r'RoundedCornerShape\(16\.dp\)', 'AppShapes.extraLarge'),
    (r'RoundedCornerShape\(24\.dp\)', 'AppShapes.extraExtraLarge'),
    (r'RoundedCornerShape\(1\.dp\)', 'AppShapes.extraSmall'),
    
    # Colors (Mapping to compatibility aliases or direct M3 tokens)
    (r'AppColors\.white(?!\.copy)', 'AppColors.textPrimary'),
    (r'AppColors\.grey(?!\.copy)', 'AppColors.textSecondary'),
    (r'AppColors\.greyLight(?!\.copy)', 'AppColors.textTertiary'),
    (r'AppColors\.greyDark(?!\.copy)', 'AppColors.surfaceContainerHigh'),
    
    # Typography (purging hardcoded .sp)
    (r'fontSize\s*=\s*10\.sp', 'style = AppTypography.labelSmall'),
    (r'fontSize\s*=\s*12\.sp', 'style = AppTypography.bodySmall'),
    (r'fontSize\s*=\s*14\.sp', 'style = AppTypography.bodyMedium'),
    (r'fontSize\s*=\s*16\.sp', 'style = AppTypography.bodyLarge'),
    (r'fontSize\s*=\s*20\.sp', 'style = AppTypography.titleLarge'),
    (r'fontSize\s*=\s*24\.sp', 'style = AppTypography.headlineSmall'),
    
    # Cleaning up imports
    (r'import androidx\.compose\.foundation\.shape\.RoundedCornerShape', 'import com.mocca.app.ui.theme.AppShapes'),
    (r'import androidx\.compose\.ui\.unit\.sp', '// Purged .sp'),
]

def process_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    orig_content = content
    for old, new in replacements:
        content = re.sub(old, new, content)
    
    if content != orig_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Purged slop from: {file_path}")

for d in search_dirs:
    for root, _, files in os.walk(d):
        for file in files:
            if file.endswith('.kt'):
                process_file(os.path.join(root, file))

print("\nGreat purging complete! Pickle Riiiiick! 🥒")
