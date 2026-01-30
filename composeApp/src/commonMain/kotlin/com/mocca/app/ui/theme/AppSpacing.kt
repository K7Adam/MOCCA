package com.mocca.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MOCCA Spacing tokens for consistent layout.
 * 4dp/8dp base grid system with comfortable touch targets.
 */
object AppSpacing {

    // ═══════════════════════════════════════════════════════════════════════════
    // BASE SPACING SCALE (4dp/8dp grid)
    // ═══════════════════════════════════════════════════════════════════════════

    /** 2dp - Minimal gap (micro adjustments) */
    val xxs: Dp = 2.dp

    /** 4dp - Very tight (inline elements) */
    val xs: Dp = 4.dp

    /** 8dp - Compact (icon gaps, tight lists) */
    val sm: Dp = 8.dp

    /** 12dp - Standard (form field gaps) */
    val md: Dp = 12.dp

    /** 16dp - Comfortable (card padding, section gaps) */
    val lg: Dp = 16.dp

    /** 20dp - Relaxed (between major sections) */
    val lgPlus: Dp = 20.dp

    /** 24dp - Spacious (screen sections) */
    val xl: Dp = 24.dp

    /** 32dp - Very spacious (major divisions) */
    val xxl: Dp = 32.dp

    /** 40dp - Hero spacing */
    val xxxl: Dp = 40.dp

    /** 48dp - Extra large spacing */
    val xxxxl: Dp = 48.dp

    /** 64dp - Maximum spacing */
    val max: Dp = 64.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // SCREEN PADDING
    // ═══════════════════════════════════════════════════════════════════════════

    /** Screen horizontal padding - comfortable touch margins */
    val screenPaddingHorizontal: Dp = 24.dp

    /** Screen horizontal padding - compact variant */
    val screenPaddingHorizontalCompact: Dp = 16.dp

    /** Screen vertical padding */
    val screenPaddingVertical: Dp = 16.dp

    /** Screen top padding (below status bar) */
    val screenPaddingTop: Dp = 16.dp

    /** Screen bottom padding (above navigation) */
    val screenPaddingBottom: Dp = 24.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // CARD PADDING
    // ═══════════════════════════════════════════════════════════════════════════

    /** Card internal padding */
    val cardPadding: Dp = 20.dp

    /** Card compact padding */
    val cardPaddingCompact: Dp = 16.dp

    /** Card large padding */
    val cardPaddingLarge: Dp = 24.dp

    /** Module card padding */
    val modulePadding: Dp = 20.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // LIST ITEM SPACING
    // ═══════════════════════════════════════════════════════════════════════════

    /** List item vertical padding */
    val listItemPaddingVertical: Dp = 16.dp

    /** List item horizontal padding */
    val listItemPaddingHorizontal: Dp = 16.dp

    /** List item compact vertical padding */
    val listItemPaddingVerticalCompact: Dp = 12.dp

    /** Gap between list items */
    val listItemGap: Dp = 8.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENT GAPS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Gap between icon and text */
    val iconTextGap: Dp = 12.dp

    /** Gap between icon and text - compact */
    val iconTextGapCompact: Dp = 8.dp

    /** Gap between stacked elements */
    val stackGap: Dp = 16.dp

    /** Gap between inline elements */
    val inlineGap: Dp = 8.dp

    /** Gap between sections */
    val sectionGap: Dp = 32.dp

    /** Gap between cards */
    val cardGap: Dp = 12.dp

    /** Gap between module cards */
    val moduleGap: Dp = 12.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // INPUT FIELD SPACING
    // ═══════════════════════════════════════════════════════════════════════════

    /** Input field internal padding */
    val inputPadding: Dp = 16.dp

    /** Input field horizontal padding */
    val inputPaddingHorizontal: Dp = 20.dp

    /** Input field vertical padding */
    val inputPaddingVertical: Dp = 16.dp

    /** Input icon padding from edge */
    val inputIconPadding: Dp = 24.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTON PADDING
    // ═══════════════════════════════════════════════════════════════════════════

    /** Button internal horizontal padding */
    val buttonPaddingHorizontal: Dp = 24.dp

    /** Button internal vertical padding */
    val buttonPaddingVertical: Dp = 16.dp

    /** Button compact horizontal padding */
    val buttonPaddingHorizontalCompact: Dp = 20.dp

    /** Button compact vertical padding */
    val buttonPaddingVerticalCompact: Dp = 12.dp

    /** Pill button horizontal padding */
    val pillPaddingHorizontal: Dp = 20.dp

    /** Pill button vertical padding */
    val pillPaddingVertical: Dp = 8.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // BADGE/TAG PADDING
    // ═══════════════════════════════════════════════════════════════════════════

    /** Badge horizontal padding */
    val badgePaddingHorizontal: Dp = 8.dp

    /** Badge vertical padding */
    val badgePaddingVertical: Dp = 4.dp

    /** Tag/chip horizontal padding */
    val tagPaddingHorizontal: Dp = 12.dp

    /** Tag/chip vertical padding */
    val tagPaddingVertical: Dp = 6.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // DIVIDER/SEPARATOR MARGINS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Divider margin */
    val dividerMargin: Dp = 8.dp

    /** Section header margin top */
    val sectionMarginTop: Dp = 32.dp

    /** Section header margin bottom */
    val sectionMarginBottom: Dp = 16.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // BORDER WIDTHS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Thin border (1dp) - subtle outlines */
    val borderThin: Dp = 1.dp

    /** Standard border (2dp) - visible outlines */
    val borderStandard: Dp = 2.dp

    /** Thick border (3dp) - prominent outlines */
    val borderThick: Dp = 3.dp

    /** Active indicator width */
    val activeIndicatorWidth: Dp = 2.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENT SIZES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Input field height */
    val inputHeight: Dp = 64.dp

    /** Input field height compact */
    val inputHeightCompact: Dp = 56.dp

    /** Button height - pill style */
    val buttonHeight: Dp = 64.dp

    /** Button height compact */
    val buttonHeightCompact: Dp = 48.dp

    /** Button height small */
    val buttonHeightSmall: Dp = 36.dp

    /** Icon button size */
    val iconButtonSize: Dp = 48.dp

    /** Icon button size compact */
    val iconButtonSizeCompact: Dp = 40.dp

    /** Status dot size */
    val statusDotSize: Dp = 8.dp

    /** Status dot size small */
    val statusDotSizeSmall: Dp = 6.dp

    /** Status dot size large */
    val statusDotSizeLarge: Dp = 10.dp

    /** Avatar size */
    val avatarSize: Dp = 40.dp

    /** Avatar size large */
    val avatarSizeLarge: Dp = 48.dp

    /** Top bar height */
    val topBarHeight: Dp = 64.dp

    /** Bottom input bar height */
    val bottomInputBarHeight: Dp = 120.dp

    /** Action toolbar height */
    val actionToolbarHeight: Dp = 56.dp

    /** FAB size */
    val fabSize: Dp = 56.dp

    /** FAB size large */
    val fabSizeLarge: Dp = 64.dp

    /** Panel width (for swipe panels) */
    val panelWidth: Dp = 300.dp

    /** Module card min height */
    val moduleCardMinHeight: Dp = 160.dp

    /** Module card standard height */
    val moduleCardHeight: Dp = 180.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // ICON SIZES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Icon size small */
    val iconSizeSmall: Dp = 16.dp

    /** Icon size medium */
    val iconSizeMedium: Dp = 20.dp

    /** Icon size large */
    val iconSizeLarge: Dp = 24.dp

    /** Icon size extra large */
    val iconSizeXLarge: Dp = 28.dp

    // ═══════════════════════════════════════════════════════════════════════════
    // CORNER RADII (for convenience)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Small corner radius */
    val cornerRadiusSmall: Dp = 8.dp

    /** Medium corner radius */
    val cornerRadiusMedium: Dp = 12.dp

    /** Large corner radius */
    val cornerRadiusLarge: Dp = 16.dp

    /** Extra large corner radius */
    val cornerRadiusXLarge: Dp = 24.dp

    /** Module corner radius */
    val cornerRadiusModule: Dp = 28.dp

    /** Pill corner radius */
    val cornerRadiusPill: Dp = 32.dp
}
