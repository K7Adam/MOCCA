import os
import re

directory = "composeApp/src"

# Regex for OptIns
optin_block = re.compile(r'@OptIn\([^)]+\)')
import_experimental = re.compile(r'^import\s+androidx\.compose\.(material3|animation|foundation)\.Experimental.*\n', re.MULTILINE)
import_encoding = re.compile(r'^import\s+kotlin\.io\.encoding\.ExperimentalEncodingApi\n', re.MULTILINE)
import_coroutines = re.compile(r'^import\s+kotlinx\.coroutines\.FlowPreview\n', re.MULTILINE)

for root, _, files in os.walk(directory):
    for file in files:
        if not file.endswith(".kt"):
            continue
        
        filepath = os.path.join(root, file)
        with open(filepath, 'r', encoding='utf-8') as f:
            original_content = f.read()
            
        content = original_content
        
        # Remove OptIns
        content = optin_block.sub('', content)
        content = import_experimental.sub('', content)
        content = import_encoding.sub('', content)
        content = import_coroutines.sub('', content)
        
        # Clean up empty lines left by OptIns
        content = re.sub(r'\n[ \t]*\n[ \t]*\n', '\n\n', content)
        
        # Fix SplitButton APIs
        content = content.replace('SplitButtonDefaults.FilledLeadingButton', 'SplitButtonDefaults.LeadingButton')
        content = content.replace('SplitButtonDefaults.filledLeadingButtonColors', 'androidx.compose.material3.ButtonDefaults.buttonColors')
        content = content.replace('SplitButtonDefaults.FilledTrailingButton', 'SplitButtonDefaults.TrailingButton')
        content = content.replace('SplitButtonDefaults.filledTrailingButtonColors', 'androidx.compose.material3.ButtonDefaults.buttonColors')
        
        # Fix HorizontalFloatingToolbar APIs
        # From: HorizontalFloatingToolbar(..., containerColor = X, contentColor = Y, ...)
        # To: HorizontalFloatingToolbar(..., colors = FloatingToolbarDefaults.horizontalToolbarColors(containerColor = X, contentColor = Y), ...)
        # We need to do this carefully. 
        if 'HorizontalFloatingToolbar(' in content:
            content = re.sub(
                r'containerColor\s*=\s*([^,]+),\s*contentColor\s*=\s*([^,]+)(?=,|\))',
                r'colors = androidx.compose.material3.FloatingToolbarDefaults.standardFloatingToolbarColors(containerColor = \1, contentColor = \2)',
                content
            )

        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Updated {filepath}")
