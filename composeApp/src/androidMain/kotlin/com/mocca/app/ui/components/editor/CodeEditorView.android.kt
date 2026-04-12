package com.mocca.app.ui.components.editor

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun CodeEditorView(
    content: String,
    language: String,
    editable: Boolean,
    onContentChanged: (String) -> Unit,
    onReady: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val currentOnContentChanged by rememberUpdatedState(onContentChanged)
    val currentOnReady by rememberUpdatedState(onReady)

    var isWebViewReady by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val sentState = remember { SentContentState() }

    AndroidView(
        factory = { ctx ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowContentAccess = false
                settings.allowFileAccess = false
                settings.loadsImagesAutomatically = false
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = false

                webViewClient = object : WebViewClientCompat() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                    override fun onPageFinished(view: WebView, url: String) {
                        isWebViewReady = true
                    }
                }

                addJavascriptInterface(
                    createBridge(sentState, currentOnContentChanged, currentOnReady),
                    "AndroidBridge",
                )

                loadUrl("https://appassets.androidplatform.net/assets/editor.html")
            }.also { webViewRef = it }
        },
        update = { view ->
            if (isWebViewReady) {
                val contentChanged = content != sentState.lastSentContent
                    || language != sentState.lastSentLanguage
                if (contentChanged) {
                    view.evaluateJavascript(
                        "setContent(${escapeForJs(content)}, '${escapeJsString(language)}')",
                        null,
                    )
                    sentState.lastSentContent = content
                    sentState.lastSentLanguage = language
                }
                if (editable != sentState.lastSentEditable) {
                    view.evaluateJavascript("setEditable($editable)", null)
                    sentState.lastSentEditable = editable
                }
            }
        },
        onRelease = { view ->
            isWebViewReady = false
            sentState.reset()
            view.destroy()
        },
        modifier = modifier,
    )

    // Pause / resume WebView with the correct Compose lifecycle owner
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var registeredObserver: LifecycleEventObserver? = null
        val lc = lifecycleOwner.lifecycle
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webViewRef?.onPause()
                Lifecycle.Event.ON_RESUME -> webViewRef?.onResume()
                else -> {}
            }
        }.also { obs ->
            registeredObserver = obs
            lc.addObserver(obs)
        }
        onDispose {
            registeredObserver?.let { obs -> lc.removeObserver(obs) }
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/**
 * Holds the last values pushed into the WebView so we can skip redundant
 * `evaluateJavascript` calls (which would reset cursor position in edit mode).
 */
private class SentContentState {
    @Volatile var lastSentContent: String = ""
    @Volatile var lastSentLanguage: String = ""
    @Volatile var lastSentEditable: Boolean = false

    fun reset() {
        lastSentContent = ""
        lastSentLanguage = ""
        lastSentEditable = false
    }
}

/**
 * Creates the JS bridge object that the WebView's JavaScript calls via
 * `AndroidBridge.onXxx()`.
 */
private fun createBridge(
    sentState: SentContentState,
    onContentChanged: (String) -> Unit,
    onReady: () -> Unit,
): Any =
    object {
        @JavascriptInterface
        fun onContentChanged(newContent: String) {
            // Record so the Compose update block won't re-send this same content
            sentState.lastSentContent = newContent
            onContentChanged(newContent)
        }

        @JavascriptInterface
        fun onReady() {
            onReady()
        }
    }

/**
 * Escapes a string for safe embedding as a JavaScript double-quoted string literal.
 * Produces the surrounding quotes as well, e.g. `"hello\nworld"`.
 */
private fun escapeForJs(content: String): String {
    val sb = StringBuilder(content.length + 16)
    sb.append('"')
    for (c in content) {
        when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> {
                if (c.code < 0x20) {
                    sb.append("\\u")
                    sb.append(c.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
    }
    sb.append('"')
    return sb.toString()
}

/** Minimal escaping for short identifier strings (language name). */
private fun escapeJsString(s: String): String =
    s.replace("\\", "\\\\").replace("'", "\\'")
