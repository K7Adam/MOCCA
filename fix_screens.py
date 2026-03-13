import os
import re

dir_path = r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui'

shapes_file = os.path.join(dir_path, 'theme', 'AppShapes.kt')
with open(shapes_file, 'r', encoding='utf-8') as f:
    shapes_content = f.read()

new_shapes = '''    val messageBubbleUser = RoundedCornerShape(topStart = 24.dp, topEnd = 2.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
    val messageBubbleAgent = RoundedCornerShape(topStart = 2.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
    val bottomSheetExpanded = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
'''
shapes_content = re.sub(r'val filePreview = RoundedCornerShape\(16\.dp\)', r'val filePreview = RoundedCornerShape(16.dp)\n' + new_shapes, shapes_content)
with open(shapes_file, 'w', encoding='utf-8') as f:
    f.write(shapes_content)

for root, dirs, files in os.walk(dir_path):
    for file in files:
        if file.endswith('.kt') and file != 'AppShapes.kt':
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()

            original_content = content
            
            # Phase 1: Shapes
            content = re.sub(r'RoundedCornerShape\(topStart = 24\.dp,\s*topEnd = 2\.dp,\s*bottomEnd = 24\.dp,\s*bottomStart = 24\.dp\)', 'AppShapes.messageBubbleUser', content)
            content = re.sub(r'RoundedCornerShape\(topStart = 2\.dp,\s*topEnd = 24\.dp,\s*bottomEnd = 24\.dp,\s*bottomStart = 24\.dp\)', 'AppShapes.messageBubbleAgent', content)
            content = re.sub(r'RoundedCornerShape\(topStart = 16\.dp,\s*topEnd = 16\.dp\)', 'AppShapes.bottomSheetExpanded', content)
            content = re.sub(r'androidx\.compose\.foundation\.shape\.RoundedCornerShape\(topStart = 16\.dp,\s*topEnd = 16\.dp\)', 'AppShapes.bottomSheetExpanded', content)
            content = re.sub(r'androidx\.compose\.foundation\.shape\.RoundedCornerShape\(AppSpacing\.cornerRadiusMedium\)', 'AppShapes.medium', content)
            content = re.sub(r'RoundedCornerShape\(32\.dp\)', 'AppShapes.extraExtraLarge', content)
            
            # Phase 2: Typography
            # Remove inline fontSize and letterSpacing
            content = re.sub(r',\s*fontSize\s*=\s*\d+\.sp', '', content)
            content = re.sub(r',\s*letterSpacing\s*=\s*[\d.]+\.sp', '', content)
            content = re.sub(r'fontSize\s*=\s*\d+\.sp\s*,?', '', content)
            content = re.sub(r'letterSpacing\s*=\s*[\d.]+\.sp\s*,?', '', content)

            if content != original_content:
                # Cleanup leftover trailing commas before closing parenthesis
                content = re.sub(r',\s*\)', '\n)', content)
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated {path}")
