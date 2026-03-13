import os
import re

dir_path = r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src'

replacements = {
    # Colors
    r'AppColors\.textPrimary': 'MaterialTheme.colorScheme.onSurface',
    r'AppColors\.textSecondary': 'MaterialTheme.colorScheme.onSurfaceVariant',
    r'AppColors\.textTertiary': 'MaterialTheme.colorScheme.onSurfaceVariant',
    r'AppColors\.textPlaceholder': 'MaterialTheme.colorScheme.onSurfaceVariant',
    r'AppColors\.accentGreen': 'MaterialTheme.colorScheme.primary',
    r'AppColors\.accent': 'MaterialTheme.colorScheme.primary',
    r'AppColors\.white': 'androidx.compose.ui.graphics.Color.White',
    r'AppColors\.error': 'MaterialTheme.colorScheme.error',
    r'AppColors\.grey': 'MaterialTheme.colorScheme.outline',
    r'AppColors\.borderLight': 'MaterialTheme.colorScheme.outlineVariant',
    r'AppColors\.border': 'MaterialTheme.colorScheme.outline',
    r'AppColors\.surfaceVariant': 'MaterialTheme.colorScheme.surfaceVariant',
    r'AppColors\.surfaceContainerHigh': 'MaterialTheme.colorScheme.surfaceContainerHigh',
    r'AppColors\.surfaceContainer': 'MaterialTheme.colorScheme.surfaceContainer',
    r'AppColors\.surfaceDim': 'MaterialTheme.colorScheme.surfaceDim',
    r'AppColors\.surface': 'MaterialTheme.colorScheme.surface',
    r'AppColors\.background': 'MaterialTheme.colorScheme.background',
    
    # Typography
    r'AppTypography\.labelLarge': 'MaterialTheme.typography.labelLarge', 
    r'AppTypography\.labelMedium': 'MaterialTheme.typography.labelMedium',
    r'AppTypography\.labelSmall': 'MaterialTheme.typography.labelSmall', 
    r'AppTypography\.bodyLarge': 'MaterialTheme.typography.bodyLarge',   
    r'AppTypography\.bodyMedium': 'MaterialTheme.typography.bodyMedium', 
    r'AppTypography\.bodySmall': 'MaterialTheme.typography.bodySmall',   
    r'AppTypography\.titleLarge': 'MaterialTheme.typography.titleLarge', 
    r'AppTypography\.titleMedium': 'MaterialTheme.typography.titleMedium',
    r'AppTypography\.titleSmall': 'MaterialTheme.typography.titleSmall', 
    r'AppTypography\.headlineLarge': 'MaterialTheme.typography.headlineLarge',
    r'AppTypography\.headlineMedium': 'MaterialTheme.typography.headlineMedium',
    r'AppTypography\.headlineSmall': 'MaterialTheme.typography.headlineSmall',
    r'AppTypography\.displayLarge': 'MaterialTheme.typography.displayLarge',
    r'AppTypography\.displayMedium': 'MaterialTheme.typography.displayMedium',
    r'AppTypography\.displaySmall': 'MaterialTheme.typography.displaySmall',
}

for root, dirs, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.kt') and file != 'AppColors.kt' and file != 'AppTypography.kt':
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()

            original_content = content
            
            # Pre-replace AppTheme.extendedColors and AppTheme.colors to AppColors to catch them all
            content = re.sub(r'AppTheme\.extendedColors\.', 'AppColors.', content)
            content = re.sub(r'AppTheme\.colors\.', 'AppColors.', content)
            
            for old, new in replacements.items():
                content = re.sub(old, new, content)

            if content != original_content:
                if 'MaterialTheme.' in content and 'import androidx.compose.material3.MaterialTheme' not in content:
                    content = re.sub(r'^(package\s+[\w.]+)', r'\1\n\nimport androidx.compose.material3.MaterialTheme', content, flags=re.MULTILINE)
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated {path}")
