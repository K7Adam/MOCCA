import os
import re

dir_path = r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src'

replacements = {
    r'AppTheme\.extendedColors\.textPrimary': 'MaterialTheme.colorScheme.onSurface',
    r'AppTheme\.extendedColors\.textSecondary': 'MaterialTheme.colorScheme.onSurfaceVariant',
    r'AppTheme\.extendedColors\.textTertiary': 'MaterialTheme.colorScheme.onSurfaceVariant',
    r'AppTheme\.extendedColors\.textPlaceholder': 'MaterialTheme.colorScheme.onSurfaceVariant',
    r'AppTheme\.extendedColors\.accentGreen': 'MaterialTheme.colorScheme.primary',
    r'AppTheme\.extendedColors\.accent': 'MaterialTheme.colorScheme.primary',
    r'AppTheme\.colors\.accentGreen': 'MaterialTheme.colorScheme.primary',
    r'AppTheme\.colors\.accent': 'MaterialTheme.colorScheme.primary',
    r'AppTheme\.extendedColors\.white': 'androidx.compose.ui.graphics.Color.White',
    r'AppTheme\.colors\.white': 'androidx.compose.ui.graphics.Color.White',
    r'AppTheme\.extendedColors\.error': 'MaterialTheme.colorScheme.error',
    r'AppTheme\.extendedColors\.grey': 'MaterialTheme.colorScheme.outline',
    r'AppTheme\.extendedColors\.border': 'MaterialTheme.colorScheme.outline',
    r'AppTheme\.extendedColors\.borderLight': 'MaterialTheme.colorScheme.outlineVariant',
    r'AppTheme\.extendedColors\.surfaceVariant': 'MaterialTheme.colorScheme.surfaceVariant',
    r'AppTheme\.extendedColors\.surfaceContainerHigh': 'MaterialTheme.colorScheme.surfaceContainerHigh',
    r'AppTheme\.extendedColors\.surfaceContainer': 'MaterialTheme.colorScheme.surfaceContainer',
    r'AppTheme\.extendedColors\.background': 'MaterialTheme.colorScheme.background',
    r'AppTheme\.colors\.background': 'MaterialTheme.colorScheme.background',    
    r'AppTheme\.colors\.surfaceDim': 'MaterialTheme.colorScheme.surfaceDim',    
    r'AppTheme\.colors\.surface': 'MaterialTheme.colorScheme.surface',

    r'AppTheme\.typography\.labelLarge': 'MaterialTheme.typography.labelLarge', 
    r'AppTheme\.typography\.labelMedium': 'MaterialTheme.typography.labelMedium',
    r'AppTheme\.typography\.labelSmall': 'MaterialTheme.typography.labelSmall', 
    r'AppTheme\.typography\.bodyLarge': 'MaterialTheme.typography.bodyLarge',   
    r'AppTheme\.typography\.bodyMedium': 'MaterialTheme.typography.bodyMedium', 
    r'AppTheme\.typography\.bodySmall': 'MaterialTheme.typography.bodySmall',   
    r'AppTheme\.typography\.titleLarge': 'MaterialTheme.typography.titleLarge', 
    r'AppTheme\.typography\.titleMedium': 'MaterialTheme.typography.titleMedium',
    r'AppTheme\.typography\.titleSmall': 'MaterialTheme.typography.titleSmall', 
    r'AppTheme\.typography\.headlineLarge': 'MaterialTheme.typography.headlineLarge',
    r'AppTheme\.typography\.headlineMedium': 'MaterialTheme.typography.headlineMedium',
    r'AppTheme\.typography\.headlineSmall': 'MaterialTheme.typography.headlineSmall',
    r'AppTheme\.typography\.displayLarge': 'MaterialTheme.typography.displayLarge',
    r'AppTheme\.typography\.displayMedium': 'MaterialTheme.typography.displayMedium',
    r'AppTheme\.typography\.displaySmall': 'MaterialTheme.typography.displaySmall',
}

for root, dirs, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()

            original_content = content
            for old, new in replacements.items():
                content = re.sub(old, new, content)

            if content != original_content:
                if 'MaterialTheme.' in content and 'import androidx.compose.material3.MaterialTheme' not in content:
                    content = 'import androidx.compose.material3.MaterialTheme\n' + content
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated {path}")
