import os
import re

ui_dir = r"C:\Users\ruzaq\AndroidStudioProjects\MOCCA\composeApp\src\commonMain\kotlin\com\mocca\app\ui"

def ensure_imports(content, imports):
    lines = content.split('\n')
    package_idx = -1
    import_idx = -1
    
    for i, line in enumerate(lines):
        if line.startswith('package '):
            package_idx = i
        elif line.startswith('import '):
            import_idx = i
            
    insert_idx = import_idx if import_idx != -1 else (package_idx + 1 if package_idx != -1 else 0)
    
    for imp in imports:
        if imp not in content:
            lines.insert(insert_idx, imp)
            
    return '\n'.join(lines)

def process_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    orig_content = content

    # Replace slowEffectsSpec() in infiniteRepeatable
    content = re.sub(
        r'animation\s*=\s*MaterialTheme\.motionScheme\.slowEffectsSpec\(\)',
        r'animation = tween(2000, easing = LinearEasing)',
        content
    )
    
    # Replace fastEffectsSpec() in infiniteRepeatable
    content = re.sub(
        r'animation\s*=\s*MaterialTheme\.motionScheme\.fastEffectsSpec\(\)',
        r'animation = tween(800, easing = LinearEasing)',
        content
    )

    # Replace defaultEffectsSpec() in infiniteRepeatable
    content = re.sub(
        r'animation\s*=\s*MaterialTheme\.motionScheme\.defaultEffectsSpec\(\)',
        r'animation = tween(1000, easing = LinearEasing)',
        content
    )

    # For fadeIn / fadeOut animationSpec replace
    # fadeIn(animationSpec = MaterialTheme.motionScheme.slowEffectsSpec()) -> fadeIn(animationSpec = tween(1500))
    content = re.sub(
        r'fadeIn\(\s*animationSpec\s*=\s*MaterialTheme\.motionScheme\.slowEffectsSpec\(\)\s*\)',
        r'fadeIn(animationSpec = tween(1500))',
        content
    )
    content = re.sub(
        r'fadeOut\(\s*animationSpec\s*=\s*MaterialTheme\.motionScheme\.slowEffectsSpec\(\)\s*\)',
        r'fadeOut(animationSpec = tween(1500))',
        content
    )
    content = re.sub(
        r'fadeIn\(\s*animationSpec\s*=\s*MaterialTheme\.motionScheme\.defaultEffectsSpec\(\)\s*\)',
        r'fadeIn(animationSpec = tween(1000))',
        content
    )
    content = re.sub(
        r'fadeOut\(\s*animationSpec\s*=\s*MaterialTheme\.motionScheme\.defaultEffectsSpec\(\)\s*\)',
        r'fadeOut(animationSpec = tween(1000))',
        content
    )

    # Missing imports to check
    needed_imports = []
    if 'tween(' in content:
        needed_imports.append('import androidx.compose.animation.core.tween')
    if 'LinearEasing' in content:
        needed_imports.append('import androidx.compose.animation.core.LinearEasing')
    if 'FastOutSlowInEasing' in content:
        needed_imports.append('import androidx.compose.animation.core.FastOutSlowInEasing')

    if needed_imports:
        content = ensure_imports(content, needed_imports)

    if content != orig_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {file_path}")

for root, _, files in os.walk(ui_dir):
    for file in files:
        if file.endswith('.kt'):
            process_file(os.path.join(root, file))

