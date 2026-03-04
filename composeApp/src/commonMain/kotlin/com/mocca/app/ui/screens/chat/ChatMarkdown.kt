package com.mocca.app.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppTypography

@Composable
fun MarkdownText(
    markdown: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    onFileClick: ((String) -> Unit)? = null
) {
    val mdColor = markdownColor(
        text = color,
        codeText = AppColors.accent,
        codeBackground = AppColors.surfaceContainer,
        inlineCodeText = AppColors.accent,
        inlineCodeBackground = AppColors.surfaceContainer,
        linkText = AppColors.accent
    )
    
    val mdTypography = markdownTypography(
        text = style,
        code = style.copy(fontSize = 12.sp, color = AppColors.accent, fontFamily = FontFamily.Monospace),
        h1 = AppTypography.headlineMedium.copy(color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        h2 = AppTypography.headlineSmall.copy(color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        h3 = AppTypography.titleLarge.copy(color = color, fontWeight = FontWeight.Bold),
        h4 = AppTypography.titleMedium.copy(color = color, fontWeight = FontWeight.Bold),
        h5 = AppTypography.titleSmall.copy(color = color, fontWeight = FontWeight.Bold),
        h6 = AppTypography.labelLarge.copy(color = color, fontWeight = FontWeight.Bold)
    )

    // Pre-process: wrap bare absolute file paths as markdown links so they become tappable
    val processedMarkdown = remember(markdown, onFileClick) {
        if (onFileClick == null) markdown
        else highlightFilePaths(markdown)
    }

    if (onFileClick != null) {
        val customUriHandler = remember(onFileClick) {
            object : UriHandler {
                override fun openUri(uri: String) {
                    if (uri.startsWith("file://")) {
                        onFileClick.invoke(uri.removePrefix("file:///").let {
                            if (it.startsWith("/")) it else "/$it"
                        })
                    }
                }
            }
        }
        CompositionLocalProvider(LocalUriHandler provides customUriHandler) {
            Markdown(
                content = processedMarkdown,
                colors = mdColor,
                typography = mdTypography,
                modifier = modifier
            )
        }
    } else {
        Markdown(
            content = processedMarkdown,
            colors = mdColor,
            typography = mdTypography,
            modifier = modifier
        )
    }
}

/** Regex to match bare absolute file paths like /path/to/file.ts or src/foo/bar.kt */
private val FILE_PATH_REGEX = Regex(
    """(?<![`\[\(\w])((?:/[\w.@:+\-]{1,}){2,}(?:\.[a-zA-Z]{1,8})|[a-zA-Z][\w.+\-]*/[\w./@:+\-]{3,}(?:\.[a-zA-Z]{1,8}))(?![\]\)])"""
)

/**
 * Wraps bare absolute file paths in the markdown text as clickable [path](file:///path) links.
 * Already-linked text (in []() or `backticks`) is left untouched.
 */
private fun highlightFilePaths(markdown: String): String {
    return markdown.replace(FILE_PATH_REGEX) { mr ->
        val path = mr.value
        val url = if (path.startsWith("/")) "file://$path" else "file:///$path"
        "[$path]($url)"
    }
}
