package com.mocca.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Terminal spacing tokens for consistent layout.
 * Based on 4dp base unit, compact for terminal aesthetic.
 */
object TerminalSpacing {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BASE SPACING SCALE (4dp base unit)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** 2dp - Minimal gap (icon-text micro spacing) */
    val xxs: Dp = 2.dp
    
    /** 4dp - Very tight (inline elements) */
    val xs: Dp = 4.dp
    
    /** 8dp - Compact (list items, form fields) */
    val sm: Dp = 8.dp
    
    /** 12dp - Standard (section gaps) */
    val md: Dp = 12.dp
    
    /** 16dp - Comfortable (card padding, screen margins) */
    val lg: Dp = 16.dp
    
    /** 24dp - Spacious (major sections) */
    val xl: Dp = 24.dp
    
    /** 32dp - Very spacious (screen divisions) */
    val xxl: Dp = 32.dp
    
    /** 40dp - Hero spacing (onboarding elements) */
    val xxxl: Dp = 40.dp
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENT-SPECIFIC SPACING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Screen horizontal padding */
    val screenPaddingHorizontal: Dp = 16.dp
    
    /** Screen vertical padding */
    val screenPaddingVertical: Dp = 16.dp
    
    /** Card internal padding */
    val cardPadding: Dp = 16.dp
    
    /** List item vertical padding */
    val listItemPaddingVertical: Dp = 12.dp
    
    /** List item horizontal padding */
    val listItemPaddingHorizontal: Dp = 16.dp
    
    /** Gap between icon and text */
    val iconTextGap: Dp = 8.dp
    
    /** Input field internal padding */
    val inputPadding: Dp = 12.dp
    
    /** Button internal horizontal padding */
    val buttonPaddingHorizontal: Dp = 24.dp
    
    /** Button internal vertical padding */
    val buttonPaddingVertical: Dp = 16.dp
    
    /** Divider margin */
    val dividerMargin: Dp = 8.dp
    
    /** Section header margin top */
    val sectionMarginTop: Dp = 24.dp
    
    /** Section header margin bottom */
    val sectionMarginBottom: Dp = 12.dp
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BORDER WIDTHS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Thin border (1dp) - subtle outlines */
    val borderThin: Dp = 1.dp
    
    /** Standard border (2dp) - input fields, cards */
    val borderStandard: Dp = 2.dp
    
    /** Thick border (4dp) - decorative brackets */
    val borderThick: Dp = 4.dp
    
    /** Active indicator width (left bar on selected items) */
    val activeIndicatorWidth: Dp = 4.dp
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENT SIZES
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Input field height */
    val inputHeight: Dp = 56.dp
    
    /** Button height */
    val buttonHeight: Dp = 56.dp
    
    /** Icon button size */
    val iconButtonSize: Dp = 48.dp
    
    /** Status dot size */
    val statusDotSize: Dp = 8.dp
    
    /** Top bar height */
    val topBarHeight: Dp = 56.dp
    
    /** Bottom input bar height */
    val bottomInputBarHeight: Dp = 120.dp
    
    /** Action toolbar height */
    val actionToolbarHeight: Dp = 48.dp
    
    /** Panel width (for swipe panels) */
    val panelWidth: Dp = 300.dp
}
