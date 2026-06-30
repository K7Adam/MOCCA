package com.mocca.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * MOCCA Color Palette — Material 3 Expressive Tonal Hierarchy.
 *
 * Seed: #AFC2FF (AnchorPrimary)
 * Neutral Seed: #0B0C10 (DarkBackgroundSeed)
 */
@Immutable
object AppColors {
    // ---------------------------------------------------------------------------
    // BRAND ANCHOR COLORS
    // ---------------------------------------------------------------------------
    val AnchorPrimary = Color(0xFFAFC2FF)
    val AnchorSecondary = Color(0xFF8EDDD0)
    val AnchorTertiary = Color(0xFFF0B8C7)
    val DarkBackgroundSeed = Color(0xFF0B0C10)

    // M3 PRIMARY TONAL SCALE (Accents)

    
    val primary = Color(0xFFAFC2FF) // Tone 80
    val onPrimary = Color(0xFF16203F) // Tone 20
    val primaryContainer = Color(0xFF33405F) // Tone 30
    val onPrimaryContainer = Color(0xFFDDE5FF) // Tone 90
    val inversePrimary = Color(0xFF51628F) // Tone 40

    // M3 SECONDARY TONAL SCALE

    
    val secondary = Color(0xFF8EDDD0)
    val onSecondary = Color(0xFF063A35)
    val secondaryContainer = Color(0xFF224E49)
    val onSecondaryContainer = Color(0xFFC3F3EC)

    val tertiary = Color(0xFFF0B8C7)
    val onTertiary = Color(0xFF4A1625)
    val tertiaryContainer = Color(0xFF623243)
    val onTertiaryContainer = Color(0xFFFFD9E3)

    // M3 NEUTRAL TONAL SCALE (Surfaces)


    val background = Color(0xFF0B0C10) // Tone 6
    val onBackground = Color(0xFFE7E9F2) // Tone 90
    
    val surface = Color(0xFF0D0E12) // Tone 7
    val onSurface = Color(0xFFE7E9F2) // Tone 90
    
    val surfaceVariant = Color(0xFF252834) // Tone 20
    val onSurfaceVariant = Color(0xFFB7BBC9) // Tone 70
    
    val surfaceTint = primary // Brand primary for surface tinting
    val inverseSurface = Color(0xFFE7E9F2) // Tone 90
    val inverseOnSurface = Color(0xFF15161C) // Tone 10
    
    val surfaceDim = Color(0xFF050608) // Tone 4
    val surfaceBright = Color(0xFF343846) // Tone 24
    
    val onSurfaceDim = onSurface.copy(alpha = 0.7f)
    val onSurfaceVariantLight = onSurfaceVariant.copy(alpha = 0.8f)
    val textPlaceholder = onSurfaceVariant.copy(alpha = 0.4f)
    
    val surfaceContainerLowest = Color(0xFF0A0B0F) // Tone 6
    val surfaceContainerLow = Color(0xFF111219) // Tone 10
    val surfaceContainer = Color(0xFF171922) // Tone 12
    val surfaceContainerHigh = Color(0xFF20232D) // Tone 17
    val surfaceContainerHighest = Color(0xFF2A2D38) // Tone 22

    // ---------------------------------------------------------------------------
    // BORDERLESS DESIGN SYSTEM — "Depth Through Color, Not Lines"
    // ---------------------------------------------------------------------------
    // Background layer tokens: each step up adds visual separation without borders
    val bgBase = Color(0xFF0B0C10)         // Base layer (matches background)
    val bgRaised = Color(0xFF171922)       // Raised surface - one step up from base
    val bgOverlay = Color(0xFF20232D)      // Overlay/modal background
    val bgElevated = Color(0xFF2A2D38)     // Elevated elements (FABs, floating cards)

    // Foreground opacity hierarchy: text importance through opacity, not color
    val fgMuted = onBackground.copy(alpha = 0.6f)   // Secondary text
    val fgSubtle = onBackground.copy(alpha = 0.4f)   // Tertiary/hint text

    // Diff viewer tokens (Wave 2 diff viewer)
    val diffAdditionLine = Color(0xFF22C55E)         // Bright green for diff additions
    val diffDeletionLine = Color(0xFFEF4444)          // Bright red for diff deletions
    val diffHunkHeader = onBackground.copy(alpha = 0.3f)  // Muted for @@ hunks
    val diffFileHeader = primary.copy(alpha = 0.8f)        // Accent for file paths

    val moduleBackground = surfaceContainer

    val outline = Color(0xFF676B7B) // Tone 44
    val outlineVariant = Color(0xFF3D4150) // Tone 35

    val error = Color(0xFFEF5350)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)
    val errorDim = error.copy(alpha = 0.1f)

    val scrim = Color(0xCC000000)
    
    val textSecondary = onSurfaceVariant
    val grey = outlineVariant

    // EXTENDED UI COLORS


    val success = Color(0xFF4CAF50)
    val warning = Color(0xFFFFB74D)
    
    val statusOnline = Color(0xFF4CAF50)
    val statusOffline = Color(0xFFEF5350)
    val statusWaiting = Color(0xFFFFB74D)
    val statusThinking = primary
    val statusProcessing = secondary

    // Semantic Status Colors (explicit tokens for status semantics)
    val statusSuccess = Color(0xFF4CAF50)
    val statusError = Color(0xFFEF5350)
    val statusWarning = Color(0xFFFFB74D)
    val statusInfo = primary

    val white = Color(0xFFFFFFFF)
    val greyLight = outline // Alias for consistency
    val greyDark = surfaceContainerHighest // Alias for consistency
    val onSurfaceVariantDark = surfaceContainerHighest // Alias for legacy buttons

    val accent = primary
    val accentBright = Color(0xFFC9D4FF)
    val accentGreen = Color(0xFF4CAF50)
    
    val badgeBackground = surfaceContainerHighest
    val badgeText = onSurface
    
    val inputGlow = Color(0x40AFC2FF)

    // Code Colors
    val syntaxKeyword = Color(0xFFC586C0)
    val syntaxFunction = Color(0xFF61DAFB)
    val syntaxString = Color(0xFFCE9178)
    val syntaxType = Color(0xFF9E9E9E)
    val syntaxComment = Color(0xFF6A9955)
    val syntaxPunctuation = Color(0xFFD4D4D4)
    val diffAddition = Color(0x1A4CAF50)
    val diffDeletion = Color(0x1AEF5350)
    val diffAdditionText = Color(0xFF4CAF50)
    val diffDeletionText = Color(0xFFEF5350)
    val lineNumbers = Color(0xFF666666)
    
    val fileTsx = Color(0xFF61DAFB)
    val fileCss = Color(0xFF9E9E9E)
    val fileJson = Color(0xFFEF5350)

    // Shimmer
    val shimmerBase = surfaceContainer
    val shimmerHighlight = surfaceContainerHighest
    val shimmerAccent = primary.copy(alpha = 0.12f)
}
