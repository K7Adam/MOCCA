import os
import re

search_dirs = [
    r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui\screens',
    r'C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui\components'
]

def cleanup_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = []
    seen_app_shapes = False
    
    for line in lines:
        # Deduplicate AppShapes import
        if 'import com.mocca.app.ui.theme.AppShapes' in line:
            if not seen_app_shapes:
                new_lines.append(line)
                seen_app_shapes = True
            continue
        
        # Restore .sp import if it's still needed (detected by remaining .sp usage)
        if '// Purged .sp' in line:
            # Check if .sp is still used in the rest of the file
            content = "".join(lines)
            if re.search(r'\d+\.sp', content):
                new_lines.append('import androidx.compose.ui.unit.sp\n')
            continue
            
        new_lines.append(line)
    
    if len(new_lines) != len(lines):
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)
        print(f"Cleaned up imports in: {file_path}")

for d in search_dirs:
    for root, _, files in os.walk(d):
        for file in files:
            if file.endswith('.kt'):
                cleanup_file(os.path.join(root, file))

print("\nFallout cleanup complete! Pickle Riiiiick! 🥒")
