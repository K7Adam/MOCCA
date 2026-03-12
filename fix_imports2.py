import sys
import re

def fix_mocca_button():
    f = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/components/modern/MoccaButton.kt'
    with open(f, 'r', encoding='utf-8') as file:
        c = file.read()
    if 'import androidx.compose.material.icons.filled.KeyboardArrowDown' not in c:
        c = c.replace('import androidx.compose.material.icons.filled.Edit', 
                      'import androidx.compose.material.icons.filled.Edit\nimport androidx.compose.material.icons.filled.KeyboardArrowDown')
    with open(f, 'w', encoding='utf-8') as file:
        file.write(c)

def fix_chat_content():
    f = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/chat/ChatContent.kt'
    with open(f, 'r', encoding='utf-8') as file:
        c = file.read()
    if 'import androidx.compose.foundation.border' not in c:
        c = c.replace('import androidx.compose.foundation.layout.*', 'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.border\nimport androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.animation.AnimatedVisibility')
    
    # Fix the AnimatedVisibility call
    c = c.replace('AnimatedVisibility(\n                visible = showHeroMoment,', 'androidx.compose.animation.AnimatedVisibility(\n                visible = showHeroMoment,')
    
    with open(f, 'w', encoding='utf-8') as file:
        file.write(c)

def fix_main_screen():
    f = 'composeApp/src/commonMain/kotlin/com/mocca/app/ui/screens/main/MainScreen.kt'
    with open(f, 'r', encoding='utf-8') as file:
        c = file.read()
    
    # Add imports to top
    if 'import androidx.compose.animation.AnimatedContent' not in c[:1000]:
        c = c.replace('import androidx.compose.foundation.layout.*', 'import androidx.compose.foundation.layout.*\nimport androidx.compose.animation.AnimatedContent\nimport com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope\nimport androidx.compose.runtime.CompositionLocalProvider')
    
    # Remove the broken block
    broken_block = """import androidx.compose.animation.AnimatedContent
import com.mocca.app.ui.navigation.LocalNavAnimatedVisibilityScope
// ...
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
// ..."""
    c = c.replace(broken_block, "")
    
    # There is also an @OptIn that might be near the broken block
    c = re.sub(r'@OptIn\([^)]+\)', '', c)

    with open(f, 'w', encoding='utf-8') as file:
        file.write(c)

fix_mocca_button()
fix_chat_content()
fix_main_screen()
print("Fixed files!")
