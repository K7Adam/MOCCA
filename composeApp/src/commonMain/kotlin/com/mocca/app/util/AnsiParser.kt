package com.mocca.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.mocca.app.ui.theme.AppColors

// ANSI COLOR PARSING (Priority 5.2)


/**
 * ANSI escape sequence parser for terminal output.
 * 
 * Supports:
 * - Standard colors (30-37, 40-47)
 * - Bright colors (90-97, 100-107)
 * - 256-color mode (38;5;n, 48;5;n)
 * - RGB true color (38;2;r;g;b, 48;2;r;g;b)
 * - Text styles (bold, italic, underline, strikethrough)
 * - Reset sequences
 */
object AnsiParser {
    
    // ANSI escape sequence pattern: ESC [ <params> m
    private val ANSI_PATTERN = Regex("\u001B\\[([0-9;]*)m")
    
    // Standard ANSI colors (indices 0-7)
    private val STANDARD_COLORS = listOf(
        Color(0xFF000000), // Black
        Color(0xFFCD0000), // Red
        Color(0xFF00CD00), // Green
        Color(0xFFCDCD00), // Yellow
        Color(0xFF0000EE), // Blue
        Color(0xFFCD00CD), // Magenta
        Color(0xFF00CDCD), // Cyan
        Color(0xFFE5E5E5)  // White
    )
    
    // Bright ANSI colors (indices 8-15)
    private val BRIGHT_COLORS = listOf(
        Color(0xFF7F7F7F), // Bright Black (Gray)
        Color(0xFFFF0000), // Bright Red
        Color(0xFF00FF00), // Bright Green
        Color(0xFFFFFF00), // Bright Yellow
        Color(0xFF5C5CFF), // Bright Blue
        Color(0xFFFF00FF), // Bright Magenta
        Color(0xFF00FFFF), // Bright Cyan
        Color(0xFFFFFFFF)  // Bright White
    )
    
    /**
     * Current text style state.
     */
    private data class AnsiStyle(
        val foreground: Color? = null,
        val background: Color? = null,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strikethrough: Boolean = false,
        val dim: Boolean = false
    ) {
        fun toSpanStyle(defaultForeground: Color): SpanStyle {
            val fg = if (dim && foreground != null) {
                foreground.copy(alpha = 0.5f)
            } else {
                foreground ?: defaultForeground
            }
            
            return SpanStyle(
                color = fg,
                background = background ?: Color.Transparent,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = when {
                    underline && strikethrough -> TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                    )
                    underline -> TextDecoration.Underline
                    strikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                }
            )
        }
    }
    
    /**
     * Parse ANSI escape sequences and return an AnnotatedString with proper styling.
     * 
     * @param text Raw text containing ANSI escape sequences
     * @param defaultForeground Default text color when no ANSI color is set
     * @return AnnotatedString with styles applied
     */
    fun parse(
        text: String,
        defaultForeground: Color = AppColors.statusOnline
    ): AnnotatedString {
        if (!text.contains("\u001B[")) {
            // Fast path: no ANSI sequences
            return AnnotatedString(text)
        }
        
        return buildAnnotatedString {
            var currentStyle = AnsiStyle()
            var lastEnd = 0
            
            ANSI_PATTERN.findAll(text).forEach { match ->
                // Append text before this escape sequence with current style
                if (match.range.first > lastEnd) {
                    val segment = text.substring(lastEnd, match.range.first)
                    withStyle(currentStyle.toSpanStyle(defaultForeground)) {
                        append(segment)
                    }
                }
                
                // Parse the escape sequence and update style
                val params = match.groupValues[1]
                currentStyle = parseSequence(params, currentStyle)
                
                lastEnd = match.range.last + 1
            }
            
            // Append remaining text
            if (lastEnd < text.length) {
                val segment = text.substring(lastEnd)
                withStyle(currentStyle.toSpanStyle(defaultForeground)) {
                    append(segment)
                }
            }
        }
    }
    
    /**
     * Strip all ANSI escape sequences from text.
     */
    fun stripAnsi(text: String): String {
        return text.replace(ANSI_PATTERN, "")
    }
    
    /**
     * Parse ANSI sequence parameters and update style.
     */
    private fun parseSequence(params: String, current: AnsiStyle): AnsiStyle {
        if (params.isEmpty() || params == "0") {
            // Reset all attributes
            return AnsiStyle()
        }
        
        var style = current
        val codes = params.split(";").mapNotNull { it.toIntOrNull() }
        var i = 0
        
        while (i < codes.size) {
            val code = codes[i]
            when (code) {
                // Reset
                0 -> style = AnsiStyle()
                
                // Text styles
                1 -> style = style.copy(bold = true)
                2 -> style = style.copy(dim = true)
                3 -> style = style.copy(italic = true)
                4 -> style = style.copy(underline = true)
                9 -> style = style.copy(strikethrough = true)
                
                // Reset specific styles
                21, 22 -> style = style.copy(bold = false, dim = false)
                23 -> style = style.copy(italic = false)
                24 -> style = style.copy(underline = false)
                29 -> style = style.copy(strikethrough = false)
                
                // Standard foreground colors (30-37)
                in 30..37 -> {
                    style = style.copy(foreground = STANDARD_COLORS[code - 30])
                }
                
                // Default foreground
                39 -> style = style.copy(foreground = null)
                
                // Standard background colors (40-47)
                in 40..47 -> {
                    style = style.copy(background = STANDARD_COLORS[code - 40])
                }
                
                // Default background
                49 -> style = style.copy(background = null)
                
                // Bright foreground colors (90-97)
                in 90..97 -> {
                    style = style.copy(foreground = BRIGHT_COLORS[code - 90])
                }
                
                // Bright background colors (100-107)
                in 100..107 -> {
                    style = style.copy(background = BRIGHT_COLORS[code - 100])
                }
                
                // Extended color modes
                38 -> {
                    // Foreground: 38;5;n (256-color) or 38;2;r;g;b (true color)
                    if (i + 1 < codes.size) {
                        when (codes[i + 1]) {
                            5 -> {
                                // 256-color mode
                                if (i + 2 < codes.size) {
                                    val colorIndex = codes[i + 2]
                                    style = style.copy(foreground = get256Color(colorIndex))
                                    i += 2
                                }
                            }
                            2 -> {
                                // True color mode
                                if (i + 4 < codes.size) {
                                    val r = codes[i + 2].coerceIn(0, 255)
                                    val g = codes[i + 3].coerceIn(0, 255)
                                    val b = codes[i + 4].coerceIn(0, 255)
                                    style = style.copy(foreground = Color(r, g, b))
                                    i += 4
                                }
                            }
                        }
                    }
                }
                
                48 -> {
                    // Background: 48;5;n (256-color) or 48;2;r;g;b (true color)
                    if (i + 1 < codes.size) {
                        when (codes[i + 1]) {
                            5 -> {
                                // 256-color mode
                                if (i + 2 < codes.size) {
                                    val colorIndex = codes[i + 2]
                                    style = style.copy(background = get256Color(colorIndex))
                                    i += 2
                                }
                            }
                            2 -> {
                                // True color mode
                                if (i + 4 < codes.size) {
                                    val r = codes[i + 2].coerceIn(0, 255)
                                    val g = codes[i + 3].coerceIn(0, 255)
                                    val b = codes[i + 4].coerceIn(0, 255)
                                    style = style.copy(background = Color(r, g, b))
                                    i += 4
                                }
                            }
                        }
                    }
                }
            }
            i++
        }
        
        return style
    }
    
    /**
     * Get color from 256-color palette.
     * 
     * 0-7: Standard colors
     * 8-15: Bright colors
     * 16-231: 6x6x6 color cube
     * 232-255: Grayscale
     */
    private fun get256Color(index: Int): Color {
        return when {
            index < 8 -> STANDARD_COLORS[index]
            index < 16 -> BRIGHT_COLORS[index - 8]
            index < 232 -> {
                // 6x6x6 color cube
                val adjusted = index - 16
                val r = (adjusted / 36) * 51
                val g = ((adjusted / 6) % 6) * 51
                val b = (adjusted % 6) * 51
                Color(r, g, b)
            }
            else -> {
                // Grayscale (232-255 -> 8-238)
                val gray = 8 + (index - 232) * 10
                Color(gray, gray, gray)
            }
        }
    }
}

/**
 * Extension function to parse ANSI sequences in a string.
 */
fun String.parseAnsi(defaultColor: Color = AppColors.statusOnline): AnnotatedString {
    return AnsiParser.parse(this, defaultColor)
}

/**
 * Extension function to strip ANSI sequences from a string.
 */
fun String.stripAnsi(): String {
    return AnsiParser.stripAnsi(this)
}
