package com.mocca.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MOCCA Spacing tokens for consistent layout.
 * 4dp/8dp base grid system with comfortable touch targets.
 *
 * Base scale (monotonically increasing):
 *   xxs=2 < xs=4 < sm=8 < md=12 < lg=16 < xl=24 < xxl=32 < xxxl=48
 */
object AppSpacing {

    // BASE SPACING SCALE (4dp/8dp grid — monotonically increasing)


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

    /** 24dp - Spacious (screen sections) */
    val xl: Dp = 24.dp

    /** 32dp - Very spacious (major divisions) */
    val xxl: Dp = 32.dp

    /** 48dp - Hero spacing */
    val xxxl: Dp = 48.dp

    // SEMANTIC SPACING — Named tokens for common use cases


    /** Clearance above bottom navigation bar for scrollable content */
    val bottomBarClearance: Dp = 80.dp

    /** Echo-style floating toolbar height */
    val floatingToolbarHeight: Dp = 72.dp

    /** Standard screen edge padding (horizontal) */
    val screenPadding: Dp = 16.dp

    /** Standard card internal padding */
    val cardPaddingDefault: Dp = 16.dp

    /** Vertical gap between major content sections */
    val sectionGapDefault: Dp = 24.dp

    /** Vertical spacing between items in a list */
    val listItemSpacing: Dp = 8.dp

    // SCREEN PADDING


    /** Screen horizontal padding - comfortable touch margins */
    val screenPaddingHorizontal: Dp = 16.dp

    /** Screen horizontal padding - compact variant */
    val screenPaddingHorizontalCompact: Dp = 12.dp

    /** Screen vertical padding */
    val screenPaddingVertical: Dp = 12.dp

    /** Screen top padding (below status bar) */
    val screenPaddingTop: Dp = 12.dp

    /** Screen bottom padding (above navigation) */
    val screenPaddingBottom: Dp = 16.dp

    // CARD PADDING


    /** Card internal padding */
    val cardPadding: Dp = 16.dp

    /** Card compact padding */
    val cardPaddingCompact: Dp = 12.dp

    /** Card large padding */
    val cardPaddingLarge: Dp = 20.dp

    /** Module card padding */
    val modulePadding: Dp = 16.dp

    // LIST ITEM SPACING


    /** List item vertical padding */
    val listItemPaddingVertical: Dp = 12.dp

    /** List item horizontal padding */
    val listItemPaddingHorizontal: Dp = 12.dp

    /** List item compact vertical padding */
    val listItemPaddingVerticalCompact: Dp = 8.dp

    /** Gap between list items */
    val listItemGap: Dp = 6.dp

    // COMPONENT GAPS


    /** Gap between icon and text */
    val iconTextGap: Dp = 8.dp

    /** Gap between icon and text - compact */
    val iconTextGapCompact: Dp = 6.dp

    /** Gap between stacked elements */
    val stackGap: Dp = 12.dp

    /** Gap between inline elements */
    val inlineGap: Dp = 6.dp

    /** Gap between sections */
    val sectionGap: Dp = 24.dp

    /** Gap between cards */
    val cardGap: Dp = 8.dp

    /** Gap between module cards */
    val moduleGap: Dp = 8.dp

    // INPUT FIELD SPACING


    /** Input field internal padding */
    val inputPadding: Dp = 12.dp

    /** Input field horizontal padding */
    val inputPaddingHorizontal: Dp = 16.dp

    /** Input field vertical padding */
    val inputPaddingVertical: Dp = 12.dp

    /** Input icon padding from edge */
    val inputIconPadding: Dp = 16.dp

    // BUTTON PADDING


    /** Button internal horizontal padding */
    val buttonPaddingHorizontal: Dp = 16.dp

    /** Button internal vertical padding */
    val buttonPaddingVertical: Dp = 12.dp

    /** Button compact horizontal padding */
    val buttonPaddingHorizontalCompact: Dp = 12.dp

    /** Button compact vertical padding */
    val buttonPaddingVerticalCompact: Dp = 8.dp

    /** Pill button horizontal padding */
    val pillPaddingHorizontal: Dp = 16.dp

    /** Pill button vertical padding */
    val pillPaddingVertical: Dp = 6.dp

    // BADGE/TAG PADDING


    /** Badge horizontal padding */
    val badgePaddingHorizontal: Dp = 6.dp

    /** Badge vertical padding */
    val badgePaddingVertical: Dp = 2.dp

    /** Tag/chip horizontal padding */
    val tagPaddingHorizontal: Dp = 10.dp

    /** Tag/chip vertical padding */
    val tagPaddingVertical: Dp = 4.dp

    // DIVIDER/SEPARATOR MARGINS


    /** Divider margin */
    val dividerMargin: Dp = 6.dp

    /** Section header margin top */
    val sectionMarginTop: Dp = 24.dp

    /** Section header margin bottom */
    val sectionMarginBottom: Dp = 12.dp

    // BORDER WIDTHS


    /** Thin border (1dp) - subtle outlines */
    val borderThin: Dp = 1.dp

    /** Standard border (2dp) - visible outlines */
    val borderStandard: Dp = 1.5.dp

    /** Thick border (3dp) - prominent outlines */
    val borderThick: Dp = 2.dp

    /** Active indicator width */
    val activeIndicatorWidth: Dp = 1.5.dp

    // COMPONENT SIZES


    /** Input field height */
    val inputHeight: Dp = 56.dp

    /** Input field height compact */
    val inputHeightCompact: Dp = 48.dp

    /** Button height - pill style */
    val buttonHeight: Dp = 56.dp

    /** Button height compact */
    val buttonHeightCompact: Dp = 40.dp

    /** Button height small */
    val buttonHeightSmall: Dp = 32.dp

    /** Icon button size */
    val iconButtonSize: Dp = 40.dp

    /** Icon button size compact */
    val iconButtonSizeCompact: Dp = 32.dp

    /** Status dot size */
    val statusDotSize: Dp = 6.dp

    /** Status dot size small */
    val statusDotSizeSmall: Dp = 4.dp

    /** Status dot size large */
    val statusDotSizeLarge: Dp = 8.dp

    /** Avatar size */
    val avatarSize: Dp = 32.dp

    /** Avatar size large */
    val avatarSizeLarge: Dp = 40.dp

    /** Top bar height */
    val topBarHeight: Dp = 56.dp

    /** Bottom input bar height */
    val bottomInputBarHeight: Dp = 96.dp

    /** Action toolbar height */
    val actionToolbarHeight: Dp = 48.dp

    /** FAB size */
    val fabSize: Dp = 48.dp

    /** FAB size large */
    val fabSizeLarge: Dp = 56.dp

    /** Panel width (for swipe panels) */
    val panelWidth: Dp = 280.dp

    /** Module card min height */
    val moduleCardMinHeight: Dp = 140.dp

    /** Module card standard height */
    val moduleCardHeight: Dp = 160.dp

    // ICON SIZES


    /** Icon size small */
    val iconSizeSmall: Dp = 16.dp

    /** Icon size medium */
    val iconSizeMedium: Dp = 20.dp

    /** Icon size large */
    val iconSizeLarge: Dp = 24.dp

    /** Icon size extra large */
    val iconSizeXLarge: Dp = 28.dp

    // CORNER RADII (for convenience)


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

    /** Connected group outer corner radius */
    val cornerRadiusGroupOuter: Dp = 24.dp

    /** Connected group inner corner radius */
    val cornerRadiusGroupInner: Dp = 4.dp

    /** Pill corner radius */
    val cornerRadiusPill: Dp = 32.dp

    // UNIFIED BOTTOM BAR DIMENSIONS


    /** Navigation mode height (compact) */
    val bottomBarCompactHeight: Dp = 56.dp

    /** Chat input mode minimum height (expanded) */
    val bottomBarExpandedMinHeight: Dp = 140.dp

    /** Chat input mode maximum height (with multi-line) */
    val bottomBarExpandedMaxHeight: Dp = 200.dp

    /** Bottom bar horizontal margin */
    val bottomBarMarginHorizontal: Dp = 16.dp
}
