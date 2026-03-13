import os

shapes_file = r'composeApp/src/commonMain/kotlin/com/mocca/app/ui/theme/AppShapes.kt'
with open(shapes_file, 'r', encoding='utf-8') as f:
    shapes_content = f.read()

new_shapes = '''    val messageBubbleUser = RoundedCornerShape(topStart = 24.dp, topEnd = 2.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
    val messageBubbleAgent = RoundedCornerShape(topStart = 2.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
    val bottomSheetExpanded = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
'''
if 'messageBubbleUser' not in shapes_content:
    # Insert right before the closing brace of the object
    shapes_content = shapes_content.replace('    val filePreview = RoundedCornerShape(16.dp)\n}', '    val filePreview = RoundedCornerShape(16.dp)\n' + new_shapes + '}')
    with open(shapes_file, 'w', encoding='utf-8') as f:
        f.write(shapes_content)

typo_file = r'composeApp/src/commonMain/kotlin/com/mocca/app/ui/theme/AppTypography.kt'
with open(typo_file, 'r', encoding='utf-8') as f:
    typo_content = f.read()

if 'displayLargeEmphasized' not in typo_content:
    emph_styles = '''
    val displayLargeEmphasized: TextStyle @Composable get() = TextStyle(fontFamily = displayEmphasized, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-1.0).sp)
    val titleMediumEmphasized: TextStyle @Composable get() = TextStyle(fontFamily = displayEmphasized, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp)
'''
    typo_content = typo_content.replace('    val displayLarge: TextStyle @Composable get() = MaterialTheme.typography.displayLarge', emph_styles + '    val displayLarge: TextStyle @Composable get() = MaterialTheme.typography.displayLarge')
    with open(typo_file, 'w', encoding='utf-8') as f:
        f.write(typo_content)

def add_import(file_path, import_stmt):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    if import_stmt not in content:
        content = content.replace('import androidx.compose.runtime.Composable', import_stmt + '\nimport androidx.compose.runtime.Composable')
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

import_appshapes = 'import com.mocca.app.ui.theme.AppShapes'
add_import(r'composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModelSelectorDialog.kt', import_appshapes)
add_import(r'composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/VariantSelectorDialog.kt', import_appshapes)
add_import(r'composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/navigation/AgentSelectorSheet.kt', import_appshapes)
add_import(r'composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/onboarding/OnboardingConnectStep.kt', import_appshapes)
