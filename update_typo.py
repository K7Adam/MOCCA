import os
import re

file_path = r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui\theme\AppTypography.kt'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace appTypography() function with M3 mapped one
new_func = '''@Composable
fun appTypography(): Typography {
    val baseline = Typography()
    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = AppTypography.displayBold, letterSpacing = (-1.0).sp),
        displayMedium = baseline.displayMedium.copy(fontFamily = AppTypography.displaySemiBold, letterSpacing = (-0.5).sp),
        displaySmall = baseline.displaySmall.copy(fontFamily = AppTypography.displayMediumFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = AppTypography.displayBold, letterSpacing = (-0.5).sp),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = AppTypography.displaySemiBold),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = AppTypography.displaySemiBold),
        titleLarge = baseline.titleLarge.copy(fontFamily = AppTypography.displayBold),
        titleMedium = baseline.titleMedium.copy(fontFamily = AppTypography.displaySemiBold),
        titleSmall = baseline.titleSmall.copy(fontFamily = AppTypography.displayMediumFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = AppTypography.bodyFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = AppTypography.bodyFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = AppTypography.bodyFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = AppTypography.displayBold, letterSpacing = 0.5.sp),
        labelMedium = baseline.labelMedium.copy(fontFamily = AppTypography.displayMediumFamily, letterSpacing = 0.5.sp),
        labelSmall = baseline.labelSmall.copy(fontFamily = AppTypography.displayMediumFamily, letterSpacing = 0.5.sp)
    )
}'''

content = re.sub(r'@Composable\s*fun appTypography\(\):\s*Typography\s*=\s*Typography\([^)]+\)', new_func, content, flags=re.MULTILINE|re.DOTALL)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
