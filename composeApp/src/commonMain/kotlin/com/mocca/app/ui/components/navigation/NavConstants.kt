package com.mocca.app.ui.components.navigation

import androidx.compose.ui.unit.dp

/**
 * Centralized constants for bottom navigation components.
 * 
 * These values ensure consistent sizing across all navigation modes
 * and comply with Material Design 3 touch target requirements.
 * 
 * CRITICAL: These values must NEVER change between navigation modes
 * to maintain optimal user experience and muscle memory.
 */
object NavConstants {

    // ICON SIZING - NEVER CHANGE BETWEEN MODES

    
    /** Nav item icon size - MUST be same in all modes */
    val NavIconSize = 22.dp
    
    /** Nav item icon size for compact indicator (bottom of chat input) */
    val NavIconSizeCompact = 22.dp  // SAME as NavIconSize - ensures consistency

    // TOUCH TARGETS - Material Design 3 Minimum

    
    /** Minimum touch target width per Material Design 3 */
    val TouchTargetMinWidth = 48.dp
    
    /** Minimum touch target height per Material Design 3 */
    val TouchTargetMinHeight = 48.dp

    // INDICATOR SIZING

    
    /** Sliding indicator width — wider for borderless pill shape */
    val IndicatorWidth = 48.dp
    
    /** Sliding indicator height */
    val IndicatorHeight = 2.dp
    
    /** Indicator dot size for selected item */
    val IndicatorDotSize = 4.dp
    
    /** Indicator dot size for unselected items */
    val IndicatorDotSizeUnselected = 3.dp

    // CONTAINER HEIGHTS

    
    /** Navigation-only mode height (compact) */
    val NavigationModeHeight = 68.dp
    
    /** Chat input mode minimum height (expanded) - includes nav row and the two-row composer */
    val ChatInputModeMinHeight = 220.dp

    // NAV ROW HEIGHTS

    
    /** Height of the persistent nav row (icons + labels/indicator) */
    val NavRowHeight = 68.dp
    
    /** Height of nav indicator section */
    val NavIndicatorSectionHeight = 10.dp
    
    /** Spacer between nav items and indicator — tighter for borderless feel */
    val NavItemToIndicatorSpacing = 4.dp

    // CHAT INPUT COMPONENT HEIGHTS

    
    /** Status bar height (model/agent selectors) - optimized for touch targets */
    val StatusBarHeight = 28.dp
    
    /** Input field minimum height */
    val InputFieldMinHeight = 28.dp
    
    /** Input field maximum height */
    val InputFieldMaxHeight = 72.dp
    
    /** Action toolbar height - maintains 48dp minimum touch targets */
    val ActionToolbarHeight = 48.dp

    // TYPOGRAPHY

    
    /** Nav label font size */
    val NavLabelFontSize = 10.dp
    
    /** Nav label letter spacing */
    val NavLabelLetterSpacing = 0.3.dp
    
    /** Nav label line height */
    val NavLabelLineHeight = 12.dp

    // SPACING

    
    /** Spacing between icon and label */
    val IconToLabelSpacing = 2.dp
    
    /** Spacing between label and indicator */
    val LabelToIndicatorSpacing = 6.dp
    
    /** Nav item internal padding horizontal */
    val NavItemPaddingHorizontal = 6.dp
    
    /** Nav item internal padding vertical */
    val NavItemPaddingVertical = 2.dp

    // CHAT INPUT REFINED SIZING (Premium Polish - Optimized for Touch Targets)

    
    /** Status bar chip height - optimized for touch targets */
    val StatusBarChipHeight = 24.dp
    
    /** Status bar icon size (increased for visibility) */
    val StatusBarIconSize = 12.dp
    
    /** Action toolbar button size (visual) - WCAG 2.5.5 minimum 48dp */
    val ActionButtonSize = 48.dp

    /** Secondary composer action size */
    val CompactActionButtonSize = 40.dp
    
    /** Send button height - maintains 48dp minimum touch targets */
    val SendButtonHeight = 48.dp
    
    /** Send button icon size - slightly larger for better visibility */
    val SendIconSize = 16.dp
    
    /** Action toolbar icon size */
    val ActionIconSize = 16.dp
    
    /** Minimum popup item height for touch targets */
    val PopupItemMinHeight = 48.dp
}
