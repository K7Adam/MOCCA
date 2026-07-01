package com.mocca.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * MOCCA Color Palette — Material 3 Expressive Mocha Tonal Hierarchy.
 *
 * Seed: Mocha #6F4E37 (HCT H=54, C=20, T=36)
 *
 * All M3 standard color roles delegate to [MaterialTheme.colorScheme] so they
 * automatically reflect the active light/dark/dynamic theme. Extended semantic
 * tokens that have no M3 equivalent are derived from M3 roles or kept as
 * hard-coded values.
 */
@Suppress("TooManyFunctions")
@Immutable
object AppColors {

    // -------------------------------------------------------------------------
    // M3 PRIMARY TONAL SCALE
    // -------------------------------------------------------------------------
    val primary: Color @Composable get() = MaterialTheme.colorScheme.primary
    val onPrimary: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val primaryContainer: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer: Color @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer
    val inversePrimary: Color @Composable get() = MaterialTheme.colorScheme.inversePrimary
    val primaryFixed: Color @Composable get() = MaterialTheme.colorScheme.primaryFixed
    val primaryFixedDim: Color @Composable get() = MaterialTheme.colorScheme.primaryFixedDim
    val onPrimaryFixed: Color @Composable get() = MaterialTheme.colorScheme.onPrimaryFixed
    val onPrimaryFixedVariant: Color @Composable get() = MaterialTheme.colorScheme.onPrimaryFixedVariant

    // -------------------------------------------------------------------------
    // M3 SECONDARY TONAL SCALE
    // -------------------------------------------------------------------------
    val secondary: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val onSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSecondary
    val secondaryContainer: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
    val secondaryFixed: Color @Composable get() = MaterialTheme.colorScheme.secondaryFixed
    val secondaryFixedDim: Color @Composable get() = MaterialTheme.colorScheme.secondaryFixedDim
    val onSecondaryFixed: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryFixed
    val onSecondaryFixedVariant: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryFixedVariant

    // -------------------------------------------------------------------------
    // M3 TERTIARY TONAL SCALE
    // -------------------------------------------------------------------------
    val tertiary: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val onTertiary: Color @Composable get() = MaterialTheme.colorScheme.onTertiary
    val tertiaryContainer: Color @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
    val onTertiaryContainer: Color @Composable get() = MaterialTheme.colorScheme.onTertiaryContainer
    val tertiaryFixed: Color @Composable get() = MaterialTheme.colorScheme.tertiaryFixed
    val tertiaryFixedDim: Color @Composable get() = MaterialTheme.colorScheme.tertiaryFixedDim
    val onTertiaryFixed: Color @Composable get() = MaterialTheme.colorScheme.onTertiaryFixed
    val onTertiaryFixedVariant: Color @Composable get() = MaterialTheme.colorScheme.onTertiaryFixedVariant

    // -------------------------------------------------------------------------
    // M3 ERROR TONAL SCALE
    // -------------------------------------------------------------------------
    val error: Color @Composable get() = MaterialTheme.colorScheme.error
    val onError: Color @Composable get() = MaterialTheme.colorScheme.onError
    val errorContainer: Color @Composable get() = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer: Color @Composable get() = MaterialTheme.colorScheme.onErrorContainer
    val errorDim: Color @Composable get() = error.copy(alpha = 0.1f)

    // -------------------------------------------------------------------------
    // M3 SURFACE / NEUTRAL TONAL SCALE
    // -------------------------------------------------------------------------
    val background: Color @Composable get() = MaterialTheme.colorScheme.background
    val onBackground: Color @Composable get() = MaterialTheme.colorScheme.onBackground

    val surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val onSurface: Color @Composable get() = MaterialTheme.colorScheme.onSurface

    val surfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    val surfaceTint: Color @Composable get() = MaterialTheme.colorScheme.surfaceTint
    val inverseSurface: Color @Composable get() = MaterialTheme.colorScheme.inverseSurface
    val inverseOnSurface: Color @Composable get() = MaterialTheme.colorScheme.inverseOnSurface

    val surfaceDim: Color @Composable get() = MaterialTheme.colorScheme.surfaceDim
    val surfaceBright: Color @Composable get() = MaterialTheme.colorScheme.surfaceBright

    val surfaceContainerLowest: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLowest
    val surfaceContainerLow: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val surfaceContainer: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerHigh: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceContainerHighest: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest

    // -------------------------------------------------------------------------
    // M3 OUTLINE
    // -------------------------------------------------------------------------
    val outline: Color @Composable get() = MaterialTheme.colorScheme.outline
    val outlineVariant: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

    // -------------------------------------------------------------------------
    // M3 SCRIM
    // -------------------------------------------------------------------------
    val scrim: Color @Composable get() = MaterialTheme.colorScheme.scrim

    // -------------------------------------------------------------------------
    // DERIVED SURFACE TOKENS — depth through tonal elevation, not lines
    // -------------------------------------------------------------------------
    val onSurfaceDim: Color @Composable get() = onSurface.copy(alpha = 0.7f)
    val onSurfaceVariantLight: Color @Composable get() = onSurfaceVariant.copy(alpha = 0.8f)
    val textPlaceholder: Color @Composable get() = onSurfaceVariant.copy(alpha = 0.4f)

    val bgBase: Color @Composable get() = background
    val bgRaised: Color @Composable get() = surfaceContainer
    val bgOverlay: Color @Composable get() = surfaceContainerHigh
    val bgElevated: Color @Composable get() = surfaceContainerHighest

    val fgMuted: Color @Composable get() = onBackground.copy(alpha = 0.6f)
    val fgSubtle: Color @Composable get() = onBackground.copy(alpha = 0.4f)

    val moduleBackground: Color @Composable get() = surfaceContainer

    // -------------------------------------------------------------------------
    // ALIASES — compatibility with legacy call sites
    // -------------------------------------------------------------------------
    val textSecondary: Color @Composable get() = onSurfaceVariant
    val grey: Color @Composable get() = outlineVariant
    val greyLight: Color @Composable get() = outline
    val greyDark: Color @Composable get() = surfaceContainerHighest
    val onSurfaceVariantDark: Color @Composable get() = surfaceContainerHighest
    val accent: Color @Composable get() = primary
    val accentBright: Color @Composable get() = primaryFixedDim
    val badgeBackground: Color @Composable get() = surfaceContainerHighest
    val badgeText: Color @Composable get() = onSurface
    val inputGlow: Color @Composable get() = primary.copy(alpha = 0.25f)

    // -------------------------------------------------------------------------
    // EXTENDED — status & semantic colors
    // -------------------------------------------------------------------------
    val success = Color(0xFF4CAF50)
    val warning = Color(0xFFFFB74D)
    val accentGreen = Color(0xFF4CAF50)

    val statusOnline = Color(0xFF4CAF50)
    val statusOffline: Color @Composable get() = error
    val statusWaiting = Color(0xFFFFB74D)
    val statusThinking: Color @Composable get() = primary
    val statusProcessing: Color @Composable get() = secondary

    val statusSuccess = Color(0xFF4CAF50)
    val statusError: Color @Composable get() = error
    val statusWarning = Color(0xFFFFB74D)
    val statusInfo: Color @Composable get() = primary

    val white = Color(0xFFFFFFFF)

    // -------------------------------------------------------------------------
    // DIFF VIEWER TOKENS
    // -------------------------------------------------------------------------
    val diffAdditionLine = Color(0xFF22C55E)
    val diffDeletionLine = Color(0xFFEF4444)
    val diffHunkHeader: Color @Composable get() = onBackground.copy(alpha = 0.3f)
    val diffFileHeader: Color @Composable get() = primary.copy(alpha = 0.8f)
    val diffAddition = Color(0x1A4CAF50)
    val diffDeletion = Color(0x1AEF5350)
    val diffAdditionText = Color(0xFF4CAF50)
    val diffDeletionText = Color(0xFFEF5350)

    // -------------------------------------------------------------------------
    // CODE / SYNTAX HIGHLIGHTING
    // -------------------------------------------------------------------------
    val syntaxKeyword = Color(0xFFC586C0)
    val syntaxFunction = Color(0xFF61DAFB)
    val syntaxString = Color(0xFFCE9178)
    val syntaxType = Color(0xFF9E9E9E)
    val syntaxComment = Color(0xFF6A9955)
    val syntaxPunctuation = Color(0xFFD4D4D4)
    val lineNumbers = Color(0xFF666666)

    val fileTsx = Color(0xFF61DAFB)
    val fileCss = Color(0xFF9E9E9E)
    val fileJson = Color(0xFFEF5350)

    // -------------------------------------------------------------------------
    // SHIMMER
    // -------------------------------------------------------------------------
    val shimmerBase: Color @Composable get() = surfaceContainer
    val shimmerHighlight: Color @Composable get() = surfaceContainerHighest
    val shimmerAccent: Color @Composable get() = primary.copy(alpha = 0.12f)
}
