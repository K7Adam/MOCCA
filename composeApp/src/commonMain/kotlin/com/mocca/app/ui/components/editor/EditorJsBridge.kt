package com.mocca.app.ui.components.editor

/**
 * Contract for the JavaScript bridge between the WebView editor and Kotlin.
 *
 * The WebView editor HTML calls these methods via `AndroidBridge.onXxx()`
 * to communicate state changes back to the native layer.
 */
interface EditorJsBridge {

    /** Called when the user modifies content in edit mode. */
    fun onContentChanged(content: String)

    /** Called once the editor HTML has finished loading and is ready for commands. */
    fun onReady()
}
