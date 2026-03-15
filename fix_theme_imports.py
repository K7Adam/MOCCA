import os
import re

target_dir = 'composeApp/src/commonMain/kotlin'

replacements = [
    (r'\bMaterialTheme\.typography\b', 'AppTheme.typography'),
    (r'\bMaterialTheme\.colorScheme\b', 'AppTheme.colors'),
    (r'\bMaterialTheme\.shapes\b', 'AppTheme.shapes')
]

updated_count = 0

for root, _, files in os.walk(target_dir):
    for file in files:
        if not file.endswith('.kt'):
            continue
        
        filepath = os.path.join(root, file)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            
        modified = False
        new_content = content
        
        for old, new in replacements:
            if re.search(old, new_content):
                new_content = re.sub(old, new, new_content)
                modified = True
                
        if modified:
            # Check if import exists
            import_stmt = 'import com.mocca.app.ui.theme.AppTheme'
            if import_stmt not in new_content:
                # Find the last import and append there, or after package
                if 'import ' in new_content:
                    # Find the last import
                    last_import_idx = new_content.rfind('import ')
                    end_of_line = new_content.find('\n', last_import_idx)
                    if end_of_line == -1:
                        end_of_line = len(new_content)
                    new_content = new_content[:end_of_line] + f'\n{import_stmt}' + new_content[end_of_line:]
                else:
                    new_content = re.sub(r'^(package\s+[^\n]+)', r'\1\n\n' + import_stmt, new_content, count=1, flags=re.MULTILINE)
            
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Updated {filepath}")
            updated_count += 1

print(f"Total files updated: {updated_count}")
