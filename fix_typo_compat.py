import os
import re

file_path = r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui\theme\AppTypography.kt'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

legacy_styles = '''
    // Legacy M3 aliases (Use MaterialTheme.typography instead)
    val displayLarge: TextStyle @Composable get() = MaterialTheme.typography.displayLarge
    val displayMedium: TextStyle @Composable get() = MaterialTheme.typography.displayMedium
    val displaySmall: TextStyle @Composable get() = MaterialTheme.typography.displaySmall
    val headlineLarge: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge
    val headlineMedium: TextStyle @Composable get() = MaterialTheme.typography.headlineMedium
    val headlineSmall: TextStyle @Composable get() = MaterialTheme.typography.headlineSmall
    val titleLarge: TextStyle @Composable get() = MaterialTheme.typography.titleLarge
    val titleMedium: TextStyle @Composable get() = MaterialTheme.typography.titleMedium
    val titleSmall: TextStyle @Composable get() = MaterialTheme.typography.titleSmall
    val bodyLarge: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val bodyMedium: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium
    val bodySmall: TextStyle @Composable get() = MaterialTheme.typography.bodySmall
    val labelLarge: TextStyle @Composable get() = MaterialTheme.typography.labelLarge
    val labelMedium: TextStyle @Composable get() = MaterialTheme.typography.labelMedium
    val labelSmall: TextStyle @Composable get() = MaterialTheme.typography.labelSmall
    val labelExtraSmall: TextStyle @Composable get() = TextStyle(fontFamily = displayMediumFamily, fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 1.sp)
}
'''

content = re.sub(r'\}\s*@Composable\s*fun appTypography', legacy_styles + '\n@Composable\nfun appTypography', content)
if 'import androidx.compose.material3.MaterialTheme' not in content:
    content = content.replace('import androidx.compose.material3.Typography', 'import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.Typography')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
