import sys

def fix_mocca_button():
    f = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/MoccaButton.kt'
    with open(f, 'r', encoding='utf-8') as file:
        c = file.read()
    if 'import androidx.compose.material.icons.filled.KeyboardArrowDown' not in c:
        c = c.replace('import androidx.compose.material.icons.filled.Edit', 
                      'import androidx.compose.material.icons.filled.Edit\nimport androidx.compose.material.icons.filled.KeyboardArrowDown')
    # Let's also fix standardFloatingToolbarColors just in case MoccaButton has it (it doesn't, but just in case)
    with open(f, 'w', encoding='utf-8') as file:
        file.write(c)

def fix_top_bar():
    f = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/ModernTopBar.kt'
    with open(f, 'r', encoding='utf-8') as file:
        c = file.read()
    c = c.replace('standardFloatingToolbarColors(containerColor = AppColors.surface, contentColor = AppColors.white)', 'standardFloatingToolbarColors()')
    with open(f, 'w', encoding='utf-8') as file:
        file.write(c)

def fix_chat_input_bar():
    f = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/navigation/ChatInputBar.kt'
    with open(f, 'r', encoding='utf-8') as file:
        c = file.read()
    c = c.replace('standardFloatingToolbarColors(containerColor = AppColors.surface, contentColor = AppColors.white)', 'standardFloatingToolbarColors()')
    with open(f, 'w', encoding='utf-8') as file:
        file.write(c)

def fix_chat_content():
    f = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatContent.kt'
    with open(f, 'r', encoding='utf-8') as file:
        c = file.read()
    imports = """import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
"""
    if 'import androidx.compose.animation.AnimatedVisibility' not in c:
        c = c.replace('import androidx.compose.foundation.layout.*', imports + 'import androidx.compose.foundation.layout.*')
    with open(f, 'w', encoding='utf-8') as file:
        file.write(c)

def fix_main_screen():
    f = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt'
    with open(f, 'r', encoding='utf-8') as file:
        c = file.read()
    imports = """import androidx.compose.animation.AnimatedContent
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope
import androidx.compose.runtime.CompositionLocalProvider
"""
    if 'import androidx.compose.animation.AnimatedContent' not in c:
        c = c.replace('import androidx.compose.foundation.layout.*', imports + 'import androidx.compose.foundation.layout.*')
    with open(f, 'w', encoding='utf-8') as file:
        file.write(c)

fix_mocca_button()
fix_top_bar()
fix_chat_input_bar()
fix_chat_content()
fix_main_screen()
print("Fixed!")
