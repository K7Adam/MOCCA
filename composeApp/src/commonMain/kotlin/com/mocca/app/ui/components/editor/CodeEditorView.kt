package com.mocca.app.ui.components.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable code editor composable backed by a WebView with highlight.js.
 *
 * @param content    Source code to display or edit.
 * @param language   Language identifier for syntax highlighting
 *                   (e.g. "kotlin", "python", "json").
 * @param editable   `true` for textarea edit mode, `false` for read-only
 *                   highlighted view.
 * @param onContentChanged  Fires when the user changes text in edit mode.
 * @param onReady           Fires once the WebView has loaded and is ready.
 * @param modifier          Standard Compose modifier.
 */
@Composable
expect fun CodeEditorView(
    content: String,
    language: String,
    editable: Boolean,
    onContentChanged: (String) -> Unit,
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
)
